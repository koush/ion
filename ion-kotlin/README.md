# Ion Kotlin Extensions - async/await

async/await allows you to write code that looks like synchronous code, but is actually run asynchronously via coroutines.

For example, if you wanted to download a list of files with async/await in Ion:

```kotlin
fun getFiles(files: Array<String>) = async {
    for (file in files) {
        Ion.with(context)
        .load(file)
        .asString()
        .await()
    }
}
```
This may look like synchronous code, but it is not. The return type of getFiles is a actually [Future](https://github.com/koush/ion#futures). The operation happens asynchronously, and only when all the files are finished downloading, will the Future's callback be called.

```kotlin
async {
    // the code in here is a suspend fun, a coroutine.
    // https://kotlinlang.org/docs/reference/coroutines.html
    // execution can be suspended and resumed.
}
```

### Java
```java
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
                Toast.makeText(Twitter.this, "Error loading tweets", Toast.LENGTH_LONG).show();
                return;
            }
            String accessToken = result.get("access_token").getAsString();
            load(accessToken);
        }
    });
}

private void load(String accessToken) {
    // load the tweets
    String url = "https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=dog_rates&count=20";

    Ion.with(this)
    .load(url)
    .setHeader("Authorization", "Bearer " + accessToken)
    .asJsonArray()
    .setCallback(new FutureCallback<JsonArray>() {
        @Override
        public void onCompleted(Exception e, JsonArray result) {
            // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
            if (e != null) {
                Toast.makeText(Twitter.this, "Error loading tweets", Toast.LENGTH_LONG).show();
                return;
            }
            // add the tweets
            for (int i = 0; i < result.size(); i++) {
                tweetAdapter.add(result.get(i).getAsJsonObject());
            }
        }
    });
}

getCredentials();
```

### Kotlin
```kotlin
fun getTweets() = async {
    try {
        val credentials = Ion.with(this)
        .load("https://api.twitter.com/oauth2/token")
        // embedding twitter api key and secret is a bad idea, but this isn't a real twitter app :)
        .basicAuthentication("e4LrcHB55R3WamRYHpNfA", "MIABn1DU5db3Aj0xXzhthsf4aUKMAdoWJTMxJJcY")
        .setBodyParameter("grant_type", "client_credentials")
        .asJsonObject()
        .await()

        val accessToken = credentials.get("access_token").getAsString()

        // load the tweets
        var url = "https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=dog_rates&count=20"

        // Request tweets from Twitter using Ion.
        // This is done using Ion's Fluent/Builder API.
        // This API lets you chain calls together to build
        // complex requests.

        // This request loads a URL as JsonArray
        val tweets = Ion.with(this)
        .load(url)
        .setHeader("Authorization", "Bearer " + accessToken)
        .asJsonArray()
        .await()

        // add the tweets
        for (i in 0..tweets.size() - 1) {
            tweetAdapter!!.add(tweets.get(i).getAsJsonObject())
        }
    }
    catch (e: Exception) {
        Toast.makeText(this@TwitterKotlin, "Error loading tweets", Toast.LENGTH_LONG).show()
    }
}
```
