package com.koushikdutta.ion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.ion.bitmap.Transform;
import com.koushikdutta.ion.loader.ContentLoader;
import com.koushikdutta.ion.loader.FileLoader;
import com.koushikdutta.ion.loader.HttpLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

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

    // map an ImageView to the url being downloaded for it.
    // but don't hold references to the ImageView...
    WeakHashMap<ImageView, String> pendingViews = new WeakHashMap<ImageView, String>();


    // track the downloads and transforms that are pending.
    // but don't maintain a reference.
    // The reference stays alive because the reference chain looks as follows:
    // AsyncServer -> AsyncHttpClient -> IonRequestBuilder.execute future ->
    // bitmap decode future -> transform future -> drawable future ->
    // asBitmap/intoImageView future
    // Thus, the reference to the future will exist, so long as it is reachable
    // by a callback; ie, it was not cancelled.
    WeakReferenceHashTable<String, IonBitmapRequestBuilder.ByteArrayToBitmapFuture> pendingDownloads = new WeakReferenceHashTable<String, IonBitmapRequestBuilder.ByteArrayToBitmapFuture>();
    WeakReferenceHashTable<String, IonBitmapRequestBuilder.BitmapToBitmap> pendingTransforms = new WeakReferenceHashTable<String, IonBitmapRequestBuilder.BitmapToBitmap>();

    private static Ion instance;
}
