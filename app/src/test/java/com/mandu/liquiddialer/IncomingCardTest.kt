package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.CALL_STYLES
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class IncomingCardTest {
 private val d=IncomingCard.Config()
 @Test fun defaultsOff(){assertFalse(d.enabled);assertEquals(d,IncomingCard.decode(LdSettings().incomingCard))}
 @Test fun exactRoundTripAllControls(){val c=d.copy(enabled=true,style="pearl",background="photo",avatar="squircle",avatarSize=125,nameSize=115,number=false,contactType=false,lastCall=false,message=false,whatsapp=false,swap=true,swipe=false,travel=150,confirmCut=true);assertEquals(c,IncomingCard.decode(IncomingCard.encode(c)))}
 @Test fun everySelectableStyleExists(){assertTrue(CALL_STYLES.map{it.key}.containsAll(IncomingCard.styles));IncomingCard.styles.forEach{assertEquals(it,IncomingCard.decode(IncomingCard.encode(d.copy(style=it))).style)}}
 @Test fun backgroundChoicesRoundTrip(){listOf("inherit","blobs","solid","photo").forEach{assertEquals(it,IncomingCard.decode(IncomingCard.encode(d.copy(background=it))).background)}}
 @Test fun shapeChoicesRoundTrip(){listOf("circle","squircle").forEach{assertEquals(it,IncomingCard.decode(IncomingCard.encode(d.copy(avatar=it))).avatar)}}
 @Test fun allSizesRoundTrip(){for(a in listOf(75,100,125))for(n in listOf(85,100,115))for(t in listOf(70,110,150)){val c=d.copy(avatarSize=a,nameSize=n,travel=t);assertEquals(c,IncomingCard.decode(IncomingCard.encode(c)))}}
 @Test fun badStyleRefused(){assertThrows(IllegalArgumentException::class.java){IncomingCard.encode(d.copy(style="missing"))}}
 @Test fun badBackgroundRefused(){assertThrows(IllegalArgumentException::class.java){IncomingCard.encode(d.copy(background="https://invalid"))}}
 @Test fun badShapeRefused(){assertThrows(IllegalArgumentException::class.java){IncomingCard.encode(d.copy(avatar="hidden"))}}
 @Test fun invalidSizeBoundsRefused(){for(a in listOf(-1,0,74,76,126,Int.MAX_VALUE))assertThrows(IllegalArgumentException::class.java){IncomingCard.encode(d.copy(avatarSize=a))};for(n in listOf(0,84,86,116))assertThrows(IllegalArgumentException::class.java){IncomingCard.encode(d.copy(nameSize=n))}}
 @Test fun invalidTravelRefused(){for(t in listOf(0,69,71,111,151))assertThrows(IllegalArgumentException::class.java){IncomingCard.encode(d.copy(travel=t))}}
 @Test fun malformedFallsBackOff(){listOf("","[1]","null","broken","{}","x".repeat(3001)).forEach{assertEquals(d,IncomingCard.decode(it))}}
 @Test fun trailingContentFallsBackOff(){val raw=IncomingCard.encode(d.copy(enabled=true));assertEquals(d,IncomingCard.decode(raw+"x"));assertEquals(d.copy(enabled=true),IncomingCard.decode(raw+"  "))}
 @Test fun wrongTypesFallBackOff(){val o=org.json.JSONObject(IncomingCard.encode(d.copy(enabled=true)));listOf("enabled" to "true","travel" to 110.5,"number" to 1,"style" to 7).forEach{(k,v)->val x=org.json.JSONObject(o.toString()).put(k,v);assertEquals(d,IncomingCard.decode(x.toString()))}}
 @Test fun versionAndMissingKeysRefuse(){val o=org.json.JSONObject(IncomingCard.encode(d.copy(enabled=true)));o.put("version",2);assertEquals(d,IncomingCard.decode(o.toString()));o.put("version",1);o.remove("message");assertEquals(d,IncomingCard.decode(o.toString()))}
 @Test fun offPreservesEntireSharedSettings(){val s=LdSettings(callCardStyle="cyber",callAvatarShape="squircle",swipeAnswer=false,incomingCard=IncomingCard.encode(d));assertSame(s,IncomingCard.effective(s,true))}
 @Test fun outgoingAndActivePreserveEntireSharedSettings(){val s=LdSettings(callCardStyle="cyber",callCardScale=1.3f,confirmBeforeEnd=true,incomingCard=IncomingCard.encode(d.copy(enabled=true)));assertSame(s,IncomingCard.effective(s,false))}
 @Test fun onlyExpectedIncomingFieldsOverride(){val s=LdSettings(callBgStyle="solid",callCardScale=1.2f,confirmBeforeEnd=true,autoRecord=true,spamBadges=false);val c=d.copy(enabled=true,style="pearl",avatar="squircle",number=false,contactType=false,lastCall=false,whatsapp=false,swipe=false);val stored=s.copy(incomingCard=IncomingCard.encode(c));assertEquals(stored.copy(callCardStyle="pearl",callAvatarShape="squircle",callShowNumber=false,showContactType=false,showLastCall=false,waPill=false,swipeAnswer=false),IncomingCard.effective(stored,true))}
 @Test fun persistenceRoundTripAndUnrelatedSettingsKept()=runBlocking{val ctx=ApplicationProvider.getApplicationContext<LdApp>();val c=d.copy(enabled=true,swap=true,travel=70);ctx.ldStore.edit{it[K.incomingCard]=IncomingCard.encode(c);it[K.callCardStyle]="cyber";it[K.confirmBeforeEnd]=true};val stored=ctx.repo.flow.first();assertEquals(c,IncomingCard.decode(stored.incomingCard));assertEquals("cyber",stored.callCardStyle);assertTrue(stored.confirmBeforeEnd)}
 @Test fun finderHasIncomingRoute(){assertTrue(SettingsFinder.all.any{it.id=="incoming-card"&&it.route=="incomingcard"});assertTrue(SettingsFinder.search("incoming calling card").any{it.id=="incoming-card"})}
}
