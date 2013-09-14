package com.koushikdutta.ion.test;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 9/13/13.
 */
public class BitmapTests extends AndroidTestCase {
    public void testBitmapCallback() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        Ion.with(getContext())
        .load("http://media.salon.com/2013/05/original.jpg")
        .asBitmap()
        .setCallback(new FutureCallback<Bitmap>() {
            @Override
            public void onCompleted(Exception e, Bitmap result) {
                assertNotNull(result);
                assertNull(e);
                semaphore.release();
            }
        });
        semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS);
    }
}
