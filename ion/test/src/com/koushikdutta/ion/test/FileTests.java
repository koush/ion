package com.koushikdutta.ion.test;

import android.os.StrictMode;
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

    public void testStrict() throws Exception {
        StreamUtility.writeFile(getContext().getFileStreamPath("test.txt"), "test");

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads()
        .detectDiskWrites()
        .detectNetwork()   // or .detectAll() for all detectable problems
        .penaltyLog()
        .build());

        File file = getContext().getFileStreamPath("test.txt");
        System.out.println(file.lastModified());
    }
}
