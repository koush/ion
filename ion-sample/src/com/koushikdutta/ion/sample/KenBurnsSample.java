package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import uk.co.senab.photoview.PhotoView;

/**
 * Created by koush on 2/1/14.
 */
public class KenBurnsSample extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KenBurnsView view = new KenBurnsView(this);
        setContentView(view);

        final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("Loading...");
        dlg.setIndeterminate(false);
        dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dlg.show();

        // this is going to load a 30mb download...
        Ion.with(this)
        .load("file:///android_asset/telescope.jpg")
        .progressDialog(dlg)
        .setLogging("DeepZoom", Log.VERBOSE)
        .withBitmap()
        .deepZoom()
        .intoImageView(view)
        .setCallback(new FutureCallback<ImageView>() {
            @Override
            public void onCompleted(Exception e, ImageView result) {
                dlg.cancel();
            }
        });
    }
}
