package com.mandu.liquiddialer

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.widget.FavWidgetModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver

/** Calls go only to Robolectric's fake TelecomManager. No device/network calls. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavWidgetCallUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val queryTrace=java.util.concurrent.CopyOnWriteArrayList<String>()
    private val fake = object : android.content.ContentProvider() {
        val delegate=FavWidgetChooserUiTest.FakeDetailProvider()
        var phones:List<Pair<String,Int>>
            get()=delegate.phones
            set(v) { delegate.phones=v }
        override fun onCreate()=true
        override fun query(uri:android.net.Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):android.database.Cursor? {
            queryTrace.add("${uri.path} id=${selectionArgs?.toList()} thread=${Thread.currentThread().name}")
            return delegate.query(uri,projection,selection,selectionArgs,sortOrder).also {
                queryTrace.add("done ${uri.path} rows=${it?.count}")
            }
        }
        override fun insert(uri:android.net.Uri,values:android.content.ContentValues?):android.net.Uri?=null
        override fun update(uri:android.net.Uri,values:android.content.ContentValues?,selection:String?,selectionArgs:Array<out String>?)=0
        override fun delete(uri:android.net.Uri,selection:String?,selectionArgs:Array<out String>?)=0
        override fun getType(uri:android.net.Uri):String?=null
    }
    private val sim = PhoneAccountHandle(ComponentName("com.mandu", "A"), "liveSim")
    private val n = "+91 90000 11111"
    private val app get() = ApplicationProvider.getApplicationContext<LdApp>()
    private val tm get() = app.getSystemService(TelecomManager::class.java)
    private fun settle() { rule.mainClock.autoAdvance = false; rule.mainClock.advanceTimeBy(400); rule.waitForIdle() }
    private fun await(check: () -> Boolean) {
        repeat(30) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if(check()) return
            Thread.sleep(100); settle()
        }
        val st=rule.activity.state
        assertTrue("awaited widget outcome: chooser=${st.numberChoiceRequest}, nonce=${st.numberChoiceNonce}, calls=${st.numberChoiceCalls}, undo=${st.undoInfo}, onboarding=${st.showOnboarding}, settings=${LdApp.settings.value}, queries=$queryTrace; tree=" + rule.onAllNodes(isRoot(),useUnmergedTree=true).printToString(),check())
    }
    @Before fun seed() = runBlocking {
        app.ldStore.edit { it[K.onboarded]=true; it[K.undoCall]=false; it[K.simMemory]=false; it[K.theme]="daylight" }
        ShadowContentResolver.registerProviderInternal(android.provider.ContactsContract.AUTHORITY, fake)
        shadowOf(app).grantPermissions(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE)
        shadowOf(tm).setCallPhonePermission(true)
        tm.registerPhoneAccount(PhoneAccount.builder(sim,"Test SIM").setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION).build())
        shadowOf(tm).enablePhoneAccount(sim,true)
        settle()
        await { LdApp.settings.value.let { it.onboarded && !it.undoCall && !it.simMemory && it.theme=="daylight" } }
        rule.runOnUiThread { rule.activity.state.showOnboarding=false }
        await { rule.onAllNodesWithTag("drawer_open",useUnmergedTree=true).fetchSemanticsNodes().size==1 }
        Unit
    }
    @After fun clear() { app.getSharedPreferences("contact_controls",0).edit().clear().apply() }
    private fun tap(number: String? = n, id: Long = 11L) {
        val i=Intent(app,MainActivity::class.java).setAction(FavWidgetModel.WIDGET_CALL)
            .putExtra("numberContactId",id).putExtra("numberName","Rahul Sharma")
        if(number!=null)i.putExtra("number",number)
        rule.runOnUiThread { org.robolectric.util.ReflectionHelpers.callInstanceMethod<Unit>(rule.activity,"onNewIntent",org.robolectric.util.ReflectionHelpers.ClassParameter.from(Intent::class.java,i)) }
    }
    @Test fun validTapPlacesExactlyOneFakeCallAndRapidDuplicateDoesNot() {
        tap(); tap()
        await { shadowOf(tm).allOutgoingCalls.size==1 }
        assertEquals(n,shadowOf(tm).onlyOutgoingCall.address.schemeSpecificPart)
        assertEquals(sim,shadowOf(tm).onlyOutgoingCall.phoneAccount)
        assertEquals("",rule.activity.state.dial)
    }
    @Test fun removedNumberShowsCurrentChooserNeverSubstitutes() {
        fake.phones=listOf("+91 90000 33333" to 2)
        tap()
        await { rule.activity.state.numberChoiceRequest!=null }
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
        assertTrue(rule.activity.state.numberChoiceCalls)
    }
    @Test fun deletedContactNeverCalls() {
        fake.phones=emptyList();tap();settle();Thread.sleep(300);settle()
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
        assertNull(rule.activity.state.numberChoiceRequest)
    }
    @Test fun missingCallPermissionNeverCallsAndGrantAloneDoesNotResume() {
        shadowOf(app).denyPermissions(Manifest.permission.CALL_PHONE)
        tap(); settle()
        shadowOf(app).grantPermissions(Manifest.permission.CALL_PHONE);settle()
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
    }
    @Test fun missingContactsPermissionNeverCalls() {
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS)
        tap();settle()
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
    }
    @Test fun staleSimPinAsksWithoutCall() {
        ContactControls.setSimPin(app,n,"removed")
        tap();await { rule.activity.state.pendingCall!=null }
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
    }
    @Test fun chooserCallsOnlyDeliberatelySelectedNumberAndRendersDayNight() {
        tap(null)
        await { rule.onAllNodesWithTag("chooser_num",useUnmergedTree=true).fetchSemanticsNodes().size==2 }
        rule.onNodeWithText("Kis number par call karein?",useUnmergedTree=true).assertExists()
        saveDialogShot("W200-chooser-day",0xFFF4F6FA)
        saveShot(rule,"W200-page-day")
        runBlocking { app.ldStore.edit { it[K.theme]="midnight" } }
        settle();Thread.sleep(200);settle();saveDialogShot("W200-chooser-night",0xFF101522)
        saveShot(rule,"W200-page-night")
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
        rule.onAllNodesWithTag("chooser_num",useUnmergedTree=true)[1].performClick()
        await { shadowOf(tm).allOutgoingCalls.size==1 }
        assertEquals("+91 90000 22222",shadowOf(tm).onlyOutgoingCall.address.schemeSpecificPart)
    }
    private fun openCallChooser() {
        assertEquals("provider fixture ready before widget intent",2,Repo.contactDetail(app,11L)?.phones?.size)
        tap(null)
        await { rule.activity.state.numberChoiceRequest?.first==11L }
        await { rule.onAllNodesWithTag("chooser_num",useUnmergedTree=true).fetchSemanticsNodes().size==2 }
    }
    @Test fun permissionRevokedAfterChooserOpensNeverCalls() {
        openCallChooser()
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS)
        rule.onAllNodesWithTag("chooser_num",useUnmergedTree=true)[0].performClick()
        settle();Thread.sleep(200);settle()
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
    }
    @Test fun editedChooserRowDoesNotCallOrSubstitute() {
        openCallChooser()
        fake.phones=listOf("+91 90000 33333" to 2)
        rule.onAllNodesWithTag("chooser_num",useUnmergedTree=true)[0].performClick()
        await { rule.activity.state.numberChoiceNonce>=2L }
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
    }
    @Test fun doubleChooserSelectionPlacesOnlyOneCall() {
        openCallChooser()
        rule.runOnUiThread {
            rule.activity.widgetTapCall(11,n,"Rahul",deliberateSelection=true)
            rule.activity.widgetTapCall(11,n,"Rahul",deliberateSelection=true)
        }
        await { shadowOf(tm).allOutgoingCalls.size==1 }
        settle(); assertEquals(1,shadowOf(tm).allOutgoingCalls.size)
    }
    @Test fun contactRemovedDuringUndoNeverCalls() {
        LdApp.settings.value=LdSettings(undoCall=true,simMemory=false,theme="daylight")
        tap(); await { rule.activity.state.undoInfo!=null }
        fake.phones=emptyList()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(3100,java.util.concurrent.TimeUnit.MILLISECONDS)
        Thread.sleep(300);settle()
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
    }
    @Test fun removedExplicitSimNeverCalls() {
        rule.runOnUiThread { rule.activity.widgetTapCall(11,n,"Rahul",PhoneAccountHandle(ComponentName("com.mandu","A"),"removed")) }
        Thread.sleep(300);settle()
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
    }

    @Test fun simPickerDoubleChoiceCannotPlaceDuplicateOrSkipContactRecheck() {
        ContactControls.setSimPin(app,n,"removed")
        tap();await { rule.activity.state.pendingCall!=null }
        fake.phones=emptyList()
        rule.runOnUiThread {
            rule.activity.choosePendingSim(n,sim,false)
            rule.activity.choosePendingSim(n,sim,false)
        }
        Thread.sleep(300);settle()
        assertTrue(shadowOf(tm).allOutgoingCalls.isEmpty())
    }

    @Test fun completedChoiceCannotReplayAfterIoAndClockAdvanceButNewTapCanCallLater() {
        openCallChooser()
        rule.runOnUiThread { rule.activity.widgetTapCall(11,n,"Rahul",deliberateSelection=true) }
        await { shadowOf(tm).allOutgoingCalls.size==1 }
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(2));settle()
        rule.runOnUiThread { rule.activity.widgetTapCall(11,n,"Rahul",deliberateSelection=true) }
        Thread.sleep(200);settle()
        assertEquals(1,shadowOf(tm).allOutgoingCalls.size)
        tap();await { shadowOf(tm).allOutgoingCalls.size==2 }
    }
    @Test fun currentSimSelectionRemainsUsableImmediatelyAfterStalePinChooser() {
        ContactControls.setSimPin(app,n,"removed")
        tap();await { rule.activity.state.pendingCall!=null }
        rule.runOnUiThread { rule.activity.choosePendingSim(n,sim,false) }
        await { shadowOf(tm).allOutgoingCalls.size==1 }
        assertEquals(sim,shadowOf(tm).onlyOutgoingCall.phoneAccount)
    }

}
