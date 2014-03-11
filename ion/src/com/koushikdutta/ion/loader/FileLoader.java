package com.koushikdutta.ion.loader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FileDataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.libcore.IoUtils;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.gif.GifAction;
import com.koushikdutta.ion.gif.GifDecoder;

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
    public Future<BitmapInfo> loadBitmap(final Ion ion, final String key, final String uri, final int resizeWidth, final int resizeHeight,
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
                    if (options == null)
                        throw new Exception("BitmapFactory.Options failed to load");
                    Point size = new Point(options.outWidth, options.outHeight);
                    Bitmap[] bitmaps;
                    int[] delays;
                    if (animateGif && TextUtils.equals("image/gif", options.outMimeType)) {
                        FileInputStream fin = new FileInputStream(file);
                        GifDecoder decoder = new GifDecoder(fin, new GifAction() {
                            @Override
                            public boolean parseOk(boolean parseStatus, int frameIndex) {
                                return animateGif;
                            }
                        });
                        decoder.run();
                        IoUtils.closeQuietly(fin);
                        if (decoder.getFrameCount() == 0)
                            throw new Exception("failed to load gif");
                        bitmaps = new Bitmap[decoder.getFrameCount()];
                        delays = decoder.getDelays();
                        for (int i = 0; i < decoder.getFrameCount(); i++) {
                            Bitmap bitmap = decoder.getFrameImage(i);
                            if (bitmap == null)
                                throw new Exception("failed to load gif frame");
                            bitmaps[i] = bitmap;
                        }
                    }
                    else {
                        Bitmap bitmap = ion.getBitmapCache().loadBitmap(file, options);
                        if (bitmap == null)
                            throw new Exception("Bitmap failed to load");
                        bitmaps = new Bitmap[] { bitmap };
                        delays = null;
                    }
                    BitmapInfo info = new BitmapInfo(key, options.outMimeType, bitmaps, size);
                    info.delays = delays;
                    info.loadedFrom =  Loader.LoaderEmitter.LOADED_FROM_CACHE;
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
