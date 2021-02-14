package com.zachklipp.composedata

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import java.io.PrintWriter

class ComposeDataProcessor : SymbolProcessor {

  private lateinit var codeGenerator: CodeGenerator
  private lateinit var logger: KSPLogger
  private lateinit var parser: Parser
  private lateinit var typeConverter: TypeConverter

  override fun init(
    options: Map<String, String>,
    kotlinVersion: KotlinVersion,
    codeGenerator: CodeGenerator,
    logger: KSPLogger
  ) {
    this.codeGenerator = codeGenerator
    this.logger = logger
    parser = Parser(logger)
    typeConverter = TypeConverter(logger)
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val annotatedSymbols =
      resolver.getSymbolsWithAnnotation(COMPOSE_DATA_ANNOTATION.qualifiedName!!)
    if (annotatedSymbols.isEmpty()) {
      return emptyList()
    }

    val invalidSymbols = mutableListOf<KSAnnotated>()

    annotatedSymbols.forEach { symbol ->
      if (symbol !is KSClassDeclaration) return@forEach
      if (!symbol.validate()) {
        logger.warn("Invalid", symbol)
        invalidSymbols += symbol
        return@forEach
      }

      processComposeDataClass(symbol, resolver)
    }

    return invalidSymbols
  }

  override fun finish() = Unit

  private fun processComposeDataClass(symbol: KSClassDeclaration, resolver: Resolver) {
    val modelInterface = parser.parseModelInterface(symbol, resolver.builtIns) ?: return
    val builderInterface = generateBuilderInterface(modelInterface, typeConverter)
    val implClass = generateImplClass(modelInterface, builderInterface, typeConverter)
    val rememberFunction =
      generateRememberFunction(modelInterface, builderInterface, implClass, typeConverter)

    val packageName = symbol.packageName.asString()
    val generatedFile = FileSpec.builder(packageName, builderInterface.spec.name!!)
      .addFunction(rememberFunction)
      .addType(builderInterface.spec)
      .addType(implClass.spec)
      .apply {
        implClass.imports.takeUnless { it.isEmpty() }?.forEach {
          addImport(it.packageName, it.simpleName)
        }
      }
      .build()

    codeGenerator.createNewFile(
      Dependencies(aggregating = false, symbol.containingFile!!),
      packageName = packageName,
      fileName = generatedFile.name
    ).let(::PrintWriter).use {
      generatedFile.writeTo(it)
    }
  }

  companion object {
    val COMPOSE_DATA_ANNOTATION = ComposeData::class
  }
}
