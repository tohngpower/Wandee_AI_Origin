package com.psn.myai

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Settings : ComponentActivity() {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var txtView: TextView
    private lateinit var gptDetails: TextView
    private lateinit var dallE: TextView
    private lateinit var buttonSave: ImageButton
    private lateinit var buttonSendG: ImageButton
    private lateinit var buttonSendC: ImageButton
    private lateinit var buttonSendO: ImageButton
    private lateinit var buttonSelectImage: ImageButton
    private lateinit var textSizeSpinner: Spinner
    private lateinit var aLanguageSpinner: Spinner
    private lateinit var imageSizeSpinner: Spinner
    private lateinit var gptModelSpinner: Spinner
    private lateinit var switchNotification: SwitchCompat
    private lateinit var switchBackground: SwitchCompat
    private lateinit var switchWhisper: SwitchCompat
    private lateinit var switchStoreData: SwitchCompat
    private lateinit var upImage: ImageView
    private lateinit var downImage: ImageView
    private lateinit var backgroundImage: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var geminiKey: EditText
    private lateinit var claudeKey: EditText
    private lateinit var openAIKey: EditText
    private lateinit var enterMaxMessage: EditText
    private lateinit var geminiLayout: LinearLayout
    private lateinit var claudeLayout: LinearLayout
    private lateinit var openAILayout: LinearLayout
    private lateinit var buttonGuide: Button
    private val systemDatabase = SystemDatabase(this)
    private val databaseHelper = DatabaseHelper(this)
    private var scrollY = 0
    private var scrollViewHeight = 0
    private var scrollViewChildHeight = 0
    private val handler = Handler(Looper.getMainLooper())
    private var notificationStatus = "OFF"
    private val gpt4oMiniDetails = "GPT-4o mini is most cost-efficient small model. Require your own OpenAI API key."
    private val gpt4oDetails = "GPT-4o is most advanced multimodal model. Require your own OpenAI API key."
    private val geminiShareKey = "Most balanced multimodal model. You can get your own Google AI API key at \nhttps://aistudio.google.com/app/apikey"
    private val geminiDetails = "Newest multimodal model. You can get your own Google AI API key at \nhttps://aistudio.google.com/app/apikey"
    private val geminiProDetails = "Reasoning for complex problems, features new thinking capabilities. You can get your own Google AI API key at \nhttps://aistudio.google.com/app/apikey"
    private val claudeDetails = "High performance AI platform built by Anthropic. You can get your own Anthropic API key at \nhttps://console.anthropic.com/settings/keys"
    private val llamaDetails = "Llama Vision is AI multi-model developed by Meta. More details at \nhttps://www.llama.com"
    private val llama31Details = "Llama is AI model developed by Meta. More details at \nhttps://www.llama.com"
    private val typhoonDetails = "Typhoon is optimized for the Thai language. More details at \nhttps://opentyphoon.ai"
    private val deepSeekDetails = "Reasoning models excel at complex problem-solving tasks that require step-by-step analysis. More details at \nhttps://huggingface.co/deepseek-ai/DeepSeek-R1"
    private val dallE2 = "imageGEN Model: DALL·E 2 is optimized for lower cost. Require your own OpenAI API key."
    private val dallE3 = "imageGEN Model: DALL·E 3 is the highest quality model. Require your own OpenAI API key."
    private val flux1Dev = "imageGEN Model: FLUX.1 [dev] support image size 1024x1024. Using this model you agree to non commercial license at \nhttps://huggingface.co/black-forest-labs/FLUX.1-dev/blob/main/LICENSE.md"
    private var geminiShare = true
    private var previousMaxMessage = 0
    private var bgUri: Uri? = null
    private var stringBgUri: String? = null
    private var backgroundStatus = "OFF"
    private val wrongKey = "Wrong format for API key! Please check if you have entered the API key incorrectly."
    private val textAPI = TextAPI()
    private var busy = false
    private var previousAppLanguage: String? = null
    private var whisperStatus = "OFF"
    private var themeNumber = 0
    private var storeDataStatus = "OFF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        contentContainer = findViewById(R.id.settings)
        txtView = findViewById(R.id.textView10)
        gptDetails = findViewById(R.id.textView17)
        dallE = findViewById(R.id.textView18)
        buttonSave = findViewById(R.id.button11)
        buttonSendG = findViewById(R.id.submit_gemini_key)
        buttonSendC = findViewById(R.id.submit_claude_key)
        buttonSendO = findViewById(R.id.submit_openAI_key)
        buttonSelectImage = findViewById(R.id.select_bg)
        textSizeSpinner = findViewById(R.id.textAppearanceSpinner)
        aLanguageSpinner = findViewById(R.id.languageSpinner)
        imageSizeSpinner = findViewById(R.id.imageSpinner)
        gptModelSpinner = findViewById(R.id.gptSpinner)
        switchNotification = findViewById(R.id.notification_sw)
        switchBackground = findViewById(R.id.set_bg)
        switchWhisper = findViewById(R.id.whisper_sw)
        switchStoreData = findViewById(R.id.store_data_sw)
        upImage = findViewById(R.id.upIMG2)
        downImage = findViewById(R.id.downIMG2)
        backgroundImage = findViewById(R.id.bg_img)
        scrollView = findViewById(R.id.SV4)
        geminiLayout = findViewById(R.id.gemini_enable)
        claudeLayout = findViewById(R.id.claude_enable)
        openAILayout = findViewById(R.id.openAI_enable)
        geminiKey = findViewById(R.id.gemini_key)
        claudeKey = findViewById(R.id.claude_key)
        openAIKey = findViewById(R.id.openAI_key)
        enterMaxMessage = findViewById(R.id.max_message)
        buttonGuide = findViewById(R.id.api_key_guide)

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

        val previousDetail = systemDatabase.searchQuestion("max message")
        if(previousDetail != null) {
            val maxMsg = try {
                previousDetail.toInt()
            } catch (e: NumberFormatException) {
                100
            }
            if(maxMsg > 999) {
                val newMaxMessage = 999
                systemDatabase.replaceAnswer("max message","$newMaxMessage")
                var i = maxMsg
                while (i > newMaxMessage) {
                    if(systemDatabase.searchQuestion("message$i") != null) {
                        break
                    } else {
                        i--
                    }
                }
                val diff = i - newMaxMessage
                var j = 1
                if(diff > 0) {
                    while(j <= i) {
                        val newDetail = systemDatabase.searchQuestion("message${j + diff}")
                        if(newDetail != null) {
                            systemDatabase.replaceAnswer("message$j",newDetail)
                        }
                        if(j > newMaxMessage) {
                            systemDatabase.deleteItem("message$j")
                        }
                        j++
                    }
                }
                enterMaxMessage.setText(newMaxMessage)
                previousMaxMessage = newMaxMessage
            } else {
                enterMaxMessage.setText(previousDetail)
                previousMaxMessage = previousDetail.toInt()
            }
        } else {
            systemDatabase.insertQuestion("max message","100")
            enterMaxMessage.setText(getString(R.string.i100))
            previousMaxMessage = 100
        }

        // Create an ArrayAdapter for the Spinner
        val textSizeAdapter = ArrayAdapter.createFromResource(this,R.array.text_appearances,android.R.layout.simple_spinner_item)
        val languageAdapter = ArrayAdapter.createFromResource(this,R.array.text_language,android.R.layout.simple_spinner_item)
        val imageAdapter = ArrayAdapter.createFromResource(this,R.array.image_size,android.R.layout.simple_spinner_item)
        val gptAdapter = ArrayAdapter.createFromResource(this,R.array.GPT,android.R.layout.simple_spinner_item)
        textSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        gptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set the ArrayAdapter on the Spinner
        textSizeSpinner.adapter = textSizeAdapter
        aLanguageSpinner.adapter = languageAdapter
        imageSizeSpinner.adapter = imageAdapter
        gptModelSpinner.adapter = gptAdapter
        textSizeSpinner.setSelection(MyApp.checkFontSize(systemDatabase))
        geminiLayout.isVisible = false
        openAILayout.isVisible = false
        claudeLayout.isVisible = false
        buttonGuide.isVisible = false
        val imageSize = systemDatabase.searchQuestion("image size").toString()
        checkImageSize(imageSize)
        checkGPT()
        val languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        checkLanguage(languageNumber - 1)
        val textAppearance: Array<String> = resources.getStringArray(R.array.text_size_display)
        txtView.text = textAppearance[languageNumber - 1]

        checkNotification()
        checkBackground()
        whisperStatus = systemDatabase.searchQuestion("whisper").toString()
        switchWhisper.isChecked = whisperStatus=="ON"
        storeDataStatus = systemDatabase.searchQuestion("store data").toString()
        switchStoreData.isChecked = storeDataStatus=="ON"

        // Set an OnItemSelectedListener for the Spinner
        textSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                MyApp.textAppearance = when (textSizeSpinner.selectedItem.toString()) {
                    "Medium" -> R.style.MediumTextAppearance
                    "Small" -> R.style.SmallTextAppearance
                    else -> R.style.LargeTextAppearance
                }
                txtView.setTextAppearance(MyApp.textAppearance)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing
            }
        }

        aLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                txtView.text = textAppearance[position]
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing
            }
        }

        imageSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                val text = when(position) {
                    0 -> dallE2
                    1 -> dallE2
                    2 -> dallE3
                    3 -> dallE3
                    else -> dallE3
                }
                dallE.text = text
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing
            }
        }

        gptModelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                val errorMax = systemDatabase.searchQuestion("errorAPI")!!.toInt() >= MyApp.MAX_ERROR
                gptDetails.text = when(position) {
                    0 -> {
                        geminiLayout.isVisible = false
                        claudeLayout.isVisible = false
                        checkImageSize(imageSize)
                        openAILayout.isVisible = if(errorMax) {
                            true
                        } else {
                            !MyApp.checkOpenAIAPIkey(systemDatabase)
                        }
                        gpt4oMiniDetails
                    }
                    1 -> {
                        geminiLayout.isVisible = false
                        claudeLayout.isVisible = false
                        checkImageSize(imageSize)
                        openAILayout.isVisible = if(errorMax) {
                            true
                        } else {
                            !MyApp.checkOpenAIAPIkey(systemDatabase)
                        }
                        gpt4oDetails
                    }
                    3 -> {
                        openAILayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        if(errorMax) {
                            geminiLayout.isVisible = true
                        } else if(MyApp.checkGeminiAPIkey(systemDatabase)) {
                            geminiLayout.isVisible = false
                        } else {
                            geminiLayout.isVisible = true
                        }
                        geminiDetails
                    }
                    4 -> {
                        openAILayout.isVisible = false
                        geminiLayout.isVisible = false
                        flux1dev()
                        claudeLayout.isVisible = if(errorMax) {
                            true
                        } else {
                            !MyApp.checkClaudeAPIkey(systemDatabase)
                        }
                        claudeDetails
                    }
                    5 -> {
                        openAILayout.isVisible = false
                        geminiLayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        llamaDetails
                    }
                    6 -> {
                        openAILayout.isVisible = false
                        geminiLayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        llama31Details
                    }
                    7 -> {
                        openAILayout.isVisible = false
                        geminiLayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        llamaDetails
                    }
                    8 -> {
                        openAILayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        if(errorMax) {
                            geminiLayout.isVisible = true
                        } else if(MyApp.checkGeminiAPIkey(systemDatabase)) {
                            geminiLayout.isVisible = false
                        } else {
                            geminiLayout.isVisible = true
                        }
                        geminiProDetails
                    }
                    9 -> {
                        openAILayout.isVisible = false
                        geminiLayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        llama31Details
                    }
                    10 -> {
                        openAILayout.isVisible = false
                        geminiLayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        typhoonDetails
                    }
                    11 -> {
                        openAILayout.isVisible = false
                        geminiLayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        deepSeekDetails
                    }
                    else -> {
                        openAILayout.isVisible = false
                        claudeLayout.isVisible = false
                        flux1dev()
                        if(errorMax) {
                            geminiLayout.isVisible = true
                        } else if(MyApp.checkGeminiAPIkey(systemDatabase)) {
                            geminiLayout.isVisible = false
                        } else {
                            geminiLayout.isVisible = true
                        }
                        geminiShareKey
                    }
                }
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                buttonGuide.isVisible = openAILayout.isVisible
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing
            }
        }

        switchNotification.setOnCheckedChangeListener { _,_ ->
            val sw = switchNotification.isChecked
            notificationStatus = if (sw) {
                "ON"
            } else {
                "OFF"
            }
        }

        switchBackground.setOnCheckedChangeListener { _,_ ->
            val sw = switchBackground.isChecked
            backgroundStatus = if (sw) {
                buttonSelectImage.isVisible = true
                backgroundImage.isVisible = true
                "ON"
            } else {
                buttonSelectImage.isVisible = false
                backgroundImage.isVisible = false
                "OFF"
            }
        }

        switchWhisper.setOnCheckedChangeListener { _,_ ->
            val sw = switchWhisper.isChecked
            whisperStatus = if (sw) {
                "ON"
            } else {
                "OFF"
            }
        }

        switchStoreData.setOnCheckedChangeListener { _,_ ->
            val sw = switchStoreData.isChecked
            storeDataStatus = if (sw) {
                "ON"
            } else {
                "OFF"
            }
        }

        buttonSave.setOnClickListener {
            onSave()
        }

        buttonSendG.setOnClickListener {
            if(geminiKey.text.isNullOrEmpty()) {
                Toast.makeText(this@Settings,"Please enter your Google AI API key first!", Toast.LENGTH_SHORT).show()
            } else {
                val key = geminiKey.text.toString().trim()
                if(MyApp.isValidText(key)&&(key.length>=39)) {
                    busy = true
                    lifecycleScope.launch {
                        geminiCall("Say test", key)
                    }
                } else {
                    Toast.makeText(this@Settings,wrongKey, Toast.LENGTH_SHORT).show()
                }
            }
        }

        buttonSendO.setOnClickListener {
            if(openAIKey.text.isNullOrEmpty()) {
                Toast.makeText(this@Settings,"Please enter your OpenAI API key first!", Toast.LENGTH_SHORT).show()
            } else {
                val key = openAIKey.text.toString().trim()
                if((MyApp.isValidText(key))&&((key.length==51)||(key.length>=56))) {
                    busy = true
                    textAPI.testAPI(this,"Say test", key) { reply ->
                        runOnUiThread {
                            try {
                                if (reply != null) {
                                    if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                        systemDatabase.replaceAnswer("errorAPI","0")
                                    }
                                    MyApp.checkToken(databaseHelper,textAPI,MyApp.getGPTList(this,0))
                                    Toast.makeText(this,"Success!", Toast.LENGTH_SHORT).show()
                                    systemDatabase.replaceAnswer("apiKEY",key)
                                    openAILayout.isVisible = false
                                    buttonGuide.isVisible = false
                                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                    openAIKey.postDelayed({imm.hideSoftInputFromWindow(openAIKey.windowToken, 0)},100)
                                } else {
                                    Toast.makeText(this,textAPI.error, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this,e.message, Toast.LENGTH_SHORT).show()
                            }
                            busy = false
                        }
                    }
                } else {
                    Toast.makeText(this@Settings,wrongKey, Toast.LENGTH_SHORT).show()
                }
            }
        }

        buttonSendC.setOnClickListener {
            if(claudeKey.text.isNullOrEmpty()) {
                Toast.makeText(this@Settings,"Please enter your Anthropic API key first!", Toast.LENGTH_SHORT).show()
            } else {
                val key = claudeKey.text.toString().trim()
                if(MyApp.isValidText(key)&&(key.length>=39)) {
                    busy = true
                    textAPI.testClaude(this,"Say test", key) { reply ->
                        runOnUiThread {
                            try {
                                if (reply != null) {
                                    if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                        systemDatabase.replaceAnswer("errorAPI","0")
                                    }
                                    MyApp.checkToken(databaseHelper,textAPI,MyApp.getGPTList(this,4))
                                    Toast.makeText(this,"Success!", Toast.LENGTH_SHORT).show()
                                    systemDatabase.replaceAnswer("ClaudeKEY",key)
                                    claudeLayout.isVisible = false
                                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                    claudeKey.postDelayed({imm.hideSoftInputFromWindow(claudeKey.windowToken, 0)},100)
                                } else {
                                    Toast.makeText(this,textAPI.error, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this,e.message, Toast.LENGTH_SHORT).show()
                            }
                            busy = false
                        }
                    }
                } else {
                    Toast.makeText(this@Settings,wrongKey, Toast.LENGTH_SHORT).show()
                }
            }
        }

        buttonSelectImage.setOnClickListener {
            openImagePicker()
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            checkScrollView()
        }

        upImage.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_UP)
        }

        downImage.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }

        buttonGuide.setOnClickListener {
            val i = Intent(this, APIKeyPage::class.java)
            startActivity(i)
        }

        handler.post(svRunnable)
        alphaAnimation(upImage)
        alphaAnimation(downImage)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(busy) {
            moveTaskToBack(true)
        } else if(checkChanging()) {
            saveDialog()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(svRunnable)
        systemDatabase.close()
        databaseHelper.close()
        super.onDestroy()
    }

    private fun checkLanguage(languageNumber: Int) {
        previousAppLanguage = systemDatabase.searchQuestion("language")
        aLanguageSpinner.setSelection(languageNumber)
    }

    private fun checkImageSize(imageSize: String) {
        imageSizeSpinner.isEnabled = true
        val text = when (imageSize) {
            "512x512" -> {
                imageSizeSpinner.setSelection(1)
                dallE2
            }
            "1024x1024" -> {
                imageSizeSpinner.setSelection(2)
                dallE3
            }
            "1792x1024" -> {
                imageSizeSpinner.setSelection(3)
                dallE3
            }
            "1024x1792" -> {
                imageSizeSpinner.setSelection(4)
                dallE3
            }
            else -> {
                imageSizeSpinner.setSelection(0)
                dallE2
            }
        }
        dallE.text = text
    }

    private fun checkNotification() {
        notificationStatus = systemDatabase.searchQuestion("notification").toString()
        switchNotification.isChecked = notificationStatus=="ON"
    }

    private fun checkBackground() {
        val bgOn = systemDatabase.searchQuestion("background switch")
        if(bgOn == null) {
            systemDatabase.insertQuestion("background switch","OFF")
            backgroundStatus = "OFF"
            switchBackground.isChecked = false
        } else {
            backgroundStatus = bgOn
            switchBackground.isChecked = bgOn=="ON"
        }

        if(switchBackground.isChecked) {
            buttonSelectImage.isVisible = true
            backgroundImage.isVisible = true

        } else {
            buttonSelectImage.isVisible = false
            backgroundImage.isVisible = false
        }

        stringBgUri = systemDatabase.searchQuestion("background image")
        if(!stringBgUri.isNullOrEmpty()) {
            val directory = File(filesDir, "picture")
            val file = File(directory, "bg.png")
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if(bitmap != null) backgroundImage.setImageBitmap(bitmap)
        }
    }

    private fun checkGPT() {
        val errorMax = systemDatabase.searchQuestion("errorAPI")!!.toInt() >= MyApp.MAX_ERROR
        var text = geminiShareKey
        geminiShare = if(errorMax) {
            true
        } else if(MyApp.checkGeminiAPIkey(systemDatabase)) {
            false
        } else {
            true
        }
        val gptModel = systemDatabase.searchQuestion("gpt model").toString()
        when (gptModel) {
            MyApp.getGPTList(this, 0) -> {
                gptModelSpinner.setSelection(0)
                openAILayout.isVisible = if(errorMax) {
                    true
                } else {
                    !MyApp.checkOpenAIAPIkey(systemDatabase)
                }
                buttonGuide.isVisible = openAILayout.isVisible
                text = gpt4oMiniDetails
                gptDetails.text = text
            }
            MyApp.getGPTList(this, 1) -> {
                gptModelSpinner.setSelection(1)
                openAILayout.isVisible = if(errorMax) {
                    true
                } else {
                    !MyApp.checkOpenAIAPIkey(systemDatabase)
                }
                buttonGuide.isVisible = openAILayout.isVisible
                text = gpt4oDetails
                gptDetails.text = text
            }
            MyApp.getGPTList(this, 3) -> {
                geminiLayout.isVisible = geminiShare
                gptModelSpinner.setSelection(3)
                text = geminiDetails
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            MyApp.getGPTList(this, 4) -> {
                gptModelSpinner.setSelection(4)
                claudeLayout.isVisible = if(errorMax) {
                    true
                } else {
                    !MyApp.checkClaudeAPIkey(systemDatabase)
                }
                text = claudeDetails
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            MyApp.getGPTList(this, 5) -> {
                gptModelSpinner.setSelection(5)
                text = llamaDetails
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            MyApp.getGPTList(this, 6) -> {
                gptModelSpinner.setSelection(6)
                text = llama31Details
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            MyApp.getGPTList(this, 7) -> {
                gptModelSpinner.setSelection(7)
                text = llamaDetails
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            MyApp.getGPTList(this, 8) -> {
                geminiLayout.isVisible = geminiShare
                gptModelSpinner.setSelection(8)
                text = geminiProDetails
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            MyApp.getGPTList(this, 9) -> {
                gptModelSpinner.setSelection(9)
                text = llama31Details
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            MyApp.getGPTList(this, 10) -> {
                gptModelSpinner.setSelection(10)
                text = typhoonDetails
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            MyApp.getGPTList(this, 11) -> {
                gptModelSpinner.setSelection(11)
                text = deepSeekDetails
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
            else -> {
                geminiLayout.isVisible = geminiShare
                gptModelSpinner.setSelection(2)
                gptDetails.text = text
                gptDetails.movementMethod = LinkMovementMethod.getInstance()
                flux1dev()
            }
        }
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

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.width > 1024) {
            val scale = 1024.toFloat() / bitmap.width
            val matrix = Matrix()
            matrix.setScale(scale, scale)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap // No resizing needed if width is already less than maxWidth
    }

    private fun overwriteImage(uri: Uri) {
        var fileOutputStream:FileOutputStream? = null
        if (uri.scheme == "content") {
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            if (bitmap != null) {
                val resizedBitmap = resizeBitmap(bitmap)
                val directory = File(filesDir, "picture")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, "bg.png")
                try {
                    fileOutputStream = FileOutputStream(file)
                    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                } catch (e: IOException) {
                    Log.e("ImageSaveError", "Error saving image to file: ${e.message}", e)
                } finally {
                    fileOutputStream?.close()
                }
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

    private fun handleImagePickerResult(data: Intent?) {
        if (data != null) {
            bgUri = data.data
            stringBgUri = bgUri.toString()
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(bgUri!!))
            if (bitmap != null) {
                val resizedBitmap = resizeBitmap(bitmap)
                backgroundImage.setImageBitmap(resizedBitmap)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private suspend fun geminiCall(trimQuestion: String, key: String) {
        val reply = withContext(Dispatchers.IO) {
            textAPI.testGemini(this@Settings, trimQuestion, key)
        }

        withContext(Dispatchers.Main) {
            handleGeminiResponse(reply, key)
        }
    }

    private fun handleGeminiResponse(reply: String?, key: String) {
        if (reply != null) {
            if (systemDatabase.searchQuestion("errorAPI") != "0") {
                systemDatabase.replaceAnswer("errorAPI", "0")
            }
            MyApp.checkToken(databaseHelper,textAPI,MyApp.getGPTList(this,2))
            systemDatabase.replaceAnswer("GeminiKEY",key)
            Toast.makeText(this@Settings,"Success!", Toast.LENGTH_SHORT).show()
            geminiShare = false
            geminiLayout.isVisible = false
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            geminiKey.postDelayed({imm.hideSoftInputFromWindow(geminiKey.windowToken, 0)},100)
        } else {
            Toast.makeText(this@Settings,textAPI.error, Toast.LENGTH_SHORT).show()
        }
        busy = false
    }

    private fun flux1dev() {
        imageSizeSpinner.setSelection(2)
        imageSizeSpinner.isEnabled = false
        dallE.postDelayed({
            dallE.text = flux1Dev
            dallE.movementMethod = LinkMovementMethod.getInstance()
        },100)
    }

    private fun onSave() {
        systemDatabase.replaceAnswer("font size",textSizeSpinner.selectedItem.toString())
        systemDatabase.replaceAnswer("language",aLanguageSpinner.selectedItem.toString())
        if((gptModelSpinner.selectedItemPosition == 0) || (gptModelSpinner.selectedItemPosition == 1)) {
            systemDatabase.replaceAnswer("image size",imageSizeSpinner.selectedItem.toString())
        }
        systemDatabase.replaceAnswer("notification",notificationStatus)
        if((gptModelSpinner.selectedItemPosition == 0) || (gptModelSpinner.selectedItemPosition == 1)) {
            if(MyApp.checkOpenAIAPIkey(systemDatabase)) {
                systemDatabase.replaceAnswer("gpt model",gptModelSpinner.selectedItem.toString())
            }
        } else if(gptModelSpinner.selectedItemPosition == 4) {
            if(MyApp.checkClaudeAPIkey(systemDatabase)) {
                systemDatabase.replaceAnswer("gpt model",gptModelSpinner.selectedItem.toString())
            }
        } else {
            systemDatabase.replaceAnswer("gpt model",gptModelSpinner.selectedItem.toString())
        }
        systemDatabase.replaceAnswer("background switch",backgroundStatus)
        systemDatabase.replaceAnswer("whisper",whisperStatus)
        systemDatabase.replaceAnswer("store data",storeDataStatus)
        val bg = systemDatabase.searchQuestion("background image")
        if(stringBgUri != bg) overwriteImage(bgUri!!)
        if(bg == null) {
            systemDatabase.insertQuestion("background image",stringBgUri.toString())
        } else {
            systemDatabase.replaceAnswer("background image",stringBgUri.toString())
        }
        if(!enterMaxMessage.text.isNullOrEmpty()) {
            val maxMessage = enterMaxMessage.text.toString()
            val newMaxMessage = maxMessage.toInt()
            if((newMaxMessage >= 100) && (previousMaxMessage != newMaxMessage)) {
                systemDatabase.replaceAnswer("max message",maxMessage)
                if(newMaxMessage < previousMaxMessage) {
                    var i = previousMaxMessage
                    while (i > newMaxMessage) {
                        if(systemDatabase.searchQuestion("message$i") != null) {
                            break
                        } else {
                            i--
                        }
                    }
                    val diff = i - newMaxMessage
                    var j = 1
                    if(diff > 0) {
                        while(j <= i) {
                            val newDetail = systemDatabase.searchQuestion("message${j + diff}")
                            if(newDetail != null) {
                                systemDatabase.replaceAnswer("message$j",newDetail)
                            }
                            if(j > newMaxMessage) {
                                systemDatabase.deleteItem("message$j")
                            }
                            j++
                        }
                    }
                }
            }
        }

        if(previousAppLanguage != aLanguageSpinner.selectedItem.toString()) {
            val i = Intent(this, MainActivity::class.java)
            //Change the activity.
            startActivity(i)
        }
        finish()
    }

    private fun checkChanging(): Boolean {
        if(systemDatabase.searchQuestion("font size")==textSizeSpinner.selectedItem.toString())
            if(systemDatabase.searchQuestion("language")==aLanguageSpinner.selectedItem.toString())
                if(systemDatabase.searchQuestion("notification")==notificationStatus)
                    if(systemDatabase.searchQuestion("gpt model")==gptModelSpinner.selectedItem.toString())
                        if(systemDatabase.searchQuestion("background switch")==backgroundStatus)
                            if(systemDatabase.searchQuestion("whisper")==whisperStatus)
                                if(systemDatabase.searchQuestion("store data")==storeDataStatus)
                                    if(systemDatabase.searchQuestion("background image")==stringBgUri)
                                        if((gptModelSpinner.selectedItemPosition == 0) || (gptModelSpinner.selectedItemPosition == 1)) {
                                            if(systemDatabase.searchQuestion("image size")==imageSizeSpinner.selectedItem.toString())
                                                return false
                                        } else {
                                            return false
                                        }

        return true
    }

    private fun saveDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Save new settings?")
        builder.setNegativeButton("OK") { _, _ ->
            onSave()
        }
        builder.setPositiveButton("Cancel") { _, _ ->
            finish()
        }
        val dialog = builder.create()
        dialog.show()
    }
}