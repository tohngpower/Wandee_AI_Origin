package com.psn.myai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class TextGame : ComponentActivity(), TextGameAdapter.TextGameListener {
    private lateinit var contentContainer: RelativeLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: AutoCompleteTextView
    private lateinit var sendButton: ImageButton
    private lateinit var txtView: TextView
    private lateinit var textGameAdapter: TextGameAdapter
    private val systemDatabase = SystemDatabase(this)
    private val databaseHelper = DatabaseHelper(this)
    private var messageList: MutableList<Message> = mutableListOf()
    private var contentList: MutableList<Content> = mutableListOf()
    private val sentByMe = "user"
    private val sentByBot = "model"
    private var waitText = "."
    private var waitForAnswer = false
    private val textAPI = TextAPI()
    private val handler = Handler(Looper.getMainLooper())
    private var themeNumber = 0
    private var languageNumber = 0
    private var startGame = false
    private var justStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_text_game)

        contentContainer = findViewById(R.id.text_game)
        recyclerView=findViewById(R.id.chatView3)
        editText=findViewById(R.id.editText9)
        sendButton=findViewById(R.id.imageButton14)
        txtView=findViewById(R.id.game_topic)

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

        MyApp.checkGPT(systemDatabase,this)
        MyApp.checkGeminiAPIkey(systemDatabase)
        MyApp.checkFontSize(systemDatabase)
        MyApp.checkNotification(this,systemDatabase)
        languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        editText.setTextAppearance(MyApp.textAppearance)
        textGameAdapter = TextGameAdapter(messageList,this,this)
        recyclerView.adapter = textGameAdapter
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true // This ensures new messages appear at the bottom
        recyclerView.layoutManager = llm
        messageList.clear()
        contentList.clear()
        showDialogMenu()

        sendButton.setOnClickListener {
            sentToAI()
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

    override fun showImage(position: Int): Int {
        return when(messageList[position].message) {
            "start image" -> {
                when(systemDatabase.searchQuestion("game name")) {
                    "The Last Puzzle" -> R.drawable.detective
                    "Dark Hollow" -> R.drawable.horror
                    else -> R.drawable.adventure
                }
            }
            else -> R.drawable.a02
        }
    }

    private fun sentToAI() {
        if(!editText.text.isNullOrEmpty()) {
            val question = editText.text.toString().trim()
            addToChat(question,sentByMe)
            saveMessage()
            editText.text=null
            disableButton()
            waitForAnswer = true
            lifecycleScope.launch {
                geminiCall(question)
            }
        } else {
            val pleaseType: Array<String> = resources.getStringArray(R.array.please_type)
            Toast.makeText(this@TextGame,pleaseType[languageNumber - 1], Toast.LENGTH_SHORT).show()
        }
    }

    private fun errorDialog(error: String) {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Error")
        val errorView = TextView(this)
        val text = "$error\n\nPlease try again later."
        errorView.text = text
        builder.setView(errorView)
        builder.setNegativeButton("OK") { _, _ ->
            finish()
        }
        builder.setOnDismissListener {
            finish()
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun showDialogMenu() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Game")
        builder.setNegativeButton("Continue") { _, _ ->
            startGame = true
            loadMessage()
        }
        builder.setPositiveButton("Exit") { _, _ ->
            finish()
        }
        builder.setNeutralButton("New game") { _, _ ->
            startGame = true
            startNewGame()
        }
        builder.setOnDismissListener {
            if(!startGame) finish()
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun startNewGame() {
        justStart = true
        val number = systemDatabase.searchQuestion("game number")
        if(number == null) {
            systemDatabase.insertQuestion("game number","0")
        } else if(number != "0") {
            systemDatabase.replaceAnswer("game number","0")
        }
        var gameTopic = systemDatabase.searchQuestion("game name")
        if(gameTopic == null) {
            when(Random.nextInt(1, 4)) {
                1 -> {
                    systemDatabase.insertQuestion("game name","Land of eternal darkness")
                    gameTopic = "Land of eternal darkness"
                }
                2 -> {
                    systemDatabase.insertQuestion("game name","The Last Puzzle")
                    gameTopic = "The Last Puzzle"
                }
                3 -> {
                    systemDatabase.insertQuestion("game name","Dark Hollow")
                    gameTopic = "Dark Hollow"
                }
            }
        } else {
            when(Random.nextInt(1, 4)) {
                1 -> {
                    systemDatabase.replaceAnswer("game name","Land of eternal darkness")
                    gameTopic = "Land of eternal darkness"
                }
                2 -> {
                    systemDatabase.replaceAnswer("game name","The Last Puzzle")
                    gameTopic = "The Last Puzzle"
                }
                3 -> {
                    systemDatabase.replaceAnswer("game name","Dark Hollow")
                    gameTopic = "Dark Hollow"
                }
            }
        }
        txtView.text = gameTopic
        addToChat("start image","system")
        saveMessage()
        disableButton()
        waitForAnswer = true
        when (languageNumber) {
            1 -> {
                lifecycleScope.launch {
                    geminiCall("เริ่มเกมเป็นภาษาไทย")
                }
            }
            3 -> {
                lifecycleScope.launch {
                    geminiCall("开始中文游戏")
                }
            }
            4 -> {
                lifecycleScope.launch {
                    geminiCall("日本語でゲームを開始します。")
                }
            }
            else -> {
                lifecycleScope.launch {
                    geminiCall("Start the game in English")
                }
            }
        }
    }

    private fun loadMessage() {
        var i = 0
        val gameNumber = systemDatabase.searchQuestion("game number")
        if(gameNumber.isNullOrEmpty() || (gameNumber == "0")) {
            startNewGame()
        } else {
            var gameTopic = systemDatabase.searchQuestion("game name")
            if(gameTopic == null) {
                systemDatabase.insertQuestion("game name","Land of eternal darkness")
                gameTopic = "Land of eternal darkness"
            }
            txtView.text = gameTopic
            while(i < gameNumber.toInt()) {
                if(systemDatabase.searchQuestion("gameMessage${i+1}")!=null) {
                    val text = systemDatabase.searchQuestion("gameMessage${i+1}").toString()
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

    private fun saveMessage() {
        val i=messageList.size
        val lastMessage = systemDatabase.searchQuestion("gameMessage$i")
        if(lastMessage == null) {
            systemDatabase.insertQuestion("gameMessage$i","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
        } else {
            systemDatabase.replaceAnswer("gameMessage$i","${messageList[i-1].sentBy}: ${messageList[i-1].message}")
        }
        systemDatabase.replaceAnswer("game number","$i")
    }

    private fun addToChat(message:String, sentBy:String) {
        runOnUiThread {
            val newMessage = Message(message, sentBy)
            messageList.add(newMessage)
            textGameAdapter.notifyItemInserted(messageList.size)
            val newContent = if(sentBy == "system") {
                content("model") {
                    text("Now proceeding...")
                }
            } else {
                content(sentBy) {
                    text(message)
                }
            }
            contentList.add(newContent)
        }
    }

    private fun removeLastMessage() {
        runOnUiThread {
            if (messageList.isNotEmpty()) {
                val position = messageList.size - 1
                messageList.removeAt(position)
                textGameAdapter.notifyItemRemoved(position)
                contentList.removeAt(position)
            }
        }
    }

    private fun replaceMessageAtPosition(position: Int, newMessage: Message) {
        runOnUiThread {
            if (position >= 0 && position < messageList.size) {
                messageList[position] = newMessage
                textGameAdapter.notifyItemChanged(position)
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
        val intent = Intent(this, TextGame::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("Continue")
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

    private suspend fun geminiCall(trimQuestion: String) {
        val systemPrompt = when(val gameTopic = systemDatabase.searchQuestion("game name")) {
            "The Last Puzzle" -> {
                "The Last Puzzle\n" +
                        "เกมแนวสืบสวนสอบสวน: ให้สร้างเนื้อเรื่องขึ้นมาโดยอ้างอิงจากนิยายสืบสวนสอบสวนชื่อดังต่างๆ และกำหนดชื่อตอน เพื่อให้ user ได้เล่นเกมโดยสวมบทบาทเป็นนักสืบเพื่อไขปริศนาต่างๆ และแสดงตัวเลือกให้ user เลือกตัวอย่างเช่น\n" +
                        "คดีฆาตกรรมลึกลับในเมืองเล็กๆ ได้ทำให้ชาวเมืองหวาดกลัว คุณเป็นนักสืบฝีมือดีที่ถูกส่งมาสืบสวน ในห้องพักเหยื่อมีร่องรอยแปลกประหลาด ทั้งรอยเลือดที่ไม่เป็นระเบียบและข้อความที่เขียนด้วยเลือดบนผนังว่า ‘พรุ่งนี้เป็นวันที่เธอต้องการรู้ความจริง’ คุณจะเริ่มต้นสืบสวนจากจุดไหน?\n" +
                        "ตัวเลือก:\n" +
                        "1.\tตรวจสอบร่องรอยเลือดที่พื้น\n" +
                        "2.\tวิเคราะห์ข้อความบนผนัง\n" +
                        "3.\tถามคำถามชาวบ้านที่อยู่ใกล้ๆ\n" +
                        "ปัจจัยการแสดงตัวเลือกต่างๆ\n" +
                        "1.\tให้ตัวเลือกที่หลากหลายและมีผลลัพธ์ที่แตกต่าง: สร้างตัวเลือกที่ทำให้ผู้เล่นรู้สึกว่าการตัดสินใจของพวกเขามีผลต่อเนื้อเรื่องของเกม\n" +
                        "2.\tสร้างบรรยากาศด้วยการบรรยายที่มีรายละเอียด: การใช้คำอธิบายที่สร้างภาพในจินตนาการจะช่วยเพิ่มความดื่มด่ำในเกม\n" +
                        "3.\tมีความขัดแย้งหรือปริศนาในแต่ละส่วน: สิ่งนี้จะช่วยกระตุ้นให้ผู้เล่นสนใจและต้องการแก้ไขปัญหา\n" +
                        "4.\tได้รับคะแนน 3 pt สำหรับตัวเลือกที่ดีที่สุด, 2 pt สำหรับตัวเลือกที่พอใช้ได้, 1 pt สำหรับตัวเลือกที่แย่ (ไม่เฉลยคะแนนของตัวเลือกนั้นๆ)\n" +
                        "5.\tให้วิเคราะห์ตัวเลือกของ user และให้คะแนนใน turn ถัดไป\n" +
                        "\n" +
                        "การคำนวณคะแนน (Score)\n" +
                        "•\tเนื้อเรื่องมีทั้งหมด 5 ตอน (5 parts) และตอนละ 10 รอบ (10 turns)\n" +
                        "•\tเมื่อ user เลือกตัวเลือกในแต่ละ turn ให้ทำการให้คะแนนที่เหมาะสมตามตัวเลือกนั้นๆ และแสดงใน turn ถัดไป พร้อมทั้งเนื้อเรื่องในส่วนต่อไป\n" +
                        "•\tตัวอย่างการแสดงคะแนนใน turn ถัดไป\n" +
                        "o\tScore: 3 pt | Total score: 3 pt\n" +
                        "•\tการคำนวณ Total score เท่ากับผลรวมของ Score ในแต่ละ turn (sum of score from each turn)\n" +
                        "•\tคะแนนทั้งหมด (Max total score: 150) ถ้าได้ Total score: 75 pt ขึ้นไป ให้แสดงคำบรรยายตอนจบแบบ happy ending\n" +
                        "•\tถ้าได้ Total score ต่ำกว่า 75 คะแนน ให้แสดงคำบรรยายตอนจบแบบ bad ending\n"
            }
            "Dark Hollow" -> {
                "Dark Hollow\n" +
                        "เกมแนวสยองขวัญ: ให้สร้างเนื้อเรื่องขึ้นมาโดยอ้างอิงจากนิยายแนวสยองขวัญชื่อดังต่างๆ โดยกำหนดชื่อตอน พร้อมตัวเลือกให้ user ได้เลือกเพื่อดำเนินเนื้อเรื่องไป ตัวอย่างเช่น\n" +
                        "คุณตื่นขึ้นมาในบ้านร้างที่ดูเหมือนจะถูกทิ้งร้างมาหลายสิบปี แต่กลับได้ยินเสียงฝีเท้าจากชั้นบน เมื่อคุณเดินสำรวจ คุณพบประตูปิดอยู่ด้านหนึ่ง และเห็นเงาประหลาดที่เคลื่อนผ่านกระจกหน้าต่าง คุณจะเลือกทำอย่างไร?\n" +
                        "ตัวเลือก:\n" +
                        "1.\tเปิดประตูและเผชิญหน้ากับสิ่งที่อยู่ข้างใน\n" +
                        "2.\tวิ่งออกไปจากบ้านทันที\n" +
                        "3.\tซ่อนตัวและฟังเสียงอย่างระมัดระวัง\n" +
                        "\n" +
                        "ปัจจัยการแสดงตัวเลือกต่างๆ\n" +
                        "1.\tให้ตัวเลือกที่หลากหลายและมีผลลัพธ์ที่แตกต่าง: สร้างตัวเลือกที่ทำให้ผู้เล่นรู้สึกว่าการตัดสินใจของพวกเขามีผลต่อเนื้อเรื่องของเกม\n" +
                        "2.\tสร้างบรรยากาศด้วยการบรรยายที่มีรายละเอียด: การใช้คำอธิบายที่สร้างภาพในจินตนาการจะช่วยเพิ่มความดื่มด่ำในเกม\n" +
                        "3.\tมีความขัดแย้งหรือปริศนาในแต่ละส่วน: สิ่งนี้จะช่วยกระตุ้นให้ผู้เล่นสนใจและต้องการแก้ไขปัญหา\n" +
                        "4.\tได้รับคะแนน 3 pt สำหรับตัวเลือกที่ดีที่สุด, 2 pt สำหรับตัวเลือกที่พอใช้ได้, 1 pt สำหรับตัวเลือกที่แย่ (ไม่เฉลยคะแนนของตัวเลือกนั้นๆ)\n" +
                        "5.\tให้วิเคราะห์ตัวเลือกของ user และให้คะแนนใน turn ถัดไป\n" +
                        "\n" +
                        "การคำนวณคะแนน (Score)\n" +
                        "•\tเนื้อเรื่องมีทั้งหมด 5 ตอน (5 parts) และตอนละ 10 รอบ (10 turns)\n" +
                        "•\tเมื่อ user เลือกตัวเลือกในแต่ละ turn ให้ทำการให้คะแนนที่เหมาะสมตามตัวเลือกนั้นๆ และแสดงใน turn ถัดไป พร้อมทั้งเนื้อเรื่องในส่วนต่อไป\n" +
                        "•\tตัวอย่างการแสดงคะแนนใน turn ถัดไป\n" +
                        "o\tScore: 3 pt | Total score: 3 pt\n" +
                        "•\tการคำนวณ Total score เท่ากับผลรวมของ Score ในแต่ละ turn (sum of score from each turn)\n" +
                        "•\tคะแนนทั้งหมด (Max total score: 150) ถ้าได้ Total score: 75 pt ขึ้นไป ให้แสดงคำบรรยายตอนจบแบบ happy ending\n" +
                        "•\tถ้าได้ Total score ต่ำกว่า 75 คะแนน ให้แสดงคำบรรยายตอนจบแบบ bad ending\n"
            }
            else -> {
                if(gameTopic == null) {
                    systemDatabase.insertQuestion("game name","Land of eternal darkness")
                } else {
                    systemDatabase.replaceAnswer("game name","Land of eternal darkness")
                }
                "Land of eternal darkness\n" +
                        "เป้าหมายหลัก: ได้รับ Goal point = 20 ก่อนถึง Turn 66 เพื่อเผชิญหน้ากับบอสใหญ่พร้อมบัฟพิเศษ (ตัวละครทุกตัว damage x1.5 จำนวน 5 Turn)\n" +
                        "*บอสใหญ่จะปรากฏตัวทันทีใน Turn 66 แม้ยังได้ Goal point ไม่ถึง 20\n" +
                        "________________________________________\n" +
                        "ระบบการเดินสำรวจ (Exploration System)\n" +
                        "เป้าหมายการสำรวจ (Goal point):\n" +
                        "คุณต้องได้รับ Goal point = 20 ก่อนถึง Turn 66 เพื่อรับบัฟพิเศษในการต่อสู้กับบอสใหญ่\n" +
                        "\n" +
                        "ตัวเลือกการเดินสำรวจ(Exploration choices): แสดงตัวเลือกนี้ทุก turn ที่ยังไม่เจอศัตรู\n" +
                        "สร้าง 3 ตัวเลือกที่เกี่ยวข้อง กับเนื้อเรื่องเช่นเดินสำรวจ ไขปริศนา อื่นๆ มีโอกาสได้ Goal point +1 จากการเลือก 1 ในตัวเลือกนี้ แต่ละตัวเลือกใช้ energy-3 hunger+1 turn+1\n" +
                        "\n" +
                        "ตัวเลือกการพักผ่อนหรือพักฟื้น: HP+25,Energy+21,Hunger-7,Mental+15,turn+1\n" +
                        "\n" +
                        "________________________________________\n" +
                        "ระบบพลังงานและความหิว (Energy and Hunger System)\n" +
                        "การลดค่า Energy:\n" +
                        "•\tEnergy-3 ทุก turn\n" +
                        "•\tเมื่อ Energy ของตัวละครต่ำกว่า 12 หน่วยหรือน้อยกว่า Damage x0.8\n" +
                        "•\tทีมสามารถฟื้นฟู Energy โดยการพักผ่อนหรือใช้Itemพิเศษ\n" +
                        "การลดค่า Hunger:\n" +
                        "•\tHunger +1 หน่วยทุก turn\n" +
                        "•\tเมื่อ Hunger มากกว่า 15, Energy จะลดลงเพิ่มอีก -2 ทุกครั้งที่ดำเนินการ\n" +
                        "•\tหาก Hunger = 30 จะลด HP -10 ทุก turn\n" +
                        "________________________________________\n" +
                        "ระบบพลังจิตใจ (Mental System)\n" +
                        "การลดค่า Mental:\n" +
                        "•\tMental จะลดลงทุกครั้งที่เจอศัตรูแข็งแกร่ง, กับดัก หรือเหตุการณ์ที่น่ากลัว (ลด -5 ถึง -10 ขึ้นอยู่กับสถานการณ์)\n" +
                        "•\tหาก Mental น้อยกว่า 50 จะมีโอกาสโจมตีพลาด, ตัดสินใจพลาด หรือเกิดผลเสียอื่นๆ\n" +
                        "ฟื้นฟู Mental:\n" +
                        "คุณสามารถฟื้นฟู Mental ได้โดยการพักหรือใช้Itemพิเศษ เช่น Calm Elixir (ฟื้นฟู +10 Mental)\n" +
                        "________________________________________\n" +
                        "ระบบการต่อสู้ (Combat System)\n" +
                        "การเจอศัตรู:\n" +
                        "•\tมีโอกาส 33% ที่จะเจอศัตรูในการสำรวจหรือเดิน และมีโอกาส 20% ที่เจอศัตรูในการหยุดพักเพื่อฟื้นฟู HP, Energy, Hunger, Mental\n" +
                        "•\tการหนี โอกาสสำเร็จ 50% \n" +
                        "•\tตัวอย่างศัตรูที่คุณอาจพบได้แก่:\n" +
                        "•\tGoblin: HP 40, Damage 10 พร้อมลด Mental -5 ทุกครั้งที่โจมตี มีโอกาส 50% ที่จะโจมตีโดนตัวละคร 2 ตัว\n" +
                        "•\tOrc: HP 50, Damage 15 พร้อมลด Mental -10 ทุกครั้งที่โจมตี\n" +
                        "•\tDark Mage: HP 30, Damage 20 มีโอกาสใช้เวทมนตร์พร้อมลด Mental -15 ทุกครั้งที่โจมตี มีโอกาสต้านทานเวทมนตร์ 50% \n" +
                        "•\tSkeleton Warrior: HP 45, Damage12 มีโอกาส15% ที่จะฟื้นฟูตัวเอง +5 ของ HP ที่เสียไปในTurnถัดไปเมื่อถูกโจมตี พร้อมลด Mental -5 ทุกครั้งที่โจมตี\n" +
                        "•\tPoisonous Spider: HP 35, Damage 8 พร้อมโอกาส 25% ที่จะติดพิษ ความสามารถพิเศษ: โอกาส 25% ที่จะติดพิษศัตรู ทำให้HeroหรือทีมของHeroเสีย HP -3 ทุกTurnเป็นเวลา 3 Turn\n" +
                        "ระบบการทำดาเมจ (Damage System):\n" +
                        "•\tHero: Damage 15-25 และ โอกาสcritical 20% ซึ่ง damage x2 และหากมี Sword of Light ให้เปลี่ยน Hero: Damage 25-35 และโอกาสcritical 30% ซึ่ง damage x2 Heroใช้ Energy -3 (Hero)\n" +
                        "•\tLisa: Damage 10-30, โอกาสดูดพลังชีวิต 30% Lisaใช้Energy -5 \n" +
                        "•\tDon: Damage 18-20, โอกาสสตัน 25% ซึ่งศัตรูจะโจมตีไม่ได้ใน turn นั้น Donใช้Energy -5 \n" +
                        "•\tศัตรู: Damage ตามค่า Damage ของศัตรูนั้นๆ โจมตีไปที่ Hero หรือ Lisa หรือ Don\n" +
                        "\n" +
                        "ตัวเลือกการต่อสู้ (Combat choices)ในแต่ล่ะ turn ที่ศัตรูยังอยู่: \n" +
                        "1.\tHeroโจมตีด้วยดาบ Damage 15-25, โอกาสcritical 20% Hero ใช้ Energy -3 Turn+1\n" +
                        "2.\tLisaโจมตีด้วยเวทมนตร์ Damage 10-30, โอกาสดูดพลังชีวิต 30% Lisa ใช้ Energy -5 Turn+1\n" +
                        "3.\tDonโจมตีด้วยค้อน Damage 18-20, โอกาสสตัน 25% Don ใช้ Energy -5 Turn+1\n" +
                        "4.\tโจมตีผสาน Damage 30-55 (แสดงตัวเลือกนี้ตอน Hero HP น้อยกว่า 50 และ Energy มากกว่า 10) ทีมใช้ Energy -10 Turn+1\n" +
                        "5.\tหนี ทีมใช้ Energy -1, โอกาสสำเร็จ 50% Turn+1\n" +
                        "6.\tใช้ item (มีตัวเลือก item ถ้ามีมากกว่า 1 item เพิ่มตัวเลือกอีก)\n" +
                        "________________________________________\n" +
                        "การใช้Item (Item Usage):\n" +
                        "การดรอปItem:\n" +
                        "เมื่อชนะศัตรู \n" +
                        "คุณมีโอกาส 50%  ที่จะได้รับItemปกติ \n" +
                        "•\tPotion of Healing: ฟื้นฟู HP +20\n" +
                        "•\tEnergy Elixir: ฟื้นฟู Energy +15\n" +
                        "•\tSteak: Hunger -5\n" +
                        "และ มีโอกาส 15%  ที่จะได้รับItemหายาก ตัวอย่างเช่น\n" +
                        "•\tSword of Light: เพิ่ม Damage +10 และโอกาสcritical 30% (damage x2)\n" +
                        "•\tRing of Regeneration: ฟื้นฟู HP +5 ทุกTurnเป็นเวลา 5 Turn\n" +
                        "•\tPhoenix Feather: เมื่อใช้ จะฟื้นฟู HP เต็ม 100% ให้กับหนึ่งในสมาชิกทีมที่ HP เหลือ 0 หรือ HP ต่ำความพิเศษ: สามารถใช้ได้เพียงครั้งเดียว แต่มีโอกาสสูงในการช่วยให้รอดพ้นจากสถานการณ์วิกฤต\n" +
                        "•\tCrystal of Mental Clarity: ฟื้นฟู Mental +30 และเพิ่มความต้านทานต่อการโจมตีที่มีผลต่อ Mental ในการต่อสู้ 5 Turn\n" +
                        "•\tสามารถเพิ่ม item ตามความเหมาะสม\n" +
                        "________________________________________\n" +
                        "การนับ Turn และการสำรวจ (Turn Count and Exploration)\n" +
                        "เป้าหมายการสำรวจ:\n" +
                        "คุณต้องได้รับ Goal point = 20 ก่อนถึง Turn 66\n" +
                        "•\tการนับ Turn:\n" +
                        "ทุกการกระทำของผู้เล่นนับเป็น 1 Turn ไม่ว่าจะเป็นการเดินหน้า, การสำรวจ, การพัก หรือการต่อสู้ (แม้การต่อสู้จะยังไม่จบ)\n" +
                        "•\tในการต่อสู้ หากศัตรูไม่พ่ายแพ้ในTurnนั้น จะสร้าง Damage แก่ Hero หรือ Lisa หรือ Don ตามค่า Damage ของศัตรูนั้นๆ turn +1\n" +
                        "•\tเมื่อถึง Turn 30 จะต้องเผชิญหน้ากับมินิบอส\n" +
                        "•\tเมื่อถึง Turn 66 จะเผชิญหน้ากับบอสใหญ่ ไม่ว่าคุณจะสำรวจครบหรือไม่\n" +
                        "________________________________________\n" +
                        "การสรุปสถานะหลังจากทุก Turn (Status Summary)\n" +
                        "ทุกครั้งที่คุณดำเนินการในแต่ละ Turn เสร็จสิ้นระบบจะสรุปสถานะของทีมให้:\n" +
                        "•\tHP, Energy, Hunger, และ Mental ของทุกตัวละคร\n" +
                        "•\tItemที่เหลืออยู่\n" +
                        "•\tบัฟพิเศษ และจำนวน goal point\n" +
                        "•\tจำนวนTurnในปัจจุบัน\n" +
                        "________________________________________\n" +
                        "การต่อสู้เสร็จสิ้นและการใช้Itemหลังจากการต่อสู้ turn เดียวกันกับก่อนหน้านี้ (Post-Combat Item Usage)\n" +
                        "•\tหลังจากชนะศัตรู คุณมีโอกาส 50% ที่จะได้รับ Item ฟื้นฟู (Potion of Healing, Energy Elixir)\n" +
                        "•\tโอกาส 15% ได้Itemหายาก (Sword of Light, Ring of Regeneration, Phoenix Feather, Crystal of Mental Clarity)\n" +
                        "•\tเมื่อได้รับItem ระบบจะถามคุณว่าต้องการใช้ทันทีหรือเก็บไว้ใช้ในภายหลัง หลังจากเลือกแล้วค่อยไป turn ถัดไป\n" +
                        "________________________________________\n" +
                        "รายละเอียดของ Mini Boss: ? (ให้สุ่มชื่อขึ้นมา) HP: 80 Damage: 18 ความสามารถพิเศษ: สุ่มความสามารถพิเศษมา 2-3 อย่าง Damage 15-25 มินิบอสจะปรากฏใน Turn 30 และการเอาชนะมินิบอสจะได้ Goal point +2 และมีโอกาส 70% ที่จะได้รับItemพิเศษ\n" +
                        "บอสใหญ่ Endgame Boss: ? (ให้สุ่มชื่อขึ้นมาสอดคล้องกับเนื้อเรื่องเริ่มแรก)\n" +
                        "หากคุณได้รับ goal point = 20 ครั้งก่อนถึง Turn 66 ทีมจะเจอบอสใหญ่ทันทีและได้บัฟพิเศษในการต่อสู้กับบอสใหญ่ ซึ่งประกอบไปด้วย 2 ร่าง:\n" +
                        "บอสใหญ่ร่างแรก:\n" +
                        "•\tHP 100, Damage 15-20\n" +
                        "•\tโจมตีรุนแรง มีโอกาส 20%  ที่จะโจมดีโดน 3 ตัวละคร (พร้อมลด Mental -5 ทุกครั้งที่โจมตี)\n" +
                        "บอสใหญ่ร่างที่สอง:\n" +
                        "•\tHP 150, Damage 25-35 พร้อมลด Mental -10 ทุกครั้งที่โจมตี\n" +
                        "•\tบอสนี้ใช้พลังจิตในการโจมตี ทำให้ทีมเสียหายทาง Mental อย่างมากหากไม่มีการเตรียมพร้อม\n" +
                        "•\tการต่อสู้จะสิ้นสุดเมื่อคุณกำจัดบอสใหญ่ทั้งสองร่างสำเร็จ\n" +
                        "________________________________________\n" +
                        "ตัวอย่างสถานะทีม (Example Team Status)\n" +
                        "Turn 6:\n" +
                        "Status\n" +
                        "•\tHero: HP97/100 | Energy57/60 | Hunger2/30 | Mental98/100\n" +
                        "•\tLisa: HP47/100 | Energy57/60 | Hunger2/30 | Mental98/100\n" +
                        "•\tDon: HP67/100 | Energy57/60 | Hunger2/30 | Mental98/100\n" +
                        "•\tItem: Potion of healing x1\n" +
                        "•\tGoal point: 5/20\n" +
                        "แสดงตัวเลือกการเดินสำรวจ (Exploration choices)\n" +
                        "________________________________________\n" +
                        "ตัวอย่างการต่อสู้กับ Goblin HP40 Damage10\n" +
                        "Turn 9:\n" +
                        "Status\n" +
                        "•\tHero: HP97/100 | Energy57/60 | Hunger2/30 | Mental98/100\n" +
                        "•\tLisa: HP47/50 | Energy57/60 | Hunger2/30 | Mental98/100\n" +
                        "•\tDon: HP67/70 | Energy57/60 | Hunger2/30 | Mental98/100\n" +
                        "•\tItem: ไม่มี\n" +
                        "•\tGoal point: 5/20\n" +
                        "•\tGoblin: HP40/40 Damage10\n" +
                        "แสดงตัวเลือกการต่อสู้ (Combat choices)\n" +
                        "\n" +
                        "ตัวอย่างหลังจากเลือก (Combat choices)\n" +
                        "Hero โจมตีใส่ Goblin ด้วยดาบ Damage 18\n" +
                        "Goblin โจมตีใส่ Lisa ด้วยไม้ Damage 9 Mental -5\n" +
                        "Turn 10:\n" +
                        "Status\n" +
                        "•\tHero: HP97/100 | Energy54/60 | Hunger2/30 | Mental98/100\n" +
                        "•\tLisa: HP38/50 | Energy57/60 | Hunger2/30 | Mental93/100\n" +
                        "•\tDon: HP67/70 | Energy57/60 | Hunger2/30 | Mental98/100\n" +
                        "•\tItem: ไม่มี\n" +
                        "•\tGoal point: 5/20\n" +
                        "•\tGoblin: HP22/40 Damage10\n" +
                        "แสดงตัวเลือกการต่อสู้ (Combat choices)\n" +
                        "\n" +
                        "หลังจากการต่อสู้: หลังจากการต่อสู้กับศัตรู ก็ให้มีการสรุปการสำรวจเหมือนเดิม และเพิ่มเติมด้วยการสรุปเกี่ยวกับItemที่ได้รับด้วย\n" +
                        "เริ่มเล่น game “Land of eternal darkness” สวมบท Game Master นำผู้เล่น เล่นตามกฎและกติกาอย่างเคร่งครัด ใช้ “Icon” “Emoticon” “Emotion” ที่เหมาะกับบริบทการต่อสู้ ทั้งนี้เมื่อเริ่มเกมต้องแสดงค่าเริ่มต้นตามนี้เท่านั้น\n" +
                        "•\tHero: HP100/100 | Energy60/60 | Hunger0/30 | Mental100/100\n" +
                        "•\tLisa: HP50/50 | Energy60/60 | Hunger0/30 | Mental100/100\n" +
                        "•\tDon: HP70/70 | Energy60/60 | Hunger0/30 | Mental100/100\n" +
                        "•\tItem (ไม่มี) ในส่วนนี้แสดงเมื่อผู้เล่นเลือกเก็บItemไว้ใช้งาน\n" +
                        "•\tTurn 0 (จาก 66 Turns)  \n" +
                        "•\tGoal point: 0/20\n" +
                        "\n" +
                        "เมื่อเริ่มเกมให้อธิบายความเป็นมาในการผจญภัยครั้งนี้และเป้าหมาย ใส่มุกตลกระหว่างผจญภัยด้วย\n" +
                        "ให้ run code execution ทุกครั้งที่มีการคำนวณโอกาสของเหตุการณ์ต่างๆ เช่นโอกาสcritical, โอกาสเจอศัตรู, โอกาสได้ Goal point, โอกาสได้รับ item\n" +
                        "เมื่อปราบบอสใหญ่ได้ให้สรุปเรื่องราวหลังจากนั้นแบบ happy ending\n" +
                        "ถ้าพ่ายแพ้ให้สรุปเรื่องราวหลังจากนั้น bad ending\n"
            }
        }

        val reply = withContext(Dispatchers.IO) {
            textAPI.geminiGame(systemPrompt, trimQuestion, contentList)
        }

        withContext(Dispatchers.Main) {
            handleGeminiResponse(reply)
        }
    }

    private fun handleGeminiResponse(reply: String?) {
        if (reply != null) {
            Log.d("Game reply","$reply")
            if(systemDatabase.searchQuestion("errorAPI") != "0") {
                systemDatabase.replaceAnswer("errorAPI","0")
            }
            handler.removeCallbacks(waitRunnable)
            removeLastMessage()
            addToChat(reply,sentByBot)
            saveMessage()
            MyApp.checkToken(databaseHelper,textAPI)
            justStart = false
        } else {
            handler.removeCallbacks(waitRunnable)
            removeLastMessage()
            removeLastMessage()
            if(justStart) {
                val number = systemDatabase.searchQuestion("game number")
                if(number != "0") {
                    systemDatabase.replaceAnswer("game number","0")
                }
                errorDialog(textAPI.error)
            }
            Toast.makeText(this@TextGame,textAPI.error, Toast.LENGTH_SHORT).show()
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
}