package com.koushikdutta.ion;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.animation.Animation;
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
        /**
         * Load an uri.
         * @param uri Uri to load. This may be a http(s), file, or content uri.
         * @return
         */
        public IonBodyParamsRequestBuilder load(String uri);

        /**
         * Load an url using the given an HTTP method such as GET or POST.
         * @param method HTTP method such as GET or POST.
         * @param url Url to load.
         * @return
         */
        public IonBodyParamsRequestBuilder load(String method, String url);

        /**
         * Load a file.
         * @param file File to load.
         * @return
         */
        public IonFutureRequestBuilder load(File file);
    }

    // set parameters
    public static interface IonBodyParamsRequestBuilder extends IonFormMultipartBodyRequestBuilder, IonUrlEncodedBodyRequestBuilder {
        /**
         * Enable logging for this request
         * @param tag LOGTAG to use
         * @param level Log level of messages to display
         * @return
         */
        public IonBodyParamsRequestBuilder setLogging(String tag, int level);

        /**
         * Callback that is invoked on download progress
         */
        public interface ProgressCallback {
            void onProgress(int downloaded, int total);
        }

        /**
         * Specify a callback that is invoked on download progress.
         * @param callback
         * @return
         */
        public IonBodyParamsRequestBuilder progress(ProgressCallback callback);

        /**
         * Post the Future callback onto the given handler. Not specifying this explicitly
         * results in the default handle of Thread.currentThread to be used, if one exists.
         * @param handler Handler to use or null
         * @return
         */
        public IonBodyParamsRequestBuilder setHandler(Handler handler);

        /**
         * Set a HTTP header
         * @param name Header name
         * @param value Header value
         * @return
         */
        public IonBodyParamsRequestBuilder setHeader(String name, String value);

        /**
         * Add an HTTP header
         * @param name Header name
         * @param value Header value
         * @return
         */
        public IonBodyParamsRequestBuilder addHeader(String name, String value);

        /**
         * Specify the timeout in milliseconds before the request will cancel.
         * A CancellationException will be returned as the result.
         * @param timeoutMilliseconds Timeout in milliseconds
         * @return
         */
        public IonBodyParamsRequestBuilder setTimeout(int timeoutMilliseconds);

        /**
         * Specify a JSONObject to send to the HTTP server. If no HTTP method was explicitly
         * provided in the load call, the default HTTP method, POST, is used.
         * @param jsonObject JSONObject to send with the request
         * @return
         */
        public IonFutureRequestBuilder setJSONObjectBody(JSONObject jsonObject);
        /**
         * Specify a String to send to the HTTP server. If no HTTP method was explicitly
         * provided in the load call, the default HTTP method, POST, is used.
         * @param string String to send with the request
         * @return
         */
        public IonFutureRequestBuilder setStringBody(String string);
    }

    // set additional body parameters for multipart/form-data
    public static interface IonFormMultipartBodyRequestBuilder extends IonFutureRequestBuilder {
        /**
         * Specify a multipart/form-data parameter to send to the HTTP server. If no HTTP method was explicitly
         * provided in the load call, the default HTTP method, POST, is used.
         * @param name Multipart name
         * @param value Multipart String value
         * @return
         */
        public IonFormMultipartBodyRequestBuilder setMultipartParameter(String name, String value);

        /**
         * Specify a multipart/form-data file to send to the HTTP server. If no HTTP method was explicitly
         * provided in the load call, the default HTTP method, POST, is used.
         * @param name Multipart name
         * @param file Multipart file to send
         * @return
         */
        public IonFormMultipartBodyRequestBuilder setMultipartFile(String name, File file);
    }

    // set additional body parameters for url form encoded
    public static interface IonUrlEncodedBodyRequestBuilder extends IonFutureRequestBuilder {
        /**
         * Specify a application/x-www-form-urlencoded name and value pair to send to the HTTP server.
         * If no HTTP method was explicitly provided in the load call, the default HTTP method, POST, is used.
         * @param name Form field name
         * @param value Form field String value
         * @return
         */
        public IonUrlEncodedBodyRequestBuilder setBodyParameter(String name, String value);
    }

    // get the result, transformed to how you want it
    public static interface IonFutureRequestBuilder extends IonBitmapFutureRequestBuilder, IonBitmapImageViewFutureRequestBuilder {
        /**
         * Execute the request and get the result as a String
         * @return
         */
        public Future<String> asString();

        /**
         * Execute the request and get the result as a JSONObject
         * @return
         */
        public Future<JSONObject> asJSONObject();

        /**
         * Use the request as a Bitmap which can then be modified and/or applied to an ImageView.
         * @return
         */
        public IonMutableBitmapRequestBuilder withBitmap();

        /**
         * Execute the request and write it to the given OutputStream.
         * The OutputStream will be closed upon finishing.
         * @param outputStream OutputStream to write the request
         * @return
         */
        public <T extends OutputStream> Future<T> write(T outputStream);

        /**
         * Execute the request and write it to the given OutputStream.
         * Specify whether the OutputStream will be closed upon finishing.
         * @param outputStream OutputStream to write the request
         * @param close Indicate whether the OutputStream should be closed on completion.
         * @return
         */
        public <T extends OutputStream> Future<T> write(T outputStream, boolean close);

        /**
         * Execute the request and write the results to a file
         * @param file File to write
         * @return
         */
        public Future<File> write(File file);
    }

    public static interface IonMutableBitmapRequestBuilder extends IonBitmapFutureRequestBuilder, IonImageViewRequestBuilder {
        /**
         * Apply a transformation to a Bitmap
         * @param transform Transform to apply
         * @return
         */
        public IonMutableBitmapRequestBuilder transform(Transform transform);
    }

    public static interface IonImageViewRequestBuilder extends IonBitmapImageViewFutureRequestBuilder {
        /**
         * Set a placeholder on the ImageView while the request is loading
         * @param bitmap
         * @return
         */
        public IonImageViewRequestBuilder placeholder(Bitmap bitmap);

        /**
         * Set a placeholder on the ImageView while the request is loading
         * @param drawable
         * @return
         */
        public IonImageViewRequestBuilder placeholder(Drawable drawable);

        /**
         * Set a placeholder on the ImageView while the request is loading
         * @param resourceId
         * @return
         */
        public IonImageViewRequestBuilder placeholder(int resourceId);

        /**
         * Set an error image on the ImageView if the request fails to load
         * @param bitmap
         * @return
         */
        public IonImageViewRequestBuilder error(Bitmap bitmap);

        /**
         * Set an error image on the ImageView if the request fails to load
         * @param drawable
         * @return
         */
        public IonImageViewRequestBuilder error(Drawable drawable);

        /**
         * Set an error image on the ImageView if the request fails to load
         * @param resourceId
         * @return
         */
        public IonImageViewRequestBuilder error(int resourceId);

        /**
         * If an ImageView is loaded successfully from a remote source or file storage,
         * animate it in using the given Animation. The default animation is to fade
         * in.
         * @param in Animation to apply to the ImageView after the request has loaded
         *           and the Bitmap has been retrieved.
         * @return
         */
        public IonImageViewRequestBuilder animateIn(Animation in);

        /**
         * If the ImageView needs to load from a remote source or file storage,
         * the given Animation will be used while it is loading.
         * @param load Animation to apply to the imageView while the request is loading.
         * @return
         */
        public IonImageViewRequestBuilder animateLoad(Animation load);
    }

    public static interface IonBitmapFutureRequestBuilder {
        /**
         * Perform the request and get the result as a Bitmap
         * @return
         */
        public Future<Bitmap> asBitmap();
    }

    public static interface IonBitmapImageViewFutureRequestBuilder {
        /**
         * Perform the request and get the result as a Bitmap, which will then be loaded
         * into the given ImageView
         * @param imageView ImageView to set once the request completes
         * @return
         */
        public Future<Bitmap> intoImageView(ImageView imageView);
    }
}
