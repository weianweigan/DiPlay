package com.shilapi.xcertplay

import android.app.Activity
import android.app.AlertDialog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBluetoothAdapter
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PhonePickerIntegrationTest {
    private var screen: DiPlayActivity? = null

    @After fun cleanup() {
        PhonePickerIntegration.picker = null
        screen?.getSharedPreferences("diplay", 0)?.edit()?.clear()?.commit()
        screen?.getSharedPreferences("xcertplay_airplay", 0)?.edit()?.clear()?.commit()
        screen?.finish()
        ShadowBluetoothAdapter.setIsBluetoothSupported(true)
        AppAppearanceRuntime.resetForTest()
    }

    @Test fun externalPickerWorksWithoutAndroidBluetoothAndPersistsSelection() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(false)
        var invoked = false
        PhonePickerIntegration.picker = object : WirelessPhonePicker {
            override fun show(activity: Activity, dialogBuilder: () -> AlertDialog.Builder,
                              onSelected: (String, String) -> Unit, onCancelled: () -> Unit) {
                invoked = true
                onSelected("02:11:22:33:44:55", "Relay phone")
            }
        }
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).setup().get().also { screen = it }
        ReflectionHelpers.callInstanceMethod<Unit>(activity, "choosePhone")
        assertTrue(invoked)
        assertEquals("02:11:22:33:44:55", DiPlayPreferences.phoneAddress(activity))
    }
}
