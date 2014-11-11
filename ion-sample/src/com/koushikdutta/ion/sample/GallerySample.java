package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.koushikdutta.ion.Ion;

import java.io.File;

/**
 * Created by koush on 9/4/13.
 */
public class GallerySample extends Activity {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG);

        setContentView(R.layout.gallery);

        int cols = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().densityDpi * 2;
        GridView view = (GridView) findViewById(R.id.results);
        view.setNumColumns(cols);
        mAdapter = new MyAdapter(this);
        view.setAdapter(mAdapter);

        loadMore();
    }

    Cursor mediaCursor;
    public void loadMore() {
        if (mediaCursor == null) {
            mediaCursor = getContentResolver().query(MediaStore.Files.getContentUri("external"), null, null, null, null);
        }

        int loaded = 0;
        while (mediaCursor.moveToNext() && loaded < 10) {
            // get the media type. ion can show images for both regular images AND video.
            int mediaType = mediaCursor.getInt(mediaCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
            if (mediaType != MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                && mediaType != MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                continue;
            }

            loaded++;

            String uri = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
            File file = new File(uri);
            // turn this into a file uri if necessary/possible
            if (file.exists())
                mAdapter.add(file.toURI().toString());
            else
                mAdapter.add(uri);
        }
    }
}
