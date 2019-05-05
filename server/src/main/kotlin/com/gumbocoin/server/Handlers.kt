package com.gumbocoin.server

import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.Success
import io.rsocket.Payload
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import systems.carson.base.*
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import java.util.*


val handlerLogger = GLogger.logger("Handler")

enum class StreamHandler(
    val request: Request.Stream,
    val handler: (RequestDataBlob) -> Flux<Payload>
) {
    NUMBERS(Request.Stream.NUMBERS, req@{ pay ->
        pay as IntDataBlob
        return@req Flux.range(0, pay.value).map { "" + it }.map { it.toPayload() }
    }),
    BLOCKCHAIN_UPDATES(Request.Stream.BLOCKCHAIN_UPDATES, req@{
        //        println("dataCache: ${serialize(dataCache)}")
        val value = ActionUpdate(dataCache, blockchain.blocks.last().hash, diff)
//        println("Value: ${serialize(value)}")
        updateSource.toFlux().startWith(value)
            .map { println("Sending out ${serialize(it)}");it }
            .map { it.toPayload() }
//        Flux.just(value)
//            .map { println("Sending out ${serialize(it)}");it }
//            .map { it.toPayload() }
    })
}

enum class ResponseHandler(
    val request: Request.Response,
    val handler: (RequestDataBlob) -> Payload
) {

    PING(Request.Response.PING, req@{ "pong".toPayload() }),
    DECRYPT(Request.Response.DECRYPT, req@{ pay ->
        pay as EncryptedDataBlob
        val plain = KeyManager.server.decryptAES(pay.data.toBytes())
        "${pay.clientID}:\"${plain.toString(Charset.forName("UTF-8"))}\"".toPayload()
    }),
    SIGN_UP(Request.Response.SIGN_UP, req@{ pay ->
        pay as SignUpDataBlob

        if (blockchain.users.any { it.id == pay.clientID })
            return@req Status(failed = true, errorMessage = "User already exists").toPayload()
        addToDataCache(pay.signUpAction)
        Status().toPayload()
    }),
    VERIFIED(Request.Response.VERIFIED, req@{ pay ->
        return@req SendableBoolean(pay.isVerified).toPayload()
    }),
    BLOCK(Request.Response.BLOCK, req@{ pay ->
        pay as BlockDataBlob
        val block: Block = pay.block
        handlerLogger.debug("got block:$block")

        if (block.difficulty != diff) {
            return@req Status(
                failed = true,
                errorMessage = "Difficulty is wrong",
                extraData = "Expected $diff, got ${block.difficulty}"
            ).toPayload()
        }

        if (!block.hash.isValid())
            return@req Status(
                failed = true,
                errorMessage = "Block hash is wrong",
                extraData = "Hash: ${block.hash}"
            ).toPayload()
        if (dataCache != pay.block.actions) {
            return@req Status(
                failed = true,
                errorMessage = "Actions are invalid",
                extraData = "Expected :$dataCache, got ${pay.block.actions}"
            ).toPayload()
        }
        if (block.lasthash != blockchain.blocks.last().hash)
            return@req Status(
                failed = true,
                errorMessage = "lasthash is not correct",
                extraData = "expected: ${blockchain.blocks.last().hash} and got ${block.lasthash}"
            ).toPayload()
        val newBlockchain = blockchain.newBlock(block)
        val valid = newBlockchain.isValid()

        if (valid.isPresent) {
            return@req Status(
                failed = true,
                errorMessage = "Block is invalid when added to the blockchain",
                extraData = valid.get()
            ).toPayload()
        }

        BlockchainManager.
        blockchain = newBlockchain
        clearDataCache()
        DiscordManager.blockchainChannel
            .flatMap {
                it.createEmbed { spec ->
                    spec.setTitle("Author:" + block.author)
                        .setTimestamp(Instant.ofEpochMilli(block.timestamp))
                        .addField("nonce", "" + block.nonce, true)
                        .addField("difficulty", "" + block.difficulty, true)
                        .addField("lasthash", block.lasthash, true)
                        .addField("hash", block.hash, true)
                    block.actions.forEach { act ->
                        when (act) {
                            is SignUpAction -> {
                                spec.addField("Signed up", act.clientID, true)
                            }
                            is DataAction -> {
                                spec.addField(
                                    "Data for ${act.clientID} (${act.data.uniqueID})",
                                    act.data.key + " : " + act.data.value,
                                    true
                                )
                            }
                            is TransactionAction -> {

//                            println("===")
//                            val gbc = DiscordManager.client
//                                .getGuildEmojiById(Snowflake.of(566998443218960434),Snowflake.of(572065175818076200))
//                                .map { w -> w.asFormat() }
//                                .block()
//                            println("===")
                                val s = " Gumbocoin"//if(true) " Gumbocoins " else "<:gbc:572065175818076200:>"
                                spec.addField("`${act.clientID}` paid `${act.recipientID}`", "${act.amount} $s", true)
                            }
                        }
                    }
                    spec.setAuthor("Block ${blockchain.blocks.size - 1}", "", "")
                }
            }

            .subscribe()
        Status().toPayload()
    }),
    BLOCKCHAIN(Request.Response.BLOCKCHAIN, { blockchain.toPayload() }),
    TRANSACTION(Request.Response.TRANSACTION, req@{ pay ->
        if (!pay.isVerified)
            return@req Status(failed = true, errorMessage = "Unverified RequestDataBlob").toPayload()

        pay as TransactionDataBlob

        if (!blockchain.users.any { it.id == pay.clientID })
            return@req Status(failed = true, errorMessage = "User does not exists").toPayload()

        if (pay.clientID != pay.transactionAction.clientID)
            return@req Status(
                failed = true,
                errorMessage = "Different clientID for the DataBlob object and the transaction object object",
                extraData = pay.clientID + " verses " + pay.transactionAction.clientID
            ).toPayload()


        val user = blockchain.users.first { it.id == pay.clientID }

        if (!pay.transactionAction.isSignatureValid(user.person.publicKey))
            return@req Status(failed = true, errorMessage = "Signature on transactionAction is not valid").toPayload()

        if (blockchain.amounts[pay.transactionAction.clientID] ?: -1 < pay.transactionAction.amount)
            return@req Status(
                failed = true, errorMessage = "You have insufficient funds",
                extraData = "You have ${blockchain.amounts[pay.transactionAction.clientID]}gc, you need ${pay.transactionAction.amount}"
            ).toPayload()

        addToDataCache(pay.transactionAction)
        Status().toPayload()
    }),
    MONEY(Request.Response.MONEY, { pay ->
        pay as StringDataBlob
        SendableInt(blockchain.amounts[pay.value] ?: 0).toPayload()
    }),
    DATA_SUBMISSION(Request.Response.DATA, req@{ pay ->

        if (!pay.isVerified)
            return@req Status(failed = true, errorMessage = "Unverified RequestDataBlob").toPayload()

        pay as DataSubmissionDataBlob

        if (pay.action.clientID != pay.clientID)
            return@req Status(
                failed = true,
                errorMessage = "Different clientID for the DataBlob object and the transaction object object",
                extraData = pay.clientID + " verses " + pay.action.clientID
            ).toPayload()

        if (!validKeys.contains(pay.action.data.key))
            return@req Status(failed = true, errorMessage = "Data with key ${pay.action.data.key} is invalid").toPayload()

        val user =
            (blockchain.users + dataCache.filter { it.type == ActionType.SIGN_UP }
                .map { it as SignUpAction }
                .map { User(it.clientID,Person.fromPublicKey(it.publicKey)) })
                .firstOrNull { it.id == pay.clientID } ?: return@req Status(failed = true, errorMessage = "Can't find user").toPayload()

        if (!pay.action.isSignatureValid(user.person.publicKey))
            return@req Status(failed = true, errorMessage = "Signature on dataAction is not valid").toPayload()

        if (blockchain.blocks.flatMap { it.actions }
                .filter { it.type == ActionType.DATA }
                .map { it as DataAction }
                .any { it.data.uniqueID == pay.action.data.uniqueID })
            return@req Status(
                failed = true, errorMessage = "Data already exists with identical ID",
                extraData = "ID: ${pay.action.data.uniqueID}"
            ).toPayload()

        addToDataCache(pay.action)
        Status().toPayload()
    }),
    VERIFY_DATA_SUBMISSION(Request.Response.VERIFY, req@{ pay ->
        if(!pay.isVerified)
            return@req Status(failed = true, errorMessage = "Unverified RequestDataBlob").toPayload()
        pay as VerifyActionBlob
        val action: VerifyAction = pay.action
        if(action.clientID != action.clientID)
            return@req Status(failed = true, errorMessage = "Action ID different then the payload's ID",
                extraData = "action.clientID: ${action.clientID}  action.clientID:${action.clientID}").toPayload()
        val user = blockchain
            .users
            .firstOrNull { it.id == action.clientID } ?: return@req Status(failed = true, errorMessage = "Couldn't find public key").toPayload()

        val dataPair = blockchain
            .blocks
            .flatMap { it.actions }
            .filter { it.type == ActionType.DATA }
            .map { it as DataAction }
            .firstOrNull { it.data.uniqueID == pay.action.dataID } ?: return@req Status(failed = true, errorMessage = "Can't find the data for the unique ID",
            extraData = "Unique ID:${pay.action.dataID}").toPayload()

        val verified = Person.verify(
            user.person,
            Signature.fromBase64(pay.action.signature),
            dataPair.toSingableString().toByteArray(Charset.forName("UTF-8")))

        if(!verified)
            return@req Status(failed = true,
                errorMessage = "Action is not properly signed",
                extraData = "").toPayload()

        //I think it's good....hopefully?
        addToDataCache(action)
        Status().toPayload()
    }),
    SUBMIT_KEY_FILE(Request.Response.SUBMIT_KEY_FILE, req@{ pay: RequestDataBlob ->
        if(!pay.isVerified)
            return@req Status(failed = true, errorMessage = "Unverified RequestDataBlob").toPayload()
        pay as SubmitKeyFileDataBlob
        val password = pay.password
        val (hash, salt) = passwordHash(password)
        val user = ServerUser(
            clientID = pay.clientID,
            keyfile = pay.key,
            salt = salt,
            hash = hash)
        println("password:$password")
        println("salt:$salt")
        println("hash:$hash")
        println("passwordHash(password,salt):${passwordHash(password,salt)}")
        Mongo
            .users
            .deleteOne(Filters.all("clientID",user.clientID))
            .toMono()
            .then(Mongo.users
                .insertOne(serializeToDocument(user))
                .toMono()
            ).subscribe()
        return@req Status().toPayload()
    }),
    GET_KEY_FILE(Request.Response.GET_KEY_FILE, req@{pay ->
        pay as StringDataBlob
        //the value is the password
        var user: ServerUser? = null
        Mongo.users
            .find(Filters.all("clientID",pay.clientID))
            .first()
            .toMono()
            .map { user = deserialize<ServerUser>(it) }
            .subscribe()
        val start = Instant.now()
        while(user == null && Duration.between(Instant.now(),start).abs().abs().minus(Duration.ofSeconds(10)).isNegative){ }
        val u = user ?: return@req Status(failed = true, errorMessage = "No ServerUser entry for you").toPayload()

        val newHash = passwordHash(pay.value,u.salt)
        if(newHash == u.hash)
            return@req SendableString(u.keyfile).toPayload()
        else
            return@req Status(failed = true,
                errorMessage = "Hash is invalid",
                extraData = "Got $newHash but needed ${u.hash}")//TODO remove
                .toPayload()
    })
}
