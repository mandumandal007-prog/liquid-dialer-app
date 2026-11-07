package com.mandu.liquiddialer

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisitingCardTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    @Test fun privateDraftRoundTripAndDelete(){
        VisitingCard.delete(ctx);assertEquals(VisitingCard(),VisitingCard.load(ctx))
        val card=VisitingCard("Sample Studio","Design & print","Asha Example","Designer","+91 90000 00000","hello@example.com","example.com","Sample Road, Kolkata")
        VisitingCard.save(ctx,card);assertEquals(card,VisitingCard.load(ctx))
        assertTrue(File(ctx.noBackupFilesDir,"visiting_card_draft.json").exists())
        VisitingCard.delete(ctx);assertFalse(File(ctx.noBackupFilesDir,"visiting_card_draft.json").exists());assertEquals(VisitingCard(),VisitingCard.load(ctx))
    }
    @Test fun corruptedDraftAndFieldLimits(){
        File(ctx.noBackupFilesDir,"visiting_card_draft.json").writeText("broken")
        assertEquals(VisitingCard(),VisitingCard.load(ctx))
        val result=VisitingCard.fromJson("{\"business\":\""+"X".repeat(300)+"\"}")
        assertEquals(300,result.business.length);assertEquals(listOf(0),result.invalidFields);assertFalse(VisitingCard().hasContent);assertTrue(result.hasContent)
        VisitingCard.delete(ctx)
    }
    @Test fun longAndUnicodeTextStaysInsideCardWithoutOmission(){
        val values=listOf("W".repeat(60),"Design print and creative services for your local business", "नाम مثال Bengali বাংলা Example Designer", "Principal designer and local business owner", "+91 90000 00000", "long.business.contact.address.for.design.studio@example.com", "https://www.example.com/design-studio/portfolio-and-printing-services", "Office 204, Second Floor, Example Business Centre, Sample Road, Central District, Kolkata, West Bengal 700001")
        val card=VisitingCard(values[0],values[1],values[2],values[3],values[4],values[5],values[6],values[7])
        val lines=VisitingCardArt.lines(card);assertTrue(lines.size>8)
        assertTrue(lines.all{it.baseline in 100f..641f && it.x>=70f})
        values.forEach{value->assertTrue(lines.joinToString(""){it.text}.replace(" ","").contains(value.replace(" ","")))}
        val out=ByteArrayOutputStream();VisitingCardArt.writePng(card,out)
        val bitmap=BitmapFactory.decodeByteArray(out.toByteArray(),0,out.size());assertEquals(1200,bitmap.width);assertEquals(720,bitmap.height)
        val f=File("build/shots/card-long.png");f.parentFile.mkdirs();f.writeBytes(out.toByteArray());bitmap.recycle()
    }
    @Test fun exportBlackGoldSample(){
        val card=VisitingCard("SAMPLE STUDIO","DESIGN · PRINT · CREATE","Asha Example","Creative designer","+91 90000 00000","hello@example.com","www.example.com","Sample Road, Kolkata")
        val f=File("build/shots/card-sample.png");f.parentFile.mkdirs();f.outputStream().use{VisitingCardArt.writePng(card,it)}
        assertTrue(f.length()>1000)
    }
}
