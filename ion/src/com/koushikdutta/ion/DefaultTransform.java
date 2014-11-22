package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.koushikdutta.ion.bitmap.Transform;

class DefaultTransform implements Transform {
    final ScaleMode scaleMode;
    final int resizeWidth;
    final int resizeHeight;

    public DefaultTransform(int width, int height, ScaleMode scaleMode) {
        resizeWidth = width;
        resizeHeight = height;
        // fixup ScaleMode to sane values.
        // no scale mode means that we are stretching, disregarding aspect ratio
        if (scaleMode == null)
            this.scaleMode = ScaleMode.FitXY;
        else
            this.scaleMode = scaleMode;
    }

    final static Paint bilinearSamplingPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

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

        RectF destination = new RectF(0, 0, resizeWidth, resizeHeight);
        ScaleMode scaleMode = this.scaleMode;

        if (scaleMode == ScaleMode.CenterInside) {
            // center inside, but bitmap bounds exceed the resize dimensions...
            // so change it to fit center.
            if (resizeWidth <= b.getWidth() || resizeHeight <= b.getHeight())
                scaleMode = ScaleMode.FitCenter;
        }

        if (scaleMode == ScaleMode.CenterInside) {
            float marginx = (resizeWidth - b.getWidth()) / 2f;
            float marginy = (resizeHeight - b.getHeight()) / 2f;
            destination.set(marginx, marginy, marginx + b.getWidth(), marginy + b.getHeight());
        }
        else if (scaleMode != ScaleMode.FitXY) {
            float ratio;
            float xratio = (float)resizeWidth / (float)b.getWidth();
            float yratio = (float)resizeHeight / (float)b.getHeight();
            if (scaleMode == ScaleMode.CenterCrop)
                ratio = Math.max(xratio, yratio);
            else
                ratio = Math.min(xratio, yratio);

            if (ratio == 0) return b;

            float postWidth = b.getWidth() * ratio;
            float postHeight = b.getHeight() * ratio;
            float transx = (resizeWidth - postWidth) / 2;
            float transy = (resizeHeight - postHeight) / 2;
            destination.set(transx, transy, resizeWidth - transx, resizeHeight - transy);
        }

        if (destination.width() == b.getWidth() && destination.height() == b.getHeight()
            && destination.top == 0 && destination.left == 0) {
            return b;
        }

        Bitmap ret = Bitmap.createBitmap(resizeWidth, resizeHeight, config);
        Canvas canvas = new Canvas(ret);

        canvas.drawBitmap(b, null, destination, bilinearSamplingPaint);
        return ret;
    }

    @Override
    public String key() {
        return scaleMode.name() + resizeWidth  + "x" + resizeHeight;
    }
}
