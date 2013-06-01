package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;
import com.koushikdutta.ion.Ion;

import java.io.File;
import java.util.concurrent.Semaphore;

/**
 * Created by koush on 5/22/13.
 */
public class FileTests extends AndroidTestCase {
    public void testFileLoader() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        File f = new File("/sdcard/test.txt");
        StreamUtility.writeFile(f, "hello world");

        assertEquals("hello world", Ion.with(getContext(), f).asString().get());
    }
}
