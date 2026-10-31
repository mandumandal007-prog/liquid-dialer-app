package com.mandu.liquiddialer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.mandu.liquiddialer.data.AppLock
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
        // Pehla lock-state mirror se: 2 primitive SP reads (sub-ms), DataStore pehli composition ko block nahi karta.
        // Poora settings neeche async collect hota hai; mirror har save pe sync likha jata hai.
        try { val (me, md) = repo.lockMirror(); settings.value = LdSettings(appLock = me, appLockDelaySec = md) } catch (e: Exception) { }
        scope.launch { repo.flow.collect { settings.value = it } }
        trackAppBackground()
    }

    // Rotation-safe background detect: started-count 0 hone ke 700ms baad bhi 0 ho to hi background mana
    private var startedCount = 0
    private val bgHandler = Handler(Looper.getMainLooper())
    private var bgPosted: Runnable? = null
    private fun trackAppBackground() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(a: Activity) { if (++startedCount == 1) bgPosted?.let { bgHandler.removeCallbacks(it) } }
            override fun onActivityStopped(a: Activity) {
                if (--startedCount == 0) {
                    val r = Runnable { if (startedCount == 0) AppLock.lastBackgroundAt = AppLock.clock() }
                    bgPosted = r; bgHandler.postDelayed(r, 700)
                }
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) { }
            override fun onActivityResumed(a: Activity) { }
            override fun onActivityPaused(a: Activity) { }
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) { }
            override fun onActivityDestroyed(a: Activity) { }
        })
    }

    companion object {
        lateinit var instance: LdApp
        val settings = MutableStateFlow(LdSettings())
    }
}
