package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.text.TextUtils;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;
import com.koushikdutta.ion.gif.GifDecoder;
import com.koushikdutta.ion.gif.GifFrame;

import java.nio.ByteBuffer;

class LoadBitmap extends LoadBitmapEmitter implements FutureCallback<Response<ByteBufferList>> {
    int resizeWidth;
    int resizeHeight;

    public LoadBitmap(Ion ion, String urlKey, boolean put, int resizeWidth, int resizeHeight, boolean animateGif) {
        super(ion, urlKey, put, animateGif);
        this.resizeWidth = resizeWidth;
        this.resizeHeight = resizeHeight;
    }

    @Override
    public void onCompleted(Exception e, final Response<ByteBufferList> response) {
        if (e == null)
            e = response.getException();
        if (e != null) {
            report(e, null);
            return;
        }
        final ByteBufferList result = response.getResult();

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

                ByteBuffer bb = null;
                try {
                    bb = result.getAll();

                    Bitmap bitmap;
                    GifDecoder gifDecoder;
                    BitmapFactory.Options options = ion.bitmapCache.prepareBitmapOptions(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), resizeWidth, resizeHeight);
                    final Point size = new Point(options.outWidth, options.outHeight);
                    if (animateGif && TextUtils.equals("image/gif", options.outMimeType)) {
//                        new GifDecoder(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
                        gifDecoder = new GifDecoder(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
                        GifFrame frame = gifDecoder.nextFrame();
                        bitmap = frame.image;
                        // the byte buffer is needed by the decoder
                        bb = null;
                    }
                    else {
                        bitmap = IonBitmapCache.loadBitmap(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), options);
                        gifDecoder = null;
                        if (bitmap == null)
                            throw new Exception("failed to load bitmap");
                    }

                    BitmapInfo info = new BitmapInfo(key, options.outMimeType, bitmap, size);
                    info.gifDecoder = gifDecoder;
                    info.servedFrom = response.getServedFrom();

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
