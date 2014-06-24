package com.koushikdutta.ion.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.JSONObjectBody;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

public class GsonBody<T extends JsonElement> implements AsyncHttpRequestBody<T> {
    byte[] mBodyBytes;
    T json;
    Gson gson;
    public GsonBody(Gson gson, T json) {
        this.json = json;
        this.gson = gson;
    }

    @Override
    public void parse(DataEmitter emitter, final CompletedCallback completed) {
        throw new AssertionError("not implemented");
    }

    @Override
    public void write(AsyncHttpRequest request, DataSink sink, final CompletedCallback completed) {
        if (mBodyBytes == null) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            OutputStreamWriter out = new OutputStreamWriter(bout);
            gson.toJson(json, out);
            mBodyBytes = bout.toByteArray();
        }
        Util.writeAll(sink, mBodyBytes, completed);
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        if (mBodyBytes == null)
            mBodyBytes = json.toString().getBytes();
        return mBodyBytes.length;
    }

    public static final String CONTENT_TYPE = JSONObjectBody.CONTENT_TYPE;

    @Override
    public T get() {
        return json;
    }
}

