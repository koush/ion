package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.ion.bitmap.Transform;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by koush on 5/23/13.
 */
public class IonBitmapRequestBuilder implements IonRequestBuilderStages.IonMutableBitmapRequestBuilder {
    IonRequestBuilder builder;
    Ion ion;
    public IonBitmapRequestBuilder(IonRequestBuilder builder) {
        this.builder = builder;
        ion = builder.ion;
    }

    ArrayList<Transform> transforms;
    @Override
    public IonRequestBuilderStages.IonMutableBitmapRequestBuilder transform(Transform transform) {
        if (transforms == null)
            transforms = new ArrayList<Transform>();
        transforms.add(transform);
        return this;
    }

    @Override
    public Future<Bitmap> intoImageView(ImageView imageView) {
        ion.pendingViews.remove(imageView);

        final String url = builder.request.getUri().toString();

        final SimpleFuture<Bitmap> ret = new SimpleFuture<Bitmap>();
        BitmapDrawable bd = builder.ion.bitmapCache.getDrawable(url);
        if (bd != null) {
            ret.setComplete(bd.getBitmap());
            return ret;
        }

        ArrayList<SimpleFuture<BitmapDrawable>> pd = ion.pendingDownloads.get(url);
        boolean needsLoad = pd == null;
        if (pd == null) {
            pd = new ArrayList<SimpleFuture<BitmapDrawable>>();
            ion.pendingDownloads.put(url, pd);
        }
        final ArrayList<SimpleFuture<BitmapDrawable>> pendingDownloads = pd;

        final WeakReference<ImageView> iv = new WeakReference<ImageView>(imageView);
        SimpleFuture<BitmapDrawable> waiter = new SimpleFuture<BitmapDrawable>();
        waiter.setCallback(new FutureCallback<BitmapDrawable>() {
            @Override
            public void onCompleted(Exception e, BitmapDrawable result) {
                if (e != null) {
                    ret.setComplete(e);
                    return;
                }

                ImageView imageView = iv.get();
                if (imageView != null)
                    imageView.setImageDrawable(result);
                ret.setComplete(result.getBitmap());
            }
        });

        pendingDownloads.add(waiter);

        if (!needsLoad)
            return ret;

        builder.execute(new BitmapBody()).setCallback(new FutureCallback<Bitmap>() {
            @Override
            public void onCompleted(Exception e, Bitmap result) {
                if (e != null) {
                    ret.setComplete(e);
                    return;
                }

                IonBitmapCache.ZombieDrawable zd = ion.bitmapCache.put(url, result);

                while (pendingDownloads.size() > 0) {
                    pendingDownloads.remove(0).setComplete(zd.cloneAndIncrementRefCounter());
                }
            }
        });

        return ret;
    }

    @Override
    public Future<Bitmap> asBitmap() {
        return intoImageView(null);
    }
}
