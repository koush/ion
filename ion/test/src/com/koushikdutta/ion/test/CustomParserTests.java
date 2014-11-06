package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 12/17/13.
 */
public class CustomParserTests extends AndroidTestCase {
    public void testCustomParser() throws Exception {
        assertNotNull(Ion.with(getContext())
        .load("https://raw.github.com/koush/AndroidAsync/master/AndroidAsyncTest/testdata/test.json")
        .as(new StringParser()).get());
    }
}
