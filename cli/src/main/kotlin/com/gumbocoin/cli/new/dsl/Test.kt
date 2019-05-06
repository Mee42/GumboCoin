package com.gumbocoin.cli.new.dsl

import com.gumbocoin.cli.new.console
import com.gumbocoin.cli.new.filteredRunner
import com.gumbocoin.cli.new.runner


val miningConsle = console {
        prompt = ">>>>>"
    action {
        name = "start"
        desc = "start the miner"
        aliases = listOf("startt")

        runner = filteredRunner {
            yes("Do you want to start the miner")
            conditional("You need to be logged in") { context -> context.isLoggedIn }
            runnerr {
                println("start")
            }
        }
    }
}