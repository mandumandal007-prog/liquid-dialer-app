package com.mandu.liquiddialer
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class CallLayoutDraftTest{
 @Test fun presetsAreValidAndEndAlwaysVisible(){CallLayoutDraft.presets().forEach{(_,v)->v.validate();assertTrue(v.bottom.split(',').contains("end"));assertFalse(v.bottom.contains("!end"))}}
 @Test fun movesPreserveEveryControl(){val b=CallLayoutDraft.capture(LdSettings());val d=b.move(false,"mute",1);assertEquals("keypad",d.grid.split(',').first());assertEquals(b.grid.split(',').toSet(),d.grid.split(',').toSet());assertEquals(b,d.move(false,"mute",-1))}
 @Test fun moveAtEdgeIsNoOp(){val b=CallLayoutDraft.capture(LdSettings());assertEquals(b,b.move(false,"mute",-1))}
 @Test fun hideAndShowAreDraftOnly(){val b=CallLayoutDraft.capture(LdSettings());val d=b.toggle(false,"record");assertTrue(d.grid.contains("!record"));assertEquals(b,d.toggle(false,"record"))}
 @Test fun cannotHideEnd(){assertThrows(IllegalArgumentException::class.java){CallLayoutDraft.capture(LdSettings()).toggle(true,"end")}}
 @Test fun invalidLayoutsRejected(){val b=CallLayoutDraft.capture(LdSettings());listOf(b.copy(grid="mute,mute"),b.copy(bottom="speaker,!end,notes"),b.copy(gridSize=Float.NaN),b.copy(bottomSize=1.5f)).forEach{assertThrows(IllegalArgumentException::class.java){it.validate()}}}
 @Test fun captureNormalizesLikeLegacyLayout(){val s=LdSettings(layoutGrid="mute,mute,garbage,!record",layoutBottom="!end,speaker",gridScale=Float.NaN);val d=CallLayoutDraft.capture(s);d.validate();assertTrue(d.grid.contains("!record"));assertEquals(1f,d.gridSize);assertTrue(d.bottom.startsWith("end,"))}
 @Test fun atomicApplyPreservesEverythingOutsideArrangement(){val p=mutablePreferencesOf();p[K.autoRecord]=true;p[K.theme]="rose";p[K.callCardStyle]="glass";p[K.callPhotoBlur]=2;val before=p.toSettings();val next=CallLayoutDraft.presets()[1].second;CallLayoutDraft.applyIfCurrent(p,CallLayoutDraft.capture(before),next);val after=p.toSettings();assertEquals(before.copy(layoutGrid=next.grid,layoutBottom=next.bottom,gridScale=next.gridSize,bottomScale=next.bottomSize),after)}
 @Test fun staleApplyAndUndoRejectedWithoutWrites(){val p=mutablePreferencesOf();val base=CallLayoutDraft.capture(p.toSettings());val next=CallLayoutDraft.presets()[1].second;CallLayoutDraft.applyIfCurrent(p,base,next);p[K.bottomScale]=1.3f;val before=p.toSettings();assertThrows(IllegalArgumentException::class.java){CallLayoutDraft.applyIfCurrent(p,next,base)};assertEquals(before,p.toSettings())}
 @Test fun undoRestoresOnlyLayout(){val p=mutablePreferencesOf();val base=CallLayoutDraft.capture(p.toSettings());val next=CallLayoutDraft.presets()[2].second;CallLayoutDraft.applyIfCurrent(p,base,next);p[K.confirmCall]=true;CallLayoutDraft.applyIfCurrent(p,next,base);assertEquals(base,CallLayoutDraft.capture(p.toSettings()));assertTrue(p.toSettings().confirmCall)}
}
