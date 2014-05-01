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
        .store("foo")
        .remove();

        Ion.getDefault(getContext())
        .store("foo")
        .putString("bar")
        .get(1000, TimeUnit.MILLISECONDS);

        String result = Ion.getDefault(getContext())
        .store("foo")
        .getString();

        assertEquals(result, "bar");
    }

    public void testJson() throws Exception {
        Ion.getDefault(getContext())
        .store("foo")
        .remove();

        JsonObject json = new JsonObject();
        json.addProperty("foo", "bar");

        Ion.getDefault(getContext())
        .store("foo")
        .putJsonObject(json)
        .get(1000, TimeUnit.MILLISECONDS);

        JsonObject result = Ion.getDefault(getContext())
        .store("foo")
        .getJsonObject();

        assertEquals(result.get("foo").getAsString(), "bar");
    }

    public static class TestClass {
        public String foo;
    }

    public void testGson() throws Exception {
        Ion.getDefault(getContext())
        .store("foo")
        .remove();

        TestClass test = new TestClass();
        test.foo = "bar";

        Ion.getDefault(getContext())
        .store("foo")
        .put(test, TestClass.class)
        .get(1000, TimeUnit.MILLISECONDS);

        TestClass result = Ion.getDefault(getContext())
        .store("foo")
        .get(TestClass.class);

        assertEquals(result.foo, "bar");
    }
}
