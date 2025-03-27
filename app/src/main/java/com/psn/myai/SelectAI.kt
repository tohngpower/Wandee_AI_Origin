package com.psn.myai

import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins

class SelectAI : ComponentActivity() {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var ai1: ImageView
    private lateinit var ai2: ImageView
    private lateinit var upImage: ImageView
    private lateinit var downImage: ImageView
    private lateinit var scrollView: ScrollView
    private val systemDatabase = SystemDatabase(this)
    private var mediaPlayer: MediaPlayer? = null
    private var scrollY = 0
    private var scrollViewHeight = 0
    private var scrollViewChildHeight = 0
    private var complete = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.select_ai)

        contentContainer = findViewById(R.id.select_ai)
        mediaPlayer = MediaPlayer.create(this, R.raw.x)
        ai1 = findViewById(R.id.imageView3)
        ai2 = findViewById(R.id.imageView4)
        upImage = findViewById(R.id.upIMG)
        downImage = findViewById(R.id.downIMG)
        scrollView = findViewById(R.id.SV5)

        ViewCompat.setOnApplyWindowInsetsListener(contentContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply insets as padding to the view:
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.updateMargins(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if(complete) {
                handler.removeCallbacks(svRunnable)
            }
            checkScrollView()
        }

        ai1.setOnClickListener {
            systemDatabase.replaceAnswer("AI","ai01")
            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                // Start playing the sound
                mediaPlayer?.start()
            }
            finish()
        }

        ai2.setOnClickListener {
            systemDatabase.replaceAnswer("AI","ai02")
            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                // Start playing the sound
                mediaPlayer?.start()
            }
            finish()
        }

        upImage.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_UP)
        }

        downImage.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
        handler.post(svRunnable)
        alphaAnimation(upImage)
        alphaAnimation(downImage)
    }

    private val svRunnable = object : Runnable {
        override fun run() {
            if(!complete) {
                checkScrollView()
                if(scrollViewHeight > 0 && scrollViewChildHeight > 0) {
                    complete = true
                }
                handler.post(this)
            } else {
                complete = false
                scrollView.fullScroll(View.FOCUS_UP)
                handler.removeCallbacks(this)
            }
        }
    }

    override fun onDestroy() {
        // Release the MediaPlayer when your activity is destroyed
        mediaPlayer?.release()
        handler.removeCallbacks(svRunnable)
        systemDatabase.close()
        super.onDestroy()
    }

    private fun checkScrollView() {
        scrollY = scrollView.scrollY
        scrollViewHeight = scrollView.height
        scrollViewChildHeight = scrollView.getChildAt(0).height
        val isAtTop = scrollY == 0
        val isAtBottom = scrollY + scrollViewHeight >= scrollViewChildHeight

        if(scrollViewChildHeight <= scrollViewHeight) {
            upImage.isVisible = false
            downImage.isVisible = false
        } else if(isAtTop) {
            upImage.isVisible = false
            downImage.isVisible = true
        }else if(isAtBottom) {
            upImage.isVisible = true
            downImage.isVisible = false
        } else {
            upImage.isVisible = true
            downImage.isVisible = true
        }
    }

    private fun alphaAnimation(imageView: ImageView) {
        val imageDrawable = imageView.drawable // Get the current drawable

        val imageAnimator = ObjectAnimator.ofInt(
            imageDrawable,
            "alpha",
            50, 255 // Adjust alpha values as needed
        ).apply {
            duration = 500 // Set the duration of the animation
            repeatMode = ObjectAnimator.REVERSE // Automatically reverse the animation
            repeatCount = ObjectAnimator.INFINITE // Repeat the animation indefinitely
        }
        imageAnimator.start()
    }
}