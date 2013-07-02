package com.koushikdutta.ion.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.future.RequestFuture;

import java.io.File;
import java.io.OutputStream;

/**
* Created by koush on 5/30/13.
*/ // get the result, transformed to how you want it
public interface FutureBuilder extends BitmapFutureBuilder, ImageViewFutureBuilder {
    /**
     * Execute the request and get the result as a String
     * @return
     */
    public RequestFuture<String> asString();

    /**
     * Execute the request and get the result as a (Gson) JsonArray
     * @return
     */
    public RequestFuture<JsonArray> asJsonArray();

    /**
     * Execute the request and get the result as a (Gson) JsonObject
     * @return
     */
    public RequestFuture<JsonObject> asJsonObject();

    /**
     * Use the request as a Bitmap which can then be modified and/or applied to an ImageView.
     * @return
     */
    public Builders.Any.BF<? extends Builders.Any.BF<?>> withBitmap();

    /**
     * Execute the request and write it to the given OutputStream.
     * The OutputStream will be closed upon finishing.
     * @param outputStream OutputStream to write the request
     * @return
     */
    public <T extends OutputStream> RequestFuture<T> write(T outputStream);

    /**
     * Execute the request and write it to the given OutputStream.
     * Specify whether the OutputStream will be closed upon finishing.
     * @param outputStream OutputStream to write the request
     * @param close Indicate whether the OutputStream should be closed on completion.
     * @return
     */
    public <T extends OutputStream> RequestFuture<T> write(T outputStream, boolean close);

    /**
     * Execute the request and write the results to a file
     * @param file File to write
     * @return
     */
    public RequestFuture<File> write(File file);

    /**
     * Deserialize the JSON request into a Java object of the given class using Gson.
     * @param <T>
     * @return
     */
    public <T> RequestFuture<T> as(Class<T> clazz);

    /**
     * Deserialize the JSON request into a Java object of the given class using Gson.
     * @param token
     * @param <T>
     * @return
     */
    public <T> RequestFuture<T> as(TypeToken<T> token);

    /**
     * Add this request to a group specified by groupKey. This key can be used in a later call to
     * Ion.cancelAll(groupKey) to cancel all the requests in the same group.
     * @param groupKey
     * @return
     */
    public FutureBuilder group(Object groupKey);
}
