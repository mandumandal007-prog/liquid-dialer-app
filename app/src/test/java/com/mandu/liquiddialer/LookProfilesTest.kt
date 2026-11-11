package com.mandu.liquiddialer
import androidx.datastore.preferences.core.*
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class LookProfilesTest{
 @Test fun capturesAndRoundTripsEverySupportedAppearanceField(){val s=LdSettings(theme="sunset",accent=0xFFCC8877,blur=31,glassDepth=0,glassFrost=2,motion=false,fontScale=1.2f,boldText=true,fontStyle=2,iconStyle=7,cleanGlass=true,quietRows=true,refinedHeaders=true,airyCall=true,premiumConfirm=true,dialerLayout=2);val rows=LookProfiles.add(emptyList()," My look ",Look.capture(s));assertEquals("My look",rows.single().name);assertEquals(rows,LookProfiles.decode(LookProfiles.encode(rows)));val p=mutablePreferencesOf();rows.single().look.apply(p);assertEquals(Look.capture(s),Look.capture(p.toSettings()))}
 @Test fun applyOnlyTouchesExplicitAppearanceAllowlist(){val p=mutablePreferencesOf(K.autoRecord to true,K.defaultSim to "SIM2",K.hiddenFeatures to setOf("notes"),K.layoutGrid to "end",K.confirmCall to true,K.callIconConfig to "preserve",K.callCardStyle to "odialer",K.recentsStyle to "list",K.profileName to "PRIVATE",K.lookProfiles to "LIBRARY");val expected=p.asMap();LookProfiles.presets(LdSettings()).first().look.apply(p);expected.forEach{(k,v)->assertEquals(k.name,v,p.asMap()[k])}}
 @Test fun renameAndDeleteDoNotMutateLook(){val rows=LookProfiles.add(emptyList(),"A",Look.capture(LdSettings()));val r=LookProfiles.rename(rows,rows.single().id,"B");assertEquals(rows.single().look,r.single().look);assertEquals("B",r.single().name);assertTrue(LookProfiles.remove(r,r.single().id).isEmpty())}
 @Test(expected=IllegalArgumentException::class) fun libraryLimitIsEnforced(){var r=emptyList<LookProfile>();repeat(7){r=LookProfiles.add(r,"Name",Look.capture(LdSettings()))}}
 @Test(expected=IllegalArgumentException::class) fun blankNameBlocked(){LookProfiles.add(emptyList()," ",Look.capture(LdSettings()))}
 @Test fun allExistingThemesCanBeSaved(){for(t in listOf("midnight","aurora","sunset","ocean","rose","daylight","sand","auto")){val rows=LookProfiles.add(emptyList(),t,Look.capture(LdSettings(theme=t)));assertEquals(rows,LookProfiles.decode(LookProfiles.encode(rows)))}}
 @Test fun presetsPreserveUnspecifiedSettingsAndAreValid(){val s=LdSettings(hiddenFeatures=setOf("notes"),callCardStyle="oneplus",layoutBottom="end",autoRecord=true,callIconConfig="overrides");LookProfiles.presets(s).forEach{it.look.validate();val n=it.look.settings(s);assertEquals(s.hiddenFeatures,n.hiddenFeatures);assertEquals(s.callCardStyle,n.callCardStyle);assertEquals(s.layoutBottom,n.layoutBottom);assertEquals(s.callIconConfig,n.callIconConfig);assertTrue(n.autoRecord)}}
 @Test fun corruptAndUnsupportedLibraryNeverBecomesEmptySuccess(){for(s in listOf("broken","{}","{\"version\":2,\"profiles\":[]}","{\"version\":1,\"profiles\":[{}]}")){assertTrue(runCatching{LookProfiles.decode(s)}.isFailure)}}
 @Test(expected=IllegalArgumentException::class) fun invalidPreviewCannotApply(){Look.capture(LdSettings()).copy(font=Float.NaN).apply(mutablePreferencesOf())}
 @Test(expected=IllegalArgumentException::class) fun duplicateIdsBlocked(){val r=LookProfiles.add(emptyList(),"A",Look.capture(LdSettings())).single();LookProfiles.encode(listOf(r,r))}
}
