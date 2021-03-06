package com.fkorotkov.kotlin.dsl

import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty

object BuilderGenerator {
  fun generateBuildersForPropertyFile(
    outputFolder: File,
    outputPackage: String,
    outputFileName: String,
    clazzToProperties: List<Pair<KClass<*>, KMutableProperty<*>>>
  ) {
    val destinationFolder = File(outputFolder, outputPackage.replace('.', File.separatorChar))
    if (!destinationFolder.exists()) {
      destinationFolder.mkdirs()
    }
    val destinationFile = File(destinationFolder, outputFileName)
    destinationFile.createNewFile()

    val allCalsses = clazzToProperties.flatMap { (clazz, property) ->
      listOf(clazz, property.returnType.classifier as KClass<*>)
    }

    destinationFile.writeText(generateBuilders(allCalsses, clazzToProperties, outputPackage))
  }

  private fun generateBuilders(allCalsses: List<KClass<*>>, clazzToProperties: List<Pair<KClass<*>, KMutableProperty<*>>>, outputPackage: String): String {
    return """// GENERATED
package $outputPackage

${
allCalsses.map { it.qualifiedName }.toSet().map { "import $it" }.sorted().joinToString("\n")
}

${
clazzToProperties.map { (clazz, property) -> extensionFunctionTemplate(clazz, property) }.joinToString("\n")
}
"""
  }

  private fun extensionFunctionTemplate(clazz: KClass<*>, property: KMutableProperty<*>): String {
    val returnClass = property.returnType.classifier as KClass<*>
    val generics: List<String> = (1..clazz.typeParameters.size).map { "T$it" }

    val clazzDecl = clazz.simpleName + genericsTemplate(generics)
    val returnClassDecl = returnClass.simpleName + genericsTemplate(Collections.nCopies(returnClass.typeParameters.size, "*"))

    return """
fun ${genericsTemplate(generics)} $clazzDecl.`${property.name}`(block: $returnClassDecl.() -> Unit = {}) {${initializer(property, returnClass)}
  this.`${property.name}`.block()
}
"""
  }

  private fun initializer(property: KMutableProperty<*>, returnClass: KClass<*>): String {
    if (returnClass.isAbstract) return ""
    return """
  if(this.`${property.name}` == null) {
    this.`${property.name}` = ${returnClass.simpleName}()
  }
"""
  }

  private fun genericsTemplate(generics: List<String>): String {
    if (generics.isEmpty()) {
      return ""
    } else {
      return generics.joinToString(
        separator = ", ",
        prefix = "<",
        postfix = ">"
      )
    }
  }
}