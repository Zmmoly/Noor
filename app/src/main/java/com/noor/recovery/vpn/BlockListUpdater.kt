package com.noor.recovery.vpn

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * BlockListUpdater — يحمّل قوائم cbuijs/hagezi ويخزنها محلياً
 *
 * المصادر:
 *   إباحية : https://raw.githubusercontent.com/cbuijs/hagezi/main/lists/nsfw/domains
 *   قمار   : https://raw.githubusercontent.com/cbuijs/hagezi/main/lists/gambling-medium/domains
 *
 * يُحدِّث القوائم أسبوعياً تلقائياً عند فتح التطبيق.
 * إذا فشل التحميل — يستمر باستخدام النسخة المحلية السابقة.
 */
object BlockListUpdater {

    private const val TAG = "BlockListUpdater"

    private const val URL_PORN     =
        "https://raw.githubusercontent.com/cbuijs/hagezi/main/lists/nsfw/domains"
    private const val URL_GAMBLING =
        "https://raw.githubusercontent.com/cbuijs/hagezi/main/lists/gambling-medium/domains"

    private const val FILE_PORN     = "blocklist_porn.txt"
    private const val FILE_GAMBLING = "blocklist_gambling.txt"
    private const val PREFS         = "noor_blocklist_prefs"
    private const val KEY_LAST_UPDATE = "last_update_ms"

    private const val UPDATE_INTERVAL_MS = 7 * 24 * 60 * 60 * 1000L  // أسبوع

    // ── تحقق وحدّث إذا مضى أسبوع ────────────────────────────────────
    suspend fun updateIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0L)
        val now = System.currentTimeMillis()

        val pornFile     = File(context.filesDir, FILE_PORN)
        val gamblingFile = File(context.filesDir, FILE_GAMBLING)

        // حدّث إذا مضى أسبوع أو الملفات غير موجودة
        val needsUpdate = (now - lastUpdate > UPDATE_INTERVAL_MS)
                || !pornFile.exists()
                || !gamblingFile.exists()

        if (!needsUpdate) {
            Log.d(TAG, "القوائم حديثة — لا حاجة للتحديث")
            return
        }

        Log.d(TAG, "جاري تحديث القوائم...")
        var success = false

        if (downloadTo(URL_PORN, pornFile)) {
            Log.d(TAG, "إباحية: ${countLines(pornFile)} نطاق")
            success = true
        }
        if (downloadTo(URL_GAMBLING, gamblingFile)) {
            Log.d(TAG, "قمار: ${countLines(gamblingFile)} نطاق")
            success = true
        }

        if (success) {
            prefs.edit().putLong(KEY_LAST_UPDATE, now).apply()
            // أعد تحميل القوائم في الذاكرة
            BlockListCache.reload(context)
        }
    }

    // ── تحديث يدوي — المستخدم يضغط زر التحديث ───────────────────────
    suspend fun forceUpdate(context: Context): UpdateResult {
        val pornFile     = File(context.filesDir, FILE_PORN)
        val gamblingFile = File(context.filesDir, FILE_GAMBLING)

        val pornOk     = downloadTo(URL_PORN, pornFile)
        val gamblingOk = downloadTo(URL_GAMBLING, gamblingFile)

        return if (pornOk || gamblingOk) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
            BlockListCache.reload(context)
            UpdateResult(
                success   = true,
                pornCount = if (pornOk) countLines(pornFile) else 0,
                gamblingCount = if (gamblingOk) countLines(gamblingFile) else 0
            )
        } else {
            UpdateResult(success = false)
        }
    }

    // ── تحميل ملف من URL إلى ملف محلي ───────────────────────────────
    private suspend fun downloadTo(url: String, dest: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout    = 60_000
                conn.setRequestProperty("User-Agent", "NoorApp/1.0")
                conn.connect()

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "فشل التحميل: ${conn.responseCode} — $url")
                    return@withContext false
                }

                // اكتب لملف مؤقت أولاً — ثم انقله
                val temp = File(dest.parent, "${dest.name}.tmp")
                conn.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                temp.renameTo(dest)
                conn.disconnect()
                true
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في التحميل: ${e.message}")
                false
            }
        }
    }

    private fun countLines(file: File): Int =
        if (file.exists()) file.bufferedReader().lines().filter {
            it.isNotBlank() && !it.startsWith("#")
        }.count().toInt() else 0

    fun getLastUpdateTime(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_UPDATE, 0L)
}

data class UpdateResult(
    val success: Boolean,
    val pornCount: Int = 0,
    val gamblingCount: Int = 0
)

// ── ذاكرة تخزين مؤقت — تُحمَّل مرة واحدة في الذاكرة ──────────────
object BlockListCache {

    private var pornDomains:     Set<String> = emptySet()
    private var gamblingDomains: Set<String> = emptySet()
    private var loaded = false

    fun isBlocked(domain: String, type: VpnType): Boolean {
        val lower = domain.lowercase().trimEnd('.')
        val list = when (type) {
            VpnType.PORN     -> pornDomains
            VpnType.GAMBLING -> gamblingDomains
            VpnType.BOTH     -> pornDomains + gamblingDomains
        }
        return list.any { blocked ->
            lower == blocked || lower.endsWith(".$blocked")
        }
    }

    fun reload(context: Context) {
        pornDomains     = loadFile(context, "blocklist_porn.txt")
        gamblingDomains = loadFile(context, "blocklist_gambling.txt")
        loaded = true
        Log.d("BlockListCache",
            "محمّل — إباحية: ${pornDomains.size} — قمار: ${gamblingDomains.size}")
    }

    fun isLoaded() = loaded

    private fun loadFile(context: Context, name: String): Set<String> {
        val file = File(context.filesDir, name)
        if (!file.exists()) return emptySet()
        return file.bufferedReader()
            .lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.trim().lowercase() }
            .toHashSet()
    }
}
