package com.gumbocoin.cli.new.dsl

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.gumbocoin.cli.*
import com.gumbocoin.cli.new.Credentials
import com.gumbocoin.cli.new.filteredRunner
import com.gumbocoin.cli.new.runner
import com.gumbocoin.cli.new.switchy
import org.apache.commons.codec.digest.DigestUtils
import systems.carson.base.*
import java.io.File
import java.nio.charset.Charset

val login = filteredRunner {

    conditional("You can't be logged in ") { !it.isLoggedIn }

    final = switchy {
        conditional = {
            val x = prompt("Do you want to use a server keyfile?")
            x.isBlank() || x[0].toUpperCase() == 'Y'
        }
        truthy = runner { context ->
            val clientID = prompt("ClientID")
            val password = prompt("Password")
            val got = context.socket.requestResponse(
                StringDataBlob("defaultID", DigestUtils.sha256Hex(password) + ":" + clientID, Request.Response.GET_KEY_FILE.intent),
                Person.default)
                .block()
            if(got == null){
                println("Didn't get a response from the server")
            }else {
                try {
                    val str = Gson().fromJson(got.trimAESPadding(), SendableString::class.java)
                    if(str?.value == null)
                        throw JsonSyntaxException("Fuck, why wasn't this caught?")
                    val decrypted = deserialize<EncryptedAESStrings>(str.value).toBytes()
                    val plaintext = AESEncryption.decryptAES(decrypted,password.toByteArray(Charset.forName("UTF-8")))//TODO make password byte[] or char[]
                    val person = Person.fromKeyFile(plaintext.toString(Charset.forName("UTF-8")))
                    context.setDaCredentials(Credentials(clientID,person))
                    println("Logged in successfully!")
                } catch (e: JsonSyntaxException) {
                    val status = Sendable.fromJson<Status>(got)
                    println("Error getting key:${status.errorMessage}")
                    status.extraData.takeIf { it.isNotBlank() }
                        ?.let { println("Extra data:$it") }
                }

            }
        }
        falsy = runner { context ->
            val keyFile = File(prompt("Keyfile") { it.isNotBlank().toError("Input can not be blank") })

            val str = keyFile.name
            var maybeID = str.replaceFirst(".gc.key", "")
            if (!str.contains(".gc.key")) {
                maybeID = ""
            }
            maybeID = if (maybeID.isNotBlank()) "($maybeID)" else ""

            var clientID = prompt("Client ID $maybeID") {
                if (maybeID.isNotBlank())
                    ErrorStatus.success()
                else
                    it.isNotBlank().toError("Input can not be blank")
            }
            if (clientID.isBlank())
                clientID = maybeID.substring(1, maybeID.length - 1)//I hate this code
            clientID = clientID.trim()

            val person = Person.fromKeyFile(keyFile.readText(Charset.forName("UTF-8")))

            val result = context.socket.requestResponse(RequestDataBlob(Request.Response.VERIFIED, clientID), person)
                .map { Sendable.fromJson<SendableBoolean>(it) }
                .map { it.value }
                .block()
            if (result == true) {
                println("Logged in successfully")
                context.setDaCredentials(Credentials(clientID,person))
            } else {
                println("Couldn't verify account. Login unsuccessful")
            }
        }
    }
}