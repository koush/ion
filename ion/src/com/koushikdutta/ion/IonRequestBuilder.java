package com.koushikdutta.ion;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.os.Handler;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Continuation;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.*;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.AbstractMap;
import java.util.List;

/**
 * Created by koush on 5/21/13.
 */
class IonRequestBuilder implements IonRequestBuilderStages.IonRequestBuilderLoad, IonRequestBuilderStages.IonRequestBuilderBodyParams {
    AsyncHttpRequest request;
    Ion ion;
    WeakReference<Context> context;

    public IonRequestBuilder(Context context, Ion ion) {
        this.ion = ion;
        this.context = new WeakReference<Context>(context);
    }

    @Override
    public IonRequestBuilderStages.IonRequestBuilderBodyParams load(String url) {
        return load(AsyncHttpGet.METHOD, url);
    }

    @Override
    public IonRequestBuilderStages.IonRequestBuilderBodyParams load(String method, String url) {
        request = new AsyncHttpRequest(URI.create(url), method);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonRequestBuilderBodyParams setHeader(String name, String value) {
        request.setHeader(name, value);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonRequestBuilderBodyParams addHeader(String name, String value) {
        request.addHeader(name, value);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonRequestBuilderBodyParams setTimeout(int timeoutMilliseconds) {
        request.setTimeout(timeoutMilliseconds);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonRequestBuilderBodyParams setHandler(Handler handler) {
        request.setHandler(handler);
        return this;
    }

    private <T> IonRequestBuilderStages.IonRequestBuilderParams setBody(AsyncHttpRequestBody<T> body) {
        request.setBody(body);
        request.setMethod(AsyncHttpPost.METHOD);
        return this;
    }

    @Override
    public IonRequestBuilderStages.IonRequestBuilderParams setJSONObjectBody(JSONObject jsonObject) {
        setHeader("Content-Type", "application/json");
        return setBody(new JSONObjectBody(jsonObject));
    }

    @Override
    public IonRequestBuilderStages.IonRequestBuilderParams setStringBody(String string) {
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

    private <T> void postExecute(final SimpleFuture<T> future, final Exception ex, final AsyncHttpRequestBody<T> body) {
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

                Exception report = ex;
                T value = null;
                try {
                    value = body.getBody();
                }
                catch (Exception e) {
                    report = e;
                }

                // unless we're invoked onto the handler/main/service thread, there's no frakking way to avoid a
                // race condition where the service or activity dies before this callback is invoked.
                if (report != null)
                    future.setComplete(report);
                else
                    future.setComplete(value);
            }
        };

        if (request.getHandler() == null)
            ion.httpClient.getServer().post(runner);
        else
            AsyncServer.post(request.getHandler(), runner);
    }

    private <T> Future<T> execute(final AsyncHttpRequestBody<T> body) {
        final SimpleFuture<T> ret = new SimpleFuture<T>();
        return ret.setParent(ion.httpClient.execute(request, new HttpConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, AsyncHttpResponse response) {
                if (ex != null) {
                    postExecute(ret, ex, body);
                    return;
                }
                response.setDataCallback(body);
                response.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        postExecute(ret, ex, body);
                    }
                });
            }
        }));
    }

    @Override
    public Future<JSONObject> asJSONObject() {
        return execute(new JSONObjectBody());
    }

    @Override
    public Future<String> asString() {
        return execute(new StringBody());
    }
}
