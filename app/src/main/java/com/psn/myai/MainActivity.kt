package com.psn.myai

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.Typeface
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Base64
import androidx.core.net.toUri

class MainActivity : ComponentActivity(), AIAssistant.AssistantListener {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var buttonLayout: LinearLayout
    private lateinit var shareView: LinearLayout
    private lateinit var editTextView: LinearLayout
    private lateinit var frameLayout: FrameLayout
    private lateinit var buttonTalk: ImageButton
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonSelectAI: ImageButton
    private lateinit var buttonSettings: ImageButton
    private lateinit var buttonMenu: ImageButton
    private lateinit var buttonSelectImage: ImageButton
    private lateinit var buttonSelectAudio: ImageButton
    private lateinit var buttonProceedAudio: ImageButton
    private lateinit var expandImage: ImageButton
    private lateinit var shareButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var speechToTextButton: ImageButton
    private lateinit var imgR: ImageView
    private lateinit var upImage: ImageView
    private lateinit var downImage: ImageView
    private lateinit var visionPicture: ImageView
    private lateinit var qrCode: ImageView
    private lateinit var txtView: TextView
    private lateinit var titleView: TextView
    private lateinit var txtFrom: TextView
    private lateinit var txtTo: TextView
    private lateinit var edText : AutoCompleteTextView
    private lateinit var scrollView: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner
    private lateinit var assistant: AIAssistant
    private val imageHandler = Handler(Looper.getMainLooper())
    private val handler = Handler(Looper.getMainLooper())
    private var imageIndex = 0
    private var imageIndex2 = 0
    private var imageIndex3 = 0
    private var waitCount = 0
    private val animationDelayRange = 1500L..5000L // Delay range between 1.5 to 5.0 seconds in milliseconds
    private lateinit var speechRecognizer: SpeechRecognizer
    private val textAPI = TextAPI()
    private val imageAPI = ImageAPI()
    private val systemDatabase = SystemDatabase(this)
    private val databaseHelper = DatabaseHelper(this)
    private val databaseImage = DatabaseImage(this)
    private var question = ""
    private var promptText = ""
    private var gotQuestion = false
    private var errorCount = 0
    private var textMode = false
    private val imageGeneratingText = "Image generating..."
    private var helpText = ""
    private var languageCode = "th-TH"
    private var inspireText = ""
    private lateinit var spannableString: SpannableString
    private var showMenuFlag = false
    private var busyFlag = false
    private var aboutText = ""
    private var themeNumber = 0
    private var noEyeBlinking = false
    private var scrollY = 0
    private var scrollViewHeight = 0
    private var scrollViewChildHeight = 0
    private var initComplete = false
    private var breathing = true
    private var justStopBreathing = false
    private var justStopEyeBlinking = false
    private var base64Image:String? = null
    private var imageType:String? = null
    private var fileType:String? = null
    private var alreadySelectedImage = false
    private var alreadySelectedFile = false
    private var alreadyClickEdit = false
    private var gemini = true
    private var claude = false
    private var imageLoad = false
    private var selectedImage: String? = null
    private var fileProcessing = ""
    private var base64File:String? = null
    private var videoFilePath:String? = null
    private var llamaVision = false
    private val success = "Image generating success!"
    private var ttsFlag = false
    private var attachedPicture = ""
    private var llamaText = false
    private var startMicSound: MediaPlayer? = null
    private var errorMicSound: MediaPlayer? = null
    private val maxMsg = 50
    private var isDark = true
    private val audioAPI = AudioAPI(this)
    private var whisperMode = false
    private var recordingFile: File? = null
    private var silenceStartTime = 0L
    private val silenceTimeout = 5000L // 5 seconds
    private var languageNumber = 0
    private var isSpeech = false
    private val modelPath = "lite-model_yamnet_classification_tflite_1.tflite"
    private val probabilityThreshold = 0.3f
    private var mediaRecorder: MediaRecorder? = null
    private var classificationRunnable: Runnable? = null
    private var isRecord = false
    private var isKeyboardVisible = true
    private var storeData = false
    private var speechToText = false
    private var stopRecordFlag = false
    private val recordingDuration = 5000L
    private var liveMode = false
    private var genImage = false
    private var nowSharing = false
    private var genImageBitmap: Bitmap? = null
    private lateinit var clipboardHelper: ClipboardHelper
    private var isLandscape = false
    private var density = 0.0f
    private var splitScreenMode = false
    private var tabletMode = false
    private var micOn = false
    private var geminiThink = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.layout)

        contentContainer = findViewById(R.id.main)
        closeButton = findViewById(R.id.imageClose3)
        frameLayout = findViewById(R.id.imageFrame)
        buttonTalk = findViewById(R.id.button)
        buttonSend = findViewById(R.id.imageButton4)
        buttonSelectAI = findViewById(R.id.button6)
        buttonSettings = findViewById(R.id.button10)
        buttonMenu = findViewById(R.id.menu_btn)
        buttonSelectImage = findViewById(R.id.picture_button)
        buttonSelectAudio = findViewById(R.id.audio_button)
        imgR = findViewById(R.id.imageView)
        upImage = findViewById(R.id.upIMG1)
        downImage = findViewById(R.id.downIMG1)
        visionPicture = findViewById(R.id.visionImage)
        txtView = findViewById(R.id.textView)
        titleView = findViewById(R.id.title)
        edText=findViewById(R.id.editText)
        scrollView=findViewById(R.id.SV1)
        progressBar = findViewById(R.id.progressBar2)
        expandImage = findViewById(R.id.expandView)
        shareButton = findViewById(R.id.share_btn)
        buttonLayout = findViewById(R.id.linearLayout13)
        shareView = findViewById(R.id.share_view)
        qrCode = findViewById(R.id.qr_code2)
        txtFrom = findViewById(R.id.from_label)
        txtTo = findViewById(R.id.to_label)
        fromSpinner = findViewById(R.id.speech_spinner)
        toSpinner = findViewById(R.id.text_spinner)
        buttonProceedAudio = findViewById(R.id.audio_proceed)
        editTextView = findViewById(R.id.editTextLayout)
        speechToTextButton = findViewById(R.id.speech_to_text_button)

        ViewCompat.setOnApplyWindowInsetsListener(contentContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply insets as padding to the view:
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.updateMargins(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            isKeyboardVisible = if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) windowInsets.isVisible(WindowInsetsCompat.Type.ime()) else true
            upImage.isVisible = false
            downImage.isVisible = false
            WindowInsetsCompat.CONSUMED
        }

        assistant = AIAssistant(this, this)
        clipboardHelper = ClipboardHelper(this)
        MyApp.setAIResource(systemDatabase)
        checkFontSize()
        val gpt = MyApp.checkGPT(systemDatabase,this)
        gemini = (gpt == 2)
        claude = (gpt == 3)
        llamaVision = (gpt == 4)
        llamaText = (gpt == 5)
        geminiThink = (gpt == 6)
        languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        checkLanguage()
        checkImageSize()
        startBreathingAnimation()
        startRandomImageAnimation()
        MyApp.checkNotification(this,systemDatabase)
        initSpeechRec()
        loadLastEditText()
        upImage.isVisible = false
        downImage.isVisible = false
        closeButton.isVisible = false
        shareButton.isVisible = false
        titleView.isVisible = false
        qrCode.isVisible = false
        buttonSelectAudio.isVisible = gemini
        buttonSelectImage.isVisible = !llamaText
        expandImage.isVisible = MyApp.messageList.isNotEmpty()

        val whisper = systemDatabase.searchQuestion("whisper")
        whisperMode = if(whisper == null) {
            systemDatabase.insertQuestion("whisper","OFF")
            false
        } else {
            (whisper == "ON")
        }
        val dataStore = systemDatabase.searchQuestion("store data")
        storeData = if(dataStore == null) {
            systemDatabase.insertQuestion("store data","OFF")
            false
        } else {
            (dataStore == "ON")
        }

        startMicSound = MediaPlayer.create(this, R.raw.start_mic)
        errorMicSound = MediaPlayer.create(this, R.raw.error_mic)

        val lgCode: Array<String> = if(whisperMode) {
            resources.getStringArray(R.array.language_code2)
        } else {
            resources.getStringArray(R.array.language_code)
        }
        languageCode = lgCode[languageNumber-1]

        val processFile: Array<String> = resources.getStringArray(R.array.file_processing)
        fileProcessing = processFile[languageNumber-1]

        val welcome: Array<String> = resources.getStringArray(R.array.welcome_text)
        txtView.hint = welcome[languageNumber-1]
        val helloMaster: Array<String> = resources.getStringArray(R.array.hello_master)
        val welcomeText = helloMaster[languageNumber-1]

        val pleaseType: Array<String> = resources.getStringArray(R.array.please_type)
        checkModeSpeechToText()
        val languageAdapter = ArrayAdapter.createFromResource(this,R.array.text_language,android.R.layout.simple_spinner_item)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fromSpinner.adapter = languageAdapter
        toSpinner.adapter = languageAdapter

        contentContainer.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                private var previousHeight = 0

                override fun onGlobalLayout() {
                    val rect = Rect()
                    contentContainer.getWindowVisibleDisplayFrame(rect)
                    val currentHeight = rect.bottom - rect.top

                    if (previousHeight != 0) {
                        val isKeyboardVisible = previousHeight > currentHeight
                        if(isKeyboardVisible) {
                            val yOffset = currentHeight - contentContainer.height
                            edText.dropDownVerticalOffset = yOffset - 300
                            Log.d("edit text","Y offset = $yOffset")
                        }
                    }
                    previousHeight = currentHeight
                }
            }
        )

        val suggestions = listOf("imageGEN ","check usage","contribute","about","help")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, suggestions)
        edText.setAdapter(adapter)
        edText.threshold = 2
        edText.dropDownWidth = 350

        when(themeNumber) {
            0 -> {
                edText.setDropDownBackgroundResource(R.drawable.light_border)
                isDark = false
            }
            else  -> {
                edText.setDropDownBackgroundResource(R.drawable.dark_border)
                isDark = true
            }
        }

        val currentInterval = MyApp.getCurrentTimeOfDay()
        val randomImage = MyApp.getRandomImageForInterval(currentInterval)
        MyApp.imageResId = randomImage
        frameLayout.setBackgroundResource(MyApp.imageResId)
        val fg = ContextCompat.getDrawable(this@MainActivity,MyApp.imageResources2[0])
        imgR.foreground = fg
        imgR.setImageResource(MyApp.imageResources[0])
        imgR.setBackgroundResource(MyApp.imageBreathing[0])

        val inspireTextHandler = Handler(Looper.getMainLooper())
        val updateTextViewRunnable = Runnable {
            txtView.text = Html.fromHtml(MyApp.makeBoxCodes(inspireText), Html.FROM_HTML_MODE_COMPACT)
            handler.post(svRunnable)
        }
        inspireTextHandler.postDelayed(updateTextViewRunnable, 1000)

        if(systemDatabase.searchQuestion("errorAPI") == null) {
            systemDatabase.insertQuestion("errorAPI","0")
        }
        if(systemDatabase.searchQuestion("errorRapidAPI") == null) {
            systemDatabase.insertQuestion("errorRapidAPI","0")
        }

        progressBar.isVisible = false

        val displayMetrics = resources.displayMetrics
        density = displayMetrics.density
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val widthDP = screenWidth / density
        isLandscape = screenWidth > screenHeight
        splitScreenMode = (screenHeight < 1200) && (screenWidth < 1500)
        if(splitScreenMode) {
            frameLayout.isVisible = false
            buttonLayout.orientation = LinearLayout.HORIZONTAL
        } else if(isLandscape) {
            buttonLayout.orientation = LinearLayout.HORIZONTAL
        } else if(widthDP > 550) {
            tabletMode = true
            buttonLayout.orientation = LinearLayout.HORIZONTAL
        } else {
            buttonLayout.orientation = LinearLayout.VERTICAL
        }
        Log.d("Display","Screen width = $screenWidth px or $widthDP dp, Screen height = $screenHeight px")

        buttonTalk.setOnClickListener {
            inspireTextHandler.removeCallbacks(updateTextViewRunnable)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO_PERMISSION
                )
            } else {
                if(initComplete) {
                    if(speechToText) {
                        liveTranscription()
                    } else {
                        MyApp.checkAIVoice(systemDatabase, assistant)
                        MyApp.checkAIPersonal(systemDatabase)
                        if(gemini || geminiThink) {
                            MyApp.checkGeminiAPIkey(systemDatabase)
                        } else if(claude) {
                            MyApp.checkClaudeAPIkey(systemDatabase)
                        } else if(!(llamaVision || llamaText)) {
                            checkAPIkey()
                        }
                        shareButton.isVisible = false
                        attachedPicture = ""
                        assistant.speak(welcomeText)
                        txtView.text=welcomeText
                        textMode=false
                        handler.postDelayed(checkAndRefreshTTS,1000)
                    }
                } else {
                    ttsDialog()
                }
            }
        }

        buttonSend.setOnClickListener {
            if(initComplete) {
                inspireTextHandler.removeCallbacks(updateTextViewRunnable)
                if(!edText.text.isNullOrEmpty()) {
                    question = edText.text.toString()
                    textMode=true
                    gotQuestion=true
                    MyApp.checkAIVoice(systemDatabase, assistant)
                    MyApp.checkAIPersonal(systemDatabase)
                    if(gemini || geminiThink) {
                        MyApp.checkGeminiAPIkey(systemDatabase)
                    } else if(claude) {
                        MyApp.checkClaudeAPIkey(systemDatabase)
                    } else if(!(llamaVision || llamaText)) {
                        checkAPIkey()
                    }
                    shareButton.isVisible = false
                    attachedPicture = ""
                    assistant.speak(" ")
                    handler.postDelayed(checkAndRefreshTTS,1000)
                } else {
                    Toast.makeText(this@MainActivity,pleaseType[languageNumber-1], Toast.LENGTH_SHORT).show()
                }
            } else {
                ttsDialog()
            }
        }

        buttonSelectAI.setOnClickListener {
            val i = Intent(this@MainActivity, SelectAI::class.java)
            startActivity(i)
            enableButton()
        }

        buttonSettings.setOnClickListener {
            val i = Intent(this@MainActivity, Settings::class.java)
            startActivity(i)
            enableButton()
        }

        buttonMenu.setOnClickListener {
            if(MyApp.isSpeaking) {
                buttonMenu.setImageResource(R.drawable.menu_icon)
                assistant.stopSpeaking()
                MyApp.isSpeaking = false
                stopImageAnimation()
                enableButton()
            } else {
                if(!busyFlag) {
                    showMenuFlag = true
                    inspireTextHandler.removeCallbacks(updateTextViewRunnable)
                    showContextMenu()
                }
            }
            if(speechToText && micOn) {
                stopRecordFlag = true
            }
        }

        buttonSelectImage.setOnClickListener {
            inspireTextHandler.removeCallbacks(updateTextViewRunnable)
            openImagePicker()
        }

        buttonSelectAudio.setOnClickListener {
            inspireTextHandler.removeCallbacks(updateTextViewRunnable)
            if(speechToText) {
                openAudioPicker()
            } else {
                openFilePicker()
            }
        }

        buttonProceedAudio.setOnClickListener {
            proceedAudio()
        }

        expandImage.setOnClickListener {
            val intent = Intent(this, ChatHistory::class.java)
            startActivity(intent)
        }

        shareView.setOnLongClickListener {
            if(speechToText && clipboardHelper.getClipboardHistory().isNotEmpty()) {
                showPasteDialog()
                true
            } else {
                false
            }
        }

        speechToTextButton.setOnClickListener {
            changeModeSpeechToText()
        }

        txtView.setOnLongClickListener {
            if(!txtView.text.isNullOrEmpty()) {
                showDialogMenu()
                true
            } else {
                false
            }
        }

        edText.setOnClickListener {
            closeButton.isVisible = true
            alreadyClickEdit = true
        }

        imgR.setOnClickListener {
            if(imageLoad) {
                if (selectedImage != null) {
                    MyApp.imageData = selectedImage
                    MyApp.splitTextKeepLastDimension(MyApp.imagePrompt)
                    val intent = Intent(this, FullImageView::class.java)
                    startActivity(intent)
                }
            } else {
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
                if(!genImage) {
                    val intent = Intent(this, FullView::class.java)
                    startActivity(intent)
                }
            }
        }

        imgR.setOnLongClickListener {
            if(MyApp.isSpeaking) {
                buttonMenu.setImageResource(R.drawable.menu_icon)
                assistant.stopSpeaking()
                MyApp.isSpeaking = false
                stopImageAnimation()
                enableButton()
            } else {
                if(!busyFlag) {
                    showMenuFlag = true
                    inspireTextHandler.removeCallbacks(updateTextViewRunnable)
                    showContextMenu()
                }
            }
            true
        }

        upImage.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_UP)
        }

        downImage.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            checkScrollView()
        }

        visionPicture.setOnClickListener {
            unselectedFile()
            unselectedImage()
        }

        closeButton.setOnClickListener {
            if(edText.text.isNotEmpty() && alreadyClickEdit && !busyFlag && !MyApp.isSpeaking) {
                edText.text.clear()
                alreadyClickEdit = false
                closeButton.isVisible = false
            }
            if(alreadySelectedImage && !busyFlag && !MyApp.isSpeaking) {
                alreadySelectedImage = false
                val text = "\uD83D\uDE0A"
                txtView.text = text
                base64Image = null
                visionPicture.setImageResource(R.drawable.a02)
                if(edText.text.isNullOrEmpty()) {
                    closeButton.isVisible = false
                } else {
                    alreadyClickEdit = true
                }
                shareButton.isVisible = false
            }
            if(alreadySelectedFile && !busyFlag && !MyApp.isSpeaking) {
                alreadySelectedFile = false
                val text = "\uD83D\uDE0A"
                txtView.text = text
                base64File = null
                videoFilePath = null
                visionPicture.setImageResource(R.drawable.a02)
                if(edText.text.isNullOrEmpty()) {
                    closeButton.isVisible = false
                } else {
                    alreadyClickEdit = true
                }
                shareButton.isVisible = false
            }
        }

        shareButton.setOnClickListener {
            takeScreenShotAndShare()
        }

        handler.post(svRunnable)
        alphaAnimation(upImage)
        alphaAnimation(downImage)
    }

    override fun onPause() {
        edText.clearFocus()
        MyApp.isAppInBackground = true
        saveLastEditText()
        handler.removeCallbacks(svRunnable)
        if(breathing) {
            stopBreathingAnimation()
            justStopBreathing = true
        }
        if(!noEyeBlinking) {
            stopRandomImageAnimation()
            justStopEyeBlinking = true
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroy() {
        saveLastEditText()
        startMicSound?.release()
        errorMicSound?.release()
        mediaRecorder?.release()
        assistant.destroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        stopClassification()
        handler.removeCallbacks(svRunnable)
        imageHandler.removeCallbacks(imageRunnable)
        imageHandler.removeCallbacks(imageRunnable2)
        imageHandler.removeCallbacks(imageRunnable3)
        systemDatabase.close()
        databaseHelper.close()
        databaseImage.close()
        super.onDestroy()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java", ReplaceWith("moveTaskToBack(true)"))
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private val checkAndRefreshTTS = object : Runnable {
        override fun run() {
            if (!ttsFlag) {
                initComplete = false
                // If not active, shut down and reinitialize TTS
                assistant.destroy()
                if (::speechRecognizer.isInitialized) {
                    speechRecognizer.destroy()
                }
                initSpeechRec()
                assistant = AIAssistant(this@MainActivity, this@MainActivity)
                handler.removeCallbacks(this)
            } else {
                ttsFlag = false
                handler.removeCallbacks(this)
            }
        }
    }

    override fun onInitCompleted() {
        runOnUiThread {
            assistant.setUtteranceProgressListener()
            initComplete = true
        }
    }

    override fun onStartSpeech() {
        runOnUiThread {
            buttonMenu.setImageResource(R.drawable.stop_icon)
            MyApp.isSpeaking = true
            ttsFlag = true
            disableButton()
            startImageAnimation()
        }
    }

    override fun onSpeechError(utteranceId: String?, errorCode: Int?) {
        runOnUiThread {
            assistant.speak("Too long to speak. Error Code: $errorCode")
        }
    }

    override fun onSpeechCompleted() {
        runOnUiThread {
            waitCount = 0
            buttonMenu.setImageResource(R.drawable.menu_icon)
            imageLoad = false
            MyApp.isSpeaking = false
            stopImageAnimation()
            MyApp.currentTime = LocalTime.now()
            MyApp.currentDate = LocalDateTime.now()
            val imagePrompt = extractImageGenPrompt(question)
            if (gotQuestion) {
                if(question.trim() == "") {
                    val text = "\uD83D\uDE0A"
                    txtView.text = text
                    shareButton.isVisible = false
                    enableButton()
                } else if((levenshteinDistance(question,"game")<2) || (levenshteinDistance(question,"play game")<3) || (levenshteinDistance(question,"play game.")<3) || (levenshteinDistance(question,"game.")<3)) {
                    if(gemini) {
                        val i = Intent(this@MainActivity, TextGame::class.java)
                        startActivity(i)
                    } else {
                        val errorText = "${MyApp.gptModel} currently does not have game. \n\n" +
                                "Please try ${MyApp.getGPTList(this, 2)} or ${MyApp.getGPTList(this,3)}. \n"
                        txtView.text = errorText
                        handler.post(svRunnable)
                    }
                    shareButton.isVisible = false
                    enableButton()
                } else if((levenshteinDistance(question,"database")<3) || (levenshteinDistance(question,"database.")<3)) {
                    val i = Intent(this@MainActivity, DatabaseManager::class.java)
                    startActivity(i)
                    shareButton.isVisible = false
                    enableButton()
                } else if((levenshteinDistance(question,"image database")<3) || (levenshteinDistance(question,"image database.")<3)) {
                    val i = Intent(this@MainActivity, ImageDatabase::class.java)
                    startActivity(i)
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"image modify")<3||levenshteinDistance(question,"image mod")<3||levenshteinDistance(question,"image modification")<3||levenshteinDistance(question,"image modify.")<3||levenshteinDistance(question,"image mod.")<3||levenshteinDistance(question,"image modification.")<3) {
                    if(gemini || geminiThink || claude || llamaVision || llamaText) {
                        val errorText = "${MyApp.gptModel} currently does not support image modification. \n\n" +
                                "Please try ${MyApp.getGPTList(this, 0)} or ${MyApp.getGPTList(this,1)}. \n" +
                                "**Require your own OpenAI API key."
                        txtView.text = errorText
                        handler.post(svRunnable)
                    } else {
                        MyApp.imageData2 = null
                        val i = Intent(this@MainActivity, ImageMod::class.java)
                        startActivity(i)
                    }
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"select AI")<3||levenshteinDistance(question,"select AI.")<3||levenshteinDistance(question,"select AI character")<3||levenshteinDistance(question,"select AI character.")<3) {
                    val i = Intent(this@MainActivity, SelectAI::class.java)
                    startActivity(i)
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"setting")<3||levenshteinDistance(question,"setting.")<3) {
                    val i = Intent(this@MainActivity, Settings::class.java)
                    startActivity(i)
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"help")<2||question.trim() == "imageGEN"||levenshteinDistance(question,"help.")<2) {
                    txtView.text = spannableString
                    txtView.movementMethod = LinkMovementMethod.getInstance()
                    handler.post(svRunnable)
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"stop")<2||levenshteinDistance(question,"หยุด")<2||levenshteinDistance(question,"stop.")<2) {
                    val text = "Yes, Master."
                    txtView.text = text
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"about")<2||levenshteinDistance(question,"about Wandee")<2||levenshteinDistance(question,"about.")<2||levenshteinDistance(question,"about Wandee.")<2) {
                    txtView.text = Html.fromHtml(aboutText, Html.FROM_HTML_MODE_COMPACT)
                    txtView.movementMethod = LinkMovementMethod.getInstance()
                    handler.post(svRunnable)
                    shareButton.isVisible = false
                    enableButton()
                } else if((levenshteinDistance(question,"check tts")<3)||(levenshteinDistance(question,"check TTS")<3)||(levenshteinDistance(question,"check tts.")<3)||(levenshteinDistance(question,"check TTS.")<3)) {
                    txtView.text=assistant.ttsEngine
                    handler.post(svRunnable)
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"check usage")<3||levenshteinDistance(question,"check usage.")<3) {
                    txtView.text=Html.fromHtml(checkUsage(), Html.FROM_HTML_MODE_COMPACT)
                    handler.post(svRunnable)
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"check time")<3||levenshteinDistance(question,"check time.")<3) {
                    txtView.text=checkTime()
                    handler.post(svRunnable)
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"world update")<3||levenshteinDistance(question,"world update.")<3) {
                    val i = Intent(this@MainActivity, NewsUpdate::class.java)
                    startActivity(i)
                    shareButton.isVisible = false
                    enableButton()
                } else if(levenshteinDistance(question,"text to speech")<3||levenshteinDistance(question,"text to speech mode")<3||levenshteinDistance(question,"text to speech.")<3||levenshteinDistance(question,"text to speech mode.")<3) {
                    val i = Intent(this@MainActivity, OnlyTextToSpeech::class.java)
                    startActivity(i)
                    finish()
                } else if(levenshteinDistance(question,"speech to text")<3||levenshteinDistance(question,"speech to text mode")<3||levenshteinDistance(question,"speech to text.")<3||levenshteinDistance(question,"speech to text mode.")<3) {
                    changeModeSpeechToText()
                } else if((levenshteinDistance(question,"chat mode")<3) || (levenshteinDistance(question,"custom assistant")<3)||(levenshteinDistance(question,"chat mode.")<3) || (levenshteinDistance(question,"custom assistant.")<3)) {
                    val i = if(gemini || geminiThink) {
                        Intent(this@MainActivity, ChatGemini::class.java)
                    } else if(claude) {
                        Intent(this@MainActivity, ChatMode::class.java)
                    } else if(llamaVision || llamaText) {
                        Intent(this@MainActivity, ChatMode::class.java)
                    } else {
                        Intent(this@MainActivity, CustomAssistant::class.java)
                    }
                    startActivity(i)
                    shareButton.isVisible = false
                    enableButton()
                } else if (imagePrompt != null) {
                    promptText = imagePrompt
                    val promptImage = promptText + " (${MyApp.imageSize})"
                    val matchingPrompt = findMatchingPrompt(promptImage)
                    stopImageAnimation()
                    stopRandomImageAnimation()
                    stopBreathingAnimation()
                    imgR.foreground = null
                    imgR.background = null
                    imgR.setImageResource(R.drawable.a02)
                    frameLayout.background = null
                    if (matchingPrompt != null) {
                        val imageData = databaseImage.loadImageForPrompt(promptImage)
                        if (imageData != null) {
                            unselectedFile()
                            unselectedImage()
                            shareButton.isVisible = true
                            selectedImage = imageData
                            attachedPicture = imageData
                            genImageBitmap = BitmapFactory.decodeFile(attachedPicture)
                            MyApp.imagePrompt = promptImage
                            imageLoad = true
                            txtView.text = promptText
                            handler.post(svRunnable)
                            imgR.setImageBitmap(genImageBitmap)
                            enableButton()
                        }
                    } else {
                        startIMGCountTime()
                        handler.post(svRunnable)
                        imgR.setBackgroundResource(R.drawable.waiting_image)
                        buttonMenu.alpha = 0.3f
                        if(gemini || geminiThink || claude || llamaVision || llamaText) {
                            textAPI.crbSafety(this, promptText) { reply ->
                                runOnUiThread {
                                    try {
                                        if (reply != null) {
                                            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                                systemDatabase.replaceAnswer("errorAPI","0")
                                            }
                                            MyApp.checkToken(databaseHelper,textAPI, MyApp.getGPTList(this,9))
                                            if(reply.contains("No. You cannot.")) {
                                                imageGenFail("This request has been blocked by our content filters.")
                                            } else {
                                                imageAPI.flux1Dev(this, reply) { file ->
                                                    runOnUiThread {
                                                        if(file != null) {
                                                            val directory = File(filesDir, "picture")
                                                            if (!directory.exists()) {
                                                                directory.mkdirs()
                                                            }
                                                            val count = systemDatabase.searchQuestion("image count")
                                                            var imageCount: Long = if(count==null) {
                                                                systemDatabase.insertQuestion("image count","0")
                                                                0
                                                            } else {
                                                                try {
                                                                    count.toLong()
                                                                } catch (e: NumberFormatException) {
                                                                    0
                                                                }
                                                            }
                                                            val newFile = File(directory, "$imageCount.png")
                                                            imageCount++
                                                            systemDatabase.replaceAnswer("image count",imageCount.toString())
                                                            try {
                                                                // Copy file to newFile
                                                                FileInputStream(file).use { input ->
                                                                    FileOutputStream(newFile).use { output ->
                                                                        input.copyTo(output)
                                                                    }
                                                                }
                                                                if(databaseImage.insertPromptAndImagePathToDB(promptImage, newFile.absolutePath)) {
                                                                    val genTime = "**$success**\n\n*Image generation time: ${waitCount / 10.0f} s*"
                                                                    txtView.text = Html.fromHtml(MyApp.makeBoxCodes(genTime), Html.FROM_HTML_MODE_COMPACT)
                                                                    val imageData = databaseImage.loadImageForPrompt(promptImage)
                                                                    if (imageData != null) {
                                                                        shareButton.isVisible = true
                                                                        selectedImage = imageData
                                                                        attachedPicture = imageData
                                                                        genImageBitmap = BitmapFactory.decodeFile(attachedPicture)
                                                                        MyApp.imagePrompt = promptImage
                                                                        imageLoad = true
                                                                        imgR.setImageBitmap(genImageBitmap)
                                                                    }
                                                                    handler.post(svRunnable)
                                                                    if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                                        showNotification(success)
                                                                    }
                                                                    stopIMGCountTime()
                                                                    enableButton()
                                                                } else {
                                                                    imageGenFail("Error during saving image file.")
                                                                }
                                                            } catch (e: IOException) {
                                                                imageGenFail(e.message)
                                                            }
                                                        } else {
                                                            imageGenFail(imageAPI.error)
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            imageGenFail(textAPI.error)
                                            errorCount()
                                        }
                                    } catch (e: Exception) {
                                        outputTextException(e)
                                    }
                                }
                            }
                        } else {
                            imageAPI.imageGeneration(promptText) { reply ->
                                runOnUiThread {
                                    try {
                                        if (reply != null) {
                                            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                                systemDatabase.replaceAnswer("errorAPI","0")
                                            }
                                            databaseImage.saveImageToDatabase(promptImage, reply) { reply ->
                                                runOnUiThread {
                                                    if(reply == success) {
                                                        val genTime = "**$reply**\n\n*Image generation time: ${waitCount / 10.0f} s*"
                                                        txtView.text = Html.fromHtml(MyApp.makeBoxCodes(genTime), Html.FROM_HTML_MODE_COMPACT)
                                                        val imageData = databaseImage.loadImageForPrompt(promptImage)
                                                        if (imageData != null) {
                                                            shareButton.isVisible = true
                                                            selectedImage = imageData
                                                            attachedPicture = imageData
                                                            genImageBitmap = BitmapFactory.decodeFile(attachedPicture)
                                                            MyApp.imagePrompt = promptImage
                                                            imageLoad = true
                                                            imgR.setImageBitmap(genImageBitmap)
                                                        }
                                                    } else {
                                                        val text = "Image size is too big to save to database. If you want to save this image then please download image file from below link.\n\n $reply"
                                                        val spannableText = SpannableString(text)
                                                        val clickableSpan = object : ClickableSpan() {
                                                            override fun onClick(view: View) {
                                                                openLinkInBrowser(reply)
                                                            }
                                                        }
                                                        // Apply the ClickableSpan to the link in the text
                                                        val start = text.indexOf(reply)
                                                        val end = start + reply.length
                                                        spannableText.setSpan(clickableSpan, start, end, 0)
                                                        // Set the SpannableString as the text of the TextView
                                                        txtView.text = spannableText
                                                        // Enable the TextView to handle clickable links
                                                        txtView.movementMethod = LinkMovementMethod.getInstance()
                                                    }
                                                    handler.post(svRunnable)
                                                    if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                        showNotification(success)
                                                    }
                                                    stopIMGCountTime()
                                                    enableButton()
                                                }
                                            }
                                        } else {
                                            imageGenFail(imageAPI.error)
                                            errorCount()
                                        }
                                    } catch (e: Exception) {
                                        imageGenFail(e.message)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val trimQuestion = question.trim()
                    if(alreadySelectedImage) {
                        stopBreathingAnimation()
                        startImageThinkingAnimation()
                        startCountTime()
                        buttonMenu.alpha = 0.3f
                        if(gemini || geminiThink) {
                            textAPI.fileG(this, trimQuestion,base64Image!!,imageType!!) { reply ->
                                runOnUiThread {
                                    try {
                                        if (reply != null) {
                                            outputTextSuccess()
                                            successVisionReply(reply)
                                        } else {
                                            outputTextError()
                                        }
                                        replyNotification()
                                    } catch (e: Exception) {
                                        outputTextException(e)
                                    }
                                }
                            }
                        } else if(claude) {
                            textAPI.claudeVision(base64Image!!,trimQuestion,imageType!!) { reply ->
                                runOnUiThread {
                                    try {
                                        if (reply != null) {
                                            outputTextSuccess()
                                            successVisionReply(reply)
                                        } else {
                                            outputTextError()
                                        }
                                        replyNotification()
                                    } catch (e: Exception) {
                                        outputTextException(e)
                                    }
                                }
                            }
                        } else if(llamaVision) {
                            textAPI.llamaVision(base64Image!!,trimQuestion,imageType!!) { reply ->
                                runOnUiThread {
                                    try {
                                        if (reply != null) {
                                            outputTextSuccess()
                                            successVisionReply(reply)
                                        } else {
                                            outputTextError()
                                        }
                                        replyNotification()
                                    } catch (e: Exception) {
                                        outputTextException(e)
                                    }
                                }
                            }
                        } else {
                            textAPI.vision(base64Image!!,trimQuestion,imageType!!) { reply ->
                                runOnUiThread {
                                    try {
                                        if (reply != null) {
                                            outputTextSuccess()
                                            successVisionReply(reply)
                                        } else {
                                            outputTextError()
                                        }
                                        replyNotification()
                                    } catch (e: Exception) {
                                        outputTextException(e)
                                    }
                                }
                            }
                        }
                    } else {
                        if(alreadySelectedFile) {
                            stopBreathingAnimation()
                            startImageThinkingAnimation()
                            startCountTime()
                            buttonMenu.alpha = 0.3f
                            if(gemini) {
                                if(videoFilePath != null) {
                                    lifecycleScope.launch {
                                        val uploader = VideoUploader(videoFilePath.toString())
                                        val reply = uploader.uploadAndGenerateContent(trimQuestion, fileType.toString())
                                        if(reply != null) {
                                            outputTextSuccess()
                                            successVideoReply(reply, uploader)
                                        } else {
                                            stopImageThinkingAnimation()
                                            stopCountTime()
                                            frameLayout.setBackgroundResource(MyApp.imageResId)
                                            txtView.text = uploader.error
                                            txtView.movementMethod = LinkMovementMethod.getInstance()
                                            handler.post(svRunnable)
                                            enableButton()
                                            errorCount()
                                        }
                                        replyNotification()
                                    }
                                } else {
                                    textAPI.fileG(this, trimQuestion,base64File!!,fileType!!) { reply ->
                                        runOnUiThread {
                                            try {
                                                if (reply != null) {
                                                    outputTextSuccess()
                                                    successVisionReply(reply)
                                                } else {
                                                    outputTextError()
                                                }
                                                replyNotification()
                                            } catch (e: Exception) {
                                                outputTextException(e)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val questionForAI = trimQuestion + " to " + systemDatabase.searchQuestion("AI")
                            val matchingQuestion = findMatchingQuestion(questionForAI)
                            if (!matchingQuestion.isNullOrEmpty() && storeData) {
                                val reply = databaseHelper.searchQuestion(matchingQuestion)
                                if (reply != null) {
                                    successReply(reply)
                                }
                            } else {
                                stopBreathingAnimation()
                                startImageThinkingAnimation()
                                startCountTime()
                                buttonMenu.alpha = 0.3f
                                if(gemini) {
                                    lifecycleScope.launch {
                                        geminiCall(trimQuestion, questionForAI)
                                    }
                                } else if(geminiThink) {
                                    textAPI.geminiThink(trimQuestion, MyApp.messageList) { reply ->
                                        runOnUiThread {
                                            try {
                                                if (reply != null) {
                                                    outputTextSuccess()
                                                    resetError()
                                                    if(storeData) {
                                                        databaseHelper.insertQuestion(questionForAI, reply)
                                                    }
                                                    successReply(reply)
                                                } else {
                                                    outputTextError()
                                                }
                                                replyNotification()
                                            } catch (e: Exception) {
                                                outputTextException(e)
                                            }
                                        }
                                    }
                                } else if(claude) {
                                    textAPI.claudeChat(trimQuestion, MyApp.messageList) { reply ->
                                        runOnUiThread {
                                            try {
                                                if (reply != null) {
                                                    outputTextSuccess()
                                                    resetError()
                                                    if(storeData) {
                                                        databaseHelper.insertQuestion(questionForAI, reply)
                                                    }
                                                    successReply(reply)
                                                } else {
                                                    outputTextError()
                                                }
                                                replyNotification()
                                            } catch (e: Exception) {
                                                outputTextException(e)
                                            }
                                        }
                                    }
                                } else if(llamaText || llamaVision) {
                                    textAPI.llamaChat(trimQuestion, MyApp.messageList) { reply ->
                                        runOnUiThread {
                                            try {
                                                if (reply != null) {
                                                    outputTextSuccess()
                                                    resetError()
                                                    if(storeData) {
                                                        databaseHelper.insertQuestion(questionForAI, reply)
                                                    }
                                                    successReply(reply)
                                                } else {
                                                    outputTextError()
                                                }
                                                replyNotification()
                                            } catch (e: Exception) {
                                                outputTextException(e)
                                            }
                                        }
                                    }
                                } else {
                                    textAPI.chatOpenAI(trimQuestion, MyApp.messageList) { reply ->
                                        runOnUiThread {
                                            try {
                                                if (reply != null) {
                                                    outputTextSuccess()
                                                    resetError()
                                                    if(storeData) {
                                                        databaseHelper.insertQuestion(questionForAI, reply)
                                                    }
                                                    successReply(reply)
                                                } else {
                                                    outputTextError()
                                                }
                                                replyNotification()
                                            } catch (e: Exception) {
                                                outputTextException(e)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                gotQuestion = false
            } else {
                if(!textMode) {
                    if(whisperMode) {
                        tfStart()
                    } else {
                        startSpeechRecognition()
                    }
                } else {
                    enableButton()
                }
            }
        }
    }

    private fun errorCount() {
        val errorCount = systemDatabase.searchQuestion("errorAPI")
        when(errorCount) {
            "0" -> systemDatabase.replaceAnswer("errorAPI","1")
            "1" -> {
                systemDatabase.replaceAnswer("errorAPI", "2")
                selectGPTModelDialog()
            }
            "2" -> {
                systemDatabase.replaceAnswer("errorAPI", "3")
                selectGPTModelDialog()
            }
            else -> selectGPTModelDialog()
        }
    }

    private fun outputTextError(audio: Boolean = false) {
        stopImageThinkingAnimation()
        stopCountTime()
        frameLayout.setBackgroundResource(MyApp.imageResId)
        txtView.text = if(audio) {
            audioAPI.error
        } else {
            textAPI.error
        }
        txtView.movementMethod = LinkMovementMethod.getInstance()
        handler.post(svRunnable)
        enableButton()
        errorCount()
    }

    private fun outputTextException(e: Exception) {
        stopImageThinkingAnimation()
        stopCountTime()
        frameLayout.setBackgroundResource(MyApp.imageResId)
        txtView.text = e.message
        txtView.movementMethod = LinkMovementMethod.getInstance()
        handler.post(svRunnable)
        enableButton()
        errorCount()
    }

    private fun findMatchingPrompt(prompt: String): String? {
        val allPrompts = getAllPromptsFromDatabase()
        for (existingPrompt in allPrompts) {
            val similarityScore = levenshteinDistance(prompt, existingPrompt)
            if (similarityScore == 0) {
                return existingPrompt
            }
        }
        return null
    }

    private fun getAllPromptsFromDatabase(): List<String> {
        // Retrieve all questions from the database
        // You need to implement this method in your DatabaseHelper class
        // It should query the database and return a list of questions
        return databaseImage.getAllPrompts()
    }

    private fun findMatchingQuestion(question: String): String? {
        val allQuestions = getAllQuestionsFromDatabase()
        val trimQuestion = question.trim()
        for (existingQuestion in allQuestions) {
            val similarityScore = levenshteinDistance(trimQuestion, existingQuestion)
            if (similarityScore == 0) {
                return existingQuestion
            }
        }
        return null
    }

    private fun getAllQuestionsFromDatabase(): List<String> {
        // Retrieve all questions from the database
        // You need to implement this method in your DatabaseHelper class
        // It should query the database and return a list of questions
        return databaseHelper.getAllQuestions()
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {

        return MyApp.levenshteinDistance(str1,str2)
    }

    private fun startImageAnimation() {
        imageIndex = 0
        frameLayout.setBackgroundResource(MyApp.imageResId)
        imgR.setImageResource(MyApp.imageResources[imageIndex])
        imageHandler.post(imageRunnable)
        if(noEyeBlinking) {
            if(!breathing) {
                startBreathingAnimation()
            }
            startRandomImageAnimation()
        }
    }

    private fun stopImageAnimation() {
        imageHandler.removeCallbacks(imageRunnable)
        imgR.setImageResource(MyApp.imageResources[0])
    }

    private fun startImageThinkingAnimation() {
        frameLayout.setBackgroundResource(R.drawable.bg_library)
        imgR.setBackgroundResource(MyApp.imageThinking)
    }

    private fun stopImageThinkingAnimation() {
        imgR.setBackgroundResource(MyApp.imageBreathing[0])
        imageHandler.removeCallbacks(imageRunnable3)
        if(!breathing) {
            startBreathingAnimation()
        }
    }

    private fun startRandomImageAnimation() {
        noEyeBlinking = false
        imageIndex2 = 0
        val randomDelay = animationDelayRange.random()
        val fg = ContextCompat.getDrawable(this@MainActivity,MyApp.imageResources2[0])
        imgR.foreground = fg
        frameLayout.setBackgroundResource(MyApp.imageResId)
        imgR.setImageResource(MyApp.imageResources[0])
        imageHandler.postDelayed(imageRunnable2, randomDelay)
    }

    private fun stopRandomImageAnimation() {
        noEyeBlinking = true
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

    private fun startCountTime() {
        busyFlag = true
        progressBar.isVisible = true
        txtView.post(waitRunnable)
        handler.postDelayed(moveToProgressBar,500L)
    }

    private fun stopCountTime() {
        busyFlag = false
        progressBar.isVisible = false
        txtView.removeCallbacks(waitRunnable)
    }

    private fun startIMGCountTime() {
        unselectedFile()
        unselectedImage()
        genImage = true
        busyFlag = true
        progressBar.isVisible = true
        txtView.post(waitForImage)
        handler.postDelayed(moveToProgressBar,500L)
    }

    private fun stopIMGCountTime() {
        busyFlag = false
        genImage = false
        progressBar.isVisible = false
        txtView.removeCallbacks(waitForImage)
    }

    private fun initSpeechRec() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        var partialResult = ""
        val speechRecListener = SpeechRec(object : RecognitionListener {
            // Implement the necessary callbacks from RecognitionListener
            // You can add additional logic or handle events as per your requirement
            // For example, you can handle the recognized speech results in onResults()
            // or display partial results in onPartialResults()
            // or handle errors in onError()
            override fun onReadyForSpeech(params: Bundle?) {
                micOn=true
                startMicSound?.start()
                partialResult = ""
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                micOn=false
                gotQuestion=true
                if(showMenuFlag) {
                    errorCount=0
                    enableButton()
                    showMenuFlag = false
                } else if (!matches.isNullOrEmpty()) {
                    question=matches[0]
                    errorCount=0
                    assistant.speak("")
                }
            }
            override fun onPartialResults(p0: Bundle?) {
                val matches = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    partialResult += "."
                    txtView.text = partialResult
                    shareButton.isVisible = false
                }
            }
            override fun onError(error: Int) {
                micOn=false
                errorMicSound?.start()
                if(showMenuFlag) {
                    errorCount=0
                    enableButton()
                    showMenuFlag = false
                } else if(error==7&&errorCount<3){
                    errorCount++
                    handler.postDelayed({ startSpeechRecognition() },1000)
                } else {
                    errorCount=0
                    enableButton()
                    Toast.makeText(this@MainActivity,"Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
        speechRecognizer.setRecognitionListener(speechRecListener)
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        speechRecognizer.startListening(intent)
    }

    private fun stopSpeech() {
        speechRecognizer.cancel()
        micOn=false
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        var HTML_TEXT = "htmlText"
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
                val fg = ContextCompat.getDrawable(this@MainActivity,MyApp.imageResources2[imageIndex2])
                imgR.foreground = fg
                imageIndex2++
                imageHandler.postDelayed(this, 30)
            } else {
                imageHandler.removeCallbacks(this)
                val randomDelay = animationDelayRange.random()
                val fg = ContextCompat.getDrawable(this@MainActivity,MyApp.imageResources2[0])
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

    private val waitRunnable = object : Runnable {
        override fun run() {
            waitCount += 1
            val timeWaiting = waitCount/10.0f
            val text = if(textMode) {
                "Thinking... $timeWaiting s"
            } else {
                "$question\nThinking... $timeWaiting s"
            }
            txtView.text = text
            txtView.postDelayed(this, 100)
        }
    }

    private val waitForImage = object : Runnable {
        override fun run() {
            waitCount += 1
            val timeWaiting = waitCount/10.0f
            val timeText = " $timeWaiting s"
            val text = imageGeneratingText+timeText
            txtView.text = text
            txtView.postDelayed(this, 100)
        }
    }

    private fun enableButton() {
        buttonTalk.isEnabled=true
        buttonSend.isEnabled = true
        buttonSelectAI.isEnabled=true
        buttonSettings.isEnabled=true
        buttonSelectImage.isEnabled=true
        buttonSelectAudio.isEnabled=true
        speechToTextButton.isEnabled=true
        edText.isEnabled=true
        edText.alpha = 1.0f
        buttonSend.alpha = 1.0f
        buttonTalk.alpha = 1.0f
        buttonSelectAI.alpha = 1.0f
        buttonSettings.alpha = 1.0f
        buttonSelectImage.alpha = 1.0f
        buttonSelectAudio.alpha = 1.0f
        buttonMenu.alpha = 1.0f
        speechToTextButton.alpha = 1.0f
    }

    private fun disableButton() {
        buttonTalk.isEnabled=false
        buttonSend.isEnabled = false
        buttonSelectAI.isEnabled=false
        buttonSettings.isEnabled=false
        buttonSelectImage.isEnabled = false
        buttonSelectAudio.isEnabled=false
        speechToTextButton.isEnabled=false
        edText.isEnabled=false
        edText.alpha = 0.3f
        buttonSend.alpha = 0.3f
        buttonTalk.alpha = 0.3f
        buttonSelectAI.alpha = 0.3f
        buttonSettings.alpha = 0.3f
        buttonSelectImage.alpha = 0.3f
        buttonSelectAudio.alpha = 0.3f
        speechToTextButton.alpha = 0.3f
        hideKeyboard()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }

    private fun hideKeyboard() {
        isKeyboardVisible = false
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        edText.postDelayed({
            imm.hideSoftInputFromWindow(edText.windowToken, 0)
            scrollView.post { scrollView.smoothScrollTo(0,0) }
        }, 100)
        if(!alreadySelectedImage&&!alreadySelectedFile) {
            closeButton.isVisible = false
        }
    }

    private fun checkAPIkey():Boolean {
        return MyApp.checkAPIkey(this,systemDatabase)
    }

    private fun extractImageGenPrompt(question: String): String? {
        val keyword = "imageGEN"
        val index = question.indexOf(keyword)

        if (index != -1) {
            // Check if the keyword is not at the end of the string
            if (index + keyword.length < question.length) {
                // Extract the text after "imageGEN"
                return question.substring(index + keyword.length).trim()
            }
        }
        // No "imageGEN" found or it's at the end of the string
        return null
    }

    private fun checkFontSize() {
        MyApp.checkFontSize(systemDatabase)
        edText.setTextAppearance(MyApp.textAppearance)
        when(themeNumber) {
            0 -> {
                buttonLayout.setBackgroundResource(R.drawable.light_border)
                edText.setBackgroundResource(R.drawable.light_border)
                editTextView.setBackgroundResource(R.drawable.light_border)
            }
            else -> {
                buttonLayout.setBackgroundResource(R.drawable.dark_border)
                edText.setBackgroundResource(R.drawable.dark_border)
                editTextView.setBackgroundResource(R.drawable.dark_border)
            }
        }
        txtView.setTextAppearance(MyApp.textAppearance)
        val title = "<h2>${resources.getString(R.string.app_name)}</h2>"
        titleView.text = Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT)
    }

    private fun checkLanguage() {
        val packageManager = packageManager
        // Get the app name
        val appName = applicationInfo.loadLabel(packageManager).toString()
        // Get the version name
        val versionName = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
        val link = "https://play.google.com/store/apps/details?id=com.personal.myai"
        val thAbout = "<b>$appName</b><br>Version <b>$versionName</b><br><br>ขอบคุณที่เลือก $appName! ถ้าคุณรู้สึกพึงพอใจ ได้โปรดเขียนรีวิวและให้คะแนน 5 ดาว คำติชมของคุณจะช่วยให้เราปรับปรุงและพัฒนาต่อไป<br><br> $link <br><br>เพื่อรีวิวและให้คะแนน $appName."
        val enAbout = "<b>$appName</b><br>Version <b>$versionName</b><br><br>Thank you for choosing $appName! If you're enjoying it, please consider supporting me by leaving a 5-star review on the Play Store. Your feedback helps me improve and grow. <br><br> $link <br><br> to rate and review $appName."

        val thaiHelpText = "คำสั่งสำหรับ $appName\n\n" +
                "database\nตรวจสอบคำถามก่อนหน้าและคำตอบ หรือลบคำถามกับคำตอบเพื่อให้ได้คำตอบใหม่ครั้งหน้า หรือเปลี่ยนคำตอบใหม่\n\n" +
                "imageGEN your prompt\nสร้างรูปภาพจากข้อความของคุณใน your prompt\nตัวอย่างเช่น: imageGEN black cat\n\n" +
                "image database\nดูรูปภาพเก่าที่ได้มาจากข้อความครั้งก่อน หรือลบข้อความครั้งก่อนเพื่อสร้างรูปภาพใหม่จากข้อความเดิม\n\n" +
                "select AI character\nตัวละครจะกำหนดพรอมต์ของระบบ (บุคลิกภาพหรือรูปแบบการโต้ตอบ)\n\n" +
                "setting\nเปลี่ยนธีม, AI model, พื้นหลังแชท, ขนาดตัวอักษร และภาษา\n\n" +
                "stop\nปิดไมค์\n\n" +
                "text to speech\nแปลงข้อความเป็นคำพูด รองรับฟังก์ชันการอ่านออกเสียงข้อความของ OpenAI หากคุณใช้ API key ของคุณเอง\n\n" +
                "speech to text\nแปลงคำพูดเป็นข้อความ คุณสามารถแปลเป็นภาษาที่คุณต้องการหรือใช้สำหรับการถอดเสียงสดจากภาษาที่คุณเลือก คุณสามารถเลือกโหมดนี้อีกครั้งเพื่อสลับกลับ\n\n" +
                "chat mode\nโหมดแชทธรรมดา\n\n" +
                "check usage\nตรวจสอบการใช้งานของแต่ AI model\n\n" +
                "help\nดูคำสั่งของ $appName\n\n" +
                "about\nเกี่ยวกับ $appName\n\n" +
                "contribute\nสนับสนุน $appName\n\n" +
                "image mod\nแก้ไขรูปภาพตาม prompt\n\n" +
                "custom assistant\nสรุปไฟล์เอกสาร พล็อตกราฟ ค้นหาข้อมูลภายในเอกสาร หรือสร้างเอกสารในรูปแบบ word, excel, csv จากข้อมูลของคุณ\n\n" +
                "game\nเกมแบบข้อความ\n\n" +
                "เคล็ดลับ: เลือกรูปภาพหรือไฟล์แล้ว จะสามารถถามเกี่ยวกับรูปภาพหรือไฟล์นั้นได้ ถ้าจะกลับมาถามปกติที่ไม่เกี่ยวกับรูปภาพหรือไฟล์ให้ กดเครื่องหมาย x ข้างปุ่มส่ง หรือ กดที่รูปนั้นหรือไอค่อนไฟล์ เพื่อเอารูปภาพที่เลือกออก"
        val englishHelpText = "Command for $appName\n\n" +
                "database\nCheck your previous question and answer or delete question to get new answer next time or replace with your answer\n\n" +
                "imageGEN your prompt\nTo generate image base on your prompt\nFor example: imageGEN black cat\n\n" +
                "image database\nCheck your previous image with your prompt or delete your prompt with image to generate new image next time\n\n" +
                "select AI character\nThe character determines the system prompt (the personality or style of interaction)\n\n" +
                "setting\nChange theme, AI model, chat background, font size and language\n\n" +
                "stop\nTurn off mic\n\n" +
                "text to speech\nConvert text into speech. Supports OpenAI's text-to-speech functionality if you provide your own API key.\n\n" +
                "speech to text\nConvert speech into text. You can translate it to your preferred language or use it for live transcription from your chosen language. You can select this mode again to toggle back.\n\n" +
                "chat mode\nNormal chat mode.\n\n" +
                "check usage\nCheck usage of each AI model.\n\n" +
                "help\nCheck Command for $appName\n\n" +
                "about\nAbout $appName\n\n" +
                "contribute\nContribute $appName\n\n" +
                "image mod\nImage modification based on prompt\n\n" +
                "custom assistant\nSummarize document file, plot graph, search information within document or create document in word, excel, csv format from  your information\n\n" +
                "game\nText-based game\n\n" +
                "Tip: Select a photo or file. You can ask about the image or file. If you want to come back and ask a normal question that isn't related to pictures or files. Press the x next to the send button or press the image or file icon to remove selected image or selected file."

        val inspire: Array<String> = resources.getStringArray(R.array.inspire_text)
        inspireText = inspire[languageNumber-1]

        val databaseSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@MainActivity, DatabaseManager::class.java)
                startActivity(intent)
            }
        }
        val imageDatabaseSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@MainActivity, ImageDatabase::class.java)
                startActivity(intent)
            }
        }
        val selectAISpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@MainActivity, SelectAI::class.java)
                startActivity(intent)
            }
        }
        val settingSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@MainActivity, Settings::class.java)
                startActivity(intent)
            }
        }
        val ttsSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@MainActivity, OnlyTextToSpeech::class.java)
                startActivity(intent)
                finish()
            }
        }
        val sttSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                changeModeSpeechToText()
            }
        }
        val chatModeSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = if(gemini || geminiThink) {
                    Intent(this@MainActivity, ChatGemini::class.java)
                } else {
                    Intent(this@MainActivity, ChatMode::class.java)
                }
                startActivity(intent)
            }
        }
        val checkUsageSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                txtView.text = Html.fromHtml(checkUsage(), Html.FROM_HTML_MODE_COMPACT)
                handler.post(svRunnable)
            }
        }
        val aboutSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                txtView.text = Html.fromHtml(aboutText, Html.FROM_HTML_MODE_COMPACT)
                txtView.movementMethod = LinkMovementMethod.getInstance()
                handler.post(svRunnable)
            }
        }
        val imageModSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@MainActivity, ImageMod::class.java)
                startActivity(intent)
            }
        }
        val customAssistantSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@MainActivity, CustomAssistant::class.java)
                startActivity(intent)
            }
        }
        val gameSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@MainActivity, TextGame::class.java)
                startActivity(intent)
            }
        }

        when(languageNumber) {
            1 -> {
                helpText = thaiHelpText
                aboutText = thAbout
                spannableString = SpannableString(helpText)
                if(spannableString.length>606) {
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, 32, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE) //database
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), 132, 141, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE) //imageGen
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), 210, 218, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE) //imageGen
                    spannableString.setSpan(StyleSpan(Typeface.ITALIC), 142, 153, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(StyleSpan(Typeface.ITALIC), 183, 195, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(StyleSpan(Typeface.ITALIC), 219, 229, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(ForegroundColorSpan(getColor(R.color.red)), 142, 153, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(ForegroundColorSpan(getColor(R.color.red)), 183, 195, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(ForegroundColorSpan(getColor(R.color.red)), 219, 229, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(databaseSpan, 24, 32, 0)
                }
            }
            else -> {
                helpText = englishHelpText
                aboutText = enAbout
                spannableString = SpannableString(helpText)
                if(spannableString.length>636) {
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, 31, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE) //database
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), 148, 156, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE) //imageGen
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), 220, 228, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE) //imageGen
                    spannableString.setSpan(StyleSpan(Typeface.ITALIC), 157, 168, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(StyleSpan(Typeface.ITALIC), 195, 207, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(StyleSpan(Typeface.ITALIC), 228, 238, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(ForegroundColorSpan(getColor(R.color.red)), 157, 168, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(ForegroundColorSpan(getColor(R.color.red)), 195, 207, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(ForegroundColorSpan(getColor(R.color.red)), 228, 238, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannableString.setSpan(databaseSpan, 23, 31, 0)
                }
            }
        }
        var i = helpText.indexOf("image database")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+14, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(imageDatabaseSpan, i, i+14, 0)

        i = helpText.indexOf("select AI character")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+19, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(selectAISpan, i, i+19, 0)

        i = helpText.indexOf("setting")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+7, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(settingSpan, i, i+7, 0)

        i = helpText.indexOf("stop")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+4, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        i = helpText.indexOf("text to speech")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+14, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ttsSpan, i, i+14, 0)

        i = helpText.indexOf("speech to text")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+14, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(sttSpan, i, i+14, 0)

        i = helpText.indexOf("chat mode")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+9, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        if(gemini || geminiThink || claude || llamaVision || llamaText) {
            spannableString.setSpan(chatModeSpan, i, i+9, 0)
        }

        i = helpText.indexOf("check usage")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+11, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(checkUsageSpan, i, i+11, 0)

        i = helpText.indexOf("help")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+4, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        i = helpText.indexOf("about")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+5, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(aboutSpan, i, i+5, 0)

        i = helpText.indexOf("image mod")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+9, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        if(!(gemini || geminiThink || claude || llamaVision || llamaText)) {
            spannableString.setSpan(imageModSpan, i, i+9, 0)
        }

        i = helpText.indexOf("custom assistant")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+16, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        if(!(gemini || geminiThink || claude || llamaVision || llamaText)) {
            spannableString.setSpan(customAssistantSpan, i, i+16, 0)
        }

        i = helpText.indexOf("game")
        spannableString.setSpan(StyleSpan(Typeface.BOLD), i, i+4, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        if(gemini) {
            spannableString.setSpan(gameSpan, i, i+4, 0)
        }
    }

    private fun checkImageSize() {
        val imageSize = systemDatabase.searchQuestion("image size")
        MyApp.imageSize = imageSize.toString()
        if(gemini || geminiThink || claude || llamaVision || llamaText) {
            MyApp.imageSize = "1024x1024"
            if(imageSize==null) {
                systemDatabase.insertQuestion("image size", "256x256")
                MyApp.imageModel = "dall-e-2"
            }
        } else if(imageSize==null) {
            systemDatabase.insertQuestion("image size", "256x256")
            MyApp.imageModel = "dall-e-2"
            MyApp.imageSize = "256x256"
        } else if(imageSize=="512x512") {
            MyApp.imageModel = "dall-e-2"
        } else if(imageSize=="1024x1024") {
            MyApp.imageModel = "dall-e-3"
        } else if(imageSize=="1792x1024") {
            MyApp.imageModel = "dall-e-3"
        } else if(imageSize=="1024x1792") {
            MyApp.imageModel = "dall-e-3"
        } else {
            MyApp.imageSize = "256x256"
            MyApp.imageModel = "dall-e-2"
        }
    }

    private fun openLinkInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = url.toUri()
        startActivity(intent)
    }

    private fun showNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "channel_id",
            "Channel Name",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        // Create an Intent to open your app when the notification is clicked
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(message)
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

    private fun showDialogMenu() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setNegativeButton("Copy") { _, _ ->
            val clipboard =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Message", txtView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
        }
        if(speechToText) {
            builder.setPositiveButton("Paste") { _, _ ->
                if(clipboardHelper.getClipboardHistory().isNotEmpty())
                {
                    showPasteDialog()
                } else {
                    Toast.makeText(this,"No clipboard!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun ttsDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Please check text-to-speech setting of your device.")
        builder.setNegativeButton("Speech-to-text") { _, _ ->
            changeModeSpeechToText()
        }
        builder.setNeutralButton("Close") { _, _ ->
            finish()
        }
        builder.setPositiveButton("Chat") { _, _ ->
            val i = if(gemini || geminiThink) {
                Intent(this@MainActivity, ChatGemini::class.java)
            } else if(claude) {
                Intent(this@MainActivity, ChatMode::class.java)
            } else if(llamaVision || llamaText) {
                Intent(this@MainActivity, ChatMode::class.java)
            } else {
                Intent(this@MainActivity, CustomAssistant::class.java)
            }
            startActivity(i)
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun showContextMenu() {
        val wrapper = ContextThemeWrapper(this, R.style.MyMenuItemStyle)
        val popup = PopupMenu(wrapper, buttonTalk)
        if(gemini) {
            popup.menuInflater.inflate(R.menu.main_menu3, popup.menu)
        } else if(claude || geminiThink || llamaVision || llamaText) {
            popup.menuInflater.inflate(R.menu.main_menu2, popup.menu)
        } else {
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        }
        popup.setOnMenuItemClickListener { item ->
            if(gemini) {
                when (item.itemId) {
                    R.id.chat_mode -> {
                        stopSpeech()
                        enableButton()
                        val intent = Intent(this@MainActivity, ChatGemini::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.text_to_speech -> {
                        val intent = Intent(this@MainActivity, OnlyTextToSpeech::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.database -> {
                        val intent = Intent(this@MainActivity, DatabaseManager::class.java)
                        startActivity(intent)
                        enableButton()
                        true
                    }
                    R.id.image_database -> {
                        val i = Intent(this@MainActivity, ImageDatabase::class.java)
                        startActivity(i)
                        enableButton()
                        true
                    }
                    R.id.update_latest -> {
                        val i = Intent(this@MainActivity, NewsUpdate::class.java)
                        startActivity(i)
                        enableButton()
                        true
                    }
                    R.id.game -> {
                        val i = Intent(this@MainActivity, TextGame::class.java)
                        startActivity(i)
                        enableButton()
                        true
                    }
                    R.id.help -> {
                        shareButton.isVisible = false
                        stopSpeech()
                        txtView.text = spannableString
                        txtView.movementMethod = LinkMovementMethod.getInstance()
                        handler.post(svRunnable)
                        enableButton()
                        true
                    }
                    else -> false
                }
            } else if(claude || geminiThink || llamaVision || llamaText) {
                when (item.itemId) {
                    R.id.chat_mode -> {
                        stopSpeech()
                        enableButton()
                        val intent = if(geminiThink) {
                            Intent(this@MainActivity, ChatGemini::class.java)
                        } else {
                            Intent(this@MainActivity, ChatMode::class.java)
                        }
                        startActivity(intent)
                        true
                    }
                    R.id.text_to_speech -> {
                        val intent = Intent(this@MainActivity, OnlyTextToSpeech::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.database -> {
                        val intent = Intent(this@MainActivity, DatabaseManager::class.java)
                        startActivity(intent)
                        enableButton()
                        true
                    }
                    R.id.image_database -> {
                        val i = Intent(this@MainActivity, ImageDatabase::class.java)
                        startActivity(i)
                        enableButton()
                        true
                    }
                    R.id.update_latest -> {
                        val i = Intent(this@MainActivity, NewsUpdate::class.java)
                        startActivity(i)
                        enableButton()
                        true
                    }
                    R.id.help -> {
                        shareButton.isVisible = false
                        stopSpeech()
                        txtView.text = spannableString
                        txtView.movementMethod = LinkMovementMethod.getInstance()
                        handler.post(svRunnable)
                        enableButton()
                        true
                    }
                    else -> false
                }
            } else {
                when (item.itemId) {
                    R.id.custom_assistant -> {
                        stopSpeech()
                        enableButton()
                        val i = Intent(this@MainActivity, CustomAssistant::class.java)
                        startActivity(i)
                        true
                    }
                    R.id.text_to_speech -> {
                        val intent = Intent(this@MainActivity, OnlyTextToSpeech::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.database -> {
                        val intent = Intent(this@MainActivity, DatabaseManager::class.java)
                        startActivity(intent)
                        enableButton()
                        true
                    }
                    R.id.image_database -> {
                        val i = Intent(this@MainActivity, ImageDatabase::class.java)
                        startActivity(i)
                        enableButton()
                        true
                    }
                    R.id.image_mod -> {
                        MyApp.imageData2 = null
                        val i = Intent(this@MainActivity, ImageMod::class.java)
                        startActivity(i)
                        enableButton()
                        true
                    }
                    R.id.update_latest -> {
                        val i = Intent(this@MainActivity, NewsUpdate::class.java)
                        startActivity(i)
                        enableButton()
                        true
                    }
                    R.id.help -> {
                        shareButton.isVisible = false
                        stopSpeech()
                        txtView.text = spannableString
                        txtView.movementMethod = LinkMovementMethod.getInstance()
                        handler.post(svRunnable)
                        enableButton()
                        true
                    }
                    else -> false
                }
            }
        }
        popup.setOnDismissListener {
            showMenuFlag = false
            stopSpeech()
            enableButton()
        }
        popup.show()
    }

    private fun saveLastEditText() {
        if(edText.text.isNullOrEmpty()) {
            if(systemDatabase.searchQuestion("lastEditText")==null) {
                systemDatabase.insertQuestion("lastEditText","")
            } else {
                systemDatabase.replaceAnswer("lastEditText","")
            }
        } else {
            if(systemDatabase.searchQuestion("lastEditText")==null) {
                systemDatabase.insertQuestion("lastEditText",edText.text.toString())
            } else {
                systemDatabase.replaceAnswer("lastEditText",edText.text.toString())
            }
        }
    }

    private fun loadLastEditText() {
        val storedText = systemDatabase.searchQuestion("lastEditText")
        if (!storedText.isNullOrEmpty()) {
            edText.setText(storedText)
        }
    }

    private fun checkUsage():String {
        var text = "<b><u>Summary</u></b><br>"
        for(i in 0 until MyApp.numberOfGPT) {
            if(databaseHelper.searchQuestion("${MyApp.getGPTList(this, i)} total token")!=null) {
                var detail = databaseHelper.searchQuestion("${MyApp.getGPTList(this, i)} total token").toString()
                text += "<br><b>${MyApp.getGPTList(this, i)}</b><br>Total usage: <i>$detail</i> tokens"
                if(databaseHelper.searchQuestion("${MyApp.getGPTList(this, i)} total input token")!=null) {
                    detail = databaseHelper.searchQuestion("${MyApp.getGPTList(this, i)} total input token").toString()
                    text += "<br>Total input: <i>$detail</i> tokens"
                }
                if(databaseHelper.searchQuestion("${MyApp.getGPTList(this, i)} total output token")!=null) {
                    detail = databaseHelper.searchQuestion("${MyApp.getGPTList(this, i)} total output token").toString()
                    text += "<br>Total output: <i>$detail</i> tokens<br>"
                }
            }
        }
        return MyApp.checkModel+text
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
        } else if(isAtTop && !isKeyboardVisible) {
            upImage.isVisible = false
            downImage.isVisible = true
        } else if(isAtBottom && !isKeyboardVisible) {
            upImage.isVisible = true
            downImage.isVisible = false
        } else if(!isKeyboardVisible) {
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

    private val svRunnable = object : Runnable {
        override fun run() {
            checkScrollView()
            if(scrollViewHeight > 0 && scrollViewChildHeight > 0) {
                scrollView.fullScroll(View.FOCUS_UP)
                handler.removeCallbacks(this)
            } else {
                handler.post(this)
            }
        }
    }

    private fun containsHtmlDoc(text: String): Boolean {
        return text.contains("</html>") && text.contains("<!DOCTYPE html>")
    }

    private fun loadVisionPicture(uri: Uri) {
        shareButton.isVisible = false
        if(alreadySelectedFile) {
            alreadySelectedFile = false
            base64File = null
            videoFilePath = null
        }
        visionPicture.setImageURI(uri)
        alreadySelectedImage = true
        closeButton.isVisible = true
        txtView.text = fileProcessing.replace("%1","$imageType")
        handler.postDelayed(moveToProgressBar,500L)
    }

    private fun handleImagePickerResult(data: Intent?) {
        if (data != null) {
            val selectedImageUri = data.data ?: return
            // Get the MIME type of the selected image
            imageType = when(selectedImageUri.scheme) {
                "content" -> {
                    contentResolver.getType(selectedImageUri)
                }
                "file" -> {
                    val file = File(selectedImageUri.path.toString())
                    getMimeTypeFromFileName(file.name)
                }
                else -> null
            }

            val inputStream = contentResolver.openInputStream(selectedImageUri)
            base64Image = inputStream?.use {
                Base64.getEncoder().encodeToString(it.readBytes())
            }
            if((base64Image != null) && (imageType != null)) {
                loadVisionPicture(selectedImageUri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            // Handle the result as needed
            handleImagePickerResult(data)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private val moveToProgressBar = Runnable { scrollView.smoothScrollTo(0, progressBar.top) }

    private fun handleFilePickerResult(data: Intent?) {
        if (data != null) {
            val selectedFileUri = data.data ?: return
            var fileName = ""
            fileType = when(selectedFileUri.scheme) {
                "content" -> {
                    try {
                        val cursor = contentResolver.query(selectedFileUri, null, null, null, null)
                        if (cursor != null && cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                fileName = cursor.getString(nameIndex)
                            }
                            cursor.close()
                        }
                    } catch (_: Exception) {}
                    contentResolver.getType(selectedFileUri)
                }
                "file" -> {
                    val file = File(selectedFileUri.path.toString())
                    fileName = file.name
                    getMimeTypeFromFileName(file.name)
                }
                else -> null
            }
            if(fileType.toString().contains("video") && fileName.isNotEmpty()) {
                shareButton.isVisible = false
                try {
                    val directory = File(filesDir, "video")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val videoFile = File(directory, fileName)
                    if (!videoFile.exists()) {
                        contentResolver.openInputStream(selectedFileUri)?.use { inputStream ->
                            FileOutputStream(videoFile).use { outputStream ->
                                inputStream.copyTo(outputStream, bufferSize = 8192) // Optimal buffer size
                            }
                        }
                    }
                    videoFilePath = videoFile.absolutePath
                    if(videoFilePath != null) {
                        txtView.text = fileProcessing.replace("%1","($fileType) : ($fileName) ")
                        visionPicture.setImageResource(R.drawable.video_icon)
                        alreadySelectedFile = true
                        closeButton.isVisible = true
                        base64File = null
                        if(alreadySelectedImage) {
                            alreadySelectedImage = false
                            base64Image = null
                        }
                        handler.postDelayed(moveToProgressBar,500L)
                    }
                } catch (e: IOException) {
                    val error = "Error copying file: ${e.message}"
                    txtView.text = error
                }
            } else if(fileType.toString().contains("audio") && fileName.isNotEmpty()) {
                shareButton.isVisible = false
                try {
                    val directory = File(filesDir, "audio")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val videoFile = File(directory, fileName)
                    if (!videoFile.exists()) {
                        contentResolver.openInputStream(selectedFileUri)?.use { inputStream ->
                            FileOutputStream(videoFile).use { outputStream ->
                                inputStream.copyTo(outputStream, bufferSize = 8192) // Optimal buffer size
                            }
                        }
                    }
                    videoFilePath = videoFile.absolutePath
                    if(videoFilePath != null) {
                        txtView.text = fileProcessing.replace("%1","($fileType) : ($fileName) ")
                        visionPicture.setImageResource(R.drawable.audio_icon)
                        alreadySelectedFile = true
                        closeButton.isVisible = true
                        base64File = null
                        if(alreadySelectedImage) {
                            alreadySelectedImage = false
                            base64Image = null
                        }
                        handler.postDelayed(moveToProgressBar,500L)
                    }
                } catch (e: IOException) {
                    val error = "Error copying file: ${e.message}"
                    txtView.text = error
                }
            } else {
                val inputStream = contentResolver.openInputStream(selectedFileUri)
                base64File = inputStream?.use {
                    Base64.getEncoder().encodeToString(it.readBytes())
                }
                if(base64File != null) {
                    shareButton.isVisible = false
                    val fileSize = base64File!!.length
                    if(fileSize > 20_000_000) {
                        val error = "Error: File size > 20MB\n$fileName: file size: ${fileSize/1_000_000}MB"
                        txtView.text = error
                        alreadySelectedFile = false
                        visionPicture.setImageResource(R.drawable.a02)
                        base64File = null
                        videoFilePath = null
                    } else {
                        txtView.text = fileProcessing.replace("%1","($fileType) : ($fileName) ")
                        alreadySelectedFile = true
                        closeButton.isVisible = true
                        videoFilePath = null
                        visionPicture.setImageResource(R.drawable.file_icon)
                    }
                    if(alreadySelectedImage) {
                        alreadySelectedImage = false
                        base64Image = null
                    }
                    handler.postDelayed(moveToProgressBar,500L)
                }
            }
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            // Handle the result as needed
            handleFilePickerResult(data)
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "file/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "text/*",
                    "application/pdf",
                    "audio/*",
                    "video/*"
                )
            )
        }
        pickFileLauncher.launch(intent)
    }

    private fun getMimeTypeFromFileName(fileName: String): String? {
        return MyApp.getMimeTypeFromFileName(fileName)
    }

    private suspend fun geminiTranslationCall(trimQuestion: String, languageCode: String) {
        val reply = withContext(Dispatchers.IO) {
            textAPI.translationG(trimQuestion, languageCode)
        }

        withContext(Dispatchers.Main) {
            handleTranslationResponse(reply)
        }
    }

    private fun handleTranslationResponse(reply: String?) {
        if (reply != null) {
            translateSuccess(reply)
        } else {
            translateError()
        }
        replyNotification()
    }

    private suspend fun geminiCall(trimQuestion: String, questionForAI: String) {
        val reply = withContext(Dispatchers.IO) {
            textAPI.geminiChat(trimQuestion, MyApp.contentList)
        }

        withContext(Dispatchers.Main) {
            handleGeminiResponse(reply, questionForAI)
        }
    }

    private fun handleGeminiResponse(reply: String?, questionForAI: String) {
        outputTextSuccess()
        if (reply != null) {
            resetError()
            if(storeData) {
                databaseHelper.insertQuestion(questionForAI, reply)
            }
            successReply(reply)
        } else {
            geminiError()
        }
        replyNotification()
    }

    private fun geminiError() {
        frameLayout.setBackgroundResource(MyApp.imageResId)
        txtView.text = textAPI.error
        txtView.movementMethod = LinkMovementMethod.getInstance()
        handler.post(svRunnable)
        enableButton()
        errorCount()
    }

    private fun outputTextSuccess() {
        buttonMenu.alpha = 1.0f
        stopImageThinkingAnimation()
        stopCountTime()
    }

    private fun successReply(reply: String) {
        val replyWithTime = if(waitCount != 0) {
            shareButton.isVisible = true
            "$reply<br><br><i>Text generation time: ${waitCount/10.0f} s</i>"
        } else {
            reply
        }
        val newText = if(containsHtmlDoc(reply)) {
            txtView.text = replyWithTime
            val i = Intent(this@MainActivity, WebSiteView::class.java)
            i.putExtra(HTML_TEXT,reply)
            startActivity(i)
            reply
        } else {
            txtView.text = Html.fromHtml(MyApp.makeBoxCodes(replyWithTime, isDark), Html.FROM_HTML_MODE_COMPACT)
            Html.fromHtml(MyApp.makeBoxCodes(reply, isDark), Html.FROM_HTML_MODE_COMPACT)
        }
        handler.post(svRunnable)

        val userContent = content("user") {
            text(question.trim())
        }
        MyApp.contentList.add(userContent)
        val modelContent = content("model") {
            text(reply)
        }
        MyApp.contentList.add(modelContent)
        if(MyApp.contentList.size > maxMsg) {
            MyApp.contentList.removeAt(0)
            MyApp.contentList.removeAt(0)
        }
        val userMessage = Message(question.trim(),"user")
        MyApp.messageList.add(userMessage)
        val assistantMessage = Message(reply,"assistant")
        MyApp.messageList.add(assistantMessage)
        if(MyApp.messageList.size > maxMsg) {
            MyApp.messageList.removeAt(0)
            MyApp.messageList.removeAt(0)
        }
        expandImage.isVisible = MyApp.messageList.isNotEmpty()

        assistant.speak(newText.toString())
    }

    private fun successVisionReply(reply: String) {
        shareButton.isVisible = true
        resetError()
        val newText = Html.fromHtml(MyApp.makeBoxCodes(reply, isDark), Html.FROM_HTML_MODE_COMPACT)
        val replyWithTime = "$reply<br><br><i>Text generation time: ${waitCount/10.0f} s</i>"
        txtView.text = Html.fromHtml(MyApp.makeBoxCodes(replyWithTime, isDark), Html.FROM_HTML_MODE_COMPACT)
        handler.post(svRunnable)
        assistant.speak(newText.toString())
    }

    private fun successVideoReply(reply: String, videoUploader: VideoUploader) {
        shareButton.isVisible = true
        if(systemDatabase.searchQuestion("errorAPI") != "0") {
            systemDatabase.replaceAnswer("errorAPI","0")
        }
        MyApp.checkTokenVideo(databaseHelper,videoUploader)
        val newText = Html.fromHtml(MyApp.makeBoxCodes(reply, isDark), Html.FROM_HTML_MODE_COMPACT)
        val replyWithTime = "$reply<br><br><i>Text generation time: ${waitCount/10.0f} s</i>"
        txtView.text = Html.fromHtml(MyApp.makeBoxCodes(replyWithTime, isDark), Html.FROM_HTML_MODE_COMPACT)
        handler.post(svRunnable)
        assistant.speak(newText.toString())
    }

    private fun imageGenFail(error: String?) {
        stopIMGCountTime()
        txtView.text = error
        txtView.movementMethod = LinkMovementMethod.getInstance()
        handler.post(svRunnable)
        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
            showNotification("Image generating fail!")
        }
        enableButton()
        startBreathingAnimation()
        startRandomImageAnimation()
    }

    private fun resetError() {
        if(systemDatabase.searchQuestion("errorAPI") != "0") {
            systemDatabase.replaceAnswer("errorAPI","0")
        }
        MyApp.checkToken(databaseHelper,textAPI)
    }

    private fun checkTime(): String {
        val currentMinute:String = if(MyApp.currentTime.minute < 10) {
            "0${MyApp.currentTime.minute}"
        } else {
            MyApp.currentTime.minute.toString()
        }
        return "date: ${MyApp.currentDate.month} ${MyApp.currentDate.dayOfMonth}, ${MyApp.currentDate.year}\ntime: ${MyApp.currentTime.hour}:${currentMinute} O'clock"
    }

    private fun replyNotification() {
        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
            showNotification("You got reply!")
        }
    }

    private fun selectGPTModelDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Select new AI model?")
        builder.setMessage("The current AI model may experience issues due to server problems from the AI provider.")
        val view = layoutInflater.inflate(R.layout.select_assistant, null)
        val gptModelList = view.findViewById<Spinner>(R.id.assistant_list)
        val currentGPT = view.findViewById<TextView>(R.id.instructions)
        val gptModelAdapter:ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item)
        gptModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val gptModelArray = mutableListOf<String>()
        val gptArray = resources.getStringArray(R.array.GPT)
        for(i in gptArray) {
            if(i != MyApp.gptModel) {
                when(i) {
                    gptArray[0] -> {
                        if(MyApp.checkOpenAIAPIkey(systemDatabase)) {
                            if(MyApp.gptModel != gptArray[1]) {
                                gptModelArray.add(i)
                            }
                        }
                    }
                    gptArray[1] -> {
                        if(MyApp.checkOpenAIAPIkey(systemDatabase)) {
                            if(MyApp.gptModel != gptArray[0]) {
                                gptModelArray.add(i)
                            }
                        }
                    }
                    gptArray[2] -> {
                        if(MyApp.gptModel != gptArray[3]) {
                            gptModelArray.add(i)
                        }
                    }
                    gptArray[3] -> {
                        if(MyApp.gptModel != gptArray[2]) {
                            gptModelArray.add(i)
                        }
                    }
                    gptArray[4] -> {
                        if(MyApp.checkClaudeAPIkey(systemDatabase)) {
                            gptModelArray.add(i)
                        }
                    }
                    else -> gptModelArray.add(i)
                }
            }
        }
        gptModelAdapter.addAll(gptModelArray)
        gptModelAdapter.notifyDataSetChanged()
        gptModelList.adapter = gptModelAdapter
        val text = "Unexpected problem on ${MyApp.gptModel}. Please select another AI model."
        currentGPT.text = text
        builder.setView(view)
        builder.setPositiveButton("Select") { _, _ ->
            systemDatabase.replaceAnswer("gpt model", gptModelList.selectedItem.toString())
            val gpt = MyApp.checkGPT(systemDatabase,this)
            gemini = (gpt == 2)
            claude = (gpt == 3)
            llamaVision = (gpt == 4)
            llamaText = (gpt == 5)
            geminiThink = (gpt == 6)
            checkImageSize()
            buttonSelectAudio.isVisible = gemini
            buttonSelectImage.isVisible = !llamaText
            if(!gemini) {
                unselectedFile()
            }
            if(llamaText) {
                unselectedFile()
                unselectedImage()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun unselectedFile() {
        if(alreadySelectedFile && !busyFlag && !MyApp.isSpeaking) {
            alreadySelectedFile = false
            val text = "\uD83D\uDE0A"
            txtView.text = text
            base64File = null
            videoFilePath = null
            visionPicture.setImageResource(R.drawable.a02)
            shareButton.isVisible = false
            closeButton.isVisible = false
            if(speechToText) {
                fromSpinner.isVisible = !gemini
                txtFrom.isVisible = !gemini
                buttonTalk.isVisible = true
            }
        }
    }

    private fun unselectedImage() {
        if(alreadySelectedImage && !busyFlag && !MyApp.isSpeaking) {
            alreadySelectedImage = false
            val text = "\uD83D\uDE0A"
            txtView.text = text
            base64Image = null
            visionPicture.setImageResource(R.drawable.a02)
            shareButton.isVisible = false
            closeButton.isVisible = false
        }
    }

    private fun loadData() {
        MyApp.setAIResource(systemDatabase)
        MyApp.isAppInBackground = false
        loadLastEditText()
        if(!(imageLoad || genImage)) {
            val fg = ContextCompat.getDrawable(this@MainActivity,MyApp.imageResources2[0])
            imgR.foreground = fg
            imgR.setImageResource(MyApp.imageResources[0])
        }
        if(justStopBreathing) {
            startBreathingAnimation()
            justStopBreathing = false
        }
        if(justStopEyeBlinking) {
            startRandomImageAnimation()
            justStopEyeBlinking = false
        }
        if(alreadySelectedImage) {
            closeButton.isVisible = true
        }
        checkFontSize()
        val gpt = MyApp.checkGPT(systemDatabase,this)
        gemini = (gpt == 2)
        claude = (gpt == 3)
        llamaVision = (gpt == 4)
        llamaText = (gpt == 5)
        geminiThink = (gpt == 6)
        checkImageSize()
        MyApp.checkNotification(this,systemDatabase)

        val whisper = systemDatabase.searchQuestion("whisper")
        whisperMode = (whisper == "ON")
        val lgCode: Array<String> = if(whisperMode) {
            resources.getStringArray(R.array.language_code2)
        } else {
            resources.getStringArray(R.array.language_code)
        }
        languageCode = lgCode[languageNumber-1]
        val dataStore = systemDatabase.searchQuestion("store data")
        storeData = (dataStore == "ON")
        checkModeSpeechToText()
    }

    private fun startRecording() {
        isRecord = true
        try {
            recordingFile = File.createTempFile("recording", ".mp3", cacheDir) //  .3gp, .mp4, .aac, .ogg
            val audioSource = if (MediaRecorder.getAudioSourceMax() > MediaRecorder.AudioSource.VOICE_RECOGNITION) MediaRecorder.AudioSource.VOICE_RECOGNITION else MediaRecorder.AudioSource.MIC
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder?.apply {
                setAudioSamplingRate(16000)
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // Or use AAC, AMR
                setAudioEncodingBitRate(96000)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }

            micOn=true
            silenceStartTime = System.currentTimeMillis() // Record start time
        } catch (e: Exception) {
            stopRecording() // Stop if there was any exception.
        }
    }

    private fun stopRecording() {
        isRecord = false
        stopClassification()
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            micOn=false

            // Handle the recorded file here
            val audioFile = recordingFile
            if((audioFile != null) && isSpeech) {
                progressBar.isVisible = true
                if(gemini || geminiThink || claude || llamaVision || llamaText) {
                    audioAPI.transcription(audioFile, languageCode) { reply ->
                        runOnUiThread {
                            try {
                                if (reply != null) {
                                    isSpeech = false
                                    gotQuestion = true
                                    question = reply
                                    assistant.speak("")
                                } else {
                                    outputTextError(true)
                                }
                            } catch (e: Exception) {
                                outputTextException(e)
                            }
                        }
                    }
                } else {
                    audioAPI.transcriptionWhisper(audioFile, languageCode) { reply ->
                        runOnUiThread {
                            try {
                                if (reply != null) {
                                    isSpeech = false
                                    gotQuestion = true
                                    question = reply
                                    assistant.speak("")
                                } else {
                                    outputTextError(true)
                                }
                            } catch (e: Exception) {
                                outputTextException(e)
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this,"Please try again.", Toast.LENGTH_SHORT).show()
                enableButton()
            }
        } catch (e: Exception) {
            isSpeech = false
            mediaRecorder?.release()
            mediaRecorder = null
            micOn=false
            Toast.makeText(this,"Error on stop record", Toast.LENGTH_SHORT).show()
            enableButton()
        }
    }

    private fun tfStart() {
        val classifier = AudioClassifier.createFromFile(this, modelPath)
        val tensor = classifier.createInputTensorAudio()
        val record = classifier.createAudioRecord()
        record.startRecording()
        startRecording()
        startClassification(classifier, tensor, record)
    }

    private fun startClassification(classifier: AudioClassifier, tensor: TensorAudio, record: AudioRecord) {
        if(isRecord) {
            classificationRunnable = object : Runnable {
                override fun run() {
                    if(isRecord) {
                        tensor.load(record)
                        val output = classifier.classify(tensor)

                        val filteredModelOutput = output[0].categories.filter {
                            it.score > probabilityThreshold
                        }
                        val outputStr =
                            filteredModelOutput.sortedBy { -it.score }
                                .joinToString(separator = "\n") { "${it.label} -> ${it.score} " }

                        if (outputStr.contains("Speech")) {
                            isSpeech = true
                            silenceStartTime = System.currentTimeMillis() // Reset timeout
                        } else {
                            if(isSpeech && (System.currentTimeMillis() - silenceStartTime >= 2000)) {
                                record.stop()
                                record.release()
                                handler.removeCallbacks(this)
                                stopRecording()
                            } else if (System.currentTimeMillis() - silenceStartTime >= silenceTimeout) {
                                record.stop()
                                record.release()
                                handler.removeCallbacks(this)
                                stopRecording()
                            }
                        }
                        handler.postDelayed(this, 500)
                    }
                }
            }
            handler.post(classificationRunnable!!)
        } else {
            stopClassification()
        }
    }

    private fun stopClassification() {
        if(classificationRunnable != null) {
            handler.removeCallbacks(classificationRunnable!!)
            classificationRunnable = null
        }
    }

    private fun takeScreenShotAndShare() {
        try {
            disableLayout()
            handler.postDelayed({
                // Take screen shot of rootView
                val bitmap = shareView.drawToBitmap( Bitmap.Config.ARGB_8888)
                // save screenshot and share
                saveAndShare(bitmap)
            },500)
        }
        catch(e : Exception) {
            //Display message when screen shot could not be taken.
            enableLayout()
            Toast.makeText(this,"Error taking screen shot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAndShare(bitmap: Bitmap) {
        // Save bitmap into cache file
        val directory = File(filesDir, "document")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "Wandee AI screenshot.png")
        val fileOutputStream: FileOutputStream?
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            //  Share screenshot
            share(file)
        } catch (e: IOException) {
            enableLayout()
            //Display message when screenshot could not be shared.
            Toast.makeText(this,"Error sharing screen shot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun share(file: File) {
        enableLayout()
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/png"
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(shareIntent, "Share to"))
    }

    private fun disableLayout() {
        nowSharing = true
        val backgroundColor = when(themeNumber) {
            0 -> resources.getColor(R.color.white,null)
            else -> resources.getColor(R.color.black,null)
        }
        if(imageLoad && attachedPicture.isNotEmpty()) {
            visionPicture.setImageBitmap(genImageBitmap)
        }
        shareView.setBackgroundColor(backgroundColor)
        expandImage.isVisible = false
        shareButton.isVisible = false
        titleView.isVisible = true
        qrCode.isVisible = true
    }

    private fun enableLayout() {
        nowSharing = false
        if(imageLoad && attachedPicture.isNotEmpty()) {
            visionPicture.setImageResource(R.drawable.a02)
        }
        shareView.background = null
        expandImage.isVisible = MyApp.messageList.isNotEmpty()
        shareButton.isVisible = true
        titleView.isVisible = false
        qrCode.isVisible = false
    }

    private fun changeModeSpeechToText() {
        shareButton.isVisible = false
        imageLoad = false
        attachedPicture = ""
        unselectedFile()
        speechToText = !speechToText
        unselectedImage()
        txtView.text = null
        hideKeyboard()
        if(!breathing) startBreathingAnimation()
        if(noEyeBlinking) startRandomImageAnimation()
        checkModeSpeechToText()
    }

    private fun checkModeSpeechToText() {
        if(speechToText) {
            fromSpinner.isVisible = !alreadySelectedFile
            toSpinner.isVisible = true
            txtFrom.isVisible = !alreadySelectedFile
            txtTo.isVisible = true
            expandImage.isVisible = false
            edText.isVisible = false
            frameLayout.isVisible = false
            buttonLayout.orientation = LinearLayout.HORIZONTAL
            closeButton.isVisible = false
            buttonSelectAI.isVisible = false
            buttonSend.isVisible = false
            buttonProceedAudio.isVisible = true
            buttonSelectImage.isVisible = false
            buttonSelectAudio.isVisible = true
        } else {
            fromSpinner.isVisible = false
            toSpinner.isVisible = false
            txtFrom.isVisible = false
            txtTo.isVisible = false
            edText.isVisible = true
            expandImage.isVisible = MyApp.messageList.isNotEmpty()
            if(splitScreenMode) {
                frameLayout.isVisible = false
                buttonLayout.orientation = LinearLayout.HORIZONTAL
            } else if(isLandscape) {
                frameLayout.isVisible = true
                buttonLayout.orientation = LinearLayout.HORIZONTAL
            } else if(tabletMode) {
                frameLayout.isVisible = true
                buttonLayout.orientation = LinearLayout.HORIZONTAL
            } else {
                frameLayout.isVisible = true
                buttonLayout.orientation = LinearLayout.VERTICAL
            }
            buttonSelectAI.isVisible = true
            buttonSend.isVisible = true
            buttonProceedAudio.isVisible = false
            buttonSelectImage.isVisible = !llamaText
            buttonSelectAudio.isVisible = gemini
            if(!gemini) {
                unselectedFile()
            }
            if(llamaText) {
                unselectedFile()
                if(!nowSharing) unselectedImage()
            }
        }
    }

    private fun handleAudioPickerResult(data: Intent?) {
        if (data != null) {
            val selectedFileUri = data.data ?: return
            var fileName = ""
            fileType = when(selectedFileUri.scheme) {
                "content" -> {
                    try {
                        val cursor = contentResolver.query(selectedFileUri, null, null, null, null)
                        if (cursor != null && cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                fileName = cursor.getString(nameIndex)
                            }
                            cursor.close()
                        }
                    } catch (_: Exception) {}
                    contentResolver.getType(selectedFileUri)
                }
                "file" -> {
                    val file = File(selectedFileUri.path.toString())
                    fileName = file.name
                    getMimeTypeFromFileName(file.name)
                }
                else -> null
            }
            shareButton.isVisible = false
            if(fileName.isNotEmpty()) {
                try {
                    val directory = File(filesDir, "audio")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val videoFile = File(directory, fileName)
                    if (!videoFile.exists()) {
                        contentResolver.openInputStream(selectedFileUri)?.use { inputStream ->
                            FileOutputStream(videoFile).use { outputStream ->
                                inputStream.copyTo(outputStream, bufferSize = 8192) // Optimal buffer size
                            }
                        }
                    }
                    videoFilePath = videoFile.absolutePath
                    val numBytes = videoFile.length()
                    if(numBytes > 25_000_000) {
                        if(gemini) {
                            selectAudioFile(fileName)
                        } else {
                            val warningText = "$fileName size = ${numBytes/1_000_000}MB > 25MB\nCurrent model cannot support."
                            txtView.text =warningText
                            fromSpinner.isVisible = true
                            txtFrom.isVisible = true
                            buttonTalk.isVisible = true
                            visionPicture.setImageResource(R.drawable.a02)
                            alreadySelectedFile = false
                            base64File = null
                            videoFilePath = null
                        }
                    } else if(numBytes > 20_000_000) {
                        selectAudioFile(fileName)
                    } else {
                        if(gemini) {
                            val inputStream = contentResolver.openInputStream(selectedFileUri)
                            base64File = inputStream?.use {
                                Base64.getEncoder().encodeToString(it.readBytes())
                            }
                            fromSpinner.isVisible = false
                            txtFrom.isVisible = false
                            buttonTalk.isVisible = false
                            val text = "audio file: ($fileName)"
                            txtView.text = text
                            visionPicture.setImageResource(R.drawable.audio_icon)
                            alreadySelectedFile = true
                            videoFilePath = null
                        } else {
                            selectAudioFile(fileName)
                        }
                    }
                } catch (e: IOException) {
                    val error = "Error copying file: ${e.message}"
                    txtView.text = error
                }
            } else {
                Toast.makeText(this@MainActivity,"Oop. Something went wrong.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            // Handle the result as needed
            handleAudioPickerResult(data)
        }
    }

    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
        }
        pickAudioLauncher.launch(intent)
    }

    private fun disableButton2() {
        handler.postDelayed(moveToProgressBar,500L)
        shareButton.isVisible = false
        busyFlag = true
        if(liveMode) txtView.text = null
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        progressBar.isVisible = true
        buttonTalk.isEnabled=false
        buttonProceedAudio.isEnabled = false
        buttonSettings.isEnabled=false
        buttonSelectAudio.isEnabled=false
        speechToTextButton.isEnabled=false
        buttonProceedAudio.alpha = 0.3f
        buttonTalk.alpha = 0.3f
        buttonSettings.alpha = 0.3f
        buttonSelectAudio.alpha = 0.3f
        speechToTextButton.alpha = 0.3f
        fromSpinner.isEnabled = false
        toSpinner.isEnabled = false
        if(!micOn) {
            buttonMenu.isEnabled=false
            buttonMenu.alpha = 0.3f
        }
    }

    private fun enableButton2() {
        handler.postDelayed(moveToProgressBar,500L)
        busyFlag = false
        liveMode = false
        progressBar.isVisible = false
        buttonTalk.isEnabled=true
        buttonProceedAudio.isEnabled = true
        buttonSettings.isEnabled=true
        buttonSelectAudio.isEnabled=true
        buttonMenu.isEnabled=true
        speechToTextButton.isEnabled=true
        buttonProceedAudio.alpha = 1.0f
        buttonTalk.alpha = 1.0f
        buttonSettings.alpha = 1.0f
        buttonSelectAudio.alpha = 1.0f
        buttonMenu.alpha = 1.0f
        speechToTextButton.alpha = 1.0f
        fromSpinner.isEnabled = true
        toSpinner.isEnabled = true
        buttonMenu.setImageResource(R.drawable.menu_icon)
    }

    private fun proceedAudio() {
        val pleaseSelectAudio: Array<String> = resources.getStringArray(R.array.select_audio)
        if(alreadySelectedFile) {
            disableButton2()
            if(gemini) {
                MyApp.checkGeminiAPIkey(systemDatabase)
                val audioPrompt = "Generate a transcript of the speech then translate to \"${toSpinner.selectedItem}\""
                if(videoFilePath != null) {
                    lifecycleScope.launch {
                        val uploader = VideoUploader(videoFilePath.toString())
                        val reply = uploader.uploadAndGenerateContent(audioPrompt, fileType.toString())
                        if(reply != null) {
                            translateSuccess(reply)
                            MyApp.checkTokenVideo(databaseHelper,uploader)
                        } else {
                            txtView.text = uploader.error
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                            handler.post(svRunnable)
                            enableButton2()
                            errorCount()
                        }
                        replyNotification()
                    }
                } else if(base64File != null) {
                    textAPI.fileG(this, audioPrompt,base64File!!,fileType!!) { reply ->
                        runOnUiThread {
                            try {
                                if (reply != null) {
                                    translateSuccess(reply)
                                } else {
                                    translateError()
                                }
                                replyNotification()
                            } catch (e: Exception) {
                                exceptionError(e)
                            }
                        }
                    }
                } else {
                    enableButton2()
                    unselectedFile()
                    Toast.makeText(this@MainActivity,pleaseSelectAudio[languageNumber-1], Toast.LENGTH_SHORT).show()
                }
            } else {
                if(videoFilePath != null) {
                    val audioFile = File(videoFilePath.toString())
                    if(claude || geminiThink || llamaVision || llamaText) {
                        audioAPI.translation(audioFile) { translate ->
                            runOnUiThread {
                                try {
                                    if (translate != null) {
                                        if(claude) {
                                            MyApp.checkClaudeAPIkey(systemDatabase)
                                            textAPI.claudeTranslation(translate, toSpinner.selectedItem.toString()) { reply ->
                                                runOnUiThread {
                                                    try {
                                                        if (reply != null) {
                                                            translateSuccess(reply)
                                                        } else {
                                                            translateError()
                                                        }
                                                        replyNotification()
                                                    } catch (e: Exception) {
                                                        exceptionError(e)
                                                    }
                                                }
                                            }
                                        } else if(geminiThink) {
                                            MyApp.checkGeminiAPIkey(systemDatabase)
                                            textAPI.geminiThinkTranslation(translate, toSpinner.selectedItem.toString()) { reply ->
                                                runOnUiThread {
                                                    try {
                                                        if (reply != null) {
                                                            translateSuccess(reply)
                                                        } else {
                                                            translateError()
                                                        }
                                                        replyNotification()
                                                    } catch (e: Exception) {
                                                        exceptionError(e)
                                                    }
                                                }
                                            }
                                        } else {
                                            textAPI.llamaTranslation(translate, toSpinner.selectedItem.toString()) { reply ->
                                                runOnUiThread {
                                                    try {
                                                        if (reply != null) {
                                                            translateSuccess(reply)
                                                        } else {
                                                            translateError()
                                                        }
                                                        replyNotification()
                                                    } catch (e: Exception) {
                                                        exceptionError(e)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        txtView.text = audioAPI.error
                                        txtView.movementMethod = LinkMovementMethod.getInstance()
                                        handler.post(svRunnable)
                                        enableButton2()
                                        errorCount()
                                    }
                                    replyNotification()
                                } catch (e: Exception) {
                                    exceptionError(e)
                                }
                            }
                        }
                    } else {
                        audioAPI.translationWhisper(audioFile) { translate ->
                            runOnUiThread {
                                try {
                                    if (translate != null) {
                                        MyApp.checkOpenAIAPIkey(systemDatabase)
                                        textAPI.translation(translate, toSpinner.selectedItem.toString()) { reply ->
                                            runOnUiThread {
                                                try {
                                                    if (reply != null) {
                                                        translateSuccess(reply)
                                                    } else {
                                                        translateError()
                                                    }
                                                    replyNotification()
                                                } catch (e: Exception) {
                                                    exceptionError(e)
                                                }
                                            }
                                        }
                                    } else {
                                        txtView.text = audioAPI.error
                                        txtView.movementMethod = LinkMovementMethod.getInstance()
                                        handler.post(svRunnable)
                                        enableButton2()
                                        errorCount()
                                    }
                                    replyNotification()
                                } catch (e: Exception) {
                                    exceptionError(e)
                                }
                            }
                        }
                    }
                } else {
                    enableButton2()
                    unselectedFile()
                    Toast.makeText(this@MainActivity,pleaseSelectAudio[languageNumber-1], Toast.LENGTH_SHORT).show()
                }
            }
        } else if(!txtView.text.isNullOrEmpty()) {
            disableButton2()
            if(gemini) {
                lifecycleScope.launch {
                    MyApp.checkGeminiAPIkey(systemDatabase)
                    geminiTranslationCall(txtView.text.toString(), toSpinner.selectedItem.toString())
                }
            } else if(geminiThink) {
                MyApp.checkGeminiAPIkey(systemDatabase)
                textAPI.geminiThinkTranslation(txtView.text.toString(), toSpinner.selectedItem.toString()) { reply ->
                    runOnUiThread {
                        try {
                            if (reply != null) {
                                translateSuccess(reply)
                            } else {
                                translateError()
                            }
                            replyNotification()
                        } catch (e: Exception) {
                            exceptionError(e)
                        }
                    }
                }
            } else if(claude) {
                MyApp.checkClaudeAPIkey(systemDatabase)
                textAPI.claudeTranslation(txtView.text.toString(), toSpinner.selectedItem.toString()) { reply ->
                    runOnUiThread {
                        try {
                            if (reply != null) {
                                translateSuccess(reply)
                            } else {
                                translateError()
                            }
                            replyNotification()
                        } catch (e: Exception) {
                            exceptionError(e)
                        }
                    }
                }
            } else if(llamaText || llamaVision) {
                textAPI.llamaTranslation(txtView.text.toString(), toSpinner.selectedItem.toString()) { reply ->
                    runOnUiThread {
                        try {
                            if (reply != null) {
                                translateSuccess(reply)
                            } else {
                                translateError()
                            }
                            replyNotification()
                        } catch (e: Exception) {
                            exceptionError(e)
                        }
                    }
                }
            } else {
                MyApp.checkOpenAIAPIkey(systemDatabase)
                textAPI.translation(txtView.text.toString(), toSpinner.selectedItem.toString()) { reply ->
                    runOnUiThread {
                        try {
                            if (reply != null) {
                                translateSuccess(reply)
                            } else {
                                translateError()
                            }
                            replyNotification()
                        } catch (e: Exception) {
                            exceptionError(e)
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this@MainActivity,pleaseSelectAudio[languageNumber-1], Toast.LENGTH_SHORT).show()
        }
    }

    private fun translateSuccess(reply: String) {
        shareButton.isVisible = true
        if(systemDatabase.searchQuestion("errorAPI") != "0") {
            systemDatabase.replaceAnswer("errorAPI","0")
        }
        MyApp.checkToken(databaseHelper,textAPI)
        txtView.text = Html.fromHtml(MyApp.makeBoxCodes(reply, isDark), Html.FROM_HTML_MODE_COMPACT)
        handler.post(svRunnable)
        enableButton2()
    }

    private fun translateError() {
        txtView.text = textAPI.error
        txtView.movementMethod = LinkMovementMethod.getInstance()
        handler.post(svRunnable)
        enableButton2()
        errorCount()
    }

    private fun exceptionError(e: Exception) {
        txtView.text = e.message
        txtView.movementMethod = LinkMovementMethod.getInstance()
        handler.post(svRunnable)
        enableButton2()
    }

    private fun selectAudioFile(fileName: String) {
        fromSpinner.isVisible = false
        txtFrom.isVisible = false
        buttonTalk.isVisible = false
        val text = "audio file: ($fileName)"
        txtView.text = text
        visionPicture.setImageResource(R.drawable.audio_icon)
        alreadySelectedFile = true
        base64File = null
    }

    private fun transcription(file: File, languageCode: String) {
        if(gemini || geminiThink || claude || llamaVision || llamaText) {
            audioAPI.transcription(file, languageCode) { reply ->
                runOnUiThread {
                    try {
                        if (reply != null) {
                            val text = "${txtView.text}\n$reply"
                            txtView.text = text
                            handler.post(svRunnable)
                        } else {
                            val text = "${txtView.text}\n${audioAPI.error}"
                            txtView.text = text
                            handler.post(svRunnable)
                            stopRecordFlag = true
                        }
                    } catch (e: Exception) {
                        val text = "${txtView.text}\n${e.message}"
                        txtView.text = text
                        handler.post(svRunnable)
                        stopRecordFlag = true
                    }
                }
            }
        } else {
            audioAPI.transcriptionWhisper(file, languageCode) { reply ->
                runOnUiThread {
                    try {
                        if (reply != null) {
                            val text = "${txtView.text}\n$reply"
                            txtView.text = text
                            handler.post(svRunnable)
                        } else {
                            val text = "${txtView.text}\n${audioAPI.error}"
                            txtView.text = text
                            handler.post(svRunnable)
                            stopRecordFlag = true
                        }
                    } catch (e: Exception) {
                        val text = "${txtView.text}\n${e.message}"
                        txtView.text = text
                        handler.post(svRunnable)
                        stopRecordFlag = true
                    }
                }
            }
        }
    }

    private fun speechStart1(languageCode: String) {
        val classifier = AudioClassifier.createFromFile(this, modelPath)
        val tensor = classifier.createInputTensorAudio()
        val record1 = classifier.createAudioRecord()
        val mediaRecorder1: MediaRecorder?
        val recordingFile1: File?
        record1.startRecording()
        try {
            recordingFile1 = File.createTempFile("recording1", ".mp3", cacheDir) //  .3gp, .mp4, .aac, .ogg
            val audioSource = if (MediaRecorder.getAudioSourceMax() > MediaRecorder.AudioSource.VOICE_RECOGNITION) MediaRecorder.AudioSource.VOICE_RECOGNITION else MediaRecorder.AudioSource.MIC
            mediaRecorder1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder1.apply {
                setAudioSamplingRate(16000)
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // Or use AAC, AMR
                setAudioEncodingBitRate(96000)
                setOutputFile(recordingFile1.absolutePath)
                prepare()
                start()
            }
            handler.postDelayed({
                tensor.load(record1)
                val output = classifier.classify(tensor)
                val filteredModelOutput = output[0].categories.filter {
                    it.score > probabilityThreshold
                }
                val outputStr =
                    filteredModelOutput.sortedBy { -it.score }
                        .joinToString(separator = "\n") { "${it.label} -> ${it.score} " }

                mediaRecorder1.apply {
                    stop()
                    reset()
                    release()
                }
                record1.apply {
                    stop()
                    release()
                }
                if (outputStr.contains("Speech") && recordingFile1 != null) {
                    transcription(recordingFile1, languageCode)
                }
                if(stopRecordFlag) {
                    micOn=false
                    enableButton2()
                } else {
                    speechStart2(languageCode)
                }
            },recordingDuration)
        } catch (e: Exception) {
            micOn=false
            val text = "${txtView.text}\n${e.message}"
            txtView.text = text
            enableButton2()
        }
    }

    private fun speechStart2(languageCode: String) {
        val classifier = AudioClassifier.createFromFile(this, modelPath)
        val tensor = classifier.createInputTensorAudio()
        val record2 = classifier.createAudioRecord()
        val mediaRecorder2: MediaRecorder?
        val recordingFile2: File?
        record2.startRecording()
        try {
            recordingFile2 = File.createTempFile("recording2", ".mp3", cacheDir) //  .3gp, .mp4, .aac, .ogg
            val audioSource = if (MediaRecorder.getAudioSourceMax() > MediaRecorder.AudioSource.VOICE_RECOGNITION) MediaRecorder.AudioSource.VOICE_RECOGNITION else MediaRecorder.AudioSource.MIC
            mediaRecorder2 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder2.apply {
                setAudioSamplingRate(16000)
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // Or use AAC, AMR
                setAudioEncodingBitRate(96000)
                setOutputFile(recordingFile2.absolutePath)
                prepare()
                start()
            }
            handler.postDelayed({
                tensor.load(record2)
                val output = classifier.classify(tensor)
                val filteredModelOutput = output[0].categories.filter {
                    it.score > probabilityThreshold
                }
                val outputStr =
                    filteredModelOutput.sortedBy { -it.score }
                        .joinToString(separator = "\n") { "${it.label} -> ${it.score} " }

                mediaRecorder2.apply {
                    stop()
                    reset()
                    release()
                }
                record2.apply {
                    stop()
                    release()
                }
                if (outputStr.contains("Speech") && recordingFile2 != null) {
                    transcription(recordingFile2, languageCode)
                }
                if(stopRecordFlag) {
                    micOn=false
                    enableButton2()
                } else {
                    speechStart1(languageCode)
                }
            },recordingDuration)
        } catch (e: Exception) {
            micOn=false
            val text = "${txtView.text}\n${e.message}"
            txtView.text = text
            enableButton2()
        }
    }

    private fun liveTranscription() {
        liveMode = true
        val lgCode: Array<String> = resources.getStringArray(R.array.language_code2)
        micOn = true
        buttonMenu.setImageResource(R.drawable.stop_icon)
        disableButton2()
        stopRecordFlag = false // Reset the flag
        speechStart1(lgCode[fromSpinner.selectedItemPosition])
    }

    private fun showPasteDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Paste clipboard?")
        val view = layoutInflater.inflate(R.layout.select_assistant, null)
        val clipboardList = view.findViewById<Spinner>(R.id.assistant_list)
        val currentClipData = view.findViewById<TextView>(R.id.instructions)
        val clipboardAdapter:ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item)
        clipboardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        clipboardAdapter.addAll(clipboardHelper.getClipboardHistory())
        clipboardList.adapter = clipboardAdapter
        clipboardList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                currentClipData.text = clipboardList.selectedItem.toString()
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing
            }
        }
        builder.setView(view)
        builder.setPositiveButton("Ok") { _, _ ->
            txtView.text = clipboardList.selectedItem.toString()
            shareButton.isVisible = false
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }
}