package com.gumbocoin.cli.console

import com.gumbocoin.cli.*
import com.gumbocoin.cli.Context
import com.gumbocoin.cli.Runner
import com.gumbocoin.cli.filteredRunner
import com.gumbocoin.cli.runner
import systems.carson.base.*
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.Callable

private val dataa : Runner
    get() = data

var data: Runner = runner {
    print("Enter key for data (? for help )")
    val input = it.scan.nextLine().trim()
    if (input.isBlank()) {
        println("Can't accept that as a key")
        dataa.run(it)
        return@runner
    }
    if (input == "?") {
        println("Valid keys:")
        validDataKeys.forEach { println("   $it") }
        println("Type \"nevermind\" to exit")
        dataa.run(it)
        return@runner

    }
    if (input == "nevermind") {
        return@runner
    }
    if (!validDataKeys.contains(input)) {
        println("\"$input\" is not a valid key")
        dataa.run(it)
        return@runner
    }
    @Suppress("UNREACHABLE_CODE")
    val value = Callable<String> {
        while (true) {
            print("Value: ")
            val inn = it.scan.nextLine().trim()
            if (inn.isBlank()) {
                println("Value can not be blank")
            } else {
                return@Callable inn
            }
        }
        error("Exited loop - tf?")
    }.call()

    //actually make request
    val pair = DataPair(
        key = input,
        value = value,
        uniqueID = UUID.randomUUID().toString()
    )
    val response = it.socket
        .requestResponse(
            blob = DataSubmissionDataBlob(
                clientID = it.credentials.clientID,
                action = DataAction.sign(
                    clientID = it.credentials.clientID,
                    data = pair,
                    person = it.credentials.keys
                )
            ),
            keys = it.credentials.keys
        )
        .mapFromJson<Status>()
        .block() ?: error("Didn't get a response from the server")
    if (!response.failed)
        println("Data added successfully!")
    else {
        println("Data submission failed\n")
        response.errorMessage.takeIf { w -> w.isNotBlank() }
            ?.let { w -> println("    error message:$w") }
        response.extraData.takeIf { w -> w.isNotBlank() }
            ?.let { w -> println("        extraData:$w") }
    }
}

val verify = filteredRunner {
    conditional("You need to be logged in") { it.isLoggedIn }
    runnerr { context -> verify(context) }
}


private fun verify(context : Context){
    print("ID of the data you want to sign? ")
    val id = context.scan.nextLine()
    if(id.isBlank()){
        println("Not a valid ID")
        return
    }
    verify(id, context)
}
private fun verify(id :String,context: Context){
    val blockchain = context.socket.requestResponse(RequestDataBlob(Request.Response.BLOCKCHAIN, context.credentials.clientID),context.credentials.keys)
        .mapFromJson<Blockchain>()
        .block() ?: error("Didn't get a response from the server")
    val dataToSign: DataAction? = blockchain
        .blocks
        .flatMap { it.actions }
        .filter { it.type == ActionType.DATA }
        .map { it as DataAction }
        .firstOrNull { it.data.uniqueID == id }
    if(dataToSign == null){
        println("Can't find data for ID \"$id\"")
        return
    }
    print("Sign ${dataToSign.clientID}'s data: ${dataToSign.data.key} = ${dataToSign.data.value} (y/n/u): ")
    when(context.scan.nextLine().trim()){
        "y" -> { /* continue */ }
        "n" -> {
            return
        }
        "u" -> {
            println(dataToSign.data.uniqueID)
            print("(y/n): ")
            val input = context.scan.nextLine().trim()
            if(input != "y"){
                return
            }
        }
        else -> {
            return
        }
    }
    val bytes = dataToSign.toSingableString().toByteArray(Charset.forName("UTF-8"))
    val signature = context.credentials.keys.sign(bytes)
    val action = VerifyAction(context.credentials.clientID,dataToSign.data.uniqueID,signature.toBase64())
    //send the action
    val response = context.socket.requestResponse(VerifyActionBlob(
        clientID = context.credentials.clientID,
        action = action
    ))
        .mapFromJson<Status>()
        .block() ?: error("Didn't get a response from the server")
    if(!response.failed)
        println("Verification added successfully!")
    else {
        println("Verification submission failed")
        response.errorMessage.takeIf { it.isNotBlank() }
            ?.let { println("    error message:$it") }
        response.extraData.takeIf { it.isNotBlank() }
            ?.let { println("        extraData:$it") }
    }
}
