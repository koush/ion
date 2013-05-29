#### Get JSON

```java
Ion.with(context).load('http://example.com/thing.json")
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
Images too
Ion.with(context).load('http://example.com/image.png")
.withBitmap()
.placeholder(R.id.placeholder)
.intoImageView(imageView);
```