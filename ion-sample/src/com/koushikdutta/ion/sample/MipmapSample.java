package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.os.Bundle;

import uk.co.senab.photoview.PhotoView;

/**
 * Created by koush on 2/1/14.
 */
public class MipmapSample extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PhotoView photoView = new PhotoView(this);
        setContentView(photoView);


    }
}
