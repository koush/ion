package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.ion.Ion;

import java.security.Provider;
import java.security.Security;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by koush on 10/27/14.
 */
public class ConscryptTests extends AndroidTestCase {
    // needs to be run on fresh vm startups...

    /*
    public void testConscryptInit() throws Exception {
        Ion.getDefault(getContext())
        .getConscryptMiddleware().initialize();

        Provider[] providers = Security.getProviders();

        System.out.println(providers);
    }

    public void testDefault() throws Exception {
        SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();

        Ion.getDefault(getContext())
        .getConscryptMiddleware().initialize();

        assertEquals(factory, HttpsURLConnection.getDefaultSSLSocketFactory());
    }
    */
}
