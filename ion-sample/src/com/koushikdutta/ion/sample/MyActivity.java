package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONObject;

public class MyActivity extends Activity {
    ArrayAdapter<JSONObject> tweetAdapter;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tweetAdapter = new ArrayAdapter<JSONObject>(this, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = getLayoutInflater().inflate(R.layout.tweet, null);


                JSONObject tweet = getItem(position);
                String twitterId = tweet.optString("screen_name", null);
                String imageUrl = String.format("https://api.twitter.com/1/users/profile_image?screen_name=%s&size=bigger", twitterId);
                NetworkImageView niv = (NetworkImageView)convertView.findViewById(R.id.image);
                niv.setImageUrl(imageUrl);
                return convertView;
            }
        };

        setContentView(R.layout.main);

        Ion.with(this).load("https://api.twitter.com/1/statuses/user_timeline.json?include_entities=true&include_rts=true&screen_name=koush&count=20")
                .asJSONArray()
                .setCallback(new FutureCallback<JSONArray>() {
                    @Override
                    public void onCompleted(Exception e, JSONArray result) {
                        for (int i = 0; i < result.length(); i++) {
                            tweetAdapter.add(result.optJSONObject(i));
                        }
                    }
                });
    }
}
