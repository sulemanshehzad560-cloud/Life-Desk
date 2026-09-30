package com.lifedesk.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.lifedesk.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob banners with Google's consent flow (UMP): the consent form is shown where the law requires it
 * (EEA, UK, Switzerland, some US states) before any ad is requested.
 * Debug builds (the GitHub APK) always use Google's test ad unit so no live ads are ever clicked while testing.
 */
object Ads {
    private const val LIVE_BANNER = "ca-app-pub-4940350948200557/5212692941"
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
    val bannerUnitId: String get() = if (BuildConfig.DEBUG) TEST_BANNER else LIVE_BANNER

    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    /** True once consent allows ads and the SDK is initialised. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** Call from the main activity's onCreate. Shows the consent form if required, then starts the SDK. */
    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    update(info)
                    if (info.canRequestAds()) start(activity)
                }
            },
            {
                // Couldn't reach the consent service; fall back to the last known consent state.
                update(info)
                if (info.canRequestAds()) start(activity)
            },
        )
        // Consent obtained in a previous session: start right away.
        update(info)
        if (info.canRequestAds()) start(activity)
    }

    /** "Ad privacy choices" in Settings, required when [privacyOptionsRequired] is true. */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            update(UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun update(info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(app) { _ready.value = true }
        }
    }
}
