package com.example.animatedspantextviewdemo

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.util.AttributeSet
import android.view.ViewPropertyAnimator
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Learned from http://chiuki.github.io/advanced-android-textview/#/41.
 *
 * An extension of TextView that allows colorfully animating a span of text.
 * It will cancel once the view is detached from the window.
 * It will stop when the activity is backgrounded and restart when foregrounded.
 */
class AnimatedSpanTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs), DefaultLifecycleObserver {

    private var colors = DEFAULT_COLORS
    private var durationMs = DEFAULT_DURATION_MS
    private var delayMs = DEFAULT_DELAY_MS

    private var animator: ViewPropertyAnimator? = null
    private var queuedOnAttachToWindow: (() -> Unit)? = null

    private var span: AnimatedColorSpan? = null
    private var spannableString: SpannableString? = null

    /**
     * @param colors        The sRGB colors to be distributed along the gradient line.
     * @param positions     May be null. The relative positions [0..1] of each corresponding
     *                      color in the colors array. If this is null, the colors are distributed
     *                      evenly along the gradient line.
     * @param gradientWidth Control the width of the linear gradient. Either in pixels, or perhaps more intuitively, as a
     *                      multiple of text size times colors size. Note the shader is set to MIRROR.
     * @param durationMs    The duration of the animation.
     * @param delayMs       Optionally, add a delay before the start of the animation and before every loop.
     * @param spanStart     Is inclusive. The index of the character in the text to start the animation at.
     * @param spanEnd       Is exclusive. The index of the character in the text to end the animation at.
     */
    fun animateSpan(
        colors: IntArray = this.colors,
        positions: FloatArray? = null,
        gradientWidth: GradientWidth = GradientWidth.TextSizeTimesColorsSizeMultiple(),
        durationMs: Long = this.durationMs,
        delayMs: Long = this.delayMs,
        spanStart: Int = 0,
        spanEnd: Int = text.length
    ) {
        this.colors = colors
        this.durationMs = durationMs
        this.delayMs = delayMs

        val span = AnimatedColorSpan(colors, positions, gradientWidth)
        val spannableString = SpannableString(text).apply {
            setSpan(span, spanStart, spanEnd, 0)
        }

        // Queuing is done so animateSpan can be called in advance but the animation won't run until the view is attached to window.
        if (isAttachedToWindow) {
            startLoopingRunnable(span, spannableString)
        } else {
            queuedOnAttachToWindow = { startLoopingRunnable(span, spannableString) }
        }

        (context as? LifecycleOwner)?.lifecycle?.addObserver(this)

        // Important to set afterwards so that the animation is not double triggered by onStart.
        // We're expecting the activity to already be started when animateSpan is called, immediately triggering onStart.
        this.span = span
        this.spannableString = spannableString
    }

    fun cancel() {
        animator?.cancel()
        animator = null
        queuedOnAttachToWindow = null
        span = null
        spannableString = null
        (context as? LifecycleOwner)?.lifecycle?.removeObserver(this)
    }

    override fun onAttachedToWindow() {
        queuedOnAttachToWindow?.let {
            it.invoke()
            queuedOnAttachToWindow = null
        }
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        cancel()
        super.onDetachedFromWindow()
    }

    override fun onStart(owner: LifecycleOwner) {
        startLoopingRunnable(span ?: return, spannableString ?: return)
    }

    override fun onStop(owner: LifecycleOwner) {
        animator?.cancel()
        animator = null
        queuedOnAttachToWindow = null
    }

    private fun startLoopingRunnable(span: AnimatedColorSpan, spannableString: SpannableString) {
        object : Runnable {
            override fun run() {
                // This stops the loop when the view is detached and prevents it from running before the view is attached.
                if (!isAttachedToWindow) return

                text = spannableString
                animator = animate().setDuration(durationMs).setInterpolator(LinearInterpolator()).setUpdateListener {
                    span.translateXPercentage = it.animatedFraction
                    text = spannableString
                }.withEndAction(this).setStartDelay(delayMs).apply {
                    start()
                }
            }
        }.run()
    }

    private class AnimatedColorSpan(
        private val colors: IntArray,
        private val positions: FloatArray?,
        private val gradientWidth: GradientWidth
    ) : CharacterStyle(), UpdateAppearance {

        private var shader: Shader? = null
        private val matrix = Matrix()
        var translateXPercentage = 0f

        override fun updateDrawState(paint: TextPaint) {
            paint.style = Paint.Style.FILL

            val width = when (gradientWidth) {
                is GradientWidth.TextSizeTimesColorsSizeMultiple -> paint.textSize * colors.size * gradientWidth.multiple
                is GradientWidth.Raw -> gradientWidth.width
            }

            val shader = shader ?: LinearGradient(
                0f, 0f, width, 0f,
                colors, positions, Shader.TileMode.MIRROR
            ).also { shader = it }

            matrix.reset()

            // Shader.TileMode.MIRROR doubles the width of the repeating length, so * 2 is necessary
            // to prevent a visual jump.
            matrix.postTranslate(width * 2 * translateXPercentage, 0f)

            shader.setLocalMatrix(matrix)
            paint.shader = shader
        }
    }

    sealed interface GradientWidth {
        /** @param multiple The width of the gradient will be calculated: paint.textSize * colors.size * multiple. */
        data class TextSizeTimesColorsSizeMultiple(val multiple: Float = 1f) : GradientWidth

        /** @param width The width of the gradient in pixels. */
        data class Raw(val width: Float) : GradientWidth
    }

    companion object {
        val DEFAULT_COLORS = intArrayOf(Color.RED, Color.GREEN, Color.BLUE)
        const val DEFAULT_DURATION_MS = 10_000L
        const val DEFAULT_DELAY_MS = 0L
    }
}
