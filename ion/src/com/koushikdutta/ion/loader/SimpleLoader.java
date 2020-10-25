package com.koushikdutta.ion.loader;

import android.content.Context;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.IonRequestOptions;
import com.koushikdutta.ion.Loader;
import com.koushikdutta.ion.Response;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.scratch.http.AsyncHttpRequest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

import kotlinx.coroutines.Deferred;

/**
 * Created by koush on 12/22/13.
 */
public class SimpleLoader implements Loader {
    @Nullable
    @Override
    public Deferred<LoaderResult> load(@NotNull Ion ion, @NotNull IonRequestOptions options, @NotNull AsyncHttpRequest request) {
        return null;
    }

    @Nullable
    @Override
    public Deferred<BitmapInfo> loadBitmap(@NotNull Context context, @NotNull Ion ion, @NotNull String key, @NotNull AsyncHttpRequest request, int resizeWidth, int resizeHeight, boolean animateGif) {
        return null;
    }

    @Nullable
    @Override
    public Deferred<AsyncHttpRequest> resolve(@NotNull Context context, @NotNull Ion ion, @NotNull AsyncHttpRequest request) {
        return null;
    }

    @Nullable
    @Override
    public <T> Deferred<Response<T>> load(@NotNull Ion ion, @NotNull AsyncHttpRequest request, @NotNull Class<T> type) {
        return null;
    }
}
