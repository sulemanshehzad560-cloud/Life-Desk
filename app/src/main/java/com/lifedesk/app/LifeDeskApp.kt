package com.lifedesk.app

import android.app.Application
import kotlinx.coroutines.launch
import com.lifedesk.app.auth.Accounts
import com.lifedesk.app.data.LifeDatabase
import com.lifedesk.app.data.LifeRepository
import com.lifedesk.app.data.Prefs
import com.lifedesk.app.notify.Reminders
import com.lifedesk.app.ocr.DocumentStore
import com.lifedesk.app.sync.CloudBackup

class LifeDeskApp : Application() {
    lateinit var repository: LifeRepository
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var accounts: Accounts
        private set
    lateinit var cloud: CloudBackup
        private set

    override fun onCreate() {
        super.onCreate()
        repository = LifeRepository(LifeDatabase.create(this).dao())
        prefs = Prefs(this)
        accounts = Accounts(this)
        cloud = CloudBackup(accounts, repository, DocumentStore.dir(this))
        // One-time cleanup of demo items loaded by older versions.
        val sp = getSharedPreferences("lifedesk", MODE_PRIVATE)
        if (!sp.getBoolean("sampleCleanupDone", false)) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching { repository.removeLegacySampleData() }
                sp.edit().putBoolean("sampleCleanupDone", true).apply()
                com.lifedesk.app.widget.DueWidget.refresh(this@LifeDeskApp)
            }
        }
        Reminders.createChannel(this)
        Reminders.schedule(this, prefs.settings.value.reminderHour, replace = false)
    }
}
