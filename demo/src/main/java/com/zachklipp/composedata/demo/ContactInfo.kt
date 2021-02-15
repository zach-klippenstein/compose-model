package com.zachklipp.composedata.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.zachklipp.composedata.ComposeData

@ComposeData
interface ContactInfo {

  val name: String
  val address: Address

  // Is excluded from builder function parameters.
  val edits: Int get() = 0

  fun onNameChanged(name: String)
  fun onSubmitClicked()

  // Generates a warning.
  fun badlyNamedHandler()

  // This function is excluded from the builder since it has a default value.
  fun log(message: String) = println("ViewModel: $message")
}

@Composable fun ContactInfo() = rememberContactInfo(
  name = "",
  address = Address()
) {
  onNameChanged {
    name = it
    edits++
  }
}

@Preview(showBackground = true)
@Composable fun ContactInfoPreview() {
  ContactInfo()
}
