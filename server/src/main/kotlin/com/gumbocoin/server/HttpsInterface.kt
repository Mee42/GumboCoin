package com.gumbocoin.server


import reactor.util.function.Tuples
import spark.Spark.*
import systems.carson.base.*

interface Killable {
    fun kill()
}

private fun prepForHtml(s: String): String = "<pre>" +
        s.replace("\n", "<br />") + "</pre>"

private fun deserializeForHtml(s: Any): String = prepForHtml(prettyPrint(s))

data class Error(val error: String)

private fun error(s: String): String {
    return deserializeForHtml(Error(s))
}


fun startHttps(): Killable {
    port(8080)
    get("/") { _, _ ->
        prepForHtml(
            """
            <a href="/hello">hello</a>
            <a href="/blockchain">blockchain</a>
            <a href="/block">block</a>
            <a href="/blocks">blocks</a>
            <a href="/users">users</a>
            <a href="/actions">actions</a>
            <a href="/transactions">transactions</a>
            <a href="/data">data</a>
        """.trimIndent()
        )
    }
    get("/hello") { _, _ -> "Hello, World!" }
    get("/blockchain") { _, _ -> deserializeForHtml(blockchain) }
    get("/block/:number") { req, _ ->
        val i = req.params("number").toIntOrNull() ?: return@get error("Cant parse int value")
        if (blockchain.blocks.size <= i)
            return@get error("No block with that index")
        return@get deserializeForHtml(blockchain.blocks[i])
    }
    get("/block") { _, _ ->
        blockchain.blocks.mapIndexed { index, block -> Pair(index, block) }
            .map { Tuples.of(it.first, it.second, (it.first.toString() + "<a/>:").padEnd("<a/>:".length + 4, ' ')) }
            .fold("") { a, b -> "$a\n<a href=\"/block/${b.t1}\">${b.t3} ${b.t2.hash}" }
            .trim()
            .let { prepForHtml(it) }
    }
    get("/blocks") { _, _ -> deserializeForHtml(blockchain.blocks.size) }
    get("/users") { _, _ ->

        deserializeForHtml(blockchain.users.map {
            SerializedUser(it.id, it.person.publicKeyBase64(),
                blockchain.blocks.flatMap { w -> w.actions }
                    .filter { w -> w.type == ActionType.DATA }
                    .map { w -> w as DataAction }
                    .filter { w -> w.clientID == it.id })
        })
    }
    get("/user/:id") req@{ req, _ ->

        val id = req.params("id")
        val user = blockchain.users.firstOrNull { it.id == id } ?: return@req error("User not found")
        deserializeForHtml(SerializedUser(id, user.person.publicKeyBase64(),
            blockchain.blocks.flatMap { w -> w.actions }
                .filter { w -> w.type == ActionType.DATA }
                .map { w -> w as DataAction }
                .filter { w -> w.clientID == id })
        )
    }
    get("/actions") { _, _ ->
        deserializeForHtml(blockchain.blocks
            .flatMap { it.actions })
    }
    get("/transactions") { _, _ ->
        deserializeForHtml(blockchain.blocks
            .flatMap { it.actions }
            .filter { it.type == ActionType.TRANSACTION })
    }
    get("/data") { _, _ ->
        deserializeForHtml(blockchain.blocks
            .flatMap { it.actions }
            .filter { it.type == ActionType.DATA }
            .map { it as DataAction }
            .map { dataAction -> SerializedDataAction(
                clientID = dataAction.clientID,
                data = dataAction.data,
                signature = dataAction.signature,
                signedBy = blockchain.blocks
                    .flatMap { it.actions }
                    .filter { it.type == ActionType.VERIFY }
                    .map { it as VerifyAction }
                    .filter { it.dataID == dataAction.data.uniqueID }
                    .map { it.clientID }
            ) })
    }


    notFound(error("404 - page not found"))

    return object : Killable {
        override fun kill() {}
    }

}
private data class SerializedDataAction(
    val clientID :String,
    val data : DataPair,
    val signature: String,
    val signedBy :List<String>)

private data class SerializedUser(
    val clientID: String,
    val publicKey: String,
    val data: List<DataAction>
)