package com.example.ion.test;

import android.test.AndroidTestCase;
import android.util.Log;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.IonRequestBuilderStages.IonBodyParamsRequestBuilder.ProgressCallback;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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
        assertNotNull(Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json").asString().get());
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

    final static String dataNameAndHash = "6691924d7d24237d3b3679310157d640";
    public void testProgress() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        final Semaphore progressSemaphore = new Semaphore(0);
        final Md5 md5 = Md5.createInstance();
        Ion.with(getContext())
                .load("https://github.com/koush/AndroidAsync/raw/master/AndroidAsyncTest/testdata/6691924d7d24237d3b3679310157d640")
                .setHandler(null)
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
        assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
        assertTrue(progressSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
    }

    public void testJSONObject() throws Exception {
        JSONObject ret = Ion.with(getContext())
                .load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
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
