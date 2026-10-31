package com.mandu.liquiddialer

import com.mandu.liquiddialer.ui.trNow
import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.ui.LdThemeHost
import com.mandu.liquiddialer.ui.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppState {
    var tab by mutableStateOf("recents")
    var contacts by mutableStateOf<List<Contact>>(emptyList())
    var recents by mutableStateOf<List<Recent>>(emptyList())
    var blocked by mutableStateOf<List<String>>(emptyList())
    var sims by mutableStateOf<List<SimInfo>>(emptyList())
    var isDefault by mutableStateOf(true)
    var dial by mutableStateOf("")
    var pendingCall by mutableStateOf<Pair<String, Boolean>?>(null)   // (number, video?) waiting for SIM choice
    var confirmNumber by mutableStateOf<String?>(null) // number waiting for call confirmation
    var undoInfo by mutableStateOf<Pair<String, android.telecom.PhoneAccountHandle?>?>(null) // number waiting out the undo window
    var permsTick by mutableIntStateOf(0)
    var birthdays by mutableStateOf<List<com.mandu.liquiddialer.data.Repo.Birthday>?>(null) // null = abhi load nahi hua
    var showOnboarding by mutableStateOf(false)
    var editContactRequest by mutableStateOf<Pair<Long?, String>?>(null) // contact editor asked from the call screen
}

val RUNTIME_PERMS: List<Pair<String, String>> = buildList {
    add(Manifest.permission.CALL_PHONE to "Call lagane ke liye")
    add(Manifest.permission.READ_PHONE_STATE to "SIM aur call ki halat jaanne ke liye")
    add(Manifest.permission.READ_PHONE_NUMBERS to "Apna number aur SIM pehchanne ke liye")
    add(Manifest.permission.READ_CALL_LOG to "Recents (call history) dikhane ke liye")
    add(Manifest.permission.WRITE_CALL_LOG to "Call history delete/clear karne ke liye")
    add(Manifest.permission.READ_CONTACTS to "Contacts dikhane aur search ke liye")
    add(Manifest.permission.WRITE_CONTACTS to "Contact add/edit/delete aur favourite ke liye")
    add(Manifest.permission.ANSWER_PHONE_CALLS to "Call uthane/kaatne ke liye")
    add(Manifest.permission.SEND_SMS to "Call kaat ke SMS bhejne ke liye")
    add(Manifest.permission.RECORD_AUDIO to "Call recording ke liye")
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS to "Incoming/missed call notification ke liye")
    if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT to "Bluetooth headset pe call sunne ke liye")
}

