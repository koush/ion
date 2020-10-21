package com.koushikdutta.ion.future;

import com.koushikdutta.scratch.Promise;
import com.koushikdutta.ion.Response;

/**
 * Created by koush on 7/2/13.
 */
public class ResponseFuture<T> extends Promise<T> {
    private Promise<Response<T>> response;
    public ResponseFuture(kotlinx.coroutines.Deferred<? extends T> wrappedDeferred, Promise<Response<T>> response) {
        super(wrappedDeferred);
        this.response = response;
    }

    public Promise<Response<T>> withResponse() {
        return response;
    }
}
