package com.koushikdutta.ion.loader;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FileDataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by koush on 5/22/13.
 */
public class FileLoader extends SimpleLoader {
    private static final class FileFuture extends SimpleFuture<DataEmitter> {
    }

    @Override
    public Future<BitmapInfo> loadBitmap(final Ion ion, final String key, final String uri, final int resizeWidth, final int resizeHeight) {
        if (uri == null || !uri.startsWith("file:/"))
            return null;

        final SimpleFuture<BitmapInfo> ret = new SimpleFuture<BitmapInfo>();

        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if (ret.isCancelled()) {
//                    Log.d("FileLoader", "Bitmap load cancelled (no longer needed)");
                    return;
                }
                try {
                    FileInputStream fin = new FileInputStream(new File(URI.create(uri)));
                    Point size = new Point();
                    Bitmap bitmap = ion.getBitmapCache().loadBitmap(fin, resizeWidth, resizeHeight, size);
                    if (bitmap == null)
                        throw new Exception("Bitmap failed to load");
                    BitmapInfo info = new BitmapInfo(key, new Bitmap[] { bitmap }, size);
                    info.loadedFrom =  Loader.LoaderEmitter.LOADED_FROM_CACHE;
                    fin.close();
                    ret.setComplete(info);
                }
                catch (OutOfMemoryError e) {
                    ret.setComplete(new Exception(e), null);
                }
                catch (Exception e) {
                    ret.setComplete(e);
                }
            }
        });

        return ret;
    }

    @Override
    public Future<InputStream> load(final Ion ion, final AsyncHttpRequest request) {
        if (!request.getUri().getScheme().startsWith("file"))
            return null;
        final SimpleFuture<InputStream> ret = new SimpleFuture<InputStream>();
        Ion.getIoExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream stream = new FileInputStream(new File(request.getUri()));
                    ret.setComplete(stream);
                } catch (Exception e) {
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
                callback.onCompleted(null, new LoaderEmitter(emitter, (int)file.length(), LoaderEmitter.LOADED_FROM_CACHE, null, request));
            }
        });
        return ret;
    }
}
