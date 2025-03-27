package com.psn.myai

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins

@SuppressLint("SetJavaScriptEnabled")
class APIKeyPage : ComponentActivity(), AIAssistant.AssistantListener {
    private lateinit var contentContainer: LinearLayout
    private lateinit var submitBTN: ImageButton
    private lateinit var ttsBTN: Button
    private lateinit var hidePDF: Button
    private lateinit var hideOpenAI: Button
    private lateinit var homeButton: Button
    private lateinit var apiText : EditText
    private lateinit var apiStatusView: TextView
    private lateinit var apiHelp: TextView
    private val textAPI = TextAPI()
    private val systemDatabase = SystemDatabase(this)
    private val databaseHelper = DatabaseHelper(this)
    private lateinit var websiteView: WebView
    private lateinit var openWeb: WebView
    private lateinit var webSettings: WebSettings
    private lateinit var assistant: AIAssistant
    private lateinit var icon: ImageView
    private var speaking = false
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.apikey_page)

        contentContainer = findViewById(R.id.api_key)
        submitBTN = findViewById(R.id.button7)
        ttsBTN = findViewById(R.id.button15)
        hidePDF = findViewById(R.id.button17)
        hideOpenAI = findViewById(R.id.button18)
        homeButton = findViewById(R.id.button19)
        apiText = findViewById(R.id.editText3)
        apiStatusView = findViewById(R.id.textView6)
        apiHelp = findViewById(R.id.textView5)
        websiteView = findViewById(R.id.webView)
        openWeb = findViewById(R.id.webView2)
        icon = findViewById(R.id.imageIcon)
        assistant = AIAssistant(this, this)

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

        val languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        val helloText: Array<String> = resources.getStringArray(R.array.hello)
        val apiHelpText: Array<String> = resources.getStringArray(R.array.help_api)
        apiHelp.text = apiHelpText[languageNumber-1]

        webSettings = websiteView.settings
        webSettings.javaScriptEnabled = true

        val handler = Handler(mainLooper)
        val runnable = Runnable {
            val params = websiteView.layoutParams
            params.height = websiteView.width * 9
            websiteView.layoutParams = params
        }
        handler.postDelayed(runnable,500)

        val apiGuide = "https://drive.google.com/file/d/1gmJzRCqfCOuORaL6mWyywDdXriToJnbC/view?usp=sharing"

        websiteView.webChromeClient = WebChromeClient()
        websiteView.loadUrl(apiGuide)

        openWeb.settings.javaScriptEnabled = true
        openWeb.webChromeClient = WebChromeClient()
        val url = "https://www.youtube.com/embed/OIV1WodaITw?rel=0&amp;showinfo=0"
        openWeb.loadUrl(url)

        val handler2 = Handler(mainLooper)
        val runnable2 = Runnable {
            val params = openWeb.layoutParams
            params.height = openWeb.width
            openWeb.layoutParams = params
        }
        handler2.postDelayed(runnable2,500)

        apiHelp.setOnClickListener {
            if(speaking) {
                assistant.stopSpeaking()
                speaking = false
            } else {
                assistant.speak(apiHelpText[languageNumber-1])
            }
        }

        hidePDF.setOnClickListener {
            if(websiteView.isVisible) {
                val text = "Show PDF"
                hidePDF.text = text
                websiteView.isVisible = false
            } else {
                val text = "Hide PDF"
                hidePDF.text = text
                websiteView.isVisible = true
            }
        }

        hideOpenAI.setOnClickListener {
            if(openWeb.isVisible) {
                val text = "Show Video"
                hideOpenAI.text = text
                openWeb.isVisible = false
            } else {
                val text = "Hide Video"
                hideOpenAI.text = text
                openWeb.isVisible = true
            }
        }

        submitBTN.setOnClickListener {
            activateAPI()
        }

        ttsBTN.setOnClickListener {
            webSettings.javaScriptEnabled = false
            openWeb.settings.javaScriptEnabled = false
            val i = Intent(this, OnlyTextToSpeech::class.java)
            //Change the activity.
            startActivity(i)
            finish()
        }

        homeButton.setOnClickListener {
            webSettings.javaScriptEnabled = false
            openWeb.settings.javaScriptEnabled = false
            val i = Intent(this, MainActivity::class.java)
            //Change the activity.
            startActivity(i)
            finish()
        }

        icon.setOnClickListener {
            if(speaking) {
                assistant.stopSpeaking()
                speaking = false
            } else {
                assistant.speak(helloText[languageNumber-1])
            }
        }
    }

    override fun onInitCompleted() {
        runOnUiThread {
            assistant.setUtteranceProgressListener()
        }
    }

    override fun onStartSpeech() {
        speaking = true
    }

    override fun onSpeechError(utteranceId: String?, errorCode: Int?) {
        speaking = false
    }

    override fun onSpeechCompleted() {
        speaking = false
    }

    override fun onDestroy() {
        assistant.destroy()
        systemDatabase.close()
        databaseHelper.close()
        super.onDestroy()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if(busy) {
            moveTaskToBack(true)
        } else {
            finish()
        }
    }

    private fun activateAPI() {
        webSettings.javaScriptEnabled = false
        openWeb.settings.javaScriptEnabled = false
        disableButton()
        if(!apiText.text.isNullOrEmpty()) {
            runOnUiThread {
                val key = apiText.text.toString().trim()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                apiText.postDelayed({
                    imm.hideSoftInputFromWindow(apiText.windowToken, 0)
                }, 100)
                if((MyApp.isValidText(key))&&((key.length==51)||(key.length>=56))) {
                    var statusText = "API status: Checking"
                    apiStatusView.text = statusText
                    textAPI.testAPI(this,"Say test", key) { reply ->
                        runOnUiThread {
                            try {
                                if (reply != null) {
                                    if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                        systemDatabase.replaceAnswer("errorAPI","0")
                                    }
                                    MyApp.checkToken(databaseHelper,textAPI,MyApp.getGPTList(this,0))
                                    statusText = "API status: Success"
                                    Toast.makeText(this@APIKeyPage,"Success!", Toast.LENGTH_SHORT).show()
                                    apiStatusView.text = statusText
                                    systemDatabase.replaceAnswer("apiKEY",key)
                                    val i = Intent(this@APIKeyPage, MainActivity::class.java)
                                    //Change the activity.
                                    startActivity(i)
                                    finish()
                                } else {
                                    if(textAPI.error.contains("Error code 401: Incorrect API key")) {
                                        val editKey = key.contains("ulP")||key.contains("dlf")||key.contains("BIb")||key.contains("OI2")||key.contains("Slv")
                                        if(editKey) {
                                            val newKey = key
                                                .replace("ulP", "uIP")
                                                .replace("dlf", "dIf")
                                                .replace("BIb", "Blb")
                                                .replace("OI2", "Ol2")
                                                .replace("Slv", "SIv")
                                            textAPI.testAPI(this,"Say test", newKey) { reply ->
                                                runOnUiThread {
                                                    try {
                                                        if (reply != null) {
                                                            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                                                systemDatabase.replaceAnswer("errorAPI","0")
                                                            }
                                                            MyApp.checkToken(databaseHelper,textAPI,MyApp.getGPTList(this,0))
                                                            statusText = "API status: Success"
                                                            Toast.makeText(this@APIKeyPage,"Success!", Toast.LENGTH_SHORT).show()
                                                            apiStatusView.text = statusText
                                                            systemDatabase.replaceAnswer("apiKEY",newKey)
                                                            val i = Intent(this@APIKeyPage, MainActivity::class.java)
                                                            //Change the activity.
                                                            startActivity(i)
                                                            finish()
                                                        } else {
                                                            webSettings.javaScriptEnabled = true
                                                            openWeb.settings.javaScriptEnabled = true
                                                            apiStatusView.text = textAPI.error
                                                            enableButton()
                                                        }
                                                    } catch (e: Exception) {
                                                        webSettings.javaScriptEnabled = true
                                                        openWeb.settings.javaScriptEnabled = true
                                                        apiStatusView.text = e.toString()
                                                        enableButton()
                                                    }
                                                }
                                            }
                                        } else {
                                            webSettings.javaScriptEnabled = true
                                            openWeb.settings.javaScriptEnabled = true
                                            apiStatusView.text = textAPI.error
                                            enableButton()
                                        }
                                    } else {
                                        webSettings.javaScriptEnabled = true
                                        openWeb.settings.javaScriptEnabled = true
                                        apiStatusView.text = textAPI.error
                                        enableButton()
                                    }
                                }
                            } catch (e: Exception) {
                                webSettings.javaScriptEnabled = true
                                openWeb.settings.javaScriptEnabled = true
                                apiStatusView.text = e.toString()
                                enableButton()
                            }
                        }
                    }
                } else {
                    webSettings.javaScriptEnabled = true
                    openWeb.settings.javaScriptEnabled = true
                    Toast.makeText(this@APIKeyPage,"Wrong format for API key! Please check if you have entered the API key incorrectly.", Toast.LENGTH_SHORT).show()
                    enableButton()
                }
            }
        } else {
            webSettings.javaScriptEnabled = true
            openWeb.settings.javaScriptEnabled = true
            Toast.makeText(this@APIKeyPage,"Enter OpenAI API Key first.", Toast.LENGTH_SHORT).show()
            enableButton()
        }
    }

    private fun enableButton() {
        busy = false
        submitBTN.isEnabled=true
        ttsBTN.isEnabled=true
        homeButton.isEnabled=true
        submitBTN.alpha=1.0f
    }

    private fun disableButton() {
        busy = true
        submitBTN.isEnabled=false
        ttsBTN.isEnabled=false
        homeButton.isEnabled=false
        submitBTN.alpha=0.3f
    }
}