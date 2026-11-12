package com.example

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin

/**
 * Five colourful sound bars. Static when idle, dancing while speaking.
 */
class SoundBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colors = intArrayOf(
        0xFF7C6DF0.toInt(),
        0xFFA79BFF.toInt(),
        0xFF2DD4BF.toInt(),
        0xFFA79BFF.toInt(),
        0xFF7C6DF0.toInt()
    )
    private val idleLevels = floatArrayOf(0.25f, 0.65f, 1.0f, 0.55f, 0.2f)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var phase = 0f
    private var animating = false
    private var animator: ValueAnimator? = null

    fun setAnimating(enabled: Boolean) {
        if (enabled == animating) return
        animating = enabled
        animator?.cancel()
        animator = null
        if (enabled) {
            animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
                duration = 900L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    phase = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        animating = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val n = idleLevels.size
        val barW = w / (n * 2f - 1f)
        for (i in 0 until n) {
            val level = if (animating) {
                0.3f + 0.7f * abs(sin(phase + i * 0.9f))
            } else {
                idleLevels[i]
            }
            val barH = h * level
            val left = i * barW * 2f
            rect.set(left, (h - barH) / 2f, left + barW, (h + barH) / 2f)
            paint.color = colors[i]
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
    }
}
