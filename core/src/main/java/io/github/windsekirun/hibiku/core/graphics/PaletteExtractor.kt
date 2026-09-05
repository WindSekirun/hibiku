package io.github.windsekirun.hibiku.core.graphics

import android.graphics.Bitmap
import androidx.palette.graphics.Palette

object PaletteExtractor {

    fun extractAccentColor(bitmap: Bitmap?, defaultColor: Int): Int {
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return defaultColor
        }

        return runCatching {
            val palette = Palette.from(bitmap).generate()
            palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: defaultColor
        }.getOrDefault(defaultColor)
    }
}
