package com.koushikdutta.ion.sample

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import com.koushikdutta.scratch.Promise

class TwitterKotlin : Activity() {
    // adapter that holds tweets, obviously :)
    internal var tweetAdapter: ArrayAdapter<JsonObject>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable global Ion logging
        //        Ion.getDefault(this).setLogging("ion-sample", Log.DEBUG);

        // create a tweet adapter for our list view
        tweetAdapter = object : ArrayAdapter<JsonObject>(this, 0) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var view = convertView
                if (view == null)
                    view = getLayoutInflater().inflate(R.layout.tweet, null)

                // we're near the end of the list adapter, so load more items
                if (position >= count - 3)
                    load()

                // grab the tweet (or retweet)
                var tweet = getItem(position)
                val retweet = tweet!!.getAsJsonObject("retweeted_status")
                if (retweet != null)
                    tweet = retweet

                // grab the user info... name, profile picture, tweet text
                val user = tweet.getAsJsonObject("user")
                val twitterId = user.get("screen_name").getAsString()

                // set the profile photo using Ion
                val imageUrl = user.get("profile_image_url").getAsString()

                val imageView = view!!.findViewById<ImageView>(R.id.image)

                // Use Ion's builder set the google_image on an ImageView from a URL

                // start with the ImageView
                Ion.with(imageView)
                        // use a placeholder google_image if it needs to load from the network
                        .placeholder(R.drawable.twitter)
                        // load the url
                        .load(imageUrl.replace("_normal", ""))

                // and finally, set the name and text
                val handle = view.findViewById<TextView>(R.id.handle)
                handle.setText(twitterId)

                val text = view.findViewById<TextView>(R.id.tweet)
                text.setText(tweet.get("text").getAsString())
                return view
            }
        }

        // basic setup of the ListView and adapter
        setContentView(R.layout.twitter)
        val listView = findViewById<ListView>(R.id.list)
        listView.setAdapter(tweetAdapter)

        // authenticate and do the first load
        getCredentials()
    }

    private fun getCredentials() = Promise {
        try {
            val credentials = Ion.with(this)
                    .load("https://api.twitter.com/oauth2/token")
                    // embedding twitter api key and secret is a bad idea, but this isn't a real twitter app :)
                    .basicAuthentication("e4LrcHB55R3WamRYHpNfA", "MIABn1DU5db3Aj0xXzhthsf4aUKMAdoWJTMxJJcY")
                    .setBodyParameter("grant_type", "client_credentials")
                    .asJsonObject()
                    .await()

            accessToken = credentials.get("access_token").getAsString()
            load()
        }
        catch (e: Exception) {
            Toast.makeText(this@TwitterKotlin, "Error loading tweets", Toast.LENGTH_LONG).show()
        }
    }

    // This "Future" tracks loading operations.
    // A Future is an object that manages the state of an operation
    // in progress that will have a "Future" result.
    // You can "await" any Future to wait for the result asynchronously
    // or cancel() it if you no longer need the result.
    internal var loading: Promise<JsonArray>? = null

    internal var accessToken: String? = null

    private fun load() = Promise {
        // don't attempt to load more if a load is already in progress
        if (loading != null && !loading!!.isCompleted && !loading!!.isCancelled)
            return@Promise

        // load the tweets
        var url = "https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=dog_rates&count=20"
        if (tweetAdapter!!.getCount() > 0) {
            // load from the "last" id
            val last = tweetAdapter!!.getItem(tweetAdapter!!.getCount() - 1)
            url += "&max_id=" + last!!.get("id_str").getAsString()
        }

        // Request tweets from Twitter using Ion.
        // This is done using Ion's Fluent/Builder API.
        // This API lets you chain calls together to build
        // complex requests.

        // This request loads a URL as JsonArray
        loading = Ion.with(this)
                .load(url)
                .setHeader("Authorization", "Bearer " + accessToken)
                .asJsonArray()

        try {
            val result = loading!!.await()

            // add the tweets
            for (i in 0 until result.size()) {
                tweetAdapter!!.add(result.get(i).asJsonObject)
            }
        }
        catch (e: Exception) {
            Toast.makeText(this@TwitterKotlin, "Error loading tweets", Toast.LENGTH_LONG).show()
        }
    }
}
