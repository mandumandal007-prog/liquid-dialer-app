package com.mandu.liquiddialer

import com.mandu.liquiddialer.ui.trNow
import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
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
    // recents empty =/= history khaali: load chal raha hai ya query fail hui ho sakti hai - alag state
    var recentsLoaded by mutableStateOf(false)
    var recentsError by mutableStateOf(false)
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
    var launcherNewContactRequest by mutableStateOf(false)
    var editContactRequest by mutableStateOf<Pair<Long?, String>?>(null) // contact editor asked from the call screen
    var numberChoiceRequest by mutableStateOf<Pair<Long, String>?>(null) // widget favorite multi-number: chooser (contactId, naam)
    var numberChoiceCalls by mutableStateOf(false)
    var numberChoiceNonce by mutableStateOf(0L) // chooser list refresh trigger (contact click-time badla ho to)
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
    private val lockState = mutableStateOf(false)
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

    private val providerReloads by lazy {
        ProviderReloads(handler) { contactsChanged ->
            clearUnavailableSources()
            recentReload.request()
            if (contactsChanged) contactReload.request()
        }
    }

    // SIM badli/add/nikli -> cached SIM list turant refresh, taaki dispatch stale handle na pakde
    private val subListener = object : android.telephony.SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            lifecycleScope.launch { simReload.request() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LdApp.instance.scope.launch {
            val s0 = LdApp.settings.first()
            if (s0.defaultTab == "dialer") state.tab = "dialer"
            // Opt-in hafte-ka auto-backup (device-local); kabhi bhi app start ko block nahi karta
            runCatching { com.mandu.liquiddialer.data.AutoBackup.maybe(this@MainActivity) }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Cold-start lock decision PEHLE composition ke (settings LdApp me sync load ho chuki)
        run {
            val sNow = LdApp.settings.value
            lockState.value = com.mandu.liquiddialer.data.AppLock.shouldLock(sNow.appLock, com.mandu.liquiddialer.data.AppLock.unlockedThisSession, com.mandu.liquiddialer.data.AppLock.lastBackgroundAt, com.mandu.liquiddialer.data.AppLock.clock(), sNow.appLockDelaySec)
        }
        setContent {
            LdThemeHost {
                val s = LdApp.settings.collectAsState().value
                LaunchedEffect(s.appLock, s.appLockDelaySec) {
                    lockState.value = com.mandu.liquiddialer.data.AppLock.shouldLock(s.appLock, com.mandu.liquiddialer.data.AppLock.unlockedThisSession, com.mandu.liquiddialer.data.AppLock.lastBackgroundAt, com.mandu.liquiddialer.data.AppLock.clock(), s.appLockDelaySec)
                }
                // Locked me MainScreen compose hi nahi hota - content flash ka sawaal hi nahi
                if (s.appLock && lockState.value) com.mandu.liquiddialer.ui.AppLockScreen(onUnlock = { unlockNow() })
                else MainScreen(state, this)
            }
        }
        lifecycleScope.launch {
            val s = (application as LdApp).repo.flow.first()
            if (!s.onboarded) state.showOnboarding = true
        }
        handleIntent(intent)
        providerReloads.register(contentResolver)
        try {
            @Suppress("DEPRECATION")
            if (has(Manifest.permission.READ_PHONE_STATE))
                getSystemService(android.telephony.SubscriptionManager::class.java).addOnSubscriptionsChangedListener(subListener)
        } catch (_: Exception) { }
    }


    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    @Deprecated("KeyguardManager credential flow (API 26-29) ke liye chahiye")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        com.mandu.liquiddialer.data.AppLock.handleCredentialResult(requestCode, resultCode)
    }

    private fun unlockNow() {
        com.mandu.liquiddialer.data.AppLock.authenticator(this) { ok ->
            runOnUiThread { if (ok) { com.mandu.liquiddialer.data.AppLock.onUnlocked(); lockState.value = false } }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-lock (timeout) + recents preview protection: appLock ON = FLAG_SECURE hamesha
        val sNow = LdApp.settings.value
        lockState.value = com.mandu.liquiddialer.data.AppLock.shouldLock(sNow.appLock, com.mandu.liquiddialer.data.AppLock.unlockedThisSession, com.mandu.liquiddialer.data.AppLock.lastBackgroundAt, com.mandu.liquiddialer.data.AppLock.clock(), sNow.appLockDelaySec)
        if (sNow.appLock) window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        refreshDefault()
        com.mandu.liquiddialer.widget.FavWidgetProvider.refresh(this)
        state.permsTick++
        reload()
        // provider write main thread par nahi - startup jank rokne ke liye background me
        lifecycleScope.launch(Dispatchers.IO) { Repo.markMissedRead(this@MainActivity) }
    }

    override fun onDestroy() {
        providerReloads.close(contentResolver)
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

    // Source-local queues: a slow contacts query cannot hold up a newer Recents refresh.
    private val recentReload: ReloadQueue by lazy { ReloadQueue(lifecycleScope) { gen ->
        var failed = false
        val rows = withContext(Dispatchers.IO) {
            if (!has(Manifest.permission.READ_CALL_LOG)) emptyList() else try {
                Repo.recents(this@MainActivity, LdApp.settings.value.recentLimit, strict = true)
            } catch(e: kotlinx.coroutines.CancellationException) { throw e }
            catch(e: Exception) { failed=true; emptyList() }
        }
        publishRecentLoad(state, rows, failed, gen, recentReload.generation)
    } }
    private val contactReload: ReloadQueue by lazy { ReloadQueue(lifecycleScope) { gen ->
        val rows = withContext(Dispatchers.IO) {
            if(has(Manifest.permission.READ_CONTACTS)) Repo.contacts(this@MainActivity,LdApp.settings.value.sortByLast) else emptyList()
        }
        if(gen==contactReload.generation) state.contacts=rows
    } }
    private val blockedReload: ReloadQueue by lazy { ReloadQueue(lifecycleScope) { gen ->
        val rows = withContext(Dispatchers.IO) { if(state.isDefault) Repo.blocked(this@MainActivity) else emptyList() }
        if(gen==blockedReload.generation) state.blocked=rows
    } }
    private val simReload: ReloadQueue by lazy { ReloadQueue(lifecycleScope) { gen ->
        val rows = withContext(Dispatchers.IO) { if(has(Manifest.permission.READ_PHONE_STATE)) Repo.sims(this@MainActivity) else emptyList() }
        if(gen==simReload.generation) state.sims=rows
    } }

    private fun clearUnavailableSources() {
        if(!has(Manifest.permission.READ_CONTACTS)) {
            state.contacts=emptyList()
            if(state.birthdays?.isNotEmpty()==true) state.birthdays=emptyList()
        }
        if(!has(Manifest.permission.READ_CALL_LOG)) { state.recents=emptyList();state.recentsError=false;state.recentsLoaded=true }
        if(!has(Manifest.permission.READ_PHONE_STATE)) state.sims=emptyList()
        if(!state.isDefault) state.blocked=emptyList()
    }

    fun reload() {
        clearUnavailableSources()
        recentReload.request();contactReload.request();blockedReload.request();simReload.request()
    }

    /** Intent -> kya karna hai (pure, seedha unit-testable). fill SIRF dialer me number bharta hai;
     *  call sirf ACTION_CALL/CALLBACK par - widget intents kabhi call produce nahi karte. */
    internal data class IntentRoute(
        val tab: String? = null,
        val fill: String? = null,
        val call: String? = null,
        val editContact: String? = null,
        val chooseNumberFor: Long? = null,
        val fillContactId: Long? = null,
        val fillContactName: String? = null,
    )

    /** Purane (stale) widget tap ka number aaj bhi valid hai ya nahi (pure). */
    internal sealed class FillReval {
        data class Fill(val number: String) : FillReval()
        object Choose : FillReval()
        object Gone : FillReval()
    }

    companion object {
        /** Carried number ab bhi contact ka hai tabhi fill; hata diya gaya to KABHI doosra number
         *  silently substitute nahi - chooser (current choice) ya no-fill feedback. */
        internal fun revalidateFill(carried: String, phones: List<Pair<String, Boolean>>): FillReval {
            if (phones.isEmpty()) return FillReval.Gone
            val identity = ContactControls.k(carried)
            return if (identity.isNotEmpty() && phones.any { ContactControls.k(it.first) == identity }) FillReval.Fill(carried) else FillReval.Choose
        }

        internal fun routeIntent(i: Intent): IntentRoute {
            val number = i.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart ?: i.getStringExtra("number")
            val tabExtra = i.getStringExtra("tab")
            val choose = i.getLongExtra("chooseNumber", -1L).takeIf { it > 0 }
            val edit = i.getStringExtra("editContact")
            val fillId = i.getLongExtra("numberContactId", -1L).takeIf { it > 0 }
            val fillName = i.getStringExtra("numberName")
            return when (i.action) {
                Intent.ACTION_DIAL -> IntentRoute(tab = "dialer", fill = number, editContact = edit, chooseNumberFor = choose, fillContactId = fillId, fillContactName = fillName)
                Intent.ACTION_VIEW -> if (number != null) IntentRoute(tab = "dialer", fill = number, editContact = edit, chooseNumberFor = choose, fillContactId = fillId, fillContactName = fillName)
                    else IntentRoute(tab = tabExtra, editContact = edit, chooseNumberFor = choose)
                Intent.ACTION_CALL, "com.mandu.liquiddialer.CALLBACK" -> IntentRoute(tab = tabExtra, call = number, editContact = edit, chooseNumberFor = choose)
                Intent.ACTION_CALL_BUTTON -> IntentRoute(tab = tabExtra ?: "recents", editContact = edit, chooseNumberFor = choose)
                else -> IntentRoute(tab = tabExtra, editContact = edit, chooseNumberFor = choose)
            }
        }
    }

    private fun handleIntent(i: Intent?) {
        i ?: return
        if(i.action==com.mandu.liquiddialer.data.LauncherContacts.ACTION){
            val config=try{com.mandu.liquiddialer.data.LauncherContacts.read(this).config}catch(_:Exception){null}
            val shortcutId=i.getStringExtra("launcherShortcutId")?:""
            if(config?.enabled==true&&shortcutId==com.mandu.liquiddialer.data.LauncherContacts.NEW){
                state.launcherNewContactRequest=true
            }else{
                val target=config?.let{com.mandu.liquiddialer.data.LauncherContacts.resolve(it,shortcutId)}
                if(target!=null){state.tab="dialer";state.dial=target.number}
                else Toast.makeText(this,"Shortcut off/removed. Settings > Calls > Icon long-press shortcuts",Toast.LENGTH_LONG).show()
            }
            return
        }
        if (i.action == com.mandu.liquiddialer.widget.FavWidgetModel.WIDGET_CALL) {
            val id = i.getLongExtra("numberContactId", i.getLongExtra("chooseNumber", -1L))
            if (id > 0) widgetTapCall(id, i.getStringExtra("number"), i.getStringExtra("numberName") ?: i.getStringExtra("chooseName") ?: "")
            return
        }
        val r = routeIntent(i)
        r.tab?.let { state.tab = it }
        r.editContact?.takeIf { it.isNotBlank() }?.let { num ->
            state.editContactRequest = try { Repo.contactIdFor(this, num) } catch (_: Exception) { null } to num
            i.removeExtra("editContact")
        }
        r.fill?.let { num ->
            val cid = r.fillContactId
            if (cid == null) state.dial = num
            else {
                // Purana widget tap: number fill karne se PEHLE live revalidate (deleted/badla hua contact par purana number nahi bharna)
                LdApp.instance.scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val phones = try { Repo.phonesOf(this@MainActivity, cid) } catch (_: Exception) { emptyList() }
                    val res = revalidateFill(num, phones)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        when (res) {
                            is FillReval.Fill -> { state.dial = res.number; state.tab = "dialer" }
                            FillReval.Choose -> { state.numberChoiceCalls = false; state.numberChoiceRequest = cid to (r.fillContactName ?: "") }
                            FillReval.Gone -> Toast.makeText(this@MainActivity, "Number ab uplabdh nahi (contact badal gaya)", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        r.chooseNumberFor?.let { id -> state.numberChoiceCalls = false; state.numberChoiceRequest = id to (i.getStringExtra("chooseName") ?: "") }
        r.call?.let { call(it) }
    }

    private var widgetBusy = false
    private var widgetLastTap = -2000L
    private var widgetPending: Triple<Long, String, String>? = null

    /** Only invoked by a widget PendingIntent or a deliberate number/SIM picker selection.
     * No permission grant callback resumes a call; the user must tap again. */
    fun widgetTapCall(contactId: Long, carried: String?, name: String, account: PhoneAccountHandle? = null, deliberateSelection: Boolean = false, afterUndo: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        if ((!afterUndo && state.undoInfo != null) || widgetBusy || (!deliberateSelection && account == null && now - widgetLastTap < 1200L)) return
        if (deliberateSelection && !afterUndo) {
            if (!state.numberChoiceCalls || state.numberChoiceRequest?.first != contactId) return
            // Consume this displayed choice synchronously. Recomposition/IO completion cannot replay it.
            state.numberChoiceRequest = null
        }
        widgetLastTap = now
        if (!has(Manifest.permission.READ_CONTACTS) || !has(Manifest.permission.CALL_PHONE)) {
            if (!has(Manifest.permission.READ_CONTACTS)) askPermission(Manifest.permission.READ_CONTACTS)
            else askPermission(Manifest.permission.CALL_PHONE)
            Toast.makeText(this, "Permission dene ke baad widget dobara dabao; abhi call nahi lagi", Toast.LENGTH_LONG).show()
            return
        }
        widgetBusy = true
        lifecycleScope.launch {
            try {
                val phones = withContext(Dispatchers.IO) { try { Repo.phonesOf(this@MainActivity, contactId) } catch (_: Exception) { emptyList() } }
                if (!has(Manifest.permission.READ_CONTACTS) || !has(Manifest.permission.CALL_PHONE)) return@launch
                val valid = carried?.takeIf { n -> phones.any { ContactControls.k(it.first) == ContactControls.k(n) && ContactControls.k(n).isNotEmpty() } }
                if (valid == null) {
                    if (phones.isEmpty()) Toast.makeText(this@MainActivity, "Contact ya number ab uplabdh nahi; call nahi lagi", Toast.LENGTH_LONG).show()
                    else {
                        state.numberChoiceCalls = true
                        state.numberChoiceNonce++
                        state.numberChoiceRequest = contactId to name
                    }
                    return@launch
                }
                state.numberChoiceRequest = null
                // Refresh accounts at the tap, not a startup snapshot. Existing pin/explicit selection rules apply.
                val liveSims = withContext(Dispatchers.IO) { Repo.sims(this@MainActivity) }
                if (account != null && liveSims.none { it.handle == account }) {
                    Toast.makeText(this@MainActivity, "Chuni hui SIM ab uplabdh nahi; call nahi lagi", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val stillCurrent = withContext(Dispatchers.IO) { try { Repo.phonesOf(this@MainActivity, contactId).any { ContactControls.k(it.first) == ContactControls.k(valid) } } catch (_: Exception) { false } }
                if (!stillCurrent || !has(Manifest.permission.READ_CONTACTS) || !has(Manifest.permission.CALL_PHONE)) {
                    Toast.makeText(this@MainActivity, "Contact/permission badal gayi; call nahi lagi", Toast.LENGTH_LONG).show()
                    return@launch
                }
                state.sims = liveSims
                widgetPending = Triple(contactId, valid, name)
                call(valid, account, widgetOrigin = true, skipUndo = afterUndo)
                if (state.pendingCall == null) widgetPending = null
            } finally { widgetBusy = false }
        }
    }

    fun cancelPendingWidgetCall() { widgetPending = null; state.pendingCall = null }

    fun choosePendingSim(number: String, account: PhoneAccountHandle, video: Boolean) {
        if (state.pendingCall != (number to video)) return
        state.pendingCall = null
        val pending = widgetPending
        widgetPending = null
        if (pending != null) widgetTapCall(pending.first, pending.second, pending.third, account)
        else call(number, account, video)
    }

    /** Place a real call, picking the SIM per settings. */
    fun call(number: String, account: PhoneAccountHandle? = null, video: Boolean = false, widgetOrigin: Boolean = false, skipUndo: Boolean = false) {
        if (!widgetOrigin) widgetPending = null
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
            // Per-number SIM pin (user ne Number sheet me khud set kiya) - memory/default se pehle, explicit pick ke baad
            val pinned = com.mandu.liquiddialer.data.ContactControls.simPin(this, n)
            acc = CallPrep.chooseSim(null, pinned, s.defaultSim, remembered, sims) { it.handle.id }?.handle
            if (acc == null && (pinned != null || CallPrep.needsSimPicker(sims.size))) {
                if (sims.isNotEmpty()) state.pendingCall = n to video
                else Toast.makeText(this, trNow("Pinned SIM uplabdh nahi. Number details me SIM pin badlo."), Toast.LENGTH_LONG).show()
                return
            }
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
        if (!skipUndo && !video && s.undoCall && n.firstOrNull()?.let { it.isDigit() || it == '+' } == true) {
            state.undoInfo = n to acc
            val widgetAtTap = if (widgetOrigin) widgetPending else null
            val token = Runnable {
                state.undoInfo = null
                if (widgetAtTap != null) widgetTapCall(widgetAtTap.first, widgetAtTap.second, widgetAtTap.third, acc, deliberateSelection = true, afterUndo = true)
                else if (!CallManager.place(this, n, acc, video)) Toast.makeText(this, trNow("Call nahi lag paayi"), Toast.LENGTH_SHORT).show()
            }
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

    /** SIM pick order: explicit user choice SABSE pehle (kabhi override nahi), phir per-number pin,
     *  phir default SIM, phir remembered, phir single-SIM; null = must ask.
     *  Pin set hai par handle ab maujood nahi (SIM nikli/slot badla) -> null: picker khulega,
     *  kisi doosri SIM par silent reroute KABHI nahi. */
    fun <T> chooseSim(explicit: T?, pinnedId: String?, defaultId: String?, rememberedId: String?, sims: List<T>, idOf: (T) -> String): T? {
        if (explicit != null) return explicit
        if (pinnedId != null) return sims.firstOrNull { idOf(it) == pinnedId }  // missing pin = ask, no fallback
        if (defaultId != null) sims.firstOrNull { idOf(it) == defaultId }?.let { return it }
        if (rememberedId != null) sims.firstOrNull { idOf(it) == rememberedId }?.let { return it }
        return if (sims.size == 1) sims[0] else null
    }

    /** Multi-SIM picker shows only when nothing was chosen and more than one SIM exists. */
    fun needsSimPicker(simCount: Int): Boolean = simCount > 1
}
