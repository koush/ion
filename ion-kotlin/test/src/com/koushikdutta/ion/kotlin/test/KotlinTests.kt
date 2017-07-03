package com.koushikdutta.ion.kotlin.test

import android.os.Looper
import android.test.AndroidTestCase
import com.koushikdutta.async.ByteBufferList
import com.koushikdutta.async.future.Future
import com.koushikdutta.ion.ContextReference
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.kotlin.AsyncContext
import com.koushikdutta.ion.kotlin.async
import junit.framework.Assert
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Created by koush on 7/2/17.
 */

class Md5 private constructor() {
    private var digest: MessageDigest? = null
    fun update(bb: ByteBufferList) {
        while (bb.size() > 0) {
            val b = bb.remove()
            digest!!.update(b)
        }
    }

    fun digest(): String {
        val hash = BigInteger(digest!!.digest()).toString(16)
        return hash
    }

    companion object {
        @Throws(NoSuchAlgorithmException::class)
        fun createInstance(): Md5 {
            val md5 = Md5()
            md5.digest = MessageDigest.getInstance("MD5")
            return md5
        }
    }
}

fun <T> AndroidTestCase.async(block: suspend AsyncContext.() -> T): Future<T> {
    return async(ContextReference.fromContext(context), block)
}

private val executor = Executors.newSingleThreadScheduledExecutor {
    Thread(it, "scheduler").apply { isDaemon = true }
}

suspend fun delay(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Unit = suspendCoroutine { cont ->
    executor.schedule({ cont.resume(Unit) }, time, unit)
}

class KotlinTests: AndroidTestCase {
    constructor()

    fun testDelayString() {
        val future = async {
            delay(3000)
            "done!"
        }

        val ret = future.get()

        Assert.assertEquals(ret, "done!")
    }

    // this testdata file was generated using /dev/random. filename is also the md5 of the file.
    internal val dataNameAndHash = "6691924d7d24237d3b3679310157d640"
    internal val githubPath = "raw.githubusercontent.com/koush/AndroidAsync/master/AndroidAsync/test/assets/"
    internal val github = "https://" + githubPath + dataNameAndHash
    @Throws(Exception::class)
    fun testGithubRandomData() {
        System.out.println("Alice")

        val ret = async {
            System.out.println("Bob")
            // get the bytes, this happens in Ion's reactor thread
            val bytes = Ion.with(context)
                    .load(github)
                    .asByteArray()
                    .await()

            // Ion automatically returns you to the main thread.
            // But we don't want to block main thread calculating md5, so use a background thread.
            Assert.assertSame(Thread.currentThread(), Looper.getMainLooper().thread)
            executor.await()

            Assert.assertNotSame(Thread.currentThread(), Looper.getMainLooper().thread)
            Assert.assertNotSame(Thread.currentThread(), Ion.getDefault(context).server.affinity)

            val md5 = Md5.createInstance()
            md5.update(ByteBufferList(bytes))

            // to get back onto the main thread to modify UI, etc, call a bare await
            await()
            Assert.assertSame(Thread.currentThread(), Looper.getMainLooper().thread)

            System.out.println("Chuck")
            return@async md5.digest()
        }

        System.out.println("David")

        val digest = ret.get()
        Assert.assertEquals(digest, dataNameAndHash)
    }

    fun myStringFunction(url: String) = async {
        try {
            return@async Ion.with(context)
                    .load(url)
                    .asString()
                    .await()
        }
        catch (e: Exception) {
            return@async "Failed to load"
        }
    }
}