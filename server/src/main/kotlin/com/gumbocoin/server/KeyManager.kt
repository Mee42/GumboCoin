package com.gumbocoin.server

import systems.carson.base.Person
import java.io.File
import java.nio.charset.Charset

enum class Release(val str :String){
    MASTER("master"),
    BETA("beta"),
    DEV("dev")
}

object KeyManager {
    private val rootDirectory by lazy { System.getenv("KEY_HOME") }
    val release by lazy { Release.valueOf(System.getenv("RELEASE").toUpperCase()) }
    private val keyDirectory by lazy {
        if(rootDirectory.endsWith("/"))
            rootDirectory + release.str + "/"
        else
            rootDirectory + "/" + release.str
    }


    private fun get(s :String):Person{
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