package com.mandu.liquiddialer.data

/** Conservative offline picker, not a phone-validity checker. Never joins separate lines. */
object ClipboardNumbers {
    data class Preview(val numbers: List<String>, val limited: Boolean = false)
    private val candidate = Regex("(?<![\\p{L}\\p{N}+*#])\\+?\\p{Nd}[\\p{Nd} ()\\-\\u00a0]{4,}\\p{Nd}(?![\\p{L}\\p{N}*#])")

    fun preview(text: String): Preview {
        // Don't scan a huge document or treat an email/URL identifier as a number.
        val bounded = text.take(12000)
        val found = linkedSetOf<String>()
        for (line in bounded.lines().filterNot { it.contains('@') || it.contains(Regex("https?:", RegexOption.IGNORE_CASE)) }.flatMap { it.split(',', ';', '|', '/') }) {
            if (line.contains('@') || line.contains(Regex("https?:", RegexOption.IGNORE_CASE))) continue
            // Extensions need manual editing; never silently drop an extension.
            if (line.contains(Regex("(?i)\\b(?:ext|extension|x)\\.?\\s*[:=]?\\s*\\d"))) continue
            for (match in candidate.findAll(line)) {
                val raw = match.value.trim()
                val normalized = buildString {
                    if (raw.startsWith('+')) append('+')
                    raw.forEach { c -> if (Character.isDigit(c)) append(Character.digit(c, 10)) }
                }
                val digits = normalized.count { it.isDigit() }
                if (digits in 7..15 && !raw.matches(Regex("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}"))) found += normalized
                if (found.size > 12) return Preview(found.take(12), true)
            }
        }
        return Preview(found.toList(), text.length > 12000)
    }
}
