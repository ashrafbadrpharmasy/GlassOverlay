package com.mido.glass

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Hosts both overlays (no "draw over apps" permission needed) and intercepts volume keys. */
class OverlayService : AccessibilityService() {
    var volume: VolumeHud? = null
    var island: Island? = null

    override fun onServiceConnected() {
        instance = this
        volume = VolumeHud(this)
        island = Island(this)
    }

    override fun onKeyEvent(e: KeyEvent): Boolean {
        val up = e.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        if (!up && e.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (e.action == KeyEvent.ACTION_DOWN) {
            volume?.show(if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER)
        }
        return true // consume: the stock volume panel never appears
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        volume?.destroy(); island?.destroy(); instance = null
        return super.onUnbind(intent)
    }

    companion object { @Volatile var instance: OverlayService? = null }
}
