package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.FakeCalls
import com.mandu.liquiddialer.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FakeCallShotTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1000);rule.waitForIdle()}
 @Before fun setup(){FakeCalls.cancel(ctx);Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)}
 @After fun cleanup(){FakeCalls.cancel(ctx);RuntimeEnvironment.setFontScale(1f)}
 @Test fun setupShowsLimitsBeforeScheduling(){
  rule.activity.setContent{LdThemeHost{FakeCallPage{}}};settle()
  rule.onNodeWithText("Sirf simulation",useUnmergedTree=true).assertExists()
  saveShot(rule,"I-fake-setup")
  rule.onNodeWithTag("fake_delay_30000").performScrollTo().performClick();settle()
  assertNotNull(FakeCalls.current(ctx));rule.onNodeWithTag("fake_cancel").performScrollTo().assertExists()
  saveShot(rule,"I-fake-pending")
  rule.onNodeWithTag("fake_cancel").performClick();settle();assertNull(FakeCalls.current(ctx))
 }
 @Test fun simulationAnswerAndLongRedEnd(){
  var answers=0;var ended=0
  rule.activity.setContent{LdThemeHost{FakeCallView("Demo caller",{answers++},{ended++})}};settle()
  saveShot(rule,"I-fake-incoming")
  rule.onNodeWithTag("fake_answer").performClick();settle();assertEquals(1,answers)
  saveShot(rule,"I-fake-connected")
  rule.onNodeWithTag("end_call").performClick();assertEquals(1,ended)
 }
 @Test fun notificationsBlockedShowsWarningAndDisabledSchedule(){
  val nm=ctx.getSystemService(android.app.NotificationManager::class.java)
  nm.createNotificationChannel(android.app.NotificationChannel(FakeCalls.CHANNEL,"Blocked",android.app.NotificationManager.IMPORTANCE_NONE))
  rule.activity.setContent{LdThemeHost{FakeCallPage{}}};settle()
  rule.onNodeWithText("Notifications band hain",useUnmergedTree=true).assertExists()
  rule.onNodeWithTag("fake_delay_30000").performScrollTo().assertIsNotEnabled()
  saveShot(rule,"I-fake-notifications-blocked")
 }
 @Test fun largeFontViewRemainsUsable(){
  RuntimeEnvironment.setFontScale(1.3f)
  rule.activity.setContent{LdThemeHost{FakeCallView("Long demo caller name",{},{})}};settle()
  rule.onNodeWithTag("end_call").assertIsDisplayed();saveShot(rule,"I-fake-largefont")
 }
}
