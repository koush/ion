package com.koushikdutta.ion;

/**
 * Created by koush on 1/18/14.
 */
public class DeferredLoadBitmap extends BitmapCallback {
    BitmapFetcher fetcher;
    public DeferredLoadBitmap(Ion ion, String key, BitmapFetcher fetcher)  {
        super(ion, key, false);
        this.fetcher = fetcher;
    }
}
