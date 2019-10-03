package com.koushikdutta.ion.kotlin.test

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


/**
 * Created by koush on 7/2/17.
 */

class KotlinTests(override val coroutineContext: CoroutineContext = EmptyCoroutineContext) : CoroutineScope {
    suspend fun testIon() {

    }
}