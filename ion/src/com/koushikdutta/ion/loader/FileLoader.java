package com.koushikdutta.ion.loader;

import android.net.Uri;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FileDataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by koush on 5/22/13.
 */
public class FileLoader implements Loader {
    private static final class FileFuture extends SimpleFuture<DataEmitter> {
    }

    @Override
    public Future<InputStream> load(final Ion ion, final AsyncHttpRequest request) {
        if (!request.getUri().getScheme().startsWith("file"))
            return null;
        final SimpleFuture<InputStream> ret = new SimpleFuture<InputStream>();
        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream stream = new FileInputStream(new File(request.getUri()));
                    ret.setComplete(stream);
                }
                catch (Exception e) {
                    ret.setComplete(e);
                }
            }
        });
        return ret;
    }

    @Override
    public Future<DataEmitter> load(final Ion ion, final AsyncHttpRequest request, final FutureCallback<LoaderEmitter> callback) {
        if (!request.getUri().getScheme().startsWith("file"))
            return null;
        final FileFuture ret = new FileFuture();
        ion.getHttpClient().getServer().post(new Runnable() {
            @Override
            public void run() {
                File file = new File(request.getUri());
                FileDataEmitter emitter = new FileDataEmitter(ion.getHttpClient().getServer(), file);
                ret.setComplete(emitter);
                callback.onCompleted(null, new LoaderEmitter(emitter, (int)file.length(), LoaderEmitter.LOADED_FROM_CACHE, null, null));
            }
        });
        return ret;
    }
}
