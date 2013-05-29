package com.koushikdutta.ion;

import android.util.Log;

/**
 * Created by koush on 5/21/13.
 */
class IonLog {
    public static final String LOGTAG = "ION";
    public static boolean debug = true;
    public static void d(String message, Exception e) {
        if (debug)
            Log.d(LOGTAG, message, e);
    }

    public static void e(String message, Exception e) {
        Log.e(LOGTAG, message, e);
    }

    public static void i(String message, Exception e) {
        Log.i(LOGTAG, message, e);
    }

    public static void w(String message, Exception e) {
        Log.w(LOGTAG, message, e);
    }

    public static void d(String message) {
        if (debug)
            Log.d(LOGTAG, message);
    }

    public static void e(String message) {
        Log.e(LOGTAG, message);
    }

    public static void i(String message) {
        Log.i(LOGTAG, message);
    }

    public static void w(String message) {
        Log.w(LOGTAG, message);
    }
}
