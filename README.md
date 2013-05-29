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
Ion.with(context).load("http://example.com/post")
.setJSONObjectBody(new JSONObject())
.asJSONObject(new FutureCallback<JSONObject>() {
   @Override
    public void onCompleted(Exception e, String result) {
        // do stuff with the result or error
    }
});
```

#### Save to a File

```java
Save files
Ion.with(context).load("http://example.com/cm-11-m7.zip")
.write(new File("/sdcard/cm-11.zip");
```

#### Load an image into an ImageView

```java
Ion.with(context).load("http://example.com/image.png")
.withBitmap()
.placeholder(R.id.placeholder)
.intoImageView(imageView);
```

#### Futures
_All__ operations return a custom [Future](http://developer.android.com/reference/java/util/concurrent/Future.html) that allows
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

Future<String> string = Ion.with(context).load("http://example.com/string.txt").asString();
Future<JSONObject> json = Ion.with(context).load("http://example.com/json.json").asJSONObject();
Future<File> file = Ion.with(context).load("http://example.com/file.zip").write(new File("/sdcard/file.zip"));
Future<Bitmap> bitmap = Ion.with(context).load("http://example.com/image.png").intoImageView(imageView);

```