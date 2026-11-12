package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34]) @LooperMode(LooperMode.Mode.PAUSED)
class LauncherContactsIntentTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val target=LauncherContacts.Target("Foreign","+449876543210")
 @Before fun clean(){ctx.getSharedPreferences("launcher_contacts",0).edit().clear().commit();runBlocking{ctx.repo.set(K.onboarded,true);ctx.repo.set(K.motion,false)}}
 @Test fun contactTapFillsExactKeypadNeverCalls(){LauncherContacts.save(ctx,"",LauncherContacts.Config(true,listOf(target)));val controller=Robolectric.buildActivity(MainActivity::class.java).create();try{val act=controller.get();controller.newIntent(LauncherContacts.intent(ctx,LauncherContacts.id(target)));assertEquals("dialer",act.state.tab);assertEquals(target.number,act.state.dial);assertNull(act.state.confirmNumber);assertNull(act.state.undoInfo);assertNull(org.robolectric.Shadows.shadowOf(act).nextStartedActivity)}finally{controller.destroy()}}
 @Test fun disabledOrRemovedTapCannotUseOldExtras(){val controller=Robolectric.buildActivity(MainActivity::class.java).create();try{val act=controller.get();act.state.dial="139";controller.newIntent(LauncherContacts.intent(ctx,LauncherContacts.id(target)).putExtra("number","+919876543210"));assertEquals("139",act.state.dial);assertNull(act.state.confirmNumber);LauncherContacts.save(ctx,"",LauncherContacts.Config(true,emptyList()));controller.newIntent(LauncherContacts.intent(ctx,LauncherContacts.id(target)));assertEquals("139",act.state.dial)}finally{controller.destroy()}}
 @Test fun newContactQueuesEditorWithoutCreatingOrCalling(){LauncherContacts.save(ctx,"",LauncherContacts.Config(true,emptyList()));val controller=Robolectric.buildActivity(MainActivity::class.java).create();try{val act=controller.get();controller.newIntent(LauncherContacts.intent(ctx,LauncherContacts.NEW));assertTrue(act.state.launcherNewContactRequest);assertNull(act.state.confirmNumber);assertNull(act.state.undoInfo)}finally{controller.destroy()}}
}
