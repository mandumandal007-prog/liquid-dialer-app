package com.mandu.liquiddialer.data
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Civil dates only: no timezone, clock, holidays, calendar or phone access. */
object CallDates {
 enum class Mode(val label:String){SHIFT("Add / subtract"),GAP("Days between"),WEEKDAYS("Mon-Fri count")}
 data class Result(val title:String,val detail:String)
 private val parser=DateTimeFormatter.ofPattern("uuuu-MM-dd",Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
 fun date(raw:String):LocalDate{require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(raw)){"Date YYYY-MM-DD mein bharo"};val d=runCatching{LocalDate.parse(raw,parser)}.getOrElse{throw IllegalArgumentException("Valid calendar date bharo")};require(d.year in 1900..2199){"Dates 1900-01-01 se 2199-12-31 tak"};return d}
 fun label(d:LocalDate)="${d} · ${d.dayOfWeek.name.lowercase(Locale.ROOT).replaceFirstChar{it.titlecase(Locale.ROOT)}}"
 fun shift(raw:String,days:String):Result{
  val d=date(raw);require(Regex("-?[0-9]{1,5}").matches(days)){"Whole days bharo, + sign/comma/decimal nahi"};val n=days.toLong();require(n in -36500..36500){"Shift -36500 se 36500 days tak"};val end=d.plusDays(n);require(end.year in 1900..2199){"Result supported date range ke bahar hai"}
  return Result(label(end),"${label(d)} ${if(n<0)"minus"else"plus"} ${kotlin.math.abs(n)} calendar days. Start date is day 0; weekends included.")
 }
 fun gap(first:String,last:String):Result{val a=date(first);val b=date(last);val days=ChronoUnit.DAYS.between(a,b);return Result("$days calendar days","${label(a)} → ${label(b)}. Signed date difference; same date = 0. Not an inclusive date count or elapsed hours.")}
 /** Count dates in [start,end), not hours. Always forward; equal dates have zero days. */
 fun weekdays(first:String,last:String):Result{
  val a=date(first);val b=date(last);require(b>=a){"Mon-Fri count mein end date start se pehle nahi"};val span=ChronoUnit.DAYS.between(a,b);var count=span/7*5;var d=a.plusDays(span/7*7)
  while(d<b){if(d.dayOfWeek!=DayOfWeek.SATURDAY&&d.dayOfWeek!=DayOfWeek.SUNDAY)count++;d=d.plusDays(1)}
  return Result("$count Monday-Friday dates","${label(a)} → ${label(b)}. Start INCLUDED, end EXCLUDED. Weekends skipped; holidays NOT excluded. Not a business-day or delivery promise.")
 }
 fun calculate(mode:Mode,first:String,last:String,days:String)=when(mode){Mode.SHIFT->shift(first,days);Mode.GAP->gap(first,last);Mode.WEEKDAYS->weekdays(first,last)}
 class History{private val saved=mutableListOf<Result>();val rows get()=saved.toList();fun add(r:Result){saved.add(0,r);while(saved.size>10)saved.removeAt(saved.lastIndex)};fun clear(){saved.clear()}}
}
