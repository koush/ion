package com.koushikdutta.ion;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.async.stream.ByteBufferListInputStream;

import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Created by koush on 11/3/13.
 */
class InputStreamParser implements AsyncParser<InputStream> {
    @Override
    public Future<InputStream> parse(DataEmitter emitter) {
        return new ByteBufferListParser().parse(emitter)
        .then(new TransformFuture<InputStream, ByteBufferList>() {
            @Override
            protected void transform(ByteBufferList result) throws Exception {
                setComplete(new ByteBufferListInputStream(result));
            }
        });
    }

    @Override
    public void write(DataSink sink, InputStream value, CompletedCallback completed) {
        throw new AssertionError("not implemented");
    }

    @Override
    public Type getType() {
        return InputStream.class;
    }
}
