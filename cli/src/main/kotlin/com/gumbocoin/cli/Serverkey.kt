package com.gumbocoin.cli

import systems.carson.base.Person

val server = Person.deserialize("""
    --- BEGIN PUBLIC KEY ---
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvw3qb27mUV7xATiaThl9qMDpEx9U1go/IG+F
pyoNYRwAbYIC90wJPl37V3xJTP5Z9+viXXJEB7N4cEAi4v/o1GNIUpfKdaCnRLPs+I5aptzjoCe4hJkA
CPa0fNpYaKyI7YWJaPonRd7s28TRXJN/Be4vzreuYKbe4jJG3Gdb2eZSafu8hN+hPXW77u7D/zgD8PzG
LPXrO5BE0I//X5QycemgkeZB5Lo1boUjtfi+R3J7B2wPNm+THDQymIldlL4wYksxnaaQo5HwP7QtYIAV
VmbSe82r/hAaKC+Oc554dViYcBI5veqgTeUT41qBnEJpjJQwgaCOw4Rgi+GiANyKaQIDAQAB
--- END PUBLIC KEY ---
""".trimIndent())
