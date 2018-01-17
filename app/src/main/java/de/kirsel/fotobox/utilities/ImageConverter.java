package de.kirsel.fotobox.utilities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/**
 * Created by simonvandeloo on 17.01.18.
 */

public class ImageConverter {

  // TODO: Implement correct funktionality

  public static Bitmap convertBitmapToMonochrome(Bitmap original, int width, int height) {
    Bitmap bmpMonochrome = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bmpMonochrome);
    ColorMatrix ma = new ColorMatrix();
    ma.setSaturation(0);
    Paint paint = new Paint();
    paint.setColorFilter(new ColorMatrixColorFilter(ma));
    canvas.drawBitmap(original, 0, 0, paint);

    int width2 = bmpMonochrome.getWidth();
    int height2 = bmpMonochrome.getHeight();

    int[] pixels = new int[width2 * height2];
    bmpMonochrome.getPixels(pixels, 0, width2, 0, 0, width2, height2);

    // Iterate over height
    for (int y = 0; y < height2; y++) {
      int offset = y * height2;
      // Iterate over width
      for (int x = 0; x < width2; x++) {
        int pixel = bmpMonochrome.getPixel(x, y);
        int lowestBit = pixel & 0xff;
        if (lowestBit < 128) {
          bmpMonochrome.setPixel(x, y, Color.BLACK);
        } else {
          bmpMonochrome.setPixel(x, y, Color.WHITE);
        }
      }
    }
    return bmpMonochrome;
  }
}
