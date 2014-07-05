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

    public static void getBitmapSnapshot(final Ion ion, final String transformKey, final ArrayList<PostProcess> postProcess) {
        // don't do this if this is already loading
        if (ion.bitmapsPending.tag(transformKey) != null)
            return;
        final BitmapCallback callback = new LoadBitmapBase(ion, transformKey, true);
        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if (ion.bitmapsPending.tag(transformKey) != callback) {
//                    Log.d("IonBitmapLoader", "Bitmap cache load cancelled (no longer needed)");
                    return;
                }

                try {
                    File file = ion.responseCache.getFileCache().getFile(transformKey);
                    Bitmap bitmap = IonBitmapCache.loadBitmap(file, null);
                    if (bitmap == null)
                        throw new Exception("Bitmap failed to load");
                    Point size = new Point(bitmap.getWidth(), bitmap.getHeight());
                    BitmapInfo info = new BitmapInfo(transformKey, "image/jpeg", new Bitmap[] { bitmap }, size);
                    info.loadedFrom =  Loader.LoaderEmitter.LOADED_FROM_CACHE;

                    if (postProcess != null) {
                        for (PostProcess p: postProcess) {
                            p.postProcess(info);
                        }
                    }

                    callback.report(null, info);
                }
                catch (OutOfMemoryError e) {
                    callback.report(new Exception(e), null);
                }
                catch (Exception e) {
                    callback.report(e, null);
                    try {
                        ion.responseCache.getFileCache().remove(transformKey);
                    } catch (Exception ex) {
                    }
                }
            }
        });
    }

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
                    Point size = null;
                    Bitmap bitmaps[] = new Bitmap[result.bitmaps.length];
                    for (int i = 0; i < result.bitmaps.length; i++) {
                        bitmaps[i] = result.bitmaps[i];
                        for (Transform transform : transforms) {
                            Bitmap bitmap = transform.transform(bitmaps[i]);
                            if (bitmap == null)
                                throw new Exception("failed to transform bitmap");
                            bitmaps[i] = bitmap;
                            if (size == null)
                                size = new Point(bitmap.getWidth(), bitmap.getHeight());
                        }
                    }
                    info = new BitmapInfo(key, result.mimeType, bitmaps, size);
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
                    return;
                }
                catch (Exception e) {
                    report(e, null);
                    return;
                }
                // the transformed bitmap was successfully load it, let's toss it into
                // the disk lru cache.
                // but don't persist gifs...
                if (info.bitmaps.length > 1)
                    return;
                FileCache cache = ion.responseCache.getFileCache();
                if (cache == null)
                    return;
                File tempFile = cache.getTempFile();
                try {
                    FileOutputStream out = new FileOutputStream(tempFile);
                    Bitmap.CompressFormat format = info.bitmaps[0].hasAlpha() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                    info.bitmaps[0].compress(format, 100, out);
                    out.close();
                    cache.commitTempFiles(key, tempFile);
                }
                catch (Exception ex) {
                }
                finally {
                    tempFile.delete();
                }
            }
        });
    }
}