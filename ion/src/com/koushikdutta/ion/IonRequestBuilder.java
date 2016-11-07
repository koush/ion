package com.koushikdutta.ion;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
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
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.NameValuePair;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.DocumentBody;
import com.koushikdutta.async.http.body.FileBody;
import com.koushikdutta.async.http.body.FilePart;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.Part;
import com.koushikdutta.async.http.body.StreamBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.ByteBufferListParser;
import com.koushikdutta.async.parser.DocumentParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.async.stream.FileDataSink;
import com.koushikdutta.async.stream.OutputStreamDataSink;
import com.koushikdutta.ion.Loader.LoaderEmitter;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.LocallyCachedStatus;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.FutureBuilder;
import com.koushikdutta.ion.builder.LoadBuilder;
import com.koushikdutta.ion.future.ImageViewFuture;
import com.koushikdutta.ion.future.ResponseFuture;
import com.koushikdutta.ion.gson.GsonArrayParser;
import com.koushikdutta.ion.gson.GsonBody;
import com.koushikdutta.ion.gson.GsonObjectParser;
import com.koushikdutta.ion.gson.GsonSerializer;
import com.koushikdutta.ion.gson.PojoBody;

import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by koush on 5/21/13.
 */
class IonRequestBuilder implements Builders.Any.B, Builders.Any.F, Builders.Any.M, Builders.Any.U, LoadBuilder<Builders.Any.B> {
    Ion ion;
    ContextReference contextReference;
    Handler handler = Ion.mainHandler;
    String method = AsyncHttpGet.METHOD;
    String uri;

    public IonRequestBuilder(ContextReference contextReference, Ion ion) {
        String alive = contextReference.isAlive();
        if (null != alive)
            Log.w("Ion", "Building request with dead context: " + alive);
        this.ion = ion;
        this.contextReference = contextReference;
    }

    @Override
    public IonRequestBuilder load(String url) {
        return loadInternal(AsyncHttpGet.METHOD, url);
    }

    private IonRequestBuilder loadInternal(String method, String url) {
        this.method = method;
        if (!TextUtils.isEmpty(url) && url.startsWith("/"))
            url = new File(url).toURI().toString();
        this.uri = url;
        return this;
    }

    boolean methodWasSet;
    @Override
    public IonRequestBuilder load(String method, String url) {
        methodWasSet = true;
        return loadInternal(method, url);
    }

    Headers headers;
    private Headers getHeaders() {
        if (headers == null) {
            headers = new Headers();
            AsyncHttpRequest.setDefaultHeaders(headers, uri == null ? null : Uri.parse(uri));
        }
        return headers;
    }

    @Override
    public IonRequestBuilder userAgent(String userAgent) {
        if (TextUtils.isEmpty(userAgent))
            return this;
        return setHeader("User-Agent", userAgent);
    }

