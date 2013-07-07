package com.koushikdutta.ion.future;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.Response;

/**
 * Created by koush on 7/2/13.
 */
public interface ResponseFuture<T> extends Future<T> {
    Future<Response<T>> withResponse();
}
