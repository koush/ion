package com.koushikdutta.ion;

import android.os.Handler;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpRequestBody;
import org.json.JSONObject;

/**
 * Created by koush on 5/21/13.
 */
public class IonRequestBuilderStages {
    public static interface IonRequestBuilderBodyParams extends IonRequestBuilderParams {
        public IonRequestBuilderBodyParams setHandler(Handler handler);
        public IonRequestBuilderBodyParams setHeader(String name, String value);
        public IonRequestBuilderBodyParams addHeader(String name, String value);
        public IonRequestBuilderBodyParams setTimeout(int timeoutMilliseconds);
        public IonRequestBuilderParams setJSONObjectBody(JSONObject jsonObject);
        public IonRequestBuilderParams setStringBody(String string);
    }

    public static interface IonRequestBuilderParams {
        public Future<String> asString();
        public Future<JSONObject> asJSONObject();
    }

    public static interface IonRequestBuilderLoad {
        public IonRequestBuilderBodyParams load(String url);
        public IonRequestBuilderBodyParams load(String method, String url);
    }
}
