package com.bikeshare.app.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.bikeshare.app.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Draws a pill-shaped marker matching the Citi Bike style:
 *   [ 🚲 ]  7
 * Left: darker circle with bike emoji. Right: bold white count.
 * Green background when bikes available, grey when empty.
 */
fun createStationMarkerBitmap(context: Context, availableBikes: Int): BitmapDescriptor {
    val d = context.resources.displayMetrics.density

    val pillHeight = 38 * d
    val cornerRadius = pillHeight / 2f
    val iconCircleRadius = pillHeight / 2f - 3 * d
    val emojiSize = 15 * d
    val numberSize = 17 * d
    val numberPadding = 12 * d

    val numberText = availableBikes.toString()

    // Measure number text width to size the pill dynamically
    val measurePaint = Paint().apply {
        textSize = numberSize
        typeface = Typeface.DEFAULT_BOLD
    }
    val numberWidth = measurePaint.measureText(numberText)

    // Left section (square) holds the icon circle; right section holds the number
    val leftSection = pillHeight
    val rightSection = numberWidth + numberPadding * 2
    val totalWidth = leftSection + rightSection

    val bitmapW = totalWidth.toInt()
    val bitmapH = pillHeight.toInt()

    val bitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor      = if (availableBikes > 0) 0xFF0057FF.toInt() else 0xFF9E9E9E.toInt()
    val darkBgColor  = if (availableBikes > 0) 0xFF003ECC.toInt() else 0xFF707070.toInt()

    // Pill background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    canvas.drawRoundRect(RectF(0f, 0f, totalWidth, pillHeight), cornerRadius, cornerRadius, bgPaint)

    // Darker circle on the left for the bike icon
    val iconCx = leftSection / 2f
    val iconCy = pillHeight / 2f
    val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = darkBgColor }
    canvas.drawCircle(iconCx, iconCy, iconCircleRadius, darkPaint)

    // White bike vector icon inside the left circle
    val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_bike)!!
    DrawableCompat.setTint(iconDrawable, Color.WHITE)
    val iconSize = (emojiSize * 1.3f).toInt()
    val iconLeft = (iconCx - iconSize / 2).toInt()
    val iconTop = (iconCy - iconSize / 2).toInt()
    iconDrawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
    iconDrawable.draw(canvas)

    // Bold white number on the right
    val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = numberSize
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val numberCx = leftSection + rightSection / 2f
    canvas.drawText(numberText, numberCx, iconCy - (numberPaint.descent() + numberPaint.ascent()) / 2, numberPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
