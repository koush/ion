package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.FilteredDataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.gson.GsonObjectParser;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Created by koush on 6/5/13.
 */
public class GsonTests extends AndroidTestCase {
    public static class Pojo {
        public String foo = "bar";
    }
    public void testPojoPost() throws Exception {
        TypeToken<Pojo> token = new TypeToken<Pojo>(){};

        JsonObject json = Ion.with(getContext())
        .load("http://koush.clockworkmod.com/test/echo")
        .setJsonPojoBody(new Pojo())
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

    public void testParserCastingSuccess() throws Exception {
        ByteBufferList b = new ByteBufferList(ByteBuffer.wrap("{}".getBytes()));
        FilteredDataEmitter emitter = new FilteredDataEmitter() {
            @Override
            public boolean isPaused() {
                return false;
            }
        };
        GsonObjectParser g = new GsonObjectParser();
        Future<JsonObject> ret = g.parse(emitter);
        emitter.onDataAvailable(emitter, b);
        emitter.getEndCallback().onCompleted(null);
        JsonObject j = ret.get();
        assertNotNull(j);
    }


    public void testParserCastingError() throws Exception {
        ByteBufferList b = new ByteBufferList(ByteBuffer.wrap("[]".getBytes()));
        FilteredDataEmitter emitter = new FilteredDataEmitter() {
            @Override
            public boolean isPaused() {
                return false;
            }
        };
        GsonObjectParser g = new GsonObjectParser();
        Future<JsonObject> ret = g.parse(emitter);
        emitter.onDataAvailable(emitter, b);
        emitter.getEndCallback().onCompleted(null);
        try {
            JsonObject j = ret.get();
            fail(j.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testParserCastingCallbackError() throws Exception {
        ByteBufferList b = new ByteBufferList(ByteBuffer.wrap("[]".getBytes()));
        FilteredDataEmitter emitter = new FilteredDataEmitter() {
            @Override
            public boolean isPaused() {
                return false;
            }
        };
        GsonObjectParser g = new GsonObjectParser();
        Future<JsonObject> ret = g.parse(emitter);
        emitter.onDataAvailable(emitter, b);
        emitter.getEndCallback().onCompleted(null);
        final Semaphore s = new Semaphore(0);
        ret.setCallback(new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                assertNull(result);
                assertNotNull(e);
                assertTrue(e instanceof ClassCastException);
                s.release();
            }
        });
        s.acquire();
    }
}
