package com.lifedesk.app

import android.app.Application
import com.lifedesk.app.data.LifeDatabase
import com.lifedesk.app.data.LifeRepository
import com.lifedesk.app.data.Prefs
import com.lifedesk.app.notify.Reminders

class LifeDeskApp : Application() {
    lateinit var repository: LifeRepository
        private set
    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        repository = LifeRepository(LifeDatabase.create(this).dao())
        prefs = Prefs(this)
        Reminders.createChannel(this)
        Reminders.schedule(this, prefs.settings.value.reminderHour, replace = false)
    }
}
