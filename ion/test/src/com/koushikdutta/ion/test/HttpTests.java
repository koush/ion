package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.Part;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.cookie.CookieMiddleware;

import org.conscrypt.OpenSSLProvider;

import java.io.File;
import java.net.HttpCookie;
import java.net.URI;
import java.security.Security;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

/**
 * Created by koush on 5/22/13.
 */
public class HttpTests extends AndroidTestCase {
    public void testString() throws Exception {
        assertNotNull(Ion.with(getContext(), "https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .asString().get());
    }

    public void testGoogle() throws Exception {
//        ConscryptMiddleware.initialize(getContext().getApplicationContext());

        Ion.getDefault(getContext())
        .getConscryptMiddleware().enable(false);


        Security.insertProviderAt(new OpenSSLProvider("MyNameBlah"), 1);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        Ion.getDefault(getContext())
        .getSpdyMiddleware().setSSLContext(ctx);

        Ion.getDefault(getContext())
        .getHttpClient().getSSLSocketMiddleware()
        .setSSLContext(ctx);

        assertNotNull(Ion.with(getContext())
        .load("https://www.google.com")
        .setTimeout(1000000)
        .asString().get(100000, TimeUnit.SECONDS));


        assertNotNull(Ion.with(getContext())
        .load("https://www.google.com")
        .setTimeout(1000000)
        .asString().get(100000, TimeUnit.SECONDS));
    }

    public void testMultipartFileContentType() throws Exception {
        File f = new File("/sdcard/ion/testdata");
        f.getParentFile().mkdirs();
        f.createNewFile();
        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.post("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                MultipartFormDataBody body = (MultipartFormDataBody)request.getBody();
                body.setMultipartCallback(new MultipartFormDataBody.MultipartCallback() {
                    @Override
                    public void onPart(Part part) {
                        response.send(part.getContentType());
                    }
                });
            }
        });
        try {
            httpServer.listen(AsyncServer.getDefault(), 6666);
            String mime = Ion.with(getContext())
            .load("http://localhost:6666/")
            .setMultipartFile("foo", "test/mime", f)
            .asString()
            .get(1000, TimeUnit.MILLISECONDS);
            assertEquals(mime, "test/mime");
        }
        finally {
            httpServer.stop();
        }
    }

    public void testStringWithCallback() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        Ion.with(getContext(),"http://www.clockworkmod.com/")
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

    public void testJsonObject() throws Exception {
        JsonObject ret = Ion.with(getContext(),"https://raw.githubusercontent.com/koush/AndroidAsync/master/AndroidAsync/test/assets/test.json")
                .asJsonObject().get();
        assertEquals("bar", ret.get("foo").getAsString());
    }

    public void testPostJsonObject() throws Exception {
        JsonObject post = new JsonObject();
        post.addProperty("ping", "pong");
        JsonObject ret = Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
                .setJsonObjectBody(post)
                .asJsonObject().get();
        assertEquals("pong", ret.get("ping").getAsString());
    }

    public void testUrlEncodedFormBody() throws Exception {
        JsonObject ret = Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
        .setBodyParameter("blit", "bip")
        .asJsonObject().get();
        assertEquals("bip", ret.get("blit").getAsString());
    }

    public void testUrlEncodedFormBodyWithNull() throws Exception {
        JsonObject ret = Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
        .setTimeout(3000000)
        .setBodyParameter("blit", null)
        .setBodyParameter("foo", "bar")
        .asJsonObject().get();
        assertTrue(!ret.has("blit"));
        assertEquals("bar", ret.get("foo").getAsString());
    }

    public void testMultipart() throws Exception {
        JsonObject ret = Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
                .setMultipartParameter("goop", "noop")
                .asJsonObject().get();
        assertEquals("noop", ret.get("goop").getAsString());
    }

    public void testCookie() throws Exception {
        Ion ion = Ion.getDefault(getContext());
        ion.getCookieMiddleware().clear();

        ion.build(getContext(), "http://google.com")
                .asString()
                .get();

        for (HttpCookie cookie: ion.getCookieMiddleware().getCookieStore().get(URI.create("http://www.google.com"))) {
            Log.i("CookieTest", cookie.getName() + ": " + cookie.getValue());
        }
        assertTrue(ion.getCookieMiddleware().getCookieManager().get(URI.create("http://www.google.com/test/path"), new Multimap()).size() > 0);

        CookieMiddleware deserialize = new CookieMiddleware(getContext(), ion.getDefault(getContext()).getName());
        assertTrue(deserialize.getCookieManager().get(URI.create("http://www.google.com/test/path"), new Multimap()).size() > 0);
    }


