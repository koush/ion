package com.koushikdutta.ion.loader;

import android.net.Uri;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.SimpleFuture;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.stream.InputStreamDataEmitter;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;

import java.io.InputStream;

/**
 * Created by koush on 5/22/13.
 */
public class ContentLoader implements Loader {
    private static final class InputStreamDataEmitterFuture extends SimpleFuture<DataEmitter> {
    }

    @Override
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request) {
        if (!request.getUri().getScheme().startsWith("content"))
            return null;

        InputStreamDataEmitterFuture ret = new InputStreamDataEmitterFuture();
        try {
            InputStream stream = ion.getContext().getContentResolver().openInputStream(Uri.parse(request.getUri().toString()));
            InputStreamDataEmitter emitter = new InputStreamDataEmitter(ion.getHttpClient().getServer(), stream);
            ret.setComplete(emitter);
        }
        catch (Exception e) {
            ret.setComplete(e);
        }
        return ret;
    }
}
