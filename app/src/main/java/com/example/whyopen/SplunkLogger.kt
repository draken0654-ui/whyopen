package com.example.whyopen

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object SplunkLogger {
    private const val LOG_FILE_NAME = "logs_pending.txt"

    fun log(context: Context, tag: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"
        
        // create the log time
        val logEntry = "[$timestamp] [$deviceInfo] [$tag]: $message\n"
        println("WhyOpen Log: $logEntry")

        try {
            // Internal storage file /data/user/0/com.example.why/files/logs_pending.txt
            val file = File(context.filesDir, LOG_FILE_NAME)

            // Append = true ensure we don't overwrite previous log
            file.appendText(logEntry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}