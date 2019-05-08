package com.gumbocoin.server

import com.xenomachina.argparser.ArgParser
import systems.carson.base.DevFlags
import systems.carson.base.Release
import java.io.File

class InputArguments(parser : ArgParser) {
    val release by parser.storing("-r","--release", help = "The release to use") { Release.valueOf(toUpperCase()) }

    val database by parser.storing("-d","--database-mode","--database", help = "The database mode to use") { BlockchainSource.valueOf(toUpperCase()) }

    val keyDir by parser.storing("-k","--key-dir", help ="The root key directory.") { File(this) }
    val devFlags by parser.adding("--dev", help = "A dev flag") { DevFlags.valueOf(toUpperCase()) }
}