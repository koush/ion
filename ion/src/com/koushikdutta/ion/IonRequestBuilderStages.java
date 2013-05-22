package com.koushikdutta.ion;

import android.os.Handler;
import com.koushikdutta.async.future.Future;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by koush on 5/21/13.
 */
public class IonRequestBuilderStages {
    public static interface IonBodyParamsRequestBuilder extends IonFutureRequestBuilder {
        public IonBodyParamsRequestBuilder setHandler(Handler handler);
        public IonBodyParamsRequestBuilder setHeader(String name, String value);
        public IonBodyParamsRequestBuilder addHeader(String name, String value);
        public IonBodyParamsRequestBuilder setTimeout(int timeoutMilliseconds);
        public IonFutureRequestBuilder setJSONObjectBody(JSONObject jsonObject);
        public IonFutureRequestBuilder setStringBody(String string);
        public IonUrlEncodedBodyRequestBuilder setBodyParameter(String name, String value);
        public IonFormMultipartBodyRequestBuilder setMultipartParameter(String name, String value);
        public IonFormMultipartBodyRequestBuilder setMultiparFile(String name, File file);
    }

    public static interface IonFormMultipartBodyRequestBuilder extends IonFutureRequestBuilder {
        public IonFormMultipartBodyRequestBuilder setMultipartParameter(String name, String value);
        public IonFormMultipartBodyRequestBuilder setMultiparFile(String name, File file);
    }

    public static interface IonUrlEncodedBodyRequestBuilder extends IonFutureRequestBuilder {
        public IonUrlEncodedBodyRequestBuilder setBodyParameter(String name, String value);
    }

    public static interface IonFutureRequestBuilder {
        public Future<String> asString();
        public Future<JSONObject> asJSONObject();
    }

    public static interface IonLoadRequestBuilder {
        public IonBodyParamsRequestBuilder load(String url);
        public IonBodyParamsRequestBuilder load(String method, String url);
    }
}
