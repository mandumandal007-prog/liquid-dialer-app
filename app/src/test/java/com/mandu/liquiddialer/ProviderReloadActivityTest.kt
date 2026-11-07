package com.mandu.liquiddialer

import android.Manifest
import android.content.*
import android.database.*
import android.net.Uri
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.mandu.liquiddialer.data.LdSettings
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProviderReloadActivityTest {
    private val app = ApplicationProvider.getApplicationContext<LdApp>()
    private val calls = AtomicInteger()
    private val contacts = AtomicInteger()
    private open class Provider: ContentProvider() {
        override fun onCreate()=true
        override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor?=null
        override fun insert(u:Uri,v:ContentValues?):Uri?=null
        override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0
        override fun delete(u:Uri,s:String?,a:Array<out String>?)=0
        override fun getType(u:Uri):String?=null
    }
    private val launch = run {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS)
        LdApp.settings.value=LdSettings(onboarded=true)
        ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY, object:Provider() {
            override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor = MatrixCursor(p).apply {
                addRow(arrayOf(calls.incrementAndGet().toLong(),"9000011111","Caller",1,1000L,0L,null,null))
            }
        })
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY, object:Provider() {
            override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor = MatrixCursor(p).apply {
                if (u==ContactsContract.CommonDataKinds.Phone.CONTENT_URI) {
                    val n=contacts.incrementAndGet()
                    addRow(arrayOf(1L,"lookup","Contact $n","Contact $n","9000011111",2,null,0,null,1))
                }
            }
        })
        Intent(app,MainActivity::class.java)
    }
    @get:Rule val rule=AndroidComposeTestRule(ActivityScenarioRule<MainActivity>(launch)) { r ->
        var act:MainActivity?=null; r.scenario.onActivity { act=it }; act!!
    }
    private fun waitFor(condition:()->Boolean) {
        rule.waitUntil(5000) { shadowOf(Looper.getMainLooper()).idle(); condition() }
        rule.waitForIdle()
    }
    private fun burst(uri:Uri) {
        rule.runOnUiThread { repeat(100) { app.contentResolver.notifyChange(uri,null) } }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(180))
    }
    @Test fun callLogDoesNotRescanContactsButContactEditAndManualReloadStillDo() {
        rule.mainClock.autoAdvance=false
        waitFor { rule.activity.state.recentsLoaded && rule.activity.state.contacts.isNotEmpty() }
        val c0=calls.get(); val p0=contacts.get()
        burst(CallLog.Calls.CONTENT_URI)
        waitFor { rule.activity.state.recents.firstOrNull()?.id == (c0+1).toLong() }
        assertEquals(c0+1,calls.get()); assertEquals(p0,contacts.get())
        burst(ContactsContract.Contacts.CONTENT_URI)
        waitFor { rule.activity.state.recents.firstOrNull()?.id == (c0+2).toLong() && rule.activity.state.contacts.firstOrNull()?.name=="Contact ${p0+1}" }
        assertEquals(c0+2,calls.get()); assertEquals(p0+1,contacts.get())
        rule.runOnUiThread { rule.activity.reload() }
        waitFor { rule.activity.state.recents.firstOrNull()?.id == (c0+3).toLong() && rule.activity.state.contacts.firstOrNull()?.name=="Contact ${p0+2}" }
        assertEquals(c0+3,calls.get()); assertEquals(p0+2,contacts.get())
        shadowOf(app).denyPermissions(Manifest.permission.READ_CONTACTS,Manifest.permission.READ_CALL_LOG)
        rule.runOnUiThread { rule.activity.reload() }
        assertTrue(rule.activity.state.contacts.isEmpty()); assertTrue(rule.activity.state.recents.isEmpty())
        assertTrue(rule.activity.state.recentsLoaded); assertFalse(rule.activity.state.recentsError)
    }
}
