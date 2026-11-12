package com.mandu.liquiddialer
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class MessageDraftsTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val row=MessageDrafts.Draft("delivery","Delivery update","आपका सामान तैयार है\nPick up: 5pm & later? + yes")
 private val file get()=File(ctx.filesDir,"message_drafts.json")
 private fun bad(f:()->Unit){try{f();fail("must reject")}catch(_:Exception){}}
 @Before fun clean(){file.delete();File(file.path+".bak").delete()}
 @Test fun defaultOffRoundTrip(){assertFalse(LdSettings().messageDrafts);runBlocking{ctx.repo.set(K.messageDrafts,true)};assertTrue(runBlocking{ctx.repo.flow.first()}.messageDrafts)}
 @Test fun emptyReadNoFile(){assertTrue(MessageDrafts.read(ctx).rows.isEmpty());assertFalse(file.exists())}
 @Test fun multilineUnicodePersists(){val a=MessageDrafts.save(ctx,"",listOf(row));assertEquals(a,MessageDrafts.read(ctx));assertEquals(a.raw,MessageDrafts.encode(MessageDrafts.decode(a.raw)))}
 @Test fun controlsBlankAndLengthsReject(){listOf(row.copy(title=""),row.copy(title="x".repeat(51)),row.copy(title="a\nb"),row.copy(body=""),row.copy(body="x".repeat(2001)),row.copy(body="a\tb"),row.copy(id="a/b")).forEach{bad{it.validate()}}}
 @Test fun maxTwentyBoundary(){val rows=(1..20).map{row.copy(id="id$it",title="a".repeat(50),body="b".repeat(2000))};val a=MessageDrafts.save(ctx,"",rows);bad{MessageDrafts.save(ctx,a.raw,rows+row)};assertEquals(rows,MessageDrafts.read(ctx).rows)}
 @Test fun duplicateIdsPreserve(){val a=MessageDrafts.save(ctx,"",listOf(row));bad{MessageDrafts.save(ctx,a.raw,listOf(row,row))};assertEquals(a,MessageDrafts.read(ctx))}
 @Test fun staleEditAndDeletePreserve(){val a=MessageDrafts.save(ctx,"",listOf(row));val b=MessageDrafts.save(ctx,a.raw,listOf(row.copy(body="new")));bad{MessageDrafts.save(ctx,a.raw,emptyList())};assertEquals(b,MessageDrafts.read(ctx))}
 @Test fun deletionOnlyChosenRow(){val a=MessageDrafts.save(ctx,"",listOf(row,row.copy(id="other")));MessageDrafts.save(ctx,a.raw,a.rows.filter{it.id!="delivery"});assertEquals("other",MessageDrafts.read(ctx).rows.single().id)}
 @Test fun corruptAndInvalidUtf8NeverOverwritten(){listOf("broken".toByteArray(),byteArrayOf(0xC3.toByte(),0x28)).forEach{bytes->file.writeBytes(bytes);bad{MessageDrafts.save(ctx,"",listOf(row))};assertArrayEquals(bytes,file.readBytes())}}
 @Test fun strictSchema(){val good=MessageDrafts.encode(listOf(row));listOf(good+"x",good.replace("\"version\":1","\"version\":2"),good.replace("\"title\":\"Delivery update\"","\"title\":12")," ".repeat(MessageDrafts.MAX_BYTES+1)).forEach{bad{MessageDrafts.decode(it)}}}
 @Test fun titleAndBodySearch(){assertEquals(listOf(row),MessageDrafts.matching(listOf(row)," delivery "));assertEquals(listOf(row),MessageDrafts.matching(listOf(row),"सामान"));assertTrue(MessageDrafts.matching(listOf(row),"missing").isEmpty())}
 @Test fun smsIntentExactRecipientAndTextNotSend(){val i=MessageDrafts.sms("+449876543210",row.body);assertEquals(Intent.ACTION_SENDTO,i.action);assertEquals("smsto",i.data!!.scheme);assertEquals("+449876543210",i.data!!.schemeSpecificPart);assertEquals(row.body,i.getStringExtra("sms_body"))}
 @Test fun whatsappEncodedExactlyOnce(){val i=MessageDrafts.whatsapp("+449876543210",row.body);assertEquals(Intent.ACTION_VIEW,i.action);assertEquals("wa.me",i.data!!.host);assertEquals("/449876543210",i.data!!.path);assertEquals(row.body,i.data!!.getQueryParameter("text"))}
 @Test fun localShortSmsAllowedButNotWhatsapp(){assertEquals("139",MessageDrafts.sms("139","Hello").data!!.schemeSpecificPart);bad{MessageDrafts.whatsapp("139","Hello")}}
 @Test fun malformedTargetAndTextRejectBeforeIntent(){listOf("","private","12","+91x9876543210","+919876;123","+९१९८७६५४३२१०").forEach{bad{MessageDrafts.sms(it,"Hi")};bad{MessageDrafts.whatsapp(it,"Hi")}};bad{MessageDrafts.sms("+919876543210","")}}
 @Test fun backupRulesExcludeMainBackupAndPendingFiles(){listOf("backup_rules","data_extraction_rules").forEach{name->val id=ctx.resources.getIdentifier(name,"xml",ctx.packageName);assertTrue(id!=0);val parser=ctx.resources.getXml(id);val paths=mutableListOf<String>();while(parser.next()!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){if(parser.eventType==org.xmlpull.v1.XmlPullParser.START_TAG&&parser.name=="exclude"&&parser.getAttributeValue(null,"domain")=="file")paths+=parser.getAttributeValue(null,"path")};listOf("message_drafts.json","message_drafts.json.bak","message_drafts.json.new").forEach{assertEquals(if(name=="data_extraction_rules")2 else 1,paths.count{p->p==it})}}}
 @Test fun settingsFinderRoutesToWorkspace(){val rows=SettingsFinder.search("message drafts");assertTrue(rows.any{it.route=="messagedrafts"});assertEquals("messagedrafts",SettingsFinder.route(SettingsFinder.all.single{it.id=="message-drafts"}))}

}
