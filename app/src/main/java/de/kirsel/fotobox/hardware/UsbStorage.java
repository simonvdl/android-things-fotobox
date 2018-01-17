/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kirsel.fotobox.hardware;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

/**
 * Helper class to deal with methods to deal with images from the camera.
 */
public class UsbStorage {
  private static final String TAG = UsbStorage.class.getSimpleName();

  private FileSystem fileSystem;

  // Lazy-loaded singleton, so only one instance of the storage is created.
  private UsbStorage() {
  }

  private static class InstanceHolder {
    private static UsbStorage mStorage = new UsbStorage();
  }

  public static UsbStorage getInstance() {
    return InstanceHolder.mStorage;
  }

  /**
   * Initialize the camera fileSystem
   */
  public void initializeUsbStorage(Context context) {
    // Discover the camera instance
    UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(context);

    for (UsbMassStorageDevice d : devices) {

      // before interacting with a fileSystem you need to call init()!
      try {
        d.init();
        // Only uses the first partition on the fileSystem
        fileSystem = d.getPartitions().get(0).getFileSystem();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void saveImage(Bitmap bitmap) {
    if (fileSystem != null) {

      UsbFile root = fileSystem.getRootDirectory();

      try {
        UsbFile file = root.createFile(getDate() + ".JPEG");

        // write to a file
        OutputStream os = UsbFileStreamFactory.createBufferedOutputStream(file, fileSystem);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        os.close();
        Log.d(TAG, "Image " + file.getName() + " saved on USB drive: " + fileSystem.getVolumeLabel());
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      Log.d(TAG, "No USB drive detected!");
    }
  }

  private String getDate() {
    Date date = new Date();
    return "" + date.getTime();
  }
}
