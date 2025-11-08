/*
 * StatusBarLyric
 * Copyright (C) 2021-2022 fkj@fkj233.cn
 * https://github.com/Block-Network/StatusBarLyric
 *
 * This software is free opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as
 * published by Block-Network contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/Block-Network/StatusBarLyric/blob/main/LICENSE>.
 */

package statusbar.lyric.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Choreographer
import android.widget.TextView
import statusbar.lyric.config.XposedOwnSP.config

class LyricTextView(context: Context) : TextView(context), Choreographer.FrameCallback {
    private var isScrolling = false
    private var textString: String = ""
    private var textLength = 0f
    private var viewWidth = 0f
    private var scrollSpeed = 4f
    private var currentX = 0f
    private var textY = 0f
    private var lastFrameNanos = 0L
    private val startScrollRunnable = Runnable { Choreographer.getInstance().postFrameCallback(this) }

    init {
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.isAntiAlias = true
        setLayerType(LAYER_TYPE_HARDWARE, paint)
    }

    override fun onDetachedFromWindow() {
        stopScroll()
        super.onDetachedFromWindow()
    }

    override fun setText(text: CharSequence, type: BufferType) {
        stopScroll()
        currentX = 0f
        textString = text.toString()
        textLength = getTextLength(textString)
        super.setText(text, type)
        recomputeTextMetrics()
        startScroll()
    }

    override fun setTextColor(color: Int) {
        paint.color = color
        postInvalidate()
    }

    fun setStrokeWidth(width: Float) {
        paint.strokeWidth = width
        postInvalidate()
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        paint.textSize = this.textSize
        recomputeTextMetrics()
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        paint.textSize = this.textSize
        recomputeTextMetrics()
    }

    override fun setTypeface(typeface: Typeface?) {
        super.setTypeface(typeface)
        paint.typeface = this.typeface
        recomputeTextMetrics()
    }

    override fun setLetterSpacing(letterSpacing: Float) {
        super.setLetterSpacing(letterSpacing)
        paint.letterSpacing = letterSpacing
        recomputeTextMetrics()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        textY = (h - (paint.descent() + paint.ascent())) / 2
        resumeScroll()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawText(textString, currentX, textY, paint)
    }

    private fun updateScrollPosition(step: Float) {
        val realTextLength = textLength
        val realLyricWidth = viewWidth
        val targetX = realLyricWidth - realTextLength
        if (currentX > targetX) {
            // 向左滚动
            val nextX = currentX - step
            if (nextX <= targetX) {
                currentX = targetX
                postInvalidate()
                stopScroll()
            } else {
                currentX = nextX
                postInvalidate()
            }
        } else if (currentX < targetX) {
            // 向右回退
            val nextX = currentX + step
            if (nextX >= targetX) {
                currentX = targetX
                postInvalidate()
                stopScroll()
            } else {
                currentX = nextX
                postInvalidate()
            }
        } else {
            // 已到位
            stopScroll()
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (isScrolling) {
            val last = lastFrameNanos
            lastFrameNanos = frameTimeNanos
            if (last != 0L) {
                val deltaSeconds = (frameTimeNanos - last) / 1_000_000_000f
                val step = scrollSpeed * (deltaSeconds * 60f)
                updateScrollPosition(step)
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun startScroll() {
        isScrolling = true
        lastFrameNanos = 0L
        postDelayed(
            startScrollRunnable,
            config.animationDuration + if (config.dynamicLyricSpeed) 200L else 500L
        )
    }

    private fun stopScroll() {
        isScrolling = false
        removeCallbacks(startScrollRunnable)
        Choreographer.getInstance().removeFrameCallback(this)
        lastFrameNanos = 0L
    }

    private fun getTextLength(text: CharSequence?): Float {
        return paint.measureText(text?.toString() ?: "")
    }

    fun setScrollSpeed(speed: Float) {
        this.scrollSpeed = speed
    }

    fun resumeScroll() {
        isScrolling = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun recomputeTextMetrics() {
        val current = this.text
        textString = current?.toString() ?: textString
        textLength = getTextLength(current)
        if (height > 0) {
            textY = (height - (paint.descent() + paint.ascent())) / 2f
        }
        postInvalidate()
    }
}