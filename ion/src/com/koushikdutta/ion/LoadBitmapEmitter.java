package com.koushikdutta.ion;

class LoadBitmapEmitter extends LoadBitmapBase {
    IonRequestBuilder.EmitterTransform emitterTransform;
    boolean animateGif;

    public LoadBitmapEmitter(Ion ion, String urlKey, boolean put, boolean animateGif, IonRequestBuilder.EmitterTransform emitterTransform) {
        super(ion, urlKey, put);
        this.animateGif = animateGif;
        this.emitterTransform = emitterTransform;
    }
}