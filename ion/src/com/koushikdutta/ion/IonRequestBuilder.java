package com.koushikdutta.ion;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ProgressBar;

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
import com.koushikdutta.async.http.AsyncHttpRequestBody;
import com.koushikdutta.async.http.JSONObjectBody;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.MultipartFormDataBody;
import com.koushikdutta.async.http.StringBody;
import com.koushikdutta.async.http.UrlEncodedFormBody;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.JSONArrayParser;
import com.koushikdutta.async.parser.JSONObjectParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.async.stream.OutputStreamDataSink;
import com.koushikdutta.ion.Loader.LoaderEmitter;
import com.koushikdutta.ion.builder.IonBodyParamsRequestBuilder;
import com.koushikdutta.ion.builder.IonFormMultipartBodyRequestBuilder;
import com.koushikdutta.ion.builder.IonFutureRequestBuilder;
import com.koushikdutta.ion.builder.IonLoadRequestBuilder;
import com.koushikdutta.ion.builder.IonMutableBitmapRequestBuilder;
import com.koushikdutta.ion.builder.IonMutableBitmapRequestPostLoadBuilder;
import com.koushikdutta.ion.builder.IonUrlEncodedBodyRequestBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.List;

/**
 * Created by koush on 5/21/13.
 */
class IonRequestBuilder implements IonLoadRequestBuilder, IonBodyParamsRequestBuilder {
    AsyncHttpRequest request;
    Ion ion;
    WeakReference<Context> context;

    public IonRequestBuilder(Context context, Ion ion) {
        this.ion = ion;
        this.context = new WeakReference<Context>(context);
    }

    @Override
    public IonBodyParamsRequestBuilder load(String url) {
        return loadInternal(AsyncHttpGet.METHOD, url);
    }

    private IonBodyParamsRequestBuilder loadInternal(String method, String url) {
        request = new AsyncHttpRequest(URI.create(url), method);
        setLogging(ion.LOGTAG, ion.logLevel);
        return this;
    }

    boolean methodWasSet;
    @Override
    public IonBodyParamsRequestBuilder load(String method, String url) {
        methodWasSet = true;
        return loadInternal(method, url);
    }

    @Override
    public IonBodyParamsRequestBuilder setHeader(String name, String value) {
        request.setHeader(name, value);
        return this;
    }

    @Override
    public IonBodyParamsRequestBuilder addHeader(String name, String value) {
        request.addHeader(name, value);
        return this;
    }

    @Override
    public IonBodyParamsRequestBuilder setTimeout(int timeoutMilliseconds) {
        request.setTimeout(timeoutMilliseconds);
        return this;
    }

    @Override
    public IonBodyParamsRequestBuilder setHandler(Handler handler) {
        request.setHandler(handler);
        return this;
    }

    private <T> IonFutureRequestBuilder setBody(AsyncHttpRequestBody<T> body) {
        request.setBody(body);
        if (!methodWasSet)
            request.setMethod(AsyncHttpPost.METHOD);
        return this;
    }

    @Override
    public IonFutureRequestBuilder setJSONObjectBody(JSONObject jsonObject) {
        setHeader("Content-Type", "application/json");
        return setBody(new JSONObjectBody(jsonObject));
    }

