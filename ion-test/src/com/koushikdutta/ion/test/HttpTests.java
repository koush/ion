package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 5/22/13.
 */
public class HttpTests extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Ion.getDefault(getContext()).setLogging("HttpTests", Log.DEBUG);
    }

    public void testString() throws Exception {
        assertNotNull(Ion.with(getContext(), "https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
                .asString().get());
    }

    public void testStringWithCallback() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        Ion.with(getContext(),"http://www.clockworkmod.com/")
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

    final static String dataNameAndHash = "6691924d7d24237d3b3679310157d640";
    public void testProgress() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        final Semaphore progressSemaphore = new Semaphore(0);
        final Md5 md5 = Md5.createInstance();
        Ion.with(getContext(),"https://github.com/koush/AndroidAsync/raw/master/AndroidAsyncTest/testdata/6691924d7d24237d3b3679310157d640")
                .setHandler(null)
                .setTimeout(600000)
                .progress(new ProgressCallback() {
                    @Override
                    public void onProgress(int downloaded, int total) {
                        // depending on gzip, etc. the total may vary... the actual length of the uncompressed data
                        // is 100000
                        assertTrue(total > 90000 && total < 110000);
                        progressSemaphore.release();
                    }
                })
                .write(new ByteArrayOutputStream())
                .setCallback(new FutureCallback<ByteArrayOutputStream>() {
                    @Override
                    public void onCompleted(Exception e, ByteArrayOutputStream result) {
                        byte[] bytes = result.toByteArray();
                        md5.update(new ByteBufferList(bytes));
                        assertEquals(md5.digest(), dataNameAndHash);
                        semaphore.release();
                    }
                });
        assertTrue(semaphore.tryAcquire(600000, TimeUnit.MILLISECONDS));
        assertTrue(progressSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
    }

    public void testJSONObject() throws Exception {
        JSONObject ret = Ion.with(getContext(),"https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
                .asJSONObject().get();
        assertEquals("bar", ret.getString("foo"));
    }

    public void testPostJSONObject() throws Exception {
        JSONObject post = new JSONObject();
        post.put("ping", "pong");
        JSONObject ret = Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
                .setJSONObjectBody(post)
                .asJSONObject().get();
        assertEquals("pong", ret.getString("ping"));
    }

    public void testUrlEncodedFormBody() throws Exception {
        JSONObject ret = Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
                .setBodyParameter("blit", "bip")
                .asJSONObject().get();
        assertEquals("bip", ret.getString("blit"));
    }

    public void testMultipart() throws Exception {
        JSONObject ret = Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
                .setMultipartParameter("goop", "noop")
                .asJSONObject().get();
        assertEquals("noop", ret.getString("goop"));
    }

    public void testCookie() throws Exception {
        Ion ion = Ion.getDefault(getContext());
        ion.getCookieMiddleware().getCookieStore().removeAll();

        ion.build(getContext(), "http://google.com")
                .asString()
                .get();

        for (HttpCookie cookie: ion.getCookieMiddleware().getCookieStore().get(URI.create("http://google.com"))) {
            Log.i("CookieTest", cookie.getName() + ": " + cookie.getValue());
        }
        assertTrue(ion.getCookieMiddleware().getCookieManager().get(URI.create("http://google.com"), new Multimap()).size() > 0);
    }


    public void testGroupCancel() throws Exception {
        Ion.getDefault(getContext()).cancelAll();
        assertEquals(Ion.getDefault(getContext()).getPendingRequestCount(getContext()), 0);

        Object cancelGroup = new Object();
        Ion.with(getContext(),"http://koush.clockworkmod.com/test/hang")
                .setHandler(null)
                .group(cancelGroup)
                .asJSONObject();

        // There's no decent way to test this yet...
        // Connecting should result in 2 keys (since clockworkmod.com has two ips).
        // One will connect and be used to serve the response.
        // The other will connect and be recycled.
        // Cancelling the group will result in the one serving the connection
        // to be severed. The other one will remain alive.

        // may need to increase this timeout? ugh horrible.
        Thread.sleep(500);
        Ion.getDefault(getContext()).getHttpClient().getServer().dump();

        Ion.getDefault(getContext()).cancelAll(cancelGroup);
        assertEquals(Ion.getDefault(getContext()).getPendingRequestCount(getContext()), 0);

        Thread.sleep(500);
        Ion.getDefault(getContext()).getHttpClient().getServer().dump();
    }

    public static class Dummy {
        public String foo;
    }

    public void testGson() throws Exception {
        JSONObject dummy1 = new JSONObject();
        dummy1.put("foo", "bar");
        JSONObject dummy2 = new JSONObject();
        dummy2.put("pong", "ping");

        JSONArray array = new JSONArray();
        array.put(dummy1);
        array.put(dummy2);

        final Semaphore semaphore = new Semaphore(0);
        Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
                .setHandler(null)
                .setJSONArrayBody(array)
                .as(new TypeToken<List<Dummy>>(){})
                .setCallback(new FutureCallback<List<Dummy>>() {
                    @Override
                    public void onCompleted(Exception e, List<Dummy> result) {
                        assertEquals("bar", result.get(0).foo);
                        semaphore.release();
                    }
                });
        assertTrue(semaphore.tryAcquire(50000, TimeUnit.MILLISECONDS));
    }
}
