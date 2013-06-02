package com.koushikdutta.ion.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.async.stream.ByteBufferListInputStream;

import java.io.InputStreamReader;

/**
 * Created by koush on 5/27/13.
 */
public class GsonParser<T extends JsonElement> implements AsyncParser<T> {
    public GsonParser() {
    }
    @Override
    public Future<T> parse(DataEmitter emitter) {
        return new TransformFuture<T, ByteBufferList>() {
            @Override
            protected void transform(ByteBufferList result) throws Exception {
                JsonParser parser = new JsonParser();
                setComplete(null, (T)parser.parse(new JsonReader(new InputStreamReader(new ByteBufferListInputStream(result)))));
            }
        }
        .from(new ByteBufferListParser().parse(emitter));
    }

    @Override
    public void write(DataSink sink, T value, CompletedCallback completed) {
        new StringParser().write(sink, value.toString(), completed);
    }
}
