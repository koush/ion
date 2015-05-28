package com.koushikdutta.ion;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.parser.AsyncParser;

import java.lang.reflect.Type;

/**
 * Created by koush on 5/27/15.
 */
class DataEmitterParser implements AsyncParser<DataEmitter> {
    @Override
    public Future<DataEmitter> parse(DataEmitter emitter) {
        SimpleFuture<DataEmitter> ret = new SimpleFuture<DataEmitter>();
        ret.setComplete(emitter);
        return ret;
    }

    @Override
    public void write(DataSink sink, DataEmitter value, CompletedCallback completed) {
        Util.pump(value, sink, completed);
    }

    @Override
    public Type getType() {
        return DataEmitter.class;
    }
}
