package com.psn.myai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatHistory : ComponentActivity(), MessageAdapter.MessageListener {
    private lateinit var contentContainer: RelativeLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleLayout: LinearLayout
    private lateinit var buttonClear: ImageButton
    private lateinit var messageAdapter: MessageAdapter
    private val systemDatabase = SystemDatabase(this)
    private var themeNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_history)

        contentContainer = findViewById(R.id.main)
        recyclerView = findViewById(R.id.chatView4)
        titleLayout = findViewById(R.id.titleView)
        buttonClear = findViewById(R.id.clear_history_btn)

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

        messageAdapter= MessageAdapter(MyApp.messageList,this,this, this)
        recyclerView.adapter=messageAdapter
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true // This ensures new messages appear at the bottom
        recyclerView.layoutManager = llm
        buttonClear.isVisible = MyApp.messageList.isNotEmpty()

        buttonClear.setOnClickListener {
            clearChatDialog()
        }
    }

    override fun onDestroy() {
        systemDatabase.close()
        super.onDestroy()
    }

    override fun showImage(position: Int) {
        TODO("Not yet implemented")
    }

    override fun showText(position: Int): String {
        return ""
    }

    override fun showContextMenu(view: View, position: Int, isLeft: Boolean, isTextImage: Boolean) {
        val llm = recyclerView.layoutManager as LinearLayoutManager
        val lastVisibleItemPosition = llm.findLastVisibleItemPosition()
        val firstVisibleItemPosition = llm.findFirstVisibleItemPosition()
        val result = firstVisibleItemPosition - lastVisibleItemPosition
        val theView = if((result in -2..2) && (view.height > (0.6f * recyclerView.height))) {
            titleLayout
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

    override fun showFileMenu(view: View, position: Int) {
        TODO("Not yet implemented")
    }

    override fun showImageMenu(view: View, position: Int) {
        TODO("Not yet implemented")
    }

    private fun removeMessageAt(position: Int) {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Delete?")
        builder.setPositiveButton("Delete") { _, _ ->
            if(MyApp.messageList.isNotEmpty() && MyApp.contentList.isNotEmpty()) {
                MyApp.messageList.removeAt(position)
                MyApp.contentList.removeAt(position)
                messageAdapter.notifyItemRemoved(position)
                buttonClear.isVisible = MyApp.messageList.isNotEmpty()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun copyMessage(position: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", MyApp.messageList[position].message)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show()
    }

    private fun clearChatDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Clear history?")
        builder.setPositiveButton("Confirm") { _, _ ->
            MyApp.messageList.clear()
            MyApp.contentList.clear()
            finish()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }
}