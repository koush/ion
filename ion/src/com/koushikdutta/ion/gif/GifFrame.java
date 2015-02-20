package com.koushikdutta.ion.gif;

import android.graphics.Bitmap;

public class GifFrame {
    public GifFrame(Bitmap im, int del) {
        image = im;
        delay = del;
    }
	
    public Bitmap image;
    public int delay;
}
