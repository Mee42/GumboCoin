package com.gumbocoin.server

import com.mongodb.client.model.Filters
import reactor.core.publisher.toMono
import spark.Spark.*
import systems.carson.base.Person
import systems.carson.base.Signature
import systems.carson.base.serialize
import java.nio.charset.Charset


fun initServerHttpsInterface(){

    path("/api/${inputArguments.release.str}"){
        get("/ping"){_,_ -> "Pong!" }

        get("/blockchain"){_,_ -> serialize(blockchain) }

        post("/keyfile"){ req,_ ->
            val id = req.queryMap("id").value()
            //val id = req.params("id")
            val user = Mongo.users
                .find(Filters.all("clientID",id))
                .first()
                .toMono()
                .map { deserialize<ServerUser>(it) }
                .blockOptional()
            if(!user.isPresent){
                hait(403)
            }
            val u = user.get()
            val password = req.bodyAsBytes()
            val newHash = passwordHash(password.toString(Charset.forName("UTF-8")),u.salt)
            if(newHash == u.hash)
                return@post u.keyfile
            else
                hait(403)
        }

        post("/verify/:string"){req,_ ->
            //test to see if the account is valid
            val id = req.queryMap("id").value()
            val string = req.params("string")
            val sig = Signature.fromBase64(req.body())
            val person = blockchain.users.firstOrNull { it.id == id }?.person ?: halt(403)
            if(Person.verify(
                    person = person,
                    signature = sig,
                    data = string.toByteArray(Charset.forName("UTF-8")))){
                hait(404)
            }else{
                hait(403)
            }
        }


    }
}


fun hait(code :Int){ halt<Nothing>(code) }

fun <R> halt(code :Int):R{
    spark.Spark.halt(code)
    error("Didn't halt correctly - calling from correct context?")
}