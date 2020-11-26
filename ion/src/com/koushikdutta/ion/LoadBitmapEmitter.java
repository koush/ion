package com.koushikdutta.ion;

class LoadBitmapEmitter extends LoadBitmapBase {
    final boolean animateGif;

    public LoadBitmapEmitter(Ion ion, String urlKey, boolean put, boolean animateGif) {
        super(ion, urlKey, put);
        this.animateGif = animateGif;
    }
}