package com.mido.glass

import android.app.PendingIntent
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/** Owns the island window. Created on demand, removed after it collapses = no idle cost. */
class Island(private val ctx: Context) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val h = Handler(Looper.getMainLooper())
    private val d = ctx.resources.displayMetrics.density
    private var view: IslandView? = null
    private var lastKey = ""
    private var lastAt = 0L
    private val collapse = Runnable { view?.collapse { remove() } }

    fun show(title: String, text: String, tap: PendingIntent?) {
        h.post {
            val key = "$title|$text"
            val now = SystemClock.uptimeMillis()
            if (key == lastKey && now - lastAt < 10_000) return@post // media notifications spam updates
            lastKey = key; lastAt = now
            h.removeCallbacks(collapse)

            val v = view ?: IslandView(ctx).also {
                it.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                it.onDismiss = { h.removeCallbacks(collapse); it.collapse { remove() } }
                view = it
                wm.addView(it, params(it))
            }
            v.onTap = {
                try { tap?.send() } catch (_: Exception) {}
                h.removeCallbacks(collapse); v.collapse { remove() }
            }
            v.bind(title, text)
            v.expand()
            h.postDelayed(collapse, 3500)
        }
    }

    private fun remove() {
        view?.let { try { wm.removeViewImmediate(it) } catch (_: Exception) {} }
        view = null
    }

    fun destroy() { h.removeCallbacksAndMessages(null); remove() }

    private fun params(v: IslandView) = WindowManager.LayoutParams(
        v.fullW, v.fullH,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = (8 * d).toInt()
        if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}
