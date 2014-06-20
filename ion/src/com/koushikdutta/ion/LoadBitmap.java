package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;
import com.koushikdutta.ion.gif.GifAction;
import com.koushikdutta.ion.gif.GifDecoder;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class LoadBitmap extends LoadBitmapEmitter implements FutureCallback<ByteBufferList> {
    int resizeWidth;
    int resizeHeight;

    public LoadBitmap(Ion ion, String urlKey, boolean put, int resizeWidth, int resizeHeight, boolean animateGif, IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform) {
        super(ion, urlKey, put, animateGif, emitterTransform);
        this.resizeWidth = resizeWidth;
        this.resizeHeight = resizeHeight;
        this.animateGif = animateGif;
        this.emitterTransform = emitterTransform;
    }

    @Override
    public void onCompleted(Exception e, final ByteBufferList result) {
        if (e != null) {
            report(e, null);
            return;
        }

        if (ion.bitmapsPending.tag(key) != this) {
            result.recycle();
            return;
        }

        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if (ion.bitmapsPending.tag(key) != LoadBitmap.this) {
                    result.recycle();
                    return;
                }

                ByteBuffer bb = result.getAll();
                try {
                    Bitmap[] bitmaps;
                    int[] delays;
                    BitmapFactory.Options options = ion.bitmapCache.prepareBitmapOptions(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), resizeWidth, resizeHeight);
                    if (options == null)
                        throw new Exception("BitmapFactory.Options failed to load");
                    final Point size = new Point(options.outWidth, options.outHeight);
                    if (animateGif && TextUtils.equals("image/gif", options.outMimeType)) {
                        GifDecoder decoder = new GifDecoder(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), new GifAction() {
                            @Override
                            public boolean parseOk(boolean parseStatus, int frameIndex) {
                                return animateGif;
                            }
                        });
                        decoder.run();
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
                        Bitmap bitmap = IonBitmapCache.loadBitmap(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), options);
                        if (bitmap == null)
                            throw new Exception("failed to load bitmap");
                        bitmaps = new Bitmap[] { bitmap };
                        delays = null;
                    }

                    BitmapInfo info = new BitmapInfo(key, options.outMimeType, bitmaps, size);
                    info.delays = delays;
                    if (emitterTransform != null)
                        info.loadedFrom = emitterTransform.loadedFrom();
                    else
                        info.loadedFrom = Loader.LoaderEmitter.LOADED_FROM_CACHE;

                    report(null, info);
                }
                catch (OutOfMemoryError e) {
                    report(new Exception(e), null);
                }
                catch (Exception e) {
                    report(e, null);
                }
                finally {
                    ByteBufferList.reclaim(bb);
                }
            }
        });
    }
}

    