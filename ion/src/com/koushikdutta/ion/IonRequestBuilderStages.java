package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.ImageView;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.bitmap.Transform;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;

/**
 * Created by koush on 5/21/13.
 */
public class IonRequestBuilderStages {
    // .load
    public static interface IonLoadRequestBuilder {
        public IonBodyParamsRequestBuilder load(String url);
        public IonBodyParamsRequestBuilder load(String method, String url);
        public IonFutureRequestBuilder load(File file);
    }

    // set parameters
    public static interface IonBodyParamsRequestBuilder extends IonFormMultipartBodyRequestBuilder, IonUrlEncodedBodyRequestBuilder {
        public IonBodyParamsRequestBuilder setHandler(Handler handler);
        public IonBodyParamsRequestBuilder setHeader(String name, String value);
        public IonBodyParamsRequestBuilder addHeader(String name, String value);
        public IonBodyParamsRequestBuilder setTimeout(int timeoutMilliseconds);
        public IonFutureRequestBuilder setJSONObjectBody(JSONObject jsonObject);
        public IonFutureRequestBuilder setStringBody(String string);
    }

    // set additional body parameters for multipart/form-data
    public static interface IonFormMultipartBodyRequestBuilder extends IonFutureRequestBuilder {
        public IonFormMultipartBodyRequestBuilder setMultipartParameter(String name, String value);
        public IonFormMultipartBodyRequestBuilder setMultiparFile(String name, File file);
    }

    // set additional body parameters for url form encoded
    public static interface IonUrlEncodedBodyRequestBuilder extends IonFutureRequestBuilder {
        public IonUrlEncodedBodyRequestBuilder setBodyParameter(String name, String value);
    }

    // get the result, transformed to how you want it
    public static interface IonFutureRequestBuilder extends IonBitmapFutureRequestBuilder {
        public Future<String> asString();
        public Future<JSONObject> asJSONObject();
        public IonMutableBitmapRequestBuilder withBitmap();
        public Future<OutputStream> write(OutputStream outputStream);
        public Future<OutputStream> write(OutputStream outputStream, boolean close);
        public Future<File> write(File file);
    }

    public static interface IonMutableBitmapRequestBuilder extends IonBitmapFutureRequestBuilder {
        public IonMutableBitmapRequestBuilder transform(Transform transform);
    }

    public static interface IonBitmapFutureRequestBuilder {
        public Future<Bitmap> intoImageView(ImageView imageView);
        public Future<Bitmap> asBitmap();
    }
}
