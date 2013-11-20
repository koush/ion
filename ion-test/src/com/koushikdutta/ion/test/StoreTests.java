package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;

import java.util.concurrent.TimeUnit;

/**
 * Created by koush on 11/20/13.
 */
public class StoreTests extends AndroidTestCase {
    public void testString() throws Exception {
        Ion.getDefault(getContext())
        .store()
        .remove("foo")
        .get(1000, TimeUnit.MILLISECONDS);

        Ion.getDefault(getContext())
        .store()
        .putString("foo", "bar")
        .get(1000, TimeUnit.MILLISECONDS);

        String result = Ion.getDefault(getContext())
        .store()
        .getString("foo")
        .get(1000, TimeUnit.MILLISECONDS);

        assertEquals(result, "bar");
    }

    public void testJson() throws Exception {
        Ion.getDefault(getContext())
        .store()
        .remove("foo")
        .get(1000, TimeUnit.MILLISECONDS);

        JsonObject json = new JsonObject();
        json.addProperty("foo", "bar");

        Ion.getDefault(getContext())
        .store()
        .putJsonObject("foo", json)
        .get(1000, TimeUnit.MILLISECONDS);

        JsonObject result = Ion.getDefault(getContext())
        .store()
        .getJsonObject("foo")
        .get(1000, TimeUnit.MILLISECONDS);

        assertEquals(result.get("foo").getAsString(), "bar");
    }

    public static class TestClass {
        public String foo;
    }

    public void testGson() throws Exception {
        Ion.getDefault(getContext())
        .store()
        .remove("foo")
        .get(1000, TimeUnit.MILLISECONDS);

        TestClass test = new TestClass();
        test.foo = "bar";

        Ion.getDefault(getContext())
        .store()
        .put("foo", test, TestClass.class)
        .get(1000, TimeUnit.MILLISECONDS);

        TestClass result = Ion.getDefault(getContext())
        .store()
        .get("foo", TestClass.class)
        .get(1000, TimeUnit.MILLISECONDS);

        assertEquals(result.foo, "bar");
    }
}
