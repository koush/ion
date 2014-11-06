package com.koushikdutta.ion.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.List;

public class TwitterGson extends Activity {
    // Tweet and User are the classes that Gson will deserialize the JSON into
    static class Tweet {
        @SerializedName("retweeted_status")
        Tweet retweetedStatus;
        User user;
        String text;
        @SerializedName("id_str")
        String id;
    }

    static class User {
        @SerializedName("screen_name")
        String screenName;
        @SerializedName("profile_image_url")
        String imageUrl;
    }

    // adapter that holds tweets, obviously :)
    ArrayAdapter<Tweet> tweetAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable global Ion logging
        Ion.getDefault(this).configure().setLogging("ion-sample", Log.DEBUG);

        // create a tweet adapter for our list view
        tweetAdapter = new ArrayAdapter<Tweet>(this, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = getLayoutInflater().inflate(R.layout.tweet, null);

                // we're near the end of the list adapter, so load more items
                if (position >= getCount() - 3)
                    load();

                // grab the tweet (or retweet)
                Tweet tweet = getItem(position);
                Tweet retweet = tweet.retweetedStatus;
                if (retweet != null)
                    tweet = retweet;

                ImageView imageView = (ImageView)convertView.findViewById(R.id.image);

                // Use Ion's builder set the google_image on an ImageView from a URL

                // start with the ImageView
                Ion.with(imageView)
                // use a placeholder google_image if it needs to load from the network
                .placeholder(R.drawable.twitter)
                // load the url
                .load(tweet.user.imageUrl);


                // and finally, set the name and text
                TextView handle = (TextView)convertView.findViewById(R.id.handle);
                handle.setText(tweet.user.screenName);

                TextView text = (TextView)convertView.findViewById(R.id.tweet);
                text.setText(tweet.text);
                return convertView;
            }
        };

        // basic setup of the ListView and adapter
        setContentView(R.layout.twitter);
        ListView listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(tweetAdapter);

        // authenticate and do the first load
        getCredentials();
    }

    // This "Future" tracks loading operations.
    // A Future is an object that manages the state of an operation
    // in progress that will have a "Future" result.
    // You can attach callbacks (setCallback) for when the result is ready,
    // or cancel() it if you no longer need the result.
    Future<List<Tweet>> loading;

    // Lets grab the authentication
    String accessToken;
    private void getCredentials() {
        Ion.with(this)
        .load("https://api.twitter.com/oauth2/token")
        // embedding twitter api key and secret is a bad idea, but this isn't a real twitter app :)
        .basicAuthentication("e4LrcHB55R3WamRYHpNfA", "MIABn1DU5db3Aj0xXzhthsf4aUKMAdoWJTMxJJcY")
        .setBodyParameter("grant_type", "client_credentials")
        .asJsonObject()
        .setCallback(new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                if (e != null) {
                    Toast.makeText(TwitterGson.this, "Error loading tweets", Toast.LENGTH_LONG).show();
                    return;
                }
                accessToken = result.get("access_token").getAsString();
                load();
            }
        });
    }

    private void load() {
        // don't attempt to load more if a load is already in progress
        if (loading != null && !loading.isDone() && !loading.isCancelled())
            return;

        // load the tweets
        String url = "https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=BestAt&count=20";
        if (tweetAdapter.getCount() > 0) {
            // load from the "last" id
            Tweet last = tweetAdapter.getItem(tweetAdapter.getCount() - 1);
            url += "&max_id=" + last.id;
        }

        // Request tweets from Twitter using Ion.
        // This is done using Ion's Fluent/Builder API.
        // This API lets you chain calls together to build
        // complex requests.

        // This request loads a URL as JsonArray and invokes
        // a callback on completion.
        loading = Ion.with(this)
        .load(url)
        .setHeader("Authorization", "Bearer " + accessToken)
        .as(new TypeToken<List<Tweet>>() {
        })
        .setCallback(new FutureCallback<List<Tweet>>() {
            @Override
            public void onCompleted(Exception e, List<Tweet> result) {
                // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                if (e != null) {
                    Toast.makeText(TwitterGson.this, "Error loading tweets", Toast.LENGTH_LONG).show();
                    return;
                }
                // add the tweets
                for (int i = 0; i < result.size(); i++) {
                    tweetAdapter.add(result.get(i));
                }
            }
        });
    }
}
