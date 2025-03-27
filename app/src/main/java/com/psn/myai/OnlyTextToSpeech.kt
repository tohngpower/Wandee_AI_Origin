package com.psn.myai

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class OnlyTextToSpeech : ComponentActivity(), AIAssistant.AssistantListener {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var frameLayout: FrameLayout
    private lateinit var assistant: AIAssistant
    private lateinit var imgR: ImageView
    private lateinit var buttonSelectAI: ImageButton
    private lateinit var buttonHome: ImageButton
    private lateinit var buttonPlay: ImageButton
    private lateinit var buttonPause: ImageButton
    private lateinit var buttonSetting: ImageButton
    private lateinit var buttonSave: ImageButton
    private lateinit var edText : EditText
    private lateinit var progressBar: ProgressBar
    private val imageHandler = Handler(Looper.getMainLooper())
    private var imageIndex = 0
    private var imageIndex2 = 0
    private var imageIndex3 = 0
    private val animationDelayRange = 2000L..5000L // Delay range between 2 to 5 seconds in milliseconds
    private var imageResId = R.drawable.bg
    private val systemDatabase = SystemDatabase(this)
    private val audioAPI = AudioAPI(this)
    private var isSpeaking = false
    private var breathing = true
    private var eyeBlinking = true
    private var enableOpenAITTS = false
    private var ttsVoice = "nova"
    private var ttsModel = "tts-1"
    private var ttsSpeed = 1.00f
    private var languageNumber = 0
    private var mediaPlayer: MediaPlayer? = null
    private var themeNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.only_text_to_speech)

        contentContainer = findViewById(R.id.only_tts)
        frameLayout = findViewById(R.id.imageFrame2)
        imgR = findViewById(R.id.imageView8)
        buttonSelectAI = findViewById(R.id.button13)
        buttonHome = findViewById(R.id.button14)
        buttonPlay = findViewById(R.id.imageButton)
        buttonPause = findViewById(R.id.imageButton2)
        buttonSetting = findViewById(R.id.imageButton11)
        buttonSave = findViewById(R.id.save_tts_btn)
        edText = findViewById(R.id.editText4)
        progressBar = findViewById(R.id.progressBar3)

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

        assistant = AIAssistant(this, this)
        MyApp.setAIResource(systemDatabase)
        checkFontSize()
        languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        startBreathingAnimation()
        startRandomImageAnimation()
        enableButton()
        progressBar.isVisible = false

        val currentInterval = MyApp.getCurrentTimeOfDay()
        val randomImage = MyApp.getRandomImageForInterval(currentInterval)
        imageResId = randomImage
        frameLayout.setBackgroundResource(imageResId)
        val fg = ContextCompat.getDrawable(this@OnlyTextToSpeech,MyApp.imageResources2[0])
        imgR.foreground = fg
        imgR.setImageResource(MyApp.imageResources[0])
        imgR.setBackgroundResource(MyApp.imageBreathing[0])
        loadLastEditText()
        checkOpenAITTS()

        buttonPlay.setOnClickListener {
            ttsStartSpeaking()
        }

        buttonPause.setOnClickListener {
            if(isSpeaking) {
                isSpeaking = false
                if(enableOpenAITTS) {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            it.stop()
                            it.release()
                            mediaPlayer = null
                        }
                    }
                } else {
                    assistant.stopSpeaking()
                }
                stopImageAnimation()
                enableButton()
            }
        }

        buttonSelectAI.setOnClickListener {
            val i = Intent(this, SelectAI::class.java)
            //Change the activity.
            startActivity(i)
        }

        buttonHome.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            //Change the activity.
            startActivity(i)
            finish()
        }

        buttonSetting.setOnClickListener {
            val i = Intent(this, Settings2::class.java)
            startActivity(i)
        }

        buttonSave.setOnClickListener {
            saveAudioDialog()
        }

        imgR.setOnClickListener {
            val backgroundAnimator = frameLayout.background?.let { backgroundDrawable ->
                ObjectAnimator.ofInt(
                    backgroundDrawable,
                    "alpha",
                    100, 255 // Adjust alpha values as needed
                ).apply {
                    duration = 1000 // Set the duration of the animation
                }
            }
            backgroundAnimator?.start()
        }

        edText.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                // Calculate the maximum number of lines based on the current height
                val maxLines = edText.height / edText.lineHeight

                // Set the calculated maxLines to the EditText
                edText.maxLines = maxLines

                // Remove the listener as we don't need it anymore
                edText.viewTreeObserver.removeOnPreDrawListener(this)

                return true
            }
        })
    }

    override fun onPause() {
        edText.clearFocus()
        if(!isSpeaking) {
            stopBreathingAnimation()
            stopRandomImageAnimation()
        }
        saveLastEditText()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroy() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
        saveLastEditText()
        assistant.destroy()
        imageHandler.removeCallbacks(imageRunnable)
        imageHandler.removeCallbacks(imageRunnable2)
        imageHandler.removeCallbacks(imageRunnable3)
        systemDatabase.close()
        super.onDestroy()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java", ReplaceWith("moveTaskToBack(true)"))
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onInitCompleted() {
        runOnUiThread {
            assistant.setUtteranceProgressListener()
        }
    }

    override fun onStartSpeech() {
        runOnUiThread {
            isSpeaking = true
            disableButton()
            startImageAnimation()
        }
    }

    override fun onSpeechError(utteranceId: String?, errorCode: Int?) {
        runOnUiThread {
            assistant.speak("Too long to speak")
        }
    }

    override fun onSpeechCompleted() {
        runOnUiThread {
            isSpeaking = false
            stopImageAnimation()
            enableButton()
        }
    }

    private fun startImageAnimation() {
        imageIndex = 0
        imgR.setImageResource(MyApp.imageResources[imageIndex])
        imageHandler.post(imageRunnable)
    }

    private fun stopImageAnimation() {
        imageHandler.removeCallbacks(imageRunnable)
        imgR.setImageResource(MyApp.imageResources[0])
    }

    private fun startRandomImageAnimation() {
        eyeBlinking = true
        imageIndex2 = 0
        val randomDelay = animationDelayRange.random()
        val fg = ContextCompat.getDrawable(this@OnlyTextToSpeech,MyApp.imageResources2[0])
        imgR.foreground = fg
        imgR.setImageResource(MyApp.imageResources[0])
        imageHandler.postDelayed(imageRunnable2, randomDelay)
    }

    private fun stopRandomImageAnimation() {
        eyeBlinking = false
        imageHandler.removeCallbacks(imageRunnable2)
        imgR.setImageResource(MyApp.imageResources[0])
    }

    private fun startBreathingAnimation() {
        breathing = true
        imageIndex3 = 0
        imageHandler.post(imageRunnable3)
    }

    private fun stopBreathingAnimation() {
        breathing = false
        imageHandler.removeCallbacks(imageRunnable3)
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
                val fg = ContextCompat.getDrawable(this@OnlyTextToSpeech,MyApp.imageResources2[imageIndex2])
                imgR.foreground = fg
                imageIndex2++
                imageHandler.postDelayed(this, 30)
            } else {
                imageHandler.removeCallbacks(this)
                val randomDelay = animationDelayRange.random()
                val fg = ContextCompat.getDrawable(this@OnlyTextToSpeech,MyApp.imageResources2[0])
                imgR.foreground = fg
                imageIndex2=0
                imageHandler.postDelayed(this, randomDelay)
            }
        }
    }

    private val imageRunnable3 = object : Runnable {
        override fun run() {
            imageIndex3 = (imageIndex3 + 1) % MyApp.imageBreathing.size
            imgR.setBackgroundResource(MyApp.imageBreathing[imageIndex3])
            imageHandler.postDelayed(this, 150)
        }
    }

    private fun checkFontSize() {
        MyApp.checkFontSize(systemDatabase)
        edText.setTextAppearance(MyApp.textAppearance)
    }

    private fun enablePauseButton() {
        buttonPause.isEnabled = true
        buttonPause.isClickable = true
        buttonPause.alpha = 1.0f
        progressBar.isVisible = false
    }

    private fun disablePauseButton() {
        buttonPause.isEnabled = false
        buttonPause.isClickable = false
        buttonPause.alpha = 0.3f
        progressBar.isVisible = true
    }

    private fun enableButton() {
        buttonPlay.isEnabled=true
        buttonPlay.isClickable=true
        buttonPause.isEnabled = false
        buttonPause.isClickable = false
        buttonSelectAI.isEnabled=true
        buttonSelectAI.isClickable=true
        buttonHome.isEnabled=true
        buttonHome.isClickable=true
        buttonSetting.isEnabled=true
        buttonSetting.isClickable=true
        buttonSave.isEnabled=true
        buttonSave.isClickable=true
        edText.isEnabled=true
        edText.alpha = 1.0f
        buttonPlay.alpha = 1.0f
        buttonSelectAI.alpha = 1.0f
        buttonHome.alpha = 1.0f
        buttonSetting.alpha = 1.0f
        buttonPause.alpha = 0.3f
        buttonSave.alpha = 1.0f
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun disableButton() {
        buttonPlay.isEnabled=false
        buttonPlay.isClickable=false
        buttonPause.isEnabled = true
        buttonPause.isClickable = true
        buttonSelectAI.isEnabled=false
        buttonSelectAI.isClickable=false
        buttonHome.isEnabled=false
        buttonHome.isClickable=false
        buttonSetting.isEnabled=false
        buttonSetting.isClickable=false
        buttonSave.isEnabled=false
        buttonSave.isClickable=false
        edText.isEnabled=false
        edText.alpha = 0.3f
        buttonPlay.alpha = 0.3f
        buttonSelectAI.alpha = 0.3f
        buttonHome.alpha = 0.3f
        buttonSetting.alpha = 0.3f
        buttonPause.alpha = 1.0f
        buttonSave.alpha = 0.3f
        hideKeyboard()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        edText.postDelayed({
            imm.hideSoftInputFromWindow(edText.windowToken, 0)
        }, 100)
    }

    private fun saveLastEditText() {
        if(edText.text.isNullOrEmpty()) {
            if(systemDatabase.searchQuestion("lastEditText3")==null) {
                systemDatabase.insertQuestion("lastEditText3","")
            } else {
                systemDatabase.replaceAnswer("lastEditText3","")
            }
        } else {
            if(systemDatabase.searchQuestion("lastEditText3")==null) {
                systemDatabase.insertQuestion("lastEditText3",edText.text.toString())
            } else {
                systemDatabase.replaceAnswer("lastEditText3",edText.text.toString())
            }
        }
    }

    private fun loadLastEditText() {
        val storedText = systemDatabase.searchQuestion("lastEditText3")
        if (!storedText.isNullOrEmpty()) {
            edText.setText(storedText)
        }
    }

    private fun checkOpenAITTS() {
        val apiKeyStatus = MyApp.checkOpenAIAPIkey(systemDatabase)
        if(apiKeyStatus) {
            val ttsOn = systemDatabase.searchQuestion("tts switch")
            if(ttsOn == null) {
                systemDatabase.insertQuestion("tts switch","OFF")
            } else {
                enableOpenAITTS = ttsOn=="ON"
            }
        }

        val voiceName = systemDatabase.searchQuestion("voice name")
        if(voiceName == null) {
            systemDatabase.insertQuestion("voice name","nova")
        } else {
            ttsVoice = voiceName
        }

        val audioModel = systemDatabase.searchQuestion("audio model")
        if(audioModel == null) {
            systemDatabase.insertQuestion("audio model","tts-1")
        } else {
            ttsModel = audioModel
        }

        val voiceSpeed = systemDatabase.searchQuestion("voice speed")
        if(voiceSpeed == null) {
            systemDatabase.insertQuestion("voice speed","100")
        } else {
            try {
                ttsSpeed = voiceSpeed.toFloat()/100
            } catch (e: NumberFormatException) {
                systemDatabase.replaceAnswer("voice speed","100")
            }
        }
    }

    private fun playMp3File(filePath: String, onStart: () -> Unit, onEnd: () -> Unit) {
        mediaPlayer = MediaPlayer()

        try {
            mediaPlayer?.apply {
                setDataSource(filePath)
                prepare()

                setOnPreparedListener {
                    onStart() // Call the onStart callback
                    start()
                }

                setOnCompletionListener {
                    onEnd() // Call the onEnd callback
                    release()
                    mediaPlayer = null
                }
            }
        } catch (e: IOException) {
            enableButton()
        }
    }

    private fun ttsStartSpeaking() {
        val text = edText.text.trim().toString()
        if(text.isNotEmpty()) {
            if(enableOpenAITTS) {
                disableButton()
                val newText = "$ttsVoice:$ttsModel:$ttsSpeed:$text"
                val audioPath = systemDatabase.searchQuestion(newText)
                if(audioPath != null) {
                    playMp3File(audioPath,
                        onStart = {
                            isSpeaking = true
                            startImageAnimation()
                        },
                        onEnd = {
                            isSpeaking = false
                            stopImageAnimation()
                            enableButton()
                        }
                    )
                } else {
                    disablePauseButton()
                    audioAPI.tts(ttsVoice,text,ttsModel,ttsSpeed) { file ->
                        runOnUiThread {
                            if(file != null) {
                                val directory = File(filesDir, "audio")
                                if (!directory.exists()) {
                                    directory.mkdirs()
                                }
                                var fileCount = 0L
                                val audioCount = systemDatabase.searchQuestion("audio count")
                                if(audioCount == null) {
                                    systemDatabase.insertQuestion("audio count","0")
                                } else {
                                    fileCount = audioCount.toLong()
                                }
                                fileCount++
                                systemDatabase.replaceAnswer("audio count",fileCount.toString())
                                val audioFile = File(directory, "$fileCount.mp3")
                                val fileInputStream = FileInputStream(file)
                                val fileOutputStream = FileOutputStream(audioFile)
                                try {
                                    fileInputStream.use { input ->
                                        fileOutputStream.use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    systemDatabase.insertQuestion(newText,audioFile.absolutePath)
                                    playMp3File(audioFile.absolutePath,
                                        onStart = {
                                            enablePauseButton()
                                            isSpeaking = true
                                            startImageAnimation()
                                        },
                                        onEnd = {
                                            isSpeaking = false
                                            stopImageAnimation()
                                            enableButton()
                                        }
                                    )
                                } catch (e: IOException) {
                                    enablePauseButton()
                                    enableButton()
                                }
                            } else {
                                Toast.makeText(this,audioAPI.error, Toast.LENGTH_SHORT).show()
                                enablePauseButton()
                                enableButton()
                            }
                        }
                    }
                }
            } else {
                MyApp.checkAIVoice(systemDatabase, assistant)
                assistant.speak(text)
            }
        } else {
            val pleaseType: Array<String> = resources.getStringArray(R.array.please_type)
            Toast.makeText(this,pleaseType[languageNumber-1], Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAudioDialog() {
        val text = edText.text.trim().toString()
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Save TTS Audio File?")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Enter TTS Audio Name"
        builder.setView(input)
        builder.setPositiveButton("Save") { _, _ ->
            val audioName = input.text.toString().trim()
            if (audioName.isNotEmpty()&&text.isNotEmpty()) {
                if(enableOpenAITTS) {
                    disableButton()
                    val newText = "$ttsVoice:$ttsModel:$ttsSpeed:$text"
                    val audioPath = systemDatabase.searchQuestion(newText)
                    if(audioPath != null) {
                        val file = File(audioPath)
                        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val audioFile = File(directory,"$audioName.mp3")
                        val fileInputStream = FileInputStream(file)
                        val fileOutputStream = FileOutputStream(audioFile)
                        try {
                            fileInputStream.use { input ->
                                fileOutputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Toast.makeText(this, "TTS saved successfully.", Toast.LENGTH_SHORT).show()
                        } catch (e: IOException) {
                            Toast.makeText(this, "Failed to save TTS: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        enableButton()
                    } else {
                        disableButton()
                        disablePauseButton()
                        audioAPI.tts(ttsVoice,text,ttsModel,ttsSpeed) { file ->
                            runOnUiThread {
                                if(file != null) {
                                    val directory = File(filesDir, "audio")
                                    if (!directory.exists()) {
                                        directory.mkdirs()
                                    }
                                    var fileCount = 0L
                                    val audioCount = systemDatabase.searchQuestion("audio count")
                                    if(audioCount == null) {
                                        systemDatabase.insertQuestion("audio count","0")
                                    } else {
                                        fileCount = audioCount.toLong()
                                    }
                                    fileCount++
                                    systemDatabase.replaceAnswer("audio count",fileCount.toString())
                                    val audioFile = File(directory, "$fileCount.mp3")
                                    val directory2 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    val audioFile2 = File(directory2,"$audioName.mp3")
                                    try {
                                        // Copy file to audioFile
                                        FileInputStream(file).use { input ->
                                            FileOutputStream(audioFile).use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        // Copy file to audioFile2
                                        FileInputStream(file).use { input ->
                                            FileOutputStream(audioFile2).use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        systemDatabase.insertQuestion(newText,audioFile.absolutePath)
                                        Toast.makeText(this, "TTS saved successfully.", Toast.LENGTH_SHORT).show()
                                    } catch (e: IOException) {
                                        Toast.makeText(this, "Failed to save TTS: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    enablePauseButton()
                                    enableButton()
                                } else {
                                    Toast.makeText(this,audioAPI.error, Toast.LENGTH_SHORT).show()
                                    enablePauseButton()
                                    enableButton()
                                }
                            }
                        }
                    }
                } else {
                    disableButton()
                    disablePauseButton()
                    MyApp.checkAIVoice(systemDatabase, assistant)
                    assistant.saveTtsToFile(text, audioName) { success ->
                        runOnUiThread {
                            if (success) {
                                Toast.makeText(this, "TTS saved successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to save TTS.", Toast.LENGTH_SHORT).show()
                            }
                            enablePauseButton()
                            enableButton()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Audio name and text cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun loadData() {
        MyApp.setAIResource(systemDatabase)
        if(!breathing) {
            startBreathingAnimation()
        }
        if(!eyeBlinking) {
            startRandomImageAnimation()
        }
        loadLastEditText()
        val fg = ContextCompat.getDrawable(this@OnlyTextToSpeech,MyApp.imageResources2[0])
        imgR.foreground = fg
        imgR.setImageResource(MyApp.imageResources[0])
        checkFontSize()
        MyApp.checkNotification(this,systemDatabase)
        checkOpenAITTS()
    }
}