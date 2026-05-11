package com.example.whyopen

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class LogUploadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // 1. Initialize the HTTP Client with SSL Bypass for self-signed certificates
    private val client = createUnsafeOkHttpClient()

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // 2. CONFIGURATION (Replace these with your actual Splunk info)
    private val splunkUrl = "https://172.16.60.130:8088/services/collector/event"
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
            val lines = logFile.readLines()
            if (lines.isEmpty()) return Result.success()

            // 6. Splunk HEC can accept multiple events in one request 
            // if they are just concatenated JSON objects.
            val sb = StringBuilder()
            for (line in lines) {
                if (line.isBlank()) continue
                val escapedLine = line.replace("\"", "\\\"")
                sb.append("{\"event\": \"$escapedLine\"}\n")
            }
            
            val jsonPayload = sb.toString()
            if (jsonPayload.isBlank()) return Result.success()

            val body = jsonPayload.toRequestBody("application/json".toMediaType())

            // 7. Build the Request with the Security Token and Channel ID
            val request = Request.Builder()
                .url(splunkUrl)
                .header("Authorization", "Splunk $splunkToken")
                .header("X-Splunk-Request-Channel", java.util.UUID.randomUUID().toString())
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