package com.koushikdutta.ion;

import android.content.Context;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.ResponseCacheMiddleware;

import java.io.File;

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

    ResponseCacheMiddleware responseCache;
    AsyncHttpClient httpClient;
    private Ion(Context context) {
        httpClient = new AsyncHttpClient(new AsyncServer());

        try {
            responseCache = ResponseCacheMiddleware.addCache(httpClient, new File(context.getCacheDir(), "ion"), 10L * 1024L * 1024L);
        }
        catch (Exception e) {
            IonLog.w("unable to set up response cache", e);
        }
    }

    private static Ion instance;
}
