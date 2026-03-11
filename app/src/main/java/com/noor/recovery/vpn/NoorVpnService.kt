package com.noor.recovery.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class NoorVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_START = "com.noor.recovery.vpn.START"
        const val ACTION_STOP  = "com.noor.recovery.vpn.STOP"
        const val EXTRA_TYPE   = "vpn_type"

        private const val TAG        = "NoorVPN"
        private const val DNS_PORT   = 53
        private const val DNS_SERVER = "1.1.1.1"
        private const val VPN_ADDRESS = "10.0.0.2"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: "BOTH"
                val vpnType = VpnType.valueOf(typeStr)
                startVpn(vpnType)
            }
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(vpnType: VpnType) {
        stopVpn()
        try {
            vpnInterface = Builder()
                .setSession("نور — حماية")
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(DNS_SERVER)
                .addRoute(DNS_SERVER, 32)
                .setMtu(1500)
                .establish()

            Log.d(TAG, "VPN started — type: $vpnType")
            serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            serviceScope.launch { runDnsProxy(vpnType) }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN: ${e.message}")
        }
    }

    private fun stopVpn() {
        serviceScope.cancel()
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        Log.d(TAG, "VPN stopped")
    }

    private suspend fun runDnsProxy(vpnType: VpnType) {
        val fd  = vpnInterface?.fileDescriptor ?: return
        val ins = FileInputStream(fd)
        val out = FileOutputStream(fd)
        val buf = ByteBuffer.allocate(32767)

        while (serviceScope.isActive) {
            buf.clear()
            val len = withContext(Dispatchers.IO) {
                try { ins.read(buf.array()) } catch (_: Exception) { -1 }
            }
            if (len <= 0) continue

            buf.limit(len)
            val packet = IpPacket(buf.array(), len)

            if (!packet.isUdp || packet.dstPort != DNS_PORT) {
                withContext(Dispatchers.IO) {
                    try { out.write(buf.array(), 0, len) } catch (_: Exception) {}
                }
                continue
            }

            val domain = extractDomainFromDns(packet.payload)

            if (domain != null && BlockListCache.isBlocked(domain, vpnType)) {
                Log.d(TAG, "Blocked: $domain")
                val response = buildBlockedResponse(packet.payload)
                val responsePacket = packet.buildResponse(response)
                withContext(Dispatchers.IO) {
                    try { out.write(responsePacket) } catch (_: Exception) {}
                }
            } else {
                val dnsResponse = forwardDnsQuery(packet.payload) ?: continue
                val responsePacket = packet.buildResponse(dnsResponse)
                withContext(Dispatchers.IO) {
                    try { out.write(responsePacket) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun extractDomainFromDns(payload: ByteArray): String? {
        return try {
            var i = 12
            val sb = StringBuilder()
            while (i < payload.size) {
                val len = payload[i].toInt() and 0xFF
                if (len == 0) break
                if (sb.isNotEmpty()) sb.append('.')
                i++
                repeat(len) {
                    if (i < payload.size) sb.append(payload[i++].toChar())
                }
            }
            if (sb.isEmpty()) null else sb.toString().lowercase()
        } catch (_: Exception) { null }
    }

    private fun buildBlockedResponse(query: ByteArray): ByteArray {
        val response = query.copyOf()
        response[2] = (response[2].toInt() or 0x80).toByte()
        response[3] = (response[3].toInt() or 0x03).toByte()
        response[6] = 0; response[7] = 0
        return response
    }

    private suspend fun forwardDnsQuery(query: ByteArray): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                protect(socket)
                socket.soTimeout = 3000
                val address = InetAddress.getByName(DNS_SERVER)
                socket.send(DatagramPacket(query, query.size, address, DNS_PORT))
                val buf = ByteArray(4096)
                val response = DatagramPacket(buf, buf.size)
                socket.receive(response)
                socket.close()
                buf.copyOf(response.length)
            } catch (_: Exception) { null }
        }
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}

private class IpPacket(private val raw: ByteArray, private val len: Int) {

    val isUdp: Boolean get() = len > 20 && raw[9].toInt() == 17

    val dstPort: Int get() {
        if (!isUdp) return -1
        val ipHeaderLen = (raw[0].toInt() and 0x0F) * 4
        return ((raw[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
               (raw[ipHeaderLen + 3].toInt() and 0xFF)
    }

    val payload: ByteArray get() {
        val ipHeaderLen = (raw[0].toInt() and 0x0F) * 4
        return raw.copyOfRange(ipHeaderLen + 8, len)
    }

    fun buildResponse(dnsResponse: ByteArray): ByteArray {
        val ipHeaderLen = (raw[0].toInt() and 0x0F) * 4
        val totalLen = ipHeaderLen + 8 + dnsResponse.size
        val result = ByteArray(totalLen)

        System.arraycopy(raw, 0, result, 0, ipHeaderLen)
        result[0] = 0x45.toByte()
        result[2] = (totalLen shr 8).toByte()
        result[3] = (totalLen and 0xFF).toByte()
        System.arraycopy(raw, 12, result, 16, 4)
        System.arraycopy(raw, 16, result, 12, 4)

        result[ipHeaderLen]     = raw[ipHeaderLen + 2]
        result[ipHeaderLen + 1] = raw[ipHeaderLen + 3]
        result[ipHeaderLen + 2] = raw[ipHeaderLen]
        result[ipHeaderLen + 3] = raw[ipHeaderLen + 1]
        val udpLen = 8 + dnsResponse.size
        result[ipHeaderLen + 4] = (udpLen shr 8).toByte()
        result[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        result[ipHeaderLen + 6] = 0
        result[ipHeaderLen + 7] = 0

        System.arraycopy(dnsResponse, 0, result, ipHeaderLen + 8, dnsResponse.size)
        return result
    }
}
