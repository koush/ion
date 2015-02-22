package com.koushikdutta.ion.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.future.ResponseFuture;

import java.nio.charset.Charset;

/**
 * Created by koush on 3/10/14.
 */
public interface GsonFutureBuilder {
    /**
     * Execute the request and get the result as a (Gson) JsonArray
     * @return
     */
    public ResponseFuture<JsonArray> asJsonArray();

    /**
     * Execute the request and get the result as a (Gson) JsonObject
     * @return
     */
    public ResponseFuture<JsonObject> asJsonObject();

    /**
     * Execute the request and get the result as a (Gson) JsonArray
     * @param charset Decode using the specified charset
     * @return
     */
    public ResponseFuture<JsonArray> asJsonArray(Charset charset);

    /**
     * Execute the request and get the result as a (Gson) JsonObject
     * @param charset Decode using the specified charset
     * @return
     */
    public ResponseFuture<JsonObject> asJsonObject(Charset charset);

    /**
     * Deserialize the JSON request into a Java object of the given class using Gson.
     * @param <T>
     * @return
     */
    public <T> ResponseFuture<T> as(Class<T> clazz);

    /**
     * Deserialize the JSON request into a Java object of the given class using Gson.
     * @param token
     * @param <T>
     * @return
     */
    public <T> ResponseFuture<T> as(TypeToken<T> token);
}
