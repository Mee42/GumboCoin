package com.gumbocoin.cli

import systems.carson.base.Person
import systems.carson.base.Release.BETA
import systems.carson.base.Release.DEV
import systems.carson.base.ReleaseManager

val server = (mapOf(
    BETA to """
--- BEGIN PUBLIC KEY ---
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgWtrqoCVpKdnckYR7ulMmup2N/AoC+Rc+9RV
J0l4Jo4NHsbEddyQ8QIZ+RGluGqENRs806tHB9xZ/klGJajZFtQ+dBE8tDuFN/GZ5qyI+KTe7eyIYmLg
NTis8b3wz/5VAQZS7aTxzsWON5P/zNkHiOLBYW6B4b+inFSmgQrztgHobwmPkiBrYNi0Yjw7tK3Y3uif
qXKB7NPhdloDvC5dYnK3tmpfKo5TK1BNKeHy2oi5Tr+KcQYZhUb4Q/66BuNDPwckNM2L9AgzQoX0zsHb
Z0tOnALgjcV9lv9qJynK6KwYO9iAmlJaWZTSMugaoZMFldT/IhX5OOXZaWUODGL3EwIDAQAB
--- END PUBLIC KEY ---
""".trimIndent(),
    DEV to """
        --- BEGIN PUBLIC KEY ---
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvMoo7+pOZMhLWNUijPFlOVw+ZL2BITYURliw
6WyZrSJ9/a46bhvCZJ0vJpqLYSXFAi9pe66GMw9aB76Baz3r3m2vGu0FfRs9OsEQsFXspd3Gxp/zTySG
Hn7TxIbY/x3uqxnasnDPxDeiK1r8GEd3oHgPpccX8H7dhkrT973SM5HQcQHwbYJt31vf4RzVoLTHHZCY
IogKOhlORdFw4TS9r2hREd/my8BTi9wmzYZNldHvQGaNN4ZjGsoKd/tuW6XFayw0c9rQe8VCX7h+8Uvm
yr6x4DylpfbUXJ+mXIYK+B3YB6v+Ab2YDL5Wo7tC9AgETaWlYUlYGIboRfv0QkDGIQIDAQAB
--- END PUBLIC KEY ---
    """.trimIndent()
)[ReleaseManager.release] ?: error("No key for release"))
    .let { Person.fromKeyFile(it) }