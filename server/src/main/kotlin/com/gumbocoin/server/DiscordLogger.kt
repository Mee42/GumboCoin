package com.gumbocoin.server

import systems.carson.base.GLevel
import systems.carson.base.GLog
import systems.carson.base.Information

class DiscordLogger :GLog{
    fun setLevel(level :GLevel){
        levelI = level
    }
    override val level: GLevel
        get() = levelI
    private var levelI = GLevel.WARNING

    override fun log(information: Information) {
        val name = information.nameOfLogger
        val message = information.level.toString() + "" +
                (if(name == null) ":" else " - $name:") +
                information.message

        DiscordManager
            .logChannel
            .flatMap { it.createMessage(message) }
            .subscribe()
    }
}