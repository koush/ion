package com.koushikdutta.ion.test;

import android.graphics.Bitmap;
import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ListenCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.util.StreamUtility;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 10/27/13.
 */
public class Issues extends AndroidTestCase {
    public void testIssue74() throws Exception {
        String data = Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        String data2 = Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        assertEquals(data, data2);
    }

    public void testSpdyReuse() throws Exception {
        String data = Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        String data2 = Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        Ion.with(getContext()).load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        assertEquals(data, data2);
    }

    public void testIssue126() throws Exception {
        Bitmap bitmap = Ion.with(getContext())
        .load("http://bdc.tsingyuan.cn/api/img?w=advanced")
        .setLogging("Issue126", Log.VERBOSE)
        .asBitmap()
        .get();

        assertNotNull(bitmap);
        assertTrue(bitmap.getWidth() > 0);
    }

    public void testIssue146() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.getHeaders().set("Cache-Control", "max-age=300");
                response.send(request.getQuery().size() + "");
            }
        });
        AsyncServer asyncServer = new AsyncServer();
        try {
            int localPort = httpServer.listen(asyncServer, 0).getLocalPort();
            String s1 = Ion.with(getContext())
            .load("http://localhost:" + localPort)
            .addQuery("query1", "q")
            .asString()
            .get();

            String s2 = Ion.with(getContext())
            .load("http://localhost:" + localPort)
            .addQuery("query1", "q")
            .addQuery("query2", "qq")
            .asString()
            .get();

            String s3 = Ion.with(getContext())
            .load("http://localhost:" + localPort)
            .addQuery("query1", "q")
            .asString()
            .get();

            assertEquals(s1, "1");
            assertEquals(s2, "2");
            assertEquals(s3, "1");
        }
        finally {
            asyncServer.stop();
        }
    }

    public void testIssue200() throws Exception {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        params.put("email", Arrays.asList("mail@mail.pl"));
        params.put("password", Arrays.asList("pass"));

        String val = Ion.with(getContext())
        .load("https://koush.clockworkmod.com/test/echo")
        .setLogging("Issue200", Log.VERBOSE)
        .setBodyParameters(params)
        .asString()
        .get(2000, TimeUnit.MILLISECONDS);

        System.out.println(val);
    }

    public void testIssue179() throws Exception {
        Ion.with(getContext())
        .load("https://api.gigaset-elements.de/app/check-support" )
        .setBodyParameter("version", "1.0")
        .asString()
        .setCallback(new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if(result!=null)
                    Log.d("WTF",result);
                if(e!=null){
                    e.printStackTrace();
                }
            }
        })
        .get();
    }

    public void testIssue253() throws Exception {
        byte[] random = new byte[100000];
        new Random(39548394).nextBytes(random);
        String b64 = Base64.encodeToString(random, 0);

        String uploadUrl = Ion.with(getContext())
        .load("https://ion-test.appspot.com/upload_url")
        .asString()
        .get();

        File file = getContext().getFileStreamPath("testData");
        StreamUtility.writeFile(file, b64);

        String data = Ion.with(getContext())
        .load(uploadUrl)
        .setMultipartFile("file", file)
        .asString()
        .get();

        assertEquals(b64, data);
    }

    /*
    public void testSSLv3Workaround() throws Exception {
        Ion.getDefault(getContext())
        .getHttpClient()
        .getSSLSocketMiddleware()
        .addEngineConfigurator(new AsyncSSLEngineConfigurator() {
            @Override
            public void configureEngine(SSLEngine engine) {
                engine.setEnabledProtocols(new String[] { "SSLv3" });
            }
        });
        Ion.with(getContext())
        .load("https://members.easynews.com/dl/893b36f51a28bb066a7401e2850ecf2401cdd97a1.jpg/Kittens-and-Puppies-13_graylady.jpg")
        .asString()
        .get();
    }
    */

    AsyncServerSocket server;
    public void testIssue312() throws Exception {
        String b64 = "SFRUUC8xLjAgMzAyIEZvdW5kDQpTZXQtQ29va2ll\n" +
        "OlNFU1NJT049NUJBRDlERTEwQjY0NjgwNDsKTG9j\n" +
        "YXRpb246IGhvbWUuY2dpCkNvbnRlbnQtdHlwZTog\n" +
        "dGV4dC9odG1sCgo8aHRtbD48aGVhZD48bWV0YSBo\n" +
        "dHRwLWVxdWl2PSdyZWZyZXNoJyBjb250ZW50PScw\n" +
        "OyB1cmw9aG9tZS5jZ2knPjwvbWV0YT48L2hlYWQ+\n" +
        "PGJvZHk+PC9ib2R5PjwvaHRtbD4K";

        /*
        HTTP/1.0 302 Found
        Set-Cookie:SESSION=5BAD9DE10B646804;
        Location: home.cgi
        Content-type: text/html

        <html><head><meta http-equiv='refresh' content='0; url=home.cgi'></meta></head><body></body></html>
         */

        // the above is using newlines, and not CRLF.

        final byte[] responseData = Base64.decode(b64, 0);


        server = Ion.getDefault(getContext())
        .getServer().listen(null, 0, new ListenCallback() {
            @Override
            public void onAccepted(final AsyncSocket socket) {
                Util.writeAll(socket, responseData, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        socket.end();
                        server.stop();
                    }
                });
            }
            @Override
            public void onListening(AsyncServerSocket socket) {
            }
            @Override
            public void onCompleted(Exception ex) {
            }
        });

        Ion.with(getContext())
        .load("http://localhost:" + server.getLocalPort())
        .followRedirect(false)
        .asString()
        .get();
    }

    public void testIssue318() throws Exception {
        String response = Ion.with(getContext()).load("http://banpo.hs.kr/custom/custom.do?dcpNo=30524").asString().get();
        assertNotNull(response);
    }

    public void testIssue329() throws Exception {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.post("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                UrlEncodedFormBody body = (UrlEncodedFormBody)request.getBody();
                response.send(body.get().getString("電"));
            }
        });

        AsyncServer asyncServer = new AsyncServer();
        try {
            int localPort = httpServer.listen(asyncServer, 0).getLocalPort();
            String s1 = Ion.with(getContext())
            .load("http://localhost:" + localPort)
            .setBodyParameter("電", "電")
            .asString()
            .get();

            assertEquals(s1, "電");
        }
        finally {
            asyncServer.stop();
        }
    }

    public void testAAIssue225() throws Exception {
        String ret = Ion.with(getContext())
        .load("https://content.fastrbooks.com/android-test.txt")
        .noCache()
        .asString()
        .get();

        System.out.println(ret);
    }

    public void testIon428() throws Exception {
        Ion.with(getContext())
        .load("https://cdn2.vox-cdn.com/thumbor/KxtZNw37jKNfxdA0hX5edHvbTBE=/0x0:2039x1359/800x536/cdn0.vox-cdn.com/uploads/chorus_image/image/44254028/lg-g-watch.0.0.jpg")
        .asString()
        .get();
    }

    public void testIon450() throws Exception {
        Ion.getDefault(getContext())
        .configure().setLogging("Test", Log.VERBOSE);

        Ion.with(getContext())
        .load("https://api.instagram.com/v1/users/self/feed")
        .asString()
        .get();

        Ion.with(getContext())
        .load("https://api.instagram.com/v1/users/self/feed")
        .asString()
        .get();

        Ion.with(getContext())
        .load("https://api.instagram.com/v1/users/self/feed")
        .asString()
        .get();

    }
}
