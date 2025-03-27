package com.psn.myai

import android.animation.AnimatorInflater
import android.animation.StateListAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Html
import android.text.InputType
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
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

class CustomAssistant : ComponentActivity(), MessageAdapter.MessageListener {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var assistantDetail: TextView
    private lateinit var assistantName: TextView
    private lateinit var runStatus: TextView
    private lateinit var assistantView: LinearLayout
    private lateinit var runButton: ImageButton
    private lateinit var uploadButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var editText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshButton: ImageView
    private lateinit var codeHelpButton: ImageView
    private lateinit var fileHelpButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var fileSearchSpinner: Spinner
    private lateinit var fileSearchAdapter: ArrayAdapter<String>
    private lateinit var fileCodeSpinner: Spinner
    private lateinit var fileCodeAdapter: ArrayAdapter<String>
    private lateinit var switchCodeInterpreter: SwitchCompat
    private lateinit var switchFileSearch: SwitchCompat
    private var messageList: MutableList<Message> = mutableListOf()
    private val sentByMe = "me"
    private val sentByBot = "bot"
    private var waitText = "."
    private var waitForAnswer = false
    private val systemDatabase = SystemDatabase(this)
    private val databaseHelper = DatabaseHelper(this)
    private val databaseImage = DatabaseImage(this)
    private val baseUrl = "https://api.openai.com/v1"
    private val aUrl = "$baseUrl/assistants"
    private val baseFileUrl = "$baseUrl/files"
    private val assistantIdArray = mutableListOf<String>()
    private val assistantNameArray = mutableListOf<String>()
    private val assistantDetailArray = mutableListOf<String>()
    private val assistantModelArray = mutableListOf<String>()
    private val messageIdArray = mutableListOf<String>()
    private val toolIdArray = mutableListOf<String>()
    private val toolNameArray = mutableListOf<String>()
    private val toolArgumentArray = mutableListOf<String>()
    private val filePathArray = mutableListOf<String>()
    private val fileIdArray = mutableListOf<String>()
    private val vectorStoreFileIdArray = mutableListOf<String>()
    private val imageTextArray = mutableListOf<String>()
    private val chatArray = mutableListOf<String>()
    private var themeNumber = 0
    private var assistantId = ""
    private var runId:String? = null
    private var threadId:String? = null
    private var vectorStoreId:String? = null
    private var numberOfAssistant = 0
    private val handler = Handler(Looper.getMainLooper())
    private val textAPI = TextAPI()
    private var runStart = false
    private var languageNumber = 0
    private var currentInstructions = ""
    private var currentName = ""
    private val version = "v2"
    private var checkRunStatus = ""
    private val runDelay = 500L
    private val fileUploadDelay = 5000L
    private val opacity = 0.9f
    private var fileSearchToolEnable = false
    private var codeInterpreterToolEnable = false
    private var updateIdDone = false
    private var selectedSearchFile = false
    private var selectedSearchFileName = ""
    private var searchFilePosition = 0
    private var selectedCodeFile = false
    private var selectedCodeFileName = ""
    private var codeFilePosition = 0
    private var question = ""
    private var getRunError = ""
    private var hasMore = ""
    private var lastId = ""
    private var chatIndex = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_custom_assistant)

        contentContainer = findViewById(R.id.custom_assistant)
        assistantDetail = findViewById(R.id.a_info)
        assistantName = findViewById(R.id.textView35)
        runStatus = findViewById(R.id.run_status)
        assistantView = findViewById(R.id.linearLayout18)
        runButton = findViewById(R.id.run_button)
        uploadButton = findViewById(R.id.upload_btn)
        menuButton = findViewById(R.id.menu_btn2)
        editText = findViewById(R.id.message_box)
        recyclerView = findViewById(R.id.chatView2)
        refreshButton = findViewById(R.id.refresh)
        switchFileSearch = findViewById(R.id.file_sw)
        switchCodeInterpreter = findViewById(R.id.codes_sw)
        fileHelpButton = findViewById(R.id.file_help)
        codeHelpButton = findViewById(R.id.codes_help)
        fileSearchSpinner = findViewById(R.id.fileSpinner2)
        fileCodeSpinner = findViewById(R.id.fileSpinner)

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

        MyApp.checkAPIkey(this,systemDatabase)
        MyApp.checkGPT(systemDatabase,this)
        MyApp.checkNotification(this,systemDatabase)
        languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        MyApp.checkFontSize(systemDatabase)
        editText.setTextAppearance(MyApp.textAppearance)
        assistantView.isVisible = false
        refreshButton.isVisible = false
        runStatus.isVisible = false
        checkBackground()

        messageAdapter= MessageAdapter(messageList,this,this, this)
        recyclerView.adapter=messageAdapter
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true // This ensures new messages appear at the bottom
        recyclerView.layoutManager = llm
        messageList.clear()

        val fileSearchHelpText: Array<String> = resources.getStringArray(R.array.file_search_help)
        val codeInterpreterHelpText: Array<String> = resources.getStringArray(R.array.code_interpreter_help)

        fileSearchAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item)
        fileSearchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fileSearchSpinner.adapter = fileSearchAdapter
        fileSearchAdapter.add("None")

        fileCodeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item)
        fileCodeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fileCodeSpinner.adapter = fileCodeAdapter
        fileCodeAdapter.add("None")

        disableFirst()
        val htmlText = "<b>Status</b>: <i>Now loading... Please wait.</i>"
        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
        runStatus.isVisible = true
        assistantList { reply ->
            runOnUiThread {
                when (reply) {
                    "Success" -> selectAssistantDialog(numberOfAssistant)
                    "Error: No assistant found!" -> createNewAssistantDialog()
                    else -> {
                        Toast.makeText(this,reply, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        fileHelpButton.setOnClickListener {
            textDialog("File Search",fileSearchHelpText[languageNumber-1])
        }

        codeHelpButton.setOnClickListener {
            textDialog("Code Interpreter",codeInterpreterHelpText[languageNumber-1])
        }

        assistantName.setOnClickListener {
            assistantView.isVisible = !assistantView.isVisible
        }

        assistantName.setOnLongClickListener {
            modifyAssistantNameDialog()
            true
        }

        assistantView.setOnLongClickListener {
            modifyAssistantInstructionsDialog()
            true
        }

        runButton.setOnClickListener {
            sentToAI()
        }

        refreshButton.setOnClickListener {
            refresh()
        }

        menuButton.setOnClickListener {
            assistantView.isVisible = !assistantView.isVisible
        }

        uploadButton.setOnClickListener {
            openFilePicker()
        }

        switchFileSearch.setOnCheckedChangeListener { _,_ ->
            fileSearchToolEnable = switchFileSearch.isChecked
        }

        switchCodeInterpreter.setOnCheckedChangeListener { _,_ ->
            codeInterpreterToolEnable = switchCodeInterpreter.isChecked
        }

        fileSearchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(val selectedItem = parent?.getItemAtPosition(position).toString()) {
                    "None" -> selectedSearchFile = false
                    else -> {
                        selectedSearchFile = true
                        selectedSearchFileName = selectedItem
                        searchFilePosition = position
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ... Handle the case where nothing is selected ...
            }
        }

        fileCodeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(val selectedItem = parent?.getItemAtPosition(position).toString()) {
                    "None" -> selectedCodeFile = false
                    else -> {
                        selectedCodeFile = true
                        selectedCodeFileName = selectedItem
                        codeFilePosition = position
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ... Handle the case where nothing is selected ...
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(waitForAnswer || runStart) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        databaseImage.close()
        systemDatabase.close()
        super.onDestroy()
    }

    override fun onPause() {
        MyApp.isAppInBackground = true
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        MyApp.isAppInBackground = false
    }

    private fun createNewThread(callback: (String?) -> Unit) {
        val url = "$baseUrl/threads"
        val json = JSONObject()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        threadId = jsonObject.optString("id", "")
                        chatArray.add(threadId.toString())
                        systemDatabase.insertQuestion("chat$chatIndex",threadId!!)
                        systemDatabase.insertQuestion("thread$chatIndex",threadId!!)
                        callback("Success")
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun getMessageList(callback: (String?) -> Unit) {
        messageIdArray.clear()
        filePathArray.clear()
        val url = "$baseUrl/threads/$threadId/messages?order=asc&limit=100"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        hasMore = jsonObject.optString("has_more","")
                        lastId = jsonObject.optString("last_id","")
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val reply = "Messages had been loaded."
                                for(i in 0 until dataArray.length()) {
                                    val data = dataArray.getJSONObject(i)
                                    val id = data.optString("id","")
                                    messageIdArray.add(id)
                                    val role = data.optString("role","")
                                    val sentBy = if(role == "user") {
                                        sentByMe
                                    } else {
                                        sentByBot
                                    }
                                    val contentArray = data.getJSONArray("content")
                                    val content = contentArray.getJSONObject(0)
                                    if(content.has("text")) {
                                        imageTextArray.add("")
                                        val text = content.getJSONObject("text")
                                        val message = text.optString("value","")
                                        val annotationsArray = text.getJSONArray("annotations")
                                        if(annotationsArray.length() > 0) {
                                            val annotationObj = annotationsArray.getJSONObject(0)
                                            val type = annotationObj.optString("type","")
                                            if(type == "file_path") {
                                                val originalName = annotationObj.optString("text","")
                                                val keyword = "data/"
                                                val index = originalName.indexOf(keyword)
                                                val fileName = originalName.substring(index + keyword.length).trim()
                                                val filePath = annotationObj.getJSONObject("file_path")
                                                val fileId = filePath.optString("file_id","")
                                                val fileAbsolutePath = systemDatabase.searchQuestion(fileId)
                                                if(fileAbsolutePath == null) {
                                                    val fileUrl = "$baseFileUrl/$fileId/content"
                                                    downloadFile(fileUrl,fileName,fileId) { path ->
                                                        runOnUiThread {
                                                            if(path != null) {
                                                                filePathArray.add(path)
                                                                addToChat(message,"file")
                                                            } else {
                                                                filePathArray.add("")
                                                                addToChat(message,sentBy)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    filePathArray.add(fileAbsolutePath)
                                                    addToChat(message,"file")
                                                }
                                            } else {
                                                filePathArray.add("")
                                                addToChat(message,sentBy)
                                            }
                                        } else {
                                            filePathArray.add("")
                                            addToChat(message,sentBy)
                                        }
                                    } else {
                                        if(content.has("image_file")) {
                                            filePathArray.add("")
                                            val imageFile = content.getJSONObject("image_file")
                                            val fileId = imageFile.optString("file_id","")
                                            if(!fileId.isNullOrEmpty()) {
                                                val filePath = databaseImage.loadImageForPrompt(fileId)
                                                if(filePath != null) {
                                                    addToChat(filePath,"image")
                                                } else {
                                                    val fileUrl = "$baseFileUrl/$fileId/content"
                                                    databaseImage.saveImageToDatabase(fileId, fileUrl, true) { status ->
                                                        runOnUiThread {
                                                            if(status == "Image generating success!") {
                                                                val newFilePath = databaseImage.loadImageForPrompt(fileId)
                                                                if(newFilePath != null) {
                                                                    addToChat(newFilePath,"image")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if(contentArray.length() > 1) {
                                                val textContent = contentArray.getJSONObject(1)
                                                if(textContent.has("text")) {
                                                    val textObj = textContent.getJSONObject("text")
                                                    val textValue = textObj.optString("value","")
                                                    imageTextArray.add(textValue)
                                                } else {
                                                    imageTextArray.add("Image")
                                                }
                                            } else {
                                                imageTextArray.add("Image")
                                            }
                                        } else {
                                            addToChat(content.toString(),sentBy)
                                        }
                                    }
                                }
                                callback(reply)
                            } else {
                                callback(null)
                            }
                        } else {
                            callback(null)
                        }
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun getMessageListNext(lastMsg: String, callback: (String?) -> Unit) {
        val url = "$baseUrl/threads/$threadId/messages?order=asc&limit=100&after=$lastMsg"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        hasMore = jsonObject.optString("has_more","")
                        lastId = jsonObject.optString("last_id","")
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val reply = "Messages had been loaded."
                                for(i in 0 until dataArray.length()) {
                                    val data = dataArray.getJSONObject(i)
                                    val id = data.optString("id","")
                                    messageIdArray.add(id)
                                    val role = data.optString("role","")
                                    val sentBy = if(role == "user") {
                                        sentByMe
                                    } else {
                                        sentByBot
                                    }
                                    val contentArray = data.getJSONArray("content")
                                    val content = contentArray.getJSONObject(0)
                                    if(content.has("text")) {
                                        imageTextArray.add("")
                                        val text = content.getJSONObject("text")
                                        val message = text.optString("value","")
                                        val annotationsArray = text.getJSONArray("annotations")
                                        if(annotationsArray.length() > 0) {
                                            val annotationObj = annotationsArray.getJSONObject(0)
                                            val type = annotationObj.optString("type","")
                                            if(type == "file_path") {
                                                val originalName = annotationObj.optString("text","")
                                                val keyword = "data/"
                                                val index = originalName.indexOf(keyword)
                                                val fileName = originalName.substring(index + keyword.length).trim()
                                                val filePath = annotationObj.getJSONObject("file_path")
                                                val fileId = filePath.optString("file_id","")
                                                val fileAbsolutePath = systemDatabase.searchQuestion(fileId)
                                                if(fileAbsolutePath == null) {
                                                    val fileUrl = "$baseFileUrl/$fileId/content"
                                                    downloadFile(fileUrl,fileName,fileId) { path ->
                                                        runOnUiThread {
                                                            if(path != null) {
                                                                filePathArray.add(path)
                                                                addToChat(message,"file")
                                                            } else {
                                                                filePathArray.add("")
                                                                addToChat(message,sentBy)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    filePathArray.add(fileAbsolutePath)
                                                    addToChat(message,"file")
                                                }
                                            } else {
                                                filePathArray.add("")
                                                addToChat(message,sentBy)
                                            }
                                        } else {
                                            filePathArray.add("")
                                            addToChat(message,sentBy)
                                        }
                                    } else {
                                        if(content.has("image_file")) {
                                            filePathArray.add("")
                                            val imageFile = content.getJSONObject("image_file")
                                            val fileId = imageFile.optString("file_id","")
                                            if(!fileId.isNullOrEmpty()) {
                                                val filePath = databaseImage.loadImageForPrompt(fileId)
                                                if(filePath != null) {
                                                    addToChat(filePath,"image")
                                                } else {
                                                    val fileUrl = "$baseFileUrl/$fileId/content"
                                                    databaseImage.saveImageToDatabase(fileId, fileUrl, true) { status ->
                                                        runOnUiThread {
                                                            if(status == "Image generating success!") {
                                                                val newFilePath = databaseImage.loadImageForPrompt(fileId)
                                                                if(newFilePath != null) {
                                                                    addToChat(newFilePath,"image")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if(contentArray.length() > 1) {
                                                val textContent = contentArray.getJSONObject(1)
                                                if(textContent.has("text")) {
                                                    val textObj = textContent.getJSONObject("text")
                                                    val textValue = textObj.optString("value","")
                                                    imageTextArray.add(textValue)
                                                } else {
                                                    imageTextArray.add("Image")
                                                }
                                            } else {
                                                imageTextArray.add("Image")
                                            }
                                        } else {
                                            addToChat(content.toString(),sentBy)
                                        }
                                    }
                                }
                                callback(reply)
                            } else {
                                callback(null)
                            }
                        } else {
                            callback(null)
                        }
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun getMessageListUpdate(lastMsg: String, callback: (String?) -> Unit) {
        val url = "$baseUrl/threads/$threadId/messages?order=asc&after=$lastMsg"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        hasMore = jsonObject.optString("has_more","")
                        lastId = jsonObject.optString("last_id","")
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val reply = "Messages had been loaded."
                                for(i in 0 until dataArray.length()) {
                                    val data = dataArray.getJSONObject(i)
                                    val id = data.optString("id","")
                                    messageIdArray.add(id)
                                    val role = data.optString("role","")
                                    val sentBy = if(role == "user") {
                                        sentByMe
                                    } else {
                                        sentByBot
                                    }
                                    val contentArray = data.getJSONArray("content")
                                    val content = contentArray.getJSONObject(0)
                                    if(content.has("text")) {
                                        imageTextArray.add("")
                                        val text = content.getJSONObject("text")
                                        val message = text.optString("value","")
                                        val annotationsArray = text.getJSONArray("annotations")
                                        if(annotationsArray.length() > 0) {
                                            val annotationObj = annotationsArray.getJSONObject(0)
                                            val type = annotationObj.optString("type","")
                                            if(type == "file_path") {
                                                val originalName = annotationObj.optString("text","")
                                                val keyword = "data/"
                                                val index = originalName.indexOf(keyword)
                                                val fileName = originalName.substring(index + keyword.length).trim()
                                                val filePath = annotationObj.getJSONObject("file_path")
                                                val fileId = filePath.optString("file_id","")
                                                val fileAbsolutePath = systemDatabase.searchQuestion(fileId)
                                                if(fileAbsolutePath == null) {
                                                    val fileUrl = "$baseFileUrl/$fileId/content"
                                                    downloadFile(fileUrl,fileName,fileId) { path ->
                                                        runOnUiThread {
                                                            if(path != null) {
                                                                filePathArray.add(path)
                                                                fileSearchAdapter.add(originalName)
                                                                fileSearchAdapter.notifyDataSetChanged()
                                                                fileCodeAdapter.add(originalName)
                                                                fileCodeAdapter.notifyDataSetChanged()
                                                                fileIdArray.add(fileId)
                                                                addToChat(message,"file")
                                                            } else {
                                                                filePathArray.add("")
                                                                addToChat(message,sentBy)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    filePathArray.add(fileAbsolutePath)
                                                    addToChat(message,"file")
                                                }
                                            } else {
                                                filePathArray.add("")
                                                addToChat(message,sentBy)
                                            }
                                        } else {
                                            filePathArray.add("")
                                            addToChat(message,sentBy)
                                        }
                                    } else {
                                        if(content.has("image_file")) {
                                            filePathArray.add("")
                                            val imageFile = content.getJSONObject("image_file")
                                            val fileId = imageFile.optString("file_id","")
                                            if(!fileId.isNullOrEmpty()) {
                                                val filePath = databaseImage.loadImageForPrompt(fileId)
                                                if(filePath != null) {
                                                    addToChat(filePath,"image")
                                                } else {
                                                    val fileUrl = "$baseFileUrl/$fileId/content"
                                                    databaseImage.saveImageToDatabase(fileId, fileUrl, true) { status ->
                                                        runOnUiThread {
                                                            if(status == "Image generating success!") {
                                                                val newFilePath = databaseImage.loadImageForPrompt(fileId)
                                                                if(newFilePath != null) {
                                                                    addToChat(newFilePath,"image")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if(contentArray.length() > 1) {
                                                val textContent = contentArray.getJSONObject(1)
                                                if(textContent.has("text")) {
                                                    val textObj = textContent.getJSONObject("text")
                                                    val textValue = textObj.optString("value","")
                                                    imageTextArray.add(textValue)
                                                } else {
                                                    imageTextArray.add("Image")
                                                }
                                            } else {
                                                imageTextArray.add("Image")
                                            }
                                        } else {
                                            addToChat(content.toString(),sentBy)
                                        }
                                    }
                                }
                                callback(reply)
                            } else {
                                callback(null)
                            }
                        } else {
                            callback(null)
                        }
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun deleteThreadAt(position: Int, callback: (String?) -> Unit) {
        val thread = systemDatabase.searchQuestion("thread$position")
        val url = "$baseUrl/threads/$thread"
        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val status = jsonObject.optString("deleted","")
                        callback(status)
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun deleteMessageAt(position: Int, callback: (String?) -> Unit) {
        val url = "$baseUrl/threads/$threadId/messages/${messageIdArray[position]}"
        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val status = jsonObject.optString("deleted","")
                        callback(status)
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun assistantList(callback: (String?) -> Unit) {
        assistantIdArray.clear()
        assistantDetailArray.clear()
        assistantNameArray.clear()
        val request = Request.Builder()
            .url(aUrl)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val reply = "Success"
                                numberOfAssistant = dataArray.length()
                                for(i in 0 until numberOfAssistant) {
                                    val data = dataArray.getJSONObject(i)
                                    val assistantID = data.optString("id", "")
                                    val assistantName = data.optString("name", "")
                                    val assistantInstruction = data.optString("instructions", "")
                                    val model = data.optString("model", "")
                                    assistantIdArray.add(assistantID)
                                    assistantNameArray.add(assistantName)
                                    assistantDetailArray.add(assistantInstruction)
                                    assistantModelArray.add(model)
                                }
                                callback(reply)
                            } else {
                                callback("Error: No assistant found!")
                            }
                        }
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun createNewAssistant(name: String,instructions:String,callback: (String?) -> Unit) {
        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("name", name)
        json.put("instructions", instructions)
        json.put("tools", assistantTools())
        if((codeInterpreterToolEnable&&selectedCodeFile)||(fileSearchToolEnable&&selectedSearchFile))
            json.put("tool_resources", assistantToolResources())

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(aUrl)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        assistantId = jsonObject.optString("id", "")
                        currentName = jsonObject.optString("name", "")
                        currentInstructions = jsonObject.optString("instructions", "")
                        assistantIdArray.add(assistantId)
                        assistantNameArray.add(currentName)
                        assistantDetailArray.add(currentInstructions)
                        callback("Success")
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun createNewAssistantDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Create new assistant")
        val view = layoutInflater.inflate(R.layout.new_assistant_dialog, null)
        val name = view.findViewById<EditText>(R.id.name_edit_text)
        val instructions = view.findViewById<EditText>(R.id.instruction_edit_text)
        var flag = false
        builder.setView(view)
        builder.setPositiveButton("create") { _, _ ->
            flag = true
            if(name.text.isNullOrEmpty()||instructions.text.isNullOrEmpty()) {
                Toast.makeText(this,"The name and instructions fields cannot be empty!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                runStatus.isVisible = true
                val htmlText = "<b>Status</b>: <i>Create assistant</i>"
                runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                createNewAssistant(name.text.trim().toString(),instructions.text.trim().toString()) { reply ->
                    runOnUiThread {
                        if(reply == "Success") {
                            assistantName.text = currentName
                            assistantDetail.text = currentInstructions
                            showAssistantView()
                            showChatListDialog()
                        } else {
                            Toast.makeText(this,reply, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            flag = true
            dialog.cancel()
            finish()
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

    private fun modifyAssistantName(name: String, callback: (String?) -> Unit) {
        val url = "$aUrl/$assistantId"
        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("name", name)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        currentName = jsonObject.optString("name", "")
                        callback("Success")
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun modifyAssistantNameDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Change name")
        val name = EditText(this)
        name.hint = "Assistant name"
        name.setText(currentName)
        builder.setView(name)
        builder.setPositiveButton("confirm") { _, _ ->
            if(name.text.isNullOrEmpty()) {
                Toast.makeText(this,"The name fields cannot be empty!", Toast.LENGTH_SHORT).show()
            } else if(name.text.trim().toString() == currentName) {
                Toast.makeText(this,"Done.", Toast.LENGTH_SHORT).show()
            } else {
                modifyAssistantName(name.text.trim().toString()) { reply ->
                    runOnUiThread {
                        if(reply == "Success") {
                            assistantName.text = currentName
                            Toast.makeText(this,"Done.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this,reply, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun modifyAssistantInstructions(instructions: String, callback: (String?) -> Unit) {
        val url = "$aUrl/$assistantId"
        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("instructions", instructions)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        currentInstructions = jsonObject.optString("instructions", "")
                        callback("Success")
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun modifyAssistantModel(callback: (String?) -> Unit) {
        val url = "$aUrl/$assistantId"
        val json = JSONObject()
        json.put("model", MyApp.gptModel)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    callback("Success")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun modifyAssistantInstructionsDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Change instructions")
        val instructions = EditText(this)
        instructions.hint = "You are smart assistant."
        instructions.setText(currentInstructions)
        builder.setView(instructions)
        builder.setPositiveButton("confirm") { _, _ ->
            if(instructions.text.isNullOrEmpty()) {
                Toast.makeText(this,"The instructions fields cannot be empty!", Toast.LENGTH_SHORT).show()
            } else if(instructions.text.trim().toString() == currentInstructions) {
                Toast.makeText(this,"Done.", Toast.LENGTH_SHORT).show()
            } else {
                modifyAssistantInstructions(instructions.text.trim().toString()) { reply ->
                    runOnUiThread {
                        if(reply == "Success") {
                            assistantDetail.text = currentInstructions
                            Toast.makeText(this,"Done.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this,reply, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun getRun(callback: (String?) -> Unit) {
        val url = "$baseUrl/threads/$threadId/runs/$runId"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val status = jsonObject.optString("status","")
                        val failedAt = jsonObject.optString("failed_at","")
                        val lastError = jsonObject.optString("last_error","")
                        when (status) {
                            "completed" -> {
                                val usage = jsonObject.getJSONObject("usage")
                                val inputToken = usage.optString("prompt_tokens","")
                                val outputToken = usage.optString("completion_tokens","")
                                val totalToken = usage.optString("total_tokens","")
                                textAPI.tokenInput = inputToken.toInt()
                                textAPI.tokenOutput = outputToken.toInt()
                                textAPI.numberOfToken = totalToken.toInt()
                                val model = jsonObject.optString("model","")
                                MyApp.latestUsage(totalToken,inputToken,outputToken,model)
                                MyApp.checkToken(databaseHelper,textAPI)
                            }
                            "requires_action" -> {
                                requireAction(jsonObject)
                            }
                            else -> {
                                getRunError = "Failed at: $failedAt Error: $lastError"
                            }
                        }
                        callback(status)
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun createNewMessage(question:String,callback: (String?) -> Unit) {
        val url = "$baseUrl/threads/$threadId/messages"
        val json = JSONObject()
        json.put("role", "user")
        json.put("content", question)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        lastId = jsonObject.optString("id", "")
                        messageIdArray.add(lastId)
                        callback("Success")
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun createRun(callback: (String?) -> Unit) {
        val url = "$baseUrl/threads/$threadId/runs"
        val json = JSONObject()
        json.put("assistant_id", assistantId)
        json.put("tools",assistantTools())
        if((codeInterpreterToolEnable&&selectedCodeFile)||(fileSearchToolEnable&&selectedSearchFile))
            json.put("tool_resources", assistantToolResources())

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        runId = jsonObject.optString("id", "")
                        systemDatabase.replaceAnswer("run_id","$runId")
                        val status = jsonObject.optString("status", "")
                        if(status == "requires_action") {
                            requireAction(jsonObject)
                        }
                        callback(status)
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
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
        chatIndex = 1
        var chatMsg = systemDatabase.searchQuestion("chat$chatIndex")
        chatArray.clear()
        while(chatMsg != null) {
            chatArray.add(chatMsg)
            chatIndex++
            chatMsg = systemDatabase.searchQuestion("chat$chatIndex")
        }
        if(chatIndex == 1) {
            val threadId = systemDatabase.searchQuestion("thread_id")
            if(threadId != null) {
                systemDatabase.insertQuestion("chat$chatIndex",threadId)
                systemDatabase.insertQuestion("thread$chatIndex",threadId)
                chatArray.add(threadId)
                chatIndex++
            }
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
                systemDatabase.replaceAnswer("chat${pos+1}",newName.toString())
                chatArray[pos] = newName.toString()
                chatAdapter.notifyDataSetChanged()
            }
        }
        builder.setView(view)
        builder.setNegativeButton("New") { _, _ ->
            flag = true
            val htmlText = "<b>Status</b>: <i>Create new chat</i>"
            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
            createNewThread { status ->
                runOnUiThread {
                    if(status == "Success") {
                        fileListExecute()
                    } else {
                        Toast.makeText(this,status, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
        builder.setPositiveButton("Select") { _, _ ->
            flag = true
            if(chatAdapter.isEmpty) {
                val htmlText = "<b>Status</b>: <i>Create new chat</i>"
                runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                createNewThread { status ->
                    runOnUiThread {
                        if(status == "Success") {
                            fileListExecute()
                        } else {
                            finish()
                        }
                    }
                }
            } else {
                threadId = systemDatabase.searchQuestion("thread${pos+1}")
                loadMessage()
            }
        }
        builder.setNeutralButton("Delete") { _, _ ->
            flag = true
            if(alreadySelected) {
                deleteThreadAt(pos+1) { status ->
                    runOnUiThread {
                        if(status == "true") {
                            if(chatArray.size == (pos+1)) {
                                systemDatabase.deleteItem("chat${chatArray.size}")
                                systemDatabase.deleteItem("thread${chatArray.size}")
                            } else {
                                var i = pos+1
                                while(i < chatArray.size) {
                                    val newChat = systemDatabase.searchQuestion("chat${i+1}")
                                    val threadIndex = systemDatabase.searchQuestion("thread${i+1}")
                                    systemDatabase.replaceAnswer("chat$i",newChat.toString())
                                    systemDatabase.replaceAnswer("thread$i",threadIndex.toString())
                                    i++
                                }
                                systemDatabase.deleteItem("chat${chatArray.size}")
                                systemDatabase.deleteItem("thread${chatArray.size}")
                            }
                            Toast.makeText(this,"Done", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this,"Failed to delete chat.", Toast.LENGTH_SHORT).show()
                        }
                        handler.post { showChatListDialog() }
                    }
                }
            } else {
                Toast.makeText(this,"Please select chat first.", Toast.LENGTH_SHORT).show()
                handler.post { showChatListDialog() }
            }
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

    private val messageRunnable = object : Runnable {
        override fun run() {
            if(hasMore == "true") {
                getMessageListNext(lastId) {
                    runOnUiThread {
                        handler.post(this)
                    }
                }
            } else {
                fileListExecute()
                handler.removeCallbacks(this)
            }
        }
    }

    private fun loadMessage() {
        if(threadId != null) {
            runStatus.isVisible = true
            val htmlText = "<b>Status</b>: <i>Get message list</i>"
            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
            getMessageList {
                runOnUiThread {
                    handler.post(messageRunnable)
                }
            }
        } else {
            fileListExecute()
        }
    }

    private fun fileListExecute() {
        val htmlText = "<b>Status</b>: <i>Get file list</i>"
        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
        fileIdArray.add("")
        fileList { reply ->
            runOnUiThread {
                enableButton()
                Toast.makeText(this,reply, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createVectorStoreExecute() {
        val htmlText = "<b>Status</b>: <i>Create vector store</i>"
        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
        runStatus.isVisible = true
        createVectorStore { reply ->
            runOnUiThread {
                if(reply == "Success") {
                    uploadFileToVectorStore()
                } else {
                    handler.removeCallbacks(waitRunnable)
                    removeLastMessage()
                    Toast.makeText(this@CustomAssistant,reply, Toast.LENGTH_SHORT).show()
                    enableButton()
                }
            }
        }
    }

    private fun uploadFileToVectorStore() {
        var htmlText = "<b>Status</b>: <i>File uploading</i>"
        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
        runStatus.isVisible = true
        if(vectorStoreId.isNullOrEmpty()) {
            handler.removeCallbacks(waitRunnable)
            removeLastMessage()
            Toast.makeText(this@CustomAssistant,"Cannot upload file. Please try again", Toast.LENGTH_SHORT).show()
            enableButton()
        } else {
            vectorStoreFile(fileIdArray[searchFilePosition]) { status ->
                runOnUiThread {
                    when (status) {
                        "completed" -> {
                            assistantRunExecute()
                        }
                        "in_progress" -> {
                            htmlText = "<b>Status</b>: <i>File uploading in progress</i>"
                            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                            handler.postDelayed(vectorStoreRunnable,fileUploadDelay)
                        }
                        else -> {
                            handler.removeCallbacks(waitRunnable)
                            removeLastMessage()
                            Toast.makeText(this@CustomAssistant,status, Toast.LENGTH_SHORT).show()
                            enableButton()
                        }
                    }
                }
            }
        }
    }

    private fun vectorStoreChecking() {
        vectorStoreId = systemDatabase.searchQuestion("vector store id")
        if(vectorStoreId == null) {
            createVectorStoreExecute()
        } else {
            var htmlText = "<b>Status</b>: <i>Check vector store</i>"
            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
            runStatus.isVisible = true
            vectorStoreList { status ->
                runOnUiThread {
                    when (status) {
                        "Success" -> {
                            htmlText = "<b>Status</b>: <i>Check vector store file</i>"
                            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                            vectorStoreFileList { reply ->
                                runOnUiThread {
                                    if(reply == "Success") {
                                        if(fileIdArray[searchFilePosition] in vectorStoreFileIdArray) {
                                            htmlText = "<b>Status</b>: <i>File exist. Start processing</i>"
                                            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                                            assistantRunExecute()
                                        } else {
                                            htmlText = "<b>Status</b>: <i>No file match. Start upload new file</i>"
                                            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                                            uploadFileToVectorStore()
                                        }
                                    } else if(reply == "Error: No vector store file found!") {
                                        htmlText = "<b>Status</b>: <i>No file. Start upload new file</i>"
                                        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                                        uploadFileToVectorStore()
                                    } else {
                                        handler.removeCallbacks(waitRunnable)
                                        removeLastMessage()
                                        Toast.makeText(this@CustomAssistant,reply, Toast.LENGTH_SHORT).show()
                                        enableButton()
                                    }
                                }
                            }
                        }
                        "No vector store found!" -> {
                            systemDatabase.deleteItem("vector store id")
                            createVectorStoreExecute()
                        }
                        else -> {
                            handler.removeCallbacks(waitRunnable)
                            removeLastMessage()
                            Toast.makeText(this@CustomAssistant,status, Toast.LENGTH_SHORT).show()
                            enableButton()
                        }
                    }
                }
            }
        }
    }

    private fun selectAssistantDialog(number: Int) {
        if(number == 1) {
            assistantId = assistantIdArray[0]
            currentName = assistantNameArray[0]
            currentInstructions = assistantDetailArray[0]
            assistantName.text = currentName
            assistantDetail.text = currentInstructions
            showAssistantView()
            if(assistantModelArray[0] == MyApp.gptModel) {
                val htmlText = "<b>Status</b>: <i>Get chat list</i>"
                runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                showChatListDialog()
            } else {
                var htmlText = "<b>Status</b>: <i>Changing GPT model</i>"
                runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                modifyAssistantModel {
                    runOnUiThread {
                        htmlText = "<b>Status</b>: <i>Get chat list</i>"
                        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                        showChatListDialog()
                    }
                }
            }
        } else {
            val builder = when(themeNumber) {
                0 -> AlertDialog.Builder(this)
                else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
            }
            builder.setTitle("Select assistant")
            val view = layoutInflater.inflate(R.layout.select_assistant, null)
            val assistantList = view.findViewById<Spinner>(R.id.assistant_list)
            val instructions = view.findViewById<TextView>(R.id.instructions)
            var flag = false
            val assistantAdapter:ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item)
            assistantAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            assistantAdapter.addAll(assistantNameArray)
            assistantAdapter.notifyDataSetChanged()
            assistantList.adapter = assistantAdapter
            assistantList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View?,
                    position: Int,
                    id: Long
                ) {
                    assistantId = assistantIdArray[position]
                    instructions.text = assistantDetailArray[position]
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {
                    // Do nothing
                }
            }
            builder.setView(view)
            builder.setPositiveButton("confirm") { _, _ ->
                flag = true
                val position = assistantList.selectedItemPosition
                assistantId = assistantIdArray[position]
                currentName = assistantNameArray[position]
                currentInstructions = assistantDetailArray[position]
                assistantName.text = currentName
                assistantDetail.text = currentInstructions
                showAssistantView()
                if(assistantModelArray[position] == MyApp.gptModel) {
                    val htmlText = "<b>Status</b>: <i>Get chat list</i>"
                    runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                    showChatListDialog()
                } else {
                    var htmlText = "<b>Status</b>: <i>Changing GPT model</i>"
                    runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                    modifyAssistantModel {
                        runOnUiThread {
                            htmlText = "<b>Status</b>: <i>Get chat list</i>"
                            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                            showChatListDialog()
                        }
                    }
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                flag = true
                dialog.cancel()
                finish()
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

    private  fun toolProcessing(callback: (JSONObject) -> Unit) {
        val json = JSONObject()
        val outputArray = JSONArray()
        for(i in 0 until toolIdArray.size) {
            if(toolNameArray[i] == "retrieve_current_date_and_time") {
                val toolObj = JSONObject()
                toolObj.put("tool_call_id",toolIdArray[i])
                toolObj.put("output",checkTime())
                outputArray.put(toolObj)
                json.put("tool_outputs", outputArray)
                callback(json)
            } else if(toolNameArray[i] == "retrieve_search_engine_information") {
                val topic = toolArgumentArray[i]
                if(topic.isNotEmpty()) {
                    textAPI.search(topic,5) { info ->
                        runOnUiThread {
                            val toolObj = JSONObject()
                            toolObj.put("tool_call_id",toolIdArray[i])
                            if(info != null) {
                                toolObj.put("output",info)
                            } else {
                                toolObj.put("output","No information.")
                            }
                            outputArray.put(toolObj)
                            json.put("tool_outputs", outputArray)
                            callback(json)
                        }
                    }
                }
            }
        }
    }

    private fun submitTool(json: JSONObject, callback: (String?) -> Unit) {
        val url = "$baseUrl/threads/$threadId/runs/$runId/submit_tool_outputs"

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val status = jsonObject.optString("status", "")
                        callback(status)
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun extractValue(jsonString: String, key: String="topic"): String? {
        val regex = Regex("\"$key\":\"(.*?)\"")
        val matchResult = regex.find(jsonString)
        return matchResult?.groups?.get(1)?.value
    }

    private fun requireAction(jsonObject: JSONObject) {
        val requiresAction = jsonObject.getJSONObject("required_action")
        val submitTool = requiresAction.getJSONObject("submit_tool_outputs")
        val toolCall = submitTool.getJSONArray("tool_calls")
        for(i in 0 until toolCall.length()) {
            val tool = toolCall.getJSONObject(i)
            val id = tool.optString("id","")
            val func = tool.getJSONObject("function")
            val arguments = func.optString("arguments","")
            if(!arguments.isNullOrEmpty()) {
                val topic = extractValue(arguments)
                if(!topic.isNullOrEmpty()) {
                    toolArgumentArray.add(topic)
                }
            } else {
                toolArgumentArray.add("")
            }
            val name = func.optString("name","")
            if(!id.isNullOrEmpty()) {
                toolIdArray.add(id)
            } else {
                toolIdArray.add("")
            }
            if(!name.isNullOrEmpty()) {
                toolNameArray.add(name)
            } else {
                toolNameArray.add("")
            }
        }
    }

    private fun assistantTools() : JSONArray {
        val toolsArray = JSONArray()
        if(fileSearchToolEnable&&selectedSearchFile) {
            val tool1 = JSONObject()
            tool1.put("type","file_search")
            toolsArray.put(tool1)
        }

        if(codeInterpreterToolEnable) {
            val tool2 = JSONObject()
            tool2.put("type","code_interpreter")
            toolsArray.put(tool2)
        }

        val tool3 = JSONObject()
        val funOBJ = JSONObject()
        funOBJ.put("name","retrieve_current_date_and_time")
        funOBJ.put("description","To get the current date and time, call this function whenever a user asks for the current date and time, someone’s age, or how many days are left until the next event. For example: \"How old is Mark Zuckerberg?\"")
        tool3.put("type","function")
        tool3.put("function",funOBJ)
        toolsArray.put(tool3)

        val tool4 = JSONObject()
        val funOBJ2 = JSONObject()
        funOBJ2.put("name","retrieve_search_engine_information")
        funOBJ2.put("description","To retrieve up-to-date information from a search engine, use this function whenever a user asks about recent events or information not covered in your database. For example: \"What is the current gold price?\" or \"Who is the current prime minister of a specific country?\"")
        val parameterOBJ2 = JSONObject()
        parameterOBJ2.put("type","object")
        val propertyOBJ2 = JSONObject()
        val topicOBJ = JSONObject()
        topicOBJ.put("type","string")
        topicOBJ.put("description","Use the topic provided by the user to search for relevant information.")
        propertyOBJ2.put("topic",topicOBJ)
        parameterOBJ2.put("properties",propertyOBJ2)
        val requiredArray = JSONArray()
        requiredArray.put("topic")
        parameterOBJ2.put("required",requiredArray)
        parameterOBJ2.put("additionalProperties",false)
        funOBJ2.put("parameters",parameterOBJ2)
        tool4.put("type","function")
        tool4.put("function",funOBJ2)
        toolsArray.put(tool4)

        return toolsArray
    }

    private fun assistantToolResources() : JSONObject {
        val toolResources = JSONObject()
        if(selectedCodeFile&&codeInterpreterToolEnable) {
            val codeObj = JSONObject()
            val fileIdArray1 = JSONArray()
            fileIdArray1.put(fileIdArray[codeFilePosition])
            codeObj.put("file_ids",fileIdArray1)
            toolResources.put("code_interpreter",codeObj)
        }

        if(selectedSearchFile&&fileSearchToolEnable) {
            val fileObj = JSONObject()
            val vectorArray = JSONArray()
            vectorArray.put(vectorStoreId)
            fileObj.put("vector_store_ids",vectorArray)
            toolResources.put("file_search",fileObj)
        }

        return toolResources
    }

    private fun checkTime(): String {
        MyApp.currentTime = LocalTime.now()
        MyApp.currentDate = LocalDateTime.now()
        val currentMinute:String = if(MyApp.currentTime.minute < 10) {
            "0${MyApp.currentTime.minute}"
        } else {
            MyApp.currentTime.minute.toString()
        }
        return "date: ${MyApp.currentDate.month} ${MyApp.currentDate.dayOfMonth}, ${MyApp.currentDate.year}\ntime: ${MyApp.currentTime.hour}:${currentMinute} O'clock"
    }

    private fun assistantRunExecute() {
        runStart = true
        var htmlText = "<b>Status</b>: <i>Add new message</i>"
        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
        runStatus.isVisible = true
        createNewMessage(question) { reply ->
            runOnUiThread {
                if(reply == "Success") {
                    htmlText = "<b>Status</b>: <i>Now processing</i>"
                    runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                    createRun { status ->
                        runOnUiThread {
                            htmlText = "<b>Status</b>: <i>$status</i>"
                            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                            handler.postDelayed(stepRunnable,runDelay)
                        }
                    }
                } else {
                    handler.removeCallbacks(waitRunnable)
                    removeLastMessage()
                    Toast.makeText(this,"Please try to refresh.", Toast.LENGTH_SHORT).show()
                    refreshButton.isVisible = true
                    enableButton()
                }
            }
        }
    }

    private fun sentToAI() {
        if(editText.text.isNullOrEmpty()) {
            val text = resources.getStringArray(R.array.please_type)
            Toast.makeText(this,text[languageNumber - 1], Toast.LENGTH_SHORT).show()
        } else if(runStart) {
            Toast.makeText(this,"Please wait for reply.", Toast.LENGTH_SHORT).show()
        } else {
            question = if(fileSearchToolEnable&&selectedSearchFile) {
                editText.text.trim().toString() + ", file name: $selectedSearchFileName"
            } else if(codeInterpreterToolEnable&&selectedCodeFile) {
                editText.text.trim().toString() + ", file name: $selectedCodeFileName"
            } else {
                editText.text.trim().toString()
            }
            addToChat(question,sentByMe)
            filePathArray.add("")
            disableButton()
            refreshButton.isVisible = false
            editText.text=null
            if(fileSearchToolEnable&&selectedSearchFile) {
                vectorStoreChecking()
            } else {
                assistantRunExecute()
            }
        }
    }

    private val stepRunnable = object : Runnable {
        override fun run() {
            getRun { reply ->
                runOnUiThread {
                    checkRunStatus = reply.toString()
                    var htmlText = "<b>Status</b>: <i>$checkRunStatus</i>"
                    runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                    if(reply == "completed") {
                        handler.removeCallbacks(waitRunnable)
                        removeLastMessage()
                        htmlText = "<b>Status</b>: <i>Get new message</i>"
                        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                        getMessageListUpdate(lastId) {
                            runOnUiThread {
                                runStart = false
                                if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                                    showNotification()
                                }
                                toolIdArray.clear()
                                toolArgumentArray.clear()
                                toolNameArray.clear()
                                enableButton()
                            }
                        }
                    } else if(reply == "requires_action") {
                        htmlText = "<b>Status</b>: <i>Submit tools</i>"
                        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                        toolProcessing { json ->
                            runOnUiThread {
                                submitTool(json) { status ->
                                    runOnUiThread {
                                        checkRunStatus = status.toString()
                                        htmlText = "<b>Status</b>: <i>$checkRunStatus</i>"
                                        runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                                        handler.postDelayed(this,runDelay)
                                    }
                                }
                            }
                        }
                    } else if((reply == "queued") || (reply == "in_progress")) {
                        handler.postDelayed(this,runDelay)
                    } else {
                        handler.removeCallbacks(waitRunnable)
                        removeLastMessage()
                        Toast.makeText(this@CustomAssistant,getRunError, Toast.LENGTH_SHORT).show()
                        runStart = false
                        enableButton()
                    }
                }
            }
        }
    }

    private val vectorStoreRunnable = object : Runnable {
        override fun run() {
            getVectorStoreFile { status ->
                runOnUiThread {
                    when (status) {
                        "completed" -> {
                            assistantRunExecute()
                        }
                        "in_progress" -> {
                            val htmlText = "<b>Status</b>: <i>$status</i>"
                            runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                            handler.postDelayed(this,fileUploadDelay)
                        }
                        else -> {
                            handler.removeCallbacks(this)
                            handler.removeCallbacks(waitRunnable)
                            removeLastMessage()
                            Toast.makeText(this@CustomAssistant,"Please try to refresh.", Toast.LENGTH_SHORT).show()
                            refreshButton.isVisible = true
                            enableButton()
                        }
                    }
                }
            }
        }
    }

    private fun refresh() {
        disableButton()
        refreshButton.isVisible = false
        getRun { reply ->
            runOnUiThread {
                when (reply) {
                    "completed" -> {
                        runStart = false
                        if(MyApp.isAppInBackground&&MyApp.notificationFlag) {
                            showNotification()
                        }
                        toolIdArray.clear()
                        toolArgumentArray.clear()
                        toolNameArray.clear()
                        enableButton()
                    }
                    "failed" -> {
                        handler.removeCallbacks(waitRunnable)
                        removeLastMessage()
                        Toast.makeText(this,reply, Toast.LENGTH_SHORT).show()
                        runStart = false
                    }
                    else -> {
                        handler.removeCallbacks(waitRunnable)
                        removeLastMessage()
                        Toast.makeText(this,"Please try to refresh.", Toast.LENGTH_SHORT).show()
                        refreshButton.isVisible = true
                    }
                }
                enableButton()
            }
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

    private fun disableFirst() {
        waitForAnswer = true
        runButton.isEnabled=false
        editText.isEnabled=false
        switchFileSearch.isEnabled=false
        switchCodeInterpreter.isEnabled=false
        fileCodeSpinner.isEnabled=false
        fileSearchSpinner.isEnabled=false
        uploadButton.isEnabled=false
        uploadButton.alpha = 0.3f
        runButton.alpha = 0.3f
        editText.alpha = 0.3f
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }

    private fun disableButton() {
        waitForAnswer = true
        runButton.isEnabled=false
        editText.isEnabled=false
        switchFileSearch.isEnabled=false
        switchCodeInterpreter.isEnabled=false
        fileCodeSpinner.isEnabled=false
        fileSearchSpinner.isEnabled=false
        uploadButton.isEnabled=false
        uploadButton.alpha = 0.3f
        runButton.alpha = 0.3f
        editText.alpha = 0.3f
        hideKeyboard()
        addToChat(waitText, sentByBot)
        handler.post(waitRunnable)
    }

    private fun enableButton() {
        runStatus.isVisible = false
        waitForAnswer = false
        runButton.isEnabled=true
        editText.isEnabled=true
        switchFileSearch.isEnabled=true
        switchCodeInterpreter.isEnabled=true
        fileCodeSpinner.isEnabled=true
        fileSearchSpinner.isEnabled=true
        uploadButton.isEnabled=true
        uploadButton.alpha = 1.0f
        runButton.alpha = 1.0f
        editText.alpha = 1.0f
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
                handler.postDelayed(this, runDelay)
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
        val intent = Intent(this, CustomAssistant::class.java)
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

    private fun copyMessage(position: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", messageList[position].message)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
    }

    private fun copyMessageImage(position: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", imageTextArray[position])
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
    }

    private fun deleteMessage(position: Int) {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Delete?")
        builder.setPositiveButton("Confirm") { _, _ ->
            if(position < messageIdArray.size) {
                if(messageIdArray[position] != "") {
                    deleteMessageAt(position) { status ->
                        runOnUiThread {
                            if(status == "true") {
                                messageIdArray.removeAt(position)
                                messageList.removeAt(position)
                                filePathArray.removeAt(position)
                                messageAdapter.notifyItemRemoved(position)
                                Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Cannot delete. Please come back again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please wait.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
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

    private fun saveFile(position: Int) {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Save File")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Enter File Name"
        builder.setView(input)
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.setPositiveButton("Save") { _, _ ->
            val fileName = input.text.toString().trim()
            if (fileName.isNotEmpty()) {
                val fileType = filePathArray[position].substringAfterLast(".")
                val file = File(filePathArray[position])
                if (file.exists()) {
                    val directory = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    if (directory != null) {
                        val fileOutput = File(directory, "$fileName.$fileType")
                        try {
                            file.inputStream().use { input ->
                                FileOutputStream(fileOutput).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Toast.makeText(this, "File saved to device storage", Toast.LENGTH_SHORT).show()
                        } catch (e: IOException) {
                            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        val dialog = builder.create()
        dialog.show()
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
        checkUpdateMsgId(position)
        val wrapper = ContextThemeWrapper(this, R.style.MyMenuItemStyle)
        val popup = PopupMenu(wrapper, theView)
        popup.menuInflater.inflate(R.menu.message_menu2, popup.menu)
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
                R.id.delete -> {
                    deleteMessage(position)
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

    override fun showImage(position: Int) {
        MyApp.imageData = messageList[position].message
        MyApp.imagePrompt = databaseImage.searchPromptFromImage(messageList[position].message)
        MyApp.splitTextKeepLastDimension(MyApp.imagePrompt)
        val intent = Intent(this, FullImageView::class.java)
        startActivity(intent)
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
        checkUpdateMsgId(position)
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
                    deleteMessage(position)
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
        checkUpdateMsgId(position)
        val wrapper = ContextThemeWrapper(this, R.style.MyMenuItemStyle)
        val popup = PopupMenu(wrapper, view)
        popup.menuInflater.inflate(R.menu.file_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.open -> {
                    if(position < filePathArray.size) openFile(position)
                    true
                }
                R.id.save -> {
                    if(position < filePathArray.size) saveFile(position)
                    true
                }
                R.id.delete -> {
                    deleteMessage(position)
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

    override fun showText(position: Int): String {
        return imageTextArray[position]
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

    private fun downloadFile(url: String, fileName: String, fileId: String, callback: (String?) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Save the downloaded content to a file
                    val directory = File(filesDir, "document")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val file = File(directory, fileName)
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    systemDatabase.insertQuestion(fileId,file.absolutePath)
                    callback(file.absolutePath)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Handle errors, e.g., show an error message
                callback(null)
            }
        })
    }

    private fun openFile(position: Int) {
        val filePath = filePathArray[position]
        val file = File(filePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val type = contentResolver.getType(uri)

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, type) // Set MIME type based on your file type
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permission to the intent

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Handle cases where there is no app to open the file
                Toast.makeText(this, "No application found to open this file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUpdateMsgId(position: Int) {
        updateIdDone = messageList.size == messageIdArray.size
        if(updateIdDone) Toast.makeText(this, "Message no. ${position + 1}", Toast.LENGTH_SHORT).show()
    }

    private fun textDialog(title: String, details: String) {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle(title)
        val textView = TextView(this)
        textView.text = details
        builder.setView(textView)
        builder.setNegativeButton("X") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun uploadFile(filePath: String, callback: (String?) -> Unit) {
        val file = File(filePath)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("purpose", "assistants")
            .addFormDataPart("file", file.name, file.asRequestBody())
            .build()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(baseFileUrl)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val fileId = jsonObject.optString("id", "")
                        val fileName = jsonObject.optString("filename", "")
                        runOnUiThread { // Ensure UI updates happen on the main thread
                            fileSearchAdapter.add(fileName)
                            fileSearchAdapter.notifyDataSetChanged()
                            fileCodeAdapter.add(fileName)
                            fileCodeAdapter.notifyDataSetChanged()
                            fileIdArray.add(fileId)
                        }
                        callback("Success")
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun fileList(callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url(baseFileUrl)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            val numberOfFile = dataArray.length()
                            if (numberOfFile > 0) {
                                for(i in 0 until numberOfFile) {
                                    val data = dataArray.getJSONObject(i)
                                    val fileID = data.optString("id", "")
                                    val fileName = data.optString("filename", "")
                                    runOnUiThread {
                                        if(getMimeTypeFromFileName(fileName) != null) {
                                            fileSearchAdapter.add(fileName)
                                            fileSearchAdapter.notifyDataSetChanged()
                                            fileCodeAdapter.add(fileName)
                                            fileCodeAdapter.notifyDataSetChanged()
                                            fileIdArray.add(fileID)
                                        }
                                    }
                                }
                                callback("Success")
                            } else {
                                callback("No file")
                            }
                        } else {
                            callback("No file")
                        }
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun handleFilePickerResult(data: Intent?) {
        if (data != null) {
            val selectedFileUri = data.data ?: return
            var fileName = ""
            var file: File? = null
            when(selectedFileUri.scheme) {
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
                }
                "file" -> {
                    file = File(selectedFileUri.path.toString())
                    fileName = file.name
                }
            }
            if(fileName.isNotEmpty()) {
                try {
                    val directory = File(filesDir, "document")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    file = File(directory, fileName)
                    val outputStream = FileOutputStream(file)
                    val inputStream = contentResolver.openInputStream(selectedFileUri)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                }  catch (_: IOException) {}
            }
            if(file != null) {
                val checkFile = systemDatabase.searchQuestion(file.absolutePath)
                if(checkFile == "Done") {
                    Toast.makeText(this, checkFile, Toast.LENGTH_SHORT).show()
                } else {
                    systemDatabase.insertQuestion(file.absolutePath,"Done")
                    disableFirst()
                    val htmlText = "<b>Status</b>: <i>File uploading</i>"
                    runStatus.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
                    runStatus.isVisible = true
                    uploadFile(file.absolutePath) { reply ->
                        runOnUiThread {
                            if(reply != null) {
                                Toast.makeText(this, reply, Toast.LENGTH_SHORT).show()
                                enableButton()
                            }
                        }
                    }
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
                    "text/*", // Include all text MIME types
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/pdf",
                    "application/json",
                    "application/typescript"
                )
            )
        }
        pickFileLauncher.launch(intent)
    }

    private fun getMimeTypeFromFileName(fileName: String): String? {
        return MyApp.getMimeTypeFromFileName(fileName)
    }

    private fun createVectorStore(callback: (String?) -> Unit) {
        val url = "$baseUrl/vector_stores"
        val json = JSONObject()
        json.put("name", assistantName.text)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val id = jsonObject.optString("id", "")
                        vectorStoreId = id
                        systemDatabase.insertQuestion("vector store id",id)
                        callback("Success")
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun vectorStoreList(callback: (String?) -> Unit) {
        val url = "$baseUrl/vector_stores"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val data = dataArray.getJSONObject(0)
                                val vectorID = data.optString("id", "")
                                vectorStoreId = vectorID
                                systemDatabase.replaceAnswer("vector store id", vectorID)
                                callback("Success")
                            } else {
                                callback("No vector store found!")
                            }
                        }
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun vectorStoreFile(fileId: String, callback: (String?) -> Unit) {
        val url = "$baseUrl/vector_stores/$vectorStoreId/files"
        val json = JSONObject()
        json.put("file_id", fileId)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val status = jsonObject.optString("status", "")
                        callback(status)
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun vectorStoreFileList(callback: (String?) -> Unit) {
        val url = "$baseUrl/vector_stores/$vectorStoreId/files"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                for(i in 0 until dataArray.length()) {
                                    val data = dataArray.getJSONObject(i)
                                    val fileID = data.optString("id", "")
                                    vectorStoreFileIdArray.add(fileID)
                                }
                                callback("Success")
                            } else {
                                callback("Error: No vector store file found!")
                            }
                        }
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun getVectorStoreFile(callback: (String?) -> Unit) {
        val url = "$baseUrl/vector_stores/$vectorStoreId/files/${fileIdArray[searchFilePosition]}"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .header("OpenAI-Beta", "assistants=$version")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(180, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(180, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val status = jsonObject.optString("status", "")
                        callback(status)
                    } catch (e: JSONException) {
                        callback(e.message)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }
        })
    }

    private fun showAssistantView() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val heightDp = resources.configuration.screenHeightDp
        assistantView.isVisible = !isLandscape && heightDp > 700
    }
}