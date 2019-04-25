package com.gumbocoin.server



import reactor.util.function.Tuples
import spark.Spark.*
import systems.carson.base.ActionType
import systems.carson.base.TransactionAction
import systems.carson.base.TransactionDataBlob
import systems.carson.base.prettyPrint

interface Killable {
    fun kill()
}

private fun prepForHtml(s :String):String = "<pre>" +
        s.replace("\n","<br />") + "</pre>"
private fun deserializeForHtml(s :Any):String = prepForHtml(prettyPrint(s))

data class Error(val error :String)

private fun error(s :String):String{
    return deserializeForHtml(Error(s))
}


fun startHttps(): Killable {
    port(8080)
    get("/") {_,_ ->
        prepForHtml("""
            <a href="/hello">hello</a>
            <a href="/blockchain">blockchain</a>
            <a href="/block">block</a>
            <a href="/blocks">blocks</a>
            <a href="/users">users</a>
            <a href="/actions">actions</a>
            <a href="/transactions">transactions</a>
            <a href="/data">data</a>
        """.trimIndent())
    }
    get("/hello") {_,_ -> "Hello, World!" }
    get("/blockchain") {_,_ -> deserializeForHtml(blockchain) }
    get("/block/:number") {req,_ ->
        val i = req.params("number").toIntOrNull() ?: return@get error("Cant parse int value")
        if(blockchain.blocks.size <= i)
            return@get error("No block with that index")
        return@get deserializeForHtml(blockchain.blocks[i])
    }
    get("/block"){_,_ ->
        blockchain.blocks.mapIndexed { index, block -> Pair(index,block) }
            .map { Tuples.of(it.first,it.second,(it.first.toString()+ "<a/>:").padEnd("<a/>:".length + 4,' '))}
            .fold("") {a,b -> "$a\n<a href=\"/block/${b.t1}\">${b.t3} ${b.t2.hash}"}
            .trim()
            .let { prepForHtml(it) }
    }
    get("/blocks"){_,_-> deserializeForHtml(blockchain.blocks.size) }
    get("/users"){_,_ ->
        deserializeForHtml(blockchain.users)
        TODO("fix this")
    }
    get("/actions") {_,_ ->
        deserializeForHtml(blockchain.blocks
            .flatMap { it.actions })
    }
    get("/transactions"){_,_ ->
        deserializeForHtml(blockchain.blocks
            .flatMap { it.actions }
            .filter { it.type == ActionType.TRANSACTION })
    }
    get("/data"){_,_ ->
        deserializeForHtml(blockchain.blocks
            .flatMap { it.actions }
            .filter { it.type == ActionType.DATA })
    }


    notFound("<html><body>${error("404 - page not found")}</body></html>")

    return object : Killable {
        override fun kill() {}
    }

}

