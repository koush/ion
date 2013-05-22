package com.koushikdutta.ion;

import android.content.Context;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.ion.loader.HttpLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koush on 5/21/13.
 */
public class Ion {
    /***
     * Get the default Ion object instance.
     * @param context
     * @return
     */
    public static IonRequestBuilderStages.IonLoadRequestBuilder with(Context context) {
        if (instance == null)
            instance = new Ion(context);
        return instance.build(context);
    }

    public IonRequestBuilderStages.IonLoadRequestBuilder build(Context context) {
        return new IonRequestBuilder(context, this);
    }

    ResponseCacheMiddleware responseCache;
    AsyncHttpClient httpClient;
    private Ion(Context context) {
        httpClient = new AsyncHttpClient(new AsyncServer());

        try {
            responseCache = ResponseCacheMiddleware.addCache(httpClient, new File(context.getCacheDir(), "ion"), 10L * 1024L * 1024L);
        }
        catch (Exception e) {
            IonLog.w("unable to set up response cache", e);
        }

        configure()
            .addLoader(new HttpLoader());
    }

    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }

    public class Config {
        ArrayList<Loader> loaders = new ArrayList<Loader>();
        public Config addLoader(int index, Loader loader) {
            loaders.add(index, loader);
            return this;
        }
        public Config insertLoader(Loader loader) {
            loaders.add(0, loader);
            return this;
        }
        public Config addLoader(Loader loader) {
            loaders.add(loader);
            return this;
        }
        public List<Loader> getLoaders() {
            return loaders;
        }
    }

    Config config = new Config();
    public Config configure() {
        return config;
    }

    private static Ion instance;
}
