package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherContactPhotosUiTest:LauncherContactsUiFixture(){
 private fun photoPage(night:Boolean=false){seed(night);LauncherContactPhotosTest().setup();LauncherContacts.save(ctx,"",LauncherContacts.Config(true,listOf(LauncherContacts.Target("Father",number)),true));rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{LauncherContactsPage(null,{},people)}}};await{rule.onAllNodesWithText("Contact photo snapshot",true,true).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun realProviderPhotoPreviewDayAndOffNeedsSave(){photoPage();rule.mainClock.autoAdvance=true;rule.onNode(hasScrollAction()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,900f)};settle();saveShot(rule,"PHOTO420-preview");rule.onNodeWithContentDescription("Contact photo",useUnmergedTree=true).assertExists();rule.onNode(hasScrollAction()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,-2000f)};settle();rule.onNodeWithText("Contact photos on icon menu",useUnmergedTree=true).performClick();settle();assertTrue(LauncherContacts.read(ctx).config.photos);click("launcher_save");await{!LauncherContacts.read(ctx).config.photos};await{rule.onAllNodesWithText("Initial icon",true,true).fetchSemanticsNodes().isNotEmpty()};assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun permissionLossRefreshUpdatesPreviewToFallback(){photoPage();Shadows.shadowOf(ctx).denyPermissions(android.Manifest.permission.READ_CONTACTS);click("launcher_refresh");await{rule.onAllNodesWithText("Contacts permission needed; using initial",true,true).fetchSemanticsNodes().isNotEmpty()};assertTrue(LauncherContacts.read(ctx).config.photos);rule.onNodeWithContentDescription("Initial icon",useUnmergedTree=true).assertExists()}
 @Test fun nightLargePhotoAndTogglePixels(){photoPage(true);rule.mainClock.autoAdvance=true;rule.onNode(hasScrollAction()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,900f)};settle();saveShot(rule,"PHOTO420-night-preview");rule.onNode(hasScrollAction()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,-2000f)};settle();saveShot(rule,"PHOTO420-night-settings")}
 @Test fun photoSettingParticipatesInDiscard(){photoPage();rule.onNode(hasScrollAction()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,-2000f)};settle();rule.onNodeWithText("Contact photos on icon menu",useUnmergedTree=true).performClick();settle();rule.onNodeWithTag("page_back",true).performClick();settle();rule.onNodeWithTag("launcher_discard",true).assertExists();assertTrue(LauncherContacts.read(ctx).config.photos)}
}
