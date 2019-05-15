package com.gumbocoin.base

private const val source = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
private const val length = 10
data class DataPair(
    val key: String,
    val value: String,
    val uniqueID: String = (0..length).map { source.random() }.joinToString("")
)