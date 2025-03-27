package com.psn.myai

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

class DrawableView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private var paint: Paint = Paint()
    private var path: Path = Path()
    private var bitmap: Bitmap? = null
    private var modifiedBitmap: Bitmap? = null
    private var tempCanvas: Canvas = Canvas()
    var alreadyDrew = false
    var error = ""
    var imageWidth = 0
    var imageHeight = 0

    init {
        paint.isAntiAlias = true
        paint.isDither = true
        paint.color = Color.TRANSPARENT
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        paint.strokeWidth = 10f
    }

    fun loadImage(imagePath: String?):Boolean {
        imagePath?.let {
            bitmap = resizeImage(imagePath)
            modifiedBitmap = createBitmap(bitmap!!.width, bitmap!!.height)
            tempCanvas = Canvas(modifiedBitmap!!)
            tempCanvas.drawBitmap(bitmap!!, 0f, 0f, null)
            setImageBitmap(modifiedBitmap)
            alreadyDrew = false

            return true
        }
        error = "Failed to load an image."

        return false
    }

    fun changeStrokeWidth(width: Float) {
        paint.strokeWidth = width
        if(alreadyDrew) {
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(modifiedBitmap != null) {
            tempCanvas.drawBitmap(modifiedBitmap!!, 0f, 0f, null)
            tempCanvas.drawPath(path, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                alreadyDrew = true
            }
            else -> return false
        }

        invalidate()
        return true
    }

    fun clearCanvas() {
        alreadyDrew = false
        path.reset()
        invalidate()
    }

    fun getModifiedBitmap(): Bitmap? {
        return modifiedBitmap
    }

    fun resizeImage(imagePath: String?): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true // Get image dimensions without loading the whole image
        BitmapFactory.decodeFile(imagePath, options)

        if((options.outWidth == 1024 && options.outHeight == 1024) || (options.outWidth == 512 && options.outHeight == 512) || (options.outWidth == 256 && options.outHeight == 256)) {
            options.inJustDecodeBounds = false
            imageWidth = options.outWidth
            imageHeight = options.outHeight
            return BitmapFactory.decodeFile(imagePath, options)
        } else if(options.outWidth > 1024 || options.outHeight > 1024) {
            val scaleFactor = calculateScaleFactor(options.outWidth, options.outHeight, 1024, 1024)
            options.inJustDecodeBounds = false // Now load the image
            options.inSampleSize = scaleFactor // Set the sample size for downscaling
            imageWidth = 1024
            imageHeight = 1024

            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            return bitmap.scale(1024, 1024) // Resize to the target dimensions
        } else if(options.outWidth > 512 || options.outHeight > 512) {
            val scaleFactor = calculateScaleFactor(options.outWidth, options.outHeight, 512, 512)
            options.inJustDecodeBounds = false // Now load the image
            options.inSampleSize = scaleFactor // Set the sample size for downscaling
            imageWidth = 512
            imageHeight = 512

            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            return bitmap.scale(512, 512) // Resize to the target dimensions
        } else if(options.outWidth > 256 || options.outHeight > 256) {
            val scaleFactor = calculateScaleFactor(options.outWidth, options.outHeight, 256, 256)
            options.inJustDecodeBounds = false // Now load the image
            options.inSampleSize = scaleFactor // Set the sample size for downscaling
            imageWidth = 256
            imageHeight = 256

            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            return bitmap.scale(256, 256) // Resize to the target dimensions
        } else {
            error = "Image size is too small."
            options.inJustDecodeBounds = false
            imageWidth = options.outWidth
            imageHeight = options.outHeight

            return BitmapFactory.decodeFile(imagePath, options)
        }
    }

    private fun calculateScaleFactor(imageWidth: Int, imageHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var scaleFactor = 1
        while (imageWidth / scaleFactor > targetWidth || imageHeight / scaleFactor > targetHeight) {
            scaleFactor *= 2
        }
        return scaleFactor
    }
}