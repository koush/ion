package com.example.ion.test;

import android.test.AndroidTestCase;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 5/22/13.
 */
public class HttpTests extends AndroidTestCase {
    public void testString() throws Exception {
        assertNotNull(Ion.with(getContext()).load("http://www.clockworkmod.com/").asString().get());
    }

    public void testStringWithCallback() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        Ion.with(getContext()).load("http://www.clockworkmod.com/")
                // need to null out the handler since the semaphore blocks the main thread
                .setHandler(null)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        assertNull(e);
                        assertNotNull(result);
                        semaphore.release();
                    }
                });
        assertTrue(semaphore.tryAcquire(1000000, TimeUnit.MILLISECONDS));
    }
}
