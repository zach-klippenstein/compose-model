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
  val contactInfo = ContactInfo()

  Column {
    BasicText("Hello, ${contactInfo.name}!")
    BasicTextField(
      value = contactInfo.name, onValueChange = contactInfo::onNameChanged,
      Modifier.border(1.dp, Color.Black).padding(4.dp)
    )
    BasicText("${contactInfo.edits} edits", style = TextStyle(fontStyle = Italic))
    Spacer(Modifier.size(16.dp))
    AddressEditor(contactInfo.address)
  }
}

@Composable private fun AddressEditor(address: Address) {
  Column {
    BasicText("Street:")
    BasicTextField(
      address.street, onValueChange = address::onStreetChanged,
      Modifier.border(1.dp, Color.Black).padding(4.dp)
    )
    BasicText("City:")
    BasicTextField(
      address.city, onValueChange = address::onCityChanged,
      Modifier.border(1.dp, Color.Black).padding(4.dp)
    )
    BasicText("State:")
    BasicTextField(
      address.state, onValueChange = address::onStateChanged,
      Modifier.border(1.dp, Color.Black).padding(4.dp)
    )
  }
}
