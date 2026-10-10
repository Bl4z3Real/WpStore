package com.metrostore.app

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metrostore.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String,
    val catalogId: String?,
    val icon: ImageBitmap?
)

class StoreViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val store = LocalStore(ctx)

    var catalog by mutableStateOf<List<StoreApp>>(emptyList())
        private set
    val installedApps = mutableStateListOf<InstalledApp>()
    val downloads = mutableStateListOf<File>()
    private val tracked = mutableListOf<TrackedApp>()
    val progress = mutableStateMapOf<String, Float>()
    var accent by mutableStateOf(Color(store.accent))
        private set
    var message by mutableStateOf<String?>(null)

    val categories: List<String> get() = catalog.map { it.category }.distinct()

    init {
        catalog = try {
            CatalogRepository.load(ctx)
        } catch (e: Exception) {
            message = "could not read the catalog: ${e.message}"
            emptyList()
        }
        tracked.addAll(store.loadTracked())
        refreshDownloads()
        refreshInstalled()
    }

    // ---- My apps: only apps really installed on the device ------------------------------------

    /** Catalog apps (Play or yours) that are installed + apps installed through this store. */
    @Suppress("DEPRECATION")
    fun refreshInstalled() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = ctx.packageManager
            val result = mutableListOf<InstalledApp>()
            val seen = HashSet<String>()
            fun add(pkg: String, fallback: String, catalogId: String?) {
                if (!seen.add(pkg)) return
                val info = try { pm.getPackageInfo(pkg, 0) } catch (e: PackageManager.NameNotFoundException) { return }
                val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                    .getOrDefault(fallback)
                val icon = runCatching { pm.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap() }.getOrNull()
                result.add(InstalledApp(pkg, label, info.versionName ?: "", catalogId, icon))
            }
            catalog.forEach { a -> a.packageName?.let { add(it, a.name, a.id) } }
            tracked.toList().forEach { add(it.packageName, it.label, it.catalogId) }
            val existing = result.map { it.packageName }.toSet()
            val before = tracked.size
            tracked.retainAll { it.packageName in existing }   // install cancelled or app removed
            if (tracked.size != before) store.saveTracked(tracked)
            withContext(Dispatchers.Main) {
                installedApps.clear()
                installedApps.addAll(result.sortedBy { it.label.lowercase() })
            }
        }
    }

    private fun track(meta: ApkMeta, catalogId: String?) {
        tracked.removeAll { it.packageName == meta.packageName }
        tracked.add(TrackedApp(meta.packageName, meta.label, catalogId))
        store.saveTracked(tracked)
    }

    fun openApp(context: Context, pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) { message = "this app cannot be opened"; return }
        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { message = "this app cannot be opened" }
    }

    fun uninstall(context: Context, pkg: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
        } catch (e: ActivityNotFoundException) { message = "could not start the uninstaller" }
    }

    fun setAccent(c: Color) { accent = c; store.accent = c.toArgb() }

    // ---- APK from the phone / install ---------------------------------------------------------

    /** Picks an APK from the device: validates it and hands it to the system installer. */
    fun importApk(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(store.downloadDir, "import_${System.currentTimeMillis()}.apk")
            try {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                } ?: throw IOException("cannot open the selected file")
                val meta = ApkInstaller.inspect(ctx, file)
                if (meta == null) {
                    file.delete()
                    message = "this file is not a valid apk"
                } else {
                    withContext(Dispatchers.Main) { refreshDownloads(); install(context, file) { track(meta, null) } }
                }
            } catch (e: Exception) {
                file.delete()
                message = "could not read the file: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun install(context: Context, file: File, onStarted: () -> Unit = {}) {
        when (val r = ApkInstaller.install(context, file)) {
            InstallResult.Started -> onStarted()
            InstallResult.NeedsPermission -> {
                message = "allow installs from this app, then tap install again"
                if (!ApkInstaller.openPermissionSettings(context)) {
                    message = "open Android settings and allow installs from this app"
                }
            }
            is InstallResult.Error -> message = r.message
        }
    }

    // ---- Authorised HTTPS APK sources (catalog "apkUrl") ----------------------------------

    /** Installs one of your own apps: from assets/apks/ (apkAsset) or from an https link (apkUrl). */
    fun installCatalogApp(context: Context, app: StoreApp) {
        val asset = app.apkAsset
        val url = app.apkUrl
        if (asset == null && url == null) return
        if (asset == null && !url!!.startsWith("https://")) { message = "only https apk sources are allowed"; return }
        if (progress.containsKey(app.id)) return
        viewModelScope.launch(Dispatchers.IO) {
            val dest = File(store.downloadDir, app.id.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".apk")
            progress[app.id] = 0f
            try {
                if (asset != null) {
                    try {
                        ctx.assets.open("apks/$asset").use { input -> dest.outputStream().use { input.copyTo(it) } }
                    } catch (e: java.io.FileNotFoundException) {
                        throw IOException("apk not found in assets/apks/: $asset")
                    }
                } else {
                    Downloader.download(url!!, dest) { progress[app.id] = it }
                }
                if (ApkInstaller.inspect(ctx, dest) == null) {
                    dest.delete()
                    message = "the downloaded file is not a valid apk"
                } else {
                    val meta = ApkInstaller.inspect(ctx, dest)!!
                    withContext(Dispatchers.Main) { refreshDownloads(); install(context, dest) { track(meta, app.id) } }
                }
            } catch (e: Exception) {
                dest.delete()
                message = "download failed: ${e.message ?: "unknown error"}"
            } finally {
                progress.remove(app.id)
            }
        }
    }

    fun refreshDownloads() {
        downloads.clear()
        downloads.addAll(
            store.downloadDir.listFiles()?.filter { it.extension == "apk" }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        )
    }

    fun deleteDownload(file: File) { file.delete(); refreshDownloads() }

    // ---- Play Store (official links only) -------------------------------------------------

    fun openPlay(context: Context, app: StoreApp) {
        val pkg = app.packageName ?: return
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).setPackage("com.android.vending")
            )
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                )
            } catch (e2: ActivityNotFoundException) {
                message = "no app can open the Play Store link"
            }
        }
    }

    fun share(context: Context, app: StoreApp) {
        val text = app.packageName?.let { "${app.name} https://play.google.com/store/apps/details?id=$it" } ?: app.name
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        try { context.startActivity(Intent.createChooser(send, app.name)) }
        catch (e: ActivityNotFoundException) { message = "nothing to share with" }
    }
}
