package com.koushikdutta.ion.builder;

import com.koushikdutta.async.future.Future;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;

/**
* Created by koush on 5/30/13.
*/ // get the result, transformed to how you want it
public interface IonFutureRequestBuilder extends IonBitmapFutureRequestBuilder, IonBitmapImageViewFutureRequestBuilder {
    /**
     * Execute the request and get the result as a String
     * @return
     */
    public Future<String> asString();

    /**
     * Execute the request and get the result as a JSONObject
     * @return
     */
    public Future<JSONObject> asJSONObject();

    /**
     * Execute the request and get the result as a JSONArray
     * @return
     */
    public Future<JSONArray> asJSONArray();

    /**
     * Use the request as a Bitmap which can then be modified and/or applied to an ImageView.
     * @return
     */
    public IonMutableBitmapRequestBuilder withBitmap();

    /**
     * Execute the request and write it to the given OutputStream.
     * The OutputStream will be closed upon finishing.
     * @param outputStream OutputStream to write the request
     * @return
     */
    public <T extends OutputStream> Future<T> write(T outputStream);

    /**
     * Execute the request and write it to the given OutputStream.
     * Specify whether the OutputStream will be closed upon finishing.
     * @param outputStream OutputStream to write the request
     * @param close Indicate whether the OutputStream should be closed on completion.
     * @return
     */
    public <T extends OutputStream> Future<T> write(T outputStream, boolean close);

    /**
     * Execute the request and write the results to a file
     * @param file File to write
     * @return
     */
    public Future<File> write(File file);
}