class MainActivity : ComponentActivity() {
    // direct video se pehle camera permission; deny par KOI call nahi lagegi (na voice, na video)
    private var pendingVideoCall: Pair<String, PhoneAccountHandle?>? = null
    private val cameraForVideo = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val p = pendingVideoCall; pendingVideoCall = null
        if (granted && p != null) call(p.first, p.second, video = true)
        else if (p != null) Toast.makeText(this, trNow("Camera permission nahi mili - video call nahi lagi"), Toast.LENGTH_LONG).show()
    }

    val state = AppState()
    private val handler = Handler(Looper.getMainLooper())
    private var pendingIntentAfterPerms: Intent? = null

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshDefault()
        askPermissions()
    }
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        state.permsTick++
        reload()
    }

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { reload() }
    }

    // SIM badli/add/nikli -> cached SIM list turant refresh, taaki dispatch stale handle na pakde
    private val subListener = object : android.telephony.SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            lifecycleScope.launch {
                if (has(Manifest.permission.READ_PHONE_STATE)) {
                    val sims = withContext(Dispatchers.IO) { Repo.sims(this@MainActivity) }
                    state.sims = sims
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LdApp.instance.scope.launch {
            val s0 = LdApp.settings.first()
            if (s0.defaultTab == "dialer") state.tab = "dialer"
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LdThemeHost { MainScreen(state, this) } }
        lifecycleScope.launch {
            val s = (application as LdApp).repo.flow.first()
            if (!s.onboarded) state.showOnboarding = true
        }
        handleIntent(intent)
        try {
            contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
            contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        } catch (_: Exception) { }
        try {
            @Suppress("DEPRECATION")
            if (has(Manifest.permission.READ_PHONE_STATE))
                getSystemService(android.telephony.SubscriptionManager::class.java).addOnSubscriptionsChangedListener(subListener)
        } catch (_: Exception) { }
    }


    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    override fun onResume() {
        super.onResume()
        refreshDefault()
        state.permsTick++
        reload()
        // provider write main thread par nahi - startup jank rokne ke liye background me
        lifecycleScope.launch(Dispatchers.IO) { Repo.markMissedRead(this@MainActivity) }
    }

    override fun onDestroy() {
        try { contentResolver.unregisterContentObserver(observer) } catch (_: Exception) { }
        try { getSystemService(android.telephony.SubscriptionManager::class.java).removeOnSubscriptionsChangedListener(subListener) } catch (_: Exception) { }
        super.onDestroy()
    }

    fun has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    fun refreshDefault() {
        state.isDefault = try { getSystemService(TelecomManager::class.java).defaultDialerPackage == packageName } catch (_: Exception) { false }
    }

    fun requestDefault() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val rm = getSystemService(RoleManager::class.java)
                if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)); return
                }
            } else {
                @Suppress("DEPRECATION")
                roleLauncher.launch(Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)); return
            }
        } catch (_: Exception) { }
        openDefaultAppsSettings()
        askPermissions()
    }

    fun screeningRoleHeld(): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        return try { getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING) } catch (_: Exception) { false }
    }

    fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= 29) try {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        } catch (_: Exception) { }
    }

    fun openDefaultAppsSettings() {
        try { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) } catch (_: Exception) { openAppSettings() }
    }

    fun askPermissions() {
        val missing = RUNTIME_PERMS.map { it.first }.filter { !has(it) }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }
    fun askPermission(p: String) { permLauncher.launch(arrayOf(p)) }

    fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    fun finishOnboarding() {
        state.showOnboarding = false
        lifecycleScope.launch { (application as LdApp).repo.set(K.onboarded, true) }
    }

    // Cada reload ganha uma geracao: um reload ANTIGO (observers/onResume em sequencia)
    // nunca sobrescreve o estado publicado pelo mais novo.
    private var reloadGen = 0

    fun reload() {
        val gen = ++reloadGen
        lifecycleScope.launch {
            // contacts permission revoke hote hi purani birthday state turant saaf (private data retain NAHI), chahe user kisi bhi tab pe ho
            if (!has(Manifest.permission.READ_CONTACTS) && state.birthdays?.isNotEmpty() == true) state.birthdays = emptyList()
            val s = LdApp.settings.value
            loadStartupSources(
                contacts = { if (has(Manifest.permission.READ_CONTACTS)) Repo.contacts(this@MainActivity, s.sortByLast) else emptyList() },
                recents = { if (has(Manifest.permission.READ_CALL_LOG)) Repo.recents(this@MainActivity, LdApp.settings.value.recentLimit) else emptyList() },
                blocked = { if (state.isDefault) Repo.blocked(this@MainActivity) else emptyList() },
                sims = { if (has(Manifest.permission.READ_PHONE_STATE)) Repo.sims(this@MainActivity) else emptyList() },
                publishRecents = { r -> if (gen == reloadGen) state.recents = r },
                publishRest = { c, b, sims ->
                    if (gen == reloadGen) {
                        state.contacts = c; state.blocked = b; state.sims = sims
                    }
                },
            )
        }
    }


    private fun handleIntent(i: Intent?) {
        i ?: return
        i.getStringExtra("tab")?.let { state.tab = it }
        i.getStringExtra("editContact")?.takeIf { it.isNotBlank() }?.let { num ->
            state.editContactRequest = try { Repo.contactIdFor(this, num) } catch (_: Exception) { null } to num
            i.removeExtra("editContact")
        }
        val number = i.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart ?: i.getStringExtra("number")
        when (i.action) {
            Intent.ACTION_DIAL, Intent.ACTION_VIEW -> if (number != null) { state.dial = number; state.tab = "dialer" } else if (i.action == Intent.ACTION_DIAL) state.tab = "dialer"
            Intent.ACTION_CALL, "com.mandu.liquiddialer.CALLBACK" -> if (number != null) call(number)
            Intent.ACTION_CALL_BUTTON -> state.tab = "recents"
        }
    }

    /** Place a real call, picking the SIM per settings. */
    fun call(number: String, account: PhoneAccountHandle? = null, video: Boolean = false) {
        var n = number.trim()
        if (n.isEmpty()) return
        if (!has(Manifest.permission.CALL_PHONE)) { askPermission(Manifest.permission.CALL_PHONE); Toast.makeText(this, trNow("Call permission do"), Toast.LENGTH_SHORT).show(); return }
        val s = LdApp.settings.value
        n = CallPrep.applyPrefix(n, s.autoPrefix)
        var acc = account
        // Startup me load hui cached SIM list; khali ho to tabhi query (dispatch pe main-thread wait kam)
        val sims = state.sims.ifEmpty { Repo.sims(this) }
        if (acc == null) {
            // SIM memory settings flow ke saath memory me rehti hai - dispatch pe runBlocking disk read NAHI
            val remembered = if (s.simMemory) s.simUse[Repo.key(n)] else null
            acc = CallPrep.chooseSim(null, s.defaultSim, remembered, sims) { it.handle.id }?.handle
            if (acc == null && CallPrep.needsSimPicker(sims.size)) { state.pendingCall = n to video; return }
        }
        if (s.simMemory && acc != null) {
            val hid = acc.id
            LdApp.instance.scope.launch { LdApp.instance.repo.rememberSim(Repo.key(n), hid) }
        }
        // direct video: pehle is SIM account ka video support check karo. Account saaf mana kare to
        // koi call nahi lagegi (na voice, na video) - silent fallback nahi. Verify na ho sake to
        // request bhejo aur connect par sachai dikhao (CallScreen notice).
        // direct video (ViLTE): pehle is SIM account ka video support check karo. Saaf mana kare ya
        // verify na ho sake to KOI call nahi lagegi (na voice, na video) - silent fallback nahi.
        if (video && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingVideoCall = n to acc
            cameraForVideo.launch(Manifest.permission.CAMERA)
            return
        }
        if (video) {
            when (canVideoCall(acc)) {
                true -> if (!CallManager.place(this, n, acc, true))
                    Toast.makeText(this, trNow("Video call nahi lag paayi - SIM/phone support check karo"), Toast.LENGTH_SHORT).show()
                false -> Toast.makeText(this, trNow(CallManager.VIDEO_UNSUPPORTED + ". Abhi koi call nahi lagi."), Toast.LENGTH_LONG).show()
                null -> Toast.makeText(this, trNow("Video support verify nahi ho paaya - isliye abhi koi call nahi lagi."), Toast.LENGTH_LONG).show()
            }
            return
        }
        if (!video && s.undoCall && n.firstOrNull()?.let { it.isDigit() || it == '+' } == true) {
            state.undoInfo = n to acc
            val token = Runnable { state.undoInfo = null; if (!CallManager.place(this, n, acc, video)) Toast.makeText(this, trNow("Call nahi lag paayi"), Toast.LENGTH_SHORT).show() }
            undoRunnable = token
            handler.postDelayed(token, 3000)
            return
        }
        if (!CallManager.place(this, n, acc, video)) Toast.makeText(this, trNow("Call nahi lag paayi"), Toast.LENGTH_SHORT).show()
    }

    /** Seedhi native video call (ViLTE) shuru karo. SIM account saaf mana kare to call nahi lagegi. */
    fun videoCall(number: String) = call(number, video = true)

    /** true = account video support batata hai; false = saaf mana; null = verify nahi ho saka. */
    fun canVideoCall(account: PhoneAccountHandle?): Boolean? {
        return try {
            val tm = getSystemService(TelecomManager::class.java)
            val handle = account ?: Repo.sims(this).firstOrNull()?.handle ?: return null
            tm.getPhoneAccount(handle)?.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING)
        } catch (_: Throwable) { null }
    }

    private var undoRunnable: Runnable? = null

    /** Tap-to-call from lists: respects the confirm-before-call setting. */
    fun callUi(number: String) {
        if (LdApp.settings.value.confirmCall) state.confirmNumber = number else call(number)
    }

    fun cancelUndo() { undoRunnable?.let { handler.removeCallbacks(it) }; undoRunnable = null; state.undoInfo = null }

    fun voicemail() {
        val custom = LdApp.settings.value.voicemail
        if (custom.isNotBlank()) { call(custom); return }
        try {
            @Suppress("MissingPermission")
            getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("voicemail", "", null), Bundle())
        } catch (_: Exception) { Toast.makeText(this, trNow("Voicemail number Settings me daalo"), Toast.LENGTH_LONG).show() }
    }
}

/** Pure call-prep decisions extracted for unit tests. */
internal object CallPrep {
    /** +91 prefix for 10-digit local numbers when enabled. */
    fun applyPrefix(n: String, enabled: Boolean): String =
        if (enabled && n.length == 10 && n.all { it.isDigit() }) "+91$n" else n

    /** SIM pick order: default SIM first, then remembered per-contact, then single-SIM; null = must ask. */
    fun <T> chooseSim(explicit: T?, defaultId: String?, rememberedId: String?, sims: List<T>, idOf: (T) -> String): T? {
        if (explicit != null) return explicit
        if (defaultId != null) sims.firstOrNull { idOf(it) == defaultId }?.let { return it }
        if (rememberedId != null) sims.firstOrNull { idOf(it) == rememberedId }?.let { return it }
        return if (sims.size == 1) sims[0] else null
    }

    /** Multi-SIM picker shows only when nothing was chosen and more than one SIM exists. */
    fun needsSimPicker(simCount: Int): Boolean = simCount > 1
}
