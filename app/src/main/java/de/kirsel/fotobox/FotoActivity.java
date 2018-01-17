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
package de.kirsel.fotobox;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.KeyEvent;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import de.kirsel.fotobox.hardware.FotoCamera;
import de.kirsel.fotobox.hardware.ThermalPrinter;
import de.kirsel.fotobox.hardware.UsbStorage;
import de.kirsel.fotobox.utilities.BoardDefaults;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Doorbell activity that capture a picture from the Raspberry Pi 3
 * Camera on a button press and post it to Firebase and Google Cloud
 * Vision API.
 */
public class FotoActivity extends Activity {
  private static final String TAG = FotoActivity.class.getSimpleName();

  public static final boolean USE_THERMAL_PRINTER = false;

  private FotoCamera mCamera;
  private UsbStorage mStorage;
  private ThermalPrinter mThermalPrinter;
  private ButtonInputDriver mButtonInputDriver;
  private Handler mCameraHandler;

  /**
   * An additional thread for running Camera tasks that shouldn't block the UI.
   */
  private HandlerThread mCameraThread;
  private HandlerThread mStorageThread;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "FotoActivity created.");

    // We need permission to access the camera
    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      // A problem occurred auto-granting the permission
      Log.w(TAG, "No Camera permission");
      // return;
    }

    if (USE_THERMAL_PRINTER) {
      mThermalPrinter = new ThermalPrinter(this);
      Log.d(TAG, mThermalPrinter.toString());
    }

    // Creates new handlers and associated threads for camera and networking operations.
    mCameraThread = new HandlerThread("CameraBackground");
    mCameraThread.start();
    mCameraHandler = new Handler(mCameraThread.getLooper());

    mStorageThread = new HandlerThread("StorageBackground");
    mStorageThread.start();

    initPIO();

    mCamera = FotoCamera.getInstance();
    mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

    mStorage = UsbStorage.getInstance();
    mStorage.initializeUsbStorage(this);
  }

  private void initPIO() {
    Log.d(TAG, "initPIO()");
    try {
      mButtonInputDriver = new ButtonInputDriver(BoardDefaults.getGPIOForButton(), Button.LogicState.PRESSED_WHEN_LOW,
          KeyEvent.KEYCODE_ENTER);
      mButtonInputDriver.register();
      Log.d(TAG, "Button registered.");
    } catch (IOException e) {
      mButtonInputDriver = null;
      Log.w(TAG, "Could not open GPIO pins", e);
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mCamera.shutDown();

    mCameraThread.quitSafely();
    mStorageThread.quitSafely();
    try {
      mButtonInputDriver.close();
    } catch (IOException e) {
      Log.e(TAG, "button driver error", e);
    }
    if (mThermalPrinter != null) {
      mThermalPrinter.close();
      mThermalPrinter = null;
    }
  }

  @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_ENTER) {
      Log.d(TAG, "button pressed");
      mCamera.takePicture();
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  /**
   * Listener for new camera images.
   */
  private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
    @Override public void onImageAvailable(ImageReader reader) {
      Image image = reader.acquireNextImage();
      // get image bytes
      ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
      final byte[] imageBytes = new byte[imageBuf.capacity()];
      imageBuf.get(imageBytes);
      Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
      image.close();

      onPictureTaken(bitmap);
    }
  };

  private void onPictureTaken(Bitmap bitmap) {
    Log.d(TAG, "Picture taken!");
    mStorage.saveImage(bitmap);
  }

  private void printImage(Bitmap bitmap) {
    if (bitmap != null) {
      //TODO
    } else {
      Log.d(TAG, "Bitmap == null");
    }
  }
}
