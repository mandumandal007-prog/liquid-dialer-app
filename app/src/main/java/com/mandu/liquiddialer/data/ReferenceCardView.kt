package com.mandu.liquiddialer.data
import java.util.Locale
/** Title-only view. Values are never inspected, reordered on disk or used for search/sort. */
object ReferenceCardView{
 enum class Order(val label:String){SAVED("Saved order"),AZ("Title A-Z"),ZA("Title Z-A")}
 fun rows(cards:List<CallReferences.Card>,query:String,order:Order):List<CallReferences.Card>{
  val rows=CallReferences.search(cards,query)
  return when(order){Order.SAVED->rows;Order.AZ->rows.sortedBy{it.title.lowercase(Locale.ROOT)};Order.ZA->rows.sortedWith(compareByDescending{it.title.lowercase(Locale.ROOT)})}
 }
}
