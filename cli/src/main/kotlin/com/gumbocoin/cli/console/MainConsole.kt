package com.gumbocoin.cli.console

import com.gumbocoin.cli.*
import com.gumbocoin.cli.console
import com.gumbocoin.cli.filteredRunner
import com.gumbocoin.cli.runner
import com.gumbocoin.cli.switchy
import systems.carson.base.*
import java.time.Duration
import java.time.Instant
import kotlin.system.exitProcess

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
            val response = try { context
                .socket
                .requestResponse(RequestDataBlob(Request.Response.PING.intent, "defaultID"))
                .block(Duration.ofSeconds(10)) }catch(e :IllegalStateException) { e.printStackTrace();null }

            val end = Instant.now()
            val dur = Duration.between(start, end).abs().toMillis()
            if (response != null) {
                println("Pong! Duration:$dur")
            } else {
                println("Didn't get a response from the server")
            }
        }
    }
    action {
        name = "money"
        desc = "check balance"
        runner = runner { context ->
            val loggedIn = context.isLoggedIn
            val clientID = if(loggedIn) context.credentials.clientID else ""
            val str = if(loggedIn) "($clientID)" else ""
            var id = promptForString("ID $str") { it.isNotBlank().or(loggedIn).toError("No default if not logged in") }
            if(id.isBlank() && loggedIn){
                id = context.credentials.clientID
            }

            val money = context.socket.requestResponse(
                StringDataBlob(
                    clientID = if(loggedIn) context.credentials.clientID else "defaultID",
                    intent = Request.Response.MONEY.intent,
                    value = id
                ), if(loggedIn) context.credentials.keys else Person.default
            )
                .mapFromJson<SendableInt>()
                .block()?.value ?: -1
            println("$id has $money Gumbocoin" + if (money == 1) "" else "s")
        }

    }
    action {
        name = "trail"
        desc = "get list of blocks mined"
        runner = runner {
            if(it.threadedMiner == null){
                println("not running a miner - are you logged in and have started the miner?")
                return@runner
            }
            it.threadedMiner!!.toFlux()
                .map { (block,status) ->
                    (if(status.failed) "failed: " else "block: ") + block.hash + " " + block.difficulty
                }.map { t -> println(t) }
                .blockLast()
        }
    }
    action {
        name = "stop"
        desc = "quits the program"
        runner = runner {
            exitProcess(0)
        }
    }
    action {
        name = "signup"
        desc = "create a gumbocoin account"
        runner = signup
    }
    action {
        name = "login"
        desc = "login in to gumbocoin"
        runner = login
    }
    action {
        name = "start-miner"
        desc = "starts the gumbocoin miner"
        runner = filteredRunner {
            conditional("You need to be logged in") { it.isLoggedIn }
            runnerr {
                if (it.threadedMiner == null) {
                    it.threadedMiner = ThreadedMiner(it)
                }
                it.threadedMiner?.start()
            }
        }
    }
    action {
        name = "stop-miner"
        desc = "stops the gumbocoin miner"
        runner = filteredRunner {
            conditional("Miner has not been started") { it.threadedMiner != null }
            conditional("Miner is not currently running") { it.threadedMiner?.isRunning ?: false }
            runnerr { it.threadedMiner!!.stop() }
        }
    }
    action {
        name = "diff"
        desc = "get the current mining difficulty"
        runner = runner { context ->
            context.socket.requestResponse(
                RequestDataBlob(
                    intent = Request.Response.DIFF.intent,
                    clientID = "defaultID"
                ),
                keys = Person.default
            )
                .map { Sendable.fromJson<SendableInt>(it) }
                .block()
                .let { println(it?.value ?: "No response from the server") }
        }
    }
    action {
        name = "status"
        desc = "get the current console status"
        runner = switchy {
            conditional = { it.isLoggedIn }
            truthy = runner {
                """
                    miner status: ${if (it.threadedMiner?.isRunning == true) "running" else "not running"}
                    blocks mined: ${it.threadedMiner?.mined ?: -1}
                """.trimIndent().let { w -> println(w) }
            }
            falsy = runner {
                """
                    Not logged in
                """.trimIndent().let { w -> println(w) }
            }
        }
    }
    action {
        name = "data"
        desc = "submit some data to the blockchain"
        runner = data
    }
    action {
        name = "verify"
        desc = "verify someone else's data"
        runner = verify
    }
}




