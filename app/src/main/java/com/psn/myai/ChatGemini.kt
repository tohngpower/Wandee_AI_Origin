package com.psn.myai

import android.animation.AnimatorInflater
import android.animation.StateListAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
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
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

class ChatGemini : ComponentActivity(), MessageAdapter.MessageListener {
    private lateinit var contentContainer: RelativeLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var clearButton: ImageView
    private val textAPI = TextAPI()
    private lateinit var messageAdapter: MessageAdapter
    private val systemDatabase = SystemDatabase(this)
    private val databaseHelper = DatabaseHelper(this)
    private var messageList: MutableList<Message> = mutableListOf()
    private var contentList: MutableList<Content> = mutableListOf()
    private val sentByMe = "user"
    private val sentByBot = "model"
    private var waitText = "."
    private var waitForAnswer = false
    private var maxMessage = 100
    private val handler = Handler(Looper.getMainLooper())
    private var themeNumber = 0
    private val opacity = 0.9f
    private var languageNumber = 0
    private var gemini = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.chat_gemini)

        contentContainer = findViewById(R.id.chat_gemini)
        clearButton = findViewById(R.id.imageClose4)
        recyclerView=findViewById(R.id.gemini_chatView)
        editText=findViewById(R.id.editText10)
        sendButton=findViewById(R.id.imageButton15)

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
        val gpt = MyApp.checkGPT(systemDatabase,this)
        gemini = (gpt == 2)
        MyApp.checkGeminiAPIkey(systemDatabase)
        MyApp.checkAIPersonal(systemDatabase)
        MyApp.checkNotification(this,systemDatabase)
        MyApp.checkFontSize(systemDatabase)
        languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        editText.setTextAppearance(MyApp.textAppearance)
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
        loadLastEditText()
        clearButton.isVisible = false
        showChatListDialog()

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
            if(gemini) {
                lifecycleScope.launch {
                    geminiCall(question)
                }
            } else {
                textAPI.geminiThink(question, messageList) { reply ->
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
                                Toast.makeText(this,textAPI.error, Toast.LENGTH_SHORT).show()
                                when(systemDatabase.searchQuestion("errorAPI")) {
                                    "0" -> systemDatabase.replaceAnswer("errorAPI","1")
                                    "1" -> systemDatabase.replaceAnswer("errorAPI","2")
                                    else -> systemDatabase.replaceAnswer("errorAPI","3")
                                }
                            }
                            if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                showNotification()
                            }
                            waitForAnswer = false
                            enableButton()
                        } catch (e: Exception) {
                            handler.removeCallbacks(waitRunnable)
                            removeLastMessage()
                            Toast.makeText(this,e.message, Toast.LENGTH_SHORT).show()
                            when(systemDatabase.searchQuestion("errorAPI")) {
                                "0" -> systemDatabase.replaceAnswer("errorAPI","1")
                                "1" -> systemDatabase.replaceAnswer("errorAPI","2")
                                else -> systemDatabase.replaceAnswer("errorAPI","3")
                            }
                            if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                showNotification()
                            }
                            waitForAnswer = false
                            enableButton()
                        }
                    }
                }
            }
        } else {
            val pleaseType: Array<String> = resources.getStringArray(R.array.please_type)
            Toast.makeText(this,pleaseType[languageNumber - 1], Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToChat(message:String, sentBy:String) {
        runOnUiThread {
            val newMessage = Message(message, sentBy)
            messageList.add(newMessage)
            messageAdapter.notifyItemInserted(messageList.size)
            recyclerView.smoothScrollToPosition(messageList.size)
            val newContent = content(sentBy) {
                text(message)
            }
            contentList.add(newContent)
        }
    }

    private fun removeLastMessage() {
        runOnUiThread {
            if (messageList.isNotEmpty()) {
                val position = messageList.size - 1
                messageList.removeAt(position)
                messageAdapter.notifyItemRemoved(position)
                contentList.removeAt(position)
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
        val intent = Intent(this, ChatGemini::class.java)
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
            if(systemDatabase.searchQuestion("lastGeminiEditText")==null) {
                systemDatabase.insertQuestion("lastGeminiEditText","")
            } else {
                systemDatabase.replaceAnswer("lastGeminiEditText","")
            }
        } else {
            if(systemDatabase.searchQuestion("lastGeminiEditText")==null) {
                systemDatabase.insertQuestion("lastGeminiEditText",editText.text.toString())
            } else {
                systemDatabase.replaceAnswer("lastGeminiEditText",editText.text.toString())
            }
        }
    }

    private fun loadLastEditText() {
        val storedText = systemDatabase.searchQuestion("lastGeminiEditText")
        if (!storedText.isNullOrEmpty()) {
            editText.setText(storedText)
        }
    }

    private fun saveMessage() {
        val chatGroup = systemDatabase.searchQuestion("gemini chat group")
        if(chatGroup != null) {
            var i=messageList.size
            if(i <= maxMessage) {
                if(systemDatabase.searchQuestion("$chatGroup message$i")==null) {
                    systemDatabase.insertQuestion("$chatGroup message$i","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
                }
            } else {
                if(systemDatabase.searchQuestion("$chatGroup message$i")==null) {
                    systemDatabase.insertQuestion("$chatGroup message$i","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
                }
                while (i>1) {
                    systemDatabase.replaceAnswer("$chatGroup message${i-1}","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
                    i--
                }
                systemDatabase.deleteItem("$chatGroup message${messageList.size}")
                messageList.removeAt(0)
                messageAdapter.notifyItemRemoved(0)
                contentList.removeAt(0)
            }
        }
    }

    private fun loadMessage() {
        val chatGroup = systemDatabase.searchQuestion("gemini chat group")
        if(chatGroup != null) {
            var i = 0
            while(i < maxMessage) {
                if(systemDatabase.searchQuestion("$chatGroup message${i+1}")!=null) {
                    val text = systemDatabase.searchQuestion("$chatGroup message${i+1}").toString()
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
    }

    private suspend fun geminiCall(trimQuestion: String) {
        val reply = withContext(Dispatchers.IO) {
            textAPI.geminiChat(trimQuestion, contentList)
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
            Toast.makeText(this,textAPI.error, Toast.LENGTH_SHORT).show()
            when(systemDatabase.searchQuestion("errorAPI")) {
                "0" -> systemDatabase.replaceAnswer("errorAPI","1")
                "1" -> systemDatabase.replaceAnswer("errorAPI","2")
                else -> systemDatabase.replaceAnswer("errorAPI","3")
            }
        }
        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
            showNotification()
        }
        waitForAnswer = false
        enableButton()
    }

    private fun removeMessageAt(position: Int) {
        runOnUiThread {
            val builder = when(themeNumber) {
                0 -> AlertDialog.Builder(this)
                else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
            }
            builder.setTitle("Delete?")
            builder.setPositiveButton("Delete") { _, _ ->
                val chatGroup = systemDatabase.searchQuestion("gemini chat group")
                if(chatGroup != null) {
                    if(messageList.isNotEmpty()) {
                        var i = messageList.size
                        while(i > (position + 1)) {
                            systemDatabase.replaceAnswer("$chatGroup message${i - 1}","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
                            i--
                        }
                        systemDatabase.deleteItem("$chatGroup message${messageList.size}")
                        messageList.removeAt(position)
                        messageAdapter.notifyItemRemoved(position)
                        contentList.removeAt(position)
                    }
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun showImage(position: Int) {
        TODO("Not yet implemented")
    }

    private fun copyMessage(position: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", messageList[position].message)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
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
        popup.menuInflater.inflate(R.menu.message_menu2, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.copy -> {
                    copyMessage(position)
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
        TODO("Not yet implemented")
    }

    override fun showFileMenu(view: View, position: Int) {
        TODO("Not yet implemented")
    }

    override fun showText(position: Int): String {
        return "None"
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

    private fun showChatListDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Chat list")
        val view = layoutInflater.inflate(R.layout.select_chat, null)
        val chatList = view.findViewById<ListView>(R.id.chatListView)
        val rename = view.findViewById<EditText>(R.id.rename_chat)
        val renameChatButton = view.findViewById<Button>(R.id.rename_chat_btn)
        renameChatButton.isVisible = false
        rename.isVisible = false
        var flag = false
        var alreadySelected = false
        var pos = 0
        var chatIndex = 0
        var chatMsg = systemDatabase.searchQuestion("gemini chat0")
        val chatArray = mutableListOf<String>()
        var index = try {
            systemDatabase.searchQuestion("gemini chat group index").toString().toInt()
        } catch (e: NumberFormatException) {
            systemDatabase.insertQuestion("gemini chat group index","0")
            0
        }
        while(chatMsg != null) {
            chatArray.add(chatMsg)
            chatIndex++
            chatMsg = systemDatabase.searchQuestion("gemini chat$chatIndex")
        }

        val chatAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chatArray)
        chatList.adapter = chatAdapter
        chatList.setOnItemClickListener { _, _, position, _ ->
            alreadySelected = true
            rename.isVisible = true
            rename.setText(chatList.getItemAtPosition(position).toString())
            renameChatButton.isVisible = true
            pos = position
            val animator = StateListAnimator()
            // Add the animation for the activated state (highlighted)
            animator.addState(intArrayOf(android.R.attr.state_activated), AnimatorInflater.loadAnimator(this, R.animator.highlight_anim))
            // Set the default animation for other states (no highlight)
            animator.addState(intArrayOf(), AnimatorInflater.loadAnimator(this, R.animator.no_highlight_anim))
            // Apply the StateListAnimator to the viewQuestion
            view.stateListAnimator = animator
        }
        renameChatButton.setOnClickListener {
            val newName = rename.text
            if(newName.isNullOrEmpty()) {
                Toast.makeText(this,"Name cannot be empty!", Toast.LENGTH_SHORT).show()
            } else {
                systemDatabase.replaceAnswer("gemini chat$pos",newName.toString())
                chatArray[pos] = newName.toString()
                chatAdapter.notifyDataSetChanged()
            }
        }
        builder.setView(view)
        builder.setNegativeButton("New") { _, _ ->
            flag = true
            if(chatIndex == 0) {
                systemDatabase.insertQuestion("gemini chat0","gemini chat group0")
                val chatGroup = systemDatabase.searchQuestion("gemini chat group")
                if(chatGroup == null) {
                    systemDatabase.insertQuestion("gemini chat group","gemini chat group0")
                } else {
                    systemDatabase.replaceAnswer("gemini chat group","gemini chat group0")
                }
            } else {
                systemDatabase.replaceAnswer("gemini chat group","gemini chat group$chatIndex")
                index++
                if(chatIndex > index) {
                    systemDatabase.insertQuestion("gemini chat$chatIndex","gemini chat group$chatIndex")
                    systemDatabase.replaceAnswer("gemini chat group index","$chatIndex")
                } else {
                    systemDatabase.insertQuestion("gemini chat$chatIndex","gemini chat group$index")
                    systemDatabase.replaceAnswer("gemini chat group index","$index")
                }
            }
        }
        builder.setPositiveButton("Select") { _, _ ->
            flag = true
            if(chatAdapter.isEmpty) {
                systemDatabase.insertQuestion("gemini chat0","gemini chat group0")
                val chatGroup = systemDatabase.searchQuestion("gemini chat group")
                if(chatGroup == null) {
                    systemDatabase.insertQuestion("gemini chat group","gemini chat group0")
                } else {
                    systemDatabase.replaceAnswer("gemini chat group","gemini chat group0")
                }
            } else {
                systemDatabase.replaceAnswer("gemini chat group","gemini chat group$pos")
                loadMessage()
            }
        }
        builder.setNeutralButton("Delete") { _, _ ->
            flag = true
            if(alreadySelected) {
                if(chatArray.size == (pos+1)) {
                    var j = 0
                    while(j < maxMessage) {
                        if(systemDatabase.searchQuestion("gemini chat group$pos message${j+1}")!=null) {
                            systemDatabase.deleteItem("gemini chat group$pos message${j+1}")
                            j++
                        } else {
                            break
                        }
                    }
                    systemDatabase.deleteItem("gemini chat${chatArray.size-1}")
                } else {
                    var i = pos
                    while(i < chatArray.size) {
                        val newChat = systemDatabase.searchQuestion("gemini chat${i+1}")
                        systemDatabase.replaceAnswer("gemini chat$i",newChat.toString())
                        var j = 0
                        while(j < maxMessage) {
                            val newMessage = systemDatabase.searchQuestion("gemini chat group${i+1} message${j+1}")
                            if(newMessage != null) {
                                if(systemDatabase.searchQuestion("gemini chat group$i message${j+1}")!=null) {
                                    systemDatabase.replaceAnswer("gemini chat group$i message${j+1}",newMessage)
                                } else {
                                    systemDatabase.insertQuestion("gemini chat group$i message${j+1}",newMessage)
                                }
                            } else {
                                if(systemDatabase.searchQuestion("gemini chat group$i message${j+1}")!=null) {
                                    systemDatabase.deleteItem("gemini chat group$i message${j+1}")
                                } else {
                                    break
                                }
                            }
                            j++
                        }
                        i++
                    }
                    var j = 0
                    while(j < maxMessage) {
                        if(systemDatabase.searchQuestion("gemini chat group${chatArray.size-1} message${j+1}")!=null) {
                            systemDatabase.deleteItem("gemini chat group${chatArray.size-1} message${j+1}")
                            j++
                        } else {
                            break
                        }
                    }
                    systemDatabase.deleteItem("gemini chat${chatArray.size-1}")
                }
                Toast.makeText(this,"Done", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this,"Please select chat first.", Toast.LENGTH_SHORT).show()
            }
            handler.post { showChatListDialog() }
        }
        val dialog = builder.create()
        dialog.setOnDismissListener {
            if(!flag) {
                finish()
            }
        }
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
}