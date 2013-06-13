package com.koushikdutta.ion.bitmap;

import android.graphics.Bitmap;
import com.koushikdutta.ion.Ion;

class LruBitmapCache extends LruCache<String, BitmapInfo> {
    public LruBitmapCache(int maxSize) {
        super(maxSize);
    }

//    @Override
//    protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
//        super.entryRemoved(evicted, key, oldValue, newValue);
//        if (evicted && oldValue != null) {
//            oldValue.recycle();
//        }
//    }

    @Override
    protected int sizeOf(String key, BitmapInfo info) {
        Bitmap value = info.bitmap;
        if (value == null)
            return 0;
        return value.getRowBytes() * value.getHeight();
    }
}
