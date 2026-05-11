package com.example.whyopen

import android.app.Service
import android.content.Intent
import android.app.NotificationManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.delay

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private lateinit var settingsManager: SettingsManager
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null

    companion object {
        var isOverlayShowing = false
        const val ACTION_START_MONITORING = "com.example.whyopen.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.whyopen.STOP_MONITORING"
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        val packageName = intent?.getStringExtra("PACKAGE_NAME") ?: return START_NOT_STICKY
        
        if (intent.action == ACTION_START_MONITORING) {
            val durationMins = intent.getIntExtra("DURATION_MINS", 0)
            if (durationMins > 0) {
                startMonitoring(packageName, durationMins)
            }
            return START_STICKY
        }

        // Safety check: Don't stack multiple overlays
        if (isOverlayShowing) {
             Log.d("WhyOpen", "Overlay already active, skipping start")
             return START_NOT_STICKY
        }

        showOverlay(packageName)
        return START_NOT_STICKY
    }

    private fun startMonitoring(packageName: String, minutes: Int) {
        SplunkLogger.log(this, "MONITORING", "Starting session monitoring for $packageName ($minutes mins)")
        Log.d("WhyOpen", "Starting background monitoring for $packageName ($minutes mins)")
        
        stopMonitoring() // Clear any existing

        // Optionally start foreground to ensure it stays alive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = FocusTimerManager.buildMonitoringNotification(this, packageName)
            startForeground(1, notification)
        }

        monitoringRunnable = Runnable {
            SplunkLogger.log(this, "MONITORING", "Session expired for $packageName. Closing app.")
            Log.d("WhyOpen", "Session expired for $packageName. Closing app.")
            FocusTimerManager.showTimeExpiredNotification(this, packageName)
            goHome()
        }
        
        handler.postDelayed(monitoringRunnable!!, minutes.toLong() * 60 * 1000)
    }

    private fun stopMonitoring() {
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        monitoringRunnable = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun showOverlay(packageName: String) {
        SplunkLogger.log(this, "OVERLAY", "Displaying mindfulness challenge for $packageName")
        isOverlayShowing = true

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            // Ensure the window can receive key focus for the Back button
            this.flags = this.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        }

        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            // Force focus for key events
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()

            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    Log.d("WhyOpen", "Back key detected, returning Home")
                    goHome()
                    true
                } else false
            }

            setContent {
                var penaltyExpiry by remember { mutableLongStateOf(settingsManager.getPenaltyExpiry(packageName)) }
                val isUnderPenalty = penaltyExpiry > System.currentTimeMillis()

                if (isUnderPenalty) {
                    PenaltyActiveScreen(
                        packageName = packageName,
                        remainingSeconds = ((penaltyExpiry - System.currentTimeMillis()) / 1000).toInt(),
                        onFinished = { goHome() }
                    )
                } else {
                    BehavioralOverlay(
                        packageName = packageName,
                        onCancel = { goHome() },
                        onUnlocked = { duration ->
                            SplunkLogger.log(this@OverlayService, "CHALLENGE", "User successfully unlocked $packageName for $duration mins")
                            settingsManager.setSessionExpiry(packageName, duration)
                            settingsManager.resetWrongAnswers(packageName)
                            
                            // Trigger monitoring
                            val monitorIntent = Intent(this@OverlayService, OverlayService::class.java).apply {
                                action = ACTION_START_MONITORING
                                putExtra("PACKAGE_NAME", packageName)
                                putExtra("DURATION_MINS", duration)
                            }
                            startService(monitorIntent)
                            
                            removeOverlay()
                            // Don't stopSelf() yet as we need to monitor
                        },
                        onPenaltyTriggered = { minutes ->
                            settingsManager.setPenalty(packageName, minutes)
                            penaltyExpiry = settingsManager.getPenaltyExpiry(packageName)
                            // State update will trigger recomposition into PenaltyActiveScreen
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            Log.e("WhyOpen", "Failed to add window", e)
            isOverlayShowing = false
        }
    }

    private fun removeOverlay() {
        composeView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (e: Exception) {}
            composeView = null
        }
        isOverlayShowing = false
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        removeOverlay()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        removeOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private class MyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val _viewModelStore = ViewModelStore()
        override val lifecycle: Lifecycle = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore = _viewModelStore
        fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
        fun performRestore(savedState: android.os.Bundle?) = savedStateRegistryController.performRestore(savedState)
    }
}

