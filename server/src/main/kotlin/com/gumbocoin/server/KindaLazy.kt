package com.gumbocoin.server


import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.reflect.KProperty

private val pool = Executors.newCachedThreadPool()

class KindaLazy<T>(initializer: () -> T) {

    private var value: T? = null
    private val thread: Future<*>

    init {
        thread = pool.submit { value = initializer() }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (value == null)
            thread.get()
        return value!!
    }
}

fun <T> kindaLazy(initializer: () -> T) = KindaLazy(initializer)
