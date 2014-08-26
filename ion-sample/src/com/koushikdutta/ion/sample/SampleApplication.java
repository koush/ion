package com.koushikdutta.ion.sample;

import android.app.Application;
import android.os.StrictMode;

/**
 * Created by koush on 8/26/14.
 */
public class SampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyDeath()
        .build());
    }
}
