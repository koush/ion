package com.koushikdutta.ion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.DocumentParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.async.stream.FileDataSink;
import com.koushikdutta.async.util.FileCache;
import com.koushikdutta.ion.gson.GsonArrayParser;
import com.koushikdutta.ion.gson.GsonObjectParser;
import com.koushikdutta.ion.gson.GsonSerializer;

import org.w3c.dom.Document;

import java.io.File;

/**
 * Created by koush on 11/17/13.
 */
public class FileCacheStore {
    Ion ion;
    FileCache cache;
    String rawKey;
    FileCacheStore(Ion ion, FileCache cache, String rawKey) {
        this.ion = ion;
        this.cache = cache;
        this.rawKey = rawKey;
    }

    private <T> Future<T> put(final T value, final AsyncParser<T> parser) {
        final SimpleFuture<T> ret = new SimpleFuture<T>();
        Ion.getIoExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final String key = computeKey();
                final File file = cache.getTempFile();
                final FileDataSink sink = new FileDataSink(ion.getServer(), file);
                parser.write(sink, value, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        sink.end();
                        if (ex != null) {
                            file.delete();
                            ret.setComplete(ex);
                            return;
                        }
                        cache.commitTempFiles(key, file);
                        ret.setComplete(value);
                    }
                });
            }
        });
        return ret;
    }

    public Future<String> putString(String value) {
        return put(value, new StringParser());
    }

    public Future<JsonObject> putJsonObject(JsonObject value) {
        return put(value, new GsonObjectParser());
    }

    public Future<Document> putDocument(Document value) {
        return put(value, new DocumentParser());
    }

    public Future<JsonArray> putJsonArray(JsonArray value) {
        return put(value, new GsonArrayParser());
    }

    /*
    public Future<InputStream> putInputStream(InputStream value) {
        throw new AssertionError("not implemented");
    }

    public Future<byte[]> putByteArray(byte[] bytes) {
        throw new AssertionError("not implemented");
    }
    */

    public <T> Future<T> put(T value, Class<T> clazz) {
        return put(value, new GsonSerializer<T>(ion.configure().getGson(), clazz));
    }

    public <T> Future<T> put(T value, TypeToken<T> token) {
        return put(value, new GsonSerializer<T>(ion.configure().getGson(), token));
    }

    private <T> Future<T> as(final AsyncParser<T> parser) {
        final SimpleFuture<T> ret = new SimpleFuture<T>();

        Ion.getIoExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String key = computeKey();
                    final File file = cache.getFile(key);
                    if (!file.exists()) {
                        ret.setComplete((T)null);
                        return;
                    }
                    ion.build(ion.getContext()).load(file)
                    .as(parser)
                    .setCallback(ret.getCompletionCallback());
                }
                catch (Exception e) {
                    ret.setComplete(e);
                }
            }
        });
        
        return ret;
    }

    private <T> T get(final AsyncParser<T> parser) {
        try {
            final String key = computeKey();
            final File file = cache.getFile(key);
            return ion.build(ion.getContext()).load(file)
            .as(parser)
            .get();
        }
        catch (Exception e) {
            return null;
        }
    }

    public String getString() {
        return get(new StringParser());
    }

    public Future<String> asString() {
        return as(new StringParser());
    }

    public Future<JsonObject> asJsonObject() {
        return as(new GsonObjectParser());
    }

    public JsonObject getJsonObject() {
        return get(new GsonObjectParser());
    }

    public Future<JsonArray> asJsonArray() {
        return as(new GsonArrayParser());
    }

    public JsonArray getJsonArray() {
        return get(new GsonArrayParser());
    }

    public Future<Document> asDocument() {
        return as(new DocumentParser());
    }

    public Document getDocument() {
        return get(new DocumentParser());
    }

    public <T> Future<T> as(Class<T> clazz) {
        return as(new GsonSerializer<T>(ion.configure().getGson(), clazz));
    }

    public <T> T get(Class<T> clazz) {
        return get(new GsonSerializer<T>(ion.configure().getGson(), clazz));
    }

    public <T> Future<T> as(TypeToken<T> token) {
        return as(new GsonSerializer<T>(ion.configure().getGson(), token));
    }

    public <T> T get(TypeToken<T> token) {
        return get(new GsonSerializer<T>(ion.configure().getGson(), token));
    }

    private String computeKey() {
        return rawKey.replace(":", "_");
    }

    public void remove() {
        final String key = computeKey();
        cache.remove(key);
    }
}
