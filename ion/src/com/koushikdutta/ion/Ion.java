package com.koushikdutta.ion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.ion.loader.ContentLoader;
import com.koushikdutta.ion.loader.FileLoader;
import com.koushikdutta.ion.loader.HttpLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
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

    public Context getContext() {
        return context;
    }

    Context context;
    ResponseCacheMiddleware responseCache;
    AsyncHttpClient httpClient;
    IonBitmapCache bitmapCache;
    private Ion(Context context) {
        httpClient = new AsyncHttpClient(new AsyncServer());
        this.context = context.getApplicationContext();

        try {
            responseCache = ResponseCacheMiddleware.addCache(httpClient, new File(context.getCacheDir(), "ion"), 10L * 1024L * 1024L);
        }
        catch (Exception e) {
            IonLog.w("unable to set up response cache", e);
        }

        bitmapCache = new IonBitmapCache(this);

        configure()
        .addLoader(new HttpLoader())
        .addLoader(new ContentLoader())
        .addLoader(new FileLoader());
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

    Hashtable<ImageView, String> pendingViews = new Hashtable<ImageView, String>();
    Hashtable<String, ArrayList<SimpleFuture<BitmapDrawable>>> pendingDownloads = new Hashtable<String, ArrayList<SimpleFuture<BitmapDrawable>>>();

    private static Ion instance;
}
