package com.koushikdutta.ion;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.http.server.AsyncHttpRequestBodyBase;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by koush on 5/22/13.
 */
class ByteArrayBody extends AsyncHttpRequestBodyBase<byte[]> {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        while (bb.size() > 0) {
            ByteBuffer b = bb.remove();
            bout.write(b.array(), b.arrayOffset() + b.position(), b.remaining());
        }
    }

    public ByteArrayBody() {
        super("image/bitmap");
    }

    @Override
    public byte[] get() {
        return bout.toByteArray();
    }
}
