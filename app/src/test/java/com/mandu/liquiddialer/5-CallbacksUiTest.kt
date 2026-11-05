package com.mandu.liquiddialer
import androidx.activity.compose.setContent
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallbacksUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 @Test fun explicitCallUsesExistingConfirmationAndDetails()=check("daylight",1f,"day")
 @Test fun largeNightActionsRemainVisible()=check("midnight",1.3f,"night-large")
 private fun check(theme:String,font:Float,suffix:String) {
  rule.mainClock.autoAdvance=false;settle()
  val ctx=ApplicationProvider.getApplicationContext<LdApp>()
  runBlocking {ctx.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.confirmCall]=true}}
  rule.waitUntil(15000){LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font && LdApp.settings.value.confirmCall}
  org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
  var opened=""
  rule.runOnUiThread {
   val st=rule.activity.state;st.recentsLoaded=true;st.recentsError=false
   st.recents=listOf(Recent(1,"+91 98765 43210","Papa",3,System.currentTimeMillis(),0,null,null))
   rule.activity.setContent {LdThemeHost {CallbacksPage(st,rule.activity,{}, {opened=it})}}
  };settle()
  assertNull(rule.activity.state.confirmNumber)
  rule.onNodeWithTag("callback_call_1").assertIsDisplayed()
  rule.onNodeWithTag("callback_details_1").assertIsDisplayed()
  saveShot(rule,"268-callbacks-"+suffix)
  rule.onNodeWithTag("callback_details_1").performClick();settle();assertEquals("+91 98765 43210",opened)
  rule.onNodeWithTag("callback_call_1").performClick();settle();assertEquals("+91 98765 43210",rule.activity.state.confirmNumber)
  rule.runOnUiThread {rule.activity.state.recents=rule.activity.state.recents+Recent(2,"+91 98765 43210",null,2,System.currentTimeMillis()+1000,15,null,null)};settle()
  rule.onNodeWithText("Abhi pending missed call nahi",useUnmergedTree=true).assertIsDisplayed()
 }
 @Test fun permissionLoadingAndErrorNeverShowCallActions() {
  rule.mainClock.autoAdvance=false;settle()
  val ctx=ApplicationProvider.getApplicationContext<LdApp>()
  val state=AppState().apply {recentsLoaded=true}
  org.robolectric.Shadows.shadowOf(ctx).denyPermissions(android.Manifest.permission.READ_CALL_LOG)
  rule.runOnUiThread {rule.activity.setContent {LdThemeHost {CallbacksPage(state,rule.activity,{}, {})}}};settle()
  rule.onNodeWithText("Call history ki permission chahiye",useUnmergedTree=true).assertIsDisplayed()
  org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
  rule.runOnUiThread {state.recentsLoaded=false;state.recentsError=false;rule.activity.setContent {LdThemeHost {CallbacksPage(state,rule.activity,{}, {})}}};settle()
  rule.onNodeWithText("Call history taiyaar ho rahi hai",useUnmergedTree=true).assertIsDisplayed()
  rule.runOnUiThread {state.recentsError=true};settle()
  rule.onNodeWithText("Call history load nahi hui",useUnmergedTree=true).assertIsDisplayed()
  assertNull(rule.activity.state.confirmNumber)
 }

}
