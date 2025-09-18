package org.love2d.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import org.love2d.android.executable.BuildConfig

class CompyActivity : GameActivity() {
  var isPlayer: Boolean = false
  private enum class Flavor {
    IDE,
    PLAYER,
    HARMONY
  }

  protected override fun getArguments(): Array<String> {
    return when (flavor) {
      Flavor.PLAYER  -> arrayOf("compy", "play", projectName)
      Flavor.HARMONY -> arrayOf("compy", "harmony")
      else           -> arrayOf("compy")
    }
  }

  private fun setFlavor() {
    val appId = BuildConfig.APPLICATION_ID
    if (appId === "toys.compy.player") {
      flavor = Flavor.PLAYER
      isPlayer = true
    } else if (appId === "toys.compy.?") {
      flavor = Flavor.HARMONY
    } else {
      flavor = Flavor.IDE
    }
  }

  @SuppressLint("UseKtx")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d("CompyActivity", "---------- Started")

    setFlavor()

    val intent = getIntent()
    handleIntent(intent)
    intent.setData(null)

    if (flavor == Flavor.IDE) {
      if (Build.VERSION.SDK_INT >= 30) {
        val allFilesPerm = Environment.isExternalStorageManager()
        val requestCode = 2296
        if (!allFilesPerm) {
          Log.i(
            "CompyActivity",
            "All files permission: ${checkCallingOrSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)}"
          )
          try {
            val permsIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            permsIntent.addCategory("android.intent.category.DEFAULT")
            permsIntent.setData("package:${applicationContext.packageName}".toUri())
            startActivityForResult(permsIntent, requestCode)
          } catch (e: Exception) {
            val permsIntent = Intent()
            permsIntent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(permsIntent, requestCode)
          }
        }
      }
    }

    if (flavor == Flavor.PLAYER) {
      if (projectPath.isEmpty()) {
        Log.d("CompyActivity", "No project selected, launching Selector intent")
        val selectIntent = Intent(this, ProjectSelector::class.java)
        startActivity(selectIntent)
      }
    }
  }

  override fun onDestroy() {
    if (isPlayer) {
      projectPath = ""
    }
    super.onDestroy()
  }

  override fun onNewIntent(intent: Intent) {
    Log.d("CompyActivity", "onNewIntent() with $intent")
    handleIntent(intent)
    if (!embed) {
      resetNative()
      startNative()
    }
  }

  override fun handleIntent(intent: Intent) {
    val uri = intent.data

    if (embed && uri != null) {
      val scheme = uri.scheme
      val path = uri.path

      if (scheme == "file") {
        Log.d("CompyActivity", "Received file:// intent with path: $path")
      } else if (scheme == "content") {
        Log.d("CompyActivity", "Received content:// intent with path: " + path)
        try {
          var filename = ""
          val pathSegments =
                  path!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
          if (pathSegments.isNotEmpty()) {
            filename = pathSegments[pathSegments.size - 1]
            val suffix = ".compy"
            if (filename.endsWith(suffix)) {
              // int l = filename.length();
              // projectName = filename.substring(0, l - suffix.length());
              projectName = filename
            }
          }
          Log.d("CompyActivity", "fn: $filename pn: $projectName")

          val destFile = this.cacheDir.path + "/" + projectName
          val data = contentResolver.openInputStream(uri)

          // copyAssetFile automatically closes the InputStream
          if (copyAssetFile(data, destFile)) {
            projectPath = destFile
          }
        } catch (e: Exception) {
          Log.d("CompyActivity", "could not read content uri ${uri.toString()}: ${e.message}")
        }
      } else {
        Log.e("CompyActivity", "Unsupported scheme: '${uri.scheme}'. path: $path")
      }
    }
  }

  companion object {
    private var flavor = Flavor.IDE
    private var projectPath = ""
    private var projectName = "project.compy"
  }
}
