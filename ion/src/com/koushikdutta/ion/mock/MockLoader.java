package com.koushikdutta.ion.mock;

import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Loader;
import com.koushikdutta.ion.future.ResponseFuture;
import com.koushikdutta.ion.loader.SimpleLoader;

import java.lang.reflect.Type;

/**
 * Created by koush on 3/6/15.
 */
public class MockLoader extends SimpleLoader {
    public static void install(Ion ion, MockRequestHandler requestHandler) {
        MockLoader mockLoader = new MockLoader(requestHandler);
        for (Loader loader: ion.configure().getLoaders()) {
            if (loader instanceof MockLoader)
                throw new RuntimeException("MockLoader already installed.");
        }
        ion.configure().addLoader(0, mockLoader);
    }

    MockRequestHandler requestHandler;
    private MockLoader(MockRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public <T> ResponseFuture<T> load(Ion ion, AsyncHttpRequest request, Type type) {
        T result = (T)requestHandler.request(request.getUri().toString());
        if (result != null) {
            MockResponseFuture<T> ret = new MockResponseFuture<T>(request);
            ret.setComplete(result);
            return ret;
        }
        return super.load(ion, request, type);
    }
}
