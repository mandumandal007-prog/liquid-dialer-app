package com.mandu.liquiddialer

import android.app.AlarmManager
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.CallReminders
import com.mandu.liquiddialer.telecom.CallActionReceiver
import com.mandu.liquiddialer.telecom.CallNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar

/** Manual call reminder: preset time math, store round-trip, alarm schedule/cancel,
 *  receiver fire par notification + store cleanup. Koi auto-call nahi. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallReminderTest {

    private fun at(h: Int, m: Int): Long = Calendar.getInstance().apply {
        set(2026, 9, 30, h, m, 0); set(Calendar.MILLISECOND, 0) // 30 Oct 2026
    }.timeInMillis

    @Test
    fun presetMathBoundaries() {
        // 30min / 1h seedha offset
        assertEquals(at(10, 30), CallReminders.presetAt("30min", at(10, 0)))
        assertEquals(at(11, 0), CallReminders.presetAt("1h", at(10, 0)))
        // evening: 18:00 se pehle -> aaj 18:00
        val ev = Calendar.getInstance().apply { timeInMillis = CallReminders.presetAt("evening", at(10, 0)) }
        assertEquals(30, ev.get(Calendar.DAY_OF_MONTH)); assertEquals(18, ev.get(Calendar.HOUR_OF_DAY))
        // evening: 18:00 ke baad -> kal 18:00
        val ev2 = Calendar.getInstance().apply { timeInMillis = CallReminders.presetAt("evening", at(18, 30)) }
        assertEquals(31, ev2.get(Calendar.DAY_OF_MONTH)); assertEquals(18, ev2.get(Calendar.HOUR_OF_DAY))
        // morning: hamesha kal 09:00
        val mo = Calendar.getInstance().apply { timeInMillis = CallReminders.presetAt("morning", at(8, 0)) }
        assertEquals(31, mo.get(Calendar.DAY_OF_MONTH)); assertEquals(9, mo.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun setScheduleCancelRoundTrip() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        CallReminders.cancel(ctx, "+919812345678")
        val at = System.currentTimeMillis() + 3600_000L
        CallReminders.set(ctx, "+91 98123 45678", "Rahul", at)
        val r = CallReminders.forNumber(ctx, "9812345678")
        assertNotNull(r); assertEquals("Rahul", r!!.name); assertEquals(at, r.atMs)
        val am = Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java))
        assertNotNull("alarm scheduled hona chahiye", am.nextScheduledAlarm)
        // dobara set = replace, duplicate nahi
        CallReminders.set(ctx, "9812345678", "Rahul", at + 600_000L)
        assertEquals(1, CallReminders.all(ctx).count { it.name == "Rahul" })
        CallReminders.cancel(ctx, "9812345678")
        assertNull(CallReminders.forNumber(ctx, "9812345678"))
        assertNull("cancel ke baad koi alarm nahi", am.nextScheduledAlarm)
    }

    private fun collision(): Pair<String,String> {
        val seen=mutableMapOf<Int,String>()
        for (n in 9812345000L..9812346000L) {
            val number=n.toString();val bucket=com.mandu.liquiddialer.data.Repo.key(number).hashCode() and 255
            val old=seen.putIfAbsent(bucket,number)
            if(old!=null)return old to number
        }
        error("Expected a collision in 1001 numbers / 256 buckets")
    }

    @Test fun collidingLegacyBucketsNowKeepTwoIndependentAlarmsAndCancellation() {
        val ctx=ApplicationProvider.getApplicationContext<LdApp>()
        ctx.getSharedPreferences("call_reminders",0).edit().clear().commit()
        val (a,b)=collision();val at=System.currentTimeMillis()+3600_000L
        CallReminders.set(ctx,a,"First",at);CallReminders.set(ctx,b,"Second",at+60_000L)
        val am=Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java))
        assertEquals(2,am.scheduledAlarms.size)
        CallReminders.set(ctx,a,"First updated",at+120_000L)
        assertEquals(2,am.scheduledAlarms.size)
        CallReminders.cancel(ctx,a)
        assertEquals(1,am.scheduledAlarms.size)
        assertNotNull(CallReminders.forNumber(ctx,b))
        val remaining=Shadows.shadowOf(am.scheduledAlarms.single().operation).savedIntent
        assertEquals(b,remaining.getStringExtra("number"))
        CallReminders.cancel(ctx,b);assertTrue(am.scheduledAlarms.isEmpty())
    }

    @Test fun legacySharedAlarmMigratesAllStoredNumbersBeforeCancel() {
        val ctx=ApplicationProvider.getApplicationContext<LdApp>()
        val p=ctx.getSharedPreferences("call_reminders",0);p.edit().clear().commit()
        val (a,b)=collision();val at=System.currentTimeMillis()+3600_000L
        p.edit().putString(a,"$a|First|$at").putString(b,"$b|Second|${at+60000}").commit()
        val old=android.content.Intent(ctx,CallActionReceiver::class.java).setAction(CallActionReceiver.CALL_REMIND).putExtra("number",b)
        val legacy=android.app.PendingIntent.getBroadcast(ctx,700+(a.hashCode() and 255),old,android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val manager=ctx.getSystemService(AlarmManager::class.java);manager.set(AlarmManager.RTC_WAKEUP,at,legacy)
        val am=Shadows.shadowOf(manager);assertEquals(1,am.scheduledAlarms.size)
        CallReminders.cancel(ctx,a)
        assertEquals(1,am.scheduledAlarms.size)
        val intent=Shadows.shadowOf(am.scheduledAlarms.single().operation).savedIntent
        assertEquals(b,intent.getStringExtra("number"));assertNotNull(intent.data)
        CallReminders.cancel(ctx,b);assertTrue(am.scheduledAlarms.isEmpty())
    }

    @Test fun rebootRearmsCollidingNumbersIndependently() {
        val ctx=ApplicationProvider.getApplicationContext<LdApp>()
        ctx.getSharedPreferences("call_reminders",0).edit().clear().commit()
        val (a,b)=collision();val at=System.currentTimeMillis()+3600_000L
        CallReminders.set(ctx,a,"First",at);CallReminders.set(ctx,b,"Second",at+60_000L)
        val am=Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java))
        while(am.nextScheduledAlarm!=null) { }
        com.mandu.liquiddialer.telecom.BootReceiver().onReceive(ctx,android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))
        assertEquals(2,am.scheduledAlarms.size)
        assertEquals(setOf(a,b),am.scheduledAlarms.map { Shadows.shadowOf(it.operation).savedIntent.getStringExtra("number") }.toSet())
        CallReminders.cancel(ctx,a);CallReminders.cancel(ctx,b)
    }

    @Test
    fun allReturnsSortedByTime() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        CallReminders.cancel(ctx, "111"); CallReminders.cancel(ctx, "222")
        val now = System.currentTimeMillis()
        CallReminders.set(ctx, "222", "Baad Wala", now + 7200_000L)
        CallReminders.set(ctx, "111", "Pehle Wala", now + 3600_000L)
        val names = CallReminders.all(ctx).map { it.name }
        assertTrue(names.indexOf("Pehle Wala") < names.indexOf("Baad Wala"))
        CallReminders.cancel(ctx, "111"); CallReminders.cancel(ctx, "222")
    }

    @Test
    fun pastRemindersAutoCleaned() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        CallReminders.cancel(ctx, "5550001")
        CallReminders.set(ctx, "5550001", "Old", System.currentTimeMillis() - 1000)
        assertTrue(CallReminders.all(ctx).none { it.number.contains("5550001") })
        CallReminders.cancel(ctx, "5550001")
    }

    @Test
    fun bootReceiverReschedulesStoredReminders() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        CallReminders.cancel(ctx, "9812345678")
        val at = System.currentTimeMillis() + 3600_000L
        CallReminders.set(ctx, "9812345678", "Rahul", at)
        val am = Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java))
        assertNotNull(am.nextScheduledAlarm)
        while (am.nextScheduledAlarm != null) { } // reboot ke baad alarms gayab (shadow: getNext poll karta hai, scheduledAlarms.clear() copy par no-op hai)
        assertNull(am.nextScheduledAlarm)
        com.mandu.liquiddialer.telecom.BootReceiver().onReceive(ctx, android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))
        val a = am.nextScheduledAlarm
        assertNotNull("boot ke baad reminder dobara schedule hona chahiye", a)
        assertEquals(at, a!!.triggerAtTime)
        // galat action par kuch nahi hota
        while (am.nextScheduledAlarm != null) { }
        com.mandu.liquiddialer.telecom.BootReceiver().onReceive(ctx, android.content.Intent("com.example.OTHER"))
        assertNull(am.nextScheduledAlarm)
        CallReminders.cancel(ctx, "9812345678")
    }

    @Test
    fun receiverPostsNotificationAndClears() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        CallNotifier.createChannels(ctx)
        val at = System.currentTimeMillis() + 3600_000L
        CallReminders.set(ctx, "9812345678", "Rahul", at)
        val intent = android.content.Intent(CallActionReceiver.CALL_REMIND)
            .putExtra("number", "9812345678").putExtra("name", "Rahul")
        CallActionReceiver().onReceive(ctx, intent)
        val nm = Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java))
        val n = nm.allNotifications.lastOrNull { it.extras.getCharSequence("android.title").toString().contains("Call reminder") }
        assertNotNull("reminder notification post honi chahiye", n)
        assertTrue(n!!.extras.getCharSequence("android.title").toString().contains("Rahul"))
        // body tap = sirf app/dialer khulta hai (contentIntent), direct call sirf "Call karo" action se
        assertNotNull("body tap intent hona chahiye", n.contentIntent)
        assertTrue("'Call karo' action hona chahiye", n.actions.any { it.title.toString() == "Call karo" })
        assertNull("fire ke baad store saaf", CallReminders.forNumber(ctx, "9812345678"))
        // number blank -> kuch nahi hota
        val before = nm.allNotifications.size
        CallActionReceiver().onReceive(ctx, android.content.Intent(CallActionReceiver.CALL_REMIND).putExtra("number", ""))
        assertEquals(before, nm.allNotifications.size)
    }

    @Test
    fun bootReceiverFiresRecentlyMissedAndDropsAncient() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        CallNotifier.createChannels(ctx)
        val now = System.currentTimeMillis()
        // phone band tha jab ye dono nikal gaye: ek 2 ghante pehle, ek 2 din pehle
        CallReminders.set(ctx, "9811111111", "Recent Miss", now - 2 * 3600_000L)
        CallReminders.set(ctx, "9822222222", "Ancient", now - 48 * 3600_000L)
        val am = Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java))
        while (am.nextScheduledAlarm != null) { } // reboot: shadow me getNext poll karta hai
        val nm = Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java))
        val before = nm.allNotifications.size
        com.mandu.liquiddialer.telecom.BootReceiver().onReceive(ctx, android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))
        // recent miss: notification aayi, store saaf, koi naya alarm nahi
        val n = nm.allNotifications.lastOrNull { it.extras.getCharSequence("android.title").toString().contains("Recent Miss") }
        assertNotNull("band phone ke dauraan nikla reminder boot par batana chahiye", n)
        assertTrue(nm.allNotifications.size > before)
        assertNull("fire ke baad recent miss store se hatna chahiye", CallReminders.allRaw(ctx).firstOrNull { it.number.contains("9811111111") })
        // ancient: chup-chap saaf, koi notification nahi
        assertNull("ancient reminder saaf hona chahiye", CallReminders.allRaw(ctx).firstOrNull { it.number.contains("9822222222") })
        assertTrue("ancient ke liye notification nahi", nm.allNotifications.none { it.extras.getCharSequence("android.title").toString().contains("Ancient") })
        assertNull("past reminders ke liye koi alarm nahi", am.nextScheduledAlarm)
    }
}
