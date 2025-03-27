package com.psn.myai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

class ChatMode : ComponentActivity(), MessageAdapter.MessageListener {
    private lateinit var contentContainer: RelativeLayout
    private lateinit var recyclerView:RecyclerView
    private lateinit var editText: AutoCompleteTextView
    private lateinit var sendButton: ImageButton
    private lateinit var clearButton: ImageView
    private val textAPI = TextAPI()
    private val imageAPI = ImageAPI()
    private lateinit var messageAdapter: MessageAdapter
    private val systemDatabase = SystemDatabase(this)
    private val databaseHelper = DatabaseHelper(this)
    private val databaseImage = DatabaseImage(this)
    private var messageList: MutableList<Message> = mutableListOf()
    private val sentByMe = "me"
    private val sentByBot = "bot"
    private var waitText = "."
    private var waitForAnswer = false
    private var maxMessage = 100
    private val handler = Handler(Looper.getMainLooper())
    private var gemini = false
    private var themeNumber = 0
    private var claude = false
    private val opacity = 0.9f
    private var languageNumber = 0
    private var llama = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.chat_mode)

        contentContainer = findViewById(R.id.chat_mode)
        clearButton = findViewById(R.id.imageClose2)
        recyclerView=findViewById(R.id.chatView)
        editText=findViewById(R.id.editText5)
        sendButton=findViewById(R.id.imageButton3)

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

        enableButton()
        MyApp.checkAIPersonal(systemDatabase)
        MyApp.checkNotification(this,systemDatabase)
        val gpt = MyApp.checkGPT(systemDatabase,this)
        gemini = (gpt == 2)
        claude = (gpt == 3)
        llama = (gpt == 4) || (gpt == 5)
        MyApp.checkFontSize(systemDatabase)
        languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        editText.setTextAppearance(MyApp.textAppearance)
        checkImageSize()
        val maximumMsg = systemDatabase.searchQuestion("max message")
        maxMessage = if(maximumMsg != null) {
            try {
                val maxMsg = maximumMsg.toInt()
                if(maxMsg > 999) {
                    999
                } else {
                    maxMsg
                }
            } catch (e: NumberFormatException) {
                100
            }
        } else {
            systemDatabase.insertQuestion("max message","100")
            100
        }
        checkBackground()

        messageAdapter= MessageAdapter(messageList,this,this, this)
        recyclerView.adapter=messageAdapter
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true // This ensures new messages appear at the bottom
        recyclerView.layoutManager = llm
        messageList.clear()
        loadMessage()
        loadLastEditText()
        clearButton.isVisible = false

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
                            editText.dropDownVerticalOffset = yOffset - 250
                        }
                    }
                    previousHeight = currentHeight
                }
            }
        )

        val suggestions = listOf("imageGEN ")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, suggestions)
        editText.setAdapter(adapter)
        editText.threshold = 2
        editText.dropDownWidth = 350
        when(themeNumber) {
            0 -> editText.setDropDownBackgroundResource(R.drawable.light_border)
            else  -> editText.setDropDownBackgroundResource(R.drawable.dark_border)
        }

        editText.setOnClickListener {
            clearButton.isVisible = true
        }

        sendButton.setOnClickListener {
            sentToAI()
        }

        clearButton.setOnClickListener {
            if(editText.text.isNotEmpty()) {
                editText.text.clear()
            }
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

    override fun onDestroy() {
        saveLastEditText()
        databaseImage.close()
        databaseHelper.close()
        systemDatabase.close()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(waitForAnswer) {
            moveTaskToBack(true)
        } else {
            MyApp.quotedMessage = ""
            super.onBackPressed()
        }
    }

    private fun sentToAI() {
        if(!editText.text.isNullOrEmpty()) {
            MyApp.currentTime = LocalTime.now()
            MyApp.currentDate = LocalDateTime.now()
            clearButton.isVisible = false
            val question = editText.text.toString().trim()
            addToChat(question,sentByMe)
            saveMessage()
            editText.text=null
            disableButton()
            waitForAnswer = true
            if (extractImageGenPrompt(question)!=null) {
                val promptText = extractImageGenPrompt(question).toString()
                val promptImage = promptText + " (${MyApp.imageSize})"
                val matchingPrompt = findMatchingPrompt(promptImage)
                if (matchingPrompt != null) {
                    handler.removeCallbacks(waitRunnable)
                    removeLastMessage()
                    val imageData = databaseImage.loadImageForPrompt(promptImage)
                    if (imageData != null) {
                        addToChat(imageData,"image")
                    }
                    saveMessage()
                    waitForAnswer = false
                    enableButton()
                    if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                        showNotification()
                    }
                } else {
                    if(gemini || claude || llama) {
                        val errorText = "${MyApp.gptModel} currently does not support image generation. \n\nPlease try ${MyApp.getGPTList(this,0)} or ${MyApp.getGPTList(this,1)}. \n**Require your own OpenAI API key."
                        handler.removeCallbacks(waitRunnable)
                        removeLastMessage()
                        addToChat(errorText,sentByBot)
                        saveMessage()
                        waitForAnswer = false
                        enableButton()
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                    } else {
                        imageAPI.imageGeneration(promptText) { reply ->
                            runOnUiThread {
                                try {
                                    if (reply != null) {
                                        databaseImage.saveImageToDatabase(promptImage, reply) { reply ->
                                            runOnUiThread {
                                                if(reply == "Image generating success!") {
                                                    handler.removeCallbacks(waitRunnable)
                                                    removeLastMessage()
                                                    val imageData = databaseImage.loadImageForPrompt(promptImage)
                                                    if (imageData != null) {
                                                        addToChat(imageData,"image")
                                                    } else {
                                                        addToChat("Failed to load image!",sentByBot)
                                                    }
                                                    saveMessage()
                                                    waitForAnswer = false
                                                    enableButton()
                                                    if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                        showNotification()
                                                    }
                                                } else {
                                                    handler.removeCallbacks(waitRunnable)
                                                    removeLastMessage()
                                                    addToChat("Failed to load image!",sentByBot)
                                                    saveMessage()
                                                    waitForAnswer = false
                                                    enableButton()
                                                    if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                                        showNotification()
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        handler.removeCallbacks(waitRunnable)
                                        removeLastMessage()
                                        addToChat(imageAPI.error,sentByBot)
                                        saveMessage()
                                        waitForAnswer = false
                                        enableButton()
                                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                            showNotification()
                                        }
                                    }
                                } catch (e: Exception) {
                                    handler.removeCallbacks(waitRunnable)
                                    removeLastMessage()
                                    addToChat(e.toString(),sentByBot)
                                    saveMessage()
                                    waitForAnswer = false
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
                if(gemini) {
                    checkGeminiAPIkey()
                    lifecycleScope.launch {
                        geminiCall(MyApp.quotedMessage + question)
                    }
                } else if(claude) {
                    checkClaudeAPIkey()
                    textAPI.claude(MyApp.quotedMessage + question) { reply ->
                        runOnUiThread {
                            try {
                                if (reply != null) {
                                    if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                        systemDatabase.replaceAnswer("errorAPI","0")
                                    }
                                    handler.removeCallbacks(waitRunnable)
                                    removeLastMessage()
                                    addToChat(reply,sentByBot)
                                    saveMessage()
                                    MyApp.checkToken(databaseHelper,textAPI)
                                } else {
                                    handler.removeCallbacks(waitRunnable)
                                    removeLastMessage()
                                    addToChat(textAPI.error,sentByBot)
                                    saveMessage()
                                    when(systemDatabase.searchQuestion("errorAPI")) {
                                        "0" -> systemDatabase.replaceAnswer("errorAPI","1")
                                        "1" -> systemDatabase.replaceAnswer("errorAPI","2")
                                        else -> systemDatabase.replaceAnswer("errorAPI","3")
                                    }
                                }
                                if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                    showNotification()
                                }
                            } catch (e: Exception) {
                                handler.removeCallbacks(waitRunnable)
                                removeLastMessage()
                                addToChat(e.toString(),sentByBot)
                                saveMessage()
                            }
                            waitForAnswer = false
                            enableButton()
                        }
                    }
                } else if(llama) {
                    textAPI.llama(MyApp.quotedMessage + question) { reply ->
                        runOnUiThread {
                            try {
                                if (reply != null) {
                                    if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                        systemDatabase.replaceAnswer("errorAPI","0")
                                    }
                                    handler.removeCallbacks(waitRunnable)
                                    removeLastMessage()
                                    addToChat(reply,sentByBot)
                                    saveMessage()
                                    MyApp.checkToken(databaseHelper,textAPI)
                                } else {
                                    handler.removeCallbacks(waitRunnable)
                                    removeLastMessage()
                                    addToChat(textAPI.error,sentByBot)
                                    saveMessage()
                                }
                                if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                    showNotification()
                                }
                            } catch (e: Exception) {
                                handler.removeCallbacks(waitRunnable)
                                removeLastMessage()
                                addToChat(e.toString(),sentByBot)
                                saveMessage()
                            }
                            waitForAnswer = false
                            enableButton()
                        }
                    }
                } else {
                    if(checkAPIkey()) {
                        textAPI.chatGPT(MyApp.quotedMessage + question) { reply ->
                            runOnUiThread {
                                try {
                                    if (reply != null) {
                                        if(systemDatabase.searchQuestion("errorAPI") != "0") {
                                            systemDatabase.replaceAnswer("errorAPI","0")
                                        }
                                        handler.removeCallbacks(waitRunnable)
                                        removeLastMessage()
                                        addToChat(reply,sentByBot)
                                        saveMessage()
                                        MyApp.checkToken(databaseHelper,textAPI)
                                    } else {
                                        handler.removeCallbacks(waitRunnable)
                                        removeLastMessage()
                                        addToChat(textAPI.error,sentByBot)
                                        saveMessage()
                                        when(systemDatabase.searchQuestion("errorAPI")) {
                                            "0" -> systemDatabase.replaceAnswer("errorAPI","1")
                                            "1" -> systemDatabase.replaceAnswer("errorAPI","2")
                                            else -> systemDatabase.replaceAnswer("errorAPI","3")
                                        }
                                    }
                                    if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                        showNotification()
                                    }
                                } catch (e: Exception) {
                                    handler.removeCallbacks(waitRunnable)
                                    removeLastMessage()
                                    addToChat(e.toString(),sentByBot)
                                    saveMessage()
                                }
                                waitForAnswer = false
                                enableButton()
                            }
                        }
                    }
                }
            }
        } else {
            val pleaseType: Array<String> = resources.getStringArray(R.array.please_type)
            Toast.makeText(this@ChatMode,pleaseType[languageNumber - 1], Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToChat(message:String, sentBy:String) {
        runOnUiThread {
            val newMessage = Message(message, sentBy)
            messageList.add(newMessage)
            messageAdapter.notifyItemInserted(messageList.size)
            recyclerView.smoothScrollToPosition(messageList.size)
        }
    }

    private fun removeLastMessage() {
        runOnUiThread {
            if (messageList.isNotEmpty()) {
                val position = messageList.size - 1
                messageList.removeAt(position)
                messageAdapter.notifyItemRemoved(position)
            }
        }
    }

    private fun replaceMessageAtPosition(position: Int, newMessage: Message) {
        runOnUiThread {
            if (position >= 0 && position < messageList.size) {
                messageList[position] = newMessage
                messageAdapter.notifyItemChanged(position)
                recyclerView.scrollToPosition(position)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        editText.postDelayed({
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
        }, 100)
    }

    private fun disableButton() {
        sendButton.isEnabled=false
        editText.isEnabled=false
        sendButton.alpha = 0.3f
        editText.alpha = 0.3f
        hideKeyboard()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        addToChat(waitText, sentByBot)
        handler.post(waitRunnable)
    }

    private fun enableButton() {
        sendButton.isEnabled=true
        editText.isEnabled=true
        sendButton.alpha = 1.0f
        editText.alpha = 1.0f
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        MyApp.quotedMessage = ""
    }

    private val waitRunnable = object : Runnable {
        override fun run() {
            runOnUiThread {
                val newMessage=Message(waitText,sentByBot)
                replaceMessageAtPosition(messageList.size-1,newMessage)
                waitText += "."
                if (waitText.length > 5) {
                    waitText = "."
                }
                handler.postDelayed(this, 500)
            }
        }
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
        val intent = Intent(this, ChatMode::class.java)
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

    private fun saveLastEditText() {
        if(editText.text.isNullOrEmpty()) {
            if(systemDatabase.searchQuestion("lastEditText2")==null) {
                systemDatabase.insertQuestion("lastEditText2","")
            } else {
                systemDatabase.replaceAnswer("lastEditText2","")
            }
        } else {
            if(systemDatabase.searchQuestion("lastEditText2")==null) {
                systemDatabase.insertQuestion("lastEditText2",editText.text.toString())
            } else {
                systemDatabase.replaceAnswer("lastEditText2",editText.text.toString())
            }
        }
    }

    private fun loadLastEditText() {
        val storedText = systemDatabase.searchQuestion("lastEditText2")
        if (!storedText.isNullOrEmpty()) {
            editText.setText(storedText)
        }
    }

    private fun saveMessage() {
        var i=messageList.size
        if(i <= maxMessage) {
            if(systemDatabase.searchQuestion("message$i")==null) {
                systemDatabase.insertQuestion("message$i","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
            }
        } else {
            if(systemDatabase.searchQuestion("message$i")==null) {
                systemDatabase.insertQuestion("message$i","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
            }
            while (i>1) {
                systemDatabase.replaceAnswer("message${i-1}","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
                i--
            }
            systemDatabase.deleteItem("message${messageList.size}")
            messageList.removeAt(0)
            messageAdapter.notifyItemRemoved(0)
        }
    }

    private fun loadMessage() {
        var i = 0
        while(i < maxMessage) {
            if(systemDatabase.searchQuestion("message${i+1}")!=null) {
                val text = systemDatabase.searchQuestion("message${i+1}").toString()
                val parts = text.split(": ", limit = 2)
                val sentBy = parts[0] // Contains the sender ("bot")
                val message = parts.getOrElse(1) { "" } // Contains the message ("how can I assist you today?")
                addToChat(message,sentBy)
                i++
            } else {
                break
            }
        }
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

    private fun checkImageSize() {
        if(systemDatabase.searchQuestion("image size")==null) {
            systemDatabase.insertQuestion("image size", "256x256")
        } else if(systemDatabase.searchQuestion("image size")=="256x256") {
            MyApp.imageSize = "256x256"
            MyApp.imageModel = "dall-e-2"
        } else if(systemDatabase.searchQuestion("image size")=="512x512") {
            MyApp.imageSize = "512x512"
            MyApp.imageModel = "dall-e-2"
        } else if(systemDatabase.searchQuestion("image size")=="1024x1024") {
            MyApp.imageSize = "1024x1024"
            MyApp.imageModel = "dall-e-3"
        } else if(systemDatabase.searchQuestion("image size")=="1792x1024") {
            MyApp.imageSize = "1792x1024"
            MyApp.imageModel = "dall-e-3"
        } else if(systemDatabase.searchQuestion("image size")=="1024x1792") {
            MyApp.imageSize = "1024x1792"
            MyApp.imageModel = "dall-e-3"
        } else {
            MyApp.imageSize = "256x256"
            MyApp.imageModel = "dall-e-2"
        }
    }

    private fun checkAPIkey():Boolean {
        return MyApp.checkAPIkey(this,systemDatabase)
    }

    private fun checkGeminiAPIkey():Boolean {
        return MyApp.checkGeminiAPIkey(systemDatabase)
    }

    private fun checkClaudeAPIkey(): Boolean {
        return MyApp.checkClaudeAPIkey(systemDatabase)
    }

    private suspend fun geminiCall(trimQuestion: String) {
        val reply = withContext(Dispatchers.IO) {
            textAPI.gemini(MyApp.quotedMessage + trimQuestion)
        }

        withContext(Dispatchers.Main) {
            handleGeminiResponse(reply)
        }
    }

    private fun handleGeminiResponse(reply: String?) {
        if (reply != null) {
            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                systemDatabase.replaceAnswer("errorAPI","0")
            }
            handler.removeCallbacks(waitRunnable)
            removeLastMessage()
            addToChat(reply,sentByBot)
            saveMessage()
            MyApp.checkToken(databaseHelper,textAPI)
        } else {
            handler.removeCallbacks(waitRunnable)
            removeLastMessage()
            addToChat(textAPI.error,sentByBot)
            saveMessage()
        }
        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
            showNotification()
        }
        waitForAnswer = false
        enableButton()
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

    private fun levenshteinDistance(str1: String, str2: String): Int {

        return MyApp.levenshteinDistance(str1,str2)
    }

    private fun removeMessageAt(position: Int) {
        runOnUiThread {
            val builder = when(themeNumber) {
                0 -> AlertDialog.Builder(this)
                else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
            }
            builder.setTitle("Delete?")
            builder.setPositiveButton("Delete") { _, _ ->
                if(messageList.isNotEmpty()) {
                    var i = messageList.size
                    while(i > (position + 1)) {
                        systemDatabase.replaceAnswer("message${i - 1}","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
                        i--
                    }
                    systemDatabase.deleteItem("message${messageList.size}")
                    messageList.removeAt(position)
                    messageAdapter.notifyItemRemoved(position)
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun showImage(position: Int) {
        MyApp.imageData = messageList[position].message
        MyApp.imagePrompt = databaseImage.searchPromptFromImage(messageList[position].message)
        MyApp.splitTextKeepLastDimension(MyApp.imagePrompt)
        val intent = Intent(this, FullImageView::class.java)
        startActivity(intent)
    }

    private fun saveImage(position: Int) {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Save Image")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Enter Image Name"
        builder.setView(input)
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.setPositiveButton("Save") { _, _ ->
            val imageName = input.text.toString().trim()
            if (imageName.isNotEmpty()) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        val file = File(messageList[position].message)
                        file.inputStream().use { inputStream ->inputStream.copyTo(outputStream)}
                    }
                    Toast.makeText(this, "Image saved to device storage", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Image name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun copyMessage(position: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", messageList[position].message)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
    }

    private fun copyMessageImage(position: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", databaseImage.searchPromptFromImage(messageList[position].message))
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
    }

    private fun quoteMessage(position: Int) {
        MyApp.quotedMessage = "Quote: ${messageList[position].message}\n\n"
        Toast.makeText(this, MyApp.quotedMessage, Toast.LENGTH_SHORT).show()
    }

    private fun quoteMessageImage(position: Int) {
        MyApp.quotedMessage = "Quote: ${databaseImage.searchPromptFromImage(messageList[position].message)}\n\n"
        Toast.makeText(this, MyApp.quotedMessage, Toast.LENGTH_SHORT).show()
    }

    override fun showContextMenu(view: View, position: Int, isLeft: Boolean, isTextImage: Boolean) {
        val llm = recyclerView.layoutManager as LinearLayoutManager
        val lastVisibleItemPosition = llm.findLastVisibleItemPosition()
        val firstVisibleItemPosition = llm.findFirstVisibleItemPosition()
        val result = firstVisibleItemPosition - lastVisibleItemPosition
        val theView = if((result in -2..2) && (view.height > (0.6f * recyclerView.height))) {
            editText
        } else {
            view
        }
        var bg = if(isLeft) {
            if(themeNumber == 0) {
                R.drawable.bot_chat_box_light_highlight
            } else {
                R.drawable.bot_chat_box_highlight
            }
        } else {
            if(themeNumber == 0) {
                R.drawable.user_chat_box_light_highlight
            } else {
                R.drawable.user_chat_box_highlight
            }
        }
        view.setBackgroundResource(bg)
        bg = if(isLeft) {
            if(themeNumber == 0) {
                R.drawable.bot_chat_box_light
            } else {
                R.drawable.bot_chat_box
            }
        } else {
            if(themeNumber == 0) {
                R.drawable.user_chat_box_light
            } else {
                R.drawable.user_chat_box
            }
        }
        Toast.makeText(this, "Message number ${position + 1}", Toast.LENGTH_SHORT).show()
        val wrapper = ContextThemeWrapper(this, R.style.MyMenuItemStyle)
        val popup = PopupMenu(wrapper, theView)
        popup.menuInflater.inflate(R.menu.message_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.copy -> {
                    if(isTextImage) {
                        copyMessageImage(position)
                    } else {
                        copyMessage(position)
                    }
                    true
                }
                R.id.quote -> {
                    if(isTextImage) {
                        quoteMessageImage(position)
                    } else {
                        quoteMessage(position)
                    }
                    true
                }
                R.id.delete -> {
                    removeMessageAt(position)
                    true
                }
                else -> false
            }
        }
        popup.setOnDismissListener {
            view.setBackgroundResource(bg)
        }
        popup.show()
    }

    override fun showImageMenu(view: View, position: Int) {
        var bg = if(themeNumber == 0) {
            R.drawable.bot_chat_box_light_highlight
        } else {
            R.drawable.bot_chat_box_highlight
        }
        view.setBackgroundResource(bg)
        bg = if(themeNumber == 0) {
            R.drawable.bot_chat_box_light
        } else {
            R.drawable.bot_chat_box
        }
        Toast.makeText(this, "Message number ${position + 1}", Toast.LENGTH_SHORT).show()
        val wrapper = ContextThemeWrapper(this, R.style.MyMenuItemStyle)
        val popup = PopupMenu(wrapper, view)
        popup.menuInflater.inflate(R.menu.image_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.save -> {
                    saveImage(position)
                    true
                }
                R.id.delete -> {
                    removeMessageAt(position)
                    true
                }
                else -> false
            }
        }
        popup.setOnDismissListener {
            view.setBackgroundResource(bg)
        }
        popup.show()
    }

    override fun showFileMenu(view: View, position: Int) {
        TODO("Not yet implemented")
    }

    override fun showText(position: Int): String {
        return databaseImage.searchPromptFromImage(messageList[position].message)
    }

    private fun setImageViewOpacity(view: View, bitmap: Bitmap) {
        val paint = Paint()
        paint.alpha = (255 * opacity).toInt()

        val newBitmap = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        val drawable = newBitmap.toDrawable(resources)
        view.background = drawable
    }

    private fun checkBackground() {
        val bgOn = systemDatabase.searchQuestion("background switch")
        if(bgOn == null) {
            systemDatabase.insertQuestion("background switch","OFF")
        } else {
            if(bgOn == "ON") {
                val bgPath = systemDatabase.searchQuestion("background image")
                if(!bgPath.isNullOrEmpty()) {
                    val bitmap = BitmapFactory.decodeFile(bgPath)
                    if(bitmap != null) setImageViewOpacity(recyclerView, bitmap)
                }
            }
        }
    }
}