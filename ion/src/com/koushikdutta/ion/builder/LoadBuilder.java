package com.koushikdutta.ion.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.Future;

import org.w3c.dom.Document;

import java.io.File;
import java.io.InputStream;

/**
* Created by koush on 5/30/13.
*/ // .load
public interface LoadBuilder<B> {
    /**
     * Load an uri.
     * @param uri Uri to load. This may be a http(s), file, or content uri.
     * @return
     */
    public B load(String uri);

    /**
     * Load an url using the given an HTTP method such as GET or POST.
     * @param method HTTP method such as GET or POST.
     * @param url Url to load.
     * @return
     */
    public B load(String method, String url);

    /**
     * Load a file.
     * @param file File to load.
     * @return
     */
    public B load(File file);
}
