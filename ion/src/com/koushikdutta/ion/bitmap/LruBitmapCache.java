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

        // on eviction, put the bitmaps into a weak ref
        if (!evicted)
            return;

        // toss the oldValue into a weak ref, and play with that.
        if (oldValue == null)
            return;
        if (oldValue.bitmaps == null)
            return;

        oldValue.bitmapsRef = new WeakReference<Bitmap[]>(oldValue.bitmaps);
        oldValue.bitmaps = null;
        put(key, oldValue);
    }


}
