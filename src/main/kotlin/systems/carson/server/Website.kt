package systems.carson.server

import spark.Spark.*
import systems.carson.shared.pretty

fun startWebsite(){
    port(7001)
    get("/blockchain") {_,_ ->
       p(pretty.toJson(blockchain))
    }
    get("/users") {_,_ ->
        p(pretty.toJson(blockchain.users()))
    }
    get("/transactions") {_,_ ->
        p(pretty.toJson(blockchain.transactions))
    }
}

private fun p(s :String):String = "<pre>" + s.replace("\n","<br />") + "</pre>"