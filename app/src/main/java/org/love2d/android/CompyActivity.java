package org.love2d.android;

import java.io.InputStream;

import org.love2d.android.executable.BuildConfig;
import android.util.Log;
import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.provider.Settings;

public class CompyActivity extends GameActivity {
  public boolean isPlayer = false;
  private enum Flavor {
    IDE,
    PLAYER,
    HARMONY
  }
  private static Flavor flavor = Flavor.IDE;
  private static String projectPath = "";
  private static String projectName = "project.compy";

  @Override
  protected String[] getArguments() {
    switch(flavor) {
      case PLAYER:
        return new String[]{"play", projectName};
      case HARMONY:
        return new String[]{"harmony"};
      default:
        return new String[0];
    }
  }

  private void setFlavor() {
    String appId = BuildConfig.APPLICATION_ID;
    if (appId == "toys.compy.player") {
      flavor = Flavor.PLAYER;
    } else if (appId == "toys.compy.?") {
      flavor = Flavor.HARMONY;
    } else {
      flavor = Flavor.IDE;
    }
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d("CompyActivity", "---------- Started");

    setFlavor();

    Intent intent = getIntent();
    handleIntent(intent);
    intent.setData(null);

    if (flavor == Flavor.IDE) {
      if (android.os.Build.VERSION.SDK_INT >= 30) {
        boolean allFilesPerm = Environment.isExternalStorageManager();
        int requestCode = 2296;
        if (! allFilesPerm) {
          Log.i("CompyActivity", "All files permission: " +
              checkCallingOrSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE));
          try {
            Intent permsIntent = new Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            permsIntent.addCategory("android.intent.category.DEFAULT");
            permsIntent.setData(Uri.parse(
                String.format("package:%s",getApplicationContext().getPackageName()))
            );
            startActivityForResult(permsIntent, requestCode);
          } catch (Exception e) {
            Intent permsIntent = new Intent();
            permsIntent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(permsIntent, requestCode);
          }
        }
      }
    }

    if (flavor == Flavor.PLAYER) {
      if (projectPath.isEmpty()) {
        Log.d("CompyActivity", "No project selected, launching Selector intent");
        Intent selectIntent = new Intent(this, ProjectSelector.class);
        startActivity(selectIntent);
      }
    }
  }

  @Override
  protected void onDestroy() {
    if (isPlayer) {
      projectPath = "";
    }
    super.onDestroy();
  }

  @Override
  protected void onNewIntent(Intent intent) {
      Log.d("CompyActivity", "onNewIntent() with " + intent);
      handleIntent(intent);
      if (!embed) {
          resetNative();
          startNative();
      }
  }


  protected void handleIntent(Intent intent) {
      Uri uri = intent.getData();

      if (embed && uri != null) {
        String scheme = uri.getScheme();
        String path = uri.getPath();

        if (scheme.equals("file")) {
          Log.d("CompyActivity",
                  "Received file:// intent with path: " + path);
        } else if (scheme.equals("content")) {
          Log.d("CompyActivity", "Received content:// intent with path: " + path);
          try {
              String filename = "";
              String[] pathSegments = path.split("/");
              if (pathSegments.length > 0) {
                filename = pathSegments[pathSegments.length - 1];
                String suffix = ".compy";
                if (filename.endsWith(suffix)) {
                  // int l = filename.length();
                  // projectName = filename.substring(0, l - suffix.length());
                  projectName = filename;
                }
              }

              String destination_file = this.getCacheDir().getPath() + "/" + projectName;
              InputStream data = getContentResolver().openInputStream(uri);

              // copyAssetFile automatically closes the InputStream
              if (copyAssetFile(data, destination_file)) {
                projectPath = destination_file;
              }
          } catch (Exception e) {
            Log.d("CompyActivity", "could not read content uri " +
                  uri.toString() + ": " + e.getMessage());
          }
        } else {
            Log.e("CompyActivity",
              "Unsupported scheme: '" + uri.getScheme() +
                      "'." + "path: " + path);
        }
      }
  }

}
