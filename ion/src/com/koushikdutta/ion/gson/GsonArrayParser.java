package com.koushikdutta.ion.gson;

import com.google.gson.JsonArray;

/**
 * Created by koush on 6/23/14.
 */
public class GsonArrayParser extends GsonParser<JsonArray> {
    public GsonArrayParser() {
        super(JsonArray.class);
    }
}
