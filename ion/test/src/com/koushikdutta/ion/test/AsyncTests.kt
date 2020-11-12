package com.koushikdutta.ion.test

import androidx.test.platform.app.InstrumentationRegistry
import com.koushikdutta.ion.Ion
import com.koushikdutta.scratch.Promise
import com.koushikdutta.scratch.event.post
import java.lang.IllegalStateException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

val DEBUG_MODE = System.getProperty("java.vm.info", "").contains("sharing")

var ionNumbers = 0
class IonTestScope {
    val ionId = synchronized(IonTestScope::class.java) {
        ionNumbers++
    }
    val ion = Ion.getInstance(InstrumentationRegistry.getInstrumentation().context, "ion-$ionId")
    val assetObserver = AssetObserverLoader()

    init {
        ion.configure().loaders.add(0, TestLoader())
        ion.configure().loaders.add(0, assetObserver)

        ion.bitmapCache.clear()
    }
}

open class AsyncTests(val timeout: Long = 10000L) {

    // android tests run on a non-ui test thread.
    open fun testAsync(expectTimeout: Boolean = false, expectIncomplete: Boolean = false, block: suspend IonTestScope.() -> Unit) {
        val semaphore = Semaphore(0)
        val scope = IonTestScope()
        val promise = Promise {
            scope.ion.loop.await()
            block(scope)
        }

        promise
        .finally {
            scope.ion.loop.post()
            semaphore.release()
        }

        val actualTimeout = if (DEBUG_MODE) Long.MAX_VALUE else timeout

        val timeout = !semaphore.tryAcquire(actualTimeout, TimeUnit.MILLISECONDS)
        scope.ion.loop.stop()
        if (!expectTimeout && timeout)
            throw Exception("test timed out")
        try {
            promise.getOrThrow()
        }
        catch (throwable: IllegalStateException) {
            if (!expectIncomplete)
                throw throwable
        }
    }
}