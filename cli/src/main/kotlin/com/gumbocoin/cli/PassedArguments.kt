package com.gumbocoin.cli

import com.xenomachina.argparser.ArgParser
import systems.carson.base.Release

class PassedArguments(parser: ArgParser){
    val release by parser.storing(
        "-r","--release",
        help = "Release"
    ) { Release.valueOf(toUpperCase()) }
}