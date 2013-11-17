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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by koush on 11/17/13.
 */
public class DiskLruCacheStore extends SimpleFuture {
    Ion ion;
    DiskLruCache cache;
    String key;
    DiskLruCacheStore(Ion ion, DiskLruCache cache, String key) {
        this.ion = ion;
        this.cache = cache;
        this.key = key;
    }

    private <T> Future<T> put(final T value, final AsyncParser<T> parser) {
        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final DiskLruCache.Editor editor;
                try {
                    final String key = ResponseCacheMiddleware.toKeyString("ion-store:" + DiskLruCacheStore.this.key);
                    editor = cache.edit(key);
                }
                catch (Exception e) {
                    setComplete(e);
                    return;
                }
                final OutputStream out;
                try {
                    out = editor.newOutputStream(0);
                }
                catch (Exception e) {
                    try {
                        editor.abort();
                    }
                    catch (Exception ex) {
                    }
                    setComplete(e);
                    return;
                }

                if (editor == null) {
                    setComplete(new Exception("unable to edit"));
                    return;
                }
                parser.write(new OutputStreamDataSink(ion.getServer(), out), value, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex == null) {
                            try {
                                out.close();
                                editor.commit();
                                setComplete(value);
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
                        setComplete(ex);
                    }
                });
            }
        });
        return this;
    }

    public Future<String> putString(String value) {
        return put(value, new StringParser());
    }

    public Future<JsonObject> putJsonObject(JsonObject value) {
        return put(value, new GsonParser<JsonObject>());
    }

    public Future<Document> putDocument(Document value) {
        return put(value, new DocumentParser());
    }

    public Future<JsonArray> putJsonArray(JsonArray value) {
        return put(value, new GsonParser<JsonArray>());
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
    
    private <T> Future<T> get(final AsyncParser<T> parser) {
        final SimpleFuture<T> ret = new SimpleFuture<T>();
        
        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String key = ResponseCacheMiddleware.toKeyString("ion-store:" + DiskLruCacheStore.this.key);
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
    
    public Future<String> getString() {
        return get(new StringParser());
    }

    public Future<JsonObject> getJsonObject() {
        return get(new GsonParser<JsonObject>());
    }

    public Future<JsonArray> getJsonArray() {
        return get(new GsonParser<JsonArray>());
    }

    public Future<Document> getDocument() {
        return get(new DocumentParser());
    }

    public <T> Future<T> get(Class<T> clazz) {
        return get(new GsonSerializer<T>(ion.configure().getGson(), clazz));
    }

    public <T> Future<T> get(TypeToken<T> token) {
        return get(new GsonSerializer<T>(ion.configure().getGson(), token));
    }
}
