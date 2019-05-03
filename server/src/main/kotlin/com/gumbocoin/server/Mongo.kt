package com.gumbocoin.server

import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.Document
import reactor.core.publisher.toMono
import systems.carson.base.ReleaseManager

object Mongo {
//    private val client by kindaLazy { MongoClients.create("mongodb://192.168.1.203") }
    private val client by lazy { MongoClients.create("mongodb://192.168.1.203") }
    private val database by lazy {
        val str = "gumbo-${ReleaseManager.release.str.toLowerCase()}"
        println("Using database \"$str\"")
        client.getDatabase(str)
    }

//    private val users: MongoCollection<Document> by kindaLazy { database.getCollection("users") }
    val blockchain: MongoCollection<Document> by kindaLazy { database.getCollection("blockchain") }

}