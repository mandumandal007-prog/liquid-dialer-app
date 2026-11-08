package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallAssistUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(check:()->Boolean){rule.waitUntil(10000){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};org.robolectric.shadows.ShadowLooper.idleMainLooper();settle();check()}}
 private fun reveal(tag:String):SemanticsNodeInteraction {
  repeat(20){val n=rule.onNodeWithTag(tag);val bounds=n.fetchSemanticsNode().boundsInRoot;val bottom=rule.onRoot().fetchSemanticsNode().boundsInRoot.bottom;if(n.isDisplayed() && bounds.top>=0 && bounds.bottom<=bottom)return n
   val top=n.fetchSemanticsNode().boundsInRoot.top;rule.onRoot().performTouchInput{if(top<0)swipeDown(durationMillis=120) else swipeUp(durationMillis=120)};settle()}
  error("Not visible $tag")
 }
 private fun show(theme:String="daylight",font:Float=1f){
  rule.mainClock.autoAdvance=false
  runBlocking{ctx.ldStore.edit{it.clear();it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}}
  await{LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font && !LdApp.settings.value.muteReminder && !LdApp.settings.value.audioRouteAlert}
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{CallAssistPage({})}}};settle()
  await{rule.onAllNodesWithTag("assist_mute_toggle",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
 }
 @Test fun bothOffByDefaultAndExplicitTogglesPersistIndependently(){
  show();reveal("assist_mute_toggle").assertTextEquals("Mute reminder: Off").performClick();settle()
  await{runBlocking{LdApp.instance.repo.flow.first().muteReminder}};assertFalse(runBlocking{LdApp.instance.repo.flow.first().audioRouteAlert})
  reveal("assist_route_toggle").performClick();settle();await{runBlocking{LdApp.instance.repo.flow.first().audioRouteAlert}}
  reveal("assist_mute_toggle").performClick();settle();await{!runBlocking{LdApp.instance.repo.flow.first().muteReminder}}
  assertTrue(runBlocking{LdApp.instance.repo.flow.first().audioRouteAlert})
 }
 @Test fun dayPixels(){show();saveShot(rule,"call-assist-day");rule.onRoot().performTouchInput{swipeUp(durationMillis=180)};settle();saveShot(rule,"call-assist-day-lower")}
 @Test fun nightLargePixels(){show("midnight",1.3f);saveShot(rule,"call-assist-night-large");rule.onRoot().performTouchInput{swipeUp(durationMillis=180)};settle();saveShot(rule,"call-assist-night-large-lower")}
}
