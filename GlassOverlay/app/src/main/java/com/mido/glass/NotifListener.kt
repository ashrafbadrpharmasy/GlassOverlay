package com.mido.glass

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Feeds notifications + music-player notifications (title/artist) to the island. */
class NotifListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification
        if (sbn.packageName == packageName) return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val media = n.category == Notification.CATEGORY_TRANSPORT
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0 && !media) return
        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        OverlayService.instance?.island?.show(if (media) "♪  $title" else title, text, n.contentIntent)
    }
}
