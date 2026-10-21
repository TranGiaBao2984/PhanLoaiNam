/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.classification.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import androidx.core.graphics.createBitmap

/**
 * Bộ chuyển đổi YUV sang RGB (định dạng ARGB_8888) viết bằng Kotlin/Java.
 * An toàn 100% và không dùng RenderScript.
 */
class YuvToRgbConverter(ctx: Context) {

    private var yuvBytes: ByteArray? = null
    private var argbBytes: IntArray? = null
    private var lastWidth = 0
    private var lastHeight = 0

    @SuppressLint("UnsafeOptInUsageError")
    @Synchronized
    fun toBitmap(imageProxy: ImageProxy): Bitmap {
        if (imageProxy.format != ImageFormat.YUV_420_888) {
            throw IllegalArgumentException("Invalid image format: ${imageProxy.format}. Must be YUV_420_888.")
        }

        val width = imageProxy.width
        val height = imageProxy.height

        // Đảm bảo các buffer có kích thước chính xác
        if (width != lastWidth || height != lastHeight) {
            yuvBytes = ByteArray(width * height * 3 / 2) // Kích thước của YUV_420_888
            argbBytes = IntArray(width * height) // Kích thước của ARGB_8888
            lastWidth = width
            lastHeight = height
        }

        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]

        val yBuffer: ByteBuffer = yPlane.buffer
        val uBuffer: ByteBuffer = uPlane.buffer
        val vBuffer: ByteBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride

        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        val yuvBytes = this.yuvBytes!!
        val argbBytes = this.argbBytes!!

        // 1. Sao chép dữ liệu YUV từ ImageProxy (có thể bị phân mảnh) vào một mảng byte liền mạch
        var yOffset = 0
        // Y plane
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            if (yPixelStride == 1) {
                yBuffer.get(yuvBytes, yOffset, width)
                yOffset += width
            } else {
                for (col in 0 until width) {
                    yuvBytes[yOffset++] = yBuffer.get(row * yRowStride + col * yPixelStride)
                }
            }
        }

        var uvOffset = width * height
        // U (Cb) và V (Cr) planes
        for (row in 0 until height / 2) {
            val uRowStart = row * uRowStride
            val vRowStart = row * vRowStride

            for (col in 0 until width / 2) {
                val uIndex = uRowStart + col * uPixelStride
                val vIndex = vRowStart + col * vPixelStride

                yuvBytes[uvOffset++] = uBuffer.get(uIndex)
                yuvBytes[uvOffset++] = vBuffer.get(vIndex)
            }
        }

        // 2. Chuyển đổi mảng YUV liền mạch sang ARGB
        convertYUV420SPToARGB8888(yuvBytes, argbBytes, width, height)

        // 3. Tạo Bitmap từ mảng ARGB
        val bitmap = createBitmap(width, height)
        bitmap.setPixels(argbBytes, 0, width, 0, 0, width, height)
        return bitmap
    }

    // Thuật toán chuyển đổi YUV 420 (dạng SP/NV21) sang ARGB
    private fun convertYUV420SPToARGB8888(yuvData: ByteArray, argbData: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var j = 0
        var yp = 0
        while (j < height) {
            var uvp = frameSize + (j shr 1) * width
            var u = 0
            var v = 0
            var i = 0
            while (i < width) {
                val y = 0xff and yuvData[yp].toInt()
                if (i and 1 == 0) {
                    v = 0xff and yuvData[uvp++].toInt()
                    u = 0xff and yuvData[uvp++].toInt()
                }

                val y1192 = 1192 * (y - 16)
                var r = y1192 + 1634 * (v - 128)
                var g = y1192 - 833 * (v - 128) - 400 * (u - 128)
                var b = y1192 + 2066 * (u - 128)

                r = if (r > 262143) 262143 else (if (r < 0) 0 else r)
                g = if (g > 262143) 262143 else (if (g < 0) 0 else g)
                b = if (b > 262143) 262143 else (if (b < 0) 0 else b)

                argbData[yp] = -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)

                i++
                yp++
            }
            j++
        }
    }
}