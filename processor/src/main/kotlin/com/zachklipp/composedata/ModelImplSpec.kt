package com.zachklipp.composedata

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

/**
 * TODO write documentation
 */
data class ModelImplSpec(
  val spec: TypeSpec,
  val name: ClassName,
  val imports: List<MemberName>,
  val saverSpec: TypeSpec?,
  val saverName: TypeName?
)
