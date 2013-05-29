package com.koushikdutta.ion.cookie;

import android.content.Context;
import com.koushikdutta.async.http.SimpleMiddleware;
import org.apache.http.impl.client.BasicCookieStore;

import java.net.CookieManager;
import java.net.CookieStore;
import java.util.List;
import java.util.Map;

/**
 * Created by koush on 5/29/13.
 */
public class CookieMiddleware extends SimpleMiddleware {
    CookieManager manager;

    public CookieStore getCookieStore() {
        return manager.getCookieStore();
    }

    public CookieManager getCookieManager() {
        return manager;
    }

    public CookieMiddleware(Context context) {
        manager = new CookieManager(null, null);
    }
    @Override
    public void onSocket(OnSocketData data) {
        try {
            Map<String, List<String>> cookies =  manager.get (data.request.getUri(), data.request.getHeaders().getHeaders().toMultimap());
            data.request.getHeaders().addCookies(cookies);
        }
        catch (Exception e) {
        }
    }

    @Override
    public void onHeadersReceived(OnHeadersReceivedData data) {
        try {
            manager.put(data.request.getUri(), data.headers.getHeaders().toMultimap());
        }
        catch (Exception e) {
        }
    }
}
