package com.koushikdutta.ion.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.text.TextUtils;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.util.StreamUtility;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;
import com.koushikdutta.ion.ResponseServedFrom;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;
import com.koushikdutta.ion.gif.GifDecoder;
import com.koushikdutta.ion.gif.GifFrame;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by koush on 6/27/14.
 */
public class StreamLoader extends SimpleLoader {
    protected BitmapInfo loadGif(String key, Point size, InputStream in, BitmapFactory.Options options) throws Exception {
        GifDecoder gifDecoder = new GifDecoder(ByteBuffer.wrap(StreamUtility.readToEndAsArray(in)));
        GifFrame frame = gifDecoder.nextFrame();
        BitmapInfo info = new BitmapInfo(key, options.outMimeType, frame.image, size);
        info.gifDecoder = gifDecoder;
        return info;
    }

    protected InputStream getInputStream(Context context, String uri) throws Exception {
        return null;
    }

    @Override
    public Future<BitmapInfo> loadBitmap(final Context context, final Ion ion, final String key, final String uri, final int resizeWidth, final int resizeHeight, final boolean animateGif) {
        final SimpleFuture<BitmapInfo> ret = new SimpleFuture<BitmapInfo>();

//        Log.d("FileLoader", "Loading file bitmap " + uri + " " + resizeWidth + "," + resizeHeight);
        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                InputStream in = null;
                try {
                    in = getInputStream(context, uri);
                    BitmapFactory.Options options = ion.getBitmapCache().prepareBitmapOptions(in, resizeWidth, resizeHeight);
                    StreamUtility.closeQuietly(in);
                    Point size = new Point(options.outWidth, options.outHeight);
                    BitmapInfo info;
                    in = getInputStream(context, uri);
                    if (animateGif && TextUtils.equals("image/gif", options.outMimeType)) {
                        info = loadGif(key, size, in, options);
                    }
                    else {
                        Bitmap bitmap = IonBitmapCache.loadBitmap(in, options);
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
                finally {
                    StreamUtility.closeQuietly(in);
                }
            }
        });

        return ret;
    }
}
