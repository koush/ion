package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 6/9/13.
 */
public class ImageViewSample extends Activity {
    public void loadCenterCrop() {
        Ion.with(getApplicationContext()).load("http://media.salon.com/2013/05/original.jpg").noCache().withBitmap().crossfade(true).intoImageView(imageView);

//
//
//        Ion.with(this)
//        .load("http://media.salon.com/2013/05/original.jpg")
//        .withBitmap()
//        .centerCrop()
//        .intoImageView(imageView);
    }

    public void loadCenterInside() {
        Ion.with(this)
        .load("http://media.salon.com/2013/05/original.jpg")
        .withBitmap()
        .centerInside()
        .intoImageView(imageView);
    }

    public void loadFitCenter() {
        Ion.with(this)
        .load("http://media.salon.com/2013/05/original.jpg")
        .withBitmap()
        .fitCenter()
        .intoImageView(imageView);
    }

    public void loadGifCenterCrop() {
        Ion.with(this)
        .load("https://raw.githubusercontent.com/koush/ion/master/ion-sample/mark.gif")
        .withBitmap()
        .centerCrop()
        .intoImageView(imageView);
    }

    public void loadGifFitCenter() {
        Ion.with(this)
        .load("https://raw.githubusercontent.com/koush/ion/master/ion-sample/mark.gif")
        .withBitmap()
        .fitCenter()
        .intoImageView(imageView);
    }

    public void loadGifResource() {
        Ion.with(this)
        .load("android.resource://" + getPackageName() + "/" + R.drawable.borg)
        .withBitmap()
        .fitCenter()
        .intoImageView(imageView);
    }

    public void loadExifRotated() {
        Ion.with(this)
        .load("https://raw.githubusercontent.com/koush/ion/master/ion/test/assets/exif.jpg")
        .intoImageView(imageView);
    }

    public void loadTwitterResource() {
        Ion.with(this)
        .load("android.resource://" + getPackageName() + "/drawable/twitter")
        .intoImageView(imageView);
    }

    Spinner fitChoices;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view);

        imageView = (ImageView)findViewById(R.id.image);
//        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
//        imageView.setImageResource(R.drawable.charles);
//        if (true) return;
        fitChoices = (Spinner)findViewById(R.id.fit_choices);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter.add("centerCrop");
        adapter.add("fitCenter");
        adapter.add("centerInside");
        adapter.add("gif centerCrop");
        adapter.add("gif fitCenter");
        adapter.add("gif resource");
        adapter.add("exif rotated");
        adapter.add("twitter drawable resource");
        fitChoices.setAdapter(adapter);
        fitChoices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    loadCenterCrop();
                else if (position == 1)
                    loadFitCenter();
                else if (position == 2)
                    loadCenterInside();
                else if (position == 3)
                    loadGifCenterCrop();
                else if (position == 4)
                    loadGifFitCenter();
                else if (position == 5)
                    loadGifResource();
                else if (position == 6)
                    loadExifRotated();
                else if (position == 7)
                    loadTwitterResource();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
