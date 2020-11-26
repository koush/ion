package com.koushikdutta.ion.bitmap;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Hashtable;

public abstract class ReferenceHashtable<K,V, R extends Reference<V>> {
    Hashtable<K, R> mTable = new Hashtable<K, R>();

    protected abstract R create(V value);

    public V put(K key, V value) {
        R old = mTable.put(key, create(value));
        if (old == null)
            return null;
        return old.get();
    }

    public V get(K key) {
        R val = mTable.get(key);
        if (val == null)
            return null;
        V ret = val.get();
        if (ret == null)
            mTable.remove(key);
        return ret;
    }

    public V remove(K k) {
        R v = mTable.remove(k);
        if (v == null)
            return null;
        return v.get();
    }

    public void clear() {
        mTable.clear();
    }
}