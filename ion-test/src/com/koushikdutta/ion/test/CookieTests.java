package com.koushikdutta.ion.test;

import android.os.SystemClock;
import android.test.AndroidTestCase;

import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import com.koushikdutta.ion.Ion;

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
        RequestHeaders requestHeaders = new RequestHeaders(uri, newHeaders);
        Map<String, List<String>> cookies = manager.get(uri, newHeaders.toMultimap());
        manager.get(uri, cookies);
        requestHeaders.addCookies(cookies);
        assertTrue(newHeaders.get("Cookie").contains("foo=goop"));
        assertTrue(newHeaders.get("Cookie").contains("poop=scoop"));
        assertFalse(newHeaders.get("Cookie").contains("bar"));
    }
}
