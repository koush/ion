package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import android.util.Base64;
import com.google.gson.JsonObject;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 6/6/13.
 */
public class AuthTests extends AndroidTestCase {
    public void testBasicAuth() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.listen(5555);

        httpServer.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    JsonObject json = new JsonObject();
                    String authorization = request.getHeaders().getHeaders().get("Authorization");
                    authorization = new String(Base64.decode(authorization, Base64.DEFAULT));
                    String[] parts = authorization.split(":");
                    assertTrue(parts.length == 2);
                    String username = parts[0];
                    String password = parts[1];
                    json.addProperty("username", username);
                    json.addProperty("password", password);
                    response.send(json.toString());
                }
                catch (Exception e) {
                    fail();
                }
            }
        });


        JsonObject result = Ion.with(getContext(), "http://localhost:5555")
        .basicAuthentication("foo", "bar")
        .asJsonObject()
        .get();

        assertEquals("bar", result.get("password").getAsString());
        assertEquals("foo", result.get("username").getAsString());
    }
}
