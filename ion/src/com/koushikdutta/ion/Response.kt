package com.koushikdutta.ion

import com.koushikdutta.scratch.Result
import com.koushikdutta.scratch.http.AsyncHttpRequest

/**
 * Created by koush on 7/6/13.
 */
class Response<T>(val request: AsyncHttpRequest?, val servedFrom: ResponseServedFrom?, val headers: HeadersResponse?, val result: Result<T>)
