package com.example.whyopen

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class LogUploadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // 1. Initialize the HTTP Client
    private val client = OkHttpClient()

    // 2. CONFIGURATION (Replace these with your actual Splunk info)
    private val splunkUrl = "https://YOUR_SPLUNK_INSTANCE:8088/services/collector/event"
    private val splunkToken = "1f9bf740-006b-4e5c-9bac-d52b05ce0d61"

    override suspend fun doWork(): Result {
        // 3. Locate the log file created in Step 3
        val logFile = File(applicationContext.filesDir, "logs_pending.txt")

        // 4. If no logs exist, we are done
        if (!logFile.exists() || logFile.length() == 0L) {
            return Result.success()
        }

        return try {
            // 5. Read all logs from the file
            val logData = logFile.readText()

            // 6. Create the JSON payload for Splunk
            // Note: We escape double quotes to avoid breaking the JSON format
            val escapedLogs = logData.replace("\"", "\\\"").replace("\n", "\\n")
            val jsonPayload = """{"event": "$escapedLogs"}"""

            val body = jsonPayload.toRequestBody("application/json".toMediaType())

            // 7. Build the Request with the Security Token
            val request = Request.Builder()
                .url(splunkUrl)
                .header("Authorization", "Splunk $splunkToken")
                .post(body)
                .build()

            // 8. Execute the upload
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    // 9. IMPORTANT: Only delete the logs if the upload worked!
                    logFile.delete()
                    Result.success()
                } else {
                    // Splunk rejected it (e.g., wrong token or server down)
                    // Result.retry() tells Android to try again later
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            // Something went wrong (like no internet)
            e.printStackTrace()
            Result.retry()
        }
    }
}