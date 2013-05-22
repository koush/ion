package com.example.ion.test;

import android.test.AndroidTestCase;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import org.json.JSONObject;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 5/22/13.
 */
public class HttpTests extends AndroidTestCase {
    public void testString() throws Exception {
        assertNotNull(Ion.with(getContext()).load("https://raw.github.com/koush/ion/master/ion-test/testdata/test.json").asString().get());
    }

    public void testStringWithCallback() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        Ion.with(getContext())
                .load("http://www.clockworkmod.com/")
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
        assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
    }

    public void testJSONObject() throws Exception {
        JSONObject ret = Ion.with(getContext())
                .load("https://raw.github.com/koush/ion/master/ion-test/testdata/test.json")
                .asJSONObject().get();
        assertEquals("bar", ret.getString("foo"));
    }

    public void testPostJSONObject() throws Exception {
        JSONObject post = new JSONObject();
        post.put("foo", "bar");
        JSONObject ret = Ion.with(getContext())
                .load("http://koush.com/test/echo")
                .setJSONObjectBody(post)
                .asJSONObject().get();
        assertEquals("bar", ret.getString("foo"));
    }

    public void testUrlEncodedFormBody() throws Exception {
        JSONObject ret = Ion.with(getContext())
                .load("http://koush.com/test/echo")
                .setBodyParameter("blit", "bip")
                .asJSONObject().get();
        assertEquals("bip", ret.getString("blit"));
    }

    public void testMultipart() throws Exception {
        JSONObject ret = Ion.with(getContext())
                .load("http://koush.com/test/echo")
                .setMultipartParameter("blit", "bip")
                .asJSONObject().get();
        assertEquals("bip", ret.getString("blit"));
    }
}
