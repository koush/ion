package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import com.koushikdutta.ion.bitmap.Transform;

import java.io.FileOutputStream;

class DefaultTransform implements Transform {
    final ScaleMode scaleMode;
    final int resizeWidth;
    final int resizeHeight;

    public DefaultTransform(int width, int height, ScaleMode scaleMode) {
        resizeWidth = width;
        resizeHeight = height;
        this.scaleMode = scaleMode;
    }

    @Override
    public Bitmap transform(Bitmap b) {
        Bitmap.Config config = b.getConfig();
        if (config == null)
            config = Bitmap.Config.ARGB_8888;
        int resizeWidth = this.resizeWidth;
        int resizeHeight = this.resizeHeight;
        if (resizeWidth <= 0) {
            float ratio = (float)b.getWidth() / (float)b.getHeight();
            resizeWidth = (int)(ratio * resizeHeight);
        }
        else if (resizeHeight <= 0) {
            float ratio = (float)b.getHeight() / (float)b.getWidth();
            resizeHeight = (int)(ratio * resizeWidth);
        }

        Bitmap ret = Bitmap.createBitmap(resizeWidth, resizeHeight, config);
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
            destination.set(transx, transy, resizeWidth - transx, resizeHeight - transy);
        }

        if (destination.width()==b.getWidth() && destination.height()==b.getHeight()
            && destination.top==0 && destination.left==0) {
            return b;
        }

        canvas.drawBitmap(b, null, destination, null);
        return ret;
    }

    @Override
    public String key() {
        return scaleMode.name() + resizeWidth  + "x" + resizeHeight;
    }
}
