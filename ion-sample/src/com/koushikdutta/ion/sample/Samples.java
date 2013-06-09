package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by koush on 5/31/13.
 */
public class Samples extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.samples);

        Button twitter = (Button)findViewById(R.id.twitter);
        twitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, Twitter.class));
            }
        });

        Button twitterGson = (Button)findViewById(R.id.twitter_gson);
        twitterGson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, TwitterGson.class));
            }
        });

        Button fileDownload = (Button)findViewById(R.id.download);
        fileDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, ProgressBarDownload.class));
            }
        });

        Button googleImageSearch = (Button)findViewById(R.id.google_image_search);
        googleImageSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, GoogleImageSearch.class));
            }
        });

        Button imageViewSample = (Button)findViewById(R.id.image_view);
        imageViewSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, ImageViewSample.class));
            }
        });
    }
}
