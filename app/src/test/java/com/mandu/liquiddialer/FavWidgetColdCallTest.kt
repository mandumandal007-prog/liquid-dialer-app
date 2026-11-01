package com.mandu.liquiddialer

import android.Manifest
import android.content.Intent
import android.telecom.TelecomManager
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavWidgetColdCallTest {
    private val app=ApplicationProvider.getApplicationContext<LdApp>()
    private val launch = run {
        ShadowContentResolver.registerProviderInternal(android.provider.ContactsContract.AUTHORITY,FavWidgetChooserUiTest.FakeDetailProvider())
        shadowOf(app).grantPermissions(Manifest.permission.CALL_PHONE,Manifest.permission.READ_CONTACTS)
        shadowOf(app.getSystemService(TelecomManager::class.java)).setCallPhonePermission(true)
        runBlocking { app.ldStore.edit { it[K.onboarded]=true;it[K.undoCall]=false;it[K.simMemory]=false } }
        LdApp.settings.value=LdSettings(undoCall=false,simMemory=false)
        Intent(app,MainActivity::class.java).setAction(FavWidgetModel.WIDGET_CALL)
            .putExtra("numberContactId",11L).putExtra("number","+91 90000 11111")
    }
    @get:Rule val rule=AndroidComposeTestRule(ActivityScenarioRule<MainActivity>(launch)) { r ->
        var act:MainActivity?=null;r.scenario.onActivity{act=it};act!!
    }
    @Test fun coldWidgetTapPlacesOneValidatedFakeCall() {
        rule.mainClock.autoAdvance=false
        val telecom=shadowOf(app.getSystemService(TelecomManager::class.java))
        repeat(30) {
            if(telecom.allOutgoingCalls.isEmpty()) { Thread.sleep(100);rule.mainClock.advanceTimeBy(200);rule.waitForIdle() }
        }
        assertEquals(1,telecom.allOutgoingCalls.size)
        assertEquals("+91 90000 11111",telecom.onlyOutgoingCall.address.schemeSpecificPart)
        assertEquals("",rule.activity.state.dial)
    }
}
