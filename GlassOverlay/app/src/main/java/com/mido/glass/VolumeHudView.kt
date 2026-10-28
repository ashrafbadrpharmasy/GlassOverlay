package com.mido.glass

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

/** Frosted-glass pill with a fill level. Zero allocations in onDraw. */
class VolumeHudView(c: Context) : View(c) {
    var level = 0f
    var onUserLevel: ((Float) -> Unit)? = null
    var onTouching: ((Boolean) -> Unit)? = null

    private val d = c.resources.displayMetrics.density
    private val radius = 24f * d
    private val rect = RectF()
    private val clip = Path()
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFFFFF.toInt() }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.2f * d; color = 0x66FFFFFF
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val i = 0.6f * d
        rect.set(i, i, w - i, h - i)
        clip.reset(); clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        // soft vertical gradient = cheap "glass" without a real-time blur
        bg.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xCC3A3D4D.toInt(), 0xCC15161C.toInt(), Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        c.drawRoundRect(rect, radius, radius, bg)
        val top = rect.bottom - rect.height() * level
        c.save(); c.clipPath(clip)
        c.drawRect(rect.left, top, rect.right, rect.bottom, fill)
        c.restore()
        c.drawRoundRect(rect, radius, radius, edge)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { parent?.requestDisallowInterceptTouchEvent(true); onTouching?.invoke(true) }
            MotionEvent.ACTION_MOVE -> onUserLevel?.invoke(1f - (e.y / height).coerceIn(0f, 1f))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onTouching?.invoke(false)
        }
        return true
    }
}
