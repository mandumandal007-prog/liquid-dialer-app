package com.mandu.liquiddialer
import android.content.pm.ShortcutManager
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class LauncherContactsTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val a=LauncherContacts.Target("Father","+919876543210");private val b=LauncherContacts.Target("Mandu 2","+449876543210")
 private val enabled=LauncherContacts.Config(true,listOf(a,b))
 private val manager get()=ctx.getSystemService(ShortcutManager::class.java)
 private fun bad(f:()->Unit){try{f();fail("must reject")}catch(_:Exception){}}
 @Before fun clean(){ctx.getSharedPreferences("launcher_contacts",0).edit().clear().commit();manager.removeAllDynamicShortcuts()}
 @Test fun defaultsOffRoundTrip(){assertFalse(LauncherContacts.read(ctx).config.enabled);val s=LauncherContacts.save(ctx,"",enabled);assertEquals(enabled,LauncherContacts.read(ctx).config);assertEquals(s.raw,LauncherContacts.encode(LauncherContacts.decode(s.raw)))}
 @Test fun validateNoBlankWrongOrDuplicate(){listOf(a.copy(name=""),a.copy(name="a\nb"),a.copy(number="+91x9876543210"),a.copy(number="12"),a.copy(number="+९१९८७६५४३२१०")).forEach{bad{it.validate()}};bad{LauncherContacts.Config(true,listOf(a,a.copy(number="+91 98765 43210"))).validate()};bad{LauncherContacts.Config(true,List(4){a}).validate()}}
 @Test fun staleSaveAndCorruptPreserve(){val s=LauncherContacts.save(ctx,"",enabled);bad{LauncherContacts.save(ctx,"",enabled.copy(enabled=false))};assertEquals(s,LauncherContacts.read(ctx));ctx.getSharedPreferences("launcher_contacts",0).edit().putString("data","broken").commit();bad{LauncherContacts.save(ctx,s.raw,enabled)};assertEquals("broken",ctx.getSharedPreferences("launcher_contacts",0).getString("data",null))}
 @Test fun schemaStrict(){val raw=LauncherContacts.encode(enabled);listOf(raw+"x",raw.replace("\"enabled\":true","\"enabled\":1"),raw.replace("\"version\":1","\"version\":2")).forEach{bad{LauncherContacts.decode(it)}}}
 @Test fun fullIdentityStableIdsNotLastTenOrPosition(){assertNotEquals(LauncherContacts.id(a),LauncherContacts.id(b));assertEquals(LauncherContacts.id(a),LauncherContacts.id(a.copy(name="New name",number="+91 98765 43210")));assertEquals(a,LauncherContacts.resolve(enabled,LauncherContacts.id(a)));assertNull(LauncherContacts.resolve(enabled.copy(targets=listOf(b)),LauncherContacts.id(a)));assertNull(LauncherContacts.resolve(enabled.copy(enabled=false),LauncherContacts.id(a)))}
 @Test fun intentsDoNotContainCallActionsOrStaleNumber(){val infos=LauncherContacts.infos(ctx,enabled,4);assertEquals(listOf("New contact","Father","Mandu 2"),infos.map{it.shortLabel.toString()});infos.forEach{i->assertEquals(LauncherContacts.ACTION,i.intent!!.action);assertEquals(i.id,i.intent!!.getStringExtra("launcherShortcutId"));assertNull(i.intent!!.getStringExtra("number"));assertEquals(MainActivity::class.java.name,i.intent!!.component!!.className)};assertEquals(0,infos[0].rank);assertEquals(2,infos[2].rank)}
 @Test fun platformCapacityAndOffRespected(){assertEquals(1,LauncherContacts.infos(ctx,enabled,1).size);assertEquals(2,LauncherContacts.infos(ctx,enabled,2).size);assertTrue(LauncherContacts.infos(ctx,enabled,0).isEmpty());assertTrue(LauncherContacts.infos(ctx,enabled.copy(enabled=false),4).isEmpty())}
 @Test fun publishActualManagerThenOffPreservesForeign(){val foreign=android.content.pm.ShortcutInfo.Builder(ctx,"unrelated").setShortLabel("Other").setIntent(android.content.Intent(ctx,MainActivity::class.java).setAction("other")).build();manager.addDynamicShortcuts(listOf(foreign));val s=LauncherContacts.save(ctx,"",enabled);val published=LauncherContacts.publish(ctx);assertTrue(published.requested>=1);assertTrue(manager.dynamicShortcuts.any{it.id==LauncherContacts.NEW});assertTrue(manager.dynamicShortcuts.any{it.id=="unrelated"});LauncherContacts.save(ctx,s.raw,enabled.copy(enabled=false));LauncherContacts.publish(ctx);assertEquals(listOf("unrelated"),manager.dynamicShortcuts.map{it.id})}
 @Test fun replacementAndRemovalNeverResolveOldTarget(){val s=LauncherContacts.save(ctx,"",enabled);LauncherContacts.publish(ctx);val next=LauncherContacts.save(ctx,s.raw,enabled.copy(targets=listOf(b)));LauncherContacts.publish(ctx);assertNull(LauncherContacts.resolve(next.config,LauncherContacts.id(a)));assertFalse(manager.dynamicShortcuts.any{it.id==LauncherContacts.id(a)});assertTrue(manager.dynamicShortcuts.any{it.id==LauncherContacts.id(b)})}
 @Test fun settingsFinderAndBackupPrivacy(){assertTrue(SettingsFinder.search("icon long press").any{it.route=="launchershortcuts"});listOf("backup_rules","data_extraction_rules").forEach{name->val p=ctx.resources.getXml(ctx.resources.getIdentifier(name,"xml",ctx.packageName));var count=0;while(p.next()!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){if(p.eventType==org.xmlpull.v1.XmlPullParser.START_TAG&&p.name=="exclude"&&p.getAttributeValue(null,"domain")=="sharedpref"&&p.getAttributeValue(null,"path")=="launcher_contacts.xml")count++};assertEquals(if(name=="data_extraction_rules")2 else 1,count)}}
}
