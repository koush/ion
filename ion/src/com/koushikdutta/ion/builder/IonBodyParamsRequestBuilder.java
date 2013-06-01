package com.koushikdutta.ion.builder;

import android.app.ProgressDialog;
import android.os.Handler;
import android.widget.ProgressBar;

import com.koushikdutta.ion.ProgressCallback;

import org.json.JSONObject;

/**
* Created by koush on 5/30/13.
*/ // set parameters
public interface IonBodyParamsRequestBuilder extends IonFormMultipartBodyRequestBuilder, IonUrlEncodedBodyRequestBuilder {
    /**
     * Enable logging for this request
     * @param tag LOGTAG to use
     * @param level Log level of messages to display
     * @return
     */
    public IonBodyParamsRequestBuilder setLogging(String tag, int level);

    /**
     * Specify a callback that is invoked on download progress. This will not be invoked
     * on the UI thread.
     * @param callback
     * @return
     */
    public IonBodyParamsRequestBuilder progress(ProgressCallback callback);

    /**
     * Specify a callback that is invoked on download progress. This will be invoked
     * on the UI thread.
     * @param callback
     * @return
     */
    public IonBodyParamsRequestBuilder progressHandler(ProgressCallback callback);

    /**
     * Specify a ProgressBar to update during the request
     * @param progressBar
     * @return
     */
    public IonBodyParamsRequestBuilder progressBar(ProgressBar progressBar);

    /**
     * Specify a ProgressDialog to update during the request
     * @param progressDialog
     * @return
     */
    public IonBodyParamsRequestBuilder progressDialog(ProgressDialog progressDialog);

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
