package com.koushikdutta.ion.sample;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.bitmap.BitmapInfo;

/**
 * Created by koush on 11/10/14.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopTransition extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.lollipop_list);

        ImageView[] ivs = new ImageView[] {
        (ImageView)findViewById(R.id.one),
        (ImageView)findViewById(R.id.two),
        (ImageView)findViewById(R.id.three),
        (ImageView)findViewById(R.id.four),
        };

        for (int i = 1; i <= ivs.length; i++) {
            final int thumb = i;
            ImageView iv = ivs[i - 1];
            Ion.with(iv)
            .centerCrop()
            .load("https://raw.githubusercontent.com/koush/SampleImages/master/" + i + ".thumb.jpg");

            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageView iv = (ImageView)v;

                    BitmapInfo bi = Ion.with(iv)
                    .getBitmapInfo();

                    Intent intent = new Intent(LollipopTransition.this, LollipopTransitionFullscreen.class);
                    intent.putExtra("bitmapInfo", bi.key);
                    intent.putExtra("thumb", thumb);
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(LollipopTransition.this, iv, "photo_hero").toBundle());
                }
            });
        }
    }
}
