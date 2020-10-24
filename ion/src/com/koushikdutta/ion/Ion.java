package com.koushikdutta.ion;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.koushikdutta.ion.bitmap.IonBitmapCache;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.LoadBuilder;
import com.koushikdutta.ion.loader.AssetLoader;
import com.koushikdutta.ion.loader.AsyncHttpRequestFactory;
import com.koushikdutta.ion.loader.ContentLoader;
import com.koushikdutta.ion.loader.FileLoader;
import com.koushikdutta.ion.loader.HttpLoader;
import com.koushikdutta.ion.loader.PackageIconLoader;
import com.koushikdutta.ion.loader.ResourceLoader;
import com.koushikdutta.ion.loader.VideoLoader;
import com.koushikdutta.scratch.Promise;
import com.koushikdutta.scratch.event.FileStore;
import com.koushikdutta.scratch.event.AsyncEventLoop;
import com.koushikdutta.scratch.event.NamedThreadFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Created by koush on 5/21/13.
 */
public class Ion {
    static final Handler mainHandler = new Handler(Looper.getMainLooper());
    static int availableProcessors = Runtime.getRuntime().availableProcessors();
    static ExecutorService ioExecutorService = NamedThreadFactory.newSynchronousWorkers("ion-io", 4);
    static int numBitmapExecutors = availableProcessors > 2 ? availableProcessors - 1 : 1;
    static ExecutorService bitmapExecutorService = NamedThreadFactory.newSynchronousWorkers("ion-bitmap", numBitmapExecutors);
    static HashMap<String, Ion> instances = new HashMap<String, Ion>();

    /**
     * Get the default Ion object instance and begin building a request
     * @param context
     * @return
     */
    public static LoadBuilder<Builders.Any.B> with(Context context) {
        return getDefault(context).build(context);
    }

    /**
     * Get the default Ion object instance and begin building a request
     * @param context
     * @return
     */
    public static LoadBuilder<Builders.Any.B> with(IonContext context) {
        return getDefault(context.getContext()).build(context);
    }

    /**
     * the default Ion object instance and begin building a request
     * @param fragment
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public static LoadBuilder<Builders.Any.B> with(Fragment fragment) {
        return getDefault(fragment.getActivity()).build(fragment);
    }

    /**
     * the default Ion object instance and begin building a request
     * @param fragment
     * @return
     */
    public static LoadBuilder<Builders.Any.B> with(androidx.fragment.app.Fragment fragment) {
        return getDefault(fragment.getActivity()).build(fragment);
    }

    /**
     * Get the default Ion instance
     * @param context
     * @return
     */
    public static Ion getDefault(Context context) {
        return getInstance(context, "ion");
    }

    /**
     * Get the given Ion instance by name
     * @param context
     * @param name
     * @return
     */
    public static Ion getInstance(Context context, String name) {
        if (context == null)
            throw new NullPointerException("Can not pass null context in to retrieve ion instance");
        Ion instance = instances.get(name);
        if (instance == null)
            instances.put(name, instance = new Ion(context, name));
        return instance;
    }

    /**
     * Create a ImageView bitmap request builder
     * @param imageView
     * @return
     */
    public static Builders.IV.F<? extends Builders.IV.F<?>> with(ImageView imageView) {
        return getDefault(imageView.getContext()).build(imageView);
    }

    AsyncEventLoop loop;
    FileStore fileStore;
    FileStore cacheStore;
    HttpLoader httpLoader;
    ContentLoader contentLoader;
    ResourceLoader resourceLoader;
    AssetLoader assetLoader;
    VideoLoader videoLoader;
    PackageIconLoader packageIconLoader;
    FileLoader fileLoader;
    String logtag;
    int logLevel;
    Gson gson;
    String userAgent;
    ArrayList<Loader> loaders = new ArrayList<Loader>();
    String name;
    Config config = new Config();
    IonBitmapCache bitmapCache;
    BitmapManager bitmapManager;
    Context context;

    public AsyncEventLoop getLoop() {
        return loop;
    }

    private Ion(Context context, String name) {
        this.context = context = context.getApplicationContext();
        this.name = name;

        loop = new AsyncEventLoop();
        new Thread("ion-" + name) {
            @Override
            public void run() {
                loop.run();
            }
        }.start();

        fileStore = new FileStore(loop, true, () -> new File(this.context.getFilesDir(), name));
        cacheStore = new FileStore(loop, true, () -> new File(this.context.getCacheDir(), name));
        bitmapCache = new IonBitmapCache(this);
        bitmapManager = new BitmapManager(this);

        configure()
                .addLoader(videoLoader = new VideoLoader())
                .addLoader(packageIconLoader = new PackageIconLoader())
                .addLoader(httpLoader = new HttpLoader(loop))
                .addLoader(contentLoader = new ContentLoader())
                .addLoader(resourceLoader = new ResourceLoader())
                .addLoader(assetLoader = new AssetLoader())
                .addLoader(fileLoader = new FileLoader());
    }

    public static ExecutorService getBitmapLoadExecutorService() {
        return bitmapExecutorService;
    }

    public static ExecutorService getIoExecutorService() {
        return ioExecutorService;
    }

