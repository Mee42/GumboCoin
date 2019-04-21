package com.gumbocoin.cli

import systems.carson.base.Person

val server = Person.deserialize("""
--- BEGIN PUBLIC KEY ---
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqBWysvscw60qhgyfIUP4b/suNdOw4Yhjzggh
DbhiFuC9g+X9NCGFEWsXflVMjbzyIDoK239P6bcJt7hmlwByvg317C0/y7hBHTL9R3OEipuHZeNoGXNG
Wb1E2ubGEClwMKK8SKerBeTP3JPhTtCVz0XOcbHhS0/m6TjBgXQrTGxJ1V53qltxxZ/pggvukhNpdXAB
8GeFG7DMP6ypF9F8MNok+Rvoxk1uV9JxzSMtl4LvqLxkXBcYvUUYdOgoOPPpG56tTz3g3lkhbKRJjnJ2
BA6UEambgpxfYoLQ0bQlhXoPQHB3aFJqstCucKSTA7Es2iMWuwrYyWWkpjrenPqP3QIDAQAB
--- END PUBLIC KEY ---
""".trimIndent())
