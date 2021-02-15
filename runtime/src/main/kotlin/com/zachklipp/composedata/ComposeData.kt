package com.zachklipp.composedata

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * TODO kdoc
 *
 * @param saveable Whether to generate a `Saver` for the model and save it using `rememberSaveable`.
 * All properties on the class will be saved. All properties much have a type that is automatically
 * saveable in a bundle.
 */
@MustBeDocumented
@Target(CLASS)
@Retention(BINARY)
// TODO @StableMarker
annotation class ComposeData(
  val saveable: Boolean = true,
)
