package com.shilapi.xcertplay.transport

import java.net.Socket
import java.net.SocketTimeoutException

/**
 * KrDiPlay 通用网络双工字节流
 * 
 * 严格实现 DiPlay 核心 [BlockingDuplexByteStream] 契约：
 * 将发往/来自 Armbian Linux 随身 WiFi 棒子的 TCP 长连接（如 RFCOMM 8901、USB CarKit）
 * 透明桥接给 DiPlay 内部的 iAP2 / CarPlay 会话状态机。
 */
class SocketDuplexStream(
    private val socket: Socket
) : BlockingDuplexByteStream {

    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()
    private var readTimeout = -1
    private var buffer = ByteArray(0)

    override fun send(data: ByteArray) {
        output.write(data)
    }

    override fun recv(maxBytes: Int, timeoutMillis: Long): ByteArray? {
        require(maxBytes > 0)
        val timeout = timeoutMillis.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
        if (readTimeout != timeout) {
            socket.soTimeout = timeout
            readTimeout = timeout
        }
        if (buffer.size < maxBytes) buffer = ByteArray(maxBytes)
        return try {
            val n = input.read(buffer, 0, maxBytes)
            if (n < 0) ByteArray(0) else buffer.copyOf(n)
        } catch (_: SocketTimeoutException) {
            null
        }
    }

    override fun close() {
        runCatching { socket.close() }
    }
}
