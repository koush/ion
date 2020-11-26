package com.koushikdutta.ion;

import android.content.Context;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.future.ResponseFuture;

import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Created by koush on 5/22/13.
 */
public interface Loader {
    public static class LoaderEmitter {
        public LoaderEmitter(DataEmitter emitter, long length, ResponseServedFrom servedFrom,
                             HeadersResponse headers,
                             AsyncHttpRequest request) {
            this.length = length;
            this.emitter = emitter;
            this.servedFrom = servedFrom;
            this.headers = headers;
            this.request = request;
        }
        DataEmitter emitter;
        long length;
        public DataEmitter getDataEmitter() {
            return emitter;
        }
        public long length() {
            return length;
        }
        ResponseServedFrom servedFrom;
        public ResponseServedFrom getServedFrom() {
            return servedFrom;
        }
        HeadersResponse headers;
        public HeadersResponse getHeaders() {
            return headers;
        }
        AsyncHttpRequest request;
        public AsyncHttpRequest getRequest() {
            return request;
        }
    }

    /**
     * returns a Future if this loader can handle a request
     * otherwise it returns null, and Ion continues to the next loader.
     * @param ion
     * @param request
     * @param callback
     * @return
     */
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request, FutureCallback<LoaderEmitter> callback);

    /**
     * returns a future if the laoder can handle the request as a bitmap
     * otherwise it returns null
     * @param ion
     * @param key
     * @param uri
     * @param resizeWidth
     * @param resizeHeight
     * @return
     */
    public Future<BitmapInfo> loadBitmap(Context context, Ion ion, String key, String uri, int resizeWidth, int resizeHeight, boolean animateGif);

    /**
     * Resolve a request into another request.
     * @param ion
     * @param request
     * @return
     */
    public Future<AsyncHttpRequest> resolve(Context context, Ion ion, AsyncHttpRequest request);

    public <T> ResponseFuture<T> load(Ion ion, AsyncHttpRequest request, Type type);
}
