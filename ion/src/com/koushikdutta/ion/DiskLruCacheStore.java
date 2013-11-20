package com.koushikdutta.ion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.async.http.libcore.DiskLruCache;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.DocumentParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.async.stream.InputStreamDataEmitter;
import com.koushikdutta.async.stream.OutputStreamDataSink;
import com.koushikdutta.ion.gson.GsonParser;
import com.koushikdutta.ion.gson.GsonSerializer;

import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by koush on 11/17/13.
 */
public class DiskLruCacheStore {
    Ion ion;
    DiskLruCache cache;
    DiskLruCacheStore(Ion ion, DiskLruCache cache) {
        this.ion = ion;
        this.cache = cache;
    }

    private <T> Future<T> put(final String rawKey, final T value, final AsyncParser<T> parser) {
        final SimpleFuture<T> ret = new SimpleFuture<T>();
        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final DiskLruCache.Editor editor;
                try {
                    final String key = ResponseCacheMiddleware.toKeyString("ion-store:" + rawKey);
                    editor = cache.edit(key);
                }
                catch (Exception e) {
                    ret.setComplete(e);
                    return;
                }
                final OutputStream out;
                try {
                    out = editor.newOutputStream(0);
                    for (int i = 1; i < cache.getValueCount(); i++) {
                        editor.newOutputStream(i).close();
                    }
                }
                catch (Exception e) {
                    try {
                        editor.abort();
                    }
                    catch (Exception ex) {
                    }
                    ret.setComplete(e);
                    return;
                }

                if (editor == null) {
                    ret.setComplete(new Exception("unable to edit"));
                    return;
                }
                parser.write(new OutputStreamDataSink(ion.getServer(), out), value, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex == null) {
                            try {
                                out.close();
                                editor.commit();
                                ret.setComplete(value);
                                return;
                            }
                            catch (Exception e) {
                                ex = e;
                            }
                        }
                        try {
                            editor.abort();
                        }
                        catch (Exception e) {
                        }
                        ret.setComplete(ex);
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
        
        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String key = ResponseCacheMiddleware.toKeyString("ion-store:" + rawKey);
                    final DiskLruCache.Snapshot snapshot = cache.get(key);
                    if (snapshot == null) {
                        ret.setComplete((T)null);
                        return;
                    }
                    InputStream inputStream = snapshot.getInputStream(0);
                    InputStreamDataEmitter emitter = new InputStreamDataEmitter(ion.getServer(), inputStream);
                    parser.parse(emitter).setCallback(new FutureCallback<T>() {
                        @Override
                        public void onCompleted(Exception e, T result) {
                            snapshot.close();
                            if (e != null)
                                ret.setComplete(e);
                            else
                                ret.setComplete(result);
                        }
                    });
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
        ion.getServer().getExecutorService().execute(new Runnable() {
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
