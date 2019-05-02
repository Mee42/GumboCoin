package com.gumbocoin.cli

import java.util.*
import java.util.concurrent.*


object TimeLimitedCodeBlock {

    fun <T> runWithTimeout(runnable :() -> T,timeout :Long, timeUnit: TimeUnit): Optional<T> {
        return runWithTimeout(Callable { runnable() },timeout,timeUnit)
    }

    fun <T> runWithTimeout(callable: Callable<T>, timeout: Long, timeUnit: TimeUnit): Optional<T> {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit(callable)
        executor.shutdown() // This does not cancel the already-scheduled task.
        return try {
            Optional.of(future.get(timeout, timeUnit))
        } catch (e: TimeoutException) {
            //remove this if you do not want to cancel the job in progress
            //or set the argument to 'false' if you do not want to interrupt the thread
            future.cancel(true)
            Optional.empty()
        } catch (e: Exception) {
            //unwrap the root cause
//            when (val t = e.cause) {
//                is Error -> throw t
//                is Exception -> throw t
//                else -> throw IllegalStateException(t)
//            }
            //just like....ignore it
            future.cancel(true)
            Optional.empty()
        }

    }

}