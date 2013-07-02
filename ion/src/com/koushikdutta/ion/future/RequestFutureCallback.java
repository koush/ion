package com.koushikdutta.ion.future;

import com.koushikdutta.async.http.libcore.RawHeaders;

/**
 * Created by koush on 7/2/13.
 */
public interface RequestFutureCallback<T> {
    public void onCompleted(Exception e, RawHeaders headers, T result);
}
