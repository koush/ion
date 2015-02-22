package com.koushikdutta.ion.gson;

import com.google.gson.JsonArray;

import java.nio.charset.Charset;

/**
 * Created by koush on 6/23/14.
 */
public class GsonArrayParser extends GsonParser<JsonArray> {
    public GsonArrayParser() {
        super(JsonArray.class);
    }

    public GsonArrayParser(Charset charset) {
        super(JsonArray.class, charset);
    }
}
