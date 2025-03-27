package com.psn.myai

import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class NewsUpdate : ComponentActivity() {
    private lateinit var contentContainer: ConstraintLayout
    private val systemDatabase = SystemDatabase(this)
    private val databaseHelper = DatabaseHelper(this)
    private lateinit var shareView: LinearLayout
    private lateinit var searchLayout: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var scrollView: ScrollView
    private lateinit var upImage: ImageView
    private lateinit var downImage: ImageView
    private lateinit var txtView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var shareButton: ImageView
    private lateinit var qrCode: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var complete = false
    private var scrollY = 0
    private var scrollViewHeight = 0
    private var scrollViewChildHeight = 0
    private val textAPI = TextAPI()
    private var waitForAnswer = false
    private var gemini = false
    private var claude = false
    private var llama = false
    private var hasKey = true
    private var themeNumber = 0
    private var questionR = ""
    private var previousText = ""
    private var geminiThink = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news_update)

        contentContainer = findViewById(R.id.new_update)
        searchLayout = findViewById(R.id.linearLayout15)
        searchEditText = findViewById(R.id.editText8)
        searchButton = findViewById(R.id.imageButton13)
        scrollView = findViewById(R.id.SV8)
        upImage = findViewById(R.id.upIMG6)
        downImage = findViewById(R.id.downIMG6)
        txtView = findViewById(R.id.textView32)
        progressBar = findViewById(R.id.progressBar)
        shareButton = findViewById(R.id.share_btn2)
        shareView = findViewById(R.id.share_view2)
        qrCode = findViewById(R.id.qr_code)

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

        checkFontSize()
        val gpt = MyApp.checkGPT(systemDatabase,this)
        gemini = (gpt == 2)
        claude = (gpt == 3)
        llama = (gpt == 4) || (gpt == 5)
        geminiThink = (gpt == 6)
        if(gemini || geminiThink) {
            MyApp.checkGeminiAPIkey(systemDatabase)
        } else if(claude) {
            hasKey = MyApp.checkClaudeAPIkey(systemDatabase)
        } else if(llama) {
            hasKey = true
        } else {
            hasKey = MyApp.checkOpenAIAPIkey(systemDatabase)
        }
        MyApp.checkNotification(this,systemDatabase)
        upImage.isVisible = false
        downImage.isVisible = false
        progressBar.isVisible = false
        qrCode.isVisible = false

        val languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        val worldUpdateHint: Array<String> = resources.getStringArray(R.array.world_update_hint)
        searchEditText.hint = worldUpdateHint[languageNumber-1]
        val welcome: Array<String> = resources.getStringArray(R.array.welcome_text)
        txtView.hint = welcome[languageNumber-1]
        val pleaseType: Array<String> = resources.getStringArray(R.array.please_type)
        loadLastText()

        searchButton.setOnClickListener {
            searching(pleaseType[languageNumber - 1])
        }

        upImage.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_UP)
        }

        downImage.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if(complete) {
                handler.removeCallbacks(svRunnable)
            }
            checkScrollView()
        }

        shareButton.setOnClickListener {
            takeScreenShotAndShare()
        }

        handler.post(svRunnable)
        alphaAnimation(upImage)
        alphaAnimation(downImage)
    }

    override fun onDestroy() {
        systemDatabase.close()
        databaseHelper.close()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(waitForAnswer) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        MyApp.isAppInBackground = true
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        MyApp.isAppInBackground = false
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
        val intent = Intent(this, NewsUpdate::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("You got reply!")
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

    private fun checkFontSize() {
        MyApp.checkFontSize(systemDatabase)
        searchEditText.setTextAppearance(MyApp.textAppearance)
        txtView.setTextAppearance(MyApp.textAppearance)
    }

    private fun processingAI(reply: String?, question: String) {
        if(gemini) {
            lifecycleScope.launch {
                geminiCall(question, reply.toString())
            }
        } else if(geminiThink) {
            textAPI.geminiThinkWorldUpdate(question,reply.toString()) { answer ->
                runOnUiThread {
                    try {
                        if(answer != null) {
                            systemDatabase.replaceAnswer("temporary searching result","")
                            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                systemDatabase.replaceAnswer("errorAPI","0")
                            }
                            MyApp.checkToken(databaseHelper,textAPI)
                            txtView.text = Html.fromHtml(MyApp.makeTextStyle(answer), Html.FROM_HTML_MODE_COMPACT)
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                            saveLastText(answer)
                        } else {
                            txtView.text = textAPI.error
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                            when(systemDatabase.searchQuestion("errorAPI")) {
                                "0" -> systemDatabase.replaceAnswer("errorAPI","1")
                                "1" -> systemDatabase.replaceAnswer("errorAPI","2")
                                else -> systemDatabase.replaceAnswer("errorAPI","3")
                            }
                        }
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    } catch (e: Exception) {
                        txtView.text = e.toString()
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    }
                }
            }
        } else if(claude) {
            textAPI.claudeWorldUpdate(question,reply.toString()) { answer ->
                runOnUiThread {
                    try {
                        if(answer != null) {
                            systemDatabase.replaceAnswer("temporary searching result","")
                            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                systemDatabase.replaceAnswer("errorAPI","0")
                            }
                            MyApp.checkToken(databaseHelper,textAPI)
                            txtView.text = Html.fromHtml(MyApp.makeTextStyle(answer), Html.FROM_HTML_MODE_COMPACT)
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                            saveLastText(answer)
                        } else {
                            txtView.text = textAPI.error
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                            when(systemDatabase.searchQuestion("errorAPI")) {
                                "0" -> systemDatabase.replaceAnswer("errorAPI","1")
                                "1" -> systemDatabase.replaceAnswer("errorAPI","2")
                                else -> systemDatabase.replaceAnswer("errorAPI","3")
                            }
                        }
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    } catch (e: Exception) {
                        txtView.text = e.toString()
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    }
                }
            }
        } else if(llama) {
            textAPI.llamaWorldUpdate(question,reply.toString()) { answer ->
                runOnUiThread {
                    try {
                        if(answer != null) {
                            systemDatabase.replaceAnswer("temporary searching result","")
                            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                systemDatabase.replaceAnswer("errorAPI","0")
                            }
                            MyApp.checkToken(databaseHelper,textAPI)
                            txtView.text = Html.fromHtml(MyApp.makeTextStyle(answer), Html.FROM_HTML_MODE_COMPACT)
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                            saveLastText(answer)
                        } else {
                            txtView.text = textAPI.error
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                        }
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    } catch (e: Exception) {
                        txtView.text = e.toString()
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    }
                }
            }
        } else {
            textAPI.worldUpdate(question,reply.toString()) { answer ->
                runOnUiThread {
                    try {
                        if(answer != null) {
                            systemDatabase.replaceAnswer("temporary searching result","")
                            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                systemDatabase.replaceAnswer("errorAPI","0")
                            }
                            MyApp.checkToken(databaseHelper,textAPI)
                            txtView.text = Html.fromHtml(MyApp.makeTextStyle(answer), Html.FROM_HTML_MODE_COMPACT)
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                            saveLastText(answer)
                        } else {
                            txtView.text = textAPI.error
                            txtView.movementMethod = LinkMovementMethod.getInstance()
                            when(systemDatabase.searchQuestion("errorAPI")) {
                                "0" -> systemDatabase.replaceAnswer("errorAPI","1")
                                "1" -> systemDatabase.replaceAnswer("errorAPI","2")
                                else -> systemDatabase.replaceAnswer("errorAPI","3")
                            }
                        }
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    } catch (e: Exception) {
                        txtView.text = e.toString()
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    }
                }
            }
        }
    }

    private fun searching(text: String) {
        if(searchEditText.text.isNullOrEmpty()) {
            Toast.makeText(this@NewsUpdate,text, Toast.LENGTH_SHORT).show()
        } else {
            val question = searchEditText.text.toString().trim()
            if(question.isEmpty()) {
                Toast.makeText(this@NewsUpdate,text, Toast.LENGTH_SHORT).show()
            } else {
                questionR = question
                if(hasKey) {
                    shareButton.isVisible = false
                    waitForAnswer = true
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                    searchButton.isEnabled = false
                    searchButton.alpha = 0.3f
                    progressBar.isVisible = true
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    searchEditText.postDelayed({
                        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
                    }, 100)
                    var tempQuestion = systemDatabase.searchQuestion("temporary searching question")
                    if(tempQuestion.isNullOrEmpty()) {
                        systemDatabase.insertQuestion("temporary searching question",question)
                        tempQuestion = question
                    } else {
                        systemDatabase.replaceAnswer("temporary searching question",question)
                    }
                    val searchingResult = systemDatabase.searchQuestion("temporary searching result")
                    val statusText = "Status: Now searching..."
                    txtView.text = statusText
                    handler.post(svRunnable)
                    if(searchingResult.isNullOrEmpty()) {
                        textAPI.search(question,10) { reply ->
                            runOnUiThread {
                                try {
                                    if (reply != null) {
                                        if(searchingResult == null) {
                                            systemDatabase.insertQuestion("temporary searching result",reply)
                                        } else {
                                            systemDatabase.replaceAnswer("temporary searching result",reply)
                                        }
                                        processingAI(reply, question)
                                    } else {
                                        txtView.text = textAPI.error
                                        enableButton()
                                        MyApp.toggleRapidKey()
                                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                            showNotification()
                                        }
                                    }
                                } catch (e: Exception) {
                                    txtView.text = e.toString()
                                    enableButton()
                                    if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                        showNotification()
                                    }
                                }
                            }
                        }
                    } else {
                        val similarityScore = MyApp.levenshteinDistance(tempQuestion,question)
                        if(similarityScore < 3) {
                            processingAI(searchingResult, question)
                        } else {
                            textAPI.search(question,10) { reply ->
                                runOnUiThread {
                                    try {
                                        if (reply != null) {
                                            systemDatabase.replaceAnswer("temporary searching result",reply)
                                            processingAI(reply, question)
                                        } else {
                                            txtView.text = textAPI.error
                                            enableButton()
                                            MyApp.toggleRapidKey()
                                            if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                showNotification()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        txtView.text = e.toString()
                                        enableButton()
                                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                            showNotification()
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val error = "No valid API key. Please press back to main page then go to settings to enter valid API key."
                    Toast.makeText(this@NewsUpdate,error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveLastText(lastText: String?) {
        if(lastText.isNullOrEmpty()) {
            if(systemDatabase.searchQuestion("lastText")==null) {
                systemDatabase.insertQuestion("lastText","")
            } else {
                systemDatabase.replaceAnswer("lastText","")
            }
        } else {
            if(systemDatabase.searchQuestion("lastText")==null) {
                systemDatabase.insertQuestion("lastText",lastText)
            } else {
                systemDatabase.replaceAnswer("lastText",lastText)
            }
        }
    }

    private fun loadLastText() {
        val storedText = systemDatabase.searchQuestion("lastText")
        if (!storedText.isNullOrEmpty()) {
            txtView.text = Html.fromHtml(MyApp.makeTextStyle(storedText), Html.FROM_HTML_MODE_COMPACT)
            txtView.movementMethod = LinkMovementMethod.getInstance()
        }
        shareButton.isVisible = !txtView.text.isNullOrEmpty()
    }

    private suspend fun geminiCall(question: String, information: String) {
        val reply = withContext(Dispatchers.IO) {
            textAPI.worldUpdateG(question,information)
        }

        withContext(Dispatchers.Main) {
            handleGeminiResponse(reply)
        }
    }

    private fun handleGeminiResponse(reply: String?) {
        if (reply != null) {
            systemDatabase.replaceAnswer("temporary searching result","")
            if (systemDatabase.searchQuestion("errorAPI") != "0") {
                systemDatabase.replaceAnswer("errorAPI", "0")
            }
            txtView.text = Html.fromHtml(MyApp.makeTextStyle(reply), Html.FROM_HTML_MODE_COMPACT)
            txtView.movementMethod = LinkMovementMethod.getInstance()
            saveLastText(reply)
            MyApp.checkToken(databaseHelper,textAPI)
        } else {
            txtView.text = textAPI.error
            txtView.movementMethod = LinkMovementMethod.getInstance()
            when (systemDatabase.searchQuestion("errorAPI")) {
                "0" -> systemDatabase.replaceAnswer("errorAPI", "1")
                "1" -> systemDatabase.replaceAnswer("errorAPI", "2")
                else -> systemDatabase.replaceAnswer("errorAPI", "3")
            }
        }
        enableButton()
        if (MyApp.isAppInBackground && MyApp.notificationFlag) {
            showNotification()
        }
    }

    private fun enableButton() {
        handler.post(svRunnable)
        shareButton.isVisible = !txtView.text.isNullOrEmpty()
        searchButton.isEnabled = true
        searchButton.alpha = 1.0f
        progressBar.isVisible = false
        waitForAnswer = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

        val file = File(directory, "Wandee AI world update.png")
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
        val backgroundColor = when(themeNumber) {
            0 -> resources.getColor(R.color.white,null)
            else -> resources.getColor(R.color.black,null)
        }
        previousText = txtView.text.toString()
        val text = "\n<h2>Wandee World Update</h2>\n$previousText"
        txtView.text = Html.fromHtml(MyApp.makeTextStyle(text), Html.FROM_HTML_MODE_COMPACT)
        shareView.setBackgroundColor(backgroundColor)
        qrCode.isVisible = true
    }

    private fun enableLayout() {
        qrCode.isVisible = false
        txtView.text = Html.fromHtml(MyApp.makeTextStyle(previousText), Html.FROM_HTML_MODE_COMPACT)
        shareView.background = null
    }
}