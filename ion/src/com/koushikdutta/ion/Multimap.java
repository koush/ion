package com.koushikdutta.ion;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by koush on 5/27/13.
 */
class Multimap<T> extends Hashtable<String, ArrayList<T>> {
    public Multimap() {
    }

    public boolean contains(String key) {
        ArrayList<T> check = get(key);
        return check != null && check.size() > 0;
    }

    public void add(String key, T value) {
        ArrayList<T> ret = get(key);
        if (ret == null) {
            ret = new ArrayList<T>();
            put(key, ret);
        }
        ret.add(value);
    }

    public ArrayList<T> removeAll(String key) {
        return remove(key);
    }
}
