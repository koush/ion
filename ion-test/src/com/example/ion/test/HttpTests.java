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
                // need to null out the handler since the semaphore blocks the main thread,
                // and ion's default behavior is to post back onto the main thread or calling Handler.
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
        post.put("ping", "pong");
        JSONObject ret = Ion.with(getContext())
                .load("https://koush.clockworkmod.com/test/echo")
                .setJSONObjectBody(post)
                .asJSONObject().get();
        assertEquals("pong", ret.getString("ping"));
    }

    public void testUrlEncodedFormBody() throws Exception {
        JSONObject ret = Ion.with(getContext())
                .load("https://koush.clockworkmod.com/test/echo")
                .setBodyParameter("blit", "bip")
                .asJSONObject().get();
        assertEquals("bip", ret.getString("blit"));
    }

    public void testMultipart() throws Exception {
        JSONObject ret = Ion.with(getContext())
                .load("https://koush.clockworkmod.com/test/echo")
                .setMultipartParameter("goop", "noop")
                .asJSONObject().get();
        assertEquals("noop", ret.getString("goop"));
    }
}
