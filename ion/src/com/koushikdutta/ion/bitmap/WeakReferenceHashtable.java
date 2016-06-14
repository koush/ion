package com.koushikdutta.ion.bitmap;

import java.lang.ref.WeakReference;

public class WeakReferenceHashtable<K,V> extends ReferenceHashtable<K, V, WeakReference<V>> {
    @Override
    protected WeakReference<V> create(V value) {
        return new WeakReference<V>(value);
    }
}