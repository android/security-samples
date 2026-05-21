package com.artui

import java.util.UUID

class Vault(private val store: Store) {

    fun add(title: String, user: String, pass: String) {
        val raw = "$title|$user|$pass"
        val enc = Crypto.enc(raw)

        store.save(UUID.randomUUID().toString(), enc)
    }

    fun list(): List<Password> {
        return store.getAll().mapNotNull {
            val dec = Crypto.dec(it.value.toString())
            val p = dec.split("|")

            if (p.size == 3) {
                Password(p[0], p[1], p[2])
            } else null
        }
    }
}