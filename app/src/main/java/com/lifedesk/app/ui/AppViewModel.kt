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
import com.lifedesk.app.domain.DocumentParser
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
data class Draft(val item: LifeItem, val confidence: Double? = null, val scanned: Boolean = false)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val lifeApp = app as LifeDeskApp
    private val repo = lifeApp.repository
    private val prefs = lifeApp.prefs

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
    fun processImage(file: File, replaceItemId: Long?, onReady: () -> Unit) {
        viewModelScope.launch {
            _scan.value = ScanState.Working("Reading your document…")
            val today = LocalDate.now()
            val currency = settings.value.currency
            val text = runCatching { TextReader.read(getApplication(), file) }.getOrNull()
            val existing = replaceItemId?.let { repo.item(it) }
            val draft = if (text.isNullOrBlank()) {
                val base = existing ?: LifeItem(title = "", category = Category.OTHER, currency = currency)
                existing?.imagePath?.takeIf { it != file.path }?.let { File(it).delete() }
                Draft(base.copy(imagePath = file.path), confidence = 0.0, scanned = true)
            } else {
                _scan.value = ScanState.Working("Understanding what matters…")
                val parsed = withContext(Dispatchers.Default) { DocumentParser.parse(text, today, currency) }
                val item = if (existing != null) {
                    existing.imagePath?.takeIf { it != file.path }?.let { File(it).delete() }
                    existing.copy(
                        imagePath = file.path, rawText = text,
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
                        contactPhone = parsed.phone, contactEmail = parsed.email, imagePath = file.path, rawText = text,
                    )
                }
                Draft(item, parsed.confidence, scanned = true)
            }
            _draft.value = draft
            _scan.value = ScanState.Idle
            onReady()
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
            onSaved(id)
        }
    }

    // ---------- item actions ----------

    fun complete(item: LifeItem) = viewModelScope.launch {
        repo.complete(item)
        _message.value = if (item.recurrence == com.lifedesk.app.data.Recurrence.NONE) "Done — archived" else "Done — next date scheduled"
    }

    fun snooze(item: LifeItem, days: Long) = viewModelScope.launch {
        repo.snooze(item, days)
        _message.value = "We'll remind you again in $days days"
    }

    fun markUsed(item: LifeItem) = viewModelScope.launch { repo.markUsed(item); _message.value = "Marked as used today" }

    fun delete(item: LifeItem) = viewModelScope.launch { repo.delete(item); _message.value = "Deleted" }

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
}
