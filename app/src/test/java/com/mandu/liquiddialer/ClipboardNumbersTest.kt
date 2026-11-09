package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.ClipboardNumbers
import org.junit.Test
import org.junit.Assert.*
class ClipboardNumbersTest {
 @Test fun namesAndSeparateNumbersStaySeparate(){assertEquals(listOf("+919876543210","9123456789"),ClipboardNumbers.preview("Rahul: +91 98765 43210\nPriya: 91234-56789").numbers)}
 @Test fun oneLineCommaAndDuplicate(){assertEquals(listOf("9876543210","9123456789"),ClipboardNumbers.preview("9876543210, 9123456789\n98765 43210").numbers)}
 @Test fun neverConcatenatesTwoLongSpaceSeparatedNumbers(){assertTrue(ClipboardNumbers.preview("9876543210 9123456789").numbers.isEmpty())}
 @Test fun rejectsShortDatesLongIdentifiersAndCodes(){for(s in listOf("2026-11-09","1234567890123456","*123#","*9876543210#","https://a.test/9876543210","user9876543210@example.com"))assertTrue(s,ClipboardNumbers.preview(s).numbers.isEmpty())}
 @Test fun extensionNotSilentlyDropped(){assertTrue(ClipboardNumbers.preview("Office 9876543210 ext 42").numbers.isEmpty())}
 @Test fun internationalNumberKeptWithoutCountryGuess(){assertEquals(listOf("+442079460123"),ClipboardNumbers.preview("+44 (20) 7946-0123").numbers)}
 @Test fun unicodeDigitsNormalized(){assertEquals(listOf("9876543210"),ClipboardNumbers.preview("९८७६५४३२१०").numbers)}
 @Test fun resultBounded(){val p=ClipboardNumbers.preview((1..15).joinToString("\n"){"9876543"+it.toString().padStart(3,'0')});assertEquals(12,p.numbers.size);assertTrue(p.limited)}
}
