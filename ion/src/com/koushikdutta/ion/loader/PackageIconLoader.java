package com.koushikdutta.ion.loader;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ResponseServedFrom;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.net.URI;

/**
 * Created by koush on 11/3/13.
 */
public class PackageIconLoader extends SimpleLoader {
    @Override
    public Future<BitmapInfo> loadBitmap(Context context, final Ion ion, final String key, final String uri, int resizeWidth, int resizeHeight, boolean animateGif) {
        if (uri == null || !uri.startsWith("package:"))
            return null;

        final SimpleFuture<BitmapInfo> ret = new SimpleFuture<BitmapInfo>();
        Ion.getBitmapLoadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final URI request = URI.create(uri);
                    String pkg = request.getHost();
                    PackageManager pm = ion.getContext().getPackageManager();
                    Bitmap bmp = ((BitmapDrawable)pm.getPackageInfo(pkg, 0).applicationInfo.loadIcon(pm)).getBitmap();
                    if (bmp == null)
                        throw new Exception("package icon failed to load");
                    BitmapInfo info = new BitmapInfo(key, null, bmp, new Point(bmp.getWidth(), bmp.getHeight()));
                    info.servedFrom = ResponseServedFrom.LOADED_FROM_CACHE;
                    ret.setComplete(info);
                }
                catch (Exception e) {
                    ret.setComplete(e);
                }
            }
        });

        return ret;
    }
}
