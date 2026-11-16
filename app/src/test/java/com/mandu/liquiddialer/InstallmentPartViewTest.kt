package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class InstallmentPartViewTest{
 private fun p()=InstallmentPreview.preview(CallEstimate.Draft(lines=listOf(CallEstimate.line("A","1","10","a"))),"0","3","2026-11-16","30")
 @Test fun defaultsOff(){assertFalse(LdSettings().installmentFind);assertFalse(LdSettings().installmentPartsFold)}
 @Test fun firstPart(){assertEquals(1,InstallmentPartView.index("1",3))}
 @Test fun lastPart(){assertEquals(36,InstallmentPartView.index("36",36))}
 @Test fun leadingZero(){assertEquals(2,InstallmentPartView.index("02",3))}
 @Test fun zeroRefused(){assertTrue(runCatching{InstallmentPartView.index("0",3)}.isFailure)}
 @Test fun outsidePreviewRefused(){assertTrue(runCatching{InstallmentPartView.index("4",3)}.isFailure)}
 @Test fun noSilentTruncation(){assertTrue(runCatching{InstallmentPartView.index("100",36)}.isFailure)}
 @Test fun signsRefused(){for(v in listOf("+1","-1"))assertTrue(runCatching{InstallmentPartView.index(v,3)}.isFailure)}
 @Test fun decimalsRefused(){for(v in listOf("1.0","1e0"))assertTrue(runCatching{InstallmentPartView.index(v,3)}.isFailure)}
 @Test fun whitespaceRefused(){for(v in listOf(" 1","1 ",""))assertTrue(runCatching{InstallmentPartView.index(v,3)}.isFailure)}
 @Test fun unicodeRefused(){assertTrue(runCatching{InstallmentPartView.index("१",3)}.isFailure)}
 @Test fun invalidPreviewCountRefused(){for(n in listOf(0,37,-1))assertTrue(runCatching{InstallmentPartView.index("1",n)}.isFailure)}
 @Test fun allPreservesOrder(){val p=p();assertEquals(p.parts,InstallmentPartView.rows(p.parts,null))}
 @Test fun originalIndexNotPosition(){val p=p();assertEquals(listOf(p.parts[1]),InstallmentPartView.rows(p.parts,2))}
 @Test fun originalAmountsDates(){val p=p();val row=InstallmentPartView.rows(p.parts,3).single();assertEquals("3.33",row.amount.toPlainString());assertEquals("2027-01-15",row.date.toString())}
 @Test fun selectionDoesNotChangePreview(){val p=p();val before=p.copy(parts=p.parts.toList());InstallmentPartView.rows(p.parts,2);assertEquals(before,p)}
 @Test fun allThirtySixSelectable(){for(i in 1..36)assertEquals(i,InstallmentPartView.index(i.toString(),36))}
 @Test fun finderRoutes(){for(q in listOf("find preview part","fold dated parts"))assertTrue(SettingsFinder.search(q).any{it.route=="installmentpreview"})}
}
