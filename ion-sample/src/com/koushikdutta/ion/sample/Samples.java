package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
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

        Button fileUpload = (Button)findViewById(R.id.upload);
        fileUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, ProgressBarUpload.class));
            }
        });

        Button imageSearch = (Button)findViewById(R.id.image_search);
        imageSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, ImageSearch.class));
            }
        });

        Button imageViewSample = (Button)findViewById(R.id.image_view);
        imageViewSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, ImageViewSample.class));
            }
        });

        Button gallerySample = (Button)findViewById(R.id.gallery);
        gallerySample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, GallerySample.class));
            }
        });

        Button deepZoomSample = (Button)findViewById(R.id.deepzoom);
        deepZoomSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, DeepZoomSample.class));
            }
        });

        findViewById(R.id.kenburns)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Samples.this, KenBurnsSample.class));
                    }
                });

        findViewById(R.id.rounded)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Samples.this, RoundedImageViewSample.class));
                    }
                });

        Button lollipopTransitionSample = (Button)findViewById(R.id.lollipop_transition);
        if (Build.VERSION.SDK_INT >= 21) {
            lollipopTransitionSample.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Samples.this, LollipopTransition.class));
                }
            });
        }
        else {
            lollipopTransitionSample.setVisibility(View.GONE);
        }
    }
}
