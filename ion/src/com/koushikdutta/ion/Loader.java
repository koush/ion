package com.koushikdutta.ion;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;

/**
 * Created by koush on 5/22/13.
 */
public interface Loader {
    public static class LoaderEmitter {
        public LoaderEmitter(DataEmitter emitter, int length) {
            this.length = length;
            this.emitter = emitter;
        }
        DataEmitter emitter;
        int length;
        public DataEmitter getDataEmitter() {
            return emitter;
        }
        public int length() {
            return length;
        }
    }
    // returns a Future if this loader can handle a request
    // otherwise it returns null, and Ion continues to the next loader.
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request, FutureCallback<LoaderEmitter> callback);
}
