package com.koushikdutta.ion.test;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.ion.Ion;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 9/13/13.
 */
public class BitmapTests extends AndroidTestCase {
    public void testBitmapCallback() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        // todo: local resource
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

    public void test404() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.listen(5566);
        try {
            final Semaphore semaphore = new Semaphore(0);
            Ion.with(getContext())
            .load("http://localhost:5566/foo.png")
            .asBitmap()
            .setCallback(new FutureCallback<Bitmap>() {
                @Override
                public void onCompleted(Exception e, Bitmap result) {
                    semaphore.release();
                    assertNotNull(e);
                }
            });
            semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS);

            Ion.with(getContext())
            .load("http://localhost:5566/foo.png")
            .asBitmap()
            .setCallback(new FutureCallback<Bitmap>() {
                @Override
                public void onCompleted(Exception e, Bitmap result) {
                    semaphore.release();
                    assertNotNull(e);
                }
            });
            semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS);
        }
        finally {
            httpServer.stop();
            AsyncServer.getDefault().stop();
        }
    }

    public void testCropping() throws Exception {
        Bitmap result = Ion.with(getContext())
        .load("https://raw.githubusercontent.com/koush/ion/master/ion/test/assets/exif.jpg")
        .withBitmap()
        .resize(1080, 1845)
        .centerCrop()
        .asBitmap()
        .get();

        assertEquals(result.getWidth(), 1080);
        assertEquals(result.getHeight(), 1845);
        // pixel should not be clear
        int pixel = result.getPixel(1079, 1844);
        System.out.println(String.format("%x", pixel));
        assertFalse(0 == pixel);
    }
}
