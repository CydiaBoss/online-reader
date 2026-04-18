package com.wang.twkanviewer

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import androidx.core.net.toUri

class UpdateManager(private val context: Context) {

    private val gitHubService: GitHubService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubService::class.java)
    }

    suspend fun checkForUpdates(
        owner: String,
        repo: String,
        currentVersion: String
    ): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val latestRelease = gitHubService.getLatestRelease(owner, repo)
            if (isNewerVersion(latestRelease.tag_name, currentVersion)) {
                latestRelease
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to check for updates", e)
            null
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestClean = latest.removePrefix("v").split(".")
        val currentClean = current.removePrefix("v").split(".")

        val length = maxOf(latestClean.size, currentClean.size)
        for (i in 0 until length) {
            val v1 = latestClean.getOrNull(i)?.toIntOrNull() ?: 0
            val v2 = currentClean.getOrNull(i)?.toIntOrNull() ?: 0
            if (v1 > v2) return true
            if (v1 < v2) return false
        }
        return false
    }

    fun downloadAndInstall(release: GitHubRelease) {
        val asset = release.assets.find { it.name.endsWith(".apk") } ?: return
        val url = asset.browser_download_url

        val request = DownloadManager.Request(url.toUri())
            .setTitle("Downloading TWKANViewer Update")
            .setDescription("New version: ${release.tag_name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    installApk()
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                context,
                onComplete,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun installApk() {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}