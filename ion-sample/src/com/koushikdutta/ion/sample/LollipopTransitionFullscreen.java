package com.koushikdutta.ion.sample;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.view.Window;
import android.widget.ImageView;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.bitmap.BitmapInfo;

/**
 * Created by koush on 11/10/14.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopTransitionFullscreen extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.lollipop_fullscreen);
        final ImageView iv = (ImageView)findViewById(R.id.image);
        String bitmapKey = getIntent().getStringExtra("bitmapInfo");
        BitmapInfo bi = Ion.getDefault(this)
        .getBitmapCache()
        .get(bitmapKey);
        iv.setImageBitmap(bi.bitmap);

        final int thumb = getIntent().getIntExtra("thumb", 1);

        getWindow().getEnterTransition().addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                getWindow().getEnterTransition().removeListener(this);

                // load the full version, crossfading from the thumbnail image
                Ion.with(iv)
                .crossfade(true)
                .load("https://raw.githubusercontent.com/koush/SampleImages/master/" + thumb + ".jpg");
            }
        });
    }
}
