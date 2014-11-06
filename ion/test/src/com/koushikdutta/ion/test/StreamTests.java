package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.util.StreamUtility;
import com.koushikdutta.ion.Ion;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by koush on 11/3/13.
 */
public class StreamTests extends AndroidTestCase {
    byte[] random = new byte[100000];
    int port;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AsyncHttpServer server = new AsyncHttpServer();
        port = server.listen(0).getLocalPort();
        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                response.code(200);
                ByteBuffer b = ByteBufferList.obtain(random.length);
                b.put(random);
                b.flip();
                ByteBufferList list = new ByteBufferList(b);
                Util.writeAll(response, list, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        response.end();
                    }
                });
            }
        });
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        AsyncServer.getDefault().stop();
    }

    public void testStream() throws Exception {
        new Random(39548394).nextBytes(random);
        Ion.with(getContext())
        .load("http://localhost:" + port + "/")
        .write(new FileOutputStream(getContext().getFileStreamPath("test")), true)
        .get();

        assertEquals(Md5.createInstance().update(random).digest(), Md5.digest(getContext().getFileStreamPath("test")));
    }


    public void testInputStream() throws Exception {
        new Random(39548394).nextBytes(random);
        InputStream is = Ion.with(getContext())
        .load("http://localhost:" + port + "/")
        .asInputStream()
        .get();

        assertEquals(Md5.createInstance().update(random).digest(),
            Md5.createInstance().update(StreamUtility.readToEndAsArray(is)).digest());
    }
}
