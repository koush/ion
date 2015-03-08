package com.koushikdutta.ion.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.text.TextUtils;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FileDataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.util.StreamUtility;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ResponseServedFrom;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by koush on 5/22/13.
 */
public class FileLoader extends StreamLoader {
    private static final class FileFuture extends SimpleFuture<DataEmitter> {
    }

    @Override
    public Future<BitmapInfo> loadBitmap(final Context context, final Ion ion, final String key, final String uri, final int resizeWidth, final int resizeHeight,
                                         final boolean animateGif) {
        if (uri == null || !uri.startsWith("file:/"))
            return null;

        final SimpleFuture<BitmapInfo> ret = new SimpleFuture<BitmapInfo>();

//        Log.d("FileLoader", "Loading file bitmap " + uri + " " + resizeWidth + "," + resizeHeight);
        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if (ret.isCancelled()) {
//                    Log.d("FileLoader", "Bitmap load cancelled (no longer needed)");
                    return;
                }
                try {
                    File file = new File(URI.create(uri));
                    BitmapFactory.Options options = ion.getBitmapCache().prepareBitmapOptions(file, resizeWidth, resizeHeight);
                    Point size = new Point(options.outWidth, options.outHeight);
                    BitmapInfo info;
                    if (animateGif && TextUtils.equals("image/gif", options.outMimeType)) {
                        FileInputStream fin = new FileInputStream(file);
                        try {
                            info = loadGif(key, size, fin, options);
                        }
                        finally {
                            StreamUtility.closeQuietly(fin);
                        }
                    }
                    else {
                        Bitmap bitmap = IonBitmapCache.loadBitmap(file, options);
                        if (bitmap == null)
                            throw new Exception("Bitmap failed to load");
                        info = new BitmapInfo(key, options.outMimeType, bitmap, size);
                    }
                    info.servedFrom = ResponseServedFrom.LOADED_FROM_CACHE;
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
    public Future<DataEmitter> load(final Ion ion, final AsyncHttpRequest request, final FutureCallback<LoaderEmitter> callback) {
        if (!request.getUri().getScheme().startsWith("file"))
            return null;
        final FileFuture ret = new FileFuture();
        ion.getHttpClient().getServer().post(new Runnable() {
            @Override
            public void run() {
                File file = new File(URI.create(request.getUri().toString()));
                FileDataEmitter emitter = new FileDataEmitter(ion.getHttpClient().getServer(), file);
                ret.setComplete(emitter);
                callback.onCompleted(null, new LoaderEmitter(emitter, (int)file.length(), ResponseServedFrom.LOADED_FROM_CACHE, null, request));
            }
        });
        return ret;
    }
}
