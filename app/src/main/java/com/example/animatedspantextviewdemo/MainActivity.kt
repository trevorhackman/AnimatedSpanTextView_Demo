package com.example.animatedspantextviewdemo

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        animateSpans()
        //removeAllIn10Seconds()
    }

    private fun animateSpans() {
        findViewById<AnimatedSpanTextView>(R.id.text1).apply {
            animateSpan()
        }

        findViewById<AnimatedSpanTextView>(R.id.text2).apply {
            animateSpan(
                durationMs = 2000L
            )
        }
        findViewById<AnimatedSpanTextView>(R.id.text3).apply {
            animateSpan(
                colors = intArrayOf(Color.MAGENTA, Color.RED),
            )
        }
        findViewById<AnimatedSpanTextView>(R.id.text4).apply {
            animateSpan(
                durationMs = 2000L,
                gradientWidth = AnimatedSpanTextView.GradientWidth.TextSizeTimesColorsSizeMultiple(0.2f),
                colors = intArrayOf(
                    TColor.Red500,
                    TColor.Orange500,
                    TColor.Yellow500,
                    TColor.Green500,
                    TColor.Blue500,
                    TColor.Indigo500,
                    TColor.Purple500
                ),
            )
        }
    }

    private fun removeAllIn10Seconds() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(10_000)

            findViewById<ViewGroup>(R.id.main).apply {
                removeAllViews()
            }
        }
    }
}
