package com.koushikdutta.ion;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.libcore.IoUtils;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.gif.GifAction;
import com.koushikdutta.ion.gif.GifDecoder;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by koush on 1/5/14.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class LoadDeepZoom extends LoadBitmapEmitter implements FutureCallback<File> {
    public LoadDeepZoom(Ion ion, String urlKey, boolean animateGif, IonRequestBuilder.EmitterTransform<File> emitterTransform) {
        super(ion, urlKey, true, animateGif, emitterTransform);
    }

    @Override
    public void onCompleted(Exception e, final File file) {
        if (e != null) {
            report(e, null);
            return;
        }

        if (ion.bitmapsPending.tag(key) != this) {
//            Log.d("IonBitmapLoader", "Bitmap load cancelled (no longer needed)");
            return;
        }

        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                FileInputStream fin = null;
                try {
                    if (isGif()) {
                        fin = new FileInputStream(file);
                        GifDecoder decoder = new GifDecoder(fin, new GifAction() {
                            @Override
                            public boolean parseOk(boolean parseStatus, int frameIndex) {
                                return animateGif;
                            }
                        });
                        decoder.run();
                        if (decoder.getFrameCount() == 0)
                            throw new Exception("failed to load gif");
                        Bitmap[] bitmaps = new Bitmap[decoder.getFrameCount()];
                        int[] delays = decoder.getDelays();
                        Point size = null;
                        for (int i = 0; i < decoder.getFrameCount(); i++) {
                            Bitmap bitmap = decoder.getFrameImage(i);
                            if (bitmap == null)
                                throw new Exception("failed to load gif frame");
                            bitmaps[i] = bitmap;
                            if (size == null)
                                size = new Point(bitmap.getWidth(), bitmap.getHeight());
                        }
                        BitmapInfo info = new BitmapInfo(key, bitmaps, size);
                        info.delays = delays;
                        if (emitterTransform != null)
                            info.loadedFrom = emitterTransform.loadedFrom();
                        else
                            info.loadedFrom = Loader.LoaderEmitter.LOADED_FROM_CACHE;
                        report(null, info);
                        return;
                    }

                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(file.toString(), false);
                    Point size = new Point(decoder.getWidth(), decoder.getHeight());
                    Point targetSize = ion.bitmapCache.computeTarget(0, 0);
                    int scale = Math.max(size.x / targetSize.x, size.y / targetSize.y);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = scale;
                    Bitmap bitmap = decoder.decodeRegion(new Rect(0, 0, size.x, size.y), options);
                    if (bitmap == null)
                        throw new Exception("unable to load decoder");
                    Bitmap[] bitmaps = new Bitmap[] { bitmap };

                    BitmapInfo info = new BitmapInfo(key, bitmaps, size);
                    info.decoder = decoder;
                    info.loadedFrom = Loader.LoaderEmitter.LOADED_FROM_NETWORK;
                    report(null, info);
                } catch (Exception e) {
                    report(e, null);
                }
                finally {
                    IoUtils.closeQuietly(fin);
                }
            }
        });
    }
}
