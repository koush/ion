package com.koushikdutta.ion;

import android.graphics.Bitmap;

class LruBitmapCache extends LruCache<String, Bitmap> {
    public LruBitmapCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);
        if (evicted && oldValue != null) {
            System.out.println("recycling bitmap: " + key);
            oldValue.recycle();
        }
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }
}
