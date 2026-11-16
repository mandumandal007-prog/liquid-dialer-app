package com.mandu.liquiddialer.data
import java.time.LocalDate
import java.util.Locale
/** Manual civil-date counting. No inferred today, holiday service, calendar, deadline or clock. */
object WeekdayShift {
 data class Result(val target:LocalDate,val count:Int,val walked:Int,val skippedWeekdays:Int,val skippedExclusions:Int,val weekdays:Set<Int>,val exclusions:Set<LocalDate>){
  fun description()="${CallDates.label(target)}\nStart excluded; target is counted. ${kotlin.math.abs(count)} selected dates ${if(count<0)"backward"else"forward"}. Calendar dates walked: $walked; skipped unselected weekdays: $skippedWeekdays; skipped selected-date exclusions: $skippedExclusions.\nIncluded weekdays (Mon=1): ${weekdays.sorted().joinToString(", ")}. Manual excluded dates: ${exclusions.sorted().joinToString(", ").ifEmpty{"none"}}. No official holiday rules or business-day/delivery promise."
 }
 fun exclusions(raw:String):Set<LocalDate>{
  require(raw.length<=500){"Excluded dates text too long"}
  if(raw.isBlank())return emptySet()
  val tokens=raw.trim().split(Regex("[,\\s]+"));require(tokens.size<=20){"At most 20 excluded dates"}
  return tokens.map{CallDates.date(it)}.toSet()
 }
 fun calculate(first:String,countText:String,weekdays:Set<Int>,excluded:String):Result {
  val start=CallDates.date(first);require(Regex("-?[0-9]{1,4}").matches(countText)){"Use signed whole count, no plus/decimal/comma"}
  val count=countText.toInt();require(count!=0&&count in -3660..3660){"Count must be -3660..-1 or 1..3660"}
  require(weekdays.isNotEmpty()&&weekdays.all{it in 1..7}){"Choose at least one weekday"}
  val skip=exclusions(excluded);val selected=weekdays.toSet();val step=if(count<0)-1L else 1L
  var d=start;var done=0;var walked=0;var notSelected=0;var excludedCount=0
  while(done<kotlin.math.abs(count)){
   d=d.plusDays(step);require(d.year in 1900..2199){"Result outside dates1900-2199"};walked++
   if(d.dayOfWeek.value !in selected)notSelected++ else if(d in skip)excludedCount++ else done++
  }
  return Result(d,count,walked,notSelected,excludedCount,selected,skip)
 }
}
