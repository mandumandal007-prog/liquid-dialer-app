package com.mandu.liquiddialer.data

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import com.mandu.liquiddialer.ui.trNow

/** App lock: SIRF app kholne ka access gate. Files/notification encryption nahi hai.
 *  Auth = device ka EXISTING fingerprint ya PIN/pattern/password (koi app PIN nahi banaya jata).
 *  Compatibility (developer.android.com, Oct 2026):
 *   - DEVICE_CREDENTIAL alone: API 30+; BIOMETRIC_STRONG|DEVICE_CREDENTIAL: API 28-29 pe unsupported.
 *   - API 30+: platform BiometricPrompt, BIOMETRIC_WEAK|DEVICE_CREDENTIAL (restricted combos me nahi).
 *   - API 26-29: KeyguardManager.createConfirmDeviceCredentialIntent; presence check isDeviceSecure()
 *     (androidx docs khud yahi recommend karte hain in versions par). Yahan fingerprint dikhna OEM par nirbhar.
 *  Session state kabhi disk pe nahi: process death = locked. */
object AppLock {
    const val CRED_REQ = 7317

    var unlockedThisSession = false
    var lastBackgroundAt = 0L
    var autoPrompt = true   // lock screen dikhte hi prompt khole; tests me band

    private var pendingCredCallback: ((Boolean) -> Unit)? = null

    var clock: () -> Long = { System.currentTimeMillis() }

    /** Test seam: production me real system auth. */
    var authenticator: (ComponentActivity, (Boolean) -> Unit) -> Unit = { act, cb -> realAuth(act, cb) }
    /** Test seam: production me real availability check. */
    var availability: (Context) -> Boolean = { ctx -> realAvailable(ctx) }

    fun shouldLock(enabled: Boolean, unlocked: Boolean, lastBgAt: Long, now: Long, delaySec: Int): Boolean {
        if (!enabled) return false
        if (!unlocked) return true                 // cold start / abhi tak unlock nahi
        if (lastBgAt <= 0) return false            // background hua hi nahi
        return now - lastBgAt >= delaySec * 1000L  // delaySec=0 (turant) = har background ke baad lock
    }

    fun onUnlocked() { unlockedThisSession = true; lastBackgroundAt = 0L }

    /** MainActivity.onActivityResult se forward; true = humne handle kiya. */
    fun handleCredentialResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != CRED_REQ) return false
        val cb = pendingCredCallback; pendingCredCallback = null
        cb?.invoke(resultCode == Activity.RESULT_OK)
        return true
    }

    fun resetForTests() {
        unlockedThisSession = false; lastBackgroundAt = 0L; autoPrompt = true
        pendingCredCallback = null
        clock = { System.currentTimeMillis() }
        authenticator = { act, cb -> realAuth(act, cb) }
        availability = { ctx -> realAvailable(ctx) }
    }

    private fun realAvailable(ctx: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= 30) {
            ctx.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
                ?.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
                android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
        } else {
            ctx.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
        }
    } catch (e: Exception) { false }

    private fun realAuth(act: ComponentActivity, cb: (Boolean) -> Unit) {
        if (!realAvailable(act)) { cb(false); return }
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(act)
                    .setTitle(trNow("Liquid Dialer lock"))
                    .setSubtitle(trNow("Fingerprint ya phone ka PIN/Pattern/Password se kholo"))
                    .setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                prompt.authenticate(CancellationSignal(), act.mainExecutor, object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult) { cb(true) }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { cb(false) }
                    // onAuthenticationFailed (galat finger) pe prompt khud khula rehta hai - cb nahi
                })
            } catch (e: Exception) { cb(false) }
        } else {
            // API 26-29: system credential screen (phone ka PIN/pattern/password)
            val km = act.getSystemService(KeyguardManager::class.java)
            val i = try { km?.createConfirmDeviceCredentialIntent(trNow("Liquid Dialer lock"), trNow("Fingerprint ya phone ka PIN/Pattern/Password se kholo")) } catch (e: Exception) { null }
            if (i == null) { cb(false); return }
            pendingCredCallback = cb
            try { act.startActivityForResult(i, CRED_REQ) } catch (e: Exception) { pendingCredCallback = null; cb(false) }
        }
    }
}
