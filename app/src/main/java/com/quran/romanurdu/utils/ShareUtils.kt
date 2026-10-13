package com.quran.romanurdu.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.quran.romanurdu.data.model.AyahEntity
import java.io.File
import java.io.FileOutputStream

/**
 * Generates a stylised image card for an ayah and shares it via Android share sheet.
 *
 * Card layout (top → bottom):
 *   • Gold header bar with بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ
 *   • Arabic text (large, RTL)
 *   • Roman-English transliteration (italic)
 *   • Surah reference  e.g.  Surah Al-Fatihah (1:1)
 *   • Dark green footer bar with app name
 */
object ShareUtils {

    private const val CARD_WIDTH  = 1080
    private const val CARD_HEIGHT = 800

    // Theme colours
    private val COLOR_GREEN  = Color.parseColor("#1B6B3A")
    private val COLOR_GOLD   = Color.parseColor("#D4AF37")
    private val COLOR_BG     = Color.parseColor("#FAFAF7")
    private val COLOR_TEXT   = Color.parseColor("#1A1A1A")
    private val COLOR_SUBTLE = Color.parseColor("#666666")

    fun shareAyah(
        context: Context,
        ayah: AyahEntity,
        surahName: String
    ) {
        val bitmap = buildAyahCard(ayah, surahName)
        val uri = saveBitmapAndGetUri(context, bitmap, "quran_share_${ayah.surahNumber}_${ayah.ayahNumber}.png")
        launchShareSheet(context, uri, "Quran ${surahName} (${ayah.surahNumber}:${ayah.ayahNumber})")
    }

    private fun buildAyahCard(ayah: AyahEntity, surahName: String): Bitmap {
        val bm = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)

        // ── Background ──────────────────────────────────────────────────
        val bgPaint = Paint().apply { color = COLOR_BG }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), bgPaint)

        // Subtle texture lines
        val linePaint = Paint().apply {
            color = Color.parseColor("#E8E8E0")
            strokeWidth = 1f
        }
        var y = 100f
        while (y < CARD_HEIGHT - 80f) {
            canvas.drawLine(60f, y, (CARD_WIDTH - 60).toFloat(), y, linePaint)
            y += 48f
        }

        // ── Header bar ──────────────────────────────────────────────────
        val headerPaint = Paint().apply { color = COLOR_GREEN }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), 90f, headerPaint)

        // Gold accent line below header
        val goldBarPaint = Paint().apply { color = COLOR_GOLD; strokeWidth = 4f }
        canvas.drawLine(0f, 90f, CARD_WIDTH.toFloat(), 90f, goldBarPaint)

        // Bismillah in header
        val bismPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ", CARD_WIDTH / 2f, 60f, bismPaint)

        // ── Arabic ayah text ─────────────────────────────────────────────
        val arabicPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 52f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
        val arabicLines = wrapText(ayah.arabic, arabicPaint, CARD_WIDTH - 120)
        var textY = 170f
        for (line in arabicLines) {
            canvas.drawText(line, CARD_WIDTH / 2f, textY, arabicPaint)
            textY += arabicPaint.fontSpacing
        }

        // ── Gold divider ─────────────────────────────────────────────────
        textY += 20f
        canvas.drawLine(120f, textY, (CARD_WIDTH - 120).toFloat(), textY, goldBarPaint)
        textY += 40f

        // ── Transliteration (Roman English) ──────────────────────────────
        val transText = ayah.transliteration
        val transPaint = Paint().apply {
            color = COLOR_GREEN
            textSize = 30f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val transLines = wrapText(transText, transPaint, CARD_WIDTH - 120)
        // Limit to 4 lines max for card space
        val displayTrans = transLines.take(4)
        for (line in displayTrans) {
            if (textY + transPaint.fontSpacing > CARD_HEIGHT - 120f) break
            canvas.drawText(line, CARD_WIDTH / 2f, textY, transPaint)
            textY += transPaint.fontSpacing
        }

        // ── Footer bar ───────────────────────────────────────────────────
        val footerPaint = Paint().apply { color = COLOR_GREEN }
        canvas.drawRect(0f, (CARD_HEIGHT - 80).toFloat(), CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), footerPaint)

        // Gold accent line above footer
        canvas.drawLine(0f, (CARD_HEIGHT - 80).toFloat(), CARD_WIDTH.toFloat(), (CARD_HEIGHT - 80).toFloat(), goldBarPaint)

        // Surah reference in footer
        val refPaint = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "Surah $surahName  •  ${ayah.surahNumber}:${ayah.ayahNumber}",
            CARD_WIDTH / 2f,
            (CARD_HEIGHT - 80 + 50).toFloat(),
            refPaint
        )

        // App credit (small, right-aligned)
        val creditPaint = Paint().apply {
            color = Color.WHITE.withAlpha(150)
            textSize = 18f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("Quran App", (CARD_WIDTH - 30).toFloat(), (CARD_HEIGHT - 14).toFloat(), creditPaint)

        return bm
    }

    /** Naive word-wrap for canvas text drawing. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) {
                current = StringBuilder(test)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap, filename: String): Uri {
        val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(shareDir, filename)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun launchShareSheet(context: Context, uri: Uri, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Ayah").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

// Extension helper
private fun Int.withAlpha(alpha: Int): Int = Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
