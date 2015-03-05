package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 3/4/15.
 */
public class RoundedImageViewSample extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rounded);

        // RoundedImageView library:
        // https://github.com/vinc3m1/RoundedImageView

        Ion.with((ImageView) findViewById(R.id.image0))
                .load("file:///android_asset/Androidify25.png");

        Ion.with((ImageView) findViewById(R.id.image1))
                .load("file:///android_asset/Androidify26.png");

        Ion.with((ImageView) findViewById(R.id.image2))
                .load("file:///android_asset/Androidify27.png");

    }
}
