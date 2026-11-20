package com.mandu.liquiddialer
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
import java.io.File
abstract class RecordingFoldActionsFixture:ConverterOptionsFixture(){
 protected val root get()=Repo.recordingsDir(ctx)
 @Before fun resetAudio(){CallManager.recording.value=null;root.listFiles()?.forEach{it.delete()};fold(false)}
 @After fun cleanAudio(){CallManager.recording.value=null;root.listFiles()?.forEach{it.delete()};fold(false)}
 protected fun fold(on:Boolean=true){runBlocking{ctx.repo.set(K.recordingFoldActions,on)};await{LdApp.settings.value.recordingFoldActions==on};settle()}
 protected fun showAudio(night:Boolean=false,many:Boolean=false){seed(night=night);val now=System.currentTimeMillis();for((i,name)in(if(many)(0..23).map{"Memo%02d.m4a".format(it)}else listOf("Office delivery details.m4a","Papa.m4a")).withIndex()){File(root,name).apply{writeText("original audio $i");setLastModified(now-i*60000L)}};mount{RecordingsPage({})};await{rule.onAllNodesWithTag("recordings_count",true).fetchSemanticsNodes().isNotEmpty()}}
 protected fun actions(name:String="Office delivery details.m4a")=hit("recording_actions_$name")
 protected fun original(name:String="Office delivery details.m4a")=File(root,name).readText()
 protected fun noFileActions(name:String){for(a in listOf("rename","export","share","delete"))rule.onNodeWithTag("recording_${a}_$name",true).assertDoesNotExist()}
 protected fun fullFileActions(name:String){for(a in listOf("rename","export","share","delete"))rule.onNodeWithTag("recording_${a}_$name",true).assertExists()}
 protected fun pixels(night:Boolean){showAudio(night);move("recording_play_Office delivery details.m4a");saveShot(rule,"AUDIO537-${if(night)"night"else"day"}-off");fold();move("recording_play_Office delivery details.m4a");saveShot(rule,"AUDIO537-${if(night)"night"else"day"}-on");actions();move("recording_rename_Office delivery details.m4a");saveShot(rule,"AUDIO537-${if(night)"night"else"day"}-expanded");fullFileActions("Office delivery details.m4a");assertEquals("original audio 0",original());assertNull(Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordingFoldActionsUiTest:RecordingFoldActionsFixture(){
 @Test fun defaultOffOneAtATimeAndReversible(){assertFalse(LdSettings().recordingFoldActions);showAudio();fullFileActions("Office delivery details.m4a");rule.onNodeWithTag("recording_actions_Papa.m4a",true).assertDoesNotExist();val before=LdApp.settings.value;fold();assertEquals(before.copy(recordingFoldActions=true),LdApp.settings.value);noFileActions("Office delivery details.m4a");noFileActions("Papa.m4a");actions();fullFileActions("Office delivery details.m4a");noFileActions("Papa.m4a");actions("Papa.m4a");fullFileActions("Papa.m4a");noFileActions("Office delivery details.m4a");actions("Papa.m4a");noFileActions("Papa.m4a");fold(false);fullFileActions("Office delivery details.m4a");fullFileActions("Papa.m4a");assertEquals(before,LdApp.settings.value);assertEquals("original audio 0",original())}
 @Test fun searchAndPagingResetExpansionNotData(){showAudio(many=true);fold();actions("Memo00.m4a");input("recordings_search","Memo");noFileActions("Memo00.m4a");actions("Memo00.m4a");hit("recordings_next");actions("Memo20.m4a");hit("recordings_previous");noFileActions("Memo00.m4a");input("recordings_search","Memo23");rule.onNodeWithTag("recording_Memo23.m4a",true).assertExists();assertEquals(24,RecordingLibrary.list(ctx).size)}
 @Test fun dayPixels(){pixels(false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordingFoldActionsSafetyUiTest:RecordingFoldActionsFixture(){
 @Test fun renameCancelDeleteCancelAndConfirmPreserveRules(){showAudio();fold();actions();hit("recording_rename_Office delivery details.m4a");rule.onNodeWithTag("recording_name",true).performTextReplacement("Changed");hit("recording_rename_cancel");assertEquals("original audio 0",original());hit("recording_delete_Office delivery details.m4a");rule.onNodeWithTag("recording_delete_cancel",true).performClick();settle();assertEquals("original audio 0",original());hit("recording_delete_Office delivery details.m4a");rule.onNodeWithTag("recording_delete_confirm",true).performClick();await{!File(root,"Office delivery details.m4a").exists()};assertEquals("original audio 1",original("Papa.m4a"))}
 @Test fun activeLocksAllActionsEvenAfterExpansion(){showAudio();fold();actions();CallManager.recording.value=CallManager.RecInfo(File(root,"Office delivery details.m4a"),1000,"MIC");settle();for(a in listOf("play","rename","export","share","delete"))rule.onNodeWithTag("recording_${a}_Office delivery details.m4a",true).assertIsNotEnabled();assertEquals("original audio 0",original());CallManager.recording.value=null;settle();rule.onNodeWithTag("recording_play_Office delivery details.m4a",true).assertIsEnabled()}
 @Test fun playbackSurvivesFoldChoiceAndShareStillChooser(){showAudio();fold();org.robolectric.shadows.ShadowMediaPlayer.addMediaInfo(org.robolectric.shadows.util.DataSource.toDataSource(File(root,"Office delivery details.m4a").absolutePath),org.robolectric.shadows.ShadowMediaPlayer.MediaInfo(30000,0));hit("recording_play_Office delivery details.m4a");await{rule.onAllNodesWithText("Pause",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithText("Pause",true).assertExists();actions();fold(false);rule.onNodeWithText("Pause",true).assertExists();fold();rule.onNodeWithText("Pause",true).assertExists();noFileActions("Office delivery details.m4a");actions();hit("recording_share_Office delivery details.m4a");val chooser=Shadows.shadowOf(rule.activity).nextStartedActivity;assertEquals(Intent.ACTION_CHOOSER,chooser.action);assertEquals("original audio 0",original());rule.onNodeWithTag("recording_stop",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordingFoldActionsNightUiTest:RecordingFoldActionsFixture(){
 @Test fun nightLargePixels(){pixels(true)}
 @Test fun copyPickerCancelAndRefreshDoNotChangeOriginal(){showAudio(true);fold();actions();hit("recording_export_Office delivery details.m4a");val pending=Shadows.shadowOf(rule.activity).nextStartedActivityForResult;assertEquals(Intent.ACTION_CREATE_DOCUMENT,pending.intent.action);rule.runOnUiThread{Shadows.shadowOf(rule.activity).receiveResult(pending.intent,android.app.Activity.RESULT_CANCELED,Intent())};settle();assertEquals("original audio 0",original());hit("recordings_refresh");await{rule.onAllNodesWithTag("recordings_count",true).fetchSemanticsNodes().isNotEmpty()};noFileActions("Office delivery details.m4a")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordingFoldActionsSettingsUiTest:OrganizedSettingsFixture(){
 @Before fun off(){runBlocking{ctx.repo.set(K.recordingFoldActions,false)}}
 @Test fun settingsSwitchOnlyChangesLayoutAndFinderResolves(){start();show("settings:recording");val before=LdApp.settings.value;tapTag("recording_fold_actions_switch");await{LdApp.settings.value.recordingFoldActions};assertEquals(before.copy(recordingFoldActions=true),LdApp.settings.value);shot("AUDIO537-settings-on");tapTag("recording_fold_actions_switch");await{!LdApp.settings.value.recordingFoldActions};assertEquals(before,LdApp.settings.value);val e=SettingsFinder.all.single{it.id=="recording-fold-actions"};assertEquals(e,SettingsFinder.resolve(SettingsFinder.route(e)));assertNull(CallManager.recording.value)}
}
