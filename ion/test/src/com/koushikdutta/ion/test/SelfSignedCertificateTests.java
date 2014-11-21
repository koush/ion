package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.ion.Ion;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by koush on 11/21/14.
 */
public class SelfSignedCertificateTests extends AndroidTestCase {
    public void testKeys() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        ks.load(getContext().getResources().openRawResource(R.raw.keystore), "storepass".toCharArray());
        kmf.init(ks, "storepass".toCharArray());


        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        ts.load(getContext().getResources().openRawResource(R.raw.keystore), "storepass".toCharArray());
        tmf.init(ts);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.listenSecure(8888, sslContext);
        httpServer.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("hello");
            }
        });

        Thread.sleep(1000);

        Ion ion = Ion.getInstance(getContext(), "CustomSSL");
        ion.getHttpClient().getSSLSocketMiddleware().setSSLContext(sslContext);
        ion.getHttpClient().getSSLSocketMiddleware().setTrustManagers(tmf.getTrustManagers());

        ion.build(getContext())
        .load("https://localhost:8888/")
        .asString()
        .get();
    }

}
