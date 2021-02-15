package com.zachklipp.composedata

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Visibility

/**
 * TODO write documentation
 */
data class ModelInterface(
  val packageName: String,
  val simpleName: String,
  val declaration: KSClassDeclaration,
  val visibility: Visibility,
  val properties: List<ModelProperty>,
  val eventHandlers: List<ModelEventHandler>,
)

data class ModelProperty(
  val name: String,
  val declaration: KSPropertyDeclaration,
  val hasDefault: Boolean = declaration.getter != null
)

data class ModelEventHandler(
  val name: String,
  val declaration: KSFunctionDeclaration,
  val hasDefault: Boolean = !declaration.isAbstract,
)
