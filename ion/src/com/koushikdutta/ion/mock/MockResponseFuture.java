package com.koushikdutta.ion.mock;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.ion.HeadersResponse;
import com.koushikdutta.ion.Response;
import com.koushikdutta.ion.ResponseServedFrom;
import com.koushikdutta.ion.future.ResponseFuture;

/**
 * Created by koush on 3/6/15.
 */
public class MockResponseFuture<T> extends SimpleFuture<T> implements ResponseFuture<T> {
    private AsyncHttpRequest request;
    public MockResponseFuture(AsyncHttpRequest request) {
        this.request = request;
    }

    protected Headers getHeaders() {
        return new Headers();
    }

    protected HeadersResponse getHeadersResponse() {
        return new HeadersResponse(200, "OK", getHeaders());
    }

    private Response<T> getResponse(Exception e, T result) {
        return new Response<T>(request, ResponseServedFrom.LOADED_FROM_NETWORK, getHeadersResponse(), e, result);
    }

    @Override
    public Future<Response<T>> withResponse() {
        final SimpleFuture<Response<T>> ret = new SimpleFuture<Response<T>>();
        setCallback(new FutureCallback<T>() {
            @Override
            public void onCompleted(Exception e, T result) {
                ret.setComplete(getResponse(e, result));
            }
        });
        ret.setParent(this);
        return ret;
    }
}
