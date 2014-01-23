package com.koushikdutta.ion;

import android.annotation.TargetApi;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.os.Build;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.io.File;

/**
 * Created by koush on 1/5/14.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class LoadMipmap extends BitmapCallback implements FutureCallback<File> {
    public LoadMipmap(Ion ion, String key) {
        super(ion, key, true);
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
                try {
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(file.toString(), false);
                    Point size = new Point(decoder.getWidth(), decoder.getHeight());
                    BitmapInfo info = new BitmapInfo(null, size);
                    info.mipmap = decoder;
                    info.loadedFrom = Loader.LoaderEmitter.LOADED_FROM_NETWORK;
                    info.key = key;
                    report(null, info);
                } catch (Exception e) {
                    report(e, null);
                }
            }
        });
    }
}
