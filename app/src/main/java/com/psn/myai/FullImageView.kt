package com.psn.myai

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins

class FullImageView : ComponentActivity() {
    private lateinit var contentContainer: LinearLayout
    private lateinit var imageDisplay: ImageView
    private lateinit var textView: TextView
    private lateinit var closeButton: ImageButton
    private lateinit var frameLayout: FrameLayout
    private val systemDatabase = SystemDatabase(this)
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var scaleFactor = 1.0f // Track the scale factor
    private var previousX = 0.0f
    private var previousY = 0.0f
    private var imageWidth = 0.0f
    private var imageHeight = 0.0f
    private var landscape = false
    private var frameWidth = 0
    private var frameHeight = 0
    private var loadSuccess = false
    private var fullPrompt = true
    private val maxPromptLength = 100

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.full_image_view)

        contentContainer = findViewById(R.id.full_image_view)
        imageDisplay = findViewById(R.id.imageView9)
        textView = findViewById(R.id.textView19)
        closeButton = findViewById(R.id.imageButton5)
        frameLayout = findViewById(R.id.Frame)

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

        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        gestureDetector = GestureDetector(this,GestureListener())

        if(MyApp.imagePrompt.isNotEmpty()) {
            textView.text = MyApp.imagePrompt
        }
        val imageD = MyApp.imageData
        if (imageD != null) {
            val bitmap = BitmapFactory.decodeFile(imageD)
            imageDisplay.setImageBitmap(bitmap)
        } else {
            finish()
        }

        closeButton.setOnClickListener {
            MyApp.imageData = null
            finish()
        }

        textView.setOnClickListener {
            fullPrompt = !fullPrompt
            textView.text = if(fullPrompt) {
                MyApp.imagePrompt
            } else {
                if(MyApp.imagePrompt.length < maxPromptLength) {
                    MyApp.imagePrompt
                } else {
                    "${MyApp.imagePrompt.substring(0..maxPromptLength)} ..."
                }
            }
        }

        imageDisplay.setOnTouchListener { _, event ->
            if(!loadSuccess) {
                loadParameter()
            }
            if(loadSuccess) {
                scaleGestureDetector?.onTouchEvent(event)
                gestureDetector?.onTouchEvent(event)

                val dx = scaleFactor*imageWidth - frameWidth
                val dy = scaleFactor*imageHeight - frameHeight
                val y = scaleFactor*imageHeight - imageHeight
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        previousX = event.x
                        previousY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!scaleGestureDetector!!.isInProgress) {
                            val deltaX = event.x - previousX
                            val deltaY = event.y - previousY
                            // Calculate the possible new position within bounds
                            val newX = imageDisplay.translationX + deltaX * scaleFactor
                            val newY = imageDisplay.translationY + deltaY * scaleFactor
                            if(dx > 0) {
                                imageDisplay.translationX = newX.coerceIn(-dx/2, dx/2)
                            }
                            if(dy > 0) {
                                imageDisplay.translationY = newY.coerceIn(y/2-dy, y/2)
                            }
                        } else {
                            if(dx <= 0) {
                                imageDisplay.translationX = 0.0f
                            }
                            if(dy <= 0) {
                                imageDisplay.translationY = y/2
                            }
                        }
                    }
                }
            }
            true
        }
        loadParameter()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java", ReplaceWith("moveTaskToBack(true)"))
    override fun onBackPressed() {
        MyApp.imageData = null
        finish()
    }

    override fun onDestroy() {
        systemDatabase.close()
        super.onDestroy()
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            // Limit the scale factor within certain bounds for smooth zooming
            scaleFactor = scaleFactor.coerceIn(1.0f, 6.0f)
            // Apply the scale factor to the image
            imageDisplay.scaleX = scaleFactor
            imageDisplay.scaleY = scaleFactor

            return true
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Handle double tap event, e.g., toggle between zoomed in/out
            scaleFactor = if(scaleFactor > 5.5) {
                5.0f
            } else if(scaleFactor > 4.5) {
                4.0f
            } else if(scaleFactor > 3.5) {
                3.0f
            } else if(scaleFactor > 2.5) {
                2.0f
            } else if(scaleFactor > 1.5) {
                1.0f
            } else {
                6.0f
            }
            imageDisplay.scaleX = scaleFactor
            imageDisplay.scaleY = scaleFactor
            imageDisplay.translationX = 0.0f
            imageDisplay.translationY = 0.0f

            return true
        }
    }

    private fun loadParameter() {
        frameWidth = frameLayout.width
        frameHeight = frameLayout.height
        imageWidth = imageDisplay.width.toFloat()
        imageHeight = imageDisplay.height.toFloat()
        loadSuccess = !(frameWidth==0||frameHeight==0||imageWidth==0.0f||imageHeight==0.0f)
        landscape = frameWidth>frameHeight
        if(MyApp.imageRatio == 1.0f) {
            if(landscape) {
                imageWidth = imageHeight
            }
        } else if(MyApp.imageRatio > 1.0f) {
            imageWidth = imageHeight / MyApp.imageRatio
        } else {
            if(landscape) {
                imageWidth = imageHeight / MyApp.imageRatio
            }
        }
    }
}