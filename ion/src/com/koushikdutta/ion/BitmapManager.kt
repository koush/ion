package com.koushikdutta.ion

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Looper
import android.text.TextUtils
import com.koushikdutta.ion.bitmap.BitmapInfo
import com.koushikdutta.ion.bitmap.IonBitmapCache
import com.koushikdutta.ion.bitmap.Transform
import com.koushikdutta.ion.gif.GifDecoder
import com.koushikdutta.ion.gif.GifFrame
import com.koushikdutta.ion.util.StreamUtility
import com.koushikdutta.scratch.buffers.ByteBuffer
import com.koushikdutta.scratch.buffers.ByteBufferList
import com.koushikdutta.scratch.createAsyncAffinity
import com.koushikdutta.scratch.event.await
import java.io.InputStream


private class UncacheableThrowable(val throwable: Throwable): Throwable(throwable)

internal class BitmapManager(val ion: Ion) {
    private val mainAffinity = Looper.getMainLooper().createAsyncAffinity()

    val pendingBitmaps = mutableMapOf<String, BitmapPromise>()

    private fun getOrCreateBitmapPromise(key: String, start: Boolean, block: suspend () -> BitmapInfo) = getOrCreateBitmapPromise(null, key, start, block)

    private fun getOrCreateBitmapPromise(lazyId: Int?, key: String, start: Boolean, block: suspend () -> BitmapInfo): BitmapPromise {
        requireMainThread()

        val loadPromise = pendingBitmaps[key]
        if (loadPromise != null)
            return loadPromise

        val newPromise = BitmapPromise(lazyId) {
            var uncacheable = false
            var forceCache = false
            val bi = try {
                try {
                    block()
                }
                catch (throwable: UncacheableThrowable) {
                    uncacheable = true
                    throw throwable.throwable
                }
            }
            catch (throwable: Throwable) {
                forceCache = true
                val bi = BitmapInfo(key, null, null, null)
                bi.exception = throwable
                bi
            }
            mainAffinity.await()
            pendingBitmaps.remove(key)
            if (!uncacheable) {
                if (forceCache)
                    ion.bitmapCache.put(bi)
                else
                    ion.bitmapCache.putSoft(bi)
            }
            bi
        }

        pendingBitmaps[key] = newPromise
        if (start)
            newPromise.start()
        return newPromise
    }

    private suspend fun getOrCreateBitmapInfo(key: String, start: Boolean, block: suspend () -> BitmapInfo): BitmapInfo {
        mainAffinity.await()
        ion.bitmapCache.get(key)?.let {
            return it
        }

        return getOrCreateBitmapPromise(key, start, block).await()
    }


    fun checkCache(bitmapRequest: BitmapRequest): BitmapInfo? {
        requireMainThread()
        bitmapRequest.computeKeys()
        return ion.bitmapCache.get(bitmapRequest.bitmapKey)
    }

    fun requestLazyLoad(bitmapRequest: BitmapRequest): BitmapPromise {
        val id = BitmapPromise.createLazyLoadPriority()
        val key = BitmapPromise.getLazyLoadKey(id, bitmapRequest.bitmapKey)
        return getOrCreateBitmapPromise(id, key, false) {
            val promise = requestInternal(bitmapRequest, true)
            promise.start()
            promise.await()
        }
    }

