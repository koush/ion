package com.koushikdutta.ion.future;

import com.koushikdutta.async.future.Future;

/**
 * Created by koush on 7/2/13.
 */
public interface RequestFuture<T> extends Future<T> {
    RequestFuture<T> setRequestCallback(RequestFutureCallback<T> callback);
}
