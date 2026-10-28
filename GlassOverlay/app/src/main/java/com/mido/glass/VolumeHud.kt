package com.mido.glass

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.roundToInt

/** Owns the HUD window. The window only exists while the HUD is visible. */
class VolumeHud(private val ctx: Context) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private val h = Handler(Looper.getMainLooper())
    private val d = ctx.resources.displayMetrics.density
    private val hide = Runnable { dismiss() }

    private var view: VolumeHudView? = null
    private var levelAnim: SpringAnimation? = null
    private var outAnim: SpringAnimation? = null
    private var touching = false

    private val LEVEL = object : FloatPropertyCompat<VolumeHudView>("level") {
        override fun getValue(o: VolumeHudView) = o.level
        override fun setValue(o: VolumeHudView, v: Float) { o.level = v.coerceIn(0f, 1f); o.invalidate() }
    }

    private fun stream() =
        if (audio.mode == AudioManager.MODE_IN_COMMUNICATION || audio.mode == AudioManager.MODE_IN_CALL)
            AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_MUSIC

    /** dir = AudioManager.ADJUST_RAISE / ADJUST_LOWER, or 0 to just show the HUD. */
    fun show(dir: Int) {
        val s = stream()
        val max = audio.getStreamMaxVolume(s)
        if (dir != 0) audio.adjustStreamVolume(s, dir, 0) // flag 0 = no system panel
        val cur = audio.getStreamVolume(s)
        val target = cur.toFloat() / max
        h.removeCallbacks(hide)

        var v = view
        if (v == null) {
            v = VolumeHudView(ctx).also {
                it.setLayerType(View.LAYER_TYPE_HARDWARE, null) // cached GPU texture, cheap to animate
                it.level = target
                it.translationX = 90f * d
                it.onTouching = { t -> touching = t; if (!t) scheduleHide() else { h.removeCallbacks(hide); levelAnim?.cancel() } }
                it.onUserLevel = { l ->
                    it.level = l; it.invalidate()
                    audio.setStreamVolume(stream(), (l * audio.getStreamMaxVolume(stream())).roundToInt(), 0)
                }
            }
            view = v
            wm.addView(v, params())
        }
        outAnim?.cancel(); outAnim = null

        if (v.translationX != 0f) {
            SpringAnimation(v, DynamicAnimation.TRANSLATION_X, 0f).apply {
                spring.stiffness = SpringForce.STIFFNESS_LOW
                spring.dampingRatio = 0.6f
            }.start()
        }
        if (!touching) {
            levelAnim?.cancel()
            levelAnim = SpringAnimation(v, LEVEL, target).apply {
                spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                spring.dampingRatio = 0.85f
            }.also { it.start() }
        }
        if (dir != 0) { // haptic-like squash on every press, harder at the limits
            val atLimit = (dir > 0 && cur == max) || (dir < 0 && cur == 0)
            v.scaleX = if (atLimit) 0.94f else 1.05f
            v.scaleY = if (atLimit) 1.10f else 1.05f
            SpringAnimation(v, DynamicAnimation.SCALE_X, 1f).apply { spring.stiffness = 500f; spring.dampingRatio = 0.4f }.start()
            SpringAnimation(v, DynamicAnimation.SCALE_Y, 1f).apply { spring.stiffness = 500f; spring.dampingRatio = 0.4f }.start()
        }
        scheduleHide()
    }

    private fun scheduleHide() { h.removeCallbacks(hide); h.postDelayed(hide, 1800) }

    fun dismiss() {
        val v = view ?: return
        outAnim = SpringAnimation(v, DynamicAnimation.TRANSLATION_X, 90f * d).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            addEndListener { _, canceled, _, _ -> if (!canceled) remove() }
            start()
        }
    }

    private fun remove() {
        view?.let { try { wm.removeViewImmediate(it) } catch (_: Exception) {} }
        view = null; outAnim = null; levelAnim = null
    }

    fun destroy() { h.removeCallbacksAndMessages(null); remove() }

    private fun params() = WindowManager.LayoutParams(
        (52 * d).toInt(), (176 * d).toInt(),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = (10 * d).toInt() }
}
