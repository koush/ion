package com.koushikdutta.ion;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.DataTrackingEmitter;
import com.koushikdutta.async.DataTrackingEmitter.DataTracker;
import com.koushikdutta.async.FilteredDataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.body.DocumentBody;
import com.koushikdutta.async.http.body.FileBody;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.StreamBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.async.stream.OutputStreamDataSink;
import com.koushikdutta.ion.Loader.LoaderEmitter;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.FutureBuilder;
import com.koushikdutta.ion.builder.LoadBuilder;
import com.koushikdutta.ion.future.ResponseFuture;
import com.koushikdutta.async.parser.DocumentParser;
import com.koushikdutta.ion.gson.GsonBody;
import com.koushikdutta.ion.gson.GsonParser;
import com.koushikdutta.ion.gson.GsonSerializer;
import com.koushikdutta.ion.gson.PojoBody;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koush on 5/21/13.
 */
class IonRequestBuilder implements Builders.Any.B, Builders.Any.F, Builders.Any.M, Builders.Any.U, LoadBuilder<Builders.Any.B> {
    Ion ion;
    WeakReference<Context> context;
    Handler handler = Ion.mainHandler;
    String method = AsyncHttpGet.METHOD;
    String uri;

    public IonRequestBuilder(Context context, Ion ion) {
        this.ion = ion;
        this.context = new WeakReference<Context>(context);
    }

    @Override
    public IonRequestBuilder load(String url) {
        return loadInternal(AsyncHttpGet.METHOD, url);
    }

    private IonRequestBuilder loadInternal(String method, String url) {
        this.method = method;
        this.uri = url;
        return this;
    }

    boolean methodWasSet;
    @Override
    public IonRequestBuilder load(String method, String url) {
        methodWasSet = true;
        return loadInternal(method, url);
    }

    RawHeaders headers;
    private RawHeaders getHeaders() {
        if (headers == null) {
            headers = new RawHeaders();
            AsyncHttpRequest.setDefaultHeaders(headers, URI.create(uri));
        }
        return headers;
    }

    @Override
    public IonRequestBuilder userAgent(String userAgent) {
        return setHeader("User-Agent", userAgent);
    }