    fun requestInternal(bitmapRequest: BitmapRequest, start: Boolean) = BitmapPromise {
        try {
            // early out
            mainAffinity.await()

            checkCache(bitmapRequest)?.let {
                return@BitmapPromise it
            }

            var startKey = bitmapRequest.decodeKey
            var transformKey = bitmapRequest.decodeKey
            val transforms = mutableListOf<Transform>()
            if (bitmapRequest.transforms != null) {
                for (transform in bitmapRequest.transforms) {
                    transformKey = BitmapRequest.appendTransformKey(transformKey, transform)
                    val transformInfo = ion.bitmapCache[transformKey]
                    if (transformInfo != null) {
                        if (transformInfo.exception != null)
                            throw Exception("bitmap transform error", transformInfo.exception)

                        transforms.clear()
                        startKey = transformKey
                    }
                    else {
                        transforms.add(transform)
                    }
                }
            }

            var bitmapInfo = if (startKey == bitmapRequest.decodeKey) {
                getOrCreateBitmapInfo(bitmapRequest.decodeKey, start) {
                    val resolvedRequest = bitmapRequest.executor.resolvedRequest.await()
                    for (loader in ion.loaders) {
                        val fastLoad = loader.loadBitmap(bitmapRequest.executor.contextReference.context, ion, startKey, resolvedRequest, bitmapRequest.sampleWidth, bitmapRequest.sampleHeight, bitmapRequest.animateGif)
                        if (fastLoad != null)
                            return@getOrCreateBitmapInfo fastLoad.await()
                    }

                    val loadResult = try {
                        bitmapRequest.executor.execute().withResponse().await()
                    }
                    catch (throwable: Throwable) {
                        throw UncacheableThrowable(throwable)
                    }

                    val buffer = loadResult.result.getOrThrow()
                    Ion.getBitmapLoadExecutorService().await()
                    loadBitmap(ion, bitmapRequest.decodeKey, buffer, bitmapRequest.sampleWidth, bitmapRequest.sampleHeight, bitmapRequest.animateGif)
                }
            }
            else {
                ion.bitmapCache.get(startKey)
            }

            // no transforms
            if (transforms.isEmpty()) {
                mainAffinity.await()
                ion.bitmapCache.put(bitmapInfo)
                return@BitmapPromise bitmapInfo
            }


            var bitmap = bitmapInfo.bitmap
            transformKey = startKey
            for (transform in transforms) {
                transformKey = BitmapRequest.appendTransformKey(transformKey, transform)

                val transformInfo = ion.bitmapCache.get(transformKey)
                if (transformInfo != null) {
                    if (transformInfo.exception != null)
                        throw transformInfo.exception
                    bitmap = transformInfo.bitmap
                    continue
                }

                bitmapInfo = getOrCreateBitmapInfo(transformKey, true) {
                    Ion.bitmapExecutorService.await()
                    val transformed = transform.transform(bitmap)
                    if (transformed == null)
                        throw NullPointerException("failed to transform bitmap")
                    BitmapInfo(transformKey, bitmapInfo.mimeType, transformed, bitmapInfo.originalSize)
                }
            }

            mainAffinity.await()
            ion.bitmapCache.put(bitmapInfo)
            bitmapInfo
        }
        finally {
            bitmapRequest.executor.affinity?.await()
        }
    }

    fun request(bitmapRequest: BitmapRequest): BitmapPromise {
        val ret = requestInternal(bitmapRequest, true)
        ret.start()
        return ret
    }

    fun processDeferred() {
        val lazy = mutableListOf<BitmapPromise>()
        var loading = 0
        // only allow a fixed number of lazy loads to happen concurrently
        for (promise in pendingBitmaps.values) {
            if (!promise.isLazyLoad)
                continue

            if (promise.isLoading) {
                loading++
                continue
            }

            lazy.add(promise)
        }

        lazy.sortByDescending {
            it.lazyPriority
        }

        var available = Ion.numBitmapExecutors - loading
        while (lazy.isNotEmpty() && available > 0) {
            val start = lazy.removeFirst()
            start.start()
            available--
        }
    }

    companion object {
        fun loadBitmap(ion: Ion, key: String, buffer: ByteBufferList, resizeWidth: Int, resizeHeight: Int, animateGif: Boolean): BitmapInfo {
            val bb = buffer.readByteBuffer()
            val bitmap: Bitmap?
            val gifDecoder: GifDecoder?
            val options = ion.bitmapCache.prepareBitmapOptions(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), resizeWidth, resizeHeight)
            val size = Point(options.outWidth, options.outHeight)

            if (animateGif && TextUtils.equals("image/gif", options.outMimeType)) {
                gifDecoder = GifDecoder(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining())
                val frame = gifDecoder.nextFrame()
                bitmap = frame.image
            }
            else {
                bitmap = IonBitmapCache.loadBitmap(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining(), options)
                gifDecoder = null
                if (bitmap == null) throw Exception("failed to load bitmap")
            }
            val info = BitmapInfo(key, options.outMimeType, bitmap, size)
            info.gifDecoder = gifDecoder
            return info
        }


        fun loadGif(key: String, size: Point, inputStream: InputStream, options: BitmapFactory.Options): BitmapInfo {
            val gifDecoder = GifDecoder(ByteBuffer.wrap(StreamUtility.readToEndAsArray(inputStream)))
            val frame: GifFrame = gifDecoder.nextFrame()
            val info = BitmapInfo(key, options.outMimeType, frame.image, size)
            info.gifDecoder = gifDecoder
            return info
        }

        fun loadBitmap(ion: Ion, key: String, inputStream: InputStream, options: BitmapFactory.Options, animateGif: Boolean): BitmapInfo {
            val size = Point(options.outWidth, options.outHeight)

            val info = if (animateGif && TextUtils.equals("image/gif", options.outMimeType)) {
                loadGif(key, size, inputStream, options)
            }
            else {
                val bitmap = IonBitmapCache.loadBitmap(inputStream, options)
                if (bitmap == null)
                    throw Exception("failed to load bitmap")
                BitmapInfo(key, options.outMimeType, bitmap, size);
            }
            info.servedFrom = ResponseServedFrom.LOADED_FROM_CACHE;
            return info
        }
    }
}
