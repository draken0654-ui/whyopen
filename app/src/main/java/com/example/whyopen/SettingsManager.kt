package com.example.whyopen

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

data class DayStats(
    val dayLabel: String, // e.g., Mon
    val dateLabel: String, // e.g., 12/05
    val fullDate: String, // e.g., Monday, May 12
    val intensity: Float, // 0f to 1f
    val focusScore: Int
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("whyopen_v2_prefs", Context.MODE_PRIVATE)

    private fun getTodayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // --- Blocked Apps Management ---
    fun setAppBlocked(packageName: String, blocked: Boolean) {
        val blockedApps = getBlockedApps().toMutableSet()
        if (blocked) blockedApps.add(packageName) else blockedApps.remove(packageName)
        prefs.edit().putStringSet("blocked_apps", blockedApps).apply()
    }

    fun getBlockedApps(): Set<String> = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

    fun isAppBlocked(packageName: String): Boolean {
        // If user explicitly blocked this app, return true
        if (getBlockedApps().contains(packageName)) return true
        
        // If app is under temporary penalty, return true even if not in blocked list
        val penaltyExpiry = prefs.getLong("penalty_$packageName", 0)
        return System.currentTimeMillis() < penaltyExpiry
    }

    // --- Penalty System ---
    fun setPenalty(packageName: String, minutes: Int) {
        val expiry = System.currentTimeMillis() + (minutes * 60 * 1000)
        prefs.edit().putLong("penalty_$packageName", expiry).apply()
    }

    fun getPenaltyExpiry(packageName: String): Long = prefs.getLong("penalty_$packageName", 0)

    fun incrementWrongAnswers(packageName: String): Int {
        val count = prefs.getInt("wrong_$packageName", 0) + 1
        prefs.edit().putInt("wrong_$packageName", count).apply()
        return count
    }

    fun resetWrongAnswers(packageName: String) {
        prefs.edit().remove("wrong_$packageName").apply()
    }

    // --- Session Management ---
    fun setSessionExpiry(packageName: String, minutes: Int) {
        val expiry = System.currentTimeMillis() + (minutes * 60 * 1000)
        prefs.edit().putLong("session_$packageName", expiry).apply()
        incrementUnlockCount()
    }

    fun isSessionActive(packageName: String): Boolean {
        val expiry = prefs.getLong("session_$packageName", 0)
        return System.currentTimeMillis() < expiry
    }

    fun clearAllSessions() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("session_") }.forEach { editor.remove(it) }
        editor.apply()
    }

    // --- Analytics & Stats ---
    fun incrementBlockedAttempt(packageName: String) {
        val today = getTodayDate()
        val key = "blocked_$today"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
        
        val appKey = "blocked_app_$packageName"
        prefs.edit().putInt(appKey, prefs.getInt(appKey, 0) + 1).apply()
        
        updateStreak()
    }

    private fun incrementUnlockCount() {
        val today = getTodayDate()
        val key = "unlocked_$today"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun getBlockedAttemptsToday(): Int = prefs.getInt("blocked_${getTodayDate()}", 0)
    fun getFocusStreak(): Int = prefs.getInt("focus_streak", 0)
    fun getTotalSavedTime(): Int = getBlockedAttemptsToday() * 5

    fun getDailyFocusScore(dateKey: String): Int {
        val blocked = prefs.getInt("blocked_$dateKey", 0).toFloat()
        val unlocked = prefs.getInt("unlocked_$dateKey", 0).toFloat()
        if (blocked + unlocked == 0f) return 0
        return ((blocked / (blocked + unlocked)) * 100).toInt()
    }
    
    fun getDailyFocusScore(): Int = getDailyFocusScore(getTodayDate())

    private fun updateStreak() {
        val today = getTodayDate()
        val lastActive = prefs.getString("last_active_date", "")
        if (lastActive != today) {
            val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val streak = if (lastActive == yesterday) prefs.getInt("focus_streak", 0) + 1 else 1
            prefs.edit().putInt("focus_streak", streak).putString("last_active_date", today).apply()
        }
    }

    fun getMostDistractingApps(): List<Pair<String, Int>> {
        return getBlockedApps().map { it to prefs.getInt("blocked_app_$it", 0) }
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(5)
    }

    fun getWeeklyActivity(): List<DayStats> {
        val stats = mutableListOf<DayStats>()
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val daySdf = SimpleDateFormat("EEE", Locale.getDefault()) // Mon, Tue...
        val shortDateSdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val fullDateSdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        
        val counts = mutableListOf<Float>()
        val details = mutableListOf<Triple<String, String, String>>()
        
        for (i in 6 downTo 0) {
            val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateKey = dateSdf.format(calendar.time)
            
            counts.add(prefs.getInt("blocked_$dateKey", 0).toFloat())
            details.add(Triple(
                daySdf.format(calendar.time),
                shortDateSdf.format(calendar.time),
                fullDateSdf.format(calendar.time)
            ))
        }
        
        val max = counts.maxOrNull() ?: 1f
        for (i in 0 until 7) {
            val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(6 - i)) }
            val dateKey = dateSdf.format(calendar.time)
            val intensity = if (max == 0f) 0f else counts[i] / max
            val score = getDailyFocusScore(dateKey)
            
            stats.add(DayStats(details[i].first, details[i].second, details[i].third, intensity, score))
        }
        
        return stats
    }
}
