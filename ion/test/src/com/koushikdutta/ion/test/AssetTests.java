package com.koushikdutta.ion.test;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 6/28/14.
 */
public class AssetTests extends AndroidTestCase {
    public void testAsset() throws Exception {
        Bitmap bitmap = Ion.with(getContext())
        .load("file:///android_asset/exif.jpg")
        .asBitmap()
        .get();

        assertNotNull(bitmap);
    }
}
