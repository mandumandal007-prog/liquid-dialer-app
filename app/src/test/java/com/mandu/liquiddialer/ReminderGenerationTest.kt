package com.mandu.liquiddialer

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.CallReminders
import com.mandu.liquiddialer.telecom.CallActionReceiver
import com.mandu.liquiddialer.telecom.CallNotifier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ReminderGenerationTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private val at get()=System.currentTimeMillis()+3600000
    @Before fun clear(){ctx.getSharedPreferences("call_reminders",0).edit().clear().commit()}
    @Test fun internationalSuffixesAndAliasesStayIndependent(){
        CallReminders.set(ctx,"+1 2025550123","US",at)
        CallReminders.set(ctx,"+44 2025550123","UK",at)
        assertEquals(2,CallReminders.all(ctx).size)
        assertEquals("US",CallReminders.forNumber(ctx,"0012025550123")!!.name)
        CallReminders.cancel(ctx,"0012025550123")
        assertEquals("UK",CallReminders.all(ctx).single().name)
        assertEquals(1,Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java)).scheduledAlarms.size)
    }
    @Test fun delimiterUnicodeNamesRoundTripAndMigrate(){
        val name="Family | Office | घर 👪"
        ctx.getSharedPreferences("call_reminders",0).edit().putString("9812345678","9812345678|$name|$at").commit()
        val first=CallReminders.allRaw(ctx).single()
        assertEquals(name,first.name);assertTrue(first.token.isNotBlank())
        assertEquals(first,CallReminders.allRaw(ctx).single())
        assertEquals(1,Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java)).scheduledAlarms.size)
        CallReminders.set(ctx,first.number,name,at+60000)
        assertEquals(name,CallReminders.allRaw(ctx).single().name)
    }
    @Test fun staleSameTimeGenerationAndCancelledBroadcastCannotNotify(){
        CallNotifier.createChannels(ctx)
        CallReminders.set(ctx,"9812345678","First",at)
        val old=CallReminders.allRaw(ctx).single()
        CallReminders.set(ctx,old.number,"Updated",old.atMs)
        val fresh=CallReminders.allRaw(ctx).single();assertNotEquals(old.token,fresh.token)
        val nm=Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java));val before=nm.allNotifications.size
        fun fire(token:String){CallActionReceiver().onReceive(ctx,Intent(CallActionReceiver.CALL_REMIND).putExtra("number",old.number).putExtra("token",token).putExtra("name","Wrong name"))}
        fire(old.token);assertEquals(before,nm.allNotifications.size);assertEquals(fresh,CallReminders.allRaw(ctx).single())
        CallReminders.cancel(ctx,old.number);fire(fresh.token)
        assertEquals(before,nm.allNotifications.size);assertTrue(CallReminders.allRaw(ctx).isEmpty())
    }
    @Test fun delayedDueRecordSurvivesListReadAndConsumesOnlyOnce(){
        CallReminders.set(ctx,"9812345678","Due",System.currentTimeMillis()-60000)
        val due=CallReminders.allRaw(ctx).single()
        assertTrue(CallReminders.all(ctx).isEmpty())
        assertEquals(due,CallReminders.consume(ctx,due.number,due.token))
        assertNull(CallReminders.consume(ctx,due.number,due.token))
    }
    @Test fun corruptRowsCannotBecomeMisparsedOrEraseValidRows(){
        val p=ctx.getSharedPreferences("call_reminders",0)
        p.edit().putString("bad","{malformed|office|123}").putString("missing","9811111111|Name|broken").commit()
        CallReminders.set(ctx,"9812345678","Valid",at)
        assertEquals("Valid",CallReminders.allRaw(ctx).single().name)
        assertEquals("{malformed|office|123}",p.getString("bad",null))
    }
    @Test fun malformedNumbersRejectedWithoutChangingExistingGeneration(){
        CallReminders.set(ctx,"9812345678","Keep",at);val original=CallReminders.allRaw(ctx).single()
        for(n in listOf("++919812345678","91+9812345678","*123#","+","9".repeat(100),"１２３")){
            try{CallReminders.set(ctx,n,"Invalid",at);fail("Accepted $n")}catch(_:IllegalArgumentException){}
        }
        assertEquals(original,CallReminders.allRaw(ctx).single())
        assertEquals(1,Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java)).scheduledAlarms.size)
    }
    @Test fun pendingMigrationRearmRetriesWithoutChangingTokens(){
        CallReminders.set(ctx,"9812345678","Retry",at);val original=CallReminders.allRaw(ctx).single()
        val manager=Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java))
        while(manager.nextScheduledAlarm!=null){}
        ctx.getSharedPreferences("call_reminders",0).edit().putBoolean("v3RearmPending",true).commit()
        assertEquals(original,CallReminders.allRaw(ctx).single());assertEquals(1,manager.scheduledAlarms.size)
        CallReminders.allRaw(ctx);assertEquals(1,manager.scheduledAlarms.size)
    }
}
