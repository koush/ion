package com.koushikdutta.ion;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.ResponseCacheMiddleware;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.util.HashList;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.FutureBuilder;
import com.koushikdutta.ion.builder.LoadBuilder;
import com.koushikdutta.ion.cookie.CookieMiddleware;
import com.koushikdutta.ion.loader.AsyncHttpRequestFactory;
import com.koushikdutta.ion.loader.ContentLoader;
import com.koushikdutta.ion.loader.FileLoader;
import com.koushikdutta.ion.loader.HttpLoader;

/**
 * Created by koush on 5/21/13.
 */
public class Ion {
    public static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Get the default Ion object instance and begin building a request
     * with the given uri
     * @param context
     * @param uri
     * @return
     */
    public static Builders.Any.B with(Context context, String uri) {
        return getDefault(context).build(context, uri);
    }

    /**
     * Get the default Ion object instance and begin building a request
     * @param context
     * @return
     */
    public static LoadBuilder<Builders.Any.B> with(Context context) {
        return getDefault(context).build(context);
    }

    /**
     * Get the default Ion object instance and begin building an operation
     * on the given file
     * @param context
     * @param file
     * @return
     */
    public static FutureBuilder with(Context context, File file) {
        return getDefault(context).build(context, file);
    }

    /**
     * Begin building an operation on the given file
     * @param context
     * @param file
     * @return
     */
    public FutureBuilder build(Context context, File file) {
        return new IonRequestBuilder(context, this).load(file);
    }

    /**
     * Get the default Ion instance
     * @param context
     * @return
     */
    public static Ion getDefault(Context context) {
        if (instance == null)
            instance = new Ion(context, "ion");
        return instance;
    }

    /**
     * Create a ImageView bitmap request builder
     * @param imageView
     * @return
     */
    public static Builders.ImageView.F<? extends Builders.ImageView.F<?>> with(ImageView imageView) {
        Ion ion = getDefault(imageView.getContext());
        return ion.build(imageView);
    }

    /**
     * Begin building a request with the given uri
     * @param context
     * @param uri
     * @return
     */
    public Builders.Any.B build(Context context, String uri) {
        return new IonRequestBuilder(context, this).load(uri);
    }

    /**
     * Begin building a request
     * @param context
     * @return
     */
    public LoadBuilder<Builders.Any.B> build(Context context) {
        return new IonRequestBuilder(context, this);
    }

