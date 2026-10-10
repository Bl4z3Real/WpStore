package com.metrostore.app.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApkMeta(val label: String, val packageName: String, val versionName: String, val icon: Drawable?)

sealed interface InstallResult {
    data object Started : InstallResult
    data object NeedsPermission : InstallResult
    data class Error(val message: String) : InstallResult
}

/** Uses only the official Android APIs: package parsing + the system package installer. */
object ApkInstaller {

    fun canInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    private fun permissionIntent(ctx: Context) =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun openPermissionSettings(ctx: Context): Boolean = try {
        ctx.startActivity(permissionIntent(ctx)); true
    } catch (e: ActivityNotFoundException) { false }

    /** Reads the APK manifest. Returns null when the file is not a valid APK. */
    @Suppress("DEPRECATION")
    fun inspect(ctx: Context, file: File): ApkMeta? {
        return try {
            val pm = ctx.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return null
            val ai = info.applicationInfo ?: return null
            ai.sourceDir = file.absolutePath
            ai.publicSourceDir = file.absolutePath
            ApkMeta(
                label = ai.loadLabel(pm).toString(),
                packageName = info.packageName,
                versionName = info.versionName ?: "",
                icon = runCatching { ai.loadIcon(pm) }.getOrNull()
            )
        } catch (e: Exception) { null }
    }

    fun install(ctx: Context, file: File): InstallResult {
        if (!file.exists()) return InstallResult.Error("the apk file no longer exists")
        if (inspect(ctx, file) == null) return InstallResult.Error("this file is not a valid apk")
        if (!canInstall(ctx)) return InstallResult.NeedsPermission
        return try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            InstallResult.Started
        } catch (e: ActivityNotFoundException) {
            InstallResult.Error("no system installer available on this device")
        } catch (e: Exception) {
            InstallResult.Error("installation failed: ${e.message ?: "unknown error"}")
        }
    }
}

object Downloader {
    @Throws(IOException::class)
    fun download(url: String, dest: File, onProgress: (Float) -> Unit) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        try {
            if (conn.responseCode !in 200..299) throw IOException("server returned ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(32 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
