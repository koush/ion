package com.koushikdutta.ion;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.ImageView;
import com.koushikdutta.async.*;
import com.koushikdutta.async.DataTrackingEmitter.DataTracker;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.http.*;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.JSONObjectParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.async.stream.OutputStreamDataSink;
import com.koushikdutta.ion.IonRequestBuilderStages.IonBodyParamsRequestBuilder;
import com.koushikdutta.ion.Loader.LoaderEmitter;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by koush on 5/21/13.
 */
class IonRequestBuilder implements IonRequestBuilderStages.IonLoadRequestBuilder, IonRequestBuilderStages.IonBodyParamsRequestBuilder {
    AsyncHttpRequest request;
    Ion ion;
    WeakReference<Context> context;

    public IonRequestBuilder(Context context, Ion ion) {
        this.ion = ion;
        this.context = new WeakReference<Context>(context);
    }

    @Override
    public IonRequestBuilderStages.IonBodyParamsRequestBuilder load(String url) {
        return loadInternal(AsyncHttpGet.METHOD, url);
    }

    private IonRequestBuilderStages.IonBodyParamsRequestBuilder  loadInternal(String method, String url) {
        request = new AsyncHttpRequest(URI.create(url), method);
        setLogging(ion.LOGTAG, ion.logLevel);
        return this;
    }

    boolean methodWasSet;
    @Override
    public IonRequestBuilderStages.IonBodyParamsRequestBuilder load(String method, String url) {
        methodWasSet = true;
        return loadInternal(method, url);
    }

    @Override
    public IonRequestBuilderStages.IonBodyParamsRequestBuilder setHeader(String name, String value) {
        request.setHeader(name, value);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonBodyParamsRequestBuilder addHeader(String name, String value) {
        request.addHeader(name, value);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonBodyParamsRequestBuilder setTimeout(int timeoutMilliseconds) {
        request.setTimeout(timeoutMilliseconds);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonBodyParamsRequestBuilder setHandler(Handler handler) {
        request.setHandler(handler);
        return this;
    }

    private <T> IonRequestBuilderStages.IonFutureRequestBuilder setBody(AsyncHttpRequestBody<T> body) {
        request.setBody(body);
        if (!methodWasSet)
            request.setMethod(AsyncHttpPost.METHOD);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonFutureRequestBuilder setJSONObjectBody(JSONObject jsonObject) {
        setHeader("Content-Type", "application/json");
        return setBody(new JSONObjectBody(jsonObject));
    }

    @Override
    public IonRequestBuilderStages.IonFutureRequestBuilder setStringBody(String string) {
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

    private <T> void postExecute(final SimpleFuture<T> future, final Exception ex, final T value) {
        final Runnable runner = new Runnable() {
            @Override
            public void run() {
                // check if the context is still alive...
                Context context = IonRequestBuilder.this.context.get();
                if (context == null)
                    return;
                if (context instanceof Activity) {
                    Activity activity = (Activity)context;
                    if (activity.isFinishing())
                        return;
                }
                else if (context instanceof Service) {
                    Service service = (Service)context;
                    if (!isServiceRunning(service))
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

        if (request.getHandler() == null)
            ion.httpClient.getServer().post(runner);
        else
            AsyncServer.post(request.getHandler(), runner);
    }

    private <T> void getLoaderEmitter(TransformFuture<T, LoaderEmitter> ret) {
        for (Loader loader: ion.config.loaders) {
            Future<DataEmitter> emitter = loader.load(ion, request, ret);
            if (emitter != null)
                return;
        }
        ret.setComplete(new Exception("Unknown uri scheme"));
    }

    private class EmitterTransform<T> extends TransformFuture<T, LoaderEmitter> {
        DataEmitter emitter;
        @Override
        protected void cancelCleanup() {
            if (emitter != null)
                emitter.close();
        }

        @Override
        protected void transform(LoaderEmitter emitter) throws Exception {
            this.emitter = emitter.getDataEmitter();

            final ProgressCallback cb = progress;
            final int total = emitter.length();
            if (cb != null && !(emitter instanceof DataTrackingEmitter)) {
                FilteredDataEmitter filtered = new FilteredDataEmitter();
                filtered.setDataEmitter(this.emitter);
                filtered.setDataTracker(new DataTracker() {
                    @Override
                    public void onData(int totalBytesRead) {
                        cb.onProgress(totalBytesRead, total);
                    }
                });
                this.emitter = filtered;
            }
        }
    }

    ProgressCallback progress;
    @Override
    public IonBodyParamsRequestBuilder progress(ProgressCallback callback) {
        progress = callback;
        return this;
    }

    <T> Future<T> execute(final DataSink sink, final boolean close, final T result) {
        EmitterTransform<T> ret = new EmitterTransform<T>() {
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
    public Future<String> asString() {
        return execute(new StringParser());
    }

    @Override
    public <T extends OutputStream> Future<T> write(T outputStream, boolean close) {
        return execute(new OutputStreamDataSink(outputStream), close, outputStream);
    }

    @Override
    public <T extends OutputStream> Future<T> write(T outputStream) {
        return execute(new OutputStreamDataSink(outputStream), true, outputStream);
    }

    @Override
    public Future<File> write(File file) {
        try {
            return execute(new OutputStreamDataSink(new FileOutputStream(file)), true, file);
        }
        catch (Exception e) {
            SimpleFuture<File> ret = new SimpleFuture<File>();
            ret.setComplete(e);
            return ret;
        }
    }

    Multimap bodyParameters;
    @Override
    public IonRequestBuilderStages.IonUrlEncodedBodyRequestBuilder setBodyParameter(String name, String value) {
        if (bodyParameters == null) {
            bodyParameters = new Multimap();
            setBody(new UrlEncodedFormBody(bodyParameters));
        }
        bodyParameters.add(name, value);
        return this;
    }

    MultipartFormDataBody multipartBody;
    @Override
    public IonRequestBuilderStages.IonFormMultipartBodyRequestBuilder setMultipartFile(String name, File file) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.addFilePart(name, file);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonFormMultipartBodyRequestBuilder setMultipartParameter(String name, String value) {
        if (multipartBody == null) {
            multipartBody = new MultipartFormDataBody();
            setBody(multipartBody);
        }
        multipartBody.addStringPart(name, value);
        return this;
    }

    IonBitmapRequestBuilder bitmapBuilder;
    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder withBitmap() {
        if (bitmapBuilder == null)
            bitmapBuilder = new IonBitmapRequestBuilder(this);
        return bitmapBuilder;
    }

    @Override
    public Future<Bitmap> intoImageView(ImageView imageView) {
        if (bitmapBuilder == null)
            bitmapBuilder = new IonBitmapRequestBuilder(this);
        return bitmapBuilder.intoImageView(imageView);
    }

    @Override
    public IonRequestBuilderStages.IonFutureRequestBuilder load(File file) {
        loadInternal(null, file.toURI().toString());
        return this;
    }

    @Override
    public Future<Bitmap> asBitmap() {
        if (bitmapBuilder == null)
            bitmapBuilder = new IonBitmapRequestBuilder(this);
        return bitmapBuilder.asBitmap();
    }

    @Override
    public IonBodyParamsRequestBuilder setLogging(String tag, int level) {
        request.setLogging(tag, level);
        return this;
    }
}
