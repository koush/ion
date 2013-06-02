package com.koushikdutta.ion.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpRequestBody;
import com.koushikdutta.async.http.AsyncHttpResponse;

public class GsonBody<T extends JsonElement> implements AsyncHttpRequestBody<T> {
    public GsonBody() {
    }

    byte[] mBodyBytes;
    T json;
    public GsonBody(T json) {
        this();
        this.json = json;
    }

    @Override
    public void parse(DataEmitter emitter, final CompletedCallback completed) {
        new GsonParser<T>().parse(emitter).setCallback(new FutureCallback<T>() {
            @Override
            public void onCompleted(Exception e, T result) {
                json = result;
                completed.onCompleted(e);
            }
        });
    }

    @Override
    public void write(AsyncHttpRequest request, AsyncHttpResponse sink) {
        Util.writeAll(sink, mBodyBytes, null);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        mBodyBytes = json.toString().getBytes();
        return mBodyBytes.length;
    }

    public static final String CONTENT_TYPE = "application/json";

    @Override
    public T get() {
        return json;
    }
}

