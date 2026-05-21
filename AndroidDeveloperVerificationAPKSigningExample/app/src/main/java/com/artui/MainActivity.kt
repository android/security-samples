package com.artui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<LinearLayout>(R.id.listContainer)

        val vault = Vault(Store(this))
        val list = vault.list()

        if (list.isEmpty()) {

            val empty = TextView(this).apply {
                text = "Nenhuma senha salva"
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
            }

            container.addView(empty)

        } else {

            for (item in list) {

                val card = TextView(this).apply {
                    text = """
                        🔑 ${item.title}
                        👤 ${item.user}
                        🔒 ${item.pass}
                    """.trimIndent()

                    setPadding(0, 20, 0, 20)
                    textSize = 16f
                    setTextColor(android.graphics.Color.WHITE)
                }

                container.addView(card)
            }
        }
    }
}