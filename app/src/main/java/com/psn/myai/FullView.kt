package com.psn.myai

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat

class FullView : ComponentActivity() {
    private lateinit var frameLayout: FrameLayout
    private lateinit var closeButton: ImageButton
    private lateinit var imgR: ImageView
    private val systemDatabase = SystemDatabase(this)
    private val imageHandler = Handler(Looper.getMainLooper())
    private var imageIndex = 0
    private var imageIndex2 = 0
    private var imageIndex3 = 0
    private val animationDelayRange = 1500L..5000L
    private var alreadySpeaking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_full_view)

        closeButton = findViewById(R.id.imageClose5)
        frameLayout = findViewById(R.id.imageFrame4)
        imgR = findViewById(R.id.imageView19)

        closeButton.setOnClickListener {
            finish()
        }

        imgR.setOnClickListener {
            bgBlink()
        }

        MyApp.setAIResource(systemDatabase)
        frameLayout.setBackgroundResource(MyApp.imageResId)
        startBreathingAnimation()
        startRandomImageAnimation()
        bgBlink()
        if(MyApp.isSpeaking) startImageAnimation()
    }

    override fun onDestroy() {
        imageHandler.removeCallbacks(imageRunnable)
        imageHandler.removeCallbacks(imageRunnable2)
        imageHandler.removeCallbacks(imageRunnable3)
        systemDatabase.close()
        super.onDestroy()
    }

    private fun startImageAnimation() {
        alreadySpeaking = true
        imageIndex = 0
        imgR.setImageResource(MyApp.imageResources[imageIndex])
        imageHandler.post(imageRunnable)
    }

    private fun stopImageAnimation() {
        alreadySpeaking = false
        imageHandler.removeCallbacks(imageRunnable)
        imgR.setImageResource(MyApp.imageResources[0])
    }

    private fun startRandomImageAnimation() {
        imageIndex2 = 0
        val randomDelay = animationDelayRange.random()
        val fg = ContextCompat.getDrawable(this,MyApp.imageResources2[0])
        imgR.foreground = fg
        imgR.setImageResource(MyApp.imageResources[0])
        imageHandler.postDelayed(imageRunnable2, randomDelay)
    }

    private fun startBreathingAnimation() {
        imageIndex3 = 0
        imageHandler.post(imageRunnable3)
    }

    private val imageRunnable = object : Runnable {
        override fun run() {
            imageIndex = (imageIndex + 1) % MyApp.imageResources.size
            imgR.setImageResource(MyApp.imageResources[imageIndex])
            imageHandler.postDelayed(this, 170)
        }
    }

    private val imageRunnable2 = object : Runnable {
        override fun run() {
            if(imageIndex2<MyApp.imageResources2.size) {
                val fg = ContextCompat.getDrawable(this@FullView,MyApp.imageResources2[imageIndex2])
                imgR.foreground = fg
                imageIndex2++
                imageHandler.postDelayed(this, 30)
            } else {
                imageHandler.removeCallbacks(this)
                val randomDelay = animationDelayRange.random()
                val fg = ContextCompat.getDrawable(this@FullView,MyApp.imageResources2[0])
                imgR.foreground = fg
                imageIndex2=0
                imageHandler.postDelayed(this, randomDelay)
            }
        }
    }

    private val imageRunnable3 = object : Runnable {
        override fun run() {
            if(MyApp.isSpeaking && !alreadySpeaking) {
                startImageAnimation()
                imageIndex3 = (imageIndex3 + 1) % MyApp.imageBreathing.size
                imgR.setBackgroundResource(MyApp.imageBreathing[imageIndex3])
                imageHandler.postDelayed(this, 150)
            } else if(!MyApp.isSpeaking && alreadySpeaking) {
                stopImageAnimation()
                imageIndex3 = (imageIndex3 + 1) % MyApp.imageBreathing.size
                imgR.setBackgroundResource(MyApp.imageBreathing[imageIndex3])
                imageHandler.postDelayed(this, 150)
            } else {
                imageIndex3 = (imageIndex3 + 1) % MyApp.imageBreathing.size
                imgR.setBackgroundResource(MyApp.imageBreathing[imageIndex3])
                imageHandler.postDelayed(this, 150)
            }
        }
    }

    private fun bgBlink() {
        val backgroundAnimator = frameLayout.background?.let { backgroundDrawable ->
            ObjectAnimator.ofInt(
                backgroundDrawable,
                "alpha",
                100, 255 // Adjust alpha values as needed
            ).apply {
                duration = 100 // Set the duration of the animation
            }
        }
        backgroundAnimator?.start()
    }
}