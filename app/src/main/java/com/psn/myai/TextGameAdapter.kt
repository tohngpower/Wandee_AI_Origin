package com.psn.myai

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class TextGameAdapter(private val messageList: List<Message>, val context: Context, private val listener: TextGameListener) :
    RecyclerView.Adapter<TextGameAdapter.MyViewHolder>() {

    class MyViewHolder(row: View) : RecyclerView.ViewHolder(row) {
        val gameView = row.findViewById<LinearLayout>(R.id.game_view)!!
        val gameTextView = row.findViewById<TextView>(R.id.game_text_view)!!
        val storyView = row.findViewById<LinearLayout>(R.id.story_image_view)!!
        val storyImage = row.findViewById<ImageView>(R.id.story_image)!!
        val userView = row.findViewById<LinearLayout>(R.id.user_chat_view)!!
        val userTextView = row.findViewById<TextView>(R.id.user_chat_text_view)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.text_game_item, parent, false)
        return MyViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message = messageList[position]

        when (message.sentBy) {
            "user" -> {
                holder.gameView.isVisible=false
                holder.storyView.isVisible=false
                holder.userView.isVisible=true
                holder.userTextView.setTextAppearance(MyApp.textAppearance)
                holder.userTextView.text=message.message
            }
            "model" -> {
                holder.gameView.isVisible=true
                holder.storyView.isVisible=false
                holder.userView.isVisible=false
                holder.gameTextView.setTextAppearance(MyApp.textAppearance)
                holder.gameTextView.text=Html.fromHtml(MyApp.makeTextStyle(message.message)
                    .replace(Regex("```python(.*?)```"),"\uD83C\uDFB2")
                    .replace(Regex("```tool_outputs(.*?)```"),"")
                    .replace("```","")
                    , Html.FROM_HTML_MODE_COMPACT)
            }
            else -> {
                holder.gameView.isVisible=false
                holder.storyView.isVisible=true
                holder.userView.isVisible=false
                holder.storyImage.setImageResource(listener.showImage(position))
            }
        }
    }

    override fun getItemCount(): Int = messageList.size

    interface TextGameListener {
        fun showImage(position: Int): Int
    }
}