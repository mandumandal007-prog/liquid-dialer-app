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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlannerUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private lateinit var id:String
    private lateinit var first:CallLists.Entry
    private lateinit var second:CallLists.Entry
    private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
    private fun reveal(tag:String, unmerged:Boolean=false):SemanticsNodeInteraction {
        repeat(20) {
            val node=rule.onNodeWithTag(tag,useUnmergedTree=unmerged)
            if(node.isDisplayed())return node
            val top=node.fetchSemanticsNode().boundsInRoot.top
            rule.onRoot().performTouchInput {if(top<0) swipeDown(durationMillis=120) else swipeUp(durationMillis=120)}
            settle()
        }
        error("Not visible: $tag")
    }
    private fun click(tag:String){println("CLICK "+tag);reveal(tag).performClick();settle()}
    private fun await(check:()->Boolean){try{rule.waitUntil(10000){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};org.robolectric.shadows.ShadowLooper.idleMainLooper();settle();check()};settle()}catch(t:Throwable){throw AssertionError(rule.onRoot(useUnmergedTree=true).printToString(maxDepth=15),t)}}
    @Before fun clean(){listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()};ctx.getSharedPreferences("call_reminders",0).edit().clear().commit()}
    private fun show(theme:String="daylight",font:Float=1f){
        id=CallLists.create(ctx,"Krishna follow-ups").single().id
        CallLists.add(ctx,id,"+442025550123","Office appointment");CallLists.add(ctx,id,"+12025550123","Family overseas")
        first=CallLists.read(ctx).single().entries.first();second=CallLists.read(ctx).single().entries.last()
        CallLists.followUp(ctx,id,first.id,"later","Ask about delivery",123)
        CallLists.schedule(ctx,id,first.id,System.currentTimeMillis()-86400000)
        rule.mainClock.autoAdvance=false;settle()
        runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.confirmCall]=true}}
        await{LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font && LdApp.settings.value.confirmCall}
        rule.runOnUiThread{rule.activity.setContent{LdThemeHost{FollowUpPlannerPage(rule.activity.state,rule.activity,{})}}};settle()
        await{rule.onAllNodesWithTag("planner_reload",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        click("planner_view_OVERDUE");await{rule.onAllNodesWithTag("planner_item_${first.id}",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
    }
    @Test fun planSaveAndClearNeverCreateAlarmOrChangeOutcome(){
        show();val before=CallLists.read(ctx).single().entries.first()
        click("planner_date_${first.id}")
        reveal("planner_date").performTextReplacement("2099-12-24");reveal("planner_time").performTextReplacement("16:30")
        click("planner_date_save");await{CallLists.read(ctx).single().entries.first().dueAt>System.currentTimeMillis()}
        val saved=CallLists.read(ctx).single().entries.first();assertEquals(before.copy(dueAt=saved.dueAt),saved);assertTrue(CallReminders.allRaw(ctx).isEmpty())
        click("planner_view_UPCOMING");click("planner_date_${first.id}");click("planner_date_clear")
        await{CallLists.read(ctx).single().entries.first().dueAt==0L};assertEquals(before.copy(dueAt=0),CallLists.read(ctx).single().entries.first());assertTrue(CallReminders.allRaw(ctx).isEmpty())
    }
    @Test fun exactEntryOpensAndCallDoesNotCompleteIt(){
        show();val before=CallLists.read(ctx)
        click("planner_call_${first.id}");assertEquals(first.number,rule.activity.state.confirmNumber);assertEquals(before,CallLists.read(ctx))
        click("planner_open_${first.id}");reveal("session_name",true).assertTextEquals(first.name);reveal("session_note").assertTextContains("Ask about delivery")
        assertEquals(before,CallLists.read(ctx))
    }
    @Test fun openingExistingReminderDoesNotReplaceIt(){
        show();val at=System.currentTimeMillis()+7200000;CallReminders.set(ctx,first.number,"Existing",at)
        val before=CallReminders.forNumber(ctx,first.number)
        click("planner_reminder_${first.id}")
        reveal("reminder_time").assertExists();assertEquals(before,CallReminders.forNumber(ctx,first.number));saveShot(rule,"planner-independent-reminder")
    }
    @Test fun unsavedPlanAsksBeforeLeaving(){
        show();val before=CallLists.read(ctx);click("planner_date_${first.id}")
        reveal("planner_time").performTextReplacement("18:17");click("planner_date_cancel")
        rule.onNodeWithTag("planner_date_discard").assertExists().performClick();settle()
        assertEquals(before,CallLists.read(ctx));assertTrue(CallReminders.allRaw(ctx).isEmpty())
    }
    @Test fun dayPlannerPixels(){show();reveal("planner_item_${first.id}",true);saveShot(rule,"planner-board-day");click("planner_date_${first.id}");saveShot(rule,"planner-date-day")}
    @Test fun nightLargePlannerPixels(){show("midnight",1.3f);reveal("planner_item_${first.id}",true);saveShot(rule,"planner-board-night-large");reveal("planner_reminder_${first.id}");saveShot(rule,"planner-board-night-large-lower");click("planner_date_${first.id}");saveShot(rule,"planner-date-night-large")}
}
