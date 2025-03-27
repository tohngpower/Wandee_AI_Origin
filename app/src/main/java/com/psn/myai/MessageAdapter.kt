package com.psn.myai

import android.content.Context
import android.graphics.BitmapFactory
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messageList: List<Message>, val context: Context, val activity: ComponentActivity, private val listener: MessageListener) :
    RecyclerView.Adapter<MessageAdapter.MyViewHolder>() {

    class MyViewHolder(row: View, private val listener: MessageListener, activity: ComponentActivity) : RecyclerView.ViewHolder(row) {
        val leftChatView = row.findViewById<LinearLayout>(R.id.left_chat_view)!!
        val rightChatView = row.findViewById<LinearLayout>(R.id.right_chat_view)!!
        val leftImageView = row.findViewById<LinearLayout>(R.id.left_image_view)!!
        val leftFileView = row.findViewById<LinearLayout>(R.id.left_file_view)!!
        val leftTextView = row.findViewById<TextView>(R.id.left_chat_text_view)!!
        val rightTextView = row.findViewById<TextView>(R.id.right_chat_text_view)!!
        val leftImage = row.findViewById<ImageView>(R.id.left_chat_image_view)!!
        val leftFileTextView = row.findViewById<TextView>(R.id.left_file_text_view)!!
        val leftImageTextView = row.findViewById<TextView>(R.id.left_image_text_view)!!

        init {
            val themeNumber = MyApp.setAppTheme(activity)
            if(themeNumber == 0) {
                leftChatView.setBackgroundResource(R.drawable.bot_chat_box_light)
                rightChatView.setBackgroundResource(R.drawable.user_chat_box_light)
                leftImageView.setBackgroundResource(R.drawable.bot_chat_box_light)
                leftFileView.setBackgroundResource(R.drawable.bot_chat_box_light)
            }
            // Set long-click listeners on the chat message views
            leftChatView.setOnLongClickListener {
                listener.showContextMenu(it,adapterPosition,true)
                true
            }
            rightChatView.setOnLongClickListener {
                listener.showContextMenu(it,adapterPosition)
                true
            }
            leftImage.setOnClickListener {
                listener.showImage(adapterPosition)
            }
            leftImage.setOnLongClickListener {
                listener.showImageMenu(leftImageView,adapterPosition)
                true
            }
            leftFileView.setOnLongClickListener {
                listener.showFileMenu(it,adapterPosition)
                true
            }
            leftImageView.setOnClickListener {
                listener.showImage(adapterPosition)
            }
            leftImageTextView.setOnClickListener {
                listener.showImage(adapterPosition)
            }
            leftImageTextView.setOnLongClickListener {
                listener.showContextMenu(leftImageView,adapterPosition,
                    isLeft = true,
                    isTextImage = true
                )
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return MyViewHolder(layout, listener, activity)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message = messageList[position]
        val themeNumber = MyApp.setAppTheme(activity)
        val isDark = themeNumber != 0

        when (message.sentBy) {
            "me" -> {
                holder.leftChatView.isVisible=false
                holder.leftImageView.isVisible=false
                holder.rightChatView.isVisible=true
                holder.leftFileView.isVisible=false
                holder.rightTextView.setTextAppearance(MyApp.textAppearance)
                holder.rightTextView.text=message.message
            }
            "user" -> {
                holder.leftChatView.isVisible=false
                holder.leftImageView.isVisible=false
                holder.rightChatView.isVisible=true
                holder.leftFileView.isVisible=false
                holder.rightTextView.setTextAppearance(MyApp.textAppearance)
                holder.rightTextView.text=message.message
            }
            "bot" -> {
                holder.leftChatView.isVisible=true
                holder.leftImageView.isVisible=false
                holder.rightChatView.isVisible=false
                holder.leftFileView.isVisible=false
                holder.leftTextView.setTextAppearance(MyApp.textAppearance)
                holder.leftTextView.text= Html.fromHtml(MyApp.makeBoxCodes(message.message, isDark), Html.FROM_HTML_MODE_COMPACT)
            }
            "model" -> {
                holder.leftChatView.isVisible=true
                holder.leftImageView.isVisible=false
                holder.rightChatView.isVisible=false
                holder.leftFileView.isVisible=false
                holder.leftTextView.setTextAppearance(MyApp.textAppearance)
                holder.leftTextView.text= Html.fromHtml(MyApp.makeBoxCodes(message.message, isDark), Html.FROM_HTML_MODE_COMPACT)
            }
            "assistant" -> {
                holder.leftChatView.isVisible=true
                holder.leftImageView.isVisible=false
                holder.rightChatView.isVisible=false
                holder.leftFileView.isVisible=false
                holder.leftTextView.setTextAppearance(MyApp.textAppearance)
                holder.leftTextView.text= Html.fromHtml(MyApp.makeBoxCodes(message.message, isDark), Html.FROM_HTML_MODE_COMPACT)
            }
            "file" -> {
                holder.leftChatView.isVisible=false
                holder.leftImageView.isVisible=false
                holder.rightChatView.isVisible=false
                holder.leftFileView.isVisible=true
                holder.leftFileTextView.setTextAppearance(MyApp.textAppearance)
                holder.leftFileTextView.text=message.message
            }
            else -> {
                holder.leftChatView.isVisible=false
                holder.leftImageView.isVisible=true
                holder.rightChatView.isVisible=false
                holder.leftFileView.isVisible=false
                holder.leftImageTextView.setTextAppearance(MyApp.textAppearance)
                holder.leftImageTextView.text=listener.showText(position)
                val bitmap = BitmapFactory.decodeFile(message.message)
                holder.leftImage.setImageBitmap(bitmap)
            }
        }
    }

    override fun getItemCount(): Int = messageList.size

    interface MessageListener {
        fun showContextMenu(view: View, position: Int, isLeft: Boolean = false, isTextImage: Boolean = false)
        fun showImageMenu(view: View, position: Int)
        fun showFileMenu(view: View, position: Int)
        fun showImage(position: Int)
        fun showText(position: Int): String
    }
}