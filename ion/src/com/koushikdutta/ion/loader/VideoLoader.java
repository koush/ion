package com.koushikdutta.ion.loader;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.io.File;
import java.net.URI;

/**
 * Created by koush on 11/6/13.
 */
public class VideoLoader extends SimpleLoader {
    private boolean useThumbnailUtils;
    public void useThumbnailUtils(boolean useThumbnailUtils) {
        this.useThumbnailUtils = useThumbnailUtils;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    public static Bitmap createVideoThumbnail(String filePath) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);
        return retriever.getFrameAtTime();
    }

    @Override
    public Future<BitmapInfo> loadBitmap(Ion ion, final String key, final String uri, int resizeWidth, int resizeHeight) {
        if (!uri.startsWith(ContentResolver.SCHEME_FILE))
            return null;

        final MediaFile.MediaFileType type = MediaFile.getFileType(uri);
        if (type == null || !MediaFile.isVideoFileType(type.fileType))
            return null;

        final SimpleFuture<BitmapInfo> ret = new SimpleFuture<BitmapInfo>();
        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final File file = new File(URI.create(uri));
                if (ret.isCancelled()) {
//                    Log.d("VideoLoader", "Bitmap load cancelled (no longer needed)");
                    return;
                }
                try {
                    Bitmap bmp;
                    if (useThumbnailUtils || Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1)
                        bmp = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
                    else
                        bmp = createVideoThumbnail(file.getAbsolutePath());
                    if (bmp == null)
                        throw new Exception("video bitmap failed to load");
                    BitmapInfo info = new BitmapInfo(key, type.mimeType, new Bitmap[] { bmp }, new Point(bmp.getWidth(), bmp.getHeight()));
                    info.loadedFrom = LoaderEmitter.LOADED_FROM_CACHE;
                    ret.setComplete(info);
                } catch (Exception e) {
                    ret.setComplete(e);
                }
            }
        });
        return ret;
    }
}
