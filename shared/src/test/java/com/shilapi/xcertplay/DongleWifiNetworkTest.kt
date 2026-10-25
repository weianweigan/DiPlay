package com.shilapi.xcertplay

import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class DongleWifiNetworkTest {
    @Test fun rejectsHomeLanEvenWhenRelayIsReachableThroughEthernet() {
        assertFalse(DongleBluetoothManager.matchesSubnet(InetAddress.getByName("192.168.100.207"), "6.6.6.6/29"))
    }
    @Test fun acceptsApClientAndChecksFullPrefix() {
        assertTrue(DongleBluetoothManager.matchesSubnet(InetAddress.getByName("6.6.6.3"), "6.6.6.6/29"))
        assertFalse(DongleBluetoothManager.matchesSubnet(InetAddress.getByName("6.6.6.8"), "6.6.6.6/29"))
        assertFalse(DongleBluetoothManager.matchesSubnet(InetAddress.getByName("6.6.6.3"), "6.6.6.6/33"))
        assertFalse(DongleBluetoothManager.matchesSubnet(InetAddress.getByName("6.6.6.3"), "invalid"))
    }
}
