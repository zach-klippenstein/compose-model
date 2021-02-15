package com.zachklipp.composedata.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.zachklipp.composedata.ComposeData

@ComposeData
interface Address {
  val street: String get() = ""
  val city: String get() = ""
  val state: String get() = ""

  fun onStreetChanged(street: String)
  fun onCityChanged(city: String)
  fun onStateChanged(state: String)
}

@Composable fun Address() = rememberAddress {
  onStreetChanged { street = it }
  onCityChanged { city = it }
  onStateChanged { state = it }
}

@Preview(showBackground = true)
@Composable fun AddressPreview() {
  Address()
}
