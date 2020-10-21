package com.koushikdutta.ion.test

import android.graphics.Bitmap
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.bitmap.LocallyCachedStatus
import com.koushikdutta.ion.bitmap.Transform
import org.junit.Test
import java.lang.ClassCastException
import kotlin.test.assertEquals

class BitmapTests : AsyncTests(1000000L) {
    val EXIF_ASSET = "file:///android_asset_observe/exif.jpg"

    @Test
    fun testAssetWithExifRotation() = testAsync {
        val notCached = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .isLocallyCached
        assertEquals(notCached, LocallyCachedStatus.NOT_CACHED)

        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .asBitmap()
                .await()

        assertEquals(result.width, 480)
        assertEquals(result.height, 640)

        val cached = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .isLocallyCached

        assertEquals(cached, LocallyCachedStatus.CACHED)
    }


    @Test
    fun testCacheBehavior() = testAsync {
        ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .asBitmap()
                .await()

        ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .asBitmap()
                .await()

        assertEquals(assetObserver.loadObserved, 1)
    }

    @Test
    fun testSimultaneousCacheBehavior() = testAsync {
        var transformed = 0
        val transform = object : Transform {
            override fun transform(b: Bitmap): Bitmap {
                transformed++
                return Bitmap.createScaledBitmap(b, 100, 100, true)
            }

            override fun key(): String {
                return "scale"
            }
        }

        val b1 = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .withBitmap()
                .transform(transform)
                .asBitmap()

        val b2 = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .withBitmap()
                .transform(transform)
                .asBitmap()

        b1.await()
        b2.await()
        assertEquals(assetObserver.loadObserved, 1)
        assertEquals(transformed, 1)
    }


    @Test
    fun testShrink() = testAsync {
        val notCached = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .isLocallyCached
        assertEquals(notCached, LocallyCachedStatus.NOT_CACHED)
        
        val transform = object : Transform {
            override fun transform(b: Bitmap): Bitmap {
                return Bitmap.createScaledBitmap(b, 100, 100, true)
            }

            override fun key(): String {
                return "scale"
            }
        }

        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .withBitmap()
                .transform(transform)
                .asBitmap()
                .await()

        assertEquals(result.width, 100)
        assertEquals(result.height, 100)

        val sourceSoftCached = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .isLocallyCached

        // should be soft cached
        assertEquals(sourceSoftCached, LocallyCachedStatus.CACHED)

        val targetCached = ion.build(InstrumentationRegistry.getInstrumentation().context)
        .load(EXIF_ASSET)
        .withBitmap()
        .transform(transform)
        .isLocallyCached

        assertEquals(targetCached, LocallyCachedStatus.CACHED)

        assertEquals(assetObserver.loadObserved, 1)
    }

    @Test
    fun testBitmapFastPath() = testAsync {
        val notCached = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .isLocallyCached
        assertEquals(notCached, LocallyCachedStatus.NOT_CACHED)

        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .setHeader("X-Allow-Bitmap", "true")
                .asBitmap()
                .await()

        assertEquals(result.width, 480)
        assertEquals(result.height, 640)

        val cached = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .isLocallyCached

        assertEquals(cached, LocallyCachedStatus.CACHED)

        assertEquals(assetObserver.loadObserved, 0)
        assertEquals(assetObserver.loadBitmapObserved, 1)
    }

    @Test
    fun testPackageIcon() = testAsync {
        val context = InstrumentationRegistry.getInstrumentation().context
        try {
            ion.build(context)
                    .load("package://${context.packageName}")
                    .asBitmap()
                    .await()
        }
        catch (ignored: ClassCastException) {
            // a class cast exception is fine. new android icons are AdaptiveIconDrawables and not a BitmapDrawable
            println("ignoring")
        }
    }
}