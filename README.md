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
All operations return a custom [Future](http://developer.android.com/reference/java/util/concurrent/Future.html) that allows
you too specify a callback that on completion.

```java
public interface Future<T> extends Cancellable, java.util.concurrent.Future<T> {
    /**
     * Set a callback to be invoked when this Future completes.
     * @param callback
     * @return
     */
    public Future<T> setCallback(FutureCallback<T> callback);
}
```