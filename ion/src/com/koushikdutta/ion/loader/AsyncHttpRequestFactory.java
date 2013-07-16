package com.koushikdutta.ion.loader;

import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.libcore.RawHeaders;

import java.net.URI;

/**
 * Created by koush on 7/15/13.
 */
public interface AsyncHttpRequestFactory {
    public AsyncHttpRequest createAsyncHttpRequest(URI uri, String method, RawHeaders headers);
}
