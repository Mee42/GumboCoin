package com.gumbocoin.base

import org.apache.commons.codec.digest.DigestUtils


actual fun sha256Hex(str :String):String = DigestUtils.sha256Hex(str)