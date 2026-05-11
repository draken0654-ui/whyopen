package com.example.whyopen

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit

class WhyOpenApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupSplunkSync()
    }

    private fun setupSplunkSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<LogUploadWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SplunkLogUpload",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWorkRequest
        )
    }
}
