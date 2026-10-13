package com.quran.romanurdu.utils

import kotlin.math.*

/**
 * Prayer time calculator — pure math, no API.
 * Supports Karachi (ISNA variant used across South Asia / Middle East).
 *
 * Algorithm: based on the PRAYTIMES.ORG formulas (public domain).
 */
object PrayerTimesUtils {

    enum class CalculationMethod(
        val fajrAngle: Double,
        val ishaAngle: Double,
        val label: String
    ) {
        KARACHI(18.0, 18.0, "University of Islamic Sciences, Karachi"),
        ISNA(15.0, 15.0, "Islamic Society of North America"),
        MWL(18.0, 17.0, "Muslim World League"),
        MAKKAH(18.5, 0.0, "Umm Al-Qura, Makkah"),   // Isha = 90 min after Maghrib
        EGYPT(19.5, 17.5, "Egyptian General Authority")
    }

    enum class AsrMethod { STANDARD, HANAFI }   // Shadow ratio 1x or 2x

    data class PrayerTimes(
        val fajr:    String,   // "05:12"
        val sunrise: String,
        val dhuhr:   String,
        val asr:     String,
        val maghrib: String,
        val isha:    String,
        /** Raw minutes since midnight for each (for notification scheduling) */
        val fajrMin:    Int,
        val sunriseMin: Int,
        val dhuhrMin:   Int,
        val asrMin:     Int,
        val maghribMin: Int,
        val ishaMin:    Int
    )

    /**
     * Calculate prayer times for a given latitude, longitude, and date.
     *
     * @param lat       Latitude in decimal degrees
     * @param lng       Longitude in decimal degrees
     * @param timezone  UTC offset in hours (e.g. +5.5 for IST)
     * @param date      Triple of (year, month, day) in Gregorian calendar
     * @param method    Calculation method (default Karachi)
     * @param asr       Asr method (default Standard = Shafi'i/Maliki/Hanbali)
     */
    fun calculate(
        lat: Double,
        lng: Double,
        timezone: Double,
        date: Triple<Int, Int, Int>,
        method: CalculationMethod = CalculationMethod.KARACHI,
        asr: AsrMethod = AsrMethod.STANDARD
    ): PrayerTimes {
        val (year, month, day) = date

        // Julian date
        val jd = julianDate(year, month, day) - lng / (15.0 * 24.0)

        // Sun's position
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(toRad(g)) + 0.020 * sin(toRad(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = toDeg(atan2(cos(toRad(e)) * sin(toRad(l)), cos(toRad(l)))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = toDeg(asin(sin(toRad(e)) * sin(toRad(l))))

        // Transit (solar noon)
        val transit = 12.0 + timezone - lng / 15.0 - eqt

        // Sunrise / Sunset angle
        fun sunAngle(angle: Double): Double {
            val a = -sin(toRad(angle)) - sin(toRad(decl)) * sin(toRad(lat))
            val b = cos(toRad(decl)) * cos(toRad(lat))
            return toDeg(acos(a / b)) / 15.0
        }

        // Asr shadow ratio
        val shadowRatio = if (asr == AsrMethod.STANDARD) 1 else 2
        fun asrAngle(): Double {
            val x = atan(1.0 / (shadowRatio + tan(toRad(abs(lat - decl)))))
            return toDeg(acos(
                (sin(x) - sin(toRad(lat)) * sin(toRad(decl))) /
                        (cos(toRad(lat)) * cos(toRad(decl)))
            )) / 15.0
        }

        val sunriseT = transit - sunAngle(0.833)
        val sunsetT  = transit + sunAngle(0.833)
        val dhuhrT   = transit + 0.0    // slight offset for sun to clear meridian
        val asrT     = transit + asrAngle()
        val fajrT    = transit - sunAngle(method.fajrAngle)
        val ishaT    = if (method == CalculationMethod.MAKKAH)
            sunsetT + 1.5   // 90 min after sunset
        else
            transit + sunAngle(method.ishaAngle)

        fun toTime(h: Double): String {
            val hour = fixHour(h)
            val hh = hour.toInt()
            val mm = ((hour - hh) * 60).roundToInt().coerceIn(0, 59)
            return "%02d:%02d".format(hh, mm)
        }

        fun toMin(h: Double): Int {
            val hour = fixHour(h)
            val hh = hour.toInt()
            val mm = ((hour - hh) * 60).roundToInt().coerceIn(0, 59)
            return hh * 60 + mm
        }

        return PrayerTimes(
            fajr    = toTime(fajrT),
            sunrise = toTime(sunriseT),
            dhuhr   = toTime(dhuhrT),
            asr     = toTime(asrT),
            maghrib = toTime(sunsetT),
            isha    = toTime(ishaT),
            fajrMin    = toMin(fajrT),
            sunriseMin = toMin(sunriseT),
            dhuhrMin   = toMin(dhuhrT),
            asrMin     = toMin(asrT),
            maghribMin = toMin(sunsetT),
            ishaMin    = toMin(ishaT)
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun julianDate(y: Int, m: Int, d: Int): Double {
        var year = y; var month = m
        if (month <= 2) { year--; month += 12 }
        val a = (year / 100.0).toInt()
        val b = 2 - a + (a / 4.0).toInt()
        return (365.25 * (year + 4716)).toInt() +
                (30.6001 * (month + 1)).toInt() + d + b - 1524.5
    }

    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(h: Double): Double  = h - 24.0  * floor(h / 24.0)
    private fun toRad(d: Double) = d * PI / 180.0
    private fun toDeg(r: Double) = r * 180.0 / PI
    private fun Double.roundToInt() = kotlin.math.round(this).toInt()
}
