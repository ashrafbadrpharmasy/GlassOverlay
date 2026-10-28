package com.mido.glass

import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.abs

/**
 * The window is a fixed size (the expanded size), so morphing never triggers a relayout:
 * only the drawn pill + clip change per frame. progress 0 = small pill, 1 = expanded.
 */
class IslandView(c: Context) : FrameLayout(c) {
    var onTap: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null
    private var onCollapsed: (() -> Unit)? = null

    private val d = c.resources.displayMetrics.density
    val fullW = (330 * d).toInt()
    val fullH = (72 * d).toInt()
    private val w0 = 110 * d
    private val h0 = 30 * d

    private var progress = 0f
    private val rect = RectF()
    private val clip = Path()
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEE0D0E13.toInt() }
    private val glossP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, fullH.toFloat(), 0x38FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
    private val edgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f * d; color = 0x44FFFFFF
    }

    private val title = TextView(c).apply {
        setTextColor(Color.WHITE); textSize = 14f; typeface = Typeface.DEFAULT_BOLD
        maxLines = 1; ellipsize = TextUtils.TruncateAt.END
    }
    private val sub = TextView(c).apply {
        setTextColor(0xB3FFFFFF.toInt()); textSize = 12f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
    }
    private val content = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
        setPadding((22 * d).toInt(), 0, (22 * d).toInt(), 0); alpha = 0f
        addView(title); addView(sub)
    }

    private val prop = object : FloatPropertyCompat<IslandView>("p") {
        override fun getValue(o: IslandView) = o.progress
        override fun setValue(o: IslandView, v: Float) {
            o.progress = v
            o.content.alpha = ((v - 0.45f) / 0.55f).coerceIn(0f, 1f)
            o.invalidate()
        }
    }
    private val spring = SpringAnimation(this, prop).setSpring(
        SpringForce(0f).setStiffness(320f).setDampingRatio(0.62f) // a little overshoot = the "morph" feel
    ).apply {
        addEndListener { _, canceled, value, _ -> if (!canceled && value < 0.05f) onCollapsed?.invoke() }
    }

    init { addView(content, LayoutParams(fullW, fullH)) }

    fun bind(t: String, s: String) { title.text = t; sub.text = s; sub.visibility = if (s.isEmpty()) GONE else VISIBLE }

    fun expand() { onCollapsed = null; spring.spring.finalPosition = 1f; spring.start() }
    fun collapse(done: () -> Unit) { onCollapsed = done; spring.spring.finalPosition = 0f; spring.start() }

    override fun dispatchDraw(canvas: Canvas) {
        val p = progress
        val w = (w0 + (fullW - w0) * p).coerceAtLeast(w0 * 0.9f)
        val h = (h0 + (fullH - h0) * p).coerceAtLeast(h0 * 0.9f)
        val r = minOf(h / 2f, 30 * d)
        rect.set((width - w) / 2f, 0f, (width + w) / 2f, h)
        canvas.drawRoundRect(rect, r, r, fillP)
        clip.reset(); clip.addRoundRect(rect, r, r, Path.Direction.CW)
        canvas.save(); canvas.clipPath(clip)
        canvas.drawRoundRect(rect, r, r, glossP)
        super.dispatchDraw(canvas)
        canvas.restore()
        canvas.drawRoundRect(rect, r, r, edgeP)
    }

    private val gd = GestureDetector(c, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean { onTap?.invoke(); return true }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (vy < -400f || abs(vx) > 900f) { onDismiss?.invoke(); return true }
            return false
        }
    })
    override fun onTouchEvent(e: MotionEvent) = gd.onTouchEvent(e)
}
