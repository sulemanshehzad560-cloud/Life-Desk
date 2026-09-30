package com.lifedesk.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.lifedesk.app.ads.Ads
import com.lifedesk.app.ui.theme.Neon

/**
 * One inline adaptive banner, clearly labelled and spaced away from buttons (AdMob placement policy).
 * Renders nothing until consent allows ads and the SDK is ready.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ready by Ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    Column(modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text("Advertisement", style = MaterialTheme.typography.labelSmall, color = Neon.Faint, modifier = Modifier.padding(bottom = 4.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt()
            val context = LocalContext.current
            val adView = remember(width) {
                AdView(context).apply {
                    adUnitId = Ads.bannerUnitId
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
                    loadAd(AdRequest.Builder().build())
                }
            }
            DisposableEffect(adView) { onDispose { adView.destroy() } }
            AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
        }
    }
}
