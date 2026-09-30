package com.lifedesk.app.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.lifedesk.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class Account(
    val uid: String,
    val email: String?,
    val name: String?,
    val emailVerified: Boolean,
    val provider: String,
)

class AuthException(message: String) : Exception(message)

/**
 * Accounts via Firebase Authentication:
 *  - email + password sign-up (a verification email is sent by Firebase),
 *  - "forgot password" (Firebase emails a reset link),
 *  - Continue with Google (Credential Manager, falling back to the Firebase browser flow).
 * Firebase is configured from build-time values; without them the app runs in offline mode.
 */
class Accounts(private val context: Context) {

    val isConfigured: Boolean = BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
        BuildConfig.FIREBASE_APP_ID.isNotBlank() && BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

    val googleEnabled: Boolean get() = isConfigured && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    private val app: FirebaseApp? = if (!isConfigured) null else runCatching {
        FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .build(),
        )
    }.getOrNull()

    private val auth: FirebaseAuth? = app?.let { FirebaseAuth.getInstance(it) }
    val firestore: FirebaseFirestore? get() = app?.let { FirebaseFirestore.getInstance(it) }

    private val _account = MutableStateFlow(current())
    val account: StateFlow<Account?> = _account.asStateFlow()

    init {
        auth?.addAuthStateListener { _account.value = current() }
    }

    private fun current(): Account? = auth?.currentUser?.let { u ->
        Account(
            uid = u.uid,
            email = u.email,
            name = u.displayName,
            emailVerified = u.isEmailVerified || u.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID },
            provider = if (u.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) "google" else "password",
        )
    }

    private fun requireAuth(): FirebaseAuth = auth ?: throw AuthException("Accounts aren't set up in this build yet. You can keep using LifeDesk offline.")

    suspend fun signUp(name: String, email: String, password: String) = guard {
        val result = requireAuth().createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build())?.await()
        result.user?.sendEmailVerification()?.await()
        refresh()
    }

    suspend fun signIn(email: String, password: String) = guard {
        requireAuth().signInWithEmailAndPassword(email.trim(), password).await()
        refresh()
    }

    suspend fun sendPasswordReset(email: String) = guard {
        requireAuth().sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun resendVerification() = guard {
        requireAuth().currentUser?.sendEmailVerification()?.await()
    }

    /** Re-reads the user from Firebase, e.g. after they tapped the link in the verification email. */
    suspend fun refresh() {
        runCatching { auth?.currentUser?.reload()?.await() }
        _account.value = current()
    }

    /**
     * Continue with Google: first the phone's native Google account sheet (Credential Manager). If that fails for any
     * reason (some phones report a rejected app, or an unregistered signing key, only as "cancelled"), fall back to
     * Google's sign-in page in a browser tab via Firebase, which doesn't depend on the app's signing key.
     */
    suspend fun signInWithGoogle(activity: Activity) = guard {
        if (!googleEnabled) throw AuthException("Google sign-in isn't configured in this build.")
        val token = runCatching { nativeGoogleToken(activity) }.getOrNull()
        if (token != null) {
            requireAuth().signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        } else {
            val a = requireAuth()
            try {
                a.pendingAuthResult?.await()
                    ?: a.startActivityForSignInWithProvider(activity, OAuthProvider.newBuilder("google.com").build()).await()
            } catch (e: Exception) {
                if (e.message?.contains("cancel", ignoreCase = true) == true) throw AuthException("Sign-in cancelled.")
                throw e
            }
        }
        refresh()
    }

    private suspend fun nativeGoogleToken(activity: Activity): String {
        val manager = CredentialManager.create(activity)
        suspend fun ask(option: androidx.credentials.CredentialOption) =
            manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val response = try {
            ask(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build())
        } catch (_: Exception) {
            ask(
                com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
            )
        }
        val credential = response.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException("Unexpected sign-in response.")
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    fun signOut() {
        auth?.signOut()
        _account.value = null
    }

    suspend fun deleteAccount() = guard {
        val user = requireAuth().currentUser ?: return@guard
        firestore?.collection("users")?.document(user.uid)?.delete()?.await()
        user.delete().await()
        _account.value = null
    }

    /** Converts Firebase errors into messages people understand. */
    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: AuthException) {
        throw e
    } catch (e: FirebaseAuthWeakPasswordException) {
        throw AuthException("Choose a stronger password (at least 6 characters, ideally 10+ with numbers).")
    } catch (e: FirebaseAuthUserCollisionException) {
        throw AuthException("An account with this email already exists. Sign in or reset your password.")
    } catch (e: FirebaseAuthInvalidUserException) {
        throw AuthException("No account found for this email.")
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        throw AuthException("Email or password is incorrect.")
    } catch (e: FirebaseAuthRecentLoginRequiredException) {
        throw AuthException("For security, sign out and sign in again, then retry.")
    } catch (e: Exception) {
        throw AuthException(e.message ?: "Something went wrong. Check your connection and try again.")
    }
}
