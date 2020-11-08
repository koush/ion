package com.koushikdutta.ion;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.bitmap.LocallyCachedStatus;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.builder.FutureBuilder;
import com.koushikdutta.ion.builder.IonPromise;
import com.koushikdutta.ion.builder.LoadBuilder;
import com.koushikdutta.ion.builder.ResponsePromise;
import com.koushikdutta.ion.gson.GsonArrayParser;
import com.koushikdutta.ion.gson.GsonArraySerializer;
import com.koushikdutta.ion.gson.GsonObjectParser;
import com.koushikdutta.ion.gson.GsonObjectSerializer;
import com.koushikdutta.ion.gson.PojoParser;
import com.koushikdutta.ion.gson.PojoSerializer;
import com.koushikdutta.ion.util.AsyncParser;
import com.koushikdutta.ion.util.AsyncSerializer;
import com.koushikdutta.ion.util.ByteArrayParser;
import com.koushikdutta.ion.util.ByteBufferListSerializer;
import com.koushikdutta.ion.util.ContentDisposition;
import com.koushikdutta.ion.util.DocumentParser;
import com.koushikdutta.ion.util.DocumentSerializer;
import com.koushikdutta.ion.util.FileParser;
import com.koushikdutta.ion.util.FileSerializer;
import com.koushikdutta.ion.util.InputStreamParser;
import com.koushikdutta.ion.util.InputStreamSerializer;
import com.koushikdutta.ion.util.MultipartBody;
import com.koushikdutta.ion.util.OutputStreamParser;
import com.koushikdutta.ion.util.ParserMessageBody;
import com.koushikdutta.ion.util.QueryString;
import com.koushikdutta.ion.util.StringParser;
import com.koushikdutta.ion.util.StringSerializer;
import com.koushikdutta.ion.util.UrlEncodedFormBody;
import com.koushikdutta.scratch.LooperKt;
import com.koushikdutta.scratch.buffers.ByteBufferList;
import com.koushikdutta.scratch.http.AsyncHttpMessageBody;
import com.koushikdutta.scratch.http.AsyncHttpRequest;
import com.koushikdutta.scratch.http.Header;
import com.koushikdutta.scratch.http.Headers;
import com.koushikdutta.scratch.http.Methods;
import com.koushikdutta.scratch.parser.Part;

import org.w3c.dom.Document;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kotlin.NotImplementedError;
import kotlin.text.Charsets;
import kotlinx.coroutines.Deferred;

/**
 * Created by koush on 5/21/13.
 */
class IonRequestBuilder implements Builders.Any.B, Builders.Any.F, Builders.Any.M, Builders.Any.U, LoadBuilder<Builders.Any.B> {
    Ion ion;
    IonContext contextReference;
    Handler handler = Ion.mainHandler;
    String method = Methods.GET.name();
    String uri;
    AsyncHttpRequest rawRequest;

    public IonRequestBuilder(IonContext contextReference, Ion ion) {
        String alive = contextReference.isAlive();
        if (null != alive)
            Log.w("Ion", "Building request with dead context: " + alive);
        this.ion = ion;
        this.contextReference = contextReference;
    }

    @Override
    public IonRequestBuilder load(String url) {
        return loadInternal(method, url);
    }

    private IonRequestBuilder loadInternal(String method, String url) {
        this.method = method;
        if (!TextUtils.isEmpty(url) && url.startsWith("/"))
            url = new File(url).toURI().toString();
        this.uri = url;
        return this;
    }

    boolean methodWasSet;
    @Override
    public IonRequestBuilder load(String method, String url) {
        methodWasSet = true;
        return loadInternal(method, url);
    }

    @Override
    public IonRequestBuilder load(AsyncHttpRequest request) {
        rawRequest = request;
        return this;
    }

    Headers headers;
    private Headers getHeaders() {
        if (headers == null)
            headers = new Headers();
        return headers;
    }

    @Override
    public IonRequestBuilder userAgent(String userAgent) {
        if (TextUtils.isEmpty(userAgent))
            return this;
        return setHeader("User-Agent", userAgent);
    }

