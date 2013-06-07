package com.koushikdutta.ion.sample;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 6/4/13.
 */
public class GoogleImageSearch extends Activity {
    private ListView mListView;
    private MyAdapter mAdapter;

    // boilerplate to hold a grid of images (GridView does not work properly)
    // Create an adapter wrapper around another adapter
    // that creates a row of items and returns that to the ListView
    // as a single item
    private class Row extends ArrayList {
    }

    private class MyGridAdapter extends BaseAdapter {
        public MyGridAdapter(Adapter adapter) {
            mAdapter = adapter;
            mAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    notifyDataSetChanged();
                }

                @Override
                public void onInvalidated() {
                    super.onInvalidated();
                    notifyDataSetInvalidated();
                }
            });
        }

        Adapter mAdapter;

        final int rowSize = 4;

        @Override
        public int getCount() {
            return (int) Math.ceil((double) mAdapter.getCount() / (double)rowSize);
        }

        @Override
        public Row getItem(int position) {
            Row row = new Row();
            for (int i = position * rowSize; i < rowSize; i++) {
                if (mAdapter.getCount() < i)
                    row.add(mAdapter.getItem(i));
                else
                    row.add(null);
            }
            return row;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getLayoutInflater().inflate(R.layout.google_image_row, null);
            LinearLayout row = (LinearLayout) convertView;
            LinearLayout l = (LinearLayout) row.getChildAt(0);
            for (int child = 0; child < rowSize; child++) {
                int i = position * rowSize + child;
                LinearLayout c = (LinearLayout) l.getChildAt(child);
                c.removeAllViews();
                if (i < mAdapter.getCount()) {
                    c.addView(mAdapter.getView(i, null, null));
                }
            }

            return convertView;
        }
    }

    // Adapter to populate and imageview from an url contained in the array adapter
    private class MyAdapter extends ArrayAdapter<String> {
        public MyAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.google_image, null);

            // find the image view
            final ImageView iv = (ImageView) convertView.findViewById(R.id.image);

            // select the image view
            Ion.with(iv)
            // fade in on load
            .animateIn(R.anim.fadein)
            // load the url
            .load(getItem(position));

            return convertView;
        }
    }

    // query 40 results and show them
    void loadMore() {
        // query googles image search api
        Ion.with(GoogleImageSearch.this, String.format("https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=%s&start=%d&imgsz=medium", Uri.encode(searchText.getText().toString()), mAdapter.getCount()))
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

                    // see if we need to load more to get 40, otherwise populate the adapter
                    if (mAdapter.getCount() < 40)
                        loadMore();
                }
                catch (Exception ex) {
                    // toast any error we encounter (google image search has an API throttling limit that sometimes gets hit)
                    Toast.makeText(GoogleImageSearch.this, ex.toString(), Toast.LENGTH_LONG).show();
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
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build());
        super.onCreate(savedInstanceState);

        Ion.getDefault(this).setLogging("ion-sample", Log.DEBUG);

        setContentView(R.layout.google_image_search);

        final Button search = (Button) findViewById(R.id.search);
        searchText = (EditText) findViewById(R.id.search_text);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.clear();
                loadMore();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
            }
        });


        mListView = (ListView) findViewById(R.id.results);
        mAdapter = new MyAdapter(this);
        MyGridAdapter a = new MyGridAdapter(mAdapter);
        mListView.setAdapter(a);
    }
}
