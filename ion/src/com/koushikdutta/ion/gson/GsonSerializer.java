package com.koushikdutta.ion.gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.async.stream.ByteBufferListInputStream;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

/**
 * Created by koush on 6/1/13.
 */
public class GsonSerializer<T> implements AsyncParser<T> {
    Gson gson;
    Type type;
    public GsonSerializer(Gson gson, Class<T> clazz) {
        this.gson = gson;
        type = clazz;
    }
    public GsonSerializer(Gson gson, TypeToken<T> token) {
        this.gson = gson;
        type = token.getType();
    }
    @Override
    public Future<T> parse(DataEmitter emitter) {
        return new ByteBufferListParser().parse(emitter)
        .thenConvert(from -> {
            ByteBufferListInputStream bin = new ByteBufferListInputStream(from);
            return  (T)gson.fromJson(new JsonReader(new InputStreamReader(bin)), type);
        });
    }

    @Override
    public void write(DataSink sink, T pojo, CompletedCallback completed) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStreamWriter out = new OutputStreamWriter(bout);
        gson.toJson(pojo, type, out);
        try {
            out.flush();
        }
        catch (final Exception e) {
            throw new AssertionError(e);
        }
        Util.writeAll(sink, bout.toByteArray(), completed);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getMime() {
        return "application/json";
    }
}
