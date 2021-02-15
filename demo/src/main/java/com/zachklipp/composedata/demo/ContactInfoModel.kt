package com.zachklipp.composedata.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.zachklipp.composedata.ComposeModel

/**
 * Represents some contact info about a person.
 * Because it's annotated with a [ComposeModel] annotation, a [rememberContactInfoModel] function is
 * automatically generated for it. The [ContactInfoModel] function below uses the generated function
 * to implement business logic.
 */
@ComposeModel
interface ContactInfoModel {

  val name: String
  val addressModel: AddressModel

  // Is excluded from builder function parameters.
  val edits: Int get() = 0

  fun onNameChanged(name: String)
  fun onSubmitClicked()

  // Generates a warning.
  fun badlyNamedHandler()

  // This function is excluded from the builder since it has a default value.
  fun log(message: String) = println("ContactInfoModel: $message")
}

/**
 * This function doesn't draw UI - it is only responsible for driving a [ContactInfoModel].
 */
@Composable fun ContactInfoModel(initialName: String = "world") = rememberContactInfoModel(
  name = initialName,
  addressModel = AddressModel()
) {
  onNameChanged {
    name = it
    edits++
  }
}

@Preview(showBackground = true)
@Composable fun ContactInfoPreview() {
  ContactInfoModel()
}
