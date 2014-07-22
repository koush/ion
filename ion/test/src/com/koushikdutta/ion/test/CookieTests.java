package com.koushikdutta.ion.test;

import android.test.AndroidTestCase;

import com.koushikdutta.async.http.cache.RawHeaders;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.cookie.CookieMiddleware;

import java.net.CookieManager;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Created by koush on 12/1/13.
 */
public class CookieTests extends AndroidTestCase {
    public void testCookies() throws Exception {
        Ion ion = Ion.getDefault(getContext());

        ion.getCookieMiddleware().clear();

        CookieManager manager = new CookieManager(null, null);

        RawHeaders headers = new RawHeaders();
        headers.setStatusLine("HTTP/1.1 200 OK");
        headers.set("Set-Cookie", "foo=bar");

        URI uri = URI.create("http://example.com");
        manager.put(uri, headers.toMultimap());

        headers.set("Set-Cookie", "poop=scoop");
        manager.put(uri, headers.toMultimap());

        headers.set("Set-Cookie", "foo=goop");
        manager.put(uri, headers.toMultimap());

        RawHeaders newHeaders = new RawHeaders();
        Map<String, List<String>> cookies = manager.get(uri, newHeaders.toMultimap());
        manager.get(uri, cookies);
        CookieMiddleware.addCookies(cookies, newHeaders);
        assertTrue(newHeaders.get("Cookie").contains("foo=goop"));
        assertTrue(newHeaders.get("Cookie").contains("poop=scoop"));
        assertFalse(newHeaders.get("Cookie").contains("bar"));
    }

    public void testReinit() throws Exception {
        CookieMiddleware middleware = Ion.getDefault(getContext()).getCookieMiddleware();
        Ion ion = Ion.getDefault(getContext());

        ion.getCookieMiddleware().clear();

        RawHeaders headers = new RawHeaders();
        headers.setStatusLine("HTTP/1.1 200 OK");
        headers.set("Set-Cookie", "foo=bar");

        URI uri = URI.create("http://example.com");
        middleware.put(uri, headers);

        headers.set("Set-Cookie", "poop=scoop");
        middleware.put(uri, headers);

        headers.set("Set-Cookie", "foo=goop");
        middleware.put(uri, headers);

        middleware.reinit(getContext(), Ion.getDefault(getContext()).getName());
        CookieManager manager = middleware.getCookieManager();

        RawHeaders newHeaders = new RawHeaders();
        Map<String, List<String>> cookies = manager.get(uri, newHeaders.toMultimap());
        manager.get(uri, cookies);
        CookieMiddleware.addCookies(cookies, newHeaders);
        assertTrue(newHeaders.get("Cookie").contains("foo=goop"));
        assertTrue(newHeaders.get("Cookie").contains("poop=scoop"));
        assertFalse(newHeaders.get("Cookie").contains("bar"));
    }
}
