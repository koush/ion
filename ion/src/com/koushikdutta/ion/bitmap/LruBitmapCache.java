package com.koushikdutta.ion.bitmap;

class LruBitmapCache extends LruCache<String, BitmapInfo> {
    public LruBitmapCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, BitmapInfo info) {
        return info.sizeOf();
    }
}
