package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.ion.Ion;

import java.security.Provider;
import java.security.Security;

/**
 * Created by koush on 10/27/14.
 */
public class ConscryptTests extends AndroidTestCase {
    public void testConscryptInit() throws Exception {
        Ion.getDefault(getContext())
        .getConscryptMiddleware().initialize();

        Provider[] providers = Security.getProviders();

        System.out.println(providers);
    }
}
