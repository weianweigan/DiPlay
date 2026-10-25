package com.shilapi.xcertplay.transport

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import org.junit.Assert.*
import org.junit.Test

class SocketDuplexStreamTest {
    @Test fun preservesReadOwnershipLimitsTimeoutAndEof() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            Socket(server.inetAddress, server.localPort).use { peer ->
                val stream = SocketDuplexStream(server.accept())
                try {
                    assertNull(stream.recv(64, 5))
                    peer.getOutputStream().write(byteArrayOf(1, 2, 3))
                    val first = stream.recv(2, 2000)!!
                    assertArrayEquals(byteArrayOf(1, 2), first)
                    assertArrayEquals(byteArrayOf(3), stream.recv(2, 2000))
                    assertArrayEquals(byteArrayOf(1, 2), first)
                    stream.send(byteArrayOf(4))
                    assertEquals(4, peer.getInputStream().read())
                    peer.shutdownOutput()
                    assertArrayEquals(byteArrayOf(), stream.recv(2, 2000))
                } finally { stream.close() }
            }
        }
    }
}