    @Override
    public IonRequestBuilder setHeader(String name, String value) {
        getHeaders().set(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder addHeader(String name, String value) {
        getHeaders().add(name, value);
        return this;
    }

    Multimap query;
    @Override
    public IonRequestBuilder addQuery(String name, String value) {
        if (query == null)
            query = new Multimap();
        query.add(name, value);
        return this;
    }

    int timeoutMilliseconds = AsyncHttpRequest.DEFAULT_TIMEOUT;
    @Override
    public IonRequestBuilder setTimeout(int timeoutMilliseconds) {
        this.timeoutMilliseconds = timeoutMilliseconds;
        return this;
    }

    @Override
    public IonRequestBuilder setHandler(Handler handler) {
        this.handler = handler;
        return this;
    }

    AsyncHttpRequestBody body;
    private <T> IonRequestBuilder setBody(AsyncHttpRequestBody<T> body) {
        if (!methodWasSet)
            method = AsyncHttpPost.METHOD;
        this.body = body;
        return this;
    }

    @Override
    public IonRequestBuilder setJsonObjectBody(JsonObject jsonObject) {
        return setBody(new GsonBody<JsonObject>(ion.configure().getGson(), jsonObject));
    }

    @Override
    public IonRequestBuilder setJsonArrayBody(JsonArray jsonArray) {
        return setBody(new GsonBody<JsonArray>(ion.configure().getGson(), jsonArray));
    }

    @Override
    public IonRequestBuilder setStringBody(String string) {
        return setBody(new StringBody(string));
    }

    boolean followRedirect = true;
    @Override
    public IonRequestBuilder followRedirect(boolean follow) {
        followRedirect = follow;
        return this;
    }

    private static boolean isServiceRunning(Service candidate) {
        ActivityManager manager = (ActivityManager)candidate.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        if (services == null)
            return false;
        for (ActivityManager.RunningServiceInfo service: services) {
            if (candidate.getClass().getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkContext() {
        Context context = IonRequestBuilder.this.context.get();
        if (context == null)
            return false;
        if (context instanceof Activity) {
            Activity activity = (Activity)context;
            if (activity.isFinishing())
                return false;
        }
        else if (context instanceof Service) {
            Service service = (Service)context;
            if (!isServiceRunning(service))
                return false;
        }

        return true;
    }

    private <T> void postExecute(final EmitterTransform<T> future, final Exception ex, final T value) {
        final Runnable runner = new Runnable() {
            @Override
            public void run() {
                // check if the context is still alive...
                if (!checkContext()) {
                    future.initialRequest.logd("context has died");
                    return;
                }

                // unless we're invoked onto the handler/main/service thread, there's no frakking way to avoid a
                // race condition where the service or activity dies before this callback is invoked.
                if (ex != null)
                    future.setComplete(ex);
                else
                    future.setComplete(value);
            }
        };

        if (handler == null)
            ion.httpClient.getServer().post(runner);
        else
            AsyncServer.post(handler, runner);
    }

    private URI prepareURI() {
        URI uri;
        try {
            if (query != null) {
                Uri.Builder builder = Uri.parse(this.uri).buildUpon();
                for (String key: query.keySet()) {
                    for (String value: query.get(key)) {
                        builder = builder.appendQueryParameter(key, value);
                    }
                }
                uri = URI.create(builder.toString());
            }
            else {
                uri = URI.create(this.uri);
            }
        }
        catch (Exception e) {
            uri = null;
        }
        if (uri == null || uri.getScheme() == null)
            return null;

        return uri;
    }

    private AsyncHttpRequest prepareRequest(URI uri, AsyncHttpRequestBody wrappedBody) {
        AsyncHttpRequest request = ion.configure().getAsyncHttpRequestFactory().createAsyncHttpRequest(uri, method, headers);
        request.setFollowRedirect(followRedirect);
        request.setBody(wrappedBody);
        request.setLogging(ion.LOGTAG, ion.logLevel);
        if (logTag != null)
            request.setLogging(logTag, logLevel);
        request.enableProxy(proxyHost, proxyPort);
        request.setTimeout(timeoutMilliseconds);
        request.setHandler(null);
        request.logd("preparing request");
        return request;
    }

    private <T> void getLoaderEmitter(final EmitterTransform<T> ret) {
        URI uri = prepareURI();
        if (uri == null) {
            ret.setComplete(new Exception("Invalid URI"));
            return;
        }

        AsyncHttpRequestBody wrappedBody = body;
        if (uploadProgressHandler != null || uploadProgressBar != null || uploadProgress != null || uploadProgressDialog != null) {
            wrappedBody = new RequestBodyUploadObserver(body, new ProgressCallback() {
                @Override
                public void onProgress(final int downloaded, final int total) {
                    assert Thread.currentThread() != Looper.getMainLooper().getThread();

                    int percent = (int)((float)total / total * 100f);

                    if (uploadProgressBar != null)
                        uploadProgressBar.setProgress(percent);

                    if (uploadProgressDialog != null)
                        uploadProgressDialog.setProgress(percent);

                    if (uploadProgress != null)
                        uploadProgress.onProgress(downloaded, total);

                    if (uploadProgressHandler != null) {
                        AsyncServer.post(Ion.mainHandler, new Runnable() {
                            @Override
                            public void run() {
                                if (ret.isCancelled() || ret.isDone())
                                    return;
                                progressHandler.onProgress(downloaded, total);
                            }
                        });
                    }
                }
            });
        }

        AsyncHttpRequest request = prepareRequest(uri, wrappedBody);
        ret.initialRequest = request;

        for (Loader loader: ion.config.loaders) {
            Future<DataEmitter> emitter = loader.load(ion, request, ret);
            if (emitter != null) {
                ret.setParent(emitter);
                return;
            }
        }
        ret.setComplete(new Exception("Unknown uri scheme"));
    }

    class EmitterTransform<T> extends TransformFuture<T, LoaderEmitter> implements ResponseFuture<T> {
        private AsyncHttpRequest initialRequest;
        private AsyncHttpRequest finalRequest;
        private int loadedFrom;
        Runnable cancelCallback;
        RawHeaders headers;
        DataEmitter emitter;

        class ResponseFutureImpl extends TransformFuture<Response<T>, T> {
            @Override
            protected void error(Exception e) {
                Response<T> response = new Response<T>();
                response.headers = headers;
                response.request = finalRequest;
                setComplete(e, response);
            }

            @Override
            protected void transform(T result) throws Exception {
                Response<T> response = new Response<T>();
                response.headers = headers;
                response.result = result;
                response.request = finalRequest;
                setComplete(response);
            }
        }

        @Override
        public Future<Response<T>> withResponse() {
            final ResponseFutureImpl ret = new ResponseFutureImpl();
            setCallback(ret);
            return ret;
        }

        public int loadedFrom() {
            return loadedFrom;
        }

        public EmitterTransform(Runnable cancelCallback) {
            this.cancelCallback = cancelCallback;
            ion.addFutureInFlight(this, context.get());
            if (groups == null)
                return;
            for (WeakReference<Object> ref: groups) {
                Object group = ref.get();
                if (group != null)
                    ion.addFutureInFlight(this, group);
            }
        }

        @Override
        protected void cancelCleanup() {
            super.cancelCleanup();
            if (emitter != null)
                emitter.close();
            if (cancelCallback != null)
                cancelCallback.run();
        }

        @Override
        protected void error(Exception e) {
            // don't call superclass which calls setComplete... get onto handler thread.
            postExecute(this, e, null);
        }

        @Override
        protected void transform(LoaderEmitter emitter) throws Exception {
            this.emitter = emitter.getDataEmitter();
            this.loadedFrom = emitter.loadedFrom();
            this.headers = emitter.getHeaders();
            this.finalRequest = emitter.getRequest();

            if (headersCallback != null) {
                final RawHeaders headers = emitter.getHeaders();
                // what do we do on loaders that don't have headers? files, content://, etc.
                AsyncServer.post(handler, new Runnable() {
                    @Override
                    public void run() {
                        headersCallback.onHeaders(headers);
                    }
                });
            }

            // hook up data progress callbacks
            final int total = emitter.length();
            DataTrackingEmitter tracker;
            if (!(emitter instanceof DataTrackingEmitter)) {
                tracker = new FilteredDataEmitter();
                tracker.setDataEmitter(this.emitter);
            }
            else {
                tracker = (DataTrackingEmitter)this.emitter;
            }
            this.emitter = tracker;
            tracker.setDataTracker(new DataTracker() {
                int lastPercent;
                @Override
                public void onData(final int totalBytesRead) {
                    assert Thread.currentThread() != Looper.getMainLooper().getThread();
                    // if the requesting context dies during the transfer... cancel
                    if (!checkContext()) {
                        initialRequest.logd("context has died, cancelling");
                        cancel();
                        return;
                    }

                    final int percent = (int)((float)totalBytesRead / total * 100f);

                    if ((progressBar != null || progressDialog != null) && percent != lastPercent) {
                        AsyncServer.post(Ion.mainHandler, new Runnable() {
                            @Override
                            public void run() {
                                if (progressBar != null) {
                                    ProgressBar bar = progressBar.get();
                                    if (bar != null)
                                        bar.setProgress(percent);
                                }
                                if (progressDialog != null) {
                                    ProgressDialog dlg = progressDialog.get();
                                    if (dlg != null)
                                        dlg.setProgress(percent);
                                }
                            }
                        });
                    }
                    lastPercent = percent;

                    if (progress != null)
                        progress.onProgress(totalBytesRead, total);

                    if (progressHandler != null) {
                        AsyncServer.post(Ion.mainHandler, new Runnable() {
                            @Override
                            public void run() {
                                if (isCancelled() || isDone())
                                    return;
                                progressHandler.onProgress(totalBytesRead, total);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public IonRequestBuilder progressBar(ProgressBar progressBar) {
        this.progressBar = new WeakReference<ProgressBar>(progressBar);
        return this;
    }

    @Override
    public IonRequestBuilder progressDialog(ProgressDialog progressDialog) {
        this.progressDialog = new WeakReference<ProgressDialog>(progressDialog);
        return this;
    }

    WeakReference<ProgressBar> progressBar;
    WeakReference<ProgressDialog> progressDialog;

    ProgressCallback progress;
    @Override
    public IonRequestBuilder progress(ProgressCallback callback) {
        progress = callback;
        return this;
    }

    ProgressCallback progressHandler;
    @Override
    public IonRequestBuilder progressHandler(ProgressCallback callback) {
        progressHandler = callback;
        return this;
    }

    <T> EmitterTransform<T> execute(final DataSink sink, final boolean close, final T result) {
        return execute(sink, close, result, null);
    }


    <T> EmitterTransform<T> execute(final DataSink sink, final boolean close, final T result, final Runnable cancel) {
        EmitterTransform<T> ret = new EmitterTransform<T>(cancel) {
            @Override
            protected void cleanup() {
                super.cleanup();
                if (close)
                    sink.close();
            }

            EmitterTransform<T> self = this;
            @Override
            protected void transform(LoaderEmitter emitter) throws Exception {
                super.transform(emitter);
                Util.pump(this.emitter, sink, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        postExecute(self, ex, result);
                    }
                });
            }
        };
        getLoaderEmitter(ret);
        return ret;
    }

    <T> EmitterTransform<T> execute(final AsyncParser<T> parser) {
        return execute(parser, null);
    }

    <T> EmitterTransform<T> execute(final AsyncParser<T> parser, Runnable cancel) {
        assert parser != null;
        EmitterTransform<T> ret = new EmitterTransform<T>(cancel) {
            EmitterTransform<T> self = this;
            @Override
            protected void transform(LoaderEmitter emitter) throws Exception {
                super.transform(emitter);
                parser.parse(this.emitter).setCallback(new FutureCallback<T>() {
                    @Override
                    public void onCompleted(Exception e, T result) {
                        postExecute(self, e, result);
                    }
                });
            }
        };
        getLoaderEmitter(ret);
        return ret;
    }

    Future<InputStream> execute() {
        URI uri = prepareURI();
        if (uri == null)
            return null;

        AsyncHttpRequest request = prepareRequest(uri, null);

        for (Loader loader: ion.config.loaders) {
            Future<InputStream> ret = loader.load(ion, request);
            if (ret != null)
                return ret;
        }
        return null;
    }

    @Override
    public ResponseFuture<JsonObject> asJsonObject() {
        return execute(new GsonParser<JsonObject>());
    }

    @Override
    public ResponseFuture<JsonArray> asJsonArray() {
        return execute(new GsonParser<JsonArray>());
    }

    @Override
    public ResponseFuture<String> asString() {
        return execute(new StringParser());
    }

    @Override
    public <F extends OutputStream> ResponseFuture<F> write(F outputStream, boolean close) {
        return execute(new OutputStreamDataSink(ion.getServer(), outputStream, true), close, outputStream);
    }

    @Override
    public <F extends OutputStream> ResponseFuture<F> write(F outputStream) {
        return execute(new OutputStreamDataSink(ion.getServer(), outputStream, true), true, outputStream);
    }

    @Override
    public ResponseFuture<File> write(final File file) {
        try {
            return execute(new OutputStreamDataSink(ion.getServer(), new FileOutputStream(file)), true, file, new Runnable() {
                @Override
                public void run() {
                    file.delete();
                }
            });
        }
        catch (Exception e) {
            EmitterTransform<File> ret = new EmitterTransform<File>(null);
            ret.setComplete(e);
            return ret;
        }
    }

    Multimap bodyParameters;
    @Override
    public IonRequestBuilder setBodyParameter(String name, String value) {
        if (bodyParameters == null) {
            bodyParameters = new Multimap();
            setBody(new UrlEncodedFormBody(bodyParameters));
        }
        bodyParameters.add(name, value);
        return this;
    }

    MultipartFormDataBody multipartBody;
    @Override
    public IonRequestBuilder setMultipartFile(String name, File file) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.addFilePart(name, file);
        return this;
    }

    @Override
    public IonRequestBuilder setMultipartParameter(String name, String value) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.addStringPart(name, value);
        return this;
    }

    @Override
    public IonBitmapRequestBuilder withBitmap() {
        return new IonBitmapRequestBuilder(this);
    }

    IonBitmapRequestBuilder withImageView(ImageView imageView) {
        return new IonBitmapRequestBuilder(this).withImageView(imageView);
    }

    @Override
    public Future<ImageView> intoImageView(ImageView imageView) {
        return new IonBitmapRequestBuilder(this).intoImageView(imageView);
    }

    @Override
    public IonRequestBuilder load(File file) {
        loadInternal(null, file.toURI().toString());
        return this;
    }

    @Override
    public Future<Bitmap> asBitmap() {
        return new IonBitmapRequestBuilder(this).asBitmap();
    }

    String logTag;
    int logLevel;
    @Override
    public IonRequestBuilder setLogging(String tag, int level) {
        logTag = tag;
        logLevel = level;
        return this;
    }

    @Override
    public <T> ResponseFuture<T> as(Class<T> clazz) {
        return execute(new GsonSerializer<T>(ion.configure().getGson(), clazz));
    }

    @Override
    public <T> ResponseFuture<T> as(TypeToken<T> token) {
        return execute(new GsonSerializer<T>(ion.configure().getGson(), token));
    }

    ArrayList<WeakReference<Object>> groups;
    @Override
    public FutureBuilder group(Object groupKey) {
        if (groups == null)
            groups = new ArrayList<WeakReference<Object>>();
        groups.add(new WeakReference<Object>(groupKey));
        return this;
    }

    String proxyHost;
    int proxyPort;
    @Override
    public IonRequestBuilder proxy(String host, int port) {
        proxyHost = host;
        proxyPort = port;
        return this;
    }

    @Override
    public IonRequestBuilder setJsonObjectBody(Object object, TypeToken token) {
        setBody(new PojoBody(ion.configure().getGson(), object, token));
        return this;
    }

    @Override
    public IonRequestBuilder setJsonObjectBody(Object object) {
        setBody(new PojoBody(ion.configure().getGson(), object, null));
        return this;
    }

    @Override
    public IonRequestBuilder basicAuthentication(String username, String password) {
        return setHeader("Authorization", "Basic " + Base64.encodeToString(String.format("%s:%s", username, password).getBytes(), Base64.NO_WRAP));
    }

    ProgressCallback uploadProgress;
    @Override
    public Builders.Any.B uploadProgress(ProgressCallback callback) {
        uploadProgress = callback;
        return this;
    }

    ProgressBar uploadProgressBar;
    @Override
    public Builders.Any.B uploadProgressBar(ProgressBar progressBar) {
        uploadProgressBar = progressBar;
        return this;
    }

    ProgressDialog uploadProgressDialog;
    @Override
    public Builders.Any.B uploadProgressDialog(ProgressDialog progressDialog) {
        uploadProgressDialog = progressDialog;
        return this;
    }

    ProgressCallback uploadProgressHandler;
    @Override
    public Builders.Any.B uploadProgressHandler(ProgressCallback callback) {
        uploadProgressHandler = callback;
        return this;
    }

    HeadersCallback headersCallback;
    @Override
    public Builders.Any.B onHeaders(HeadersCallback callback) {
        headersCallback = callback;
        return this;
    }

    @Override
    public Builders.Any.F setDocumentBody(Document document) {
        setBody(new DocumentBody(document));
        return this;
    }

    @Override
    public ResponseFuture<Document> asDocument() {
        return execute(new DocumentParser());
    }

    @Override
    public Builders.Any.F setFileBody(File file) {
        setBody(new FileBody(file));
        return this;
    }

    @Override
    public Builders.Any.F setStreamBody(InputStream inputStream) {
        setBody(new StreamBody(inputStream, -1));
        return this;
    }

    @Override
    public Builders.Any.F setStreamBody(InputStream inputStream, int length) {
        setBody(new StreamBody(inputStream, length));
        return this;
    }
}
