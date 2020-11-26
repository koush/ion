package com.koushikdutta.ion.kotlin.test

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.koushikdutta.async.kotlin.await
import com.koushikdutta.ion.Ion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Semaphore
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext

    @Test
    fun testIon() {
        val semaphore = Semaphore(0)
        async {
            val ret = Ion.with(InstrumentationRegistry.getInstrumentation().targetContext)
                    .load("https://google.com/robots.txt")
                    .asString()
                    .await()
            println(ret)
            semaphore.release()
        }
        semaphore.acquire()
    }
}
