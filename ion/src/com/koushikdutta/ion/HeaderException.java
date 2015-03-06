package com.koushikdutta.ion;

/**
  * Created by collin on 3/5/15.
  */
 public class HeaderException extends Exception {

    private HeadersResponse response;

    public HeaderException(HeadersResponse headersResponse) {
        response = headersResponse;
    }

    public int getCode() {
        return response != null ? response.code() : -1;
    }

 }
