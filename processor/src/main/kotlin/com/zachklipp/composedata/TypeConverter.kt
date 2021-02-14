package com.zachklipp.composedata

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

/**
 * TODO write documentation
 */
class TypeConverter(private val logger: KSPLogger) {

  fun KSTypeReference.toTypeName(): TypeName {
    logger.warn("toTypeName($this)")

    val resolved = resolve()

    return ClassName(
      resolved.declaration.packageName.asString(),
      resolved.declaration.simpleName.getShortName()
    )

    return when (val element = element) {
      is KSClassifierReference -> {
        ClassName.bestGuess(element.referencedName())
          .apply {
            if (element.typeArguments.isNotEmpty()) {
              parameterizedBy(element.typeArguments.map {
                it.type!!.toTypeName()
              })
            }
          }
      }
      // is KSCallableReference -> {
      //   TODO()
      // }
      null -> {
        logger.error("Type element is null", this)
        TODO()
      }
      else -> {
        logger.error("Unsupported type: ${element::class.simpleName}: $this", this)
        TODO()
      }
    }
  }
}