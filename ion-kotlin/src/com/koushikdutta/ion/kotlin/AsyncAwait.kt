package com.koushikdutta.ion.kotlin

import android.app.Activity
import android.app.Fragment
import android.app.Service
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.future.Future
import com.koushikdutta.async.future.SimpleFuture
import com.koushikdutta.ion.ContextReference
import com.koushikdutta.ion.IonContext
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.*

/**
 * Created by koush on 7/2/17.
 */

private class WrappingContextReference {

}

fun <T> Activity.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.ActivityContextReference(this) as ContextReference<Object>, block)
}

fun <T> Service.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.ServiceContextReference(this) as ContextReference<Object>, block)
}

fun <T> Fragment.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.FragmentContextReference(this) as ContextReference<Object>, block)
}

fun <T> android.support.v4.app.Fragment.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.SupportFragmentContextReference(this) as ContextReference<Object>, block)
}

class AsyncContext(val ionContext: IonContext, val future: SimpleFuture<Object>): IonContext {
    override fun getContext(): Context? {
        return ionContext.context
    }

    override fun isAlive(): String? {
        return ionContext.isAlive
    }

    suspend fun <T> Future<T>.await(): T {
        return suspendCoroutine { continuation ->
            setCallback { e, result ->
                if (checkLive()) {
                    if (e != null)
                        continuation.resumeWithException(e);
                    else
                        continuation.resume(result);
                }
            }
        }
    }

    suspend fun await(looper: Looper) {
        return await(Handler(looper))
    }

    suspend fun await() {
        return await(Looper.getMainLooper())
    }

    suspend fun await(handler: Handler) {
        return suspendCoroutine { continuation ->
            handler.post {
                if (checkLive())
                    continuation.resume(Unit)
            }
        }
    }

    suspend fun await(asyncServer: AsyncServer) {
        return suspendCoroutine { continuation ->
            asyncServer.post {
                if (checkLive())
                    continuation.resume(Unit)
            }
        }
    }

    suspend fun await(executor: Executor) {
        return suspendCoroutine { continuation ->
            executor.execute {
                if (checkLive())
                    continuation.resume(Unit)
            }
        }
    }

    fun checkLive(): Boolean {
        if (isAlive == null)
            return true
        future.cancelSilently()
        return false
    }
}

private class StandaloneCoroutine<T>(val asyncContext: AsyncContext, val future: SimpleFuture<T>, override val context: CoroutineContext): Continuation<T> {
    override fun resume(value: T) {
        if (asyncContext.isAlive() == null)
            future.setComplete(value)
        else
            future.cancelSilently()
    }

    override fun resumeWithException(exception: Throwable) {
        if (asyncContext.isAlive() == null) {
            if (exception is Exception)
                future.setComplete(exception)
            else
                future.setComplete(Exception(exception))
        }
        else {
            future.cancelSilently()
        }
    }
}

internal fun <T> async(ionContext: IonContext, block: suspend AsyncContext.() -> T): Future<T> {
    val future = SimpleFuture<T>()
    val asyncContext = AsyncContext(ionContext, future as SimpleFuture<Object>)
    val coroutineContext = StandaloneCoroutine<T>(asyncContext, future, EmptyCoroutineContext)
    block.startCoroutine(asyncContext, coroutineContext)
    return future
}
