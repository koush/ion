package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.HeadersCallback;
import com.koushikdutta.ion.HeadersResponse;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 6/30/13.
 */
public class HeadersTests extends AndroidTestCase {
    boolean gotHeaders;
    public void testHeaders() throws Exception {
        gotHeaders = false;
        AsyncHttpServer httpServer = new AsyncHttpServer();
        try {
            httpServer.get("/", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    response.send("hello");
                }
            });
            httpServer.listen(Ion.getDefault(getContext()).getServer(), 5555);

            Ion.with(getContext())
            .load("http://localhost:5555/")
            .onHeaders(new HeadersCallback() {
                @Override
                public void onHeaders(HeadersResponse headers) {
                    assertEquals(headers.code(), 200);
                    gotHeaders = true;
                }
            })
            .asString()
            .get();

            assertTrue(gotHeaders);
        }
        finally {
            httpServer.stop();
            Ion.getDefault(getContext()).getServer().stop();
        }
    }

    public void testHeadersCallback() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        try {
            httpServer.get("/", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    response.send("hello");
                }
            });
            httpServer.listen(Ion.getDefault(getContext()).getServer(), 5555);

            final Semaphore semaphore = new Semaphore(0);

            Ion.with(getContext())
                    .load("http://localhost:5555/")
                    .asString()
                    .withResponse()
                    .setCallback(new FutureCallback<Response<String>>() {
                        @Override
                        public void onCompleted(Exception e, Response<String> result) {
                            assertEquals(result.getHeaders().code(), 200);
                            semaphore.release();
                        }
                    });

            assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
        }
        finally {
            httpServer.stop();
            Ion.getDefault(getContext()).getServer().stop();
        }
    }

    public void testBustedJson() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        try {
            httpServer.get("/", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    response.send("hello");
                }
            });
            httpServer.listen(Ion.getDefault(getContext()).getServer(), 5555);

            Response<JsonObject> response = Ion.with(getContext())
                    .load("http://localhost:5555/")
                    .asJsonObject()
                    .withResponse()
                    .get();

            assertNull(response.getResult());
            assertNotNull(response.getException());
        }
        finally {
            httpServer.stop();
            Ion.getDefault(getContext()).getServer().stop();
        }
    }

    public void testHeadCasing() throws Exception {
        Headers headers = new Headers();
        headers.set("Foo", "bar");
        assertTrue(headers.toString().contains("Foo"));
    }
}
