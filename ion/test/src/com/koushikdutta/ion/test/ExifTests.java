package com.koushikdutta.ion.test;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 11/5/13.
 */
public class ExifTests extends AndroidTestCase {
    public void testRotated() throws Exception {
        Bitmap bitmap = Ion.with(getContext())
        .load("https://raw.githubusercontent.com/koush/ion/master/ion/test/assets/exif.jpg")
        .asBitmap()
        .get();

        assertTrue(bitmap.getWidth() < bitmap.getHeight());
    }
}
