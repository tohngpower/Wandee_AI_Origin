package com.psn.myai

import android.content.ClipboardManager
import android.content.Context

class ClipboardHelper(context: Context) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val clipboardHistory = mutableListOf<String>()

    init {
        // Add a listener to detect clipboard changes
        clipboardManager.addPrimaryClipChangedListener {
            val clipData = clipboardManager.primaryClip
            val item = clipData?.getItemAt(0)?.text?.toString()
            item?.let {
                // Add new clipboard content to the history list
                if (!clipboardHistory.contains(it)) {
                    clipboardHistory.add(it)
                }
            }
        }
    }

    // Get the clipboard history
    fun getClipboardHistory(): List<String> {
        return clipboardHistory
    }
}