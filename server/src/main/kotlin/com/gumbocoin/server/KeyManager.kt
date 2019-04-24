package com.gumbocoin.server

import systems.carson.base.Person
import java.io.File
import java.nio.charset.Charset

enum class Release(val str :String){
    BETA("beta")
}

object KeyManager {
    private val rootDirectory by lazy { System.getenv("KEY_HOME") }
    private val release by lazy { Release.valueOf(System.getenv("RELEASE").toUpperCase()) }
    private val keyDirectory by lazy {
        if(rootDirectory.endsWith("/"))
            rootDirectory + release.str + "/"
        else
            rootDirectory + "/" + release.str
    }


    private fun get(s :String):Person{
        val person = File("$keyDirectory/$s")
        if(!person.exists())
            error("Can't find server key")
        val text = person.readText(Charset.forName("UTF-8"))
        return Person.fromKeyFile(text)
    }


    val server :Person by lazy { get("server.gc.key") }

}