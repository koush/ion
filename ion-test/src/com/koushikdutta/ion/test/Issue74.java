package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import android.util.Log;

import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 10/27/13.
 */
public class Issue74 extends AndroidTestCase {
    public void testIssue() throws Exception {
        Ion.getDefault(getContext()).getResponseCache().clear();

        String data = Ion.with(getContext(), "https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        String data2 = Ion.with(getContext(), "https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .setLogging("MyLogs", Log.VERBOSE)
        .asString().get();

        assertEquals(data, data2);
    }
}
