package com.shilapi.xcertplay.orchestration

import android.content.Context
import android.net.Network
import com.shilapi.xcertplay.network.WirelessHotspotInfo
import com.shilapi.xcertplay.transport.BlockingDuplexByteStream
import java.net.InetAddress

/** Installed by the application before a controller is created. Null preserves native Bluetooth. */
object WirelessIntegration {
    @Volatile var provider: WirelessTransportProvider? = null
}

interface WirelessTransportProvider {
    fun validateNetworkIdentity(network: Network, address: InetAddress, ssid: String)
    fun prepare(
        context: Context,
        hotspot: WirelessHotspotInfo,
        phoneAddress: String?,
        isCancelled: () -> Boolean,
        diagnostic: (String) -> Unit,
    ): WirelessBootstrapConnection
}

interface WirelessBootstrapConnection {
    val hostBluetoothMac: String
    /** The caller registers ownership before requesting the peer's Bluetooth connection. */
    fun openStream(): BlockingDuplexByteStream
    fun onStreamRegistered()
}
