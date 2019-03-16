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
import kotlin.coroutines.*

/**
 * Created by koush on 7/2/17.
 */

fun <T> Activity.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.ActivityContextReference(this), block)
}

fun <T> Service.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.ServiceContextReference(this), block)
}

fun <T> Fragment.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.FragmentContextReference(this), block)
}

fun <T> android.support.v4.app.Fragment.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.SupportFragmentContextReference(this), block)
}

class AsyncContext(private val ionContext: IonContext, private val future: SimpleFuture<*>) : IonContext {
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

    suspend fun Looper.await() {
        return Handler(this).await()
    }

    suspend fun await() {
        return Looper.getMainLooper().await()
    }

    suspend fun Handler.await() {
        return suspendCoroutine { continuation ->
            post {
                if (checkLive())
                    continuation.resume(Unit)
            }
        }
    }

    suspend fun AsyncServer.await() {
        return suspendCoroutine { continuation ->
            post {
                if (checkLive())
                    continuation.resume(Unit)
            }
        }
    }

    suspend fun Executor.await() {
        return suspendCoroutine { continuation ->
            execute {
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

private class StandaloneCoroutine<T>(val asyncContext: AsyncContext, val future: SimpleFuture<T>, override val context: CoroutineContext) : Continuation<T> {
    override fun resumeWith(result: Result<T>) {
        if (asyncContext.isAlive == null) {
            if (result.isSuccess) {
                future.setComplete(result.getOrNull())
            }
            else {
                val exception = result.exceptionOrNull();
                if (exception is Exception)
                    future.setComplete(exception)
                else
                    future.setComplete(Exception(exception))
            }
        }
        else {
            future.cancelSilently()
        }
    }
}

fun <T> async(ionContext: IonContext, block: suspend AsyncContext.() -> T): Future<T> {
    val future = SimpleFuture<T>()
    val asyncContext = AsyncContext(ionContext, future)
    val coroutineContext = StandaloneCoroutine(asyncContext, future, EmptyCoroutineContext)
    block.startCoroutine(asyncContext, coroutineContext)
    return future
}
