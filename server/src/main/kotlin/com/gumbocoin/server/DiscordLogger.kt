package com.gumbocoin.server

import reactor.core.publisher.Flux
import systems.carson.base.GLevel
import systems.carson.base.GLog
import systems.carson.base.Information
import java.time.Duration
import java.util.*

class DiscordLogger : GLog {
    fun setLevel(level: GLevel) {
        levelI = level
    }

    override val level: GLevel
        get() = levelI
    private var levelI = GLevel.WARNING

    private val cache = mutableListOf<String>()

    private val lock = Any()

    override fun log(information: Information) {
        val name = information.nameOfLogger
        val message =
            (if (information.level == GLevel.FATAL) "<@293853365891235841> " else "") +
                    information.level.toString() + "" +
                    (if (name == null) ":" else " - $name:") +
                    information.message
//        println("Adding $message to cache")
        synchronized(lock) {
            cache.add(message)
        }
    }

    init {
        Flux.interval(Duration.ofSeconds(1))
            .map {
                synchronized(lock) {
                    //                    println("Mapping $cache")
                    if (cache.isEmpty()) {
                        Optional.empty()
                    } else {
                        var str = ""
                        while (cache.isNotEmpty()) {
                            str += cache.removeAt(0) + "\n"
                            if (str.length > 1500 || (cache.isNotEmpty() && cache[0].length + str.length >= 1900))
                                break
                        }
                        Optional.of(str)
                    }
                }
            }
//            .map { println("Got ${it.isPresent}");it }
            .filter { it.isPresent }
            .map { it.get() }
//            .map { println("Sending: $it");it }
            .flatMap { message -> DiscordManager.logChannel.flatMap { channel -> channel.createMessage(message) } }
            .subscribe()
    }

}