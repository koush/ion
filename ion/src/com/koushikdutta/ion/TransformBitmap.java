package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.Point;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.util.FileCache;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.IonBitmapCache;
import com.koushikdutta.ion.bitmap.PostProcess;
import com.koushikdutta.ion.bitmap.Transform;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

class TransformBitmap extends BitmapCallback implements FutureCallback<BitmapInfo> {
    static class PostProcessNullTransform implements Transform {
        String key;
        public PostProcessNullTransform(String key) {
            this.key = key;
        }

        @Override
        public Bitmap transform(Bitmap b) {
            return b;
        }

        @Override
        public String key() {
            return key;
        }
    }

    ArrayList<Transform> transforms;
    ArrayList<PostProcess> postProcess;

    String downloadKey;
    public TransformBitmap(Ion ion, String transformKey, String downloadKey, ArrayList<Transform> transforms, ArrayList<PostProcess> postProcess) {
        super(ion, transformKey, true);
        this.transforms = transforms;
        this.downloadKey = downloadKey;
        this.postProcess = postProcess;
    }

    @Override
    public void onCompleted(Exception e, final BitmapInfo result) {
        if (e != null) {
            report(e, null);
            return;
        }

        if (ion.bitmapsPending.tag(key) != this) {
//            Log.d("IonBitmapLoader", "Bitmap transform cancelled (no longer needed)");
            return;
        }

        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if (ion.bitmapsPending.tag(key) != TransformBitmap.this) {
//            Log.d("IonBitmapLoader", "Bitmap transform cancelled (no longer needed)");
                    return;
                }

                BitmapInfo info;
                try {
                    Bitmap bitmaps[] = new Bitmap[result.bitmaps.length];
                    for (int i = 0; i < result.bitmaps.length; i++) {
                        bitmaps[i] = result.bitmaps[i];
                        for (Transform transform : transforms) {
                            Bitmap bitmap = transform.transform(bitmaps[i]);
                            if (bitmap == null)
                                throw new Exception("failed to transform bitmap");
                            bitmaps[i] = bitmap;
                        }
                    }
                    info = new BitmapInfo(key, result.mimeType, bitmaps, result.originalSize);
                    info.delays = result.delays;
                    info.loadedFrom = result.loadedFrom;

                    if (postProcess != null) {
                        for (PostProcess p: postProcess) {
                            p.postProcess(info);
                        }
                    }

                    report(null, info);
                }
                catch (OutOfMemoryError e) {
                    report(new Exception(e), null);
                }
                catch (Exception e) {
                    report(e, null);
                }
            }
        });
    }
}