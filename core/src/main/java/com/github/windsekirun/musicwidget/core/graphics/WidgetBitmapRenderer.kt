package com.github.windsekirun.musicwidget.core.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.github.windsekirun.musicwidget.domain.model.RingStyle

object WidgetBitmapRenderer {

    fun renderArtworkWithRing(
        artwork: Bitmap?,
        progress: Float,
        isPlaying: Boolean,
        ringStyle: RingStyle,
        ringColor: Int,
        trackColor: Int = 0x33FFFFFF,
        sizePx: Int = 200
    ): Bitmap {
        val size = maxOf(1, sizePx)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val cx = size / 2f
        val cy = size / 2f

        val ringStrokeWidth = maxOf(2f, size * 0.05f)
        val thumbRadius = ringStrokeWidth * 1.1f
        val ringPadding = ringStrokeWidth + thumbRadius
        val ringRadius = maxOf(4f, (size / 2f) - ringPadding)
        val ringBounds = RectF(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)

        val artworkGap = if (ringStyle == RingStyle.FLOATING_CLEAN) {
            ringStrokeWidth * 0.8f
        } else {
            ringStrokeWidth * 0.4f
        }
        val artworkRadius = maxOf(2f, ringRadius - (ringStrokeWidth / 2f) - artworkGap)

        // 1. Draw artwork or placeholder
        if (artwork != null && !artwork.isRecycled) {
            val saveCount = canvas.saveLayer(0f, 0f, size.toFloat(), size.toFloat(), null)

            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, artworkRadius, maskPaint)

            val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }

            val srcW = artwork.width
            val srcH = artwork.height
            val minDim = minOf(srcW, srcH)
            val srcLeft = (srcW - minDim) / 2
            val srcTop = (srcH - minDim) / 2
            val srcRect = Rect(srcLeft, srcTop, srcLeft + minDim, srcTop + minDim)
            val dstRect = RectF(
                cx - artworkRadius,
                cy - artworkRadius,
                cx + artworkRadius,
                cy + artworkRadius
            )
            canvas.drawBitmap(artwork, srcRect, dstRect, artworkPaint)

