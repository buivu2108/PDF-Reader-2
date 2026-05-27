package com.pdfapp.reader.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private const val TAG = "AdManager"
private const val INTERSTITIAL_AD_UNIT_TEST = "ca-app-pub-3940256099942544/1033173712"
private const val FREQUENCY_CAP_MS = 60_000L

class AdManager {

    private var interstitialAd: InterstitialAd? = null
    private var lastShownAt: Long = 0L

    fun initializeAds(context: Context) {
        MobileAds.initialize(context) { initStatus ->
            Log.d(TAG, "AdMob initialized: ${initStatus.adapterStatusMap}")
        }
    }

    fun loadInterstitialAd(context: Context) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_TEST, request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            })
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        if (now - lastShownAt < FREQUENCY_CAP_MS) {
            Log.d(TAG, "Frequency cap active, skipping ad")
            onDismissed()
            return
        }
        val ad = interstitialAd ?: run {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastShownAt = System.currentTimeMillis()
                onDismissed()
                loadInterstitialAd(activity)
            }
        }
        ad.show(activity)
    }
}
