package com.mandu.liquiddialer

import android.app.Application
import com.mandu.liquiddialer.data.LdSettings
import com.mandu.liquiddialer.data.SettingsRepo
import com.mandu.liquiddialer.telecom.CallNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LdApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repo: SettingsRepo

    override fun onCreate() {
        super.onCreate()
        instance = this
        BuildConfigHelper.version = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0" } catch (e: Exception) { "1.0" }
        repo = SettingsRepo(this)
        CallNotifier.createChannels(this)
        scope.launch { repo.flow.collect { settings.value = it } }
    }

    companion object {
        lateinit var instance: LdApp
        val settings = MutableStateFlow(LdSettings())
    }
}
