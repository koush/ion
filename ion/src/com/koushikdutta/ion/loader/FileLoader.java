package com.koushikdutta.ion.loader;

import com.koushikdutta.async.FileDataEmitter;
import com.koushikdutta.async.future.FutureDataEmitter;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;

/**
 * Created by koush on 5/22/13.
 */
public class FileLoader implements Loader {
    private static final class FileFuture extends SimpleFuture<FileDataEmitter> implements FutureDataEmitter<FileDataEmitter> {
    }

    @Override
    public FutureDataEmitter load(Ion ion, AsyncHttpRequest request) {
        return null;
    }
}
