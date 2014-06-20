package com.koushikdutta.ion.loader;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.text.TextUtils;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.util.StreamUtility;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;
import com.koushikdutta.ion.gif.GifAction;
import com.koushikdutta.ion.gif.GifDecoder;

import java.io.InputStream;

/**
 * Created by koush on 6/20/14.
 */
public class ResourceLoader extends SimpleLoader {
    @Override
    public Future<BitmapInfo> loadBitmap(final Context context, final Ion ion, final String key, final String uri, final int resizeWidth, final int resizeHeight, final boolean animateGif) {
        if (uri == null || !uri.startsWith("android.resource:/"))
            return null;

        final SimpleFuture<BitmapInfo> ret = new SimpleFuture<BitmapInfo>();

//        Log.d("FileLoader", "Loading file bitmap " + uri + " " + resizeWidth + "," + resizeHeight);
        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri u = Uri.parse(uri);
                    if (u.getPathSegments() == null)
                        throw new IllegalArgumentException("uri is not a valid resource uri");
                    String pkg = u.getAuthority();
                    Context ctx = context.createPackageContext(pkg, 0);
                    Resources res = ctx.getResources();
                    int id;
                    if (u.getPathSegments().size() == 1)
                        id = Integer.valueOf(u.getPathSegments().get(0));
                    else if (u.getPathSegments().size() == 2) {
                        String type = u.getPathSegments().get(0);
                        String name = u.getPathSegments().get(1);
                        id = res.getIdentifier(name, type, pkg);
                    }
                    else {
                        throw new IllegalArgumentException("uri is not a valid resource uri");
                    }

                    BitmapFactory.Options options = ion.getBitmapCache().prepareBitmapOptions(res, id, resizeWidth, resizeHeight);
                    if (options == null)
                        throw new Exception("BitmapFactory.Options failed to load");
                    Point size = new Point(options.outWidth, options.outHeight);
                    Bitmap[] bitmaps;
                    int[] delays;
                    if (animateGif && TextUtils.equals("image/gif", options.outMimeType)) {
                        InputStream in = res.openRawResource(id);
                        GifDecoder decoder = new GifDecoder(in, new GifAction() {
                            @Override
                            public boolean parseOk(boolean parseStatus, int frameIndex) {
                                return animateGif;
                            }
                        });
                        decoder.run();
                        StreamUtility.closeQuietly(in);
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
                        Bitmap bitmap = IonBitmapCache.loadBitmap(res, id, options);
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
}
