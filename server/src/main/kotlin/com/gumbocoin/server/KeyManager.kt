package com.gumbocoin.server

import systems.carson.base.Person
import systems.carson.base.ReleaseManager.release
import java.io.File
import java.nio.charset.Charset


object KeyManager {
    private val rootDirectory by lazy { System.getenv("KEY_HOME") }
    private val keyDirectory by lazy {
        if(rootDirectory.endsWith("/"))
            rootDirectory + release.str + "/"
        else
            rootDirectory + "/" + release.str
    }


    private fun get(@Suppress("SameParameterValue") s :String):Person{
        val keyFile = File("$keyDirectory/$s")
        if(!keyFile.exists())
            error("Can't find server key")
        val text = keyFile.readText(Charset.forName("UTF-8"))
        return Person.fromKeyFile(text)
    }


    val server :Person by lazy { get("server.gc.key") }

    val discord :String by lazy {
        val keyFile = File("$keyDirectory/discord.key")
        if(!keyFile.exists())
            error("Can't find discord key")
        keyFile.readText(Charset.forName("UTF-8")).trim()
    }
}