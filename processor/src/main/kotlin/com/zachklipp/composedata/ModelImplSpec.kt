package com.zachklipp.composedata

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec

/**
 * TODO write documentation
 */
data class ModelImplSpec(
  val spec: TypeSpec,
  val imports: List<MemberName>
)
