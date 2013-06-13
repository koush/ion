package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.UUID;

/**
 * Created by koush on 6/4/13.
 */
public class GoogleImageSearch extends Activity {
    private MyAdapter mAdapter;

    final static float GOLDEN_RATIO = 1.6180339887498948482f;

    // Adapter to populate and imageview from an url contained in the array adapter
    private class MyAdapter extends ArrayAdapter<String> {
        public MyAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // see if we need to load more to get 40, otherwise populate the adapter
            if (position > getCount() - 4)
                loadMore();

            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.google_image, null);

            // find the image view
            final ImageView iv = (ImageView) convertView.findViewById(R.id.image);

            // select the image view
            Ion.with(iv)
            .resize(256, 256)
            .centerCrop()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .load(getItem(position));

            return convertView;
        }
    }

    Future<JsonObject> loading;
    void loadMore() {
        if (loading != null && !loading.isDone() && !loading.isCancelled())
            return;

        // query googles image search api
        loading = Ion.with(GoogleImageSearch.this, String.format("https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=%s&start=%d&imgsz=medium", Uri.encode(searchText.getText().toString()), mAdapter.getCount()))
        // get the results as json
        .asJsonObject()
        .setCallback(new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                try {
                    if (e != null)
                        throw e;
                    // find the results and populate
                    JsonArray results = result.getAsJsonObject("responseData").getAsJsonArray("results");
                    for (int i = 0; i < results.size(); i++) {
                        mAdapter.add(results.get(i).getAsJsonObject().get("url").getAsString());
                    }
                }
                catch (Exception ex) {
                    // toast any error we encounter (google image search has an API throttling limit that sometimes gets hit)
//                    Toast.makeText(GoogleImageSearch.this, ex.toString(), Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    EditText searchText;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
//        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//        .detectAll()
//        .penaltyLog()
//        .build());
        super.onCreate(savedInstanceState);

        Ion.getDefault(this).setLogging("ion-sample", Log.DEBUG);

        setContentView(R.layout.google_image_search);

        final Button search = (Button) findViewById(R.id.search);
        searchText = (EditText) findViewById(R.id.search_text);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        int cols = getResources().getDisplayMetrics().widthPixels / 512;
        GridView view = (GridView) findViewById(R.id.results);
        view.setNumColumns(cols);
        mAdapter = new MyAdapter(this);
        view.setAdapter(mAdapter);

        search();
    }

    private void search() {
        mAdapter.clear();
        loadMore();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
    }
}
