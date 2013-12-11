package com.koushikdutta.ion.bitmap;

import android.graphics.Bitmap;

import java.lang.ref.WeakReference;

class LruBitmapCache extends LruCache<String, BitmapInfo> {
    public LruBitmapCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, BitmapInfo info) {
        return info.sizeOf();
    }

    @Override
    protected void entryRemoved(boolean evicted, String key, BitmapInfo oldValue, BitmapInfo newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);

        // this shit is broken
        if (true)
            return;

        // on eviction, put the bitmaps into a weak ref
        if (!evicted)
            return;

        // toss the oldValue into a weak ref, and play with that.
        if (oldValue == null)
            return;
        if (oldValue.bitmaps == null)
            return;
        // don't try to weak ref on gifs, because only one bitmap
        // ref total will be held.
        if (oldValue.bitmaps.length > 1)
            return;

        oldValue.bitmapRef = new WeakReference<Bitmap>(oldValue.bitmaps[0]);
        oldValue.bitmaps = null;
        put(key, oldValue);
    }


}