enum class Step { TIMING, CUSTOM_TIMING, QUESTION }

@Composable
fun BehavioralOverlay(
    packageName: String,
    onCancel: () -> Unit,
    onUnlocked: (Int) -> Unit,
    onPenaltyTriggered: (Int) -> Unit
) {
    var currentStep by remember { mutableStateOf(Step.TIMING) }
    var decisionTimer by remember { mutableIntStateOf(15) }
    var selectedDuration by remember { mutableIntStateOf(0) }
    var customTimeInput by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val qm = remember { QuestionManager() }
    val question by remember { mutableStateOf(qm.getRandomQuestion()) }
    var userAnswer by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val appName = remember {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName.split(".").last().replaceFirstChar { it.uppercase() }
        }
    }

    LaunchedEffect(currentStep) {
        if (currentStep == Step.TIMING) {
            while (decisionTimer > 0) {
                delay(1000)
                decisionTimer--
            }
            if (selectedDuration == 0) {
                onPenaltyTriggered(1) // 1 min block for timeout
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("WhyOpen?", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Normal)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Protecting your focus on $appName", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(40.dp))

            when (currentStep) {
                Step.TIMING -> {
                    Text("How long will you use this app?", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Decide in: $decisionTimer s", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(32.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(1, 3, 5, 15).forEach { mins ->
                            val label = if (mins == 1) "1 minute" else "$mins minutes"
                            OutlinedButton(
                                onClick = {
                                    selectedDuration = mins
                                    currentStep = Step.QUESTION
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(label, fontSize = 16.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { currentStep = Step.CUSTOM_TIMING },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Custom duration", fontSize = 16.sp)
                        }
                    }
                }
                
                Step.CUSTOM_TIMING -> {
                    Text("Duration in minutes", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customTimeInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) customTimeInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val mins = customTimeInput.toIntOrNull() ?: 0
                            if (mins > 0) {
                                selectedDuration = mins
                                currentStep = Step.QUESTION
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = customTimeInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Next Step")
                    }
                    TextButton(onClick = { currentStep = Step.TIMING }) {
                        Text("Go Back", color = Color.White.copy(alpha = 0.5f))
                    }
                }

                Step.QUESTION -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(question.text, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                            
                            if (question.answer != null) {
                                Spacer(modifier = Modifier.height(24.dp))
                                OutlinedTextField(
                                    value = userAnswer,
                                    onValueChange = { userAnswer = it; showError = false },
                                    label = { Text("Your answer") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = showError,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val isCorrect = question.answer == null || userAnswer.trim().equals(question.answer, ignoreCase = true)
                            if (isCorrect) {
                                onUnlocked(selectedDuration)
                            } else {
                                showError = true
                                val wrongs = settingsManager.incrementWrongAnswers(packageName)
                                if (wrongs >= 3) {
                                    onPenaltyTriggered(3) // 3 min block for 3 errors
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Unlock ($selectedDuration min)", fontWeight = FontWeight.Normal, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onCancel) {
                Text("Cancel and Go Home", color = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
fun PenaltyActiveScreen(packageName: String, remainingSeconds: Int, onFinished: () -> Unit) {
    var seconds by remember { mutableIntStateOf(remainingSeconds) }
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000)
            seconds--
        }
        onFinished()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Focus Penalty", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Too many failed intentionality checks for $packageName.", 
                color = Color.White.copy(alpha = 0.7f), 
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("$seconds", color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Normal)
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onFinished, 
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Close and Return Home")
            }
        }
    }
}
