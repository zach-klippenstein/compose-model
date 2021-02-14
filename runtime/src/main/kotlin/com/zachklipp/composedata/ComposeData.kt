package com.zachklipp.composedata

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * TODO kdoc
 *
 * @param generateEntryPoint If true, a regular, non-Composable builder function will be generated
 * to create an instance of the view model outside of a composition. The entry point will require
 * a `CoroutineScope` and will launch an internal composition to run the builder in.
 */
@MustBeDocumented
@Target(CLASS)
@Retention(BINARY)
// TODO @StableMarker
annotation class ComposeData(val generateEntryPoint: Boolean = false)
