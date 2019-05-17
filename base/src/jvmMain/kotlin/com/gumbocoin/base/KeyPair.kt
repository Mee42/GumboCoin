package com.gumbocoin.base

import org.apache.commons.codec.binary.Base64
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Optional


actual class KeyPair(private val publicKey : Optional<PublicKey>, private val privateKey :Optional<PrivateKey>){
    actual fun serializeToKeyfile(): String {
        fun addNewLines(s: String): String {
            val new = StringBuilder()
            var i = 0
            for (char in s) {
                new.append(char)
                if (++i % 80 == 0)
                    new.append('\n')
            }
            return new.toString()
        }

        var s = ""
        privateKey.ifPresent {
            s += "$START_PRIVATE\n"
            s += addNewLines(Base64.encodeBase64String(it.encoded)) + "\n"
            s += "$END_PRIVATE\n"
        }
        publicKey.ifPresent {
            s += "$START_PUBLIC\n"
            s += addNewLines(Base64.encodeBase64String(it.encoded)) + "\n"
            s += "$END_PUBLIC\n"
        }
        return s
    }
    actual companion object {
        actual fun deserializeFromKeyFile(string: String) :KeyPair{
            var private: PrivateKey? = null
            var public: PublicKey? = null
            if (string.contains(START_PRIVATE)) {
                var str = string
                    .substring(string.indexOf(START_PRIVATE))
                    .replaceFirst(START_PRIVATE, "")
                str = str.substring(0, str.indexOf(END_PRIVATE))
                    .replace("\n", "")
                    .trim()
                private = generatePrivateFromBase64(str)
            }
            if (string.contains(START_PUBLIC)) {
                var str = string
                    .substring(string.indexOf(START_PUBLIC))
                    .replaceFirst(START_PUBLIC, "")
                str = str.substring(0, str.indexOf(END_PUBLIC))
                    .replace("\n", "")
                    .trim()
                public = generatePublicFromBase64(str)
            }
            return KeyPair(Optional.ofNullable(public),Optional.ofNullable(private))
        }


        /** Takes Base64 */
        private fun generatePublicFromBase64(s: String): PublicKey {
            val encoded = Base64.decodeBase64(s)
            val spec = X509EncodedKeySpec(encoded)
            return KeyFactory.getInstance("RSA").generatePublic(spec)
        }

        /** Takes Base64 */
        private fun generatePrivateFromBase64(s: String): PrivateKey {
            val encoded = Base64.decodeBase64(s)
            val spec = PKCS8EncodedKeySpec(encoded)
            return KeyFactory.getInstance("RSA").generatePrivate(spec)
        }
    }

    actual fun hasPrivateKey(): Boolean {
        return privateKey.isPresent
    }

    actual fun hasPublicKey(): Boolean {
        return publicKey.isPresent
    }


}