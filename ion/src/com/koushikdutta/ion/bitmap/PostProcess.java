package com.koushikdutta.ion.bitmap;

/**
 * Created by koush on 7/1/14.
 */
public interface PostProcess {
    public void postProcess(BitmapInfo info) throws Exception;
    public String key();
}
