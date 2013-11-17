package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import android.util.Log;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

/**
 * Created by koush on 7/20/13.
 */
public class RedirectTests extends AndroidTestCase {
    public void testFinalLocation() throws Exception {
        try {
            AsyncHttpServer server = new AsyncHttpServer();
            server.listen(Ion.getDefault(getContext()).getServer(), 5555);
            server.get("/", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    response.redirect("/foo");
                }
            });

            server.get("/foo", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    response.send("bar");
                }
            });

            Response<String> response = Ion.with(getContext())
            .load("http://localhost:5555")
            .asString()
            .withResponse()
            .get();

            assertEquals(response.getResult(), "bar");
            assertEquals(response.getRequest().getUri().toString(), "http://localhost:5555/foo");
        }
        finally {
            Ion.getDefault(getContext()).getServer().stop();
        }
    }
}
