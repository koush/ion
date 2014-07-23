package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.async.util.StreamUtility;
import com.koushikdutta.ion.Ion;

import java.io.File;

/**
 * Created by koush on 5/22/13.
 */
public class FileTests extends AndroidTestCase {
    public void testFileLoader() throws Exception {
        File f = getContext().getFileStreamPath("test.txt");
        StreamUtility.writeFile(f, "hello world");

        assertEquals("hello world", Ion.with(getContext()).load(f).asString().get());
    }

    public void testFileUpload() throws Exception {
        File f = getContext().getFileStreamPath("test.txt");
        StreamUtility.writeFile(f, "hello world");

        Ion.with(getContext())
        .load("POST", "http://koush.com/test/echo")
        .asString()
        .get();
    }
}
