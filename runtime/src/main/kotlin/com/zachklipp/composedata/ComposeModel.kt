package com.zachklipp.composedata

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Annotates an interface as a model. The interface must contain no mutable properties, but
 * properties may have default getters. All functions must either have a default implementation or
 * return `Unit`.
 *
 * A composable function will then be generated for an interface `Foo` named `rememberFoo`. This
 * function will create an remember a `Foo` and return it. It takes a parameter for each property
 * of the interface without a default value, as well as a function that should implement the model's
 * business logic. The function parameter has a receiver of type `FooBuilder` – this is a generated
 * class that mirrors `Foo`, but does not implement it:
 *  - For each property in `Foo`, `FooBuilder` has a mutable property of the same name. This
 *    property will be initialized to either the original property's default value or the value
 *    passed to `rememberFoo`. When the property is changed, it will notify observers of the
 *    property on the original interface of the change.
 *  - For each abstract function in `Foo`, `FooBuilder` has a function of the same name that takes
 *    a _lambda_ with the same signature as the original function. The lambda passed to this
 *    function will be invoked whenever the function on the returned interface is called.
 *
 * @param saveable Whether to generate a `Saver` for the model and save it using `rememberSaveable`.
 * All properties on the class will be saved. All properties much have a type that is automatically
 * saveable in a bundle. Default is true.
 */
@MustBeDocumented
@Target(CLASS)
@Retention(BINARY)
// TODO @StableMarker
annotation class ComposeModel(
  val saveable: Boolean = true,
)
