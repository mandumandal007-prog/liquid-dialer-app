package com.mandu.liquiddialer

import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.LdSettings
import com.mandu.liquiddialer.data.ReceivedNames
import com.mandu.liquiddialer.telecom.CallManager
import android.telecom.Call
import android.telecom.TelecomManager
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ReceivedNamesTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private val a=TelecomManager.PRESENTATION_ALLOWED
    @After fun clean(){ctx.getSharedPreferences("received_caller_names",0).edit().clear().apply()}
    @Test fun persistsFullIdentityAndDoesNotMergeForeignSuffix(){
        ReceivedNames.learn(ctx,"+12025559876","Network name",a,a)
        assertEquals("Network name",ReceivedNames.get(ctx,"0012025559876"))
        assertNull(ReceivedNames.get(ctx,"+442025559876"))
    }
    @Test fun restrictedAndUnknownPresentationNeverLearn(){
        ReceivedNames.learn(ctx,"9876543210","Restricted",TelecomManager.PRESENTATION_RESTRICTED,a)
        ReceivedNames.learn(ctx,"9876543210","Unknown",a,TelecomManager.PRESENTATION_UNKNOWN)
        ReceivedNames.learn(ctx,"","Blank",a,a)
        assertNull(ReceivedNames.get(ctx,"9876543210"));assertNull(ReceivedNames.get(ctx,""))
    }
    @Test fun lateNameReplacesEarlierBlankSessionLookup(){
        val blank=FakeCall.make("9876543210","",Call.STATE_RINGING)
        assertNotEquals("Network name",CallManager.nameFor(ctx,blank))
        val named=FakeCall.make("9876543210","Network name",Call.STATE_RINGING)
        assertEquals("Network name",CallManager.nameFor(ctx,named))
        assertEquals("Network name",CallManager.nameFor(ctx,blank))
    }
    @Test fun dismissPreventsSameWrongNameReturning(){
        ReceivedNames.learn(ctx,"9876543210","Wrong",a,a)
        ReceivedNames.dismiss(ctx,"9876543210")
        ReceivedNames.learn(ctx,"9876543210","Wrong",a,a)
        assertNull(ReceivedNames.get(ctx,"9876543210"))
    }
    @Test fun invalidNameDoesNotOverwriteGood(){
        ReceivedNames.learn(ctx,"9876543210","Good",a,a)
        ReceivedNames.learn(ctx,"9876543210","Bad\nName",a,a)
        assertEquals("Good",ReceivedNames.get(ctx,"9876543210"))
    }
    @Test fun savedContactWinsAndDeletesLearnedName() {
        ReceivedNames.learn(ctx,"9876543210","Network",a,a)
        val provider=object:android.content.ContentProvider(){
            override fun onCreate()=true
            override fun query(uri:android.net.Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):android.database.Cursor? {
                if(!uri.toString().contains("phone_lookup"))return null
                return android.database.MatrixCursor(projection).apply { addRow(arrayOf("Saved by me",null)) }
            }
            override fun insert(u:android.net.Uri,v:android.content.ContentValues?):android.net.Uri?=null
            override fun delete(u:android.net.Uri,s:String?,a:Array<out String>?)=0
            override fun update(u:android.net.Uri,v:android.content.ContentValues?,s:String?,a:Array<out String>?)=0
            override fun getType(u:android.net.Uri):String?=null
        }
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(android.provider.ContactsContract.AUTHORITY,provider)
        assertEquals("Saved by me",CallManager.nameFor(ctx,FakeCall.make("9876543210","Network",Call.STATE_RINGING)))
        assertNull(ReceivedNames.get(ctx,"9876543210"))
    }
    @Test fun privatePresentationNeverExposesPreviouslyLearnedName() {
        ReceivedNames.learn(ctx,"9876543210","Network",a,a)
        val c=FakeCall.make("9876543210","Network",Call.STATE_RINGING)
        val f=Call.Details::class.java.getDeclaredField("mHandlePresentation");f.isAccessible=true;f.set(c.details,TelecomManager.PRESENTATION_RESTRICTED)
        assertEquals("Private number",CallManager.nameFor(ctx,c))
    }

    @Test fun appearanceBackupContainsNoLearnedName() {
        ReceivedNames.learn(ctx,"9876543210","Private network name",a,a)
        val exported=com.mandu.liquiddialer.ui.SettingsBackup.buildJson(LdSettings())
        assertFalse(exported.contains("Private network name"))
        assertFalse(exported.contains("9876543210"))
    }

    @Test fun dismissRemovesCurrentCallDisplayAndRestrictedCacheNeverExposedWithoutContactsPermission() {
        val c=FakeCall.make("9876543210","Wrong",Call.STATE_RINGING)
        assertEquals("Wrong",CallManager.nameFor(ctx,c))
        ReceivedNames.dismiss(ctx,"9876543210")
        assertNotEquals("Wrong",CallManager.nameFor(ctx,c))
        ReceivedNames.learn(ctx,"9876543210","Private cache",a,a)
        org.robolectric.Shadows.shadowOf(ctx).denyPermissions(android.Manifest.permission.READ_CONTACTS)
        val f=Call.Details::class.java.getDeclaredField("mCallerDisplayNamePresentation");f.isAccessible=true;f.set(c.details,TelecomManager.PRESENTATION_RESTRICTED)
        assertNotEquals("Private cache",CallManager.nameFor(ctx,c))
        assertNotEquals("Wrong",CallManager.nameFor(ctx,c))
    }

}
