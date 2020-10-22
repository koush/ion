package com.koushikdutta.ion.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.nio.charset.Charset;

/**
 * Created by koush on 3/10/14.
 */
public interface GsonFutureBuilder {
    /**
     * Execute the request and get the result as a (Gson) JsonArray
     * @return
     */
    public ResponsePromise<JsonArray> asJsonArray();

    /**
     * Execute the request and get the result as a (Gson) JsonObject
     * @return
     */
    public ResponsePromise<JsonObject> asJsonObject();

    /**
     * Execute the request and get the result as a (Gson) JsonArray
     * @param charset Decode using the specified charset
     * @return
     */
    public ResponsePromise<JsonArray> asJsonArray(Charset charset);

    /**
     * Execute the request and get the result as a (Gson) JsonObject
     * @param charset Decode using the specified charset
     * @return
     */
    public ResponsePromise<JsonObject> asJsonObject(Charset charset);

    /**
     * Deserialize the JSON request into a Java object of the given class using Gson.
     * @param <T>
     * @return
     */
    public <T> ResponsePromise<T> as(Class<T> clazz);

    /**
     * Deserialize the JSON request into a Java object of the given class using Gson.
     * @param token
     * @param <T>
     * @return
     */
    public <T> ResponsePromise<T> as(TypeToken<T> token);
}
