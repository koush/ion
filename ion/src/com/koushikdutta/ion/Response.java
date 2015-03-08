package com.koushikdutta.ion;

import com.koushikdutta.async.http.AsyncHttpRequest;

/**
 * Created by koush on 7/6/13.
 */
public class Response<T> {
    public Response(AsyncHttpRequest request, ResponseServedFrom servedFrom, HeadersResponse headers, Exception e, T result) {
        this.request = request;
        this.servedFrom = servedFrom;
        this.headers = headers;
        this.exception = e;
        this.result = result;
    }

    private ResponseServedFrom servedFrom;
    public ResponseServedFrom getServedFrom() {
        return servedFrom;
    }

    private AsyncHttpRequest request;
    public AsyncHttpRequest getRequest() {
        return request;
    }

    private T result;
    public T getResult() {
        return result;
    }

    private Exception exception;
    public Exception getException() {
        return exception;
    }

    private HeadersResponse headers;
    public HeadersResponse getHeaders() {
        return headers;
    }
}
