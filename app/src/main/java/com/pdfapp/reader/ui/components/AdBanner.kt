package com.pdfapp.reader.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

/**
 * Composable that wraps an AdMob banner ad view.
 * Loads a test banner ad on creation.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // Ad is loaded once on factory; no update needed
            adView.resume()
        }
    )
}
