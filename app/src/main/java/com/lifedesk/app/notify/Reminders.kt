package com.lifedesk.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lifedesk.app.LifeDeskApp
import com.lifedesk.app.MainActivity
import com.lifedesk.app.R
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.domain.countdown
import com.lifedesk.app.domain.daysLeft
import com.lifedesk.app.domain.headline
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.needsAttention
import com.lifedesk.app.domain.reminderDueToday
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL = "reminders"
    const val EXTRA_ITEM_ID = "itemId"
    private const val WORK_NAME = "daily-reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.channel_reminders_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Runs [ReminderWorker] once a day at [hour]:00 local time. */
    fun schedule(context: Context, hour: Int, replace: Boolean) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun sendTest(context: Context) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<ReminderWorker>().setInputData(workDataOf("test" to true)).build()
        )
    }

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notify(context: Context, id: Int, title: String, body: String, itemId: Long?) {
        if (!canNotify(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            itemId?.let { putExtra(EXTRA_ITEM_ID, it) }
        }
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LifeDeskApp
        val today = LocalDate.now()
        val items = app.repository.activeItems()
        val currency = app.prefs.settings.value.currency
        val due = if (inputData.getBoolean("test", false)) {
            items.filter { it.needsAttention(today) }.ifEmpty { items.filter { it.dueDate != null } }
                .sortedBy { it.dueDate }.take(1)
                .ifEmpty {
                    Reminders.notify(applicationContext, 0, "LifeDesk reminders are on", "You'll be reminded before anything is due.", null)
                    return Result.success()
                }
        } else {
            items.filter { it.reminderDueToday(today) }.sortedBy { it.daysLeft(today) }
        }
        due.take(6).forEach { item -> Reminders.notify(applicationContext, item.id.toInt(), title(item, today), body(item, today, currency), item.id) }
        if (due.size > 6) {
            Reminders.notify(applicationContext, Int.MAX_VALUE, "${due.size - 6} more items need attention", "Open LifeDesk to see everything that's coming up.", null)
        }
        return Result.success()
    }

    private fun title(item: LifeItem, today: LocalDate) = "${item.category.emoji} ${item.countdown(today)}: ${item.title}"

    private fun body(item: LifeItem, today: LocalDate, currency: String): String = buildString {
        append(item.headline(today))
        item.amount?.let { append(" Amount: ${money(it, item.currency.ifBlank { currency })}.") }
        item.provider?.let { append(" Provider: $it.") }
    }
}
