package com.gumbocoin.base

enum class Release(val str: String) {
    MASTER("master"),
    BETA("beta"),
    DEV("dev")
}

enum class DevFlags{
    LOW_DIFF
}
val PORT = mapOf(
    Release.MASTER to 48625,
    Release.BETA to 48626,
    Release.DEV to 48627)

val IP = mapOf(
    Release.MASTER to "72.66.54.109",
    Release.BETA to "72.66.54.109",
    Release.DEV to "localhost")



val validDataKeys = listOf(
    "first_name",
    "last_name",
    "email",
    "discord")

