package com.koushikdutta.ion.cookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.koushikdutta.async.http.SimpleMiddleware;
import com.koushikdutta.async.http.libcore.RawHeaders;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Created by koush on 5/29/13.
 */
public class CookieMiddleware extends SimpleMiddleware {
    CookieManager manager;
    SharedPreferences preferences;

    public CookieStore getCookieStore() {
        return manager.getCookieStore();
    }

    public void clear() {
        getCookieStore().removeAll();
        preferences.edit().clear().commit();
    }

    public CookieManager getCookieManager() {
        return manager;
    }

    public CookieMiddleware(Context context, String name) {
        reinit(context, name);
    }

    public void reinit(Context context, String name) {
        manager = new CookieManager(null, null);
        preferences = context.getSharedPreferences(name + "-cookies", Context.MODE_PRIVATE);

        Map<String, ?> allPrefs = preferences.getAll();
        for (String key: allPrefs.keySet()) {
            try {
                String value = preferences.getString(key, null);
                RawHeaders headers = new RawHeaders();
                String[] lines = value.split("\n");
                boolean first = true;
                for (String line: lines) {
                    if (first) {
                        first = false;
                        headers.setStatusLine(line);
                    }
                    else if (!TextUtils.isEmpty(line)) {
                        headers.addLine(line);
                    }
                }
                manager.put(URI.create(key), headers.toMultimap());
            }
            catch (Exception e) {
                Log.e("Ion", "unable to load cookies", e);
            }
        }
    }

    @Override
    public void onSocket(OnSocketData data) {
        try {
            Map<String, List<String>> cookies = manager.get(URI.create(data.request.getUri().toString()), data.request.getHeaders().getHeaders().toMultimap());
            data.request.getHeaders().addCookies(cookies);
        }
        catch (Exception e) {
        }
    }

    @Override
    public void onHeadersReceived(OnHeadersReceivedData data) {
        try {
            put(URI.create(data.request.getUri().toString()), data.headers.getHeaders());
        }
        catch (Exception e) {
        }
    }

    public void put(URI uri, RawHeaders headers) {
        try {
            manager.put(uri, headers.toMultimap());

            // no cookies to persist.
            if (headers.get("Set-Cookie") == null)
                return;

            List<HttpCookie> cookies = manager.getCookieStore().get(uri);

            RawHeaders dump = new RawHeaders();
            dump.setStatusLine("HTTP/1.1 200 OK");
            for (HttpCookie cookie: cookies) {
                dump.add("Set-Cookie", cookie.getName() + "=" + cookie.getValue());
            }

            String key = uri.getScheme() + "://" + uri.getAuthority();
            preferences.edit().putString(key, dump.toHeaderString()).commit();
        }
        catch (Exception e) {
        }
    }
}
