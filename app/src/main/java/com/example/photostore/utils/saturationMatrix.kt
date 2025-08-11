package com.example.photostore.utils

 fun createSaturationMatrix(saturation: Float): FloatArray {
    val invSat = 1f - saturation
    val R = 0.213f * invSat
    val G = 0.715f * invSat
    val B = 0.072f * invSat

    return floatArrayOf(
        R + saturation, G, B, 0f, 0f,
        R, G + saturation, B, 0f, 0f,
        R, G, B + saturation, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

 fun createHueRotationMatrix(hue: Float): FloatArray {
    val hueRadians = hue * kotlin.math.PI.toFloat() / 180f
    val cosHue = kotlin.math.cos(hueRadians.toDouble()).toFloat()
    val sinHue = kotlin.math.sin(hueRadians.toDouble()).toFloat()

    return floatArrayOf(
        0.299f + 0.701f * cosHue + 0.168f * sinHue,
        0.587f - 0.587f * cosHue + 0.330f * sinHue,
        0.114f - 0.114f * cosHue - 0.497f * sinHue,
        0f, 0f,

        0.299f - 0.299f * cosHue - 0.328f * sinHue,
        0.587f + 0.413f * cosHue + 0.035f * sinHue,
        0.114f - 0.114f * cosHue + 0.292f * sinHue,
        0f, 0f,

        0.299f - 0.300f * cosHue + 1.25f * sinHue,
        0.587f - 0.588f * cosHue - 1.05f * sinHue,
        0.114f + 0.886f * cosHue - 0.203f * sinHue,
        0f, 0f,

        0f, 0f, 0f, 1f, 0f
    )
}

 fun multiplyMatrices(result: FloatArray, matrix: FloatArray) {
    val temp = result.copyOf()

    for (i in 0..3) {
        for (j in 0..4) {
            var value = 0f
            for (k in 0..3) {
                value += temp[i * 5 + k] * matrix[k * 5 + j]
            }
            if (j == 4) {
                value += temp[i * 5 + 4]
            }
            result[i * 5 + j] = value
        }
    }
}