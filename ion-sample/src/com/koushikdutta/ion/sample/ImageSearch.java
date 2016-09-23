package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

/**
 * Created by koush on 6/4/13.
 */
public class ImageSearch extends Activity {
    private MyAdapter mAdapter;

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
                convertView = getLayoutInflater().inflate(R.layout.image, null);

            // find the image view
            final ImageView iv = (ImageView) convertView.findViewById(R.id.image);

            // select the image view
            Ion.with(iv)
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

        String url = getApiUrl(searchText.getText().toString(), mAdapter.getCount());

        // query googles image search api
        loading = Ion.with(ImageSearch.this)
        .load(url)
        // get the results as json
        .asJsonObject()
        .setCallback(new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                try {
                    if (e != null)
                        throw e;
                    processApiResult(result);
                }
                catch (Exception ex) {
                    // toast any error we encounter (most image search APIs have a throttling limit that sometimes gets hit)
                    Toast.makeText(ImageSearch.this, ex.toString(), Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    /**
     * Build the url for the next request. Uses Flickr's API
     *
     * https://www.flickr.com/services/feeds/docs/photos_public/
     *
     * @param text Search text
     * @param page Current page we're in
     * @return Url for the next request
     */
    String getApiUrl(String text, int page) {
        String base = "https://api.flickr.com/services/feeds/photos_public.gne?format=json&nojsoncallback=?";
        if (text != null && !text.isEmpty()) {
            base += "&tags=" + text;
        }

        return base;
    }

    /**
     * Process a successfull API result
     * @param result the API's response
     */
    void processApiResult(JsonObject result) {
        processFlickrApiResult(result);
    }

    void processFlickrApiResult(JsonObject result) {
        // find the results and populate
        JsonArray results = result.getAsJsonArray("items");
        for (int i = 0; i < results.size(); i++) {
            mAdapter.add(results.get(i).getAsJsonObject().getAsJsonObject("media").get("m").getAsString());
        }
    }

    EditText searchText;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG);

        setContentView(R.layout.image_search);

        final Button search = (Button) findViewById(R.id.search);
        searchText = (EditText) findViewById(R.id.search_text);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        int cols = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().densityDpi;
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
