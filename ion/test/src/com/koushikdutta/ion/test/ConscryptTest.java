package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.ion.Ion;

import org.conscrypt.OpenSSLProvider;

import java.security.Security;

import javax.net.ssl.SSLContext;

/**
 * Created by koush on 7/14/14.
 */
public class ConscryptTest extends AndroidTestCase {
    private void testConscrypt() throws Exception {
        Ion.getDefault(getContext())
        .getConscryptMiddleware().enable(false);

        Security.insertProviderAt(new OpenSSLProvider("MyNameBlah"), 1);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);


        Ion.getDefault(getContext())
        .getSpdyMiddleware().setSSLContext(ctx);

        Ion.with(getContext())
        .load("https://www.google.com")
        .asString()
        .get();
    }
}
