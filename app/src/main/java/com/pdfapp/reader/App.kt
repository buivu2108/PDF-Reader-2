package com.pdfapp.reader

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

private const val TAG = "PdfReaderApp"

class App : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        myApplicationInstance = this
        appContainer = AppContainer(this)

        // Initialize PdfBox resources (required before any PDF manipulation)
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "PdfBox init failed", e)
        }

        // Initialize AdMob (delegated to AdManager to avoid blocking main thread)
        appContainer.adManager.initializeAds(this)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var myApplicationInstance: App

        fun get(): App {
            return myApplicationInstance
        }
    }
}
