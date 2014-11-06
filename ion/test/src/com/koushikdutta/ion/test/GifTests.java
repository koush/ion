package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.gif.GifDecoder;
import com.koushikdutta.ion.gif.GifFrame;

/**
 * Created by koush on 10/31/14.
 */
public class GifTests extends AndroidTestCase {
    public void testGif() throws Exception {
        byte[] bytes = Ion.with(getContext())
        .load("https://raw2.github.com/koush/ion/master/ion-sample/mark.gif")
        .asByteArray()
        .get();

        GifDecoder decoder = new GifDecoder(bytes);

        GifFrame frame = decoder.nextFrame();
        frame = decoder.nextFrame();
    }
}
