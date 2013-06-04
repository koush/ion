package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by koush on 6/4/13.
 */
public class GoogleImageSearch extends Activity {
    private ListView mListView;
    private MyAdapter mAdapter;

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

    private class MyAdapter extends ArrayAdapter<String> {

        public MyAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.google_image, null);

            final ImageView iv = (ImageView) convertView.findViewById(R.id.image);
            Ion.with(iv)
            .animateIn(R.anim.fadein)
            .load(getItem(position));
//            Picasso.with(GoogleImageSearch.this)
//                    .load(getItem(position))
//                    .into(iv);

            return convertView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem clear = menu.add("Clear Cache");
        clear.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    final ArrayList<String> tmpResults = new ArrayList<String>();
    void loadMore() {
        Ion.with(GoogleImageSearch.this, String.format("https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=%s&start=%d&imgsz=medium", Uri.encode(searchText.getText().toString()), tmpResults.size()))
        .asJsonObject()
        .setCallback(new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                try {
                    if (e != null)
                        throw e;
                    JsonArray results = result.getAsJsonObject("responseData").getAsJsonArray("results");
                    for (int i = 0; i < results.size(); i++) {
                        tmpResults.add(results.get(i).getAsJsonObject().get("url").getAsString());
                    }
                    if (tmpResults.size() < 40)
                        loadMore();
                    else
                        mAdapter.addAll(tmpResults);
                }
                catch (Exception ex) {
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

        mListView = (ListView) findViewById(R.id.results);
        mAdapter = new MyAdapter(this);
        MyGridAdapter a = new MyGridAdapter(mAdapter);
        mListView.setAdapter(a);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.clear();
                tmpResults.clear();
                loadMore();
            }
        });

    }
}
