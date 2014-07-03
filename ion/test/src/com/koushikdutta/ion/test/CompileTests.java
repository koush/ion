package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 6/16/13.
 */
public class CompileTests extends AndroidTestCase {
    // should never actually run
    void check() {
        if (true)
            return;

        Ion.with(getContext())
                .load("")
                .withBitmap()
                .resize(0, 0)
                .animateLoad(null)
                .intoImageView(null);

        Ion.with(getContext())
                .load("")
                .withBitmap()
                .resize(0, 0)
                .centerCrop()
                .resize(0, 0)
                .placeholder(null)
                .intoImageView(null);

        // shouldnt compile

//        Ion.with(getContext())
//                .load("")
//                .withBitmap()
//                .resize(0, 0)
//                .placeholder(null)
//                .asBitmap();
    }
}
