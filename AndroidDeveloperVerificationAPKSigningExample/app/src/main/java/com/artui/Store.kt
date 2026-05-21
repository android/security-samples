package com.artui

import android.content.Context

class Store(context: Context) {

    private val prefs = context.getSharedPreferences("vault", Context.MODE_PRIVATE)

    fun save(id: String, value: String) {
        prefs.edit().putString(id, value).apply()
    }

    fun getAll(): Map<String, *> = prefs.all
}