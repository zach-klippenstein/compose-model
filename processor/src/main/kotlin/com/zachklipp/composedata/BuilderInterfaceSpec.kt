package com.zachklipp.composedata

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

data class BuilderInterfaceSpec(
  val modelInterface: ModelInterface,
  val spec: TypeSpec,
  val properties: List<Pair<ModelProperty, PropertySpec>>,
  val eventHandlers: List<EventHandlerSpec>
)

data class EventHandlerSpec(
  val name: String,
  val eventHandler: ModelEventHandler,
  val lambdaTypeName: LambdaTypeName,
  val setter: FunSpec
)
