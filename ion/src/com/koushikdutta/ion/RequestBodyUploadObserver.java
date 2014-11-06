package com.koushikdutta.ion;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;

import java.nio.ByteBuffer;

/**
 * Created by koush on 6/13/13.
 */
class RequestBodyUploadObserver implements AsyncHttpRequestBody {
    AsyncHttpRequestBody body;
    ProgressCallback callback;
    public RequestBodyUploadObserver(AsyncHttpRequestBody body, ProgressCallback callback) {
        this.body = body;
        this.callback = callback;
    }

    @Override
    public void write(AsyncHttpRequest request, final DataSink sink, final CompletedCallback completed) {
        final int length = body.length();
        body.write(request, new DataSink() {
            int totalWritten;

            @Override
            public void write(ByteBufferList bb) {
                int start = bb.remaining();
                sink.write(bb);
                int wrote = start - bb.remaining();
                totalWritten += wrote;
                callback.onProgress(totalWritten, length);
            }

            @Override
            public void setWriteableCallback(WritableCallback handler) {
                sink.setWriteableCallback(handler);
            }

            @Override
            public WritableCallback getWriteableCallback() {
                return sink.getWriteableCallback();
            }

            @Override
            public boolean isOpen() {
                return sink.isOpen();
            }

            @Override
            public void end() {
                sink.end();
            }

            @Override
            public void setClosedCallback(CompletedCallback handler) {
                sink.setClosedCallback(handler);
            }

            @Override
            public CompletedCallback getClosedCallback() {
                return sink.getClosedCallback();
            }

            @Override
            public AsyncServer getServer() {
                return sink.getServer();
            }
        }, completed);
    }

    @Override
    public void parse(DataEmitter emitter, CompletedCallback completed) {
        body.parse(emitter, completed);
    }

    @Override
    public String getContentType() {
        return body.getContentType();
    }

    @Override
    public boolean readFullyOnRequest() {
        return body.readFullyOnRequest();
    }

    @Override
    public int length() {
        return body.length();
    }

    @Override
    public Object get() {
        return body.get();
    }
}
