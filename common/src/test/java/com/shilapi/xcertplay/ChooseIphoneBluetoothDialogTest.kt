package com.shilapi.xcertplay

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowBluetoothAdapter
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/** The fork chooses phones from Armbian, even when Android has no Bluetooth adapter. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "en-w1000dp-h700dp-land")
class ChooseIphoneBluetoothDialogTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private var activity: DiPlayActivity? = null

    @After fun tearDown() {
        activity?.finish()
        ShadowBluetoothAdapter.setIsBluetoothSupported(true)
        AppAppearanceRuntime.resetForTest()
        context.getSharedPreferences("diplay", 0).edit().clear().commit()
        context.getSharedPreferences("xcertplay_airplay", 0).edit().clear().commit()
    }

    private fun show(devices: List<BluetoothDeviceInfo>): AlertDialog {
        val screen = Robolectric.buildActivity(DiPlayActivity::class.java).setup().get().also { activity = it }
        ReflectionHelpers.callInstanceMethod<Unit>(screen, "showPhoneChoices", ClassParameter.from(List::class.java, devices))
        return ShadowAlertDialog.getLatestAlertDialog()
    }

    private fun texts(dialog: AlertDialog): List<String> = descendants(dialog.window!!.decorView)
        .filterIsInstance<TextView>().map { it.text.toString() }.filter { it.isNotEmpty() }.toList()

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }

    @Test fun missingAndroidBluetoothOffersArmbianPairing() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(false)
        val dialog = show(emptyList())
        val shown = texts(dialog)
        assertTrue(shown.toString(), shown.any { "KrDiPlay" in it })
        assertEquals("开启 KrPlay 蓝牙配对", dialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
    }

    @Test fun pairedRelayPhoneIsSelectableWithoutAndroidBluetooth() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(false)
        val dialog = show(listOf(BluetoothDeviceInfo("00:11:22:33:44:55", "Relay iPhone", true)))
        assertEquals("Relay iPhone", dialog.listView.adapter.getItem(0))
        assertEquals("配对新设备到 KrPlay", dialog.getButton(AlertDialog.BUTTON_NEUTRAL).text.toString())
    }

    @Test fun connectedIndicatorComesFromTheRelay() {
        val dialog = show(listOf(
            BluetoothDeviceInfo("00:11:22:33:44:55", "First", true, connected = true),
            BluetoothDeviceInfo("00:11:22:33:44:66", "Second", true, connected = false),
        ))
        assertEquals("First · 已连", dialog.listView.adapter.getItem(0))
        assertEquals("Second", dialog.listView.adapter.getItem(1))
    }
}
