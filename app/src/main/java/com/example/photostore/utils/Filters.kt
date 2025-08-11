package com.example.photostore.utils

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

fun applyFilters(
    originalBitmap: Bitmap,
    brightness: Float, // -255 to 255
    saturation: Float, // 1 = original, 0 = grayscale
    contrast: Float,   // 1 = original, <1 = decrease, >1 = increase
    hue: Float         // -180 to 180 degrees
): Bitmap {
    val config = originalBitmap.config ?: Config.ARGB_8888
    val editedBitmap = createBitmap(originalBitmap.width, originalBitmap.height, config)

    val canvas = Canvas(editedBitmap)
    val paint = Paint()

    val colorMatrix = ColorMatrix()

    // 1. Saturation
    colorMatrix.setSaturation(saturation)

    // 2. Contrast & Brightness
    val contrastMatrix = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, brightness,
        0f, contrast, 0f, 0f, brightness,
        0f, 0f, contrast, 0f, brightness,
        0f, 0f, 0f, 1f, 0f
    ))
    colorMatrix.postConcat(contrastMatrix)

    // 3. Hue Adjustment
    if (hue != 0f) {
        val hueMatrix = ColorMatrix()
        hueMatrix.setRotate(0, hue) // Red
        hueMatrix.setRotate(1, hue) // Green
        hueMatrix.setRotate(2, hue) // Blue
        colorMatrix.postConcat(hueMatrix)
    }

    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
    return editedBitmap
}
