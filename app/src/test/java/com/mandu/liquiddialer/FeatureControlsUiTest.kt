package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FeatureControlsUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(600);rule.waitForIdle()}
 private fun await(test:()->Boolean){rule.waitUntil(15000){settle();test()}}
 private fun seed(night:Boolean=false,hidden:Set<String> = emptySet()){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.hiddenFeatures]=hidden;it[K.quietRows]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f}};await{LdApp.settings.value.hiddenFeatures==hidden&&LdApp.settings.value.theme==(if(night)"midnight"else"daylight")}}
 private fun mount(content:@androidx.compose.runtime.Composable ()->Unit){rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{content()}}};settle()}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).onChildren().filter(hasClickAction()).onFirst().performClick();settle()}
 @After fun clean(){runBlocking{ctx.ldStore.edit{it[K.hiddenFeatures]=emptySet();it[K.quietRows]=false;it[K.fontScale]=1f}}}
 @Test fun switchPersistsAndSearchDoesNotChangeChoice(){seed();mount{FeatureControlsPage({},{})};rule.onNode(hasSetTextAction()).performTextInput("Saved notes");settle();click("feature_notes");await{"notes" in LdApp.settings.value.hiddenFeatures};assertEquals(setOf("notes"),LdApp.settings.value.hiddenFeatures);rule.onNode(hasSetTextAction()).performTextClearance();rule.onNode(hasSetTextAction()).performTextInput("Saved notes");settle();click("feature_notes");await{LdApp.settings.value.hiddenFeatures.isEmpty()}}
 @Test fun hiddenRouteHasRecoveryAndDoesNotDeleteChecklist(){seed(hidden=setOf("checklist"));val n="+919876543210";CallChecklist.add(ctx,n,"Keep saved task");val before=CallChecklist.read(ctx,n);var opened="";mount{Page("checklist:$n",rule.activity.state,rule.activity,{}, {opened=it},null,"",{})};rule.onNodeWithTag("hidden_feature_settings",true).assertExists().performClick();assertEquals("featurecontrols",opened);assertEquals(before,CallChecklist.read(ctx,n))}
 @Test fun gatedCallToolsDisappearButIndependentToolsRemain(){seed(hidden=setOf("checklist","estimate","timeline"));mount{CallToolsRow("test",{},{},{},{})};rule.onNodeWithTag("details_checklist",true).assertDoesNotExist();rule.onNodeWithTag("details_estimate",true).assertDoesNotExist();rule.onNodeWithTag("details_timeline",true).assertDoesNotExist();rule.onNodeWithTag("details_brief",true).assertExists()}
 @Test fun dayControlsPixels(){seed();mount{FeatureControlsPage({},{})};saveShot(rule,"CTRL399-day");rule.onNode(hasSetTextAction()).performTextInput("Saved notes");settle();saveShot(rule,"CTRL399-search")}
 @Test fun nightLargeControlsPixels(){seed(true);mount{FeatureControlsPage({},{})};saveShot(rule,"CTRL399-night-large");rule.onNode(hasSetTextAction()).performTextInput("What-if");settle();saveShot(rule,"CTRL399-night-search")}
 @Test fun sharedRowsAndGroupIconsPixels(){seed();mount{Page("settings",rule.activity.state,rule.activity,{}, {},null,"",{})};saveShot(rule,"CTRL399-settings");mount{AppearanceHubPage({},{})};saveShot(rule,"CTRL399-appearance")}
}
