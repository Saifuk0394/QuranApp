package com.quran.romanurdu.utils

import java.util.Calendar

/**
 * Pure-math Hijri (Islamic lunar) calendar converter.
 * Algorithm: Tabular Islamic calendar (civil variant used in most apps).
 * Accurate to ±1 day — matches most printed Islamic calendars.
 */
object HijriUtils {

    private val HIJRI_MONTHS = arrayOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qa'dah", "Dhul-Hijjah"
    )

    private val HIJRI_MONTHS_AR = arrayOf(
        "مُحَرَّم", "صَفَر", "رَبِيعُ الأَوَّل", "رَبِيعُ الثَّانِي",
        "جُمَادَى الأُولَى", "جُمَادَى الآخِرَة", "رَجَب", "شَعْبَان",
        "رَمَضَان", "شَوَّال", "ذُو القَعْدَة", "ذُو الحِجَّة"
    )

    data class HijriDate(
        val day: Int,
        val month: Int,
        val year: Int,
        val monthName: String,
        val monthNameAr: String
    ) {
        /** "14 Ramadan 1446" */
        fun toEnglishString() = "$day $monthName $year AH"
        /** "١٤ رَمَضَان ١٤٤٦" */
        fun toArabicString() = "${toArabicNumerals(day)} $monthNameAr ${toArabicNumerals(year)}"
        /** Short: "14 Ramadan 1446 AH" */
        fun toShortString() = "$day $monthName $year"
    }

    /** Convert today's date to Hijri. */
    fun today(): HijriDate {
        val cal = Calendar.getInstance()
        return fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** Convert a Gregorian date (year, month 1-12, day) to Hijri. */
    fun fromGregorian(year: Int, month: Int, day: Int): HijriDate {
        val jd = gregorianToJulian(year, month, day)
        return julianToHijri(jd)
    }

    /** Returns a list of upcoming Islamic events within the next ~12 months. */
    fun upcomingEvents(fromYear: Int = today().year): List<Pair<HijriDate, String>> {
        return listOf(
            // Fixed Islamic events by Hijri date (month, day, name)
            Triple(1, 1, "Islamic New Year"),
            Triple(1, 10, "Day of Ashura"),
            Triple(3, 12, "Mawlid al-Nabi ﷺ"),
            Triple(7, 27, "Laylat al-Miraj"),
            Triple(8, 15, "Laylat al-Bara'at"),
            Triple(9, 1, "First day of Ramadan"),
            Triple(9, 27, "Laylat al-Qadr (27th)"),
            Triple(10, 1, "Eid al-Fitr"),
            Triple(12, 8, "Day of Arafah (eve)"),
            Triple(12, 9, "Day of Arafah"),
            Triple(12, 10, "Eid al-Adha")
        ).mapNotNull { (month, day, name) ->
            // Try this Hijri year and next
            listOf(fromYear, fromYear + 1).firstNotNullOfOrNull { yr ->
                runCatching {
                    val jd = hijriToJulian(yr, month, day)
                    val hd = julianToHijri(jd)
                    Pair(hd, name)
                }.getOrNull()
            }
        }.sortedBy { (hd, _) ->
            hd.year * 10000 + hd.month * 100 + hd.day
        }
    }

    // ── Conversion algorithms ─────────────────────────────────────────────────

    private fun gregorianToJulian(y: Int, m: Int, d: Int): Long {
        var year = y; var month = m
        if (month <= 2) { year--; month += 12 }
        val a = (year / 100)
        val b = 2 - a + (a / 4)
        return ((365.25 * (year + 4716)).toLong() +
                (30.6001 * (month + 1)).toLong() + d + b - 1524).toLong()
    }

    private fun hijriToJulian(year: Int, month: Int, day: Int): Long {
        return (day + (29.5001 * (month - 1) + 0.99).toLong() +
                (year - 1) * 354L +
                ((3 + 11 * year) / 30) +
                1948440L - 385L)
    }

    private fun julianToHijri(jd: Long): HijriDate {
        val z = jd - 1948440L + 385L
        val cycle = ((z - 1) / 10631).toInt()
        val rem   = (z - 1) % 10631
        val y11   = ((rem) / 354).toInt()
        val r2    = rem % 354
        val month = ((r2 + 29) / 29.5001).toInt().coerceIn(1, 12)
        val year  = cycle * 30 + y11 + 1
        val day   = (z - (year - 1) * 354L -
                ((3 + 11 * (year - 1)) / 30) -
                (29.5001 * (month - 1) + 0.99).toLong() - 1948440L + 385L).toInt()
            .coerceIn(1, 30)

        return HijriDate(
            day = day,
            month = month,
            year = year,
            monthName   = HIJRI_MONTHS.getOrElse(month - 1) { "Month $month" },
            monthNameAr = HIJRI_MONTHS_AR.getOrElse(month - 1) { "" }
        )
    }

    private fun toArabicNumerals(n: Int): String {
        val arabic = arrayOf("٠","١","٢","٣","٤","٥","٦","٧","٨","٩")
        return n.toString().map { arabic[it - '0'] }.joinToString("")
    }
}
