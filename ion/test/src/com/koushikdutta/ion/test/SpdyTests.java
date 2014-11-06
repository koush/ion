package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import com.koushikdutta.async.util.StreamUtility;
import com.koushikdutta.ion.Ion;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

/**
 * Created by koush on 7/28/14.
 */
public class SpdyTests extends AndroidTestCase {
    public void testAppEngineSpdy() throws Exception {
//        Ion.getDefault(getContext())
//        .getConscryptMiddleware().enable(false);

//        Security.insertProviderAt(new OpenSSLProvider("MyNameBlah"), 1);

        String uploadUrl = Ion.with(getContext())
        .load("https://ion-test.appspot.com/upload_url")
        .asString()
        .get();

        byte[] random = new byte[100000];
        new Random(39548394).nextBytes(random);
        String b64 = Base64.encodeToString(random, 0);

        File file = getContext().getFileStreamPath("testData");
        StreamUtility.writeFile(file, b64);

        String data = Ion.with(getContext())
        .load(uploadUrl)
        .setLogging("test", Log.VERBOSE)
        .setMultipartFile("file", file)
        .asString()
        .get();

        assertEquals(b64, data);
    }

    public void testQueryString() throws Exception {
        String data = Ion.with(getContext())
        .load("https://ion-test.appspot.com/querystring")
        .addQuery("foo", "bar")
        .setLogging("test", Log.VERBOSE)
        .asString()
        .get();

        assertEquals("foo=bar", data);
    }

    public void testGoogleSpdy() throws Exception {
        assertNotNull(Ion.with(getContext())
        .load("https://www.google.com")
        .setLogging("test", Log.VERBOSE)
        .setTimeout(1000000)
        .asString().get(100000, TimeUnit.SECONDS));


        assertNotNull(Ion.with(getContext())
        .load("https://www.google.com")
        .setLogging("test", Log.VERBOSE)
        .setTimeout(1000000)
        .asString().get(100000, TimeUnit.SECONDS));
    }
}