    /**
     * Begin building a request
     * @param context
     * @return
     */
    public LoadBuilder<Builders.Any.B> build(Context context) {
        return new IonRequestBuilder(ContextReference.fromContext(context), this);
    }

    /**
     * Begin building a request
     * @param context
     * @return
     */
    public LoadBuilder<Builders.Any.B> build(IonContext context) {
        return new IonRequestBuilder(context, this);
    }

    /**
     * Begin building a request
     * @param fragment
     * @return
     */
    public LoadBuilder<Builders.Any.B> build(Fragment fragment) {
        return new IonRequestBuilder(new ContextReference.FragmentContextReference(fragment), this);
    }

    /**
     * Begin building a request
     * @param fragment
     * @return
     */
    public LoadBuilder<Builders.Any.B> build(androidx.fragment.app.Fragment fragment) {
        return new IonRequestBuilder(new ContextReference.SupportFragmentContextReference(fragment), this);
    }

    /**
     * Create a builder that can be used to build an network request
     * @param imageView
     * @return
     */
    public Builders.IV.F<? extends Builders.IV.F<?>> build(ImageView imageView) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread())
            throw new IllegalStateException("must be called from UI thread");
        return new IonImageViewRequestBuilder(this).withImageView(imageView);
    }

    int groupCount(Object group) {
        FutureSet members;
        synchronized (this) {
            members = inFlight.get(group);
        }

        if (members == null)
            return 0;

        return members.size();
    }

    private static Comparator<BitmapPromise> DEFERRED_COMPARATOR = new Comparator<BitmapPromise>() {
        @Override
        public int compare(BitmapPromise lhs, BitmapPromise rhs) {
            // higher is more recent
            if (lhs.getLazyPriority() == rhs.getLazyPriority())
                return 0;
            if (lhs.getLazyPriority() < rhs.getLazyPriority())
                return 1;
            return -1;
        }
    };

    private Runnable processDeferred = new Runnable() {
        @Override
        public void run() {
            bitmapManager.processDeferred();
        }
    };

    void processDeferred() {
        mainHandler.removeCallbacks(processDeferred);
        mainHandler.post(processDeferred);
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

        for (Promise<?> future: members.keySet()) {
            if (future != null)
                future.cancel();
        }
    }

    void addFutureInFlight(Promise<?> future, Object group) {
        if (group == null || future == null)
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
            for (Promise future: members.keySet()) {
                if (!future.isCancelled() && !future.isCompleted())
                    ret++;
            }
            return ret;
        }
    }

    /**
     * Get the application Context object in use by this Ion instance
     * @return
     */
    public Context getContext() {
        return context;
    }

    static class FutureSet extends WeakHashMap<Promise, Boolean> {
    }
    // maintain a list of futures that are in being processed, allow for bulk cancellation
    WeakHashMap<Object, FutureSet> inFlight = new WeakHashMap<Object, FutureSet>();

    public FileStore getCache() {
        return cacheStore;
    }

    public FileStore getStore() {
        return fileStore;
    }

    public String getName() {
        return name;
    }

    /**
     * Get the AsyncServer reactor in use by this Ion instance
     * @return
     */
    public AsyncEventLoop getServer() {
        return loop;
    }

    public class Config {
        public HttpLoader getHttpLoader() {
            return httpLoader;
        }

        public PackageIconLoader getPackageIconLoader() {
            return packageIconLoader;
        }

        public ContentLoader getContentLoader() {
            return contentLoader;
        }

        public FileLoader getFileLoader() {
            return fileLoader;
        }

        /**
         * Get the Gson object in use by this Ion instance.
         * This can be used to customize serialization and deserialization
         * from java objects.
         * @return
         */
        public synchronized Gson getGson() {
            if (gson == null)
                gson = new Gson();
            return gson;
        }

        /**
         * Set the log level for all requests made by Ion.
         * @param logtag
         * @param logLevel
         * @return
         */
        public Config setLogging(String logtag, int logLevel) {
            Ion.this.logtag = logtag;
            Ion.this.logLevel = logLevel;
            return this;
        }

        /**
         * Set the Gson object in use by this Ion instance.
         * This can be used to customize serialization and deserialization
         * from java objects.
         * @param gson
         */
        public void setGson(Gson gson) {
            Ion.this.gson = gson;
        }

        AsyncHttpRequestFactory asyncHttpRequestFactory = new DefaultAsyncHttpRequestFactory();

        public AsyncHttpRequestFactory getAsyncHttpRequestFactory() {
            return asyncHttpRequestFactory;
        }

        public Config setAsyncHttpRequestFactory(AsyncHttpRequestFactory asyncHttpRequestFactory) {
            this.asyncHttpRequestFactory = asyncHttpRequestFactory;
            return this;
        }

        public String userAgent() {
            return userAgent;
        }

        public Config userAgent(String userAgent) {
            Ion.this.userAgent = userAgent;
            return this;
        }

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

    public Config configure() {
        return config;
    }

    /**
     * Return the bitmap cache used by this Ion instance
     * @return
     */
    public IonBitmapCache getBitmapCache() {
        return bitmapCache;
    }
}
