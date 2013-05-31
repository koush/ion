package com.koushikdutta.ion.builder;

/**
* Created by koush on 5/30/13.
*/ // set additional body parameters for url form encoded
public interface IonUrlEncodedBodyRequestBuilder extends IonFutureRequestBuilder {
    /**
     * Specify a application/x-www-form-urlencoded name and value pair to send to the HTTP server.
     * If no HTTP method was explicitly provided in the load call, the default HTTP method, POST, is used.
     * @param name Form field name
     * @param value Form field String value
     * @return
     */
    public IonUrlEncodedBodyRequestBuilder setBodyParameter(String name, String value);
}
