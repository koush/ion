package com.koushikdutta.ion.loader

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import com.koushikdutta.ion.Ion
import com.koushikdutta.scratch.asPromise
import com.koushikdutta.scratch.async.async
import com.koushikdutta.scratch.uri.URI
import kotlinx.coroutines.Deferred
import java.io.InputStream

/**
 * Created by koush on 6/20/14.
 */
class ResourceLoader : StreamLoader() {
    private class Resource( var res: Resources, val id: Int)

    override fun getInputStream(ion: Ion, uri: URI): Deferred<Pair<Long?, InputStream>>? {
        if (uri.scheme != "android.resource")
            return null

        return ion.loop.async {
            val res = lookupResource(ion.context, uri)
            val stream = res.res.openRawResource(res.id)
            val available = stream.available().toLong()
            Pair(available, stream)
        }
    }

    companion object {
        private fun lookupResource(context: Context, uri: URI): Resource {
            val u = Uri.parse(uri.toString())
            requireNotNull(u.pathSegments) { "uri is not a valid resource uri" }
            val pkg = u.authority
            val ctx = context.createPackageContext(pkg, 0)
            val res = ctx.resources
            val id: Int
            if (u.pathSegments.size == 1) id = Integer.valueOf(u.pathSegments[0])
            else if (u.pathSegments.size == 2) {
                val type = u.pathSegments[0]
                val name = u.pathSegments[1]
                id = res.getIdentifier(name, type, pkg)
                require(id != 0) { "resource not found in given package" }
            }
            else {
                throw IllegalArgumentException("uri is not a valid resource uri")
            }
            return Resource(res, id)
        }
    }
}
