package com.koushikdutta.ion;

import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.libcore.RawHeaders;

/**
 * Created by koush on 7/6/13.
 */
public class Response<T> {
    T result;
    public T getResult() {
        return result;
    }

    RawHeaders headers;
    public RawHeaders getHeaders() {
        return headers;
    }

    AsyncHttpRequest request;
    public AsyncHttpRequest getRequest() {
        return request;
    }
}
