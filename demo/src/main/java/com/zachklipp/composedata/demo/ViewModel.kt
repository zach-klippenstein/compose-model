package com.zachklipp.composedata.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zachklipp.composedata.ComposeData

@ComposeData
interface ViewModel {

  val name: String
  val address: Address

  // Is excluded from builder function parameters.
  val edits: Int get() = 0

  fun onNameChanged(name: String)
  fun onSubmitClicked()

  // Generates a warning.
  fun badlyNamedHandler()

  // Is excluded from the builder.
  fun log(message: String) = println("ViewModel: $message")
}

@Composable fun ViewModel() = rememberViewModel(
  name = "",
  address = Address()
) {
  var nameValue by rememberSaveable { mutableStateOf("") }

  it.onNameChanged { name ->
    nameValue = name
    it.edits++
  }

  it.name = nameValue
}

// @Composable private fun buildViewModelSample(
//   name: String,
//   address: Address,
//   update: @Composable (ViewModelBuilderSample) -> Unit
// ): ViewModel {
//   return remember { ViewModelImplSample(name, address) }
//     .also { update(it) }
// }
//
// @Stable
// private interface ViewModelBuilderSample {
//
//   var name: String
//   var address: Address
//   var serial: Int
//
//   fun onNameChanged(handler: (name: String) -> Unit)
//   fun onSubmitClicked(handler: () -> Unit)
//   fun badlyNamedHandler(handler: () -> Unit)
// }
//
// private class ViewModelImplSample(
//   name: String,
//   address: Address
// ) : ViewModel, ViewModelBuilderSample {
//   override var name: String by mutableStateOf(name)
//   override var address: Address by mutableStateOf(address)
//   override var edits: Int by mutableStateOf(super.edits)
//
//   private var _onNameChanged: ((String) -> Unit)? = null
//   private var _onSubmitClicked: (() -> Unit)? = null
//   private var _badlyNamedHandler: (() -> Unit)? = null
//
//   override fun onNameChanged(handler: (String) -> Unit) {
//     _onNameChanged = handler
//   }
//
//   override fun onSubmitClicked(handler: () -> Unit) {
//     _onSubmitClicked = handler
//   }
//
//   override fun badlyNamedHandler(handler: () -> Unit) {
//     _badlyNamedHandler = handler
//   }
//
//   override fun onNameChanged(name: String) {
//     _onNameChanged?.invoke(name)
//   }
//
//   override fun onSubmitClicked() {
//     _onSubmitClicked?.invoke()
//   }
//
//   override fun badlyNamedHandler() {
//     _badlyNamedHandler?.invoke()
//   }
// }
