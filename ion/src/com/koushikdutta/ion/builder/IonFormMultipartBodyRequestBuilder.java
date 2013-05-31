package com.koushikdutta.ion.builder;

import java.io.File;

/**
* Created by koush on 5/30/13.
*/ // set additional body parameters for multipart/form-data
public interface IonFormMultipartBodyRequestBuilder extends IonFutureRequestBuilder {
    /**
     * Specify a multipart/form-data parameter to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param name Multipart name
     * @param value Multipart String value
     * @return
     */
    public IonFormMultipartBodyRequestBuilder setMultipartParameter(String name, String value);

    /**
     * Specify a multipart/form-data file to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param name Multipart name
     * @param file Multipart file to send
     * @return
     */
    public IonFormMultipartBodyRequestBuilder setMultipartFile(String name, File file);
}
