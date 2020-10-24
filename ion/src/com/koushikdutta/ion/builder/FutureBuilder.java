package com.koushikdutta.ion.builder;

import com.koushikdutta.ion.util.AsyncParser;
import com.koushikdutta.scratch.AsyncInput;

import org.w3c.dom.Document;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
* Created by koush on 5/30/13.
*/ // get the result, transformed to how you want it
public interface FutureBuilder extends BitmapFutureBuilder, ImageViewFutureBuilder, GsonFutureBuilder {
    /**
     * Execute the request and get the result as a String
     * @return
     */
    public ResponsePromise<String> asString();

    /**
     * Execute the request and get the result as a String
     * @param charset Specify a charset to use.
     * @return
     */
    public ResponsePromise<String> asString(Charset charset);

    /**
     * Execute the request and get the result as an InputStream.
     * This method will load the entire response into memory
     * and should not be used for large responses.
     * @return
     */
    public ResponsePromise<InputStream> asInputStream();

    /**
     * Execute the request and get the result as an XML Document
     * @return
     */
    public ResponsePromise<Document> asDocument();

    /**
     * Use the request as a Bitmap which can then be modified and/or applied to an ImageView.
     * @return
     */
    public Builders.Any.BF<? extends Builders.Any.BF<?>> withBitmap();

    /**
     * Execute the request and write it to the given OutputStream.
     * The OutputStream will be closed upon finishing.
     * The OutputStream must be a non-blocking stream, such as FileOutputStream
     * or ByteArrayOutputStream.
     * @param outputStream OutputStream to write the request
     * @return
     */
    @Deprecated
    public <T extends OutputStream> ResponsePromise<T> write(T outputStream);

    /**
     * Execute the request and write it to the given OutputStream.
     * Specify whether the OutputStream will be closed upon finishing.
     * The OutputStream must be a non-blocking stream, such as FileOutputStream
     * or ByteArrayOutputStream.
     * @param outputStream OutputStream to write the request
     * @param close Indicate whether the OutputStream should be closed on completion.
     * @return
     */
    @Deprecated
    public <T extends OutputStream> ResponsePromise<T> write(T outputStream, boolean close);

    /**
     * Execute the request and write the results to a file
     * @param file File to write
     * @return
     */
    public ResponsePromise<File> write(File file);

    /**
     * Deserialize a response into an object given a custom parser.
     * @param parser
     * @param <T>
     * @return
     */
    public <T> ResponsePromise<T> as(AsyncParser<T> parser);

    /**
     * Execute the request and get the result as a byte array
     * @return
     */
    public ResponsePromise<byte[]> asByteArray();

    /**
     * Add this request to a group specified by groupKey. This key can be used in a later call to
     * Ion.cancelAll(groupKey) to cancel all the requests in the same group.
     * @param groupKey
     * @return
     */
    public FutureBuilder group(Object groupKey);
}
