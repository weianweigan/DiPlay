package com.shilapi.xcertplay

import android.app.Activity
import android.app.AlertDialog

/** An optional product-owned picker; null keeps the system Bluetooth picker. */
object PhonePickerIntegration {
    @Volatile var picker: WirelessPhonePicker? = null
}

interface WirelessPhonePicker {
    fun show(activity: Activity, dialogBuilder: () -> AlertDialog.Builder, onSelected: (String, String) -> Unit, onCancelled: () -> Unit)
}