    @Override
    public IonFutureRequestBuilder setStringBody(String string) {
        setHeader("Content-Type", "text/plain");
        return setBody(new StringBody(string));
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

    private <T> void postExecute(final SimpleFuture<T> future, final Exception ex, final T value) {
        final Runnable runner = new Runnable() {
            @Override
            public void run() {
                // check if the context is still alive...
                if (!checkContext())
                    return;

                // unless we're invoked onto the handler/main/service thread, there's no frakking way to avoid a
                // race condition where the service or activity dies before this callback is invoked.
                if (ex != null)
                    future.setComplete(ex);
                else
                    future.setComplete(value);
            }
        };

        if (request.getHandler() == null)
            ion.httpClient.getServer().post(runner);
        else
            AsyncServer.post(request.getHandler(), runner);
    }

    private <T> void getLoaderEmitter(TransformFuture<T, LoaderEmitter> ret) {
        if (request == null || request.getUri() == null || request.getUri().getScheme() == null) {
            ret.setComplete(new Exception("Invalid URI"));
            return;
        }

        for (Loader loader: ion.config.loaders) {
            Future<DataEmitter> emitter = loader.load(ion, request, ret);
            if (emitter != null) {
                ret.setParent(emitter);
                return;
            }
        }
        ret.setComplete(new Exception("Unknown uri scheme"));
    }

    private class EmitterTransform<T> extends TransformFuture<T, LoaderEmitter> {
        public EmitterTransform() {
            ion.addFutureInFlight(this, context.get());
        }
        DataEmitter emitter;
        @Override
        protected void cancelCleanup() {
            if (emitter != null)
                emitter.close();
        }

        @Override
        protected void error(Exception e) {
            // don't call superclass which calls setComplete... get onto handler thread.
            postExecute(this, e, null);
        }

        @Override
        protected void transform(LoaderEmitter emitter) throws Exception {
            this.emitter = emitter.getDataEmitter();

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
                @Override
                public void onData(final int totalBytesRead) {
                    // if the requesting context dies during the transfer... cancel
                    if (!checkContext()) {
                        cancel();
                        return;
                    }

                    int percent = (int)((float)totalBytesRead / total * 100f);

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

                    if (progress != null)
                        progress.onProgress(totalBytesRead, total);

                    if (progressHandler != null) {
                        request.getHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                progressHandler.onProgress(totalBytesRead, total);
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public IonBodyParamsRequestBuilder progressBar(ProgressBar progressBar) {
        this.progressBar = new WeakReference<ProgressBar>(progressBar);
        return this;
    }

    @Override
    public IonBodyParamsRequestBuilder progressDialog(ProgressDialog progressDialog) {
        this.progressDialog = new WeakReference<ProgressDialog>(progressDialog);
        return this;
    }

    WeakReference<ProgressBar> progressBar;
    WeakReference<ProgressDialog> progressDialog;

    ProgressCallback progress;
    @Override
    public IonBodyParamsRequestBuilder progress(ProgressCallback callback) {
        progress = callback;
        return this;
    }

    ProgressCallback progressHandler;
    @Override
    public IonBodyParamsRequestBuilder progressHandler(ProgressCallback callback) {
        progressHandler = callback;
        return this;
    }

    <T> Future<T> execute(final DataSink sink, final boolean close, final T result) {
        return execute(sink, close, result, null);
    }


    <T> Future<T> execute(final DataSink sink, final boolean close, final T result, final Runnable cancel) {
        EmitterTransform<T> ret = new EmitterTransform<T>() {
            @Override
            protected void cancelCleanup() {
                super.cancelCleanup();
                if (close)
                    sink.close();
                if (cancel != null)
                    cancel.run();
            }

            TransformFuture<T, LoaderEmitter> self = this;
            @Override
            protected void transform(LoaderEmitter emitter) throws Exception {
                super.transform(emitter);
                Util.pump(this.emitter, sink, new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (close)
                            sink.close();
                        postExecute(self, ex, result);
                    }
                });
            }
        };
        getLoaderEmitter(ret);
        return ret;
    }

    <T> Future<T> execute(final AsyncParser<T> parser) {
        assert parser != null;
        EmitterTransform<T> ret = new EmitterTransform<T>() {
            TransformFuture<T, LoaderEmitter> self = this;
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

    @Override
    public Future<JSONObject> asJSONObject() {
        return execute(new JSONObjectParser());
    }

    @Override
    public Future<JSONArray> asJSONArray() {
        return execute(new JSONArrayParser());
    }

    @Override
    public Future<String> asString() {
        return execute(new StringParser());
    }

    @Override
    public <F extends OutputStream> Future<F> write(F outputStream, boolean close) {
        return execute(new OutputStreamDataSink(outputStream), close, outputStream);
    }

    @Override
    public <F extends OutputStream> Future<F> write(F outputStream) {
        return execute(new OutputStreamDataSink(outputStream), true, outputStream);
    }

    @Override
    public Future<File> write(final File file) {
        try {
            return execute(new OutputStreamDataSink(new FileOutputStream(file)), true, file, new Runnable() {
                @Override
                public void run() {
                    file.delete();
                }
            });
        }
        catch (Exception e) {
            SimpleFuture<File> ret = new SimpleFuture<File>();
            ret.setComplete(e);
            return ret;
        }
    }

    Multimap bodyParameters;
    @Override
    public IonUrlEncodedBodyRequestBuilder setBodyParameter(String name, String value) {
        if (bodyParameters == null) {
            bodyParameters = new Multimap();
            setBody(new UrlEncodedFormBody(bodyParameters));
        }
        bodyParameters.add(name, value);
        return this;
    }

    MultipartFormDataBody multipartBody;
    @Override
    public IonFormMultipartBodyRequestBuilder setMultipartFile(String name, File file) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.addFilePart(name, file);
        return this;
    }

    @Override
    public IonFormMultipartBodyRequestBuilder setMultipartParameter(String name, String value) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.addStringPart(name, value);
        return this;
    }

    @Override
    public IonMutableBitmapRequestBuilder withBitmap() {
        return new IonBitmapRequestBuilder(this);
    }

    IonMutableBitmapRequestPostLoadBuilder withImageView(ImageView imageView) {
        return new IonBitmapRequestBuilder(this).withImageView(imageView);
    }

    @Override
    public Future<Bitmap> intoImageView(ImageView imageView) {
        return new IonBitmapRequestBuilder(this).intoImageView(imageView);
    }

    @Override
    public IonFutureRequestBuilder load(File file) {
        loadInternal(null, file.toURI().toString());
        return this;
    }

    @Override
    public Future<Bitmap> asBitmap() {
        return new IonBitmapRequestBuilder(this).asBitmap();
    }

    @Override
    public IonBodyParamsRequestBuilder setLogging(String tag, int level) {
        request.setLogging(tag, level);
        return this;
    }
}
