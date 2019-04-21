package com.gumbocoin.server

import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoClients
import org.bson.Document
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono

private object Connection{
    private val client by kindaLazy { MongoClients.create("mongodb://192.168.1.203") }
    private val database by kindaLazy { client.getDatabase("gumbo") }

    val users by kindaLazy { database.getCollection("users") }
}

fun main() {
   Connection.users
       .deleteMany(Filters.exists("number"))
       .toMono()
       .block()
       .let { println(it) }
}

private fun mapDoc(vararg map :Pair<String,Any>):Document{
    val doc = Document()
    map.forEach {
        doc.append(it.first,it.second)
    }
    return doc
}