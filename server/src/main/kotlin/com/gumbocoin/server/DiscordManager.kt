package com.gumbocoin.server

import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.TextChannel
import discord4j.core.`object`.util.Snowflake
import reactor.core.publisher.Mono
import systems.carson.base.Release.*
import systems.carson.base.ReleaseManager.release

object DiscordManager {
    val client: DiscordClient =
        DiscordClientBuilder(KeyManager.discord).build()


    val logChannel: Mono<TextChannel> by lazy {
        client.getChannelById(
            Snowflake.of(
                logging[release]
                    ?: error("Can't find logging channel for release $release")
            )
        ).cast(TextChannel::class.java)
    }
    val blockchainChannel: Mono<TextChannel> by lazy {
        client.getChannelById(
            Snowflake.of(
                blockchain[release]
                    ?: error("Can't find blockchain channel for release $release")
            )
        )
            .cast(TextChannel::class.java)
    }

    private val blockchain = mapOf(
        MASTER to 571742880776060948,
        BETA to 571742905706872845,
        DEV to 571742930004606997
    )
    private val logging = mapOf(
        MASTER to 566999855730262036,
        BETA to 567120508186001449,
        DEV to 571753009487544340
    )

}