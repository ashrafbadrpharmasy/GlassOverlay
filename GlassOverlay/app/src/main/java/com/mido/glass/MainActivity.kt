package com.mido.glass

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * d).toInt(), (48 * d).toInt(), (24 * d).toInt(), 0)
            setBackgroundColor(0xFF0E0F14.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Glass Overlay\n\nDo steps 1 and 2 once, then press the volume buttons or tap the test buttons."
            setTextColor(Color.WHITE); textSize = 15f
        })
        fun btn(t: String, a: () -> Unit) = root.addView(Button(this).apply {
            text = t; isAllCaps = false; setOnClickListener { a() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (12 * d).toInt() })

        btn("1. Enable Accessibility service") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        btn("2. Enable Notification access") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        btn("Test volume HUD") { OverlayService.instance?.volume?.show(0) ?: needService() }
        btn("Test island") {
            OverlayService.instance?.island?.show("Hello Mido", "This is the dynamic island", null) ?: needService()
        }
        setContentView(root)
    }
    private fun needService() = Toast.makeText(this, "Enable the Accessibility service first", Toast.LENGTH_SHORT).show()
}
