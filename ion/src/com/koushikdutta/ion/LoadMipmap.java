package com.koushikdutta.ion;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.io.File;

/**
 * Created by koush on 1/5/14.
 */
public class LoadMipmap extends BitmapCallback implements FutureCallback<File> {
    public LoadMipmap(Ion ion, String key) {
        super(ion, key, true);
    }

    @Override
    public void onCompleted(Exception e, File result) {
        if (e != null) {
            report(e, null);
            return;
        }

        if (ion.bitmapsPending.tag(key) != this) {
//            Log.d("IonBitmapLoader", "Bitmap load cancelled (no longer needed)");
            return;
        }

        ion.configure().getFileLoader().loadBitmap(ion, result.toURI().toString(), 256, 256)
        .setCallback(new FutureCallback<BitmapInfo>() {
            @Override
            public void onCompleted(Exception e, BitmapInfo result) {
                if (result != null)
                    result.key = key;
                report(e, result);
            }
        });
    }
}
