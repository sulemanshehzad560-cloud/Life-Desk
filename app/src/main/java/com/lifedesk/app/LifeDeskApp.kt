package com.lifedesk.app

import android.app.Application
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
        Reminders.createChannel(this)
        Reminders.schedule(this, prefs.settings.value.reminderHour, replace = false)
    }
}
