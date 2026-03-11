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

/**
 * NoorVpnService — VPN محلي يعترض طلبات DNS ويحجب النطاقات المحددة
 *
 * كيف يعمل:
 * 1. يُنشئ واجهة VPN افتراضية على الجهاز
 * 2. يعترض حزم UDP على المنفذ 53 (DNS)
 * 3. يفحص كل طلب DNS — إذا كان النطاق محجوباً → يُعيد 0.0.0.0
 * 4. إذا لم يكن محجوباً → يُمرره لسيرفر DNS حقيقي (Cloudflare 1.1.1.1)
 *
 * لا يمر أي حركة بيانات أخرى عبر هذا الـ VPN.
 */
class NoorVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null

    companion object {
        const val ACTION_START = "com.noor.recovery.vpn.START"
        const val ACTION_STOP  = "com.noor.recovery.vpn.STOP"
        const val EXTRA_TYPE   = "vpn_type"

        private const val TAG        = "NoorVPN"
        private const val DNS_PORT   = 53
        private const val DNS_SERVER = "1.1.1.1"   // Cloudflare — سريع وموثوق

        // IP وهمي للـ VPN — لا يُستخدم للاتصال الحقيقي
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE   = "0.0.0.0"
        private const val VPN_PREFIX  = 0
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
        stopVpn() // أوقف أي جلسة سابقة

        try {
            vpnInterface = Builder()
                .setSession("نور — حماية")
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(DNS_SERVER)
                // نوجّه DNS فقط — لا كل الحركة
                .addRoute("$DNS_SERVER", 32)
                .setMtu(1500)
                .establish()

            Log.d(TAG, "VPN started — type: $vpnType")

            serviceJob = CoroutineScope(Dispatchers.IO).launch {
                runDnsProxy(vpnType)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN: ${e.message}")
        }
    }

    private fun stopVpn() {
        serviceJob?.cancel()
        serviceJob = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        Log.d(TAG, "VPN stopped")
    }

    // ── DNS Proxy — القلب الحقيقي للحجب ────────────────────────────
    private suspend fun runDnsProxy(vpnType: VpnType) {
        val fd  = vpnInterface?.fileDescriptor ?: return
        val ins = FileInputStream(fd)
        val out = FileOutputStream(fd)
        val buf = ByteBuffer.allocate(32767)

        while (isActive) {
            buf.clear()
            val len = withContext(Dispatchers.IO) {
                try { ins.read(buf.array()) } catch (_: Exception) { -1 }
            }
            if (len <= 0) continue

            buf.limit(len)

            // نستخرج فقط حزم UDP على المنفذ 53
            val packet = IpPacket(buf.array(), len)
            if (!packet.isUdp || packet.dstPort != DNS_PORT) {
                // ليس DNS — أعده كما هو
                withContext(Dispatchers.IO) {
                    try { out.write(buf.array(), 0, len) } catch (_: Exception) {}
                }
                continue
            }

            // استخرج اسم النطاق من طلب DNS
            val domain = extractDomainFromDns(packet.payload)

            if (domain != null && BlockListCache.isBlocked(domain, vpnType)) {
                // ── محجوب → أرجع NXDOMAIN (0.0.0.0) ──────────────
                Log.d(TAG, "Blocked: $domain")
                val response = buildBlockedResponse(packet.payload)
                val responsePacket = packet.buildResponse(response)
                withContext(Dispatchers.IO) {
                    try { out.write(responsePacket) } catch (_: Exception) {}
                }
            } else {
                // ── غير محجوب → أرسله لـ DNS الحقيقي ──────────────
                val dnsResponse = forwardDnsQuery(packet.payload) ?: continue
                val responsePacket = packet.buildResponse(dnsResponse)
                withContext(Dispatchers.IO) {
                    try { out.write(responsePacket) } catch (_: Exception) {}
                }
            }
        }
    }

    // ── استخراج اسم النطاق من حزمة DNS خام ─────────────────────────
    private fun extractDomainFromDns(payload: ByteArray): String? {
        return try {
            // تخطي 12 بايت header
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

    // ── بناء رد DNS محجوب — يُعيد 0.0.0.0 ──────────────────────────
    private fun buildBlockedResponse(query: ByteArray): ByteArray {
        val response = query.copyOf(query.size + 16)
        // QR=1 (response), RCODE=3 (NXDOMAIN)
        response[2] = (response[2].toInt() or 0x80).toByte()  // QR bit
        response[3] = (response[3].toInt() or 0x03).toByte()  // RCODE NXDOMAIN
        // ANCOUNT = 0
        response[6] = 0; response[7] = 0
        return response
    }

    // ── إرسال الطلب لـ DNS حقيقي واستقبال الرد ──────────────────────
    private suspend fun forwardDnsQuery(query: ByteArray): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                protect(socket)  // مهم — يُخرجه من الـ VPN حتى لا يحدث حلقة
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

// ── مساعد لتحليل حزم IP/UDP ─────────────────────────────────────────
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
        val udpStart = ipHeaderLen + 8  // تخطي UDP header
        return raw.copyOfRange(udpStart, len)
    }

    /** بناء حزمة رد UDP/IP بنفس المصدر والوجهة معكوسين */
    fun buildResponse(dnsResponse: ByteArray): ByteArray {
        val ipHeaderLen = (raw[0].toInt() and 0x0F) * 4
        val totalLen = ipHeaderLen + 8 + dnsResponse.size
        val result = ByteArray(totalLen)

        // IP header
        System.arraycopy(raw, 0, result, 0, ipHeaderLen)
        result[0] = 0x45.toByte()               // IPv4, header=20
        result[2] = (totalLen shr 8).toByte()
        result[3] = (totalLen and 0xFF).toByte()
        // اعكس src/dst
        System.arraycopy(raw, 12, result, 16, 4)
        System.arraycopy(raw, 16, result, 12, 4)

        // UDP header
        result[ipHeaderLen]     = raw[ipHeaderLen + 2]   // src port = dst port القديم
        result[ipHeaderLen + 1] = raw[ipHeaderLen + 3]
        result[ipHeaderLen + 2] = raw[ipHeaderLen]       // dst port = src port القديم
        result[ipHeaderLen + 3] = raw[ipHeaderLen + 1]
        val udpLen = 8 + dnsResponse.size
        result[ipHeaderLen + 4] = (udpLen shr 8).toByte()
        result[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        result[ipHeaderLen + 6] = 0   // checksum (اختياري للـ VPN)
        result[ipHeaderLen + 7] = 0

        // DNS response
        System.arraycopy(dnsResponse, 0, result, ipHeaderLen + 8, dnsResponse.size)

        return result
    }
}
