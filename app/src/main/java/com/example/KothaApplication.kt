package com.example

import android.app.Application
import android.content.Context
import com.example.settings.SettingsRepository
import com.example.util.AccessibilityKeeper
import com.example.util.DiagnosticsLog

class KothaApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var accessibilityKeeper: AccessibilityKeeper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Crash Safety: Restore audio stream volumes if app crashed or was killed while dictating
        com.example.util.AudioBeepSuppressor.restoreCrashSafetyIfNeeded(this)

        settingsRepository = SettingsRepository(this)
        accessibilityKeeper = AccessibilityKeeper(this, settingsRepository)

        DiagnosticsLog.log(
            DiagnosticsLog.Category.SYSTEM,
            "KothaApplication initialized"
        )
    }

    companion object {
        lateinit var instance: KothaApplication
            private set

        fun getAppContext(): Context = instance.applicationContext
    }
}
