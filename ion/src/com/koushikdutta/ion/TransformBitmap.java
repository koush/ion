package com.koushikdutta.ion;

import android.graphics.Bitmap;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.PostProcess;
import com.koushikdutta.ion.bitmap.Transform;

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
                    Bitmap bitmap = result.bitmap;
                    for (Transform transform : transforms) {
                        bitmap = transform.transform(bitmap);
                        if (bitmap == null)
                            throw new Exception("failed to transform bitmap");
                    }
                    info = new BitmapInfo(key, result.mimeType, bitmap, result.originalSize);
                    info.servedFrom = result.servedFrom;

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