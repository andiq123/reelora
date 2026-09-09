package tv.reelora.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

internal data class UpdateStatus(val label: String = "Check for updates", val busy: Boolean = false, val message: String = "")
internal data class AppRelease(val version: String, val url: String, val sha256: String, val size: Long)

internal fun versionParts(version: String): List<Int>? =
    version.removePrefix("v").takeIf { it.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")) }
        ?.split('.')?.map { it.toIntOrNull() ?: return null }

internal fun newerVersion(candidate: String, installed: String): Boolean {
    val next = versionParts(candidate) ?: return false
    val current = versionParts(installed) ?: return false
    return next.zip(current).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a > b } ?: false
}

internal fun parseAppRelease(json: JSONObject): AppRelease {
    check(!json.optBoolean("draft") && !json.optBoolean("prerelease"))
    val version = json.getString("name").removePrefix("Reelora TV ").trim()
    check(versionParts(version) != null) { "Invalid release version" }
    val assets = json.getJSONArray("assets")
    val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
        .single { it.getString("name") == "ReeloraTV-latest.apk" }
    return validateReleaseAsset(version, apk.getString("browser_download_url"), apk.getString("digest"), apk.getLong("size"))
}

internal fun validateReleaseAsset(version: String, url: String, digest: String, size: Long): AppRelease {
    check(versionParts(version) != null)
    check(url == "https://github.com/andiq123/reelora/releases/download/latest/ReeloraTV-latest.apk")
    check(digest.matches(Regex("sha256:[a-fA-F0-9]{64}"))) { "Missing release checksum" }
    check(size in 1..50L * 1024 * 1024) { "Invalid APK size" }
    return AppRelease(version, url, digest.removePrefix("sha256:"), size)
}

// Android owns the download and retries. No service, polling loop, or background update checks.
internal class AppUpdater(private val activity: ComponentActivity) {
    val status = MutableStateFlow(UpdateStatus())
    private val scope = MainScope()
    private val preferences = activity.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val downloads by lazy { activity.getSystemService(DownloadManager::class.java) }
    private val apk get() = File(checkNotNull(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)), "updates/latest.apk")
    private var working = false
    private var refreshPending = false
    private var foreground = false
    private var awaitingPermission = false
    private val permission = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        awaitingPermission = false
        if (activity.packageManager.canRequestPackageInstalls()) check()
        else status.value = UpdateStatus("Install update", message = "Allow installation to continue")
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == preferences.getLong("download", -2)) refresh()
        }
    }

    init {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(activity, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    fun resume() {
        foreground = true
        if (!awaitingPermission && preferences.contains("download")) refresh()
    }

    fun pause() { foreground = false }

    fun check() {
        if (working) return
        if (preferences.contains("download")) {
            preferences.edit().putBoolean("install", true).apply()
            refresh()
            return
        }
        working = true
        status.value = UpdateStatus("Checking…", busy = true)
        scope.launch {
            try {
                val release = parseAppRelease(readJson("https://api.github.com/repos/andiq123/reelora/releases/latest"))
                if (!newerVersion(release.version, BuildConfig.VERSION_NAME)) {
                    status.value = UpdateStatus(message = "You're up to date")
                } else {
                    withContext(Dispatchers.IO) {
                        apk.parentFile?.mkdirs()
                        apk.delete()
                        val downloadId = downloads.enqueue(DownloadManager.Request(Uri.parse(release.url))
                            .setTitle("Reelora TV ${release.version}")
                            .setMimeType("application/vnd.android.package-archive")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "updates/latest.apk"))
                        preferences.edit().putLong("download", downloadId).putString("sha256", release.sha256)
                            .putLong("size", release.size).putBoolean("install", true).commit()
                    }
                    status.value = UpdateStatus("Downloading…", busy = true)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status.value = UpdateStatus(message = "Update check failed · try again")
            } finally {
                working = false
            }
            if (preferences.contains("download")) refresh()
        }
    }

    private fun refresh() {
        if (working) {
            refreshPending = true
            return
        }
        refreshPending = false
        working = true
        scope.launch {
            try {
                val id = preferences.getLong("download", -1)
                val state = withContext(Dispatchers.IO) {
                    downloads.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                        check(cursor.moveToFirst()) { "Download missing" }
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    }
                }
                when (state) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val newer = withContext(Dispatchers.IO) { validateApk() }
                        if (!newer) {
                            withContext(Dispatchers.IO) { downloads.remove(id); apk.delete() }
                            preferences.edit().clear().apply()
                            status.value = UpdateStatus(message = "You're up to date")
                        } else {
                            status.value = UpdateStatus("Install update")
                            if (foreground && preferences.getBoolean("install", false)) install()
                        }
                    }
                    DownloadManager.STATUS_FAILED -> error("Download failed")
                    else -> status.value = UpdateStatus("Downloading…", busy = true)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                withContext(Dispatchers.IO) { runCatching { downloads.remove(preferences.getLong("download", -1)); apk.delete() } }
                preferences.edit().clear().apply()
                status.value = UpdateStatus(message = "Update could not be verified · try again")
            } finally {
                working = false
                if (refreshPending) refresh()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun validateApk(): Boolean {
        check(apk.length() == preferences.getLong("size", -1))
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual.equals(preferences.getString("sha256", null), ignoreCase = true))
        val manager = activity.packageManager
        val archive = checkNotNull(manager.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES))
        check(archive.packageName == activity.packageName)
        val installed = manager.getPackageInfo(activity.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        if (archive.longVersionCode <= installed.longVersionCode) return false
        val signers = checkNotNull(archive.signingInfo).apkContentsSigners
        check(signers.isNotEmpty() && signers.all {
            manager.hasSigningCertificate(activity.packageName, it.toByteArray(), PackageManager.CERT_INPUT_RAW_X509)
        }) { "Release uses a different signing key" }
        return true
    }

    private fun install() {
        preferences.edit().putBoolean("install", false).apply()
        if (!activity.packageManager.canRequestPackageInstalls()) {
            awaitingPermission = true
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
            runCatching { permission.launch(settings) }
                .getOrElse { permission.launch(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
        } else {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updates", apk)
            activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }
    }

    fun close() {
        activity.unregisterReceiver(receiver)
        scope.cancel()
    }
}
