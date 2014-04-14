package com.koushikdutta.ion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.DocumentParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.async.stream.FileDataSink;
import com.koushikdutta.async.stream.InputStreamDataEmitter;
import com.koushikdutta.async.stream.OutputStreamDataSink;
import com.koushikdutta.async.util.FileCache;
import com.koushikdutta.ion.gson.GsonParser;
import com.koushikdutta.ion.gson.GsonSerializer;

import org.w3c.dom.Document;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by koush on 11/17/13.
 */
public class FileCacheStore {
    Ion ion;
    FileCache cache;
    FileCacheStore(Ion ion, FileCache cache) {
        this.ion = ion;
        this.cache = cache;
    }

    private <T> Future<T> put(final String rawKey, final T value, final AsyncParser<T> parser) {
        final SimpleFuture<T> ret = new SimpleFuture<T>();
        Ion.getIoExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final String key = FileCache.toKeyString("ion-store:", rawKey);
                final File file = cache.getTempFile();
                final FileDataSink sink = new FileDataSink(ion.getServer(), file);
                parser.write(sink, value, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        sink.close();
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

    public Future<String> putString(String key, String value) {
        return put(key, value, new StringParser());
    }

    public Future<JsonObject> putJsonObject(String key, JsonObject value) {
        return put(key, value, new GsonParser<JsonObject>());
    }

    public Future<Document> putDocument(String key, Document value) {
        return put(key, value, new DocumentParser());
    }

    public Future<JsonArray> putJsonArray(String key, JsonArray value) {
        return put(key, value, new GsonParser<JsonArray>());
    }

    /*
    public Future<InputStream> putInputStream(InputStream value) {
        throw new AssertionError("not implemented");
    }

    public Future<byte[]> putByteArray(byte[] bytes) {
        throw new AssertionError("not implemented");
    }
    */

    public <T> Future<T> put(String key, T value, Class<T> clazz) {
        return put(key, value, new GsonSerializer<T>(ion.configure().getGson(), clazz));
    }

    public <T> Future<T> put(String key, T value, TypeToken<T> token) {
        return put(key, value, new GsonSerializer<T>(ion.configure().getGson(), token));
    }
    
    private <T> Future<T> get(final String rawKey, final AsyncParser<T> parser) {
        final SimpleFuture<T> ret = new SimpleFuture<T>();

        Ion.getIoExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String key = FileCache.toKeyString("ion-store:", rawKey);
                    final File file = cache.getFile(key);
                    if (!file.exists()) {
                        ret.setComplete((T)null);
                        return;
                    }
                    ion.build(ion.getContext(), file)
                    .as(parser)
                    .setCallback(ret.getCompletionCallback());
                }
                catch (Exception e) {
                }
            }
        });
        
        return ret;
    }
    
    public Future<String> getString(String key) {
        return get(key, new StringParser());
    }

    public Future<JsonObject> getJsonObject(String key) {
        return get(key, new GsonParser<JsonObject>());
    }

    public Future<JsonArray> getJsonArray(String key) {
        return get(key, new GsonParser<JsonArray>());
    }

    public Future<Document> getDocument(String key) {
        return get(key, new DocumentParser());
    }

    public <T> Future<T> get(String key, Class<T> clazz) {
        return get(key, new GsonSerializer<T>(ion.configure().getGson(), clazz));
    }

    public <T> Future<T> get(String key, TypeToken<T> token) {
        return get(key, new GsonSerializer<T>(ion.configure().getGson(), token));
    }

    public Future<String> remove(final String key) {
        final SimpleFuture<String> ret = new SimpleFuture<String>();
        Ion.getIoExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    cache.remove(key);
                    ret.setComplete(key);
                }
                catch (Exception e) {
                    ret.setComplete(e);
                }
            }
        });
        return ret;
    }
}