    IonBitmapRequestBuilder bitmapBuilder = new IonBitmapRequestBuilder(this);
    /**
     * Create a builder that can be used to build an network request
     * @param imageView
     * @return
     */
    public Builders.ImageView.F<? extends Builders.ImageView.F<?>> build(ImageView imageView) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread())
            throw new IllegalStateException("must be called from UI thread");
        bitmapBuilder.reset();
        bitmapBuilder.ion = this;
        return bitmapBuilder.withImageView(imageView);
    }

    /**
     * Cancel all pending requests associated with the request group
     * @param group
     */
    public void cancelAll(Object group) {
        FutureSet members;
        synchronized (this) {
            members = inFlight.remove(group);
        }

        if (members == null)
            return;

        for (Future future: members.keySet()) {
            if (future != null)
                future.cancel();
        }
    }

    /**
     * Route all http requests through the given proxy.
     * @param host
     * @param port
     */
    public void proxy(String host, int port) {
        httpClient.getSocketMiddleware().enableProxy(host, port);
    }

    /**
     * Route all https requests through the given proxy.
     * Note that https proxying requires that the Android device has the appropriate
     * root certificate installed to function properly.
     * @param host
     * @param port
     */
    public void proxySecure(String host, int port) {
        httpClient.getSSLSocketMiddleware().enableProxy(host, port);
    }

    /**
     * Disable routing of http requests through a previous provided proxy
     */
    public void disableProxy() {
        httpClient.getSocketMiddleware().disableProxy();
    }

    /**
     * Disable routing of https requests through a previous provided proxy
     */
    public void disableSecureProxy() {
        httpClient.getSocketMiddleware().disableProxy();
    }

    void removeFutureInFlight(Future future, Object group) {

    }

    void addFutureInFlight(Future future, Object group) {
        if (group == null || future == null || future.isDone() || future.isCancelled())
            return;

        FutureSet members;
        synchronized (this) {
            members = inFlight.get(group);
            if (members == null) {
                members = new FutureSet();
                inFlight.put(group, members);
            }
        }

        members.put(future, true);
    }

    /**
     * Cancel all pending requests
     */
    public void cancelAll() {
        ArrayList<Object> groups;

        synchronized (this) {
            groups = new ArrayList<Object>(inFlight.keySet());
        }

        for (Object group: groups)
            cancelAll(group);
    }

    /**
     * Cancel all pending requests associated with the given context
     * @param context
     */
    public void cancelAll(Context context) {
        cancelAll((Object)context);
    }

    public int getPendingRequestCount(Object group) {
        synchronized (this) {
            FutureSet members = inFlight.get(group);
            if (members == null)
                return 0;
            int ret = 0;
            for (Future future: members.keySet()) {
                if (!future.isCancelled() && !future.isDone())
                    ret++;
            }
            return ret;
        }
    }

    public void dump() {
        bitmapCache.dump();
        Log.i(LOGTAG, "Pending bitmaps: " + bitmapsPending.size());
        Log.i(LOGTAG, "Groups: " + inFlight.size());
        for (FutureSet futures: inFlight.values()) {
            Log.i(LOGTAG, "Group size: " + futures.size());
        }
    }

    /**
     * Get the application Context object in use by this Ion instance
     * @return
     */
    public Context getContext() {
        return context;
    }

    private static class AsyncHttpRequestFactoryImpl implements AsyncHttpRequestFactory {
        @Override
        public AsyncHttpRequest createAsyncHttpRequest(URI uri, String method, RawHeaders headers) {
            return new AsyncHttpRequest(uri, method, headers);
        }
    }

    AsyncHttpClient httpClient;
    CookieMiddleware cookieMiddleware;
    ResponseCacheMiddleware responseCache;

    static class FutureSet extends WeakHashMap<Future, Boolean> {
    }
    // maintain a list of futures that are in being processed, allow for bulk cancellation
    WeakHashMap<Object, FutureSet> inFlight = new WeakHashMap<Object, FutureSet>();

    private void addCookieMiddleware() {
        httpClient.insertMiddleware(cookieMiddleware = new CookieMiddleware(context, name));
    }

    HttpLoader httpLoader;
    ContentLoader contentLoader;
    FileLoader fileLoader;

    public HttpLoader getHttpLoader() {
        return httpLoader;
    }

    public ContentLoader getContentLoader() {
        return contentLoader;
    }

    public FileLoader getFileLoader() {
        return fileLoader;
    }

    String name;
    public String getName() {
        return name;
    }

    Context context;
    private Ion(Context context, String name) {
        httpClient = new AsyncHttpClient(new AsyncServer());
        this.context = context = context.getApplicationContext();
        this.name = name;

        try {
            responseCache = ResponseCacheMiddleware.addCache(httpClient, new File(context.getCacheDir(), name), 10L * 1024L * 1024L);
        }
        catch (Exception e) {
            IonLog.w("unable to set up response cache", e);
        }

        // TODO: Support pre GB?
        if (Build.VERSION.SDK_INT >= 9)
            addCookieMiddleware();

        httpClient.getSocketMiddleware().setConnectAllAddresses(true);
        httpClient.getSSLSocketMiddleware().setConnectAllAddresses(true);

        bitmapCache = new IonBitmapCache(this);

        configure()
        .addLoader(httpLoader = new HttpLoader())
        .addLoader(contentLoader = new ContentLoader())
        .addLoader(fileLoader = new FileLoader());
    }

    /**
     * Get the Cookie middleware that is attached to the AsyncHttpClient instance.
     * @return
     */
    public CookieMiddleware getCookieMiddleware() {
        return cookieMiddleware;
    }

    /**
     * Get the AsyncHttpClient object in use by this Ion instance
     * @return
     */
    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Get the AsyncServer reactor in use by this Ion instance
     * @return
     */
    public AsyncServer getServer() {
        return httpClient.getServer();
    }

    public static class Config {
        AsyncHttpRequestFactory asyncHttpRequestFactory = new AsyncHttpRequestFactoryImpl();

        public AsyncHttpRequestFactory getAsyncHttpRequestFactory() {
            return asyncHttpRequestFactory;
        }

        public Config setAsyncHttpRequestFactory(AsyncHttpRequestFactory asyncHttpRequestFactory) {
            this.asyncHttpRequestFactory = asyncHttpRequestFactory;
            return this;
        }

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

    String LOGTAG;
    int logLevel;
    /**
     * Set the log level for all requests made by Ion.
     * @param logtag
     * @param logLevel
     */
    public void setLogging(String logtag, int logLevel) {
        LOGTAG = logtag;
        this.logLevel = logLevel;
    }

    Config config = new Config();
    public Config configure() {
        return config;
    }

    HashList<FutureCallback<BitmapInfo>> bitmapsPending = new HashList<FutureCallback<BitmapInfo>>();

    IonBitmapCache bitmapCache;
    /**
     * Return the bitmap cache used by this Ion instance
     * @return
     */
    public IonBitmapCache getBitmapCache() {
        return bitmapCache;
    }

    Gson gson = new Gson();
    /**
     * Get the Gson object in use by this Ion instance.
     * This can be used to customize serialization and deserialization
     * from java objects.
     * @return
     */
    public Gson getGson() {
        return gson;
    }

    static Ion instance;
}
