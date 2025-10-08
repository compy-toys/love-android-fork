package org.love2d.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.appcompat.app.AppCompatActivity

import kotlin.io.path.*

class ProjectSelector : AppCompatActivity() {
  val label = "ProjectSelector"

  private val projectPicker = registerForActivityResult(OpenDocument()) { uri ->
    uri?.let {
      val intent = Intent(this, CompyActivity::class.java)
      intent.setData(uri)
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      startActivity(intent)
    } ?: run {

    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val context = this
    val projectsFolder = context.filesDir.resolve("projects")
    if (! projectsFolder.exists() ) {
      projectsFolder.mkdir()
    }
    projectPicker.launch(arrayOf("*/*"))
  }
}