    public void testGroupCancel() throws Exception {
        Ion.getDefault(getContext()).cancelAll();
        assertEquals(Ion.getDefault(getContext()).getPendingRequestCount(getContext()), 0);

        Object cancelGroup = new Object();
        Ion.with(getContext(),"http://koush.clockworkmod.com/test/hang")
                .setHandler(null)
                .group(cancelGroup)
                .asJsonObject();

        // There's no decent way to test this yet...
        // Connecting should result in 2 keys (since clockworkmod.com has two ips).
        // One will connect and be used to serve the response.
        // The other will connect and be recycled.
        // Cancelling the group will result in the one serving the connection
        // to be severed. The other one will remain alive.

        // may need to increase this timeout? ugh horrible.
        Thread.sleep(500);
        Ion.getDefault(getContext()).getHttpClient().getServer().dump();

        Ion.getDefault(getContext()).cancelAll(cancelGroup);
        assertEquals(Ion.getDefault(getContext()).getPendingRequestCount(getContext()), 0);

        Thread.sleep(500);
        Ion.getDefault(getContext()).getHttpClient().getServer().dump();
    }

    public static class Dummy {
        public String foo;
    }

    public void testGson() throws Exception {
        JsonObject dummy1 = new JsonObject();
        dummy1.addProperty("foo", "bar");
        JsonObject dummy2 = new JsonObject();
        dummy2.addProperty("pong", "ping");

        JsonArray array = new JsonArray();
        array.add(dummy1);
        array.add(dummy2);

        final Semaphore semaphore = new Semaphore(0);
        Ion.with(getContext(),"https://koush.clockworkmod.com/test/echo")
                .setHandler(null)
                .setJsonArrayBody(array)
                .as(new TypeToken<List<Dummy>>() {
                })
                .setCallback(new FutureCallback<List<Dummy>>() {
                    @Override
                    public void onCompleted(Exception e, List<Dummy> result) {
                        assertEquals("bar", result.get(0).foo);
                        semaphore.release();
                    }
                });
        assertTrue(semaphore.tryAcquire(50000, TimeUnit.MILLISECONDS));
    }

    boolean wasProxied;
    public void testProxy() throws Exception {
        wasProxied = false;
        final AsyncServer proxyServer = new AsyncServer();
        AsyncHttpServer httpServer = new AsyncHttpServer();
        try {
            httpServer.get(".*", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                    Log.i("Proxy", "Proxying request");
                    wasProxied = true;
                    AsyncHttpClient proxying = new AsyncHttpClient(proxyServer);

                    String url = request.getPath();
                    proxying.executeString(new AsyncHttpGet(url), new AsyncHttpClient.StringCallback() {
                        @Override
                        public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
                            response.send(result);
                        }
                    });
                }
            });

            httpServer.listen(proxyServer, 5555);

            Future<String> ret = Ion.with(getContext(), "http://www.clockworkmod.com")
            .proxy("localhost", 5555)
            .asString();

            String data;
            assertNotNull(data = ret.get(10000, TimeUnit.MILLISECONDS));
            assertTrue(data.contains("ClockworkMod"));
            assertTrue(wasProxied);
        }
        finally {
            httpServer.stop();
            proxyServer.stop();
        }
    }

    public void testSSLNullRef() throws Exception {
        Ion.with(getContext(), "https://launchpad.net/")
                .asString()
                .get();
    }

    public void testPut() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        try {
            httpServer.addAction("PUT", "/", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                    response.send(request.getMethod());
                }
            });

            httpServer.listen(Ion.getDefault(getContext()).getServer(), 5555);

            Future<String> ret = Ion.with(getContext())
                    .load("PUT", "http://localhost:5555/")
                    .asString();

            assertEquals(ret.get(), "PUT");
        }
        finally {
            httpServer.stop();
        }
    }
}
