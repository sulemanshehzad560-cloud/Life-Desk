package com.lifedesk.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifedesk.app.LifeDeskApp
import com.lifedesk.app.data.Category
import com.lifedesk.app.data.LifeItem
import com.lifedesk.app.data.PriceRecord
import com.lifedesk.app.data.Settings
import com.lifedesk.app.auth.Account
import com.lifedesk.app.data.DateKind
import com.lifedesk.app.data.Recurrence
import com.lifedesk.app.domain.DocumentParser
import com.lifedesk.app.domain.ParsedDocument
import com.lifedesk.app.domain.QuickAdd
import com.lifedesk.app.domain.money
import com.lifedesk.app.domain.pretty
import com.lifedesk.app.widget.DueWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.lifedesk.app.notify.Reminders
import com.lifedesk.app.ocr.DocumentStore
import com.lifedesk.app.ocr.TextReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

sealed interface ScanState {
    data object Idle : ScanState
    data class Working(val message: String) : ScanState
    data class Failed(val message: String) : ScanState
}

/** The item being reviewed/edited before saving, plus how sure the scanner was. */
data class Draft(val item: LifeItem, val confidence: Double? = null, val scanned: Boolean = false, val parsed: ParsedDocument? = null)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val lifeApp = app as LifeDeskApp
    private val repo = lifeApp.repository
    private val prefs = lifeApp.prefs
    private val accounts = lifeApp.accounts
    private val cloud = lifeApp.cloud

    val account: StateFlow<Account?> = accounts.account
    val accountsAvailable: Boolean get() = accounts.isConfigured
    val googleSignInAvailable: Boolean get() = accounts.googleEnabled

    private val _busy = MutableStateFlow(false)
    /** True while an account / backup operation is running. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _locked = MutableStateFlow(prefs.settings.value.appLock)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    fun lock() { if (settings.value.appLock) _locked.value = true }
    fun unlock() { _locked.value = false }

    val items: StateFlow<List<LifeItem>> = repo.items.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val prices: StateFlow<List<PriceRecord>> = repo.prices.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings: StateFlow<Settings> = prefs.settings

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan: StateFlow<ScanState> = _scan.asStateFlow()

    private val _draft = MutableStateFlow<Draft?>(null)
    val draft: StateFlow<Draft?> = _draft.asStateFlow()

    /** One-shot navigation requests coming from outside the UI (notifications, share sheet). */
    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()
    fun consumeRoute() { _pendingRoute.value = null }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun consumeMessage() { _message.value = null }

    fun observeItem(id: Long): Flow<LifeItem?> = repo.observeItem(id)
    suspend fun priceHistory(id: Long): List<PriceRecord> = repo.pricesForItem(id)
    suspend fun previousPrice(category: Category, provider: String?): PriceRecord? = repo.previousPrice(category, provider)

    fun openItem(id: Long) { _pendingRoute.value = "item/$id" }

    // ---------- scanning ----------

    fun newPhotoFile(): File = DocumentStore.newPhotoFile(getApplication())

    fun importSharedImage(uri: Uri) {
        viewModelScope.launch {
            _scan.value = ScanState.Working("Saving your document…")
            val file = runCatching { DocumentStore.copyIn(getApplication(), uri) }.getOrElse {
                _scan.value = ScanState.Failed("Couldn't open that image.")
                return@launch
            }
            processImage(file, null) { _pendingRoute.value = "edit" }
        }
    }

    fun processPickedImage(uri: Uri, replaceItemId: Long?, onReady: () -> Unit) {
        viewModelScope.launch {
            _scan.value = ScanState.Working("Saving your document…")
            val file = runCatching { DocumentStore.copyIn(getApplication(), uri) }.getOrElse {
                _scan.value = ScanState.Failed("Couldn't open that image.")
                return@launch
            }
            processImage(file, replaceItemId, onReady)
        }
    }

    /** Photo → OCR → understand → draft. On OCR failure the user can still fill the form by hand. */
    fun processImage(file: File, replaceItemId: Long?, onReady: () -> Unit) = processFiles(listOf(file), replaceItemId, onReady)

    /** Several page images (e.g. a PDF): OCR each, keep the first page as the document photo. */
    private fun processFiles(files: List<File>, replaceItemId: Long?, onReady: () -> Unit) {
        viewModelScope.launch {
            _scan.value = ScanState.Working(if (files.size > 1) "Reading ${files.size} pages…" else "Reading your document…")
            val text = files.mapNotNull { f -> runCatching { TextReader.read(getApplication(), f) }.getOrNull() }
                .joinToString("\n\n").ifBlank { null }
            files.drop(1).forEach { it.delete() }
            _draft.value = buildDraft(text, files.first(), replaceItemId)
            _scan.value = ScanState.Idle
            onReady()
        }
    }

    private suspend fun buildDraft(text: String?, image: File?, replaceItemId: Long?): Draft {
        val today = LocalDate.now()
        val currency = settings.value.currency
        val existing = replaceItemId?.let { repo.item(it) }
        if (existing != null && image != null) existing.imagePath?.takeIf { it != image.path }?.let { File(it).delete() }
        if (text.isNullOrBlank()) {
            val base = existing ?: LifeItem(title = "", category = Category.OTHER, currency = currency)
            return Draft(base.copy(imagePath = image?.path ?: base.imagePath), confidence = 0.0, scanned = true)
        }
        _scan.value = ScanState.Working("Understanding what matters…")
        val parsed = withContext(Dispatchers.Default) { DocumentParser.parse(text, today, currency) }
        val item = if (existing != null) {
            existing.copy(
                imagePath = image?.path ?: existing.imagePath, rawText = text,
                dueDate = parsed.date ?: existing.dueDate,
                amount = parsed.amount ?: existing.amount,
                currency = parsed.currency ?: existing.currency,
                referenceNumber = parsed.referenceNumber ?: existing.referenceNumber,
                snoozedUntil = null, archived = false,
            )
        } else {
            LifeItem(
                title = parsed.title, category = parsed.category, kind = parsed.kind, provider = parsed.provider,
                amount = parsed.amount, currency = parsed.currency ?: currency, dueDate = parsed.date,
                recurrence = parsed.recurrence, referenceNumber = parsed.referenceNumber, asset = parsed.asset,
                contactPhone = parsed.phone, contactEmail = parsed.email, imagePath = image?.path, rawText = text,
            )
        }
        return Draft(item, parsed.confidence, scanned = true, parsed = parsed)
    }

    /** E-invoices, statements and policies as PDF. */
    fun processPdf(uri: Uri, replaceItemId: Long?, onReady: () -> Unit) {
        viewModelScope.launch {
            _scan.value = ScanState.Working("Opening the PDF…")
            val pages = runCatching { DocumentStore.pdfPages(getApplication(), uri) }.getOrElse {
                _scan.value = ScanState.Failed("Couldn't open that PDF. If it's password-protected, save an unlocked copy first.")
                return@launch
            }
            if (pages.isEmpty()) { _scan.value = ScanState.Failed("That PDF has no pages."); return@launch }
            processFiles(pages, replaceItemId, onReady)
        }
    }

    /** Text shared from Gmail/WhatsApp/SMS — e.g. a bill or booking confirmation email. */
    fun processText(text: String, onReady: () -> Unit) {
        viewModelScope.launch {
            _scan.value = ScanState.Working("Reading the message…")
            _draft.value = buildDraft(text, null, null)
            _scan.value = ScanState.Idle
            onReady()
        }
    }

    fun importSharedPdf(uri: Uri) = processPdf(uri, null) { _pendingRoute.value = "edit" }
    fun importSharedText(text: String) = processText(text) { _pendingRoute.value = "edit" }

    /** "DEWA bill 450 dirhams due next Friday" → saved reminder. Returns false if nothing usable was understood. */
    fun quickAdd(text: String): Boolean {
        if (text.isBlank()) return false
        val p = QuickAdd.parse(text, LocalDate.now(), settings.value.currency)
        val item = LifeItem(
            title = p.title, category = p.category, kind = p.kind, provider = p.provider, amount = p.amount,
            currency = p.currency ?: settings.value.currency, dueDate = p.date, recurrence = p.recurrence,
            referenceNumber = p.referenceNumber, asset = p.asset,
        )
        viewModelScope.launch {
            repo.save(item)
            _message.value = "Added: ${item.title}" + (item.dueDate?.let { " · ${it.pretty()}" } ?: " (no date — tap to add one)") +
                (item.amount?.let { " · ${money(it, item.currency)}" } ?: "")
            changed()
        }
        return true
    }

    /** One reminder per instalment, e.g. the 4 cheques of a tenancy contract. */
    fun saveSchedule(base: LifeItem, schedule: List<Pair<LocalDate, Double>>, onSaved: () -> Unit) {
        viewModelScope.launch {
            val title = base.title.trim().ifEmpty { base.provider ?: base.category.label }
            schedule.forEachIndexed { i, (date, amount) ->
                repo.save(base.copy(
                    id = 0, title = "$title — payment ${i + 1}/${schedule.size}", dueDate = date, amount = amount,
                    kind = DateKind.PAYMENT_DUE, recurrence = Recurrence.NONE,
                    // Keep the document photo only on the first instalment so deleting one doesn't remove it for all.
                    imagePath = if (i == 0) base.imagePath else null,
                ))
            }
            _draft.value = null
            _message.value = "Created ${schedule.size} payment reminders"
            changed()
            onSaved()
        }
    }

    fun dismissScanError() { _scan.value = ScanState.Idle }

    fun startManual(category: Category = Category.OTHER) {
        _draft.value = Draft(LifeItem(title = "", category = category, kind = category.defaultKind,
            recurrence = category.defaultRecurrence, currency = settings.value.currency))
    }

    fun startEdit(item: LifeItem) { _draft.value = Draft(item) }

    fun discardDraft() {
        val d = _draft.value ?: return
        // A freshly scanned photo that was never saved should not linger on disk.
        if (d.scanned && d.item.id == 0L) d.item.imagePath?.let { File(it).delete() }
        _draft.value = null
    }

    fun saveDraft(item: LifeItem, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.save(item.copy(title = item.title.trim().ifEmpty { item.provider ?: item.category.label }))
            _draft.value = null
            _message.value = "Added to your LifeDesk"
            changed()
            onSaved(id)
        }
    }

    // ---------- item actions ----------

    fun complete(item: LifeItem) = viewModelScope.launch {
        repo.complete(item)
        changed()
        _message.value = if (item.recurrence == com.lifedesk.app.data.Recurrence.NONE) "Done — archived" else "Done — next date scheduled"
    }

    fun snooze(item: LifeItem, days: Long) = viewModelScope.launch {
        repo.snooze(item, days)
        changed()
        _message.value = "We'll remind you again in $days days"
    }

    fun markUsed(item: LifeItem) = viewModelScope.launch { repo.markUsed(item); _message.value = "Marked as used today" }

    fun delete(item: LifeItem) = viewModelScope.launch { repo.delete(item); changed(); _message.value = "Deleted" }

    // ---------- settings & data ----------

    fun updateSettings(transform: (Settings) -> Settings) {
        val oldHour = settings.value.reminderHour
        prefs.update(transform)
        if (settings.value.reminderHour != oldHour) Reminders.schedule(getApplication(), settings.value.reminderHour, replace = true)
    }

    fun sendTestReminder() = Reminders.sendTest(getApplication())

    fun loadSampleData() = viewModelScope.launch {
        repo.loadSampleData(settings.value.currency)
        _message.value = "Sample data loaded"
    }

    fun clearAll() = viewModelScope.launch {
        repo.clearAll(DocumentStore.dir(getApplication()))
        _message.value = "All data deleted"
    }

    fun exportTo(uri: Uri) = viewModelScope.launch {
        runCatching {
            val json = repo.exportJson()
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("no stream")
            }
        }.onSuccess { _message.value = "Backup saved" }.onFailure { _message.value = "Backup failed: ${it.message}" }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        runCatching {
            val json = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("no stream")
            }
            repo.importJson(json, DocumentStore.dir(getApplication()))
        }.onSuccess { _message.value = "Restored $it items" }.onFailure { _message.value = "Restore failed: ${it.message}" }
    }

    // ---------- after every change: widget + debounced auto backup ----------

    private var backupJob: Job? = null

    private fun changed() {
        DueWidget.refresh(getApplication())
        if (account.value == null || !settings.value.autoBackup) return
        backupJob?.cancel()
        backupJob = viewModelScope.launch {
            delay(5_000)
            runCatching { cloud.backup() }.onSuccess { prefs.update { s -> s.copy(lastBackupAt = System.currentTimeMillis()) } }
        }
    }

    // ---------- accounts ----------

    private fun accountAction(success: String?, block: suspend () -> Unit, after: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { block() }
                .onSuccess { success?.let { _message.value = it }; after() }
                .onFailure { _message.value = it.message ?: "Something went wrong." }
            _busy.value = false
        }
    }

    fun signUp(name: String, email: String, password: String, onDone: () -> Unit) = accountAction(
        "Account created. We've sent a verification link to $email.",
        { accounts.signUp(name, email, password); if (name.isNotBlank()) updateSettings { it.copy(name = name.trim()) } },
        onDone,
    )

    fun signIn(email: String, password: String, onDone: () -> Unit) = accountAction("Signed in", {
        accounts.signIn(email, password)
        offerRestore()
    }, onDone)

    fun signInWithGoogle(activity: android.app.Activity, onDone: () -> Unit) = accountAction("Signed in with Google", {
        accounts.signInWithGoogle(activity)
        account.value?.name?.let { n -> if (settings.value.name.isBlank()) updateSettings { it.copy(name = n.substringBefore(' ')) } }
        offerRestore()
    }, onDone)

    fun sendPasswordReset(email: String) = accountAction("Password reset link sent to $email. Check your inbox (and spam).", {
        accounts.sendPasswordReset(email)
    })

    fun resendVerification() = accountAction("Verification email sent again.", { accounts.resendVerification() })

    fun refreshAccount() = viewModelScope.launch { accounts.refresh() }

    fun signOut() {
        accounts.signOut()
        _message.value = "Signed out. Your data stays on this phone."
    }

    fun deleteAccount() = accountAction("Account and cloud backup deleted. Data on this phone is kept.", { accounts.deleteAccount() })

    /** After signing in on a phone with no items, pull the cloud backup automatically. */
    private suspend fun offerRestore() {
        if (repo.activeItems().isNotEmpty()) return
        runCatching { cloud.restore() }.onSuccess { n -> if (n > 0) _message.value = "Welcome back — restored $n items from your account" }
    }

    fun backupNow() = accountAction(null, {
        val n = cloud.backup()
        prefs.update { it.copy(lastBackupAt = System.currentTimeMillis()) }
        _message.value = "Backed up $n items to your account"
    })

    fun restoreFromCloud() = accountAction(null, {
        val n = cloud.restore()
        _message.value = "Restored $n items from your account"
        DueWidget.refresh(getApplication())
    })
}