            canvas.restoreToCount(saveCount)
        } else {
            // Dark grey circular background with musical note
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF2A2A2A.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, artworkRadius, bgPaint)

            drawMusicalNote(canvas, cx, cy, artworkRadius * 0.5f)
        }

        // 2. Draw ring style
        val sweepAngle = RingMathHelper.calculateSweepAngle(progress)
        val startAngle = -90f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.STROKE
            strokeWidth = ringStrokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = ringStrokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        when (ringStyle) {
            RingStyle.SOLID_CLASSIC -> {
                canvas.drawCircle(cx, cy, ringRadius, trackPaint)
                if (sweepAngle > 0f) {
                    canvas.drawArc(ringBounds, startAngle, sweepAngle, false, ringPaint)
                }
            }

            RingStyle.FLOATING_CLEAN -> {
                canvas.drawCircle(cx, cy, ringRadius, trackPaint)

                // Thin inner circle line around artwork with small gap
                val innerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = trackColor
                    style = Paint.Style.STROKE
                    strokeWidth = maxOf(1f, ringStrokeWidth * 0.25f)
                }
                canvas.drawCircle(cx, cy, artworkRadius + (artworkGap / 2f), innerLinePaint)

                if (sweepAngle > 0f) {
                    canvas.drawArc(ringBounds, startAngle, sweepAngle, false, ringPaint)
                }
            }

            RingStyle.SEGMENTED_MINIMAL -> {
                val totalSegments = 36
                val ticks = RingMathHelper.calculateSegmentTicks(progress, totalSegments)
                val tickLength = ringStrokeWidth * 1.3f
                val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = maxOf(2f, ringStrokeWidth * 0.35f)
                    strokeCap = Paint.Cap.ROUND
                }

                for (i in 0 until totalSegments) {
                    val angleDeg = startAngle + i * (360f / totalSegments)
                    val (x1, y1) = RingMathHelper.calculateThumbPosition(
                        cx, cy, ringRadius - tickLength / 2f, angleDeg
                    )
                    val (x2, y2) = RingMathHelper.calculateThumbPosition(
                        cx, cy, ringRadius + tickLength / 2f, angleDeg
                    )
                    tickPaint.color = if (ticks[i]) ringColor else trackColor
                    canvas.drawLine(x1, y1, x2, y2, tickPaint)
                }
            }

            RingStyle.GLOW_THUMB -> {
                canvas.drawCircle(cx, cy, ringRadius, trackPaint)
                if (sweepAngle > 0f) {
                    canvas.drawArc(ringBounds, startAngle, sweepAngle, false, ringPaint)
                }

                if (progress > 0f) {
                    val thumbAngle = RingMathHelper.calculateThumbAngle(progress, startAngle)
                    val (tx, ty) = RingMathHelper.calculateThumbPosition(cx, cy, ringRadius, thumbAngle)

                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = ringColor
                        alpha = 0x55
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(tx, ty, thumbRadius * 1.6f, glowPaint)

                    val solidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = ringColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(tx, ty, thumbRadius, solidPaint)

                    val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(tx, ty, thumbRadius * 0.4f, centerDotPaint)
                }
            }

            RingStyle.SQUIGGLY_WAVE -> {
                canvas.drawCircle(cx, cy, ringRadius, trackPaint)

                if (sweepAngle > 0f) {
                    if (!isPlaying) {
                        canvas.drawArc(ringBounds, startAngle, sweepAngle, false, ringPaint)
                    } else {
                        val wavePath = Path()
                        val steps = maxOf(4, (sweepAngle * 2).toInt())
                        val angleStep = sweepAngle / steps
                        val amplitude = ringStrokeWidth * 0.35f
                        val waveCount = 12

                        for (i in 0..steps) {
                            val angleDeg = startAngle + i * angleStep
                            val angleRad = Math.toRadians(angleDeg.toDouble())
                            val radialOffset = RingMathHelper.calculateSquigglyRadialOffset(
                                angleRad = angleRad,
                                waveCount = waveCount,
                                amplitude = amplitude
                            )
                            val r = ringRadius + radialOffset
                            val (x, y) = RingMathHelper.calculateThumbPosition(cx, cy, r, angleDeg)
                            if (i == 0) {
                                wavePath.moveTo(x, y)
                            } else {
                                wavePath.lineTo(x, y)
                            }
                        }
                        canvas.drawPath(wavePath, ringPaint)
                    }
                }
            }
        }

        return output
    }

    fun renderBlurredBackground(
        artwork: Bitmap?,
        widthPx: Int,
        heightPx: Int,
        dimAlpha: Float = 0.35f,
        cornerRadiusPx: Float = 32f,
        strokeColor: Int = 0x33FFFFFF
    ): Bitmap {
        val w = maxOf(1, widthPx)
        val h = maxOf(1, heightPx)
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())

        if (artwork == null || artwork.isRecycled) {
            if (strokeColor != 0) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = strokeColor
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                val strokeBounds = RectF(1f, 1f, w - 1f, h - 1f)
                canvas.drawRoundRect(strokeBounds, cornerRadiusPx, cornerRadiusPx, strokePaint)
            }
            return output
        }

        // Downscale and center-crop for fast, smooth blur
        val scaleFactor = 4
        val scaledW = maxOf(16, w / scaleFactor)
        val scaledH = maxOf(16, h / scaleFactor)
        val scaledBitmap = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
        val scaledCanvas = Canvas(scaledBitmap)

        val srcW = artwork.width
        val srcH = artwork.height
        val targetAspect = scaledW.toFloat() / scaledH.toFloat()
        val srcAspect = srcW.toFloat() / srcH.toFloat()

        val srcRect = if (srcAspect > targetAspect) {
            val cropW = (srcH * targetAspect).toInt()
            val left = (srcW - cropW) / 2
            Rect(left, 0, left + cropW, srcH)
        } else {
            val cropH = (srcW / targetAspect).toInt()
            val top = (srcH - cropH) / 2
            Rect(0, top, srcW, top + cropH)
        }
        val dstRect = Rect(0, 0, scaledW, scaledH)
        val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        scaledCanvas.drawBitmap(artwork, srcRect, dstRect, filterPaint)

        // Apply fast blur
        val blurredBitmap = fastBlur(scaledBitmap, radius = 12)
        if (blurredBitmap !== scaledBitmap) {
            scaledBitmap.recycle()
        }

        // Clip to rounded rect and draw blurred image + dim layer
        val saveCount = canvas.saveLayer(bounds, null)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val roundPath = Path().apply {
            addRoundRect(bounds, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
        }
        canvas.drawPath(roundPath, maskPaint)

        val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(blurredBitmap, null, bounds, blurPaint)

        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((dimAlpha.coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawRect(bounds, dimPaint)

        canvas.restoreToCount(saveCount)
        blurredBitmap.recycle()

        // Stroke border if needed
        if (strokeColor != 0) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val strokeBounds = RectF(1f, 1f, w - 1f, h - 1f)
            canvas.drawRoundRect(strokeBounds, cornerRadiusPx, cornerRadiusPx, strokePaint)
        }

        return output
    }

    private fun drawMusicalNote(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xBBFFFFFF.toInt()
            style = Paint.Style.FILL
        }

        val leftHeadCx = cx - size * 0.30f
        val leftHeadCy = cy + size * 0.25f
        val rightHeadCx = cx + size * 0.25f
        val rightHeadCy = cy + size * 0.10f
        val headRadiusX = size * 0.22f
        val headRadiusY = size * 0.16f

        val ovalLeft = RectF(
            leftHeadCx - headRadiusX,
            leftHeadCy - headRadiusY,
            leftHeadCx + headRadiusX,
            leftHeadCy + headRadiusY
        )
        val ovalRight = RectF(
            rightHeadCx - headRadiusX,
            rightHeadCy - headRadiusY,
            rightHeadCx + headRadiusX,
            rightHeadCy + headRadiusY
        )

        canvas.save()
        canvas.rotate(-20f, leftHeadCx, leftHeadCy)
        canvas.drawOval(ovalLeft, notePaint)
        canvas.restore()

        canvas.save()
        canvas.rotate(-20f, rightHeadCx, rightHeadCy)
        canvas.drawOval(ovalRight, notePaint)
        canvas.restore()

        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xBBFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = size * 0.09f
            strokeCap = Paint.Cap.BUTT
        }

        val leftStemX = leftHeadCx + headRadiusX * 0.7f
        val rightStemX = rightHeadCx + headRadiusX * 0.7f
        val leftStemTopY = cy - size * 0.40f
        val rightStemTopY = cy - size * 0.55f

        canvas.drawLine(leftStemX, leftHeadCy, leftStemX, leftStemTopY, stemPaint)
        canvas.drawLine(rightStemX, rightHeadCy, rightStemX, rightStemTopY, stemPaint)

        val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xBBFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = size * 0.20f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(leftStemX, leftStemTopY, rightStemX, rightStemTopY, beamPaint)
    }

    private fun fastBlur(src: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceAtLeast(1)
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val temp = IntArray(w * h)

        // 3 passes of box blur (horizontal + vertical) simulates Gaussian blur
        for (pass in 0 until 3) {
            boxBlurHorizontal(pixels, temp, w, h, r)
            boxBlurVertical(temp, pixels, w, h, r)
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val windowSize = 2 * r + 1
        for (y in 0 until h) {
            val rowOffset = y * w
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0

            for (i in -r..r) {
                val px = src[rowOffset + i.coerceIn(0, w - 1)]
                sumA += (px ushr 24) and 0xFF
                sumR += (px ushr 16) and 0xFF
                sumG += (px ushr 8) and 0xFF
                sumB += px and 0xFF
            }

            for (x in 0 until w) {
                dst[rowOffset + x] = ((sumA / windowSize) shl 24) or
                        ((sumR / windowSize) shl 16) or
                        ((sumG / windowSize) shl 8) or
                        (sumB / windowSize)

                val addPx = src[rowOffset + (x + r + 1).coerceIn(0, w - 1)]
                val remPx = src[rowOffset + (x - r).coerceIn(0, w - 1)]

                sumA += ((addPx ushr 24) and 0xFF) - ((remPx ushr 24) and 0xFF)
                sumR += ((addPx ushr 16) and 0xFF) - ((remPx ushr 16) and 0xFF)
                sumG += ((addPx ushr 8) and 0xFF) - ((remPx ushr 8) and 0xFF)
                sumB += (addPx and 0xFF) - (remPx and 0xFF)
            }
        }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val windowSize = 2 * r + 1
        for (x in 0 until w) {
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0

            for (i in -r..r) {
                val px = src[i.coerceIn(0, h - 1) * w + x]
                sumA += (px ushr 24) and 0xFF
                sumR += (px ushr 16) and 0xFF
                sumG += (px ushr 8) and 0xFF
                sumB += px and 0xFF
            }

            for (y in 0 until h) {
                dst[y * w + x] = ((sumA / windowSize) shl 24) or
                        ((sumR / windowSize) shl 16) or
                        ((sumG / windowSize) shl 8) or
                        (sumB / windowSize)

                val addPx = src[(y + r + 1).coerceIn(0, h - 1) * w + x]
                val remPx = src[(y - r).coerceIn(0, h - 1) * w + x]

                sumA += ((addPx ushr 24) and 0xFF) - ((remPx ushr 24) and 0xFF)
                sumR += ((addPx ushr 16) and 0xFF) - ((remPx ushr 16) and 0xFF)
                sumG += ((addPx ushr 8) and 0xFF) - ((remPx ushr 8) and 0xFF)
                sumB += (addPx and 0xFF) - (remPx and 0xFF)
            }
        }
    }
}
