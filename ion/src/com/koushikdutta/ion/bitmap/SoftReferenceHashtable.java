package com.koushikdutta.ion.bitmap;

import java.lang.ref.SoftReference;

public class SoftReferenceHashtable<K,V> extends ReferenceHashtable<K, V, SoftReference<V>> {
    @Override
    protected SoftReference<V> create(V value) {
        return new SoftReference<V>(value);
    }
}