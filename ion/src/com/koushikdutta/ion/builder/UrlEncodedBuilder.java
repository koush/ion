package com.koushikdutta.ion.builder;

import java.util.List;
import java.util.Map;

/**
* Created by koush on 5/30/13.
*/ // set additional body parameters for url form encoded
public interface UrlEncodedBuilder<U extends UrlEncodedBuilder> {
    /**
     * Specify a application/x-www-form-urlencoded name and value pair to send to the HTTP server.
     * If no HTTP method was explicitly provided in the load call, the default HTTP method, POST, is used.
     * @param name Form field name
     * @param value Form field String value
     * @return
     */
    public U setBodyParameter(String name, String value);
    /**
     * Specifies a map with application/x-www-form-urlencoded name and value pairs to send to the HTTP server.
     * If no HTTP method was explicitly provided in the load call, the default HTTP method, POST, is used.
     * @param params The map containing key value pairs
     * @return
     */
    public U setBodyParameters(Map<String, List<String>> params);
}
