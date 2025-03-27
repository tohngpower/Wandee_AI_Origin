package com.psn.myai

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import java.io.File

class ImageDatabase : ComponentActivity() {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var listView: ListView
    private lateinit var promptAdapter: ArrayAdapter<String>
    private var promptList: List<String>? = null
    private val databaseImage = DatabaseImage(this)
    private val systemDatabase = SystemDatabase(this)
    private lateinit var deleteButton: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var searchButton: ImageButton
    private lateinit var imageDisplay: ImageView
    private lateinit var upImage: ImageView
    private lateinit var downImage: ImageView
    private lateinit var noImageView: TextView
    private lateinit var buttonLayout: LinearLayout
    private var selectedPrompt = ""
    private var selectedImage: String? = null
    private var previousPrompt = "a"
    private var themeNumber = 0
    private var listViewChild = 0
    private var completeLS = false
    private val handler = Handler(Looper.getMainLooper())
    private var currentPosition = 0
    private var languageNumber = 0
    private var noImageText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.image_database)

        contentContainer = findViewById(R.id.image_database)
        listView = findViewById(R.id.listView2)
        deleteButton = findViewById(R.id.button8)
        saveButton = findViewById(R.id.save_picture)
        searchButton = findViewById(R.id.search_btn2)
        imageDisplay = findViewById(R.id.imageView5)
        upImage = findViewById(R.id.upIMG5)
        downImage = findViewById(R.id.downIMG5)
        noImageView = findViewById(R.id.noImage)
        buttonLayout = findViewById(R.id.linearLayout23)

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

        buttonLayout.isVisible = false
        MyApp.setAIResource(systemDatabase)
        imageDisplay.setImageResource(MyApp.imageDatabase)
        languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        promptAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, databaseImage.getAllPrompts())
        listView.adapter = promptAdapter
        val text: Array<String> = resources.getStringArray(R.array.no_image)
        noImageText = text[languageNumber - 1]
        if(promptAdapter.isEmpty) {
            noImageView.text = noImageText
            searchButton.isVisible = false
        } else {
            noImageView.isVisible = false
        }

        listView.viewTreeObserver.addOnScrollChangedListener {
            if(completeLS) {
                handler.removeCallbacks(lsRunnable)
            }
            checkScrollList()
        }

        listView.setOnItemClickListener { _, view, position, _ ->
            buttonLayout.isVisible = true
            selectedPrompt = promptAdapter.getItem(position).toString()
            MyApp.imagePrompt = selectedPrompt
            if(selectedPrompt != previousPrompt) {
                selectedImage = databaseImage.loadImageForPrompt(selectedPrompt)
                previousPrompt = selectedPrompt
            }
            if(selectedImage != null) {
                val bitmap = BitmapFactory.decodeFile(selectedImage)
                imageDisplay.setImageBitmap(bitmap)
            }

            listView.setItemChecked(position, true)
            view.isActivated = true
            val animator = StateListAnimator()
            // Add the animation for the activated state (highlighted)
            animator.addState(intArrayOf(android.R.attr.state_activated), AnimatorInflater.loadAnimator(this, R.animator.highlight_anim))
            // Set the default animation for other states (no highlight)
            animator.addState(intArrayOf(), AnimatorInflater.loadAnimator(this, R.animator.no_highlight_anim))
            // Apply the StateListAnimator to the viewQuestion
            view.stateListAnimator = animator
            currentPosition = position
        }

        deleteButton.setOnClickListener {
            deleteDialog()
        }

        saveButton.setOnClickListener {
            if (selectedImage != null) {
                showSaveImageDialog()
            }
        }

        searchButton.setOnClickListener {
            searchDialog()
        }

        imageDisplay.setOnLongClickListener {
            if (selectedImage != null) {
                showSaveImageDialog()
            } else {
                Toast.makeText(this, "No image selected to save", Toast.LENGTH_SHORT).show()
            }
            true
        }

        imageDisplay.setOnClickListener {
            if (selectedImage != null) {
                MyApp.imageData = selectedImage
                MyApp.splitTextKeepLastDimension(MyApp.imagePrompt)
                val intent = Intent(this, FullImageView::class.java)
                startActivity(intent)
            }
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showCopyDialog(position)
            true
        }

        upImage.setOnClickListener {
            if(upImage.isVisible) {
                listView.setSelection(0)
            }
        }

        downImage.setOnClickListener {
            if(downImage.isVisible) {
                val lastPos = listView.adapter.count - 1
                listView.setSelection(lastPos)
            }
        }

        handler.post(lsRunnable)
        alphaAnimation(upImage)
        alphaAnimation(downImage)
    }

    override fun onDestroy() {
        handler.removeCallbacks(lsRunnable)
        databaseImage.close()
        systemDatabase.close()
        super.onDestroy()
    }

    private fun saveImageToStorage(imagePath: Any, imageName: String) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                val file: Any
                when (imagePath) {
                    is String -> {
                        file = File(imagePath)
                        file.inputStream().use { inputStream ->inputStream.copyTo(outputStream)}
                    }

                    is ByteArray -> {
                        file = imagePath
                        resolver.openOutputStream(uri).use { outputStream.write(file) }
                    }

                    else -> {
                        null
                    }
                }
            }
            Toast.makeText(this, "Image saved to device storage", Toast.LENGTH_SHORT).show()
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
                saveImageToStorage(selectedImage!!, "$imageName.png")
            } else {
                Toast.makeText(this, "Image name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun extractMessage(message: String): Int {
        val keyword = "message"
        val index = message.indexOf(keyword)

        if (index != -1) {
            // Check if the keyword is not at the end of the string
            if (index + keyword.length < message.length) {
                return message.substring(index + keyword.length).trim().toInt()
            }
        }
        return -1
    }

    private fun deleteDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Delete?")
        builder.setPositiveButton("Delete") { _, _ ->
            if (selectedPrompt.isNotEmpty()) {
                val imagePath = databaseImage.loadImageForPrompt(selectedPrompt)
                if(imagePath is String) {
                    val detail = "image: $imagePath"
                    var message = systemDatabase.searchDetail(detail)
                    while(!message.isNullOrEmpty()) {
                        val index = extractMessage(message)
                        if(index == -1) break
                        val maxMessage = if(systemDatabase.searchQuestion("max message") != null) {
                            systemDatabase.searchQuestion("max message").toString().toInt()
                        } else {
                            systemDatabase.insertQuestion("max message","100")
                            100
                        }
                        var i = index
                        while(i <= maxMessage) {
                            val newDetail = systemDatabase.searchQuestion("message${i + 1}")
                            if(!newDetail.isNullOrEmpty()) {
                                systemDatabase.replaceAnswer("message$i",newDetail)
                            } else {
                                systemDatabase.deleteItem("message$i")
                                break
                            }
                            i++
                        }
                        message = systemDatabase.searchDetail(detail)
                    }
                }
                databaseImage.deletePromptAndImage(selectedPrompt)
                selectedImage = null
                selectedPrompt = ""
                updateListView()
                if(currentPosition > 0) {
                    listView.post { // Use post() to ensure the ListView is laid out
                        listView.smoothScrollToPosition(currentPosition - 1)
                    }
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showCopyDialog(position: Int) {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setPositiveButton("Copy") { _, _ ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = MyApp.splitTextKeepLastDimension(promptAdapter.getItem(position).toString())
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("imageGEN") { _, _ ->
            val text = "imageGEN "+MyApp.splitTextKeepLastDimension(promptAdapter.getItem(position).toString())
            systemDatabase.replaceAnswer("lastEditText",text)
            finish()
        }
        builder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun checkScrollList() {
        if(listView.getChildAt(0) != null) {
            listViewChild = listView.getChildAt(0).height
            val isAtTop = listView.firstVisiblePosition == 0
            val isAtBottom = listView.lastVisiblePosition == listView.adapter.count-1

            if(isAtTop && isAtBottom) {
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
        } else {
            upImage.isVisible = false
            downImage.isVisible = false
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

    private val lsRunnable = object : Runnable {
        override fun run() {
            if(!completeLS) {
                checkScrollList()
                if(listViewChild > 0) {
                    completeLS = true
                }
                handler.post(this)
            } else {
                completeLS = false
                handler.removeCallbacks(this)
            }
        }
    }

    private fun updateListView() {
        promptList = databaseImage.getAllPrompts()
        promptAdapter.clear()
        promptAdapter.addAll(promptList!!.map { it })
        promptAdapter.notifyDataSetChanged()
        listView.adapter=promptAdapter
        listView.isActivated = false
        buttonLayout.isVisible = false
        imageDisplay.setImageResource(MyApp.imageDatabase)
        if(promptAdapter.isEmpty) {
            searchButton.isVisible = false
            noImageView.isVisible = true
            noImageView.text = noImageText
        } else {
            noImageView.isVisible = false
        }
        Toast.makeText(this,"Done", Toast.LENGTH_SHORT).show()
    }

    private fun updateSearchView(searchText: String) {
        promptList = databaseImage.getSearchPrompts(searchText)
        promptAdapter.clear()
        promptAdapter.addAll(promptList!!.map { it })
        promptAdapter.notifyDataSetChanged()
        listView.adapter=promptAdapter
        listView.isActivated = false
        buttonLayout.isVisible = false
        imageDisplay.setImageResource(MyApp.imageDatabase)
        if(promptAdapter.isEmpty) {
            searchButton.isVisible = false
            noImageView.isVisible = true
            noImageView.text = noImageText
        } else {
            noImageView.isVisible = false
        }
        Toast.makeText(this,"Done", Toast.LENGTH_SHORT).show()
    }

    private fun searchDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Search")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Search"
        builder.setView(input)
        builder.setPositiveButton("Search") { _, _ ->
            if(input.text.isNullOrEmpty()) {
                updateListView()
            } else {
                updateSearchView(input.text.toString())
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }
}