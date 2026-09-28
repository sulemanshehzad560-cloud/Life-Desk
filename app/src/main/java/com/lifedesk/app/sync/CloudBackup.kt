package com.lifedesk.app.sync

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.lifedesk.app.auth.Accounts
import com.lifedesk.app.auth.AuthException
import com.lifedesk.app.data.LifeRepository
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Backs up the user's items (not document photos) to Firestore at users/{uid}.
 * Only the signed-in user can read or write their document (see firestore.rules).
 */
class CloudBackup(private val accounts: Accounts, private val repo: LifeRepository, private val docsDir: File) {

    private fun doc() = accounts.account.value?.let { a -> accounts.firestore?.collection("users")?.document(a.uid) }
        ?: throw AuthException("Sign in to use cloud backup.")

    suspend fun backup(): Int {
        val json = repo.exportJson(includeRawText = false)
        doc().set(mapOf("backup" to json, "updatedAt" to FieldValue.serverTimestamp(), "app" to "LifeDesk"), SetOptions.merge()).await()
        return repo.activeItems().size
    }

    /** Timestamp (ms) of the last cloud backup, or null if none. */
    suspend fun lastBackupAt(): Long? = runCatching { doc().get().await().getTimestamp("updatedAt")?.toDate()?.time }.getOrNull()

    suspend fun restore(): Int {
        val json = doc().get().await().getString("backup") ?: throw AuthException("No cloud backup found for this account yet.")
        return repo.importJson(json, docsDir)
    }
}
