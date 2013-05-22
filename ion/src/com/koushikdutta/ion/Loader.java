package com.koushikdutta.ion;

import com.koushikdutta.async.future.FutureDataEmitter;
import com.koushikdutta.async.http.AsyncHttpRequest;

/**
 * Created by koush on 5/22/13.
 */
public interface Loader {
    // returns a Future if this loader can handle a request
    // otherwise it returns null, and Ion continues to the next loader.
    public FutureDataEmitter load(Ion ion, AsyncHttpRequest request);
}
