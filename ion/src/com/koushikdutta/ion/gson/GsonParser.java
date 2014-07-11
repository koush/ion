package com.koushikdutta.ion.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
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
import com.koushikdutta.async.util.Charsets;

import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Created by koush on 5/27/13.
 */
public abstract class GsonParser<T extends JsonElement> implements AsyncParser<T> {
    Class<? extends JsonElement> clazz;
    public GsonParser(Class<? extends T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Future<T> parse(DataEmitter emitter) {
        final String charset = emitter.charset() == null ? Charset.defaultCharset().name() : emitter.charset();
        return new ByteBufferListParser().parse(emitter)
        .then(new TransformFuture<T, ByteBufferList>() {
            @Override
            protected void transform(ByteBufferList result) throws Exception {
                JsonParser parser = new JsonParser();
                JsonElement parsed = parser.parse(new JsonReader(new InputStreamReader(new ByteBufferListInputStream(result), charset)));
                if (parsed.isJsonNull() || parsed.isJsonPrimitive())
                    throw new JsonParseException("unable to parse json");
                if (!clazz.isInstance(parsed))
                    throw new ClassCastException(parsed.getClass().getCanonicalName() + " can not be casted to " + clazz.getCanonicalName());
                setComplete(null, (T)parsed);
            }
        });
    }

    @Override
    public void write(DataSink sink, T value, CompletedCallback completed) {
        new StringParser().write(sink, value.toString(), completed);
    }
}
