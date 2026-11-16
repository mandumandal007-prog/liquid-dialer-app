package com.mandu.liquiddialer.data
object WeekdayChoices{
 enum class Shortcut(val label:String,val days:Set<Int>){WEEKDAYS("Mon-Fri",setOf(1,2,3,4,5)),ALL("All seven days",(1..7).toSet()),WEEKENDS("Sat-Sun",setOf(6,7))}
 fun summary(raw:String):String=runCatching{val n=WeekdayShift.exclusions(raw).size;if(n==0)"No manual exclusions"else"$n unique manual excluded dates"}.getOrElse{"Manual exclusions need checking"}
}
