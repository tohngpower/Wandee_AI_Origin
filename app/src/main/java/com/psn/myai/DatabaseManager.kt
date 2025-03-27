package com.psn.myai

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
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

class DatabaseManager : ComponentActivity() {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var listView: ListView
    private lateinit var questionAdapter: ArrayAdapter<String>
    private var questionList = mutableListOf<String>()
    private val databaseHelper = DatabaseHelper(this)
    private val systemDatabase = SystemDatabase(this)
    private lateinit var replaceButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var searchButton: ImageButton
    private lateinit var edText2 : EditText
    private lateinit var answerDisplay: TextView
    private lateinit var upImage: ImageView
    private lateinit var downImage: ImageView
    private lateinit var upImage2: ImageView
    private lateinit var downImage2: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var editTextLayout: LinearLayout
    private var selectedQuestion: String? = null
    private var listViewChild = 0
    private var completeLS = false
    private var scrollY = 0
    private var scrollViewHeight = 0
    private var scrollViewChildHeight = 0
    private var completeSV = false
    private val handler = Handler(Looper.getMainLooper())
    private val handler2 = Handler(Looper.getMainLooper())
    private var currentPosition = 0
    private var themeNumber = 0
    private var languageNumber = 0
    private var latestSearch = ""
    private var alreadySearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.database_manager)

        contentContainer = findViewById(R.id.database)
        listView = findViewById(R.id.listView)
        replaceButton = findViewById(R.id.button3)
        deleteButton = findViewById(R.id.button4)
        searchButton = findViewById(R.id.search_btn1)
        edText2 = findViewById(R.id.editText2)
        answerDisplay = findViewById(R.id.textView3)
        upImage = findViewById(R.id.upIMG3)
        downImage = findViewById(R.id.downIMG3)
        upImage2 = findViewById(R.id.upIMG4)
        downImage2 = findViewById(R.id.downIMG4)
        scrollView = findViewById(R.id.SV2)
        editTextLayout = findViewById(R.id.linearLayout21)

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

        editTextLayout.isVisible = false
        deleteButton.isVisible = false

        questionList = databaseHelper.getAllQuestions()
        questionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, questionList)
        listView.adapter = questionAdapter
        languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        checkFontSize()
        val welcome: Array<String> = resources.getStringArray(R.array.welcome_text)
        answerDisplay.hint = welcome[languageNumber-1]
        if(questionAdapter.isEmpty) {
            val text: Array<String> = resources.getStringArray(R.array.no_database)
            answerDisplay.text = text[languageNumber-1]
            searchButton.isVisible = false
        }

        listView.setOnItemClickListener { _, view, position, _ ->
            deleteButton.isVisible = true
            editTextLayout.isVisible = true

            selectedQuestion = questionAdapter.getItem(position)
            val selectedAnswer = selectedQuestion?.let { databaseHelper.searchQuestion(it) }
            answerDisplay.text=selectedAnswer
            handler2.post(svRunnable)

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

        listView.viewTreeObserver.addOnScrollChangedListener {
            if(completeLS) {
                handler.removeCallbacks(lsRunnable)
            }
            checkScrollList()
        }

        replaceButton.setOnClickListener {
            if(edText2.text.isNullOrEmpty()) {
                val pleaseType: Array<String> = resources.getStringArray(R.array.please_type)
                Toast.makeText(this,pleaseType[languageNumber-1], Toast.LENGTH_SHORT).show()
            } else {
                replaceDialog()
            }
        }

        deleteButton.setOnClickListener {
            deleteDialog()
        }

        searchButton.setOnClickListener {
            searchDialog()
        }

        answerDisplay.setOnLongClickListener {
            if(!answerDisplay.text.isNullOrEmpty()) {
                showCopyAnswer()
                true
            } else {
                false
            }
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showCopyDialog(position)
            true
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if(completeSV) {
                handler2.removeCallbacks(svRunnable)
            }
            checkScrollView()
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

        upImage2.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_UP)
        }

        downImage2.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
        handler.post(lsRunnable)
        alphaAnimation(upImage)
        alphaAnimation(downImage)
        alphaAnimation(upImage2)
        alphaAnimation(downImage2)
    }

    override fun onDestroy() {
        handler2.removeCallbacks(svRunnable)
        handler.removeCallbacks(lsRunnable)
        databaseHelper.close()
        systemDatabase.close()
        super.onDestroy()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        edText2.postDelayed({
            imm.hideSoftInputFromWindow(edText2.windowToken, 0)
        }, 100)
    }

    private fun checkFontSize() {
        MyApp.checkFontSize(systemDatabase)
        edText2.setTextAppearance(MyApp.textAppearance)
        answerDisplay.setTextAppearance(MyApp.textAppearance)
    }

    private fun splitTextKeepLastDimension(text: String): String {
        val ai = MyApp.TOTAL_AI
        var splitText = ""
        for(i in 1..ai) {
            val pattern = Regex("to ai0$i")
            val lastPattern = pattern.findAll(text).lastOrNull()?.range?.first ?: -1
            splitText = if (lastPattern!=-1) {
                text.substring(0, lastPattern).trim()
            } else {
                ""
            }
            if(splitText.isNotEmpty()) {
                break
            }
        }
        if(splitText.isEmpty()) {
            splitText = text
        }
        return splitText
    }

    private fun showCopyDialog(position: Int) {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setPositiveButton("Copy") { _, _ ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = splitTextKeepLastDimension(questionAdapter.getItem(position).toString())
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Ask again") { _, _ ->
            val text = splitTextKeepLastDimension(questionAdapter.getItem(position).toString())
            systemDatabase.replaceAnswer("lastEditText",text)
            finish()
        }
        builder.setNeutralButton("TTS") { _, _ ->
            val text = splitTextKeepLastDimension(questionAdapter.getItem(position).toString())
            if(systemDatabase.searchQuestion("lastEditText3")==null) {
                systemDatabase.insertQuestion("lastEditText3",text)
            } else {
                systemDatabase.replaceAnswer("lastEditText3",text)
            }
            val i = Intent(this, OnlyTextToSpeech::class.java)
            //Change the activity.
            startActivity(i)
            finish()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showCopyAnswer() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setPositiveButton("Copy") { _, _ ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", answerDisplay.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
        }
        builder.setNeutralButton("TTS") { _, _ ->
            if(systemDatabase.searchQuestion("lastEditText3")==null) {
                systemDatabase.insertQuestion("lastEditText3",answerDisplay.text.toString())
            } else {
                systemDatabase.replaceAnswer("lastEditText3",answerDisplay.text.toString())
            }
            val i = Intent(this, OnlyTextToSpeech::class.java)
            //Change the activity.
            startActivity(i)
            finish()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
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

    private fun checkScrollView() {
        scrollY = scrollView.scrollY
        scrollViewHeight = scrollView.height
        scrollViewChildHeight = scrollView.getChildAt(0).height
        val isAtTop = scrollY == 0
        val isAtBottom = scrollY + scrollViewHeight >= scrollViewChildHeight

        if(scrollViewChildHeight <= scrollViewHeight) {
            upImage2.isVisible = false
            downImage2.isVisible = false
        } else if(isAtTop) {
            upImage2.isVisible = false
            downImage2.isVisible = true
        }else if(isAtBottom) {
            upImage2.isVisible = true
            downImage2.isVisible = false
        } else {
            upImage2.isVisible = true
            downImage2.isVisible = true
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

    private val svRunnable = object : Runnable {
        override fun run() {
            if(!completeSV) {
                checkScrollView()
                if(scrollViewHeight > 0 && scrollViewChildHeight > 0) {
                    completeSV = true
                }
                handler2.post(this)
            } else {
                completeSV = false
                scrollView.fullScroll(View.FOCUS_UP)
                handler2.removeCallbacks(this)
            }
        }
    }

    private fun updateListView() {
        editTextLayout.isVisible = false
        questionList.clear()
        questionList = databaseHelper.getAllQuestions()
        questionAdapter.clear()
        questionAdapter.addAll(questionList.map { it })
        questionAdapter.notifyDataSetChanged()
        listView.adapter=questionAdapter
        listView.isActivated = false
        deleteButton.isVisible = false
        hideKeyboard()
        answerDisplay.text = if(questionAdapter.isEmpty) {
            searchButton.isVisible = false
            val text: Array<String> = resources.getStringArray(R.array.no_database)
            text[languageNumber-1]
        } else {
            searchButton.isVisible = true
            null
        }
    }

    private fun updateSearchView(searchText: String) {
        editTextLayout.isVisible = false
        questionList.clear()
        questionList = databaseHelper.getSearchQuestion(searchText)
        questionAdapter.clear()
        questionAdapter.addAll(questionList.map { it })
        questionAdapter.notifyDataSetChanged()
        listView.adapter=questionAdapter
        listView.isActivated = false
        deleteButton.isVisible = false
        hideKeyboard()
        answerDisplay.text = if(questionAdapter.isEmpty) {
            searchButton.isVisible = false
            val text: Array<String> = resources.getStringArray(R.array.no_database)
            text[languageNumber-1]
        } else {
            searchButton.isVisible = true
            null
        }
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
                latestSearch = ""
                updateListView()
            } else {
                latestSearch = input.text.toString()
                updateSearchView(input.text.toString())
            }
            alreadySearch = true
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun deleteDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Delete?")
        builder.setPositiveButton("Delete") { _, _ ->
            if (!selectedQuestion.isNullOrEmpty()) {
                databaseHelper.deleteQuestionAndAnswer(selectedQuestion!!)
                questionList.removeAt(currentPosition)
                if(alreadySearch) {
                    if(latestSearch.isNotEmpty()) {
                        updateSearchView(latestSearch)
                    } else {
                        updateListView()
                    }
                    if(currentPosition > 0) {
                        listView.post { // Use post() to ensure the ListView is laid out
                            listView.smoothScrollToPosition(currentPosition - 1)
                        }
                    }
                } else {
                    questionAdapter.notifyDataSetChanged()
                    if(currentPosition > 0) {
                        listView.setItemChecked(currentPosition, false)
                    }
                    answerDisplay.text = null
                    editTextLayout.isVisible = false
                }
                deleteButton.isVisible = false
                Toast.makeText(this,"Done", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun replaceDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Replace with new answer?")
        builder.setPositiveButton("Replace") { _, _ ->
            val newAnswer = edText2.text.toString()
            databaseHelper.replaceAnswer(selectedQuestion!!,newAnswer)
            answerDisplay.text = newAnswer
            hideKeyboard()
            Toast.makeText(this,"Done", Toast.LENGTH_SHORT).show()
            edText2.text = null
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }
}