package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class IvrMenusTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun m()=IvrMenus.Menu("test","Service menu","12#*")
 @Before fun clean(){ctx.getSharedPreferences("ivr_menus",0).edit().clear().commit()}
 @Test fun emptyDefaultsAndHelperOff(){assertTrue(IvrMenus.decode("").isEmpty());assertFalse(LdSettings().ivrHelper)}
 @Test fun strictRoundtrip(){val xs=listOf(m(),m().copy(id="two",name="Hindi choices",digits="90"));assertEquals(xs,IvrMenus.decode(IvrMenus.encode(xs)))}
 @Test fun noPhoneOrPauseDialStrings(){listOf("+911234", "12,3","12;3","tel:123","12 3","123\n","１２").forEach{assertThrows(IllegalArgumentException::class.java){m().copy(digits=it).validate()}}}
 @Test fun onlyKeypadAlphabet(){m().copy(digits="0123456789*#").validate()}
 @Test fun noEmptyOrOversizeDigits(){listOf("","1".repeat(25)).forEach{assertThrows(IllegalArgumentException::class.java){m().copy(digits=it).validate()}};m().copy(digits="1".repeat(24)).validate()}
 @Test fun labelsAndIdsBounded(){listOf(m().copy(name=""),m().copy(name="x".repeat(41)),m().copy(name="bad\nlabel"),m().copy(id="../oops")).forEach{assertThrows(IllegalArgumentException::class.java){it.validate()}}}
 @Test fun maxAndUniqueIds(){assertThrows(IllegalArgumentException::class.java){IvrMenus.encode(List(13){m().copy(id="m$it")})};assertThrows(IllegalArgumentException::class.java){IvrMenus.encode(listOf(m(),m()))}}
 @Test fun malformedAndTrailingRejected(){val raw=IvrMenus.encode(listOf(m()));listOf("broken",raw+"{}",raw+"\u0000",raw.replace("\"version\":1","\"version\":2"),raw.replace("\"digits\":\"12#*\"","\"digits\":123")).forEach{assertThrows(Exception::class.java){IvrMenus.decode(it)}}}
 @Test fun atomicSaveAndUnrelatedSettingsUnchanged(){val before=LdApp.settings.value;val next=IvrMenus.save(ctx,"",listOf(m()));assertEquals(next,IvrMenus.raw(ctx));assertEquals(before,LdApp.settings.value)}
 @Test fun staleSaveNeverOverwrites(){val raw=IvrMenus.save(ctx,"",listOf(m()));assertThrows(IllegalArgumentException::class.java){IvrMenus.save(ctx,"",emptyList())};assertEquals(raw,IvrMenus.raw(ctx))}
 @Test fun invalidSaveLeavesOriginal(){val raw=IvrMenus.save(ctx,"",listOf(m()));assertThrows(IllegalArgumentException::class.java){IvrMenus.save(ctx,raw,listOf(m().copy(digits="")))};assertEquals(raw,IvrMenus.raw(ctx))}
 @Test fun corruptDataPreserved(){ctx.getSharedPreferences("ivr_menus",0).edit().putString("data","broken").commit();assertThrows(Exception::class.java){IvrMenus.decode(IvrMenus.raw(ctx))};assertEquals("broken",IvrMenus.raw(ctx))}
 @Test fun selectionDoesNotSend(){val session=IvrMenus.Session(m());assertEquals(0,session.index);assertEquals('1',session.next())}
 @Test fun oneTapOneDigitAndNoAutoAdvance(){val sent=mutableListOf<Char>();val s=IvrMenus.Session(m());assertTrue(s.sendOne{sent.add(it)});assertEquals(listOf('1'),sent);assertEquals('2',s.next());assertEquals(1,s.index)}
 @Test fun failureKeepsPosition(){val s=IvrMenus.Session(m());assertFalse(s.sendOne{false});assertEquals(0,s.index);assertEquals('1',s.next())}
 @Test fun completedSequenceCannotReplay(){val s=IvrMenus.Session(m());val sent=mutableListOf<Char>();repeat(6){s.sendOne{sent.add(it)}};assertEquals(listOf('1','2','#','*'),sent);assertNull(s.next());assertEquals(4,s.index)}
 @Test fun resetOnlyMovesPosition(){val s=IvrMenus.Session(m());var sent=0;s.sendOne{sent++;true};s.reset();assertEquals(1,sent);assertEquals(0,s.index);assertEquals('1',s.next())}
}