    @Override
    public IonRequestBuilder setHeader(String name, String value) {
        if (value == null)
            getHeaders().remove(name);
        else
            getHeaders().set(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder addHeader(String name, String value) {
        if (value != null)
            getHeaders().add(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder addHeaders(Map<String, List<String>> params) {
        if (params == null)
            return this;
        Headers headers = getHeaders();
        for (Map.Entry<String, List<String>> entry: params.entrySet()) {
            for (String value: entry.getValue()) {
                headers.add(entry.getKey(), value);
            }
        }
        return this;
    }

    boolean noCache;
    @Override
    public Builders.Any.B noCache() {
        noCache = true;
        return this;
    }

    QueryString query;
    @Override
    public IonRequestBuilder addQuery(String name, String value) {
        if (value == null)
            return this;
        if (query == null)
            query = new QueryString();
        query.add(name, value);
        return this;
    }

    @Override
    public IonRequestBuilder addQueries(Map<String, List<String>> params) {
       if (query == null)
          query = new QueryString();
       query.getMap().putAll(params);
       return this;
    }

    int timeoutMilliseconds = 10000;
    @Override
    public IonRequestBuilder setTimeout(int timeoutMilliseconds) {
        this.timeoutMilliseconds = timeoutMilliseconds;
        return this;
    }

    @Override
    public IonRequestBuilder setHandler(Handler handler) {
        this.handler = handler;
        return this;
    }

    Deferred<AsyncHttpMessageBody> body;
    private IonRequestBuilder setBody(Deferred<AsyncHttpMessageBody> body) {
        if (!methodWasSet)
            method = Methods.POST.name();
        this.body = body;
        return this;
    }

    private <T> IonRequestBuilder setBody(T value, AsyncSerializer<T> parser) {
        if (!methodWasSet)
            method = Methods.POST.name();
        this.body = new ParserMessageBody<>(value, parser).defer();
        return this;
    }

    @Override
    public IonRequestBuilder setJsonObjectBody(JsonObject jsonObject) {
        return setBody(jsonObject, new GsonObjectSerializer());
    }

    @Override
    public IonRequestBuilder setJsonArrayBody(JsonArray jsonArray) {
        return setBody(jsonArray, new GsonArraySerializer());
    }

    @Override
    public IonRequestBuilder setStringBody(String string) {
        return setBody(string, new StringSerializer(Charsets.UTF_8, "text/plain"));
    }

    boolean followRedirect = true;
    @Override
    public IonRequestBuilder followRedirect(boolean follow) {
        followRedirect = follow;
        return this;
    }

    @Override
    public IonRequestBuilder progressBar(ProgressBar progressBar) {
        this.progressBar = new WeakReference<ProgressBar>(progressBar);
        return this;
    }

    @Override
    public IonRequestBuilder progressDialog(ProgressDialog progressDialog) {
        this.progressDialog = new WeakReference<ProgressDialog>(progressDialog);
        return this;
    }

    WeakReference<ProgressBar> progressBar;
    WeakReference<ProgressDialog> progressDialog;

    ProgressCallback progress;
    @Override
    public IonRequestBuilder progress(ProgressCallback callback) {
        progress = callback;
        return this;
    }

    ProgressCallback progressHandler;
    @Override
    public IonRequestBuilder progressHandler(ProgressCallback callback) {
        progressHandler = callback;
        return this;
    }

    static interface LoadRequestCallback {
        boolean loadRequest(AsyncHttpRequest request);
    }
    LoadRequestCallback loadRequestCallback;

    <T> ResponsePromise<T> execute(final AsyncParser<T> parser) {
        return execute(parser, null);
    }

    <T> ResponsePromise<T> execute(final AsyncParser<T> parser, Runnable cancel) {
        return prepareExecute(parser, cancel).execute();
    }

    <T> IonExecutor<T> prepareExecute(final AsyncParser<T> parser, Runnable cancel) {
        return new IonExecutor<>(this, parser, cancel);
    }

    @Override
    public ResponsePromise<JsonObject> asJsonObject() {
        return execute(new GsonObjectParser());
    }

    @Override
    public ResponsePromise<JsonArray> asJsonArray() {
        return execute(new GsonArrayParser());
    }

    @Override
    public ResponsePromise<JsonObject> asJsonObject(Charset charset) {
        return execute(new GsonObjectParser(charset));
    }

    @Override
    public ResponsePromise<JsonArray> asJsonArray(Charset charset) {
        return execute(new GsonArrayParser(charset));
    }

    @Override
    public ResponsePromise<String> asString() {
        return execute(new StringParser());
    }

    @Override
    public ResponsePromise<String> asString(Charset charset) {
        return execute(new StringParser(charset, "text/plain"));
    }

    @Override
    public ResponsePromise<byte[]> asByteArray() {
        return execute(new ByteArrayParser("application/octet-stream"));
    }

    @Override
    public ResponsePromise<InputStream> asInputStream() {
        return execute(new InputStreamParser());
    }

    @Override
    public <T> ResponsePromise<T> as(AsyncParser<T> parser) {
        return execute(parser);
    }

    @Override
    public ResponsePromise<OutputStream> write(OutputStream outputStream, boolean close) {
        return execute(new OutputStreamParser(outputStream, close, "application/octet-stream"));
    }

    @Override
    public ResponsePromise<OutputStream> write(OutputStream outputStream) {
        return write(outputStream, true);
    }

    @Override
    public ResponsePromise<File> write(final File file) {
        return execute(new FileParser(ion.loop, file, "application/octet-stream"));
    }

    UrlEncodedFormBody bodyParameters;
    private void prepareUrlEncodedFormBody() {
        if (bodyParameters == null) {
            bodyParameters = new UrlEncodedFormBody();
            setBody(bodyParameters.defer());
        }
    }

    @Override
    public IonRequestBuilder setBodyParameter(String name, String value) {
        prepareUrlEncodedFormBody();
        if (value != null)
            bodyParameters.add(name, value);
        return this;
    }

    public IonRequestBuilder setBodyParameters(Map<String, List<String>> params) {
       prepareUrlEncodedFormBody();
       bodyParameters.getMap().putAll(params);
       return this;
    }

    MultipartBody multipartBody;

    @Override
    public IonRequestBuilder setMultipartFile(String name, File file) {
        return setMultipartFile(name, null, file);
    }

    private void prepareMultipartBody() {
        if (multipartBody != null)
            return;
        multipartBody = new MultipartBody();
        setBody(multipartBody.getDeferred());
    }

    private ContentDisposition prepareMultipartContentDisposition(String name) {
        ContentDisposition cd = new ContentDisposition();
        cd.setName(name);
        return cd;
    }

    @Override
    public IonRequestBuilder setMultipartFile(String name, String contentType, File file) {
        prepareMultipartBody();
        if (contentType == null)
            contentType = "application/octet-stream";
        ContentDisposition cd = prepareMultipartContentDisposition(name);
        cd.setFilename(file.getName());
        multipartBody.addPart(cd, new ParserMessageBody<>(file, new FileSerializer(ion.loop, contentType)).defer());
        return this;
    }

    @Override
    public IonRequestBuilder setMultipartParameter(String name, String value) {
        prepareMultipartBody();
        ContentDisposition cd = prepareMultipartContentDisposition(name);
        multipartBody.addPart(cd, new ParserMessageBody<>(value, new StringSerializer(Charsets.UTF_8, "text/plain")).defer());
        return this;
    }

    @Override
    public IonRequestBuilder setMultipartParameters(Map<String, List<String>> params) {
        for (String key: params.keySet()) {
            for (String value: params.get(key)) {
                if (value != null)
                    setMultipartParameter(key, value);
            }
        }
        return this;
    }

    @Override
    public IonRequestBuilder addMultipartParts(Iterable<Part> parameters) {
        prepareMultipartBody();

        for (Part part: parameters) {
            multipartBody.addPart(part);
        }
        return this;
    }

    @Override
    public Builders.Any.M addMultipartParts(Part... parameters) {
        prepareMultipartBody();

        for (Part part: parameters) {
            multipartBody.addPart(part);
        }
        return this;
    }

    @Override
    public IonRequestBuilder setMultipartContentType(String contentType) {
        throw new NotImplementedError();
    }

    @Override
    public IonImageViewRequestBuilder withBitmap() {
        return new IonImageViewRequestBuilder(this);
    }

    @Override
    public ImageViewFuture intoImageView(ImageView imageView) {
        return new IonImageViewRequestBuilder(this).withImageView(imageView).intoImageView(imageView);
    }

    @Override
    public IonRequestBuilder load(File file) {
        loadInternal(null, file.toURI().toString());
        return this;
    }

    @Override
    public BitmapInfo asCachedBitmap() {
        return new IonImageViewRequestBuilder(this).asCachedBitmap();
    }

    @Override
    public void removeCachedBitmap() {
        new IonImageViewRequestBuilder(this).removeCachedBitmap();
    }

    @Override
    public LocallyCachedStatus isLocallyCached() {
        return new IonImageViewRequestBuilder(this).isLocallyCached();
    }

    @Override
    public IonPromise<Bitmap> asBitmap() {
        return new IonPromise<>(contextReference, handler != null ? LooperKt.createScheduler(handler) : null, new IonImageViewRequestBuilder(this).asBitmap());
    }

    String logTag;
    int logLevel;
    @Override
    public IonRequestBuilder setLogging(String tag, int level) {
        logTag = tag;
        logLevel = level;
        return this;
    }

    @Override
    public <T> ResponsePromise<T> as(Class<T> clazz) {
        return execute(new PojoParser<>(ion.configure().getGson(), clazz));
    }

    @Override
    public <T> ResponsePromise<T> as(TypeToken<T> token) {
        return execute(new PojoParser<>(ion.configure().getGson(), token.getType()));
    }

    ArrayList<WeakReference<Object>> groups;
    @Override
    public FutureBuilder group(Object groupKey) {
        if (groups == null)
            groups = new ArrayList<>();
        groups.add(new WeakReference<>(groupKey));
        return this;
    }

    String proxyHost;
    int proxyPort;
    @Override
    public IonRequestBuilder proxy(String host, int port) {
        proxyHost = host;
        proxyPort = port;
        return this;
    }

    @Override
    public <T> IonRequestBuilder setJsonPojoBody(T object, TypeToken<T> token) {
        setBody(object, new PojoSerializer<>(ion.configure().getGson(), token.getType()));
        return this;
    }

    @Override
    public <T> IonRequestBuilder setJsonPojoBody(T object) {
        setBody(object, new PojoSerializer<>(ion.configure().getGson(), object.getClass()));
        return this;
    }

    @Override
    public IonRequestBuilder basicAuthentication(String username, String password) {
        return setHeader("Authorization", "Basic " + Base64.encodeToString(String.format("%s:%s", username, password).getBytes(), Base64.NO_WRAP));
    }

    ProgressCallback uploadProgress;
    @Override
    public Builders.Any.B uploadProgress(ProgressCallback callback) {
        uploadProgress = callback;
        return this;
    }

    WeakReference<ProgressBar> uploadProgressBar;
    @Override
    public Builders.Any.B uploadProgressBar(ProgressBar progressBar) {
        uploadProgressBar = new WeakReference<>(progressBar);
        return this;
    }

    WeakReference<ProgressDialog> uploadProgressDialog;
    @Override
    public Builders.Any.B uploadProgressDialog(ProgressDialog progressDialog) {
        uploadProgressDialog = new WeakReference<>(progressDialog);
        return this;
    }

    ProgressCallback uploadProgressHandler;
    @Override
    public Builders.Any.B uploadProgressHandler(ProgressCallback callback) {
        uploadProgressHandler = callback;
        return this;
    }

    HeadersCallback headersCallback;
    @Override
    public Builders.Any.B onHeaders(HeadersCallback callback) {
        headersCallback = callback;
        return this;
    }

    @Override
    public Builders.Any.F setDocumentBody(Document document) {
        setBody(document, new DocumentSerializer());
        return this;
    }

    @Override
    public ResponsePromise<Document> asDocument() {
        return execute(new DocumentParser());
    }

    @Override
    public Builders.Any.F setFileBody(File file) {
        setBody(file, new FileSerializer(ion.loop, "application/octet-stream"));
        return this;
    }

    @Override
    public Builders.Any.F setByteArrayBody(byte[] bytes) {
        ByteBufferList buffer = new ByteBufferList(ByteBuffer.wrap(bytes));
        setBody(buffer, new ByteBufferListSerializer());
        return this;
    }

    @Override
    public Builders.Any.F setStreamBody(InputStream inputStream) {
        setBody(inputStream, new InputStreamSerializer(null, "application/octet-stream"));
        return this;
    }

    @Override
    public Builders.Any.F setStreamBody(InputStream inputStream, long length) {
        setBody(inputStream, new InputStreamSerializer(length, "application/octet-stream"));
        return this;
    }

    @Override
    public Builders.Any.B setHeader(Header... header) {
        Headers headers = getHeaders();
        for (Header h: header) {
            headers.set(h);
        }
        return this;
    }
}
