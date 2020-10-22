package com.koushikdutta.ion

import android.content.Context
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.ion.builder.ResponsePromise
import com.koushikdutta.scratch.AsyncInput
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.http.AsyncHttpRequest
import java.lang.reflect.Type

/**
 * Created by koush on 5/22/13.
 */
interface Loader {
    class LoaderResult(val input: AsyncInput, val length: Long?, val servedFrom: ResponseServedFrom,
                       val headers: HeadersResponse?,
                       val resolvedRequest: AsyncHttpRequest)

    /**
     * returns a Future if this loader can handle a request
     * otherwise it returns null, and Ion continues to the next loader.
     * @param ion
     * @param request
     * @param callback
     * @return
     */
    fun load(ion: Ion, request: AsyncHttpRequest): Promise<LoaderResult>?

    /**
     * returns a future if the laoder can handle the request as a bitmap
     * otherwise it returns null
     * @param ion
     * @param key
     * @param request
     * @param resizeWidth
     * @param resizeHeight
     * @return
     */
    fun loadBitmap(context: Context, ion: Ion, key: String, request: AsyncHttpRequest, resizeWidth: Int, resizeHeight: Int, animateGif: Boolean): Promise<BitmapInfo>?

    /**
     * Resolve a request into another request.
     * @param ion
     * @param request
     * @return
     */
    fun resolve(context: Context, ion: Ion, request: AsyncHttpRequest): Promise<AsyncHttpRequest>?

    /**
     * Resolve a request to a Response<T extends type>
     * @param ion
     * @param request
     * @param type
     * @return
     */
    fun <T> load(ion: Ion, request: AsyncHttpRequest, type: Type): ResponsePromise<T>?
}