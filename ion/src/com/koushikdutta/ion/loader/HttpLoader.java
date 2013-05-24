package com.koushikdutta.ion.loader;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;

/**
 * Created by koush on 5/22/13.
 */
public class HttpLoader implements Loader {
    @Override
    public Future<DataEmitter> load(Ion ion, AsyncHttpRequest request) {
        if (!request.getUri().getScheme().startsWith("http"))
            return null;
        return (Future< DataEmitter >)(Future)ion.getHttpClient().execute(request);
    }
}
