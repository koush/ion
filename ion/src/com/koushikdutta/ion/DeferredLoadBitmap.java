package com.koushikdutta.ion;

/**
 * Created by koush on 1/18/14.
 */
public class DeferredLoadBitmap extends BitmapCallback {
    public static int DEFER_COUNTER = 0;

    BitmapFetcher fetcher;
    int priority = ++DEFER_COUNTER;
    public DeferredLoadBitmap(Ion ion, String key, BitmapFetcher fetcher)  {
        super(ion, key, false);
        this.fetcher = fetcher;
    }
}
