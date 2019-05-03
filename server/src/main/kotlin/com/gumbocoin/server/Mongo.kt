package com.gumbocoin.server

import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.Document
import reactor.core.publisher.toMono

object Mongo {
    private val client by kindaLazy { MongoClients.create("mongodb://192.168.1.203") }
    private val database by kindaLazy { client.getDatabase("gumbo") }

//    private val users: MongoCollection<Document> by kindaLazy { database.getCollection("users") }
    val blockchain: MongoCollection<Document> by kindaLazy { database.getCollection("users") }

}
//
//fun main() {
//
//    Mongo.users
//        .deleteMany(Filters.exists("number"))
//        .toMono()
//        .block()
////       .let { println(it) }
//}
//
//private fun mapDoc(vararg map: Pair<String, Any>): Document {
//    val doc = Document()
//    map.forEach {
//        doc.append(it.first, it.second)
//    }
//    return doc
//}