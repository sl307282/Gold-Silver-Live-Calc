package com.goldsilver.livecalc.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * OtaUpdateManager — handles downloading and installing APK updates from GitHub Releases.
 *
 * Flow:
 *  1. ViewModel checks Firebase Remote Config for apk_download_url + latest_version_code
 *  2. If newer, user sees update dialog
 *  3. User taps "Download & Install"
 *  4. OtaUpdateManager downloads APK to external cache dir, reporting Float progress (0f..1f)
 *  5. On completion, triggers system installer via FileProvider URI
 */
class OtaUpdateManager(private val context: Context) {

    companion object {
        private const val APK_FILE_NAME = "gold_silver_update.apk"
        private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".ota.fileprovider"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // Large file download needs more time
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Downloads the APK from the given URL, emitting progress via [onProgress] (0f..1f).
     * Returns [OtaResult] indicating success or failure.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): OtaResult = withContext(Dispatchers.IO) {
        try {
            val apkFile = getApkFile()
            // Clean up any previous partial download
            if (apkFile.exists()) apkFile.delete()

            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext OtaResult.Error(
                        "Download failed: HTTP ${response.code} — ${response.message}"
                    )
                }

                val body = response.body
                    ?: return@withContext OtaResult.Error("Empty response body from server")

                val contentLength = body.contentLength()

                FileOutputStream(apkFile).use { outputStream ->
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (contentLength > 0) {
                                withContext(Dispatchers.Main) {
                                    onProgress(totalBytesRead.toFloat() / contentLength.toFloat())
                                }
                            }
                        }
                        outputStream.flush()
                    }
                }

                withContext(Dispatchers.Main) { onProgress(1f) }
                OtaResult.Success(apkFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            OtaResult.Error(e.localizedMessage ?: "Unknown download error")
        }
    }

    /**
     * Opens Android's system package installer with the downloaded APK.
     * Handles the Unknown Sources permission flow on Android 8+.
     */
    fun installApk(context: Context) {
        val apkFile = getApkFile()
        if (!apkFile.exists()) return

        // Android 8+: check if we can request package installs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                // Open the settings page to grant unknown sources for this app
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        val authority = "${context.packageName}$FILE_PROVIDER_AUTHORITY_SUFFIX"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    /** Returns true if a previously downloaded APK is ready to install. */
    fun isApkReady(): Boolean = getApkFile().exists() && getApkFile().length() > 0

    /** Deletes any cached APK file (call after successful install or on cancel). */
    fun cleanup() {
        getApkFile().takeIf { it.exists() }?.delete()
    }

    private fun getApkFile(): File {
        val dir = context.externalCacheDir ?: context.cacheDir
        return File(dir, APK_FILE_NAME)
    }
}

/** Sealed result type for OTA download operations. */
sealed class OtaResult {
    data class Success(val file: File) : OtaResult()
    data class Error(val message: String) : OtaResult()
}

/** Represents the current OTA update lifecycle state. */
enum class OtaState {
    IDLE,           // No update in progress
    DOWNLOADING,    // APK download in progress
    READY,          // APK downloaded, ready to install
    ERROR           // Download failed
}
