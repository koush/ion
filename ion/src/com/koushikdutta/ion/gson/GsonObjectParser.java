package com.koushikdutta.ion.gson;

import com.google.gson.JsonObject;

/**
 * Created by koush on 6/23/14.
 */
public class GsonObjectParser extends GsonParser<JsonObject> {
    public GsonObjectParser() {
        super(JsonObject.class);
    }
}
