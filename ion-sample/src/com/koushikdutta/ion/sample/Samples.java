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

    Button twitter;
    Button twitterGson;
    Button fileDownload;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.samples);

        twitter = (Button)findViewById(R.id.twitter);
        twitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, Twitter.class));
            }
        });

        twitterGson = (Button)findViewById(R.id.twitter_gson);
        twitterGson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, TwitterGson.class));
            }
        });

        fileDownload = (Button)findViewById(R.id.download);
        fileDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Samples.this, ProgressBarDownload.class));
            }
        });
    }
}
