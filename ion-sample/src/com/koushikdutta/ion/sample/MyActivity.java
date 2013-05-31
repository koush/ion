package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONObject;

public class MyActivity extends Activity {
    ArrayAdapter<JSONObject> tweetAdapter;

    // this tracks loading operations
    Future<JSONArray> loading;

    private void load() {
        // don't attempt to load if a load is already in progress
        if (loading != null && !loading.isDone() && !loading.isCancelled())
            return;

        // load the tweets
        String url = "https://api.twitter.com/1/statuses/user_timeline.json?include_entities=true&include_rts=true&screen_name=koush&count=20";
        if (tweetAdapter.getCount() > 0) {
            // load from the "last" id
            JSONObject last = tweetAdapter.getItem(tweetAdapter.getCount() - 1);
            url += "&max_id=" + last.optString("id_str");
        }

        // off we go!
        loading = Ion.with(this).load(url)
                .asJSONArray()
                .setCallback(new FutureCallback<JSONArray>() {
                    @Override
                    public void onCompleted(Exception e, JSONArray result) {
                        // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                        if (e != null) {
                            Toast.makeText(MyActivity.this, "Error loading tweets", Toast.LENGTH_LONG).show();
                            return;
                        }
                        // add the tweets
                        for (int i = 0; i < result.length(); i++) {
                            tweetAdapter.add(result.optJSONObject(i));
                        }
                    }
                });
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable global Ion logging
        Ion.getDefault(this).setLogging("ion-sample", Log.DEBUG);

        // create a tweet adapter
        tweetAdapter = new ArrayAdapter<JSONObject>(this, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = getLayoutInflater().inflate(R.layout.tweet, null);

                // we're near the end, so load more
                if (position >= getCount() - 3)
                    load();

                // grab the tweet (or retweet)
                JSONObject tweet = getItem(position);
                JSONObject retweet = tweet.optJSONObject("retweeted_status");
                if (retweet != null)
                    tweet = retweet;

                // grab the user info... name, profile picture, tweet text
                JSONObject user = tweet.optJSONObject("user");
                String twitterId = user.optString("screen_name", null);

                // set the profile photo using NetworkImageView (courtesy of Volley, repurposed for Ion)
                String imageUrl = String.format("https://api.twitter.com/1/users/profile_image?screen_name=%s&size=bigger", twitterId);
                NetworkImageView niv = (NetworkImageView)convertView.findViewById(R.id.image);
                niv.setDefaultImageResId(R.drawable.twitter);
                niv.setImageUrl(imageUrl);

                // set the name and text
                TextView handle = (TextView)convertView.findViewById(R.id.handle);
                handle.setText(twitterId);

                TextView text = (TextView)convertView.findViewById(R.id.tweet);
                text.setText(tweet.optString("text", null));
                return convertView;
            }
        };

        setContentView(R.layout.main);

        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(tweetAdapter);

        // do the first load
        load();
    }
}
