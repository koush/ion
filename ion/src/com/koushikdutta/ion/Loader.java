package com.koushikdutta.ion;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.libcore.RawHeaders;

import java.io.InputStream;

/**
 * Created by koush on 5/22/13.
 */
public interface Loader {
    public static class LoaderEmitter {
        public static final int LOADED_FROM_MEMORY = 0;
        public static final int LOADED_FROM_CACHE = 1;
        public static final int LOADED_FROM_CONDITIONAL_CACHE = 2;
        public static final int LOADED_FROM_NETWORK = 3;

        public LoaderEmitter(DataEmitter emitter, int length, int loadedFrom, RawHeaders headers, AsyncHttpRequest request) {
            this.length = length;
            this.emitter = emitter;
            this.loadedFrom = loadedFrom;
            this.headers = headers;
            this.request = request;
        }
        DataEmitter emitter;
        int length;
        public DataEmitter getDataEmitter() {
            return emitter;
        }
        public int length() {
            return length;
        }
        int loadedFrom;
        public int loadedFrom() {
            return loadedFrom;
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

    // returns a Future if this loader can handle a request as a stream.
    // this implies that the stream is essentially non blocking...
    // ie file or memory based.
    public Future<InputStream> load(Ion ion, AsyncHttpRequest request);

    // returns a Future if this loader can handle a request
    // otherwise it returns null, and Ion continues to the next loader.
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request, FutureCallback<LoaderEmitter> callback);
}
