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
import java.util.Set;

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
            Map<String, List<String>> cookies = manager.get (data.request.getUri(), data.request.getHeaders().getHeaders().toMultimap());
            data.request.getHeaders().addCookies(cookies);
        }
        catch (Exception e) {
        }
    }

    @Override
    public void onHeadersReceived(OnHeadersReceivedData data) {
        try {
            manager.put(data.request.getUri(), data.headers.getHeaders().toMultimap());

            // no cookies to persist.
            if (data.headers.getHeaders().get("Set-Cookie") == null)
                return;

            URI uri = data.request.getUri();
            String key = uri.getScheme() + "://" + uri.getAuthority();
            preferences.edit().putString(key, data.headers.getHeaders().toHeaderString()).commit();
        }
        catch (Exception e) {
        }
    }
}
