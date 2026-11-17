package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class ReferenceCardViewTest{
 private val cards=listOf(CallReferences.Card("z","Zulu","alpha secret"),CallReferences.Card("a","alpha","zebra"),CallReferences.Card("b","ALPHA","beta"))
 @Test fun defaultsOff(){assertFalse(LdSettings().referenceSort);assertFalse(LdSettings().referenceManageFold)}
 @Test fun savedOrder(){assertEquals(cards,ReferenceCardView.rows(cards,"",ReferenceCardView.Order.SAVED))}
 @Test fun az(){assertEquals(listOf("a","b","z"),ReferenceCardView.rows(cards,"",ReferenceCardView.Order.AZ).map{it.id})}
 @Test fun zaStableTies(){assertEquals(listOf("z","a","b"),ReferenceCardView.rows(cards,"",ReferenceCardView.Order.ZA).map{it.id})}
 @Test fun filterComposes(){assertEquals(listOf("a","b"),ReferenceCardView.rows(cards,"ALP",ReferenceCardView.Order.AZ).map{it.id})}
 @Test fun valuesNotSearched(){assertTrue(ReferenceCardView.rows(cards,"secret",ReferenceCardView.Order.AZ).isEmpty())}
 @Test fun valuesNotSorted(){assertEquals("zebra",ReferenceCardView.rows(cards,"",ReferenceCardView.Order.AZ).first().value)}
 @Test fun empty(){assertTrue(ReferenceCardView.rows(emptyList(),"",ReferenceCardView.Order.AZ).isEmpty())}
 @Test fun trimmedQuery(){assertEquals(listOf("z"),ReferenceCardView.rows(cards," ZUL ",ReferenceCardView.Order.ZA).map{it.id})}
 @Test fun sourceUntouched(){val before=cards.toList();ReferenceCardView.rows(cards,"",ReferenceCardView.Order.AZ);assertEquals(before,cards)}
 @Test fun identityRetained(){val row=ReferenceCardView.rows(cards,"",ReferenceCardView.Order.AZ).first();assertSame(cards[1],row)}
 @Test fun duplicateTitleAllRetained(){assertEquals(3,ReferenceCardView.rows(cards.map{it.copy(title="same")},"",ReferenceCardView.Order.ZA).size)}
 @Test fun tiesBothDirectionsStable(){for(o in ReferenceCardView.Order.entries)assertEquals(cards.map{it.id},ReferenceCardView.rows(cards.map{it.copy(title="same")},"",o).map{it.id})}
 @Test fun rootLocaleIndependent(){val old=java.util.Locale.getDefault();try{java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr"));val v=listOf(CallReferences.Card("i","I","v"),CallReferences.Card("h","h","v"));assertEquals(listOf("h","i"),ReferenceCardView.rows(v,"",ReferenceCardView.Order.AZ).map{it.id})}finally{java.util.Locale.setDefault(old)}}
 @Test fun finderRoutes(){for(q in listOf("sort reference titles","fold card management"))assertTrue(SettingsFinder.search(q).any{it.route=="callreferences"})}
}
