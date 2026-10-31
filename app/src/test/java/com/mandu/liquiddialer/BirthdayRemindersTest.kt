package com.mandu.liquiddialer

import android.app.AlarmManager
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.BirthdayReminders
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.data.SettingsRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Birthday reminder: fire-time math (DST-safe), default OFF, alarm lifecycle,
 *  notification sirf tab jab on + aaj birthday ho. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BirthdayRemindersTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private fun ms(date: LocalDate, hour: Int, min: Int = 0) =
        date.atTime(hour, min).atZone(zone).toInstant().toEpochMilli()

    @After fun tearDown() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        // JVM-shared sandbox: persisted state agle test classes ko mat do
        SettingsRepo(ctx).set(K.birthdayRemind, false)
        BirthdayReminders.cancel(ctx)
    }

    @Test
    fun nextFireMath() {
        val day = LocalDate.of(2026, 10, 31)
        // subah 9 se pehle = aaj 9 baje
        assertEquals(ms(day, 9), BirthdayReminders.nextFireMs(ms(day, 8, 15), zone))
        // theek 9 baje ya uske baad = kal 9 baje
        assertEquals(ms(day.plusDays(1), 9), BirthdayReminders.nextFireMs(ms(day, 9), zone))
        assertEquals(ms(day.plusDays(1), 9), BirthdayReminders.nextFireMs(ms(day, 23, 59), zone))
    }

    @Test
    fun defaultOff() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertFalse("default OFF hona chahiye", SettingsRepo(ctx).flow.first().birthdayRemind)
    }

    @Test
    fun alarmLifecycle() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val am = Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java))
        BirthdayReminders.cancel(ctx)
        assertNull(am.nextScheduledAlarm)
        val now = System.currentTimeMillis()
        BirthdayReminders.schedule(ctx, now)
        val a = am.nextScheduledAlarm // shadow poll karta hai - ek baar pakdo
        assertNotNull("on karne par agla alarm set ho", a)
        assertEquals(BirthdayReminders.nextFireMs(now), a!!.triggerAtTime)
        BirthdayReminders.cancel(ctx)
        assertNull("off karne par alarm cancel", am.nextScheduledAlarm)
    }

    @Test
    fun fireOnlyWhenEnabledAndBirthdayToday() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val am = Shadows.shadowOf(ctx.getSystemService(AlarmManager::class.java))
        BirthdayReminders.cancel(ctx)
        val bday = Repo.Birthday("Rahul", "9812345678", 0)
        var notified: List<Repo.Birthday>? = null
        var loads = 0

        // disabled: kuch nahi - na load, na notification, na alarm
        BirthdayReminders.onAlarm(ctx, enabled = false, load = { loads++; emptyList() }, notify = { _, l -> notified = l })
        assertNull(notified); assertEquals(0, loads); assertNull(am.nextScheduledAlarm)

        // enabled + aaj koi birthday nahi: notification nahi, par agla alarm zaroor
        BirthdayReminders.onAlarm(ctx, enabled = true, load = { emptyList() }, notify = { _, l -> notified = l })
        assertNull("birthday nahi to notification nahi", notified)
        assertNotNull("agla roz ka alarm phir bhi arm ho", am.nextScheduledAlarm)

        // enabled + aaj birthday: notification + agla alarm
        BirthdayReminders.onAlarm(ctx, enabled = true, load = { listOf(bday) }, notify = { _, l -> notified = l })
        assertEquals(listOf(bday), notified)
        assertNotNull(am.nextScheduledAlarm)
    }
}
