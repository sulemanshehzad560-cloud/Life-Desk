package com.lifedesk.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.lifedesk.app.LifeDeskApp
import com.lifedesk.app.widget.DueWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** "Mark paid" / "Snooze 1 day" buttons on reminder notifications — no need to open the app. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Reminders.EXTRA_ITEM_ID, -1L)
        if (id <= 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = (context.applicationContext as LifeDeskApp).repository
                val item = repo.item(id)
                if (item != null) {
                    when (intent.action) {
                        ACTION_DONE -> repo.complete(item)
                        ACTION_SNOOZE -> repo.snooze(item, 1)
                    }
                }
                NotificationManagerCompat.from(context).cancel(id.toInt())
                DueWidget.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.lifedesk.app.action.DONE"
        const val ACTION_SNOOZE = "com.lifedesk.app.action.SNOOZE"
    }
}
