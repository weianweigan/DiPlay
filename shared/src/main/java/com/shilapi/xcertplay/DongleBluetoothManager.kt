package com.shilapi.xcertplay

import android.net.Network
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URL

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val isDongle: Boolean,
    val connected: Boolean = false,
)

/** REST control plane for the Armbian Bluetooth relay. */
object DongleBluetoothManager {
    private const val TAG = "DongleBluetoothManager"
    @Volatile var activeDongleHost: String = "6.6.6.6"
    private val candidates = listOf("6.6.6.6", "192.168.100.146", "100.96.0.1", "192.168.43.1", "192.168.225.1", "127.0.0.1")

    fun getCandidateHosts(): List<String> = (listOf(activeDongleHost) + candidates).filter { it.isNotBlank() }.distinct()

    private fun request(host: String, path: String, post: Boolean = false, network: Network? = null): Pair<Int, String> {
        val url = URL("http://$host:8088$path")
        val connection = (network?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
        return try {
            connection.connectTimeout = 800
            connection.readTimeout = 1500
            connection.requestMethod = if (post) "POST" else "GET"
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            status to body
        } finally {
            connection.disconnect()
        }
    }

    fun fetchDongleDevices(): List<BluetoothDeviceInfo> {
        for (host in getCandidateHosts()) {
            try {
                val (status, body) = request(host, "/api/bluetooth/devices")
                if (status != 200) continue
                val array = JSONArray(body)
                val result = (0 until array.length()).map { i ->
                    val item = array.getJSONObject(i)
                    BluetoothDeviceInfo(item.getString("address"), item.optString("name", "iPhone"), true, item.optBoolean("connected"))
                }
                activeDongleHost = host
                return result
            } catch (_: Exception) { }
        }
        return emptyList()
    }

    private fun post(path: String): Boolean {
        for (host in getCandidateHosts()) {
            try {
                val (status, body) = request(host, path, post = true)
                activeDongleHost = host
                val result = JSONObject(body)
                // HTTP 202 means accepted, not RFCOMM connected or iAP2 authenticated.
                val accepted = status in 200..299 && result.optBoolean("success", false)
                Log.i(TAG, "Relay request accepted=$accepted state=${result.optString("state")} host=$host")
                return accepted // A reachable relay's business failure must not trigger duplicate requests on other NICs.
            } catch (_: Exception) { }
        }
        return false
    }

    fun triggerDongleConnect(address: String): Boolean {
        require(address.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) { "Invalid Bluetooth address" }
        return post("/api/bluetooth/connect?address=$address")
    }

    fun enableDonglePairable(): Boolean = post("/api/bluetooth/pair")

    /** Redacted Android SSIDs must not combine saved AP credentials with another LAN's IP. */
    fun validateWifiNetwork(network: Network, localAddress: InetAddress, ssid: String) {
        for (host in getCandidateHosts()) {
            val response = try { request(host, "/api/wifi", network = network) } catch (_: Exception) { continue }
            if (response.first != 200) continue
            val info = JSONObject(response.second)
            if (info.optString("ssid") != ssid) throw IOException("Armbian hotspot does not match the saved Wi-Fi name")
            val addresses = info.optJSONArray("addresses") ?: throw IOException("Armbian hotspot has no IPv4 address")
            val matches = (0 until addresses.length()).any { matchesSubnet(localAddress, addresses.getString(it)) }
            if (!matches) throw IOException("Connect Android to $ssid before starting CarPlay; current Wi-Fi is a different network")
            activeDongleHost = host
            return
        }
        throw IOException("Cannot verify the CarPlay Wi-Fi network; connect to $ssid and check the Armbian relay")
    }

    internal fun matchesSubnet(local: InetAddress, cidr: String): Boolean {
        val parts = cidr.split('/')
        if (parts.size != 2 || local !is Inet4Address) return false
        val prefix = parts[1].toIntOrNull()?.takeIf { it in 1..32 } ?: return false
        val host = parts[0].takeIf { it.matches(Regex("[0-9.]+")) } ?: return false
        val address = runCatching { InetAddress.getByName(host) as? Inet4Address }.getOrNull() ?: return false
        val mask = -1 shl (32 - prefix)
        fun number(bytes: ByteArray): Int = bytes.fold(0) { result, byte -> (result shl 8) or (byte.toInt() and 255) }
        return (number(local.address) and mask) == (number(address.address) and mask)
    }
}
