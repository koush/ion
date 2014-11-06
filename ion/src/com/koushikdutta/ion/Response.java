package com.koushikdutta.ion;

import com.koushikdutta.async.http.AsyncHttpRequest;

/**
 * Created by koush on 7/6/13.
 */
public class Response<T> {
    AsyncHttpRequest request;
    public AsyncHttpRequest getRequest() {
        return request;
    }

    T result;
    public T getResult() {
        return result;
    }

    Exception exception;
    public Exception getException() {
        return exception;
    }

    HeadersResponse headers;
    public HeadersResponse getHeaders() {
        return headers;
    }
}
