package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.Part;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 6/13/13.
 */
public class ProgressTests extends AndroidTestCase {
    final static String dataNameAndHash = "6691924d7d24237d3b3679310157d640";
    public void testProgress() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        final Semaphore progressSemaphore = new Semaphore(0);
        final Md5 md5 = Md5.createInstance();
        Ion.with(getContext())
        .load("https://raw.githubusercontent.com/koush/AndroidAsync/master/AndroidAsync/test/assets/6691924d7d24237d3b3679310157d640")
        .setHandler(null)
        .setTimeout(600000)
        .setLogging("testProgress", Log.VERBOSE)
        .progress(new ProgressCallback() {
            @Override
            public void onProgress(long downloaded, long total) {
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

    public void testUpload() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        try {
            httpServer.listen(Ion.getDefault(getContext()).getServer(), 5000);
            httpServer.post("/", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                    MultipartFormDataBody multipartFormDataBody = (MultipartFormDataBody)request.getBody();

                    multipartFormDataBody.setMultipartCallback(new MultipartFormDataBody.MultipartCallback() {
                        @Override
                        public void onPart(Part part) {

                        }
                    });

                    multipartFormDataBody.setEndCallback(new CompletedCallback() {
                        @Override
                        public void onCompleted(Exception ex) {
                            response.send("Got parts!");
                        }
                    });
                }
            });

            final Semaphore semaphore = new Semaphore(0);
            Ion.with(getContext()).load("http://localhost:5000/")
                    .uploadProgress(new ProgressCallback() {
                        @Override
                        public void onProgress(long downloaded, long total) {
                            semaphore.release();
                        }
                    })
                    .setMultipartParameter("foo", "bar")
                    .asString()
                    .get();
            assertTrue(semaphore.tryAcquire());
        }
        finally {
            Ion.getDefault(getContext()).getServer().stop();
        }
    }
}
