package com.koushikdutta.ion.builder;

import android.app.ProgressDialog;
import android.os.Handler;
import android.widget.ProgressBar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.http.NameValuePair;
import com.koushikdutta.ion.HeadersCallback;
import com.koushikdutta.ion.ProgressCallback;

import org.w3c.dom.Document;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
* Created by koush on 5/30/13.
*/ // set parameters
public interface RequestBuilder<F, R extends RequestBuilder, M extends MultipartBodyBuilder, U extends UrlEncodedBuilder> extends MultipartBodyBuilder<M>, UrlEncodedBuilder<U> {
    /**
     * Enable logging for this request
     * @param tag LOGTAG to use
     * @param level Log level of messages to display
     * @return
     */
    public R setLogging(String tag, int level);

    /**
     * Route the request through the given proxy server.
     * @param host
     * @param port
     */
    public R proxy(String host, int port);

    /**
     * Specify a callback that is invoked on download progress. This will not be invoked
     * on the UI thread.
     * @param callback
     * @return
     */
    public R progress(ProgressCallback callback);

    /**
     * Specify a callback that is invoked on download progress. This will be invoked
     * on the UI thread.
     * @param callback
     * @return
     */
    public R progressHandler(ProgressCallback callback);

    /**
     * Specify a ProgressBar to update during the request
     * @param progressBar
     * @return
     */
    public R progressBar(ProgressBar progressBar);

    /**
     * Specify a ProgressDialog to update during the request
     * @param progressDialog
     * @return
     */
    public R progressDialog(ProgressDialog progressDialog);

    /**
     * Specify a callback that is invoked on upload progress of a HTTP
     * request body.
     * @param callback
     * @return
     */
    public R uploadProgress(ProgressCallback callback);

    /**
     * Specify a callback that is invoked on upload progress of a HTTP
     * request body. This will be invoked on the UI thread.
     * @param callback
     * @return
     */
    public R uploadProgressHandler(ProgressCallback callback);

    /**
     * Specify a ProgressBar to update while uploading
     * a request body.
     * @param progressBar
     * @return
     */
    public R uploadProgressBar(ProgressBar progressBar);

    /**
     * Specify a ProgressDialog to update while uploading
     * a request body.
     * @param progressDialog
     * @return
     */
    public R uploadProgressDialog(ProgressDialog progressDialog);

    /**
     * Post the Future callback onto the given handler. Not specifying this explicitly
     * results in the default handle of Thread.currentThread to be used, if one exists.
     * @param handler Handler to use or null
     * @return
     */
    public R setHandler(Handler handler);

    /**
     * Set a HTTP header
     * @param name Header name
     * @param value Header value
     * @return
     */
    public R setHeader(String name, String value);

    /**
     * Set HTTP headers
     * @param header
     * @return
     */
    public R setHeader(NameValuePair... header);

    /**
     * Disable usage of the cache for this request
     * @return
     */
    public R noCache();

    /**
     * Set whether this request will follow redirects
     */
    public R followRedirect(boolean follow);

    /**
     * Add an HTTP header
     * @param name Header name
     * @param value Header value
     * @return
     */
    public R addHeader(String name, String value);

    /**
     * Add multiple headers at once
     * @param params
     * @return
     */
    public R addHeaders(Map<String, List<String>> params);

    /**
     * Add a query parameter
     * @param name
     * @param value
     * @return
     */
    public R addQuery(String name, String value);

    /**
     * Add multiple query parameters at once
     * @param params
     * @return
     */
    public R addQueries(Map<String, List<String>> params);

    /**
     * Set the user agent of this request.
     * @param userAgent
     * @return
     */
    public R userAgent(String userAgent);

    /**
     * Specify the timeout in milliseconds before the request will cancel.
     * A CancellationException will be returned as the result.
     * @param timeoutMilliseconds Timeout in milliseconds
     * @return
     */
    public R setTimeout(int timeoutMilliseconds);

    /**
     * Invoke the given callback when the http request headers are received.
     * @param callback
     * @return
     */
    public R onHeaders(HeadersCallback callback);

    /**
     * Provide Basic authentication credentials to be sent with the request.
     * @param username
     * @param password
     * @return
     */
    public R basicAuthentication(String username, String password);

    /**
     * Specify a JsonObject to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param jsonObject JsonObject to send with the request
     * @return
     */
    public F setJsonObjectBody(JsonObject jsonObject);

    /**
     * Specify an object to convert to json and send to the HTTP server. If no HTTP
     * method was explicitly provided in the load call, the default HTTP method,
     * POST, is used.
     * @param object Object to serialize with Json and send with the request
     * @param token Type token to assist with generic type serialization
     * @return
     */
    public <T> F setJsonPojoBody(T object, TypeToken<T> token);

    /**
     * Specify an object to convert to json and send to the HTTP server. If no HTTP
     * method was explicitly provided in the load call, the default HTTP method,
     * POST, is used.
     * @param object Object to serialize with Json and send with the request
     * @return
     */
    public <T> F setJsonPojoBody(T object);

    /**
     * Specify a JsonArray to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param jsonArray JsonObject to send with the request
     * @return
     */
    public F setJsonArrayBody(JsonArray jsonArray);

    /**
     * Specify a String to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param string String to send with the request
     * @return
     */
    public F setStringBody(String string);

    /**
     * Specify an XML Document to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param document Document to send with the request
     * @return
     */
    public F setDocumentBody(Document document);

    /**
     * Specify a File to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param file File to send with the request
     * @return
     */
    public F setFileBody(File file);

    /**
     * Specify a byte array to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param bytes Bytes to send with the request
     * @return
     */
    public F setByteArrayBody(byte[] bytes);

    /**
     * Specify an InputStream to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param inputStream InputStream to send with the request
     * @return
     */
    public Builders.Any.F setStreamBody(InputStream inputStream);

    /**
     * Specify an InputStream to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param inputStream InputStream to send with the request
     * @param length length of the input stream (in bytes) to read
     * @return
     */
    public Builders.Any.F setStreamBody(InputStream inputStream, int length);
}
