package com.gumbocoin.cli.new

import systems.carson.base.Request
import systems.carson.base.RequestDataBlob
import java.time.Duration
import java.time.Instant

/**
 * This is stuff that has to deal with logging in, logging out, an
 * */
val mainConsole = console {
    prompt = "$"
    action {
        name = "ping"
        desc = "pings the server"
        runner = runner { context ->
            val start = Instant.now()
            val response = context.socket.requestResponse(RequestDataBlob(Request.Response.PING.intent, "defaultID")).block()
            val end = Instant.now()
            val dur = Duration.between(start, end).abs().toMillis()
            if (response != null) {
                println("Pong! Duration:$dur")
            }else {
                println("Didn't get a response from the server")
            }
        }
    }
    action {
        name = "test"
        desc = "test stuff"
        runner = filteredRunner {
            yes("Do you want to continue?")
            yes("Are you sure?")
            yes("hello")
            final = runner {
                println("did final")
            }
        }
    }
}