package com.koushikdutta.ion;

import android.graphics.Bitmap;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.Transform;

import java.util.ArrayList;

class BitmapToBitmapInfo extends BitmapCallback implements FutureCallback<BitmapInfo> {
    ArrayList<Transform> transforms;

    public BitmapToBitmapInfo(Ion ion, String transformKey, ArrayList<Transform> transforms) {
        super(ion, transformKey, true);
        this.transforms = transforms;
    }

    @Override
    public void onCompleted(Exception e, final BitmapInfo result) {
        if (e != null) {
            report(e, null);
            return;
        }

        ion.getServer().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap tmpBitmap = result.bitmap;
                    for (Transform transform : transforms) {
//                            builder.request.logd("applying transform: " + transform.key());
                        tmpBitmap = transform.transform(tmpBitmap);
                    }
                    BitmapInfo info = new BitmapInfo();
                    info.loadedFrom = result.loadedFrom;
                    info.bitmap = tmpBitmap;
                    info.key = key;
                    report(null, info);
                } catch (Exception e) {
                    report(e, null);
                }
            }
        });
    }
}