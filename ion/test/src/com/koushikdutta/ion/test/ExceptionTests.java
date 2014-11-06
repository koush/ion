package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

/**
 * Created by koush on 11/4/13.
 */
public class ExceptionTests extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AsyncHttpServer server = new AsyncHttpServer();
        server.listen(5555);
        server.get("/contentlength", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                response.code(200);
                response.getHeaders().set("Content-Length", "10");
                Util.writeAll(response, "five!".getBytes(), new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        // close the socket prematurely
                        response.getSocket().close();
                    }
                });
            }
        });
        server.get("/chunked", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                response.code(200);
                Util.writeAll(response, "five!".getBytes(), new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        // close the socket prematurely
                        response.getSocket().close();
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

    public void testDisconnect() throws Exception {
        Response<String> response = Ion.with(getContext())
        .load("http://localhost:5555/contentlength")
        .setLogging("DISCONNECT", Log.DEBUG)
        .asString()
        .withResponse()
        .get();

        assertNotNull(response);
        assertNotNull(response.getException());

        response = Ion.with(getContext())
        .load("http://localhost:5555/chunked")
        .setLogging("DISCONNECT", Log.DEBUG)
        .asString()
        .withResponse()
        .get();

        assertNotNull(response);
        assertNotNull(response.getException());
    }
}

