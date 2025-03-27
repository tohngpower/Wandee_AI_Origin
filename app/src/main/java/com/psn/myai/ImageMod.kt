package com.psn.myai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageMod : ComponentActivity() {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var drawableView: DrawableView
    private lateinit var edText : EditText
    private lateinit var statusText: TextView
    private lateinit var buttonSelectImage: ImageButton
    private lateinit var buttonSubmit: ImageButton
    private lateinit var buttonClear: ImageButton
    private lateinit var buttonSave: ImageButton
    private lateinit var frameLayout: FrameLayout
    private lateinit var seekBar: SeekBar
    private lateinit var image1: ImageView
    private lateinit var mask1: ImageView
    private lateinit var helpView: ImageView
    private lateinit var imgDBView: ImageView
    private val imageAPI = ImageAPI()
    private val systemDatabase = SystemDatabase(this)
    private val databaseImage = DatabaseImage(this)
    private val databaseHelper = DatabaseHelper(this)
    private var themeNumber = 0
    private var loadImage = false
    private var appName = ""
    private val statusHandler = Handler(Looper.getMainLooper())
    private var fw = 0
    private var fh = 0
    private var w = 0
    private var h = 0
    private var alreadyLock = false
    private var alreadyUnlock = false
    private var maskNumber:Long = 0
    private var currentMask = ""
    private var waitingTime = false
    private var count = 0
    private var imageFile: File? = null
    private var maskFile: File? = null
    private var imageSize = ""
    private var newPrompt = ""
    private var prompt = ""
    private var languageCode = 1
    private var insufficientScreenHeight = ""
    private var drawMask = ""
    private var selectImageFirst = ""
    private val errorWrongFile = "Wrong file!!"
    private val errorFileRead = "File read error. Please enable app permission in setting."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_mod)

        contentContainer = findViewById(R.id.image_mod)
        drawableView = findViewById(R.id.drawableView)
        edText = findViewById(R.id.editText6)
        statusText = findViewById(R.id.textView29)
        buttonSelectImage = findViewById(R.id.imageButton7)
        buttonSubmit = findViewById(R.id.imageButton8)
        buttonClear = findViewById(R.id.imageButton9)
        buttonSave = findViewById(R.id.imageButton10)
        frameLayout = findViewById(R.id.imageFrame3)
        seekBar = findViewById(R.id.seekBar1)
        image1 = findViewById(R.id.imagePNG)
        mask1 = findViewById(R.id.maskPNG)
        helpView = findViewById(R.id.qPNG)
        imgDBView = findViewById(R.id.imgDB)

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

        seekBar.isVisible = false
        buttonClear.isVisible = false
        buttonSave.isVisible = false
        val packageManager = packageManager
        appName = applicationInfo.loadLabel(packageManager).toString()
        statusHandler.postDelayed(statusChecking,500)
        MyApp.checkNotification(this,systemDatabase)
        MyApp.checkGPT(systemDatabase,this)
        languageCode = MyApp.checkAppLanguage(systemDatabase,this)

        val initialText: Array<String> = resources.getStringArray(R.array.select_image_detail)
        statusText.text = initialText[languageCode - 1]
        val textDrawMask: Array<String> = resources.getStringArray(R.array.mask)
        drawMask = textDrawMask[languageCode - 1]
        val selectImage: Array<String> = resources.getStringArray(R.array.select_image)
        selectImageFirst = selectImage[languageCode - 1]

        if(MyApp.imageData2 != null) {
            loadImage = true
            drawableView.loadImage(MyApp.imageData2!!)
            seekBar.isVisible = true
            buttonClear.isVisible = true
            buttonSave.isVisible = true
        }

        buttonSelectImage.setOnClickListener {
            openImagePicker()
        }

        buttonClear.setOnClickListener {
            if(drawableView.alreadyDrew) {
                drawableView.clearCanvas()
                drawableView.loadImage(MyApp.imageData2!!)
                statusText.text = if(alreadyUnlock) {
                    insufficientScreenHeight
                } else {
                    drawMask
                }
            }
        }

        buttonSave.setOnClickListener {
            if(loadImage) {
                showSaveImageDialog()
            }
        }

        buttonSubmit.setOnClickListener {
            sendImage()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Update the stroke width based on the progress of the SeekBar
                drawableView.changeStrokeWidth(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Not needed for this example
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Not needed for this example
            }
        })

        image1.setOnClickListener {
            if(newPrompt.isNotEmpty()) {
                val imagePath = databaseImage.loadImageForPrompt(newPrompt)
                if(imagePath is String) {
                    showImageDialog(imagePath.toString())
                }
            }
        }

        helpView.setOnClickListener {
            if(!waitingTime) {
                showHelpDialog()
            }
        }

        imgDBView.setOnClickListener {
            if(!waitingTime) {
                val i = Intent(this@ImageMod, ImageDatabase::class.java)
                startActivity(i)
            }
        }
    }

    override fun onDestroy() {
        statusHandler.removeCallbacks(statusChecking)
        systemDatabase.close()
        databaseImage.close()
        databaseHelper.close()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(waitingTime) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
            statusHandler.removeCallbacks(statusChecking)
        }
    }

    override fun onPause() {
        MyApp.isAppInBackground = true
        edText.clearFocus()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        MyApp.isAppInBackground = false
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            // Handle the result as needed
            handleImagePickerResult(data)
        }
    }

    private fun handleImagePickerResult(data: Intent?) {
        if(data != null) {
            val contentUri = data.data
            if (contentUri != null) {
                alreadyUnlock = false
                drawableView.clearCanvas()
                val imagePath = getImagePathFromContentUri(contentUri) ?: getImagePathFromDownloadManager(contentUri)
                MyApp.imageData2 = imagePath
                if(MyApp.imageData2 != null) {
                    imageFile = imagePathToFile(MyApp.imageData2!!)
                    if(drawableView.loadImage(MyApp.imageData2)) {
                        seekBar.isVisible = true
                        buttonClear.isVisible = true
                        buttonSave.isVisible = true
                        loadImage = true
                    } else {
                        val text = drawableView.error
                        statusText.text = text
                        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
                        seekBar.isVisible = false
                        buttonClear.isVisible = false
                        buttonSave.isVisible = false
                        loadImage = false
                    }
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun imagePathToFile(imagePath: String): File? {
        val bitmap = drawableView.resizeImage(imagePath)
        if((drawableView.imageWidth < 256) || (bitmap == null)) {
            return null
        } else {
            val outputFile = File(cacheDir, "temp_file.png")
            try {
                // Create an output stream for the PNG file
                FileOutputStream(outputFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // Encode as PNG
                }
                return outputFile
            } catch (e: IOException) {
                Log.e("ImageMod error", "General error processing file", e)
                return null
            }
        }
    }

    private fun getImagePathFromContentUri(contentUri: Uri): String? {
        var imagePath:String? = null
        Log.d("ImageMod", "Content URI: $contentUri")
        if (contentUri.scheme == "content") {
            try {
                val cursor = contentResolver.query(contentUri, null, null, null, null)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        Log.d("ImageMod", "Cursor Columns: ${cursor.columnNames.joinToString(",")}")
                        if (index != -1) {
                            Log.d("ImageMod", "Path = ${cursor.getString(index)}")
                            imagePath = cursor.getString(index)
                        }
                    }
                    cursor.close() // Close the cursor even if no data was found
                }
            } catch (e: Exception) {
                Log.e("ImageMod error", "General error retrieving image path", e)
            }
        }
        return imagePath
    }

    private fun getImagePathFromDownloadManager(contentUri: Uri): String? {
        var imagePath:String? = null
        if (contentUri.scheme == "content") {
            try {
                val inputStream = contentResolver.openInputStream(contentUri)
                if (inputStream != null) {
                    // Create a temporary file to store the image data
                    val tempFile = File.createTempFile("temp_image", ".png", cacheDir)
                    val outputStream = FileOutputStream(tempFile)

                    // Copy the image data from the input stream to the temporary file
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()

                    imagePath = tempFile.absolutePath
                    Log.d("ImageMod", "Image absolute path: $imagePath")
                }
            } catch (e: IOException) {
                Log.e("ImageMod error", "General error retrieving image path", e)
            }
        }
        return imagePath
    }

    private fun prepareMaskImage(imageName: String) {
        maskFile = File(cacheDir, imageName)
        try {
            // Create a FileOutputStream to write the bitmap data to the file
            FileOutputStream(maskFile).use { outputStream ->
                // Compress the bitmap to PNG format
                drawableView.getModifiedBitmap()!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
        } catch (e: IOException) {
            Log.e("ImageMod error", "Error processing file", e)
        }
    }

    private fun saveImageToStorage(imageName: String) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                drawableView.getModifiedBitmap()?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            Toast.makeText(this, "Modified image saved to device storage", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaveImageDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Save Image")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Enter Image Name"
        builder.setView(input)
        builder.setPositiveButton("Save") { _, _ ->
            val imageName = input.text.toString().trim()
            if (imageName.isNotEmpty()) {
                saveImageToStorage(imageName)
            } else {
                Toast.makeText(this, "Image name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showImageDialog(imagePath: String) {
        val imageView = ImageView(this)
        val file = File(imagePath)
        imageView.setImageURI(Uri.fromFile(file))
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setView(imageView)
            .setPositiveButton(prompt) { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private val statusChecking = object : Runnable {
        override fun run() {
            if(loadImage) {
                if(image1.background == null) {
                    image1.setBackgroundResource(R.color.yellow)
                    statusText.text = drawMask
                }
            } else {
                if(image1.background != null) {
                    image1.background = null
                }
            }
            if(drawableView.alreadyDrew) {
                if(mask1.background == null) {
                    mask1.setBackgroundResource(R.color.yellow)
                    val text: Array<String> = resources.getStringArray(R.array.already_drew)
                    statusText.text = text[languageCode - 1]
                }
            } else {
                if(mask1.background != null) {
                    mask1.background = null
                    statusText.text = if(alreadyUnlock) {
                        insufficientScreenHeight
                    } else {
                        drawMask
                    }
                }
            }
            if(MyApp.imageData2 != null) {
                if(imageFile == null) {
                    imageFile = imagePathToFile(MyApp.imageData2!!)
                }
                if(fw == 0) {
                    fw = frameLayout.width
                }
                if(fh == 0) {
                    fh = frameLayout.height
                }
                w = drawableView.imageWidth
                h = drawableView.imageHeight
                imageSize = when(w) {
                    1024 -> "1024x1024"
                    512 -> "512x512"
                    256 -> "256x256"
                    else -> ""
                }
                val textScreenHeight: Array<String> = resources.getStringArray(R.array.screen_height_insufficient)
                insufficientScreenHeight = textScreenHeight[languageCode - 1].replace("%1",imageSize)
                if((fw >= w) && (fh >= h) && !alreadyLock) {
                    alreadyLock = true
                    statusText.text = drawMask
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else if((fh < h) && !alreadyUnlock) {
                    alreadyUnlock = true
                    statusText.text = insufficientScreenHeight
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            if(waitingTime) {
                count++
                val waitingTime = count/10.0f
                val text = "Image modification. Please wait...$waitingTime"
                statusText.text = text
            } else {
                count = 0
            }
            statusHandler.postDelayed(this,100)
        }
    }

    private fun sendImage() {
        if(checkAPIkey()) {
            if(edText.text.isNullOrEmpty()) {
                prompt = ""
                if(loadImage) {
                    if(!drawableView.alreadyDrew) {
                        disableButton()
                        waitingTime = true
                        if(imageSize.isNotEmpty()) {
                            if(imageFile != null) {
                                imageAPI.imageVariation(imageFile!!,imageSize) { reply ->
                                    runOnUiThread {
                                        try {
                                            if (reply != null) {
                                                val imageCount = if(systemDatabase.searchQuestion("image count") == null) {
                                                    0L
                                                } else {
                                                    systemDatabase.searchQuestion("image count")!!.toLong()
                                                }
                                                newPrompt = "Image Mod $imageCount"
                                                databaseImage.saveImageToDatabase(newPrompt, reply) { reply ->
                                                    runOnUiThread {
                                                        if(reply == "Image generating success!") {
                                                            val success = "Image modification success! Image had been saved to image database!"
                                                            val imagePath = databaseImage.loadImageForPrompt(newPrompt)
                                                            if(imagePath is String) {
                                                                showImageDialog(imagePath.toString())
                                                            }
                                                            waitingTime = false
                                                            statusText.text = success
                                                            Toast.makeText(this, success, Toast.LENGTH_SHORT).show()
                                                            enableButton()
                                                            if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                                showNotification()
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                waitingTime = false
                                                statusText.text = imageAPI.error
                                                enableButton()
                                                if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                    showNotification()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            waitingTime = false
                                            statusText.text = e.toString()
                                            enableButton()
                                        }
                                    }
                                }
                            } else {
                                waitingTime = false
                                statusText.text = errorFileRead
                                enableButton()
                            }
                        } else {
                            waitingTime = false
                            statusText.text = errorWrongFile
                            enableButton()
                        }
                    } else {
                        val text: Array<String> = resources.getStringArray(R.array.please_type)
                        statusText.text = text[languageCode - 1]
                        Toast.makeText(this, text[languageCode - 1], Toast.LENGTH_SHORT).show()
                    }
                } else {
                    statusText.text = selectImageFirst
                    Toast.makeText(this, selectImageFirst, Toast.LENGTH_SHORT).show()
                }
            } else {
                disableButton()
                prompt = edText.text.toString().trim()
                if(drawableView.alreadyDrew) {
                    maskNumber = if(systemDatabase.searchQuestion("mask") == null) {
                        systemDatabase.insertQuestion("mask","1")
                        1
                    } else {
                        val text = systemDatabase.searchQuestion("mask").toString()
                        text.toLong() + 1
                    }
                    currentMask = "mask$maskNumber"
                    waitingTime = true
                    prepareMaskImage(currentMask)
                    if(imageSize.isNotEmpty()) {
                        if(imageFile != null) {
                            imageAPI.imageEdition(imageFile!!,maskFile,prompt,imageSize) { reply ->
                                runOnUiThread {
                                    try {
                                        if (reply != null) {
                                            val imageCount = if(systemDatabase.searchQuestion("image count") == null) {
                                                0L
                                            } else {
                                                systemDatabase.searchQuestion("image count")!!.toLong()
                                            }
                                            newPrompt = "$prompt (Image Mod $imageCount)"
                                            databaseImage.saveImageToDatabase(newPrompt, reply) { reply ->
                                                runOnUiThread {
                                                    if(reply == "Image generating success!") {
                                                        val success = "Image modification success! Image had been saved to image database!"
                                                        val imagePath = databaseImage.loadImageForPrompt(newPrompt)
                                                        if(imagePath is String) {
                                                            showImageDialog(imagePath.toString())
                                                        }
                                                        waitingTime = false
                                                        statusText.text = success
                                                        Toast.makeText(this, success, Toast.LENGTH_SHORT).show()
                                                        enableButton()
                                                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                            showNotification()
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            waitingTime = false
                                            statusText.text = imageAPI.error
                                            enableButton()
                                            if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                showNotification()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        waitingTime = false
                                        statusText.text = e.toString()
                                        enableButton()
                                    }
                                }
                            }
                        } else {
                            waitingTime = false
                            statusText.text = errorFileRead
                            enableButton()
                        }
                    } else {
                        waitingTime = false
                        statusText.text = errorWrongFile
                        enableButton()
                    }
                } else {
                    if(loadImage) {
                        waitingTime = true
                        if(imageSize.isNotEmpty()) {
                            if(imageFile != null) {
                                imageAPI.imageEdition(imageFile!!,null,prompt,imageSize) { reply ->
                                    runOnUiThread {
                                        try {
                                            if (reply != null) {
                                                val imageCount = if(systemDatabase.searchQuestion("image count") == null) {
                                                    0L
                                                } else {
                                                    systemDatabase.searchQuestion("image count")!!.toLong()
                                                }
                                                newPrompt = "$prompt (Image Mod $imageCount)"
                                                databaseImage.saveImageToDatabase(newPrompt, reply) { reply ->
                                                    runOnUiThread {
                                                        if(reply == "Image generating success!") {
                                                            val success = "Image modification success! Image had been saved to image database!"
                                                            val imagePath = databaseImage.loadImageForPrompt(newPrompt)
                                                            if(imagePath is String) {
                                                                showImageDialog(imagePath.toString())
                                                            }
                                                            waitingTime = false
                                                            statusText.text = success
                                                            Toast.makeText(this, success, Toast.LENGTH_SHORT).show()
                                                            enableButton()
                                                            if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                                showNotification()
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                waitingTime = false
                                                statusText.text = imageAPI.error
                                                enableButton()
                                                if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                    showNotification()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            waitingTime = false
                                            statusText.text = e.toString()
                                            enableButton()
                                        }
                                    }
                                }
                            } else {
                                waitingTime = false
                                statusText.text = errorFileRead
                                enableButton()
                            }
                        } else {
                            waitingTime = false
                            statusText.text = errorWrongFile
                            enableButton()
                        }
                    } else {
                        statusText.text = selectImageFirst
                        Toast.makeText(this, selectImageFirst, Toast.LENGTH_SHORT).show()
                        enableButton()
                    }
                }
            }
        }
    }

    private fun disableButton() {
        hideKeyboard()
        buttonSubmit.isEnabled = false
        buttonSubmit.alpha = 0.3f
        buttonSave.isEnabled = false
        buttonSave.alpha = 0.3f
        buttonClear.isEnabled = false
        buttonClear.alpha = 0.3f
        buttonSelectImage.isEnabled = false
        buttonSelectImage.alpha = 0.3f
    }

    private fun enableButton() {
        buttonSubmit.isEnabled = true
        buttonSubmit.alpha = 1.0f
        buttonSave.isEnabled = true
        buttonSave.alpha = 1.0f
        buttonClear.isEnabled = true
        buttonClear.alpha = 1.0f
        buttonSelectImage.isEnabled = true
        buttonSelectImage.alpha = 1.0f
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        edText.postDelayed({
            imm.hideSoftInputFromWindow(edText.windowToken, 0)
        }, 100)
    }

    private fun showNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "channel_id",
            "Channel Name",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        // Create an Intent to open your app when the notification is clicked
        val intent = Intent(this, ImageMod::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("Image modification done!")
            .setSmallIcon(R.drawable.icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the sound for the notification
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Remove the notification when tapped
        // Show the notification
        val notificationId = 1 // Unique ID for the notification
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun showHelpDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Image Modification")
        val textView = TextView(this)
        val text: Array<String> = resources.getStringArray(R.array.image_mod_help)
        textView.text = text[languageCode - 1]
        builder.setView(textView)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun checkAPIkey():Boolean {
        return MyApp.checkAPIkey(this,systemDatabase)
    }
}