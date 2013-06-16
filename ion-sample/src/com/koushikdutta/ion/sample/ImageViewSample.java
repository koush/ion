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
        Ion.with(this)
        .load("http://media.salon.com/2013/05/original.jpg")
        .withBitmap()
        .resize(512, 512)
        .centerCrop()
        .intoImageView(imageView);
    }

    public void loadCenterInside() {
        Ion.with(this)
        .load("http://media.salon.com/2013/05/original.jpg")
        .withBitmap()
        .resize(512, 512)
        .centerInside()
        .intoImageView(imageView);
    }

    Spinner fitChoices;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view);

        imageView = (ImageView)findViewById(R.id.image);
        fitChoices = (Spinner)findViewById(R.id.fit_choices);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter.add("centerCrop");
        adapter.add("centerInside");
        fitChoices.setAdapter(adapter);
        fitChoices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    loadCenterCrop();
                else
                    loadCenterInside();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadCenterCrop();
    }
}
