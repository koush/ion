package com.koushikdutta.ion.bitmap;

import com.koushikdutta.async.util.LruCache;

class LruBitmapCache extends LruCache<String, BitmapInfo> {
    private SoftReferenceHashtable<String, BitmapInfo> soft = new SoftReferenceHashtable<String, BitmapInfo>();

    public void putSoft(String key, BitmapInfo value) {
        soft.put(key, value);
    }

    public LruBitmapCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected long sizeOf(String key, BitmapInfo info) {
        return info.sizeOf();
    }

    public BitmapInfo getBitmapInfo(String key) {
        BitmapInfo ret = get(key);
        if (ret != null)
            return ret;

        ret = soft.remove(key);
        if (ret != null)
            put(key, ret);

        return ret;
    }

    public BitmapInfo removeBitmapInfo(String key) {
        BitmapInfo i1 = soft.remove(key);
        BitmapInfo i2 = remove(key);
        if (i2 != null)
            return i2;
        return i1;
    }

    public void evictAllBitmapInfo() {
        evictAll();
        soft.clear();
    }

    @Override
    protected void entryRemoved(boolean evicted, String key, BitmapInfo oldValue, BitmapInfo newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);

        // on eviction, put the bitmaps into the soft ref table
        if (evicted)
            soft.put(key, oldValue);
    }
}
