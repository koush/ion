package com.koushikdutta.ion.loader;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.FileDataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;

import java.io.File;

/**
 * Created by koush on 5/22/13.
 */
public class FileLoader implements Loader {
    private static final class FileFuture extends SimpleFuture<DataEmitter> {
    }

    @Override
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request) {
        if (!request.getUri().getScheme().startsWith("file"))
            return null;
        FileFuture ret = new FileFuture();
        ret.setComplete(new FileDataEmitter(ion.getHttpClient().getServer(), new File(request.getUri())));
        return ret;
    }
}
