package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.Ion;

import junit.framework.Test;

import java.util.concurrent.ExecutionException;

/**
 * Created by koush on 6/5/13.
 */
public class GsonTests extends AndroidTestCase {
    public static class Pojo {
        public String foo = "bar";
    }
    public void testPojoPost() throws Exception {
        TypeToken<Pojo> token = new TypeToken<Pojo>(){};

        JsonObject json = Ion.with(getContext(), "http://koush.clockworkmod.com/test/echo")
        .setJsonObjectBody(new Pojo())
        .asJsonObject().get();

        assertEquals(json.get("foo").getAsString(), "bar");
    }

    public void testJunkPayload() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        try {
            httpServer.get("/", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                    response.send("not json!");
                }
            });

            httpServer.listen(5555);

            Future<JsonObject> ret = Ion.with(getContext())
                    .load("PUT", "http://localhost:5555/")
                    .asJsonObject();

            ret.get();
            fail();
        }
        catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof JsonParseException);
        }
        finally {
            httpServer.stop();
            AsyncServer.getDefault().stop();
        }
    }
}
