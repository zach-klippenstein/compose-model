package com.zachklipp.composedata

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.zachklipp.composedata.ComposeDataProcessor.Companion.COMPOSE_DATA_ANNOTATION
import kotlin.reflect.KProperty1

/**
 * TODO write documentation
 */
class Parser(private val logger: KSPLogger) {

  fun parseModelInterface(symbol: KSClassDeclaration, builtins: KSBuiltIns): ModelInterface? {
    if (symbol.classKind != INTERFACE) {
      logger.error(
        "Only interfaces may be annotated with ${COMPOSE_DATA_ANNOTATION}, but this is a ${symbol.classKind}",
        symbol
      )
      return null
    }
    if (symbol.superTypes.isNotEmpty()) {
      logger.error(
        "$COMPOSE_DATA_ANNOTATION-annotated interfaces must not extend any interfaces, " +
          "but this interface extends ${symbol.superTypes}",
        symbol
      )
      return null
    }
    if (symbol.typeParameters.isNotEmpty()) {
      logger.error(
        "$COMPOSE_DATA_ANNOTATION-annotated interfaces must not have type parameters.",
        symbol
      )
      return null
    }

    val properties = symbol.getDeclaredProperties().map {
      parseModelProperty(it) ?: return null
    }
    val eventHandlers = symbol.getDeclaredFunctions().mapNotNull {
      parseModelEventHandler(it, builtins)
    }

    return ModelInterface(
      symbol.packageName.asString(),
      symbol.simpleName.asString(),
      symbol,
      symbol.getVisibility(),
      properties,
      eventHandlers
    )
  }

  fun parseConfig(model: ModelInterface, resolver: Resolver): Config {
    val composeDataAnnotationType = resolver.getClassDeclarationByName<ComposeData>()
    val annotation = model.declaration.annotations.single {
      it.annotationType.resolve().declaration == composeDataAnnotationType
    }
    return Config(
      saveable = annotation[ComposeData::saveable] ?: true,
    )
  }

  private fun parseModelProperty(declaration: KSPropertyDeclaration): ModelProperty? {
    if (declaration.isMutable) {
      logger.error(
        "$COMPOSE_DATA_ANNOTATION interfaces must not declare mutable properties: $declaration",
        declaration
      )
      return null
    }

    return ModelProperty(declaration.simpleName.asString(), declaration)
  }

  private fun parseModelEventHandler(
    declaration: KSFunctionDeclaration,
    builtins: KSBuiltIns
  ): ModelEventHandler? {
    // Ignore functions with a default implementation.
    if (!declaration.isAbstract) {
      logger.info(
        "Skipping function $declaration because it has a default implementation.",
        declaration
      )
      return null
    }
    if (declaration.returnType == null) {
      logger.error("Unknown return type", declaration)
      return null
    }
    if (declaration.returnType!!.resolve() != builtins.unitType) {
      logger.error(
        "$COMPOSE_DATA_ANNOTATION event handler functions must return Unit",
        declaration
      )
      return null
    }

    if (declaration.typeParameters.isNotEmpty()) {
      logger.error(
        "$COMPOSE_DATA_ANNOTATION interfaces must not declare generic functions: $declaration",
        declaration
      )
      return null
    }

    if (!declaration.simpleName.asString().startsWith("on")) {
      logger.warn(
        "Event handler functions should have the prefix \"on\": $declaration",
        declaration
      )
    }

    return ModelEventHandler(
      name = declaration.simpleName.asString(),
      declaration,
      hasDefault = !declaration.isAbstract
    )
  }

  @Suppress("UNCHECKED_CAST")
  private operator fun <T> KSAnnotation.get(name: KProperty1<*, T>): T? =
    arguments.single { it.name!!.asString() == name.name }.value as T?
}