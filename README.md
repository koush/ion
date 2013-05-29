*The missing Networking Library for Android*

### Features
 * Download files, JSON, strings, bitmaps, and more
 * Post text/plain, application/json, application/x-www-form-urlencoded, multipart/form-data
 * Easy to use Fluent API
 * Caching (transparent)
 * Compression (transparent)
 * Connection reuse (transparent)
 * Cookies (transparent)
 * All operations return a [Future](http://developer.android.com/reference/java/util/concurrent/Future.html) and can be cancelled
 * Automatically cancels operations when the calling Activity finishes
 * Manages invocation back onto the UI thread
 * ImageView loading, caching, and memory management (including ListView and convertView recycling)
 * Supports file:/, http:/, and content:/ URIs
 * Based on [NIO](http://en.wikipedia.org/wiki/New_I/O) and [AndroidAsync](https://github.com/koush/AndroidAsync)

### Examples

#### Get JSON

```java
Ion.with(context).load("http://example.com/thing.json")
.asJSONObject()
.setCallback(new FutureCallback<JSONObject>() {
   @Override
    public void onCompleted(Exception e, String result) {
        // do stuff with the result or error
    }
});
```

#### Post JSON and read JSON

```java
JSONObject json = new JSONObject();
json.putString("foo", "bar");

Ion.with(context).load("http://example.com/post")
.setJSONObjectBody(json)
.asJSONObject()
.setCallback(new FutureCallback<JSONObject>() {
   @Override
    public void onCompleted(Exception e, String result) {
        // do stuff with the result or error
    }
});
```

#### Download a File

```java
Ion.with(context).load("http://example.co/cm-11-m7.zip")
.write(new File("/sdcard/cm-11.zip")
.setCallback(new FutureCallback<File>() {
   @Override
    public void onCompleted(Exception e, String File) {
        // download done...
        // do stuff with the File or error
    }
});
```

#### Load an image into an ImageView

```java
Ion.with(context).load("http://example.com/image.png")
.withBitmap()
.placeholder(R.drawable.placeholder_image
.error(R.drawable.error_image)
.animateLoad(spinAnimation)
.animateIn(fadeInAnimation)
.intoImageView(imageView);
```

#### Setting Headers

```java
Ion.with(context).load("http://example.com/test.txt")
// set the header
.setHeader("foo", "bar")
.asString()
.setCallback(...)
```

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

Future<JSONObject> json = Ion.with(context)
    .load("http://example.com/json.json")
    .asJSONObject();

Future<File> file = Ion.with(context)
    .load("http://example.com/file.zip")
    .write(new File("/sdcard/file.zip"));

Future<Bitmap> bitmap = Ion.with(context)
    .load("http://example.com/image.png")
    .intoImageView(imageView);

```