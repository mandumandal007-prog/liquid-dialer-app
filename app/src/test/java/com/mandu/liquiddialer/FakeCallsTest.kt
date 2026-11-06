package com.mandu.liquiddialer

import android.app.*
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.BootReceiver
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class FakeCallsTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    @Before fun clear(){FakeCalls.cancel(ctx);Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS);FakeCalls.notificationsAllowed(ctx)}
    @After fun cleanup(){FakeCalls.cancel(ctx)}
    @Test fun schedulePersistsAndCancellationRetiresToken(){
        val p=FakeCalls.schedule(ctx,"Local demo",30_000,100_000)
        assertEquals(p,FakeCalls.current(ctx));assertFalse(p.ready)
        FakeCalls.cancel(ctx);assertNull(FakeCalls.current(ctx));assertFalse(FakeCalls.deliver(ctx,p.token,130_000))
    }
    @Test fun oldGenerationCannotConsumeNewSchedule(){
        val old=FakeCalls.schedule(ctx,"Old",30_000,100_000)
        val next=FakeCalls.schedule(ctx,"Next",60_000,100_000)
        assertFalse(FakeCalls.deliver(ctx,old.token,160_000));assertEquals(next,FakeCalls.current(ctx))
    }
    @Test fun lateExpiredNeverResurrects(){
        val p=FakeCalls.schedule(ctx,"Demo",30_000,100_000)
        assertFalse(FakeCalls.deliver(ctx,p.token,430_001));assertNull(FakeCalls.current(ctx))
    }
    @Test fun dueOncePostsTapNotificationNotFullscreen(){
        val p=FakeCalls.schedule(ctx,"Demo",30_000,100_000)
        assertFalse(FakeCalls.deliver(ctx,p.token,129_999))
        assertTrue(FakeCalls.deliver(ctx,p.token,130_000));assertFalse(FakeCalls.deliver(ctx,p.token,130_000))
        val n=Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java)).getNotification(FakeCalls.NOTICE)
        assertNotNull(n.contentIntent);assertNull(n.fullScreenIntent)
        assertEquals("Fake call: Demo",n.extras.getString("android.title"))
    }
    @Test fun blockedChannelCannotSchedule(){
        ctx.getSystemService(NotificationManager::class.java).deleteNotificationChannel(FakeCalls.CHANNEL)
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(FakeCalls.CHANNEL,"Blocked",NotificationManager.IMPORTANCE_NONE))
        assertFalse(FakeCalls.notificationsAllowed(ctx))
        try{FakeCalls.schedule(ctx,"Demo",30_000);fail("must block")}catch(_:IllegalStateException){}
        assertNull(FakeCalls.current(ctx))
    }
    @Test fun channelRevokedAfterSchedulingCancelsDelivery(){
        val p=FakeCalls.schedule(ctx,"Demo",30_000,100_000)
        val nm=ctx.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(FakeCalls.CHANNEL)
        nm.createNotificationChannel(NotificationChannel(FakeCalls.CHANNEL,"Blocked",NotificationManager.IMPORTANCE_NONE))
        assertFalse(FakeCalls.deliver(ctx,p.token,130_000));assertNull(FakeCalls.current(ctx))
    }
    @Test fun realCallRetiresAlreadyPostedAlert(){
        val p=FakeCalls.schedule(ctx,"Demo",30_000,100_000);FakeCalls.deliver(ctx,p.token,130_000)
        assertNotNull(Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java)).getNotification(FakeCalls.NOTICE))
        FakeCalls.cancel(ctx) // onCallAdded's first action after super; no real-call behavior altered.
        assertNull(FakeCalls.current(ctx));assertNull(Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java)).getNotification(FakeCalls.NOTICE))
    }
    @Test fun rebootCancelsInsteadOfRestoring(){
        FakeCalls.schedule(ctx,"Demo",60_000)
        BootReceiver().onReceive(ctx,Intent(Intent.ACTION_BOOT_COMPLETED));assertNull(FakeCalls.current(ctx))
    }
    @Test fun previewCannotReplacePendingAndDoesNotCall(){
        FakeCalls.schedule(ctx,"Scheduled",60_000)
        try{FakeCalls.preview(ctx);fail("pending plan must survive")}catch(_:IllegalStateException){}
        assertEquals("Scheduled",FakeCalls.current(ctx)?.name)
        FakeCalls.cancel(ctx)
        val p=FakeCalls.preview(ctx);assertTrue(p.ready);assertEquals(p,FakeCalls.usable(ctx,p.token))
        assertTrue(com.mandu.liquiddialer.telecom.CallManager.calls.value.isEmpty())
    }
}
