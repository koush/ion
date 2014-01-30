package com.koushikdutta.ion;

import com.koushikdutta.async.ByteBufferList;

import java.net.URI;

class LoadBitmapEmitter extends LoadBitmapBase {
    IonRequestBuilder.EmitterTransform<ByteBufferList> emitterTransform;
    boolean animateGif;

    public LoadBitmapEmitter(Ion ion, String urlKey, boolean put, boolean animateGif, IonRequestBuilder.EmitterTransform emitterTransform) {
        super(ion, urlKey, put);
        this.animateGif = animateGif;
        this.emitterTransform = emitterTransform;
    }

    protected boolean isGif() {
        if (emitterTransform == null)
            return false;
        if (emitterTransform.finalRequest != null) {
            URI uri = emitterTransform.finalRequest.getUri();
            if (uri != null && uri.toString().endsWith(".gif"))
                return true;
        }
        if (emitterTransform.headers == null)
            return false;
        return "image/gif".equals(emitterTransform.headers.get("Content-Type"));
    }
}

    