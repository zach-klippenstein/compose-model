package com.zachklipp.composedata.demo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle.Italic
import androidx.compose.ui.unit.dp

class DemoActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      App()
    }
  }
}

@Composable fun App() {
  val contactInfo = ContactInfoModel()

  Column {
    BasicText("Hello, ${contactInfo.name}!")
    BasicTextField(
      value = contactInfo.name, onValueChange = contactInfo::onNameChanged,
      Modifier.border(1.dp, Color.Black).padding(4.dp)
    )
    BasicText("${contactInfo.edits} edits", style = TextStyle(fontStyle = Italic))
    Spacer(Modifier.size(16.dp))
    AddressEditor(contactInfo.addressModel)
  }
}

@Composable private fun AddressEditor(addressModel: AddressModel) {
  Column {
    BasicText("Street:")
    BasicTextField(
      addressModel.street, onValueChange = addressModel::onStreetChanged,
      Modifier.border(1.dp, Color.Black).padding(4.dp)
    )
    BasicText("City:")
    BasicTextField(
      addressModel.city, onValueChange = addressModel::onCityChanged,
      Modifier.border(1.dp, Color.Black).padding(4.dp)
    )
    BasicText("State:")
    BasicTextField(
      addressModel.state, onValueChange = addressModel::onStateChanged,
      Modifier.border(1.dp, Color.Black).padding(4.dp)
    )
  }
}
