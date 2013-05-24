package com.koushikdutta.ion;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.ImageView;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataParser;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.*;
import com.koushikdutta.async.stream.OutputStreamDataCallback;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private <T> void postExecute(final SimpleFuture<T> future, Exception ex, final DataParser<T> body) {
        T value = null;
        try {
            if (ex == null)
                value = body.get();
        }
        catch (Exception e) {
            ex = e;
        }
        postExecute(future, ex, value);
    }

    private <T> void execute(final SimpleFuture<T> ret, final DataEmitter emitter, final DataParser<T> parser) {
        emitter.setDataCallback(parser);
        emitter.setEndCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                postExecute(ret, ex, parser);
            }
        });
    }

    <T> Future<T> execute(final DataParser<T> parser) {
        final SimpleFuture<T> ret = new SimpleFuture<T>();
        for (Loader loader: ion.config.loaders) {
            Future<DataEmitter> emitter = loader.load(ion, request);
            if (emitter != null) {
                emitter.setCallback(new FutureCallback<DataEmitter>() {
                    @Override
                    public void onCompleted(Exception e, DataEmitter result) {
                        if (e != null) {
                            postExecute(ret, e, parser);
                            return;
                        }

                        execute(ret, result, parser);
                    }
                });
                ret.setParent(emitter);
                return ret;
            }
        }
        ret.setComplete(new Exception("Unknown uri scheme"));
        return ret;
    }

    @Override
    public Future<JSONObject> asJSONObject() {
        return execute(new JSONObjectBody());
    }

    @Override
    public Future<String> asString() {
        return execute(new StringBody());
    }

    private static class OutputStreamWriter extends OutputStreamDataCallback implements DataParser<OutputStream> {
        private boolean close;
        public OutputStreamWriter(OutputStream outputStream, boolean close) {
            super(outputStream);
            this.close = close;
        }
        @Override
        public OutputStream get() {
            OutputStream ret = getOutputStream();
            if (close) {
                try {
                    ret.close();
                }
                catch (Exception e) {
                }
            }
            return ret;
        }
    }

    @Override
    public Future<OutputStream> write(OutputStream outputStream, boolean close) {
        return execute(new OutputStreamWriter(outputStream, close));
    }

    @Override
    public Future<OutputStream> write(OutputStream outputStream) {
        return execute(new OutputStreamWriter(outputStream, true));
    }

    private static class FileWriter extends OutputStreamDataCallback implements DataParser<File> {
        private File file;
        public FileWriter(File file) throws IOException {
            super(new FileOutputStream(file));
            this.file = file;
        }
        @Override
        public File get() {
            OutputStream ret = getOutputStream();
            try {
                ret.close();
            }
            catch (Exception e) {
            }
            return file;
        }
    }

    @Override
    public Future<File> write(File file) {
        try {
            return execute(new FileWriter(file));
        }
        catch (Exception e) {
            SimpleFuture<File> ret = new SimpleFuture<File>();
            ret.setComplete(e);
            return ret;
        }
    }

    ArrayList<NameValuePair> bodyParameters;
    @Override
    public IonRequestBuilderStages.IonUrlEncodedBodyRequestBuilder setBodyParameter(String name, String value) {
        if (bodyParameters == null) {
            bodyParameters = new ArrayList<NameValuePair>();
            setBody(new UrlEncodedFormBody(bodyParameters));
        }
        bodyParameters.add(new BasicNameValuePair(name, value));
        return this;
    }

    MultipartFormDataBody multipartBody;
    @Override
    public IonRequestBuilderStages.IonFormMultipartBodyRequestBuilder setMultiparFile(String name, File file) {
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
        return null;
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
}
