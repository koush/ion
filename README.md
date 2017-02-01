*Android Asynchronous Networking and Image Loading*

![](ion-sample/ion-sample.png)

#### Download
 * [Maven](https://github.com/koush/ion#get-ion)
 * [Git](https://github.com/koush/ion#get-ion)

#### Features
 * Asynchronously download:
   * [Images](https://github.com/koush/ion#load-an-image-into-an-imageview) into ImageViews or Bitmaps (animated GIFs supported too)
   * [JSON](https://github.com/koush/ion#get-json) (via [Gson](https://code.google.com/p/google-gson/))
   * Strings
   * [Files](https://github.com/koush/ion#download-a-file-with-a-progress-bar)
   * Java types using [Gson](https://github.com/koush/ion#seamlessly-use-your-own-java-classes-with-gson)
 * Easy to use Fluent API designed for Android
   * Automatically cancels operations when the calling Activity finishes
   * Manages invocation back onto the UI thread
   * All operations return a [Future](https://github.com/koush/ion#futures) and [can be cancelled](https://github.com/koush/ion#cancelling-requests)
 * HTTP POST/PUT:
   * text/plain
   * application/json - both [JsonObject](https://github.com/koush/ion#post-json-and-read-json) and [POJO](https://github.com/koush/ion#seamlessly-use-your-own-java-classes-with-gson)
   * [application/x-www-form-urlencoded](https://github.com/koush/ion#post-applicationx-www-form-urlencoded-and-read-a-string)
   * [multipart/form-data](https://github.com/koush/ion#post-multipartform-data-and-read-json-with-an-upload-progress-bar)
 * Transparent usage of HTTP features and optimizations:
   * SPDY and HTTP/2
   * Caching
   * Gzip/Deflate Compression
   * Connection pooling/reuse via HTTP Connection: keep-alive
   * Uses the best/stablest connection from a server if it has multiple IP addresses
   * Cookies
 * [View received headers](https://github.com/koush/ion#viewing-received-headers)
 * [Grouping and cancellation of requests](https://github.com/koush/ion#request-groups)
 * [Download progress callbacks](https://github.com/koush/ion#download-a-file-with-a-progress-bar)
 * Supports file:/, http(s):/, and content:/ URIs
 * Request level [logging and profiling](https://github.com/koush/ion#logging)
 * [Support for proxy servers](https://github.com/koush/ion#proxy-servers-like-charles-proxy) like [Charles Proxy](http://www.charlesproxy.com/) to do request analysis
 * Based on [NIO](http://en.wikipedia.org/wiki/New_I/O) and [AndroidAsync](https://github.com/koush/AndroidAsync)
 * Ability to use [self signed SSL certificates](https://github.com/koush/ion/issues/3)

#### Samples

The included documented [ion-sample](https://github.com/koush/ion/tree/master/ion-sample) project includes some samples that demo common Android network operations:

 * [Twitter Client Sample](https://github.com/koush/ion/blob/master/ion-sample/src/com/koushikdutta/ion/sample/Twitter.java)
   * Download JSON from a server (twitter feed)
   * Populate a ListView Adapter and fetch more data as you scroll to the end
   * Put images from a URLs into ImageViews (twitter profile pictures)
 * File Download with [Progress Bar Sample](https://github.com/koush/ion/blob/master/ion-sample/src/com/koushikdutta/ion/sample/ProgressBarDownload.java)
 * Get JSON and show images with the [Image Search Sample](https://github.com/koush/ion/blob/master/ion-sample/src/com/koushikdutta/ion/sample/ImageSearch.java)

#### More Examples

Looking for more? Check out the examples below that demonstrate some other common scenarios. You can also take a look
at 30+ ion unit tests in the [ion-test](https://github.com/koush/ion/tree/master/ion/test/src/com/koushikdutta/ion/test).

#### Get JSON

```java
Ion.with(context)
.load("http://example.com/thing.json")
.asJsonObject()
.setCallback(new FutureCallback<JsonObject>() {
   @Override
    public void onCompleted(Exception e, JsonObject result) {
        // do stuff with the result or error
    }
});
```

#### Post JSON and read JSON

```java
JsonObject json = new JsonObject();
json.addProperty("foo", "bar");

Ion.with(context)
.load("http://example.com/post")
.setJsonObjectBody(json)
.asJsonObject()
.setCallback(new FutureCallback<JsonObject>() {
   @Override
    public void onCompleted(Exception e, JsonObject result) {
        // do stuff with the result or error
    }
});
```

#### Post application/x-www-form-urlencoded and read a String

```java
Ion.with(getContext())
.load("https://koush.clockworkmod.com/test/echo")
.setBodyParameter("goop", "noop")
.setBodyParameter("foo", "bar")
.asString()
.setCallback(...)
```

#### Post multipart/form-data and read JSON with an upload progress bar

```java
Ion.with(getContext())
.load("https://koush.clockworkmod.com/test/echo")
.uploadProgressBar(uploadProgressBar)
.setMultipartParameter("goop", "noop")
.setMultipartFile("archive", "application/zip", new File("/sdcard/filename.zip"))
.asJsonObject()
.setCallback(...)
```

#### Download a File with a progress bar

```java
Ion.with(context)
.load("http://example.com/really-big-file.zip")
// have a ProgressBar get updated automatically with the percent
.progressBar(progressBar)
// and a ProgressDialog
.progressDialog(progressDialog)
// can also use a custom callback
.progress(new ProgressCallback() {@Override
   public void onProgress(long downloaded, long total) {
       System.out.println("" + downloaded + " / " + total);
   }
})
.write(new File("/sdcard/really-big-file.zip"))
.setCallback(new FutureCallback<File>() {
   @Override
    public void onCompleted(Exception e, File file) {
        // download done...
        // do stuff with the File or error
    }
});
```

#### Setting Headers

```java
Ion.with(context)
.load("http://example.com/test.txt")
// set the header
.setHeader("foo", "bar")
.asString()
.setCallback(...)
```

#### Load an image into an ImageView

```java
// This is the "long" way to do build an ImageView request... it allows you to set headers, etc.
Ion.with(context)
.load("http://example.com/image.png")
.withBitmap()
.placeholder(R.drawable.placeholder_image)
.error(R.drawable.error_image)
.animateLoad(spinAnimation)
.animateIn(fadeInAnimation)
.intoImageView(imageView);

// but for brevity, use the ImageView specific builder...
Ion.with(imageView)
.placeholder(R.drawable.placeholder_image)
.error(R.drawable.error_image)
.animateLoad(spinAnimation)
.animateIn(fadeInAnimation)
.load("http://example.com/image.png");
```

The Ion Image load API has the following features:
 * Disk and memory caching
 * Bitmaps are held via weak references so memory is managed very efficiently
 * ListView Adapter recycling support
 * Bitmap transformations via the .transform(Transform)
 * Animate loading and loaded ImageView states
 * [DeepZoom](http://www.youtube.com/watch?v=yIMltNEAKZY) for extremely large images

#### Futures

_All_ operations return a custom [Future](http://developer.android.com/reference/java/util/concurrent/Future.html) that allows
you to specify a callback that runs on completion.

```java
public interface Future<T> extends Cancellable, java.util.concurrent.Future<T> {
    /**
     * Set a callback to be invoked when this Future completes.
     * @param callback
     * @return
     */
    public Future<T> setCallback(FutureCallback<T> callback);
}

Future<String> string = Ion.with(context)
.load("http://example.com/string.txt")
.asString();

Future<JsonObject> json = Ion.with(context)
.load("http://example.com/json.json")
.asJsonObject();

Future<File> file = Ion.with(context)
.load("http://example.com/file.zip")
.write(new File("/sdcard/file.zip"));

Future<Bitmap> bitmap = Ion.with(context)
.load("http://example.com/image.png")
.intoImageView(imageView);
```

#### Cancelling Requests

Futures can be cancelled by calling .cancel():

```java
bitmap.cancel();
json.cancel();
```

#### Blocking on Requests

Though you should try to use callbacks for handling requests whenever possible, blocking on requests is possible too.
All Futures have a Future<T>.get() method that waits for the result of the request, by blocking if necessary.

```java
JsonObject json = Ion.with(context)
.load("http://example.com/thing.json").asJsonObject().get();
```

#### Seamlessly use your own Java classes with [Gson](https://code.google.com/p/google-gson/)

```java
public static class Tweet {
    public String id;
    public String text;
    public String photo;
}

public void getTweets() throws Exception {
    Ion.with(context)
    .load("http://example.com/api/tweets")
    .as(new TypeToken<List<Tweet>>(){})
    .setCallback(new FutureCallback<List<Tweet>>() {
       @Override
        public void onCompleted(Exception e, List<Tweet> tweets) {
          // chirp chirp
        }
    });
}
```

#### Logging

Wondering why your app is slow? Ion lets you do both global and request level logging.

To enable it globally:

```java
Ion.getDefault(getContext()).configure().setLogging("MyLogs", Log.DEBUG);
```

Or to enable it on just a single request:

```java
Ion.with(context)
.load("http://example.com/thing.json")
.setLogging("MyLogs", Log.DEBUG)
.asJsonObject();
```

Log entries will look like this:

```
D/MyLogs(23153): (0 ms) http://example.com/thing.json: Executing request.
D/MyLogs(23153): (106 ms) http://example.com/thing.json: Connecting socket
D/MyLogs(23153): (2985 ms) http://example.com/thing.json: Response is not cacheable
D/MyLogs(23153): (3003 ms) http://example.com/thing.json: Connection successful
```

#### Request Groups

By default, Ion automatically places all requests into a group with all the other requests
created by that Activity or Service. Using the cancelAll(Activity) call, all requests
still pending can be easily cancelled:

```java
Future<JsonObject> json1 = Ion.with(activity, "http://example.com/test.json").asJsonObject();
Future<JsonObject> json2 = Ion.with(activity, "http://example.com/test2.json").asJsonObject();

// later... in activity.onStop
@Override
protected void onStop() {
    Ion.getDefault(activity).cancelAll(activity);
    super.onStop();
}
```

Ion also lets you tag your requests into groups to allow for easy cancellation of requests in that group later:

```java
Object jsonGroup = new Object();
Object imageGroup = new Object();

Future<JsonObject> json1 = Ion.with(activity)
.load("http://example.com/test.json")
// tag in a custom group
.group(jsonGroup)
.asJsonObject();

Future<JsonObject> json2 = Ion.with(activity)
.load("http://example.com/test2.json")
// use the same custom group as the other json request
.group(jsonGroup)
.asJsonObject();

Future<Bitmap> image1 = Ion.with(activity)
.load("http://example.com/test.png")
// for this image request, use a different group for images
.group(imageGroup)
.intoImageView(imageView1);

Future<Bitmap> image2 = Ion.with(activity)
.load("http://example.com/test2.png")
// same imageGroup as before
.group(imageGroup)
.intoImageView(imageView2);

// later... to cancel only image downloads:
Ion.getDefault(activity).cancelAll(imageGroup);
```

#### Proxy Servers (like Charles Proxy)

Proxy server settings can be enabled all Ion requests, or on a per request basis:

```java
// proxy all requests
Ion.getDefault(context).configure().proxy("mycomputer", 8888);

// or... to proxy specific requests
Ion.with(context)
.load("http://example.com/proxied.html")
.proxy("mycomputer", 8888)
.getString();
```

Using Charles Proxy on your desktop computer in conjunction with request proxying will prove invaluable for debugging!

![](ion-sample/charles.png)

#### Viewing Received Headers

Ion operations return a [ResponseFuture](https://github.com/koush/ion/blob/master/ion/src/com/koushikdutta/ion/future/ResponseFuture.java),
which grant access to response properties via the [Response object](https://github.com/koush/ion/blob/master/ion/src/com/koushikdutta/ion/Response.java).
The Response object contains the headers, as well as the result:

```java
Ion.with(getContext())
.load("http://example.com/test.txt")
.asString()
.withResponse()
.setCallback(new FutureCallback<Response<String>>() {
    @Override
    public void onCompleted(Exception e, Response<String> result) {
        // print the response code, ie, 200
        System.out.println(result.getHeaders().code());
        // print the String that was downloaded
        System.out.println(result.getResult());
    }
});
```


#### Get Ion

##### Maven
```xml
<dependency>
   <groupId>com.koushikdutta.ion</groupId>
   <artifactId>ion</artifactId>
   <version>2,</version>
</dependency>
```

##### Gradle
```groovy
dependencies {
    compile 'com.koushikdutta.ion:ion:2.+'
}
````

##### Local Checkout (with [AndroidAsync](https://github.com/koush/AndroidAsync) dependency)
```
git clone git://github.com/koush/AndroidAsync.git
git clone git://github.com/koush/ion.git
cd ion/ion
ant -Dsdk.dir=$ANDROID_HOME release install
```
Jars are at
 * ion/ion/bin/classes.jar
 * AndroidAsync/AndroidAsync/bin/classes.jar

#### Hack in Eclipse
```
git clone git://github.com/koush/AndroidAsync.git
git clone git://github.com/koush/ion.git
```
* Import the project from AndroidAsync/AndroidAsync into your workspace
* Import all the ion projects (ion/ion, ion/ion-sample) into your workspace.

#### Projects using ion

There's hundreds of apps using ion. Feel free to contact me or submit a pull request to add yours to this list.

* [AllCast](https://play.google.com/store/apps/details?id=com.koushikdutta.cast)
* [Helium](https://play.google.com/store/apps/details?id=com.koushikdutta.backup)
* [Repost](https://play.google.com/store/apps/details?id=com.dodgingpixels.repost)
* [Cloupload](https://play.google.com/store/apps/details?id=de.gidix.cloupload)
* [Binge](https://play.google.com/store/apps/details?id=com.stfleurs.binge)
* [PictureCast](https://play.google.com/store/apps/details?id=com.unstableapps.picturecast.app)
* [Eventius](https://play.google.com/store/apps/details?id=com.eventius.android)
* [Plume](https://play.google.com/store/apps/details?id=com.levelup.touiteur)
* [GameRaven](https://play.google.com/store/apps/details?id=com.ioabsoftware.gameraven)
* [See You There](https://play.google.com/store/apps/details?id=com.maps.wearat&hl=en)
* [Doogles](https://play.google.com/store/apps/details?id=io.dooglesapp)