    @Override
    public IonRequestBuilder setHeader(String name, String value) {
        if (value == null)
            getHeaders().removeAll(name);
        else
            getHeaders().set(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder addHeader(String name, String value) {
        if (value != null)
            getHeaders().add(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder addHeaders(Map<String, List<String>> params) {
        if (params == null)
            return this;
        Headers headers = getHeaders();
        for (Map.Entry<String, List<String>> entry: params.entrySet()) {
            headers.addAll(entry.getKey(), entry.getValue());
        }
        return this;
    }

    boolean noCache;
    @Override
    public Builders.Any.B noCache() {
        noCache = true;
        return setHeader("Cache-Control", "no-cache");
    }

    Multimap query;
    @Override
    public IonRequestBuilder addQuery(String name, String value) {
        if (value == null)
            return this;
        if (query == null)
            query = new Multimap();
        query.add(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder addQueries(Map<String, List<String>> params) {
       if (query == null)
          query = new Multimap();
       query.putAll(params);
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

    private <T> void postExecute(final EmitterTransform<T> future, final Exception ex, final T value) {
        final Runnable runner = new Runnable() {
            @Override
            public void run() {
                // check if the context is still alive...
                String deadReason = contextReference.isAlive();
                if (deadReason != null) {
                    future.initialRequest.logd("context has died: " + deadReason);
                    future.cancelSilently();
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

    private Uri prepareURI() {
        Uri uri;
        try {
            if (query != null) {
                Uri.Builder builder = Uri.parse(this.uri).buildUpon();
                for (String key: query.keySet()) {
                    for (String value: query.get(key)) {
                        builder = builder.appendQueryParameter(key, value);
                    }
                }
                uri = builder.build();
            }
            else {
                uri = Uri.parse(this.uri);
            }
        }
        catch (Exception e) {
            uri = null;
        }
        if (uri == null || uri.getScheme() == null)
            return null;

        return uri;
    }

    private AsyncHttpRequest prepareRequest(Uri uri) {
        AsyncHttpRequest request = ion.configure().getAsyncHttpRequestFactory().createAsyncHttpRequest(uri, method, headers);
        request.setFollowRedirect(followRedirect);
        request.setBody(body);
        request.setLogging(ion.logtag, ion.logLevel);
        if (logTag != null)
            request.setLogging(logTag, logLevel);
        request.enableProxy(proxyHost, proxyPort);
        request.setTimeout(timeoutMilliseconds);
        request.logd("preparing request");
        return request;
    }

    static interface LoadRequestCallback {
        boolean loadRequest(AsyncHttpRequest request);
    }

    LoadRequestCallback loadRequestCallback;

    private <T> void getLoaderEmitter(final EmitterTransform<T> ret) {
        Uri uri = prepareURI();
        if (uri == null) {
            ret.setComplete(new Exception("Invalid URI"));
            return;
        }

        AsyncHttpRequest request = prepareRequest(uri);
        ret.initialRequest = request;

        getLoaderEmitter(ret, request);
    }

    private <T> void getLoaderEmitter(final EmitterTransform<T> ret, AsyncHttpRequest request) {
        if (body != null && (uploadProgressHandler != null || uploadProgressBar != null || uploadProgress != null || uploadProgressDialog != null)) {
            AsyncHttpRequestBody wrappedBody = new RequestBodyUploadObserver(body, new ProgressCallback() {
                    @Override
                    public void onProgress(final long downloaded, final long total) {
                    assert Thread.currentThread() != Looper.getMainLooper().getThread();

                    final int percent = (int)((float)downloaded / total * 100f);

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
                                uploadProgressHandler.onProgress(downloaded, total);
                            }
                        });
                    }
                }
            });

            request.setBody(wrappedBody);
        }

        resolveAndLoadRequest(request, ret);
    }

    Future<AsyncHttpRequest> recursiveResolveRequest(final AsyncHttpRequest request) {
        final SimpleFuture<AsyncHttpRequest> ret = new SimpleFuture<AsyncHttpRequest>();

        Runnable resolver = new Runnable() {
            AsyncHttpRequest inRequest = request;
            Runnable runner = this;

            @Override
            public void run() {
                Future<AsyncHttpRequest> resolved = resolveRequest(inRequest);
                if (resolved == null) {
                    ret.setComplete(inRequest);
                    return;
                }
                resolved.setCallback(new FutureCallback<AsyncHttpRequest>() {
                    @Override
                    public void onCompleted(Exception e, final AsyncHttpRequest result) {
                        if (e != null) {
                            ret.setComplete(e);
                            return;
                        }
                        inRequest = result;
                        runner.run();
                    }
                });
            }
        };
        resolver.run();
        return ret;
    }

    <T> void resolveAndLoadRequest(final AsyncHttpRequest request, final EmitterTransform<T> ret) {
        recursiveResolveRequest(request)
        .setCallback(new FutureCallback<AsyncHttpRequest>() {
            @Override
            public void onCompleted(Exception e, AsyncHttpRequest result) {
                if (e != null) {
                    ret.setComplete(e);
                    return;
                }
                ret.finalRequest = result;

                if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
                    AsyncServer.post(Ion.mainHandler, new Runnable() {
                        @Override
                        public void run() {
                            invokeLoadRequest(request, ret);
                        }
                    });
                    return;
                }
                invokeLoadRequest(request, ret);
            }
        });
    }

    <T> void invokeLoadRequest(final AsyncHttpRequest request, final EmitterTransform<T> ret) {
        if (loadRequestCallback == null || loadRequestCallback.loadRequest(request))
            loadRequest(request, ret);
    }

    <T> void loadRequest(AsyncHttpRequest request, final EmitterTransform<T> ret) {
        // now attempt to fetch it directly
        for (Loader loader: ion.loaders) {
            Future<DataEmitter> emitter = loader.load(ion, request, ret);
            if (emitter != null) {
                request.logi("Using loader: " + loader);
                ret.setParent(emitter);
                return;
            }
        }
        ret.setComplete(new Exception("Unknown uri scheme"));
    }

    <T> Future<AsyncHttpRequest> resolveRequest(AsyncHttpRequest request) {
        // first attempt to resolve the url
        for (Loader loader: ion.loaders) {
            Future<AsyncHttpRequest> resolved = loader.resolve(contextReference.getContext(), ion, request);
            if (resolved != null)
                return resolved;

        }
        return null;
    }

    // transforms a LoaderEmitter, which is a DataEmitter and all associated properties about the data source
    // into the final result.
    class EmitterTransform<T> extends TransformFuture<T, LoaderEmitter> implements ResponseFuture<T> {
        AsyncHttpRequest initialRequest;
        AsyncHttpRequest finalRequest;
        ResponseServedFrom servedFrom;
        Runnable cancelCallback;
        HeadersResponse headers;
        DataEmitter emitter;

        public Response<T> getResponse(Exception e, T result) {
            return new Response<T>(finalRequest, servedFrom, headers, e, result);
        }

        @Override
        public Future<Response<T>> withResponse() {
            final SimpleFuture<Response<T>> ret = new SimpleFuture<Response<T>>();
            setCallback(new FutureCallback<T>() {
                @Override
                public void onCompleted(Exception e, T result) {
                    if (emitter != null) {
                        ret.setComplete(getResponse(e, result));
                        return;
                    }
                    ret.setComplete(e, null);
                }
            });
            ret.setParent(this);
            return ret;
        }

        public EmitterTransform(Runnable cancelCallback) {
            this.cancelCallback = cancelCallback;
            ion.addFutureInFlight(this, contextReference.getContext());
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
            this.servedFrom = emitter.getServedFrom();
            this.headers = emitter.getHeaders();
            this.finalRequest = emitter.getRequest();

            if (headersCallback != null) {
                final HeadersResponse headers = emitter.getHeaders();
                // what do we do on loaders that don't have headers? files, content://, etc.
                AsyncServer.post(handler, new Runnable() {
                    @Override
                    public void run() {
                        headersCallback.onHeaders(headers);
                    }
                });
            }

            // hook up data progress callbacks
            final long total = emitter.length();
            DataTrackingEmitter tracker;
            if (!(this.emitter instanceof DataTrackingEmitter)) {
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
                    String deadReason = contextReference.isAlive();
                    if (deadReason != null) {
                        initialRequest.logd("context has died, cancelling");
                        cancelSilently();
                        return;
                    }

                    final int percent = (int)((float)totalBytesRead / total * 100f);

                    if ((progressBar != null || progressDialog != null) && percent != lastPercent) {
                        AsyncServer.post(Ion.mainHandler, new Runnable() {
                            @Override
                            public void run() {
                                if (isCancelled() || isDone())
                                    return;
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
                    sink.end();
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

    <T> ResponseFuture<T> execute(final AsyncParser<T> parser) {
        return execute(parser, null);
    }

    <T> ResponseFuture<T> execute(final AsyncParser<T> parser, Runnable cancel) {
        assert parser != null;

        final Uri uri = prepareURI();
        AsyncHttpRequest request = null;

        if (uri != null) {
            request = prepareRequest(uri);
            Type type = parser.getType();
            for (Loader loader: ion.loaders) {
                ResponseFuture<T> quickLoad = loader.load(ion, request, type);
                if (quickLoad != null)
                    return quickLoad;
            }
        }

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

        if (uri == null) {
            ret.setComplete(new Exception("Invalid URI"));
            return ret;
        }

        ret.initialRequest = request;
        getLoaderEmitter(ret);
        return ret;
    }

    @Override
    public ResponseFuture<JsonObject> asJsonObject() {
        return execute(new GsonObjectParser());
    }

    @Override
    public ResponseFuture<JsonArray> asJsonArray() {
        return execute(new GsonArrayParser());
    }

    @Override
    public ResponseFuture<JsonObject> asJsonObject(Charset charset) {
        return execute(new GsonObjectParser(charset));
    }

    @Override
    public ResponseFuture<JsonArray> asJsonArray(Charset charset) {
        return execute(new GsonArrayParser(charset));
    }

    @Override
    public ResponseFuture<String> asString() {
        return execute(new StringParser());
    }

    @Override
    public ResponseFuture<String> asString(Charset charset) {
        return execute(new StringParser(charset));
    }

    @Override
    public ResponseFuture<byte[]> asByteArray() {
        return execute(new AsyncParser<byte[]>() {
            @Override
            public Future<byte[]> parse(DataEmitter emitter) {
                return new ByteBufferListParser().parse(emitter)
                .then(new TransformFuture<byte[], ByteBufferList>() {
                    @Override
                    protected void transform(ByteBufferList result) throws Exception {
                        setComplete(result.getAllByteArray());
                    }
                });
            }

            @Override
            public void write(DataSink sink, byte[] value, CompletedCallback completed) {
                new ByteBufferListParser().write(sink, new ByteBufferList(value), completed);
            }

            @Override
            public Type getType() {
                return byte[].class;
            }
        });
    }

    @Override
    public ResponseFuture<InputStream> asInputStream() {
        return execute(new InputStreamParser());
    }

    @Override
    public ResponseFuture<DataEmitter> asDataEmitter() {
        return execute(new DataEmitterParser());
    }

    @Override
    public <T> ResponseFuture<T> as(AsyncParser<T> parser) {
        return execute(parser);
    }

    @Override
    public <F extends OutputStream> ResponseFuture<F> write(F outputStream, boolean close) {
        return execute(new OutputStreamDataSink(ion.getServer(), outputStream), close, outputStream);
    }

    @Override
    public <F extends OutputStream> ResponseFuture<F> write(F outputStream) {
        return execute(new OutputStreamDataSink(ion.getServer(), outputStream), true, outputStream);
    }

    @Override
    public EmitterTransform<File> write(final File file) {
        return execute(new FileDataSink(ion.getServer(), file), true, file, new Runnable() {
            @Override
            public void run() {
                file.delete();
            }
        });
    }

    Multimap bodyParameters;
    @Override
    public IonRequestBuilder setBodyParameter(String name, String value) {
        if (bodyParameters == null) {
            bodyParameters = new Multimap();
            setBody(new UrlEncodedFormBody(bodyParameters));
        }
        if (value != null)
            bodyParameters.add(name, value);
        return this;
    }

    public IonRequestBuilder setBodyParameters(Map<String, List<String>> params) {
       if (bodyParameters == null) {
           bodyParameters = new Multimap();
           setBody(new UrlEncodedFormBody(bodyParameters));
       }
       bodyParameters.putAll(params);
       return this;
    }

    MultipartFormDataBody multipartBody;
    @Override
    public IonRequestBuilder setMultipartFile(String name, File file) {
        return setMultipartFile(name, null, file);
    }

    @Override
    public IonRequestBuilder setMultipartFile(String name, String contentType, File file) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }

        FilePart part = new FilePart(name, file);

        if (contentType == null)
            contentType = AsyncHttpServer.tryGetContentType(file.getAbsolutePath());

        if (contentType != null)
            part.setContentType(contentType);

        multipartBody.addPart(part);
        return this;
    }

    @Override
    public IonRequestBuilder setMultipartParameter(String name, String value) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        if (value != null)
            multipartBody.addStringPart(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder setMultipartParameters(Map<String, List<String>> params) {
        for (String key: params.keySet()) {
            for (String value: params.get(key)) {
                if (value != null)
                    setMultipartParameter(key, value);
            }
        }
        return this;
    }

    @Override
    public IonRequestBuilder addMultipartParts(Iterable<Part> parameters) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }

        for (Part part: parameters) {
            multipartBody.addPart(part);
        }
        return this;
    }

    @Override
    public Builders.Any.M addMultipartParts(Part... parameters) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }

        for (Part part: parameters) {
            multipartBody.addPart(part);
        }
        return this;
    }

    @Override
    public IonRequestBuilder setMultipartContentType(String contentType) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.setContentType(contentType);
        return this;
    }

    @Override
    public IonImageViewRequestBuilder withBitmap() {
        return new IonImageViewRequestBuilder(this);
    }

    @Override
    public ImageViewFuture intoImageView(ImageView imageView) {
        return new IonImageViewRequestBuilder(this).withImageView(imageView).intoImageView(imageView);
    }

    @Override
    public IonRequestBuilder load(File file) {
        loadInternal(null, file.toURI().toString());
        return this;
    }

    @Override
    public BitmapInfo asCachedBitmap() {
        return new IonImageViewRequestBuilder(this).asCachedBitmap();
    }

    @Override
    public void removeCachedBitmap() {
        new IonImageViewRequestBuilder(this).removeCachedBitmap();
    }

    @Override
    public LocallyCachedStatus isLocallyCached() {
        return new IonImageViewRequestBuilder(this).isLocallyCached();
    }

    @Override
    public Future<Bitmap> asBitmap() {
        return new IonImageViewRequestBuilder(this).asBitmap();
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
    public IonRequestBuilder setJsonPojoBody(Object object, TypeToken token) {
        setBody(new PojoBody(ion.configure().getGson(), object, token));
        return this;
    }

    @Override
    public IonRequestBuilder setJsonPojoBody(Object object) {
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
    public Builders.Any.F setByteArrayBody(byte[] bytes) {
        if (bytes != null)
            setBody(new StreamBody(new ByteArrayInputStream(bytes), bytes.length));
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

    @Override
    public Builders.Any.B setHeader(NameValuePair... header) {
        Headers headers = getHeaders();
        for (NameValuePair h: header) {
            headers.set(h.getName(), h.getValue());
        }
        return this;
    }
}
