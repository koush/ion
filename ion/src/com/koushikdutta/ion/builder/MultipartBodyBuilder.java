package com.koushikdutta.ion.builder;

import java.io.File;

/**
* Created by koush on 5/30/13.
*/ // set additional body parameters for multipart/form-data
public interface MultipartBodyBuilder<M extends MultipartBodyBuilder> {
    /**
     * Specify a multipart/form-data parameter to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param name Multipart name
     * @param value Multipart String value
     * @return
     */
    public M setMultipartParameter(String name, String value);

    /**
     * Specify a multipart/form-data file to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param name Multipart name
     * @param file Multipart file to send
     * @return
     */
    public M setMultipartFile(String name, File file);

    /**
     * Specify a multipart/form-data file to send to the HTTP server. If no HTTP method was explicitly
     * provided in the load call, the default HTTP method, POST, is used.
     * @param name Multipart name
     * @param file Multipart Content-Type
     * @param file Multipart file to send
     * @return
     */
    public M setMultipartFile(String name, String contentType, File file);
}
