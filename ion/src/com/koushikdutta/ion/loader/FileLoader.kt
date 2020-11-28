package com.koushikdutta.ion.loader

import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.IonRequestOptions
import com.koushikdutta.ion.Loader.LoaderResult
import com.koushikdutta.ion.ResponseServedFrom
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.http.AsyncHttpRequest
import kotlinx.coroutines.Deferred
import java.io.File
import java.net.URI

/**
 * Created by koush on 5/22/13.
 */
class FileLoader : SimpleLoader() {
    override fun load(ion: Ion, options: IonRequestOptions, request: AsyncHttpRequest): Deferred<LoaderResult>? {
        if (request.uri.scheme != "file")
            return null

        return ion.loop.async {
            val file = File(URI.create(request.uri.toString()))
            val input = openFile(file)
            val size = input.size()
            LoaderResult(input, size, ResponseServedFrom.LOADED_FROM_CACHE, null, request)
        }
    }
}