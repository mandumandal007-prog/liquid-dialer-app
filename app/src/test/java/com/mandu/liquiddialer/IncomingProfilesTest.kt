package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class IncomingProfilesTest {
 private val c=IncomingCard.Config(enabled=true,style="pearl",avatar="squircle",avatarSize=125,nameSize=115,swap=true,swipe=false,travel=70,confirmCut=true,number=false,message=false,whatsapp=false)
 private fun rows()=IncomingProfiles.capture(emptyList(),"Evening",c)
 private fun raw()=IncomingProfiles.encode(rows())
 private fun fail(f:()->Unit){assertThrows(Exception::class.java,f)}
 @Test fun studioDefaultOffAndLibraryEmpty(){assertFalse(LdSettings().incomingStudio);assertEquals(emptyList<IncomingProfiles.Profile>(),IncomingProfiles.decode(LdSettings().incomingProfiles))}
 @Test fun captureExcludesMasterOnly(){val r=rows().single();assertEquals("Evening",r.name);assertEquals(c.copy(enabled=false),r.choices);assertTrue(r.id.isNotBlank())}
 @Test fun encodeDecodeExactAllFields(){val r=rows();assertEquals(r,IncomingProfiles.decode(IncomingProfiles.encode(r)))}
 @Test fun allControlsRetained(){for(a in IncomingCard.styles)for(t in listOf(70,110,150)){val r=IncomingProfiles.capture(emptyList(),"Sample",c.copy(style=a,travel=t,background="photo",lastCall=false,contactType=false));assertEquals(r,IncomingProfiles.decode(IncomingProfiles.encode(r)))}}
 @Test fun sixProfilesLimitWithoutDropping(){var r=emptyList<IncomingProfiles.Profile>();repeat(6){r=IncomingProfiles.capture(r,"Profile $it",c)};val before=r;fail{IncomingProfiles.capture(r,"Seventh",c)};assertEquals(before,r);assertEquals(6,IncomingProfiles.decode(IncomingProfiles.encode(r)).size)}
 @Test fun blankLongControlNamesRefused(){for(n in listOf("","  ","x".repeat(33),"Line\nname","Tab\tname"))fail{IncomingProfiles.capture(emptyList(),n,c)}}
 @Test fun trimsNameUnicodePreserved(){val r=IncomingProfiles.capture(emptyList(),"  रात वाला  ",c);assertEquals("रात वाला",r.single().name);assertEquals(r,IncomingProfiles.decode(IncomingProfiles.encode(r)))}
 @Test fun duplicateCaseInsensitiveNamesRefused(){fail{IncomingProfiles.capture(rows()," EVENING ",c)}}
 @Test fun renameKeepsIdentityAndChoices(){val r=rows();val x=IncomingProfiles.rename(r,r[0].id,"Work");assertEquals(r[0].copy(name="Work"),x.single())}
 @Test fun renameMissingAndCollisionRefused(){val r=IncomingProfiles.capture(rows(),"Work",c);fail{IncomingProfiles.rename(r,r[0].id,"work")};fail{IncomingProfiles.rename(r,"missing","Other")}}
 @Test fun removeOnlySelectedProfile(){val r=IncomingProfiles.capture(rows(),"Work",c);assertEquals(listOf(r[1]),IncomingProfiles.remove(r,r[0].id));fail{IncomingProfiles.remove(r,"missing")}}
 @Test fun malformedNeverPretendsEmpty(){listOf("{}","broken","[]","null",raw()+"x","x".repeat(24001)).forEach{fail{IncomingProfiles.decode(it)}}}
 @Test fun corruptNestedChoicesRefused(){val o=org.json.JSONObject(raw());o.getJSONArray("profiles").getJSONObject(0).getJSONObject("choices").put("style","hidden");fail{IncomingProfiles.decode(o.toString())}}
 @Test fun masterOnInsideSavedDataRefused(){val o=org.json.JSONObject(raw());o.getJSONArray("profiles").getJSONObject(0).getJSONObject("choices").put("enabled",true);fail{IncomingProfiles.decode(o.toString())}}
 @Test fun wrongTypedNestedDataRefused(){val o=org.json.JSONObject(raw());o.getJSONArray("profiles").getJSONObject(0).getJSONObject("choices").put("swipe","true");fail{IncomingProfiles.decode(o.toString())}}
 @Test fun duplicateIdsRefused(){val r=IncomingProfiles.capture(rows(),"Work",c);fail{IncomingProfiles.encode(listOf(r[0],r[1].copy(id=r[0].id)))}}
 @Test fun unsupportedVersionRefused(){val o=org.json.JSONObject(raw()).put("version",2);fail{IncomingProfiles.decode(o.toString())}}
 @Test fun planPreservesMasterOff(){val r=rows();val lib=IncomingProfiles.encode(r);val before=IncomingCard.encode(IncomingCard.Config());val plan=IncomingProfiles.plan(before,lib,r[0].id);assertEquals(c.copy(enabled=false),IncomingCard.decodeStrict(plan.after));assertEquals(before,plan.before)}
 @Test fun planPreservesMasterOn(){val r=rows();val lib=IncomingProfiles.encode(r);val before=IncomingCard.encode(IncomingCard.Config(enabled=true));val plan=IncomingProfiles.plan(before,lib,r[0].id);assertTrue(IncomingCard.decodeStrict(plan.after).enabled);assertEquals(c,IncomingCard.decodeStrict(plan.after))}
 @Test fun missingCurrentUsesDefaultsWithoutEnabling(){val r=rows();val plan=IncomingProfiles.plan("",IncomingProfiles.encode(r),r[0].id);assertFalse(IncomingCard.decodeStrict(plan.after).enabled);assertEquals("",plan.before)}
 @Test fun exactPlanAppliesAndCanUndo(){val r=rows();val lib=IncomingProfiles.encode(r);val plan=IncomingProfiles.plan("",lib,r[0].id);assertEquals(plan.after,IncomingProfiles.apply(plan,"",lib));assertEquals("",IncomingProfiles.undo(plan,plan.after))}
 @Test fun changedCurrentMasterOrLibraryRefuses(){val r=rows();val lib=IncomingProfiles.encode(r);val plan=IncomingProfiles.plan("",lib,r[0].id);fail{IncomingProfiles.apply(plan,IncomingCard.encode(IncomingCard.Config(enabled=true)),lib)};fail{IncomingProfiles.apply(plan,"",IncomingProfiles.encode(IncomingProfiles.rename(r,r[0].id,"Renamed")))}}
 @Test fun tamperedPlanAndUndoRefused(){val r=rows();val lib=IncomingProfiles.encode(r);val p=IncomingProfiles.plan("",lib,r[0].id);fail{IncomingProfiles.apply(p.copy(after="bad"),"",lib)};fail{IncomingProfiles.undo(p.copy(before="bad"),p.after)};fail{IncomingProfiles.undo(p,"changed")}}
 @Test fun corruptCurrentNeverOverwritten(){val r=rows();fail{IncomingProfiles.plan("broken",IncomingProfiles.encode(r),r[0].id)}}
 @Test fun differencesExcludeMasterAndEnumerate14(){assertTrue(IncomingProfiles.differences(c,c.copy(enabled=false)).isEmpty());val d=IncomingCard.Config();val other=d.copy(style="pearl",background="solid",avatar="squircle",avatarSize=75,nameSize=85,number=false,contactType=false,lastCall=false,message=false,whatsapp=false,swap=true,swipe=false,travel=70,confirmCut=true);assertEquals(14,IncomingProfiles.differences(d,other).size);assertTrue(IncomingProfiles.differences(d,other).any{it=="Swipe travel dp: 110 → 70"})}
 @Test fun persistenceAndOtherSettingsPreserved()=runBlocking{val ctx=ApplicationProvider.getApplicationContext<LdApp>();val lib=raw();ctx.ldStore.edit{it[K.incomingProfiles]=lib;it[K.incomingStudio]=true;it[K.callCardStyle]="cyber";it[K.incomingCard]=IncomingCard.encode(c)};val s=ctx.repo.flow.first();assertEquals(lib,s.incomingProfiles);assertTrue(s.incomingStudio);assertEquals("cyber",s.callCardStyle);assertEquals(c,IncomingCard.decodeStrict(s.incomingCard))}
 @Test fun finderStudioRouteExists(){assertTrue(SettingsFinder.all.any{it.id=="incoming-studio"&&it.route=="incomingstudio"});assertTrue(SettingsFinder.search("incoming profiles compare").any{it.route=="incomingstudio"})}
}
