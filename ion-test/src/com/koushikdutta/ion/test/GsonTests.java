package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.Ion;

import junit.framework.Test;

/**
 * Created by koush on 6/5/13.
 */
public class GsonTests extends AndroidTestCase {
    public static class Pojo {
        public String foo = "bar";
    }
    public void testPojoPost() throws Exception {
        TypeToken<Pojo> token = new TypeToken<Pojo>(){};

        JsonObject json = Ion.with(getContext(), "http://koush.clockworkmod.com/test/echo")
        .setJsonObjectBody(new Pojo())
        .asJsonObject().get();

        assertEquals(json.get("foo").getAsString(), "bar");
    }
}
