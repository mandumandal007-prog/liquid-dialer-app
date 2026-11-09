package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class CallIconConfigTest {
 @Test fun defaultsKeepApprovedAppearance(){assertEquals(CallIconConfig(),CallIconConfig.decode(""));assertFalse(CallIconConfig().polish);assertEquals(-1,CallIconConfig().base.style);assertEquals(0,CallIconConfig().base.color)}
 @Test fun roundTripIndependentOverridesAndFallback(){val c=CallIconConfig(CallIconChoice(6,2),mapOf("mic" to CallIconChoice(2,3)),true);val x=CallIconConfig.decode(c.encode());assertEquals(c,x);assertEquals(CallIconChoice(2,3),x.choice("mic"));assertEquals(CallIconChoice(6,2),x.choice("video"));assertEquals(c.base,c.copy(overrides=emptyMap()).choice("mic"))}
 @Test fun malformedUnknownOrOutOfRangeIsSafeDefault(){for(x in listOf("broken","{\"style\":80}","{\"color\":99}","{\"icons\":{\"evil\":{}}}","x".repeat(6001)))assertEquals(CallIconConfig(),CallIconConfig.decode(x))}
 @Test fun registryCoversEveryDeclaredCallScreenGlyph(){assertEquals(CallIconConfig.ids,CALL_ICON_ITEMS.map{it.second.name}.toSet());assertEquals(21,CALL_ICON_ITEMS.size)}
 @Test fun everyPaletteHasContrastOnLightAndDarkSurface(){for(light in listOf(true,false)){val p=paletteFor(if(light)"daylight"else "midnight",Color(0xFF5B8CFF),!light);for(i in 1..6){val c=callIconColor(i,p,p.text);val a=c.luminance();val b=p.bg2.luminance();val contrast=(maxOf(a,b)+.05f)/(minOf(a,b)+.05f);assertTrue("$light palette$i $contrast",contrast>=3f)}}}
}
