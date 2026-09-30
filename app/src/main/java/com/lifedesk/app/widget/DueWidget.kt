package com.lifedesk.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lifedesk.app.LifeDeskApp
import com.lifedesk.app.MainActivity
import com.lifedesk.app.R
import com.lifedesk.app.domain.countdown
import com.lifedesk.app.domain.isSnoozed
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.needsAttention
import com.lifedesk.app.voice.VoiceCommandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Home-screen widget: the next things that need attention, at a glance. */
class DueWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try { render(context, manager, ids) } finally { pending.finish() }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val rows = intArrayOf(R.id.widget_row1, R.id.widget_row2, R.id.widget_row3)

        /** Call after data changes; cheap no-op when no widget is placed. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, DueWidget::class.java))
            if (ids.isEmpty()) return
            scope.launch { runCatching { render(app, manager, ids) } }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val app = context.applicationContext as LifeDeskApp
            val today = LocalDate.now()
            val items = app.repository.activeItems()
            val attention = items.filter { it.needsAttention(today) }.sortedBy { it.dueDate }
            val upcoming = attention.ifEmpty { items.filter { it.dueDate != null && !it.isSnoozed(today) && !it.dueDate.isBefore(today) }.sortedBy { it.dueDate } }
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_due).apply {
                setTextViewText(
                    R.id.widget_title,
                    if (attention.isEmpty()) "LifeDesk · all clear ✓" else "LifeDesk · ${attention.size} need attention",
                )
                rows.forEachIndexed { i, rowId ->
                    val item = upcoming.getOrNull(i)
                    setTextViewText(rowId, item?.let {
                        "${it.category.emoji} ${it.title} — ${it.countdown(today)}" + (it.amount?.let { a -> " · ${money(a, it.currency)}" } ?: "")
                    } ?: if (i == 0 && upcoming.isEmpty()) "Nothing coming up. Tap to add something." else "")
                }
                setOnClickPendingIntent(R.id.widget_root, open)
                setOnClickPendingIntent(R.id.widget_mic, PendingIntent.getActivity(
                    context, 1, Intent(context, VoiceCommandActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ))
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
