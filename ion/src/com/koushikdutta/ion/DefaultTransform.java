package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import com.koushikdutta.ion.bitmap.Transform;

import java.io.FileOutputStream;

class DefaultTransform implements Transform {
    ScaleMode scaleMode;
    int resizeWidth;
    int resizeHeight;

    public DefaultTransform(int width, int height, ScaleMode scaleMode) {
        resizeWidth = width;
        resizeHeight = height;
        this.scaleMode = scaleMode;
    }

    @Override
    public Bitmap transform(Bitmap b) {
        Bitmap ret = Bitmap.createBitmap(resizeWidth, resizeHeight, b.getConfig());
        Canvas canvas = new Canvas(ret);

        RectF destination = new RectF(0, 0, resizeWidth, resizeHeight);
        if (scaleMode != ScaleMode.FitXY) {
            float ratio;
            float xratio = (float)resizeWidth / (float)b.getWidth();
            float yratio = (float)resizeHeight / (float)b.getHeight();
            if (scaleMode == ScaleMode.CenterCrop)
                ratio = Math.max(xratio, yratio);
            else
                ratio = Math.min(xratio, yratio);

            float postWidth = b.getWidth() * ratio;
            float postHeight = b.getHeight() * ratio;
            float transx = (resizeWidth - postWidth) / 2;
            float transy = (resizeHeight - postHeight) / 2;
            destination.set(transx, transy, transx + postWidth, transy + postHeight);
        }

        canvas.drawBitmap(b, null, destination, null);
        return ret;
    }

    @Override
    public String key() {
        return scaleMode.name() + resizeWidth  + "x" + resizeHeight;
    }
}
