package com.gumbocoin.cli.new.dsl

import com.gumbocoin.cli.*
import com.gumbocoin.cli.new.*
import org.apache.commons.codec.digest.DigestUtils
import systems.carson.base.*
import java.io.File
import java.nio.charset.Charset
import java.util.*


@Suppress("UNREACHABLE_CODE")
private val passwordFunction:(Context) -> String = fun(context :Context):String {
    val readPassword: () -> CharArray = reader@ {
        System.console()?.readPassword() ?: return@reader context.scan.nextLine().toCharArray()
    }
    while(true){
        print("password:")
        val a: CharArray = readPassword()
        print("enter again:")
        val b :CharArray = readPassword()
        if(!a.contentEquals(b))
            println("Content is not equals")
        else if(a.toString().isBlank())
            println("Password can not be blank")
        else
            return a.fold("") {aa,bb -> "$aa$bb"}
    }
    error("kotlin is a dumbass")
}


val signup = filteredRunner {

    conditional("You can't create an account while logged in") { !it.isLoggedIn }

    final = runner { context: Context ->
        val clientID = DigestUtils.sha1Hex(UUID.randomUUID().toString()).substring(0, 10)
        println("clientID: $clientID")
        val keys = Person.generateNew()
        val status = context.socket.requestResponse(
            SignUpDataBlob(clientID,SignUpAction(clientID,keys.publicKeyBase64())), keys)
            .mapFromJson<Status>()
            .block()
        when {
            status == null -> println("Didn't get a response from the server")
            status.failed -> {
                println("Error:" + status.errorMessage)
                status.extraData.let { if (it.isBlank()) null else it }?.let { println("Extra Data:$it") }
            }
            else -> {
                println("Success!")
                context.setDaCredentials(Credentials(clientID,keys))
                while(!saveKeys(context)){
                    println("you need to save it somewhere, or you will be locked out of your account")
                    println("You can always upload to the cloud later")
                }
                //saved
            }
        }
    }
}

fun saveKeys(context :Context):Boolean{
    var saved = 0
    split {
        one = filteredRunner {
            yes("Do you want to save the keys on the server?\n" +
                    "Only people with the password will have access to it")
            runnerr {
                val password = passwordFunction(context)
                val result = it.socket.requestResponse(
                    SubmitKeyFileDataBlob(
                        context.credentials.clientID,
                        context.credentials.keys.serialize(),
                        DigestUtils.sha256Hex(password),
                        Request.Response.SUBMIT_KEY_FILE.intent
                    ), context.credentials.keys)
                    .mapFromJson<Status>()
                    .printStatus("Succeeded!")
                    .block()!!
                if(result.failed){
                    println("Failed to save to cloud. You should save locally, and then publish it to the could after")
                }else {
                    println("Successfully store in cloud!")
                    saved++
                }
            }
        }
        two = filteredRunner {
            yes("Do you want to save the keys locally?")
            runnerr {
                val def = "./${context.credentials.clientID}.gc.key"
                val file = File(prompt("Keyfile: ($def)").takeIf { it.isNotBlank() } ?: def)
                file.writeText(context.credentials.keys.serialize(), Charset.forName("UTF-8"))
                println("Written")
                saved++
            }
        }
    }.run(context)
    return saved > 0
}