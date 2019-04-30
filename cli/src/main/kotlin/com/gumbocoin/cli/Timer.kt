package com.gumbocoin.cli

import systems.carson.base.GLogger
import systems.carson.base.logger
import java.time.Duration

class Timer {
    private var start: Long = -1
    private var end: Long = -1
    fun start() {
        start = System.nanoTime()
    }

    fun end() {
        end = System.nanoTime()
    }

    fun elapsed(): Duration {
        return Duration.ofNanos(end - start)
    }

    override fun toString(): String {
        val t = elapsed()
        val hours = t.toHours()
        val minutes = t.toMinutes() % Duration.ofHours(1).toMinutes()
        val seconds = t.seconds % Duration.ofMinutes(1).seconds
        val ms = t.toMillis() % Duration.ofSeconds(1).toMillis()
        var s = ""
        if (hours != 0L) s += "" + hours + "h "
        if (minutes != 0L) s += "" + minutes + "m "
        if (seconds != 0L) s += "" + seconds + "s "
        if (ms != 0L) s += "" + ms + "ms "
        if (s == "")
            s = "No time elapsed"
        return s.trim()
    }

}


fun <T> time(print: String, closure: () -> T): T {
    val logger = GLogger.logger("Time")
    val timer = Timer()
    timer.start()
    val t = closure()
    timer.end()
    logger.info("$print: $timer")
    return t
}
