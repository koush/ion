package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.os.Looper;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.nio.ByteBuffer;

class LoadBitmap extends BitmapCallback implements FutureCallback<ByteBufferList> {
    int resizeWidth;
    int resizeHeight;
    IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform;

    public LoadBitmap(Ion ion, String urlKey, boolean put, int resizeWidth, int resizeHeight, IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform) {
        super(ion, urlKey, put);
        this.resizeWidth = resizeWidth;
        this.resizeHeight = resizeHeight;
        this.emitterTransform = emitterTransform;
    }

    @Override
    public void onCompleted(Exception e, final ByteBufferList result) {
        if (e != null) {
            report(e, null);
            return;
        }

        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                ByteBuffer bb = null;
                try {
                	if (result.size() == 0)
                		throw new Exception("Bitmap failed to download");

                	bb = result.getAll();
                	
                    Bitmap bitmap = ion.bitmapCache.loadBitmap(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), resizeWidth, resizeHeight);

                    if (bitmap == null)
                        throw new Exception("bitmap failed to load");

                    BitmapInfo info = new BitmapInfo();
                    info.key = key;
                    info.bitmap = bitmap;
                    info.loadedFrom = emitterTransform.loadedFrom();

                    report(null, info);
                } catch (Exception e) {
                    report(e, null);
                }
                finally {
                	if (bb != null)
                		ByteBufferList.reclaim(bb);
                }
            }
        });
    }
}

    