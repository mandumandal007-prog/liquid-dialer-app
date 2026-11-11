package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class FeatureControlsTest {
 @Test fun defaultNeverRemovesExistingFeatures(){val s=LdSettings();assertEquals(22,FeatureControls.all.size);assertEquals(FeatureControls.all.size,FeatureControls.ids.size);assertTrue(FeatureControls.all.all{FeatureControls.enabled(s,it.id)})}
 @Test fun eachSwitchRoundTripsWithoutTouchingOtherChoices(){for(id in FeatureControls.ids){val off=FeatureControls.toggled(emptySet(),id,false);assertEquals(setOf(id),off);assertFalse(FeatureControls.enabled(LdSettings(hiddenFeatures=off),id));assertTrue(FeatureControls.ids.filter{it!=id}.all{FeatureControls.enabled(LdSettings(hiddenFeatures=off),it)});assertTrue(FeatureControls.toggled(off,id,true).isEmpty())}}
 @Test fun routesAndRecoveryAreSeparated(){for(id in listOf("notes","brief","timeline","checklist","estimate")){assertEquals(id,FeatureControls.pageId("$id:+919876543210"))};for(route in listOf("settings","settings:calls","featurecontrols","appearancehub","calllayout","editor")){assertNull(FeatureControls.pageId(route));assertTrue(FeatureControls.enabled(LdSettings(hiddenFeatures=FeatureControls.ids),route))}}
 @Test fun staleUnknownKeysCannotHideRecoveryOrNewFunctions(){assertEquals(setOf("notes"),FeatureControls.toggled(setOf("stale","settings"),"notes",false));assertTrue(FeatureControls.enabled(LdSettings(hiddenFeatures=setOf("settings")),"settings"))}
 @Test(expected=IllegalArgumentException::class) fun unknownSwitchCannotBeSaved(){FeatureControls.toggled(emptySet(),"settings",false)}
}
