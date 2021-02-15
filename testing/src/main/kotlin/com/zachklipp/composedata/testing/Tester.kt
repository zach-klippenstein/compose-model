package com.zachklipp.composedata.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.coroutines.EmptyCoroutineContext

/**
 * TODO write documentation
 */
fun <VM : Any> test(
  content: /*@Composable*/ () -> VM,
  block: () -> Unit
) {
  val scope = CoroutineScope(EmptyCoroutineContext)
  try {
    // TODO create composition
    block()
  } finally {
    scope.cancel()
  }
}
