package com.koushikdutta.ion;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

class WeakReferenceHashTable<K,V> {
    Hashtable<K, WeakReference<V>> mTable = new Hashtable<K, WeakReference<V>>();

    public List<V> values() {
        ArrayList<V> ret = new ArrayList<V>();
        for (WeakReference<V> v: mTable.values()) {
            V val = v.get();
            if (val != null)
                ret.add(val);
        }
        return ret;
    }

    public int size() {
        int size = 0;
        for (K k: mTable.keySet()) {
            WeakReference<V> v = mTable.get(k);
            if (v != null && v.get() != null) {
//                System.out.println(k);
                size++;
            }
        }
        return size;
    }

    public V put(K key, V value) {
        WeakReference<V> old = mTable.put(key, new WeakReference<V>(value));
        if (old == null)
            return null;
        return old.get();
    }
    
    public V get(K key) {
        WeakReference<V> val = mTable.get(key);
        if (val == null)
            return null;
        V ret = val.get();
        if (ret == null)
            mTable.remove(key);
        return ret;
    }
    
    public V remove(K k) {
        WeakReference<V> v = mTable.remove(k);
        if (v == null)
            return null;
        return v.get();
    }
}
