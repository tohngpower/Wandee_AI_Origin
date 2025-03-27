package com.psn.myai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.util.*

class AIAssistant(private val context: Context, private val listener: AssistantListener) :
    TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech = TextToSpeech(context, this,"com.google.android.tts")
    lateinit var ttsEngine: String

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            when (textToSpeech.setLanguage(Locale.getDefault())) {
                TextToSpeech.LANG_MISSING_DATA -> {
                    showLanguageMissingData()
                }
                TextToSpeech.LANG_NOT_SUPPORTED -> {
                    textToSpeech.setLanguage(Locale.ENGLISH)
                    listener.onInitCompleted()
                    val text = "Not support"
                    ttsEngine = textToSpeech.engines.toString() + "\n\n" + textToSpeech.voice + "\n\n$text"
                }
                else -> {
                    var desiredVoice: Voice?
                    var voiceName = ""
                    val availableVoices = textToSpeech.voices
                    val desiredVoiceName = when (MyApp.appLanguage) {
                        "Thai" -> {
                            when(MyApp.AI) {
                                2 -> "th-th-x-mol-local"
                                else -> "th-TH-language"
                            }
                        }
                        "English" -> {
                            when (MyApp.AI) {
                                2 -> "en-us-x-tpf-local"
                                else -> "en-US-language"
                            }
                        }
                        "Chinese" -> {
                            when (MyApp.AI) {
                                2 -> "zh-CN-language"
                                else -> "zh-TW-language"
                            }
                        }
                        "Japanese" -> {
                            when (MyApp.AI) {
                                2 -> "ja-jp-x-jab-local"
                                else -> "ja-JP-language"
                            }
                        }
                        else -> ""
                    }
                    for (voice in availableVoices) {
                        voiceName+=voice.name + "\n"
                        if (voice.name == desiredVoiceName) {
                            desiredVoice = voice
                            textToSpeech.setVoice(desiredVoice)
                        }
                    }
                    listener.onInitCompleted()
                    val text = Locale.getDefault().toString()
                    ttsEngine = textToSpeech.engines.toString() + "\n\n" + textToSpeech.voice + "\n\nLanguage: " + text.substring(0..1) + "\n\nAvailable voice list\n$voiceName"
                }
            }
        } else {
            Toast.makeText(context, "Text-to-Speech initialization failed.", Toast.LENGTH_SHORT).show()
        }
    }

    fun speak(text: String) {
        val params = Bundle()
        val utteranceId = UUID.randomUUID().toString()

        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.9f) // Set volume to maximum

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        textToSpeech.stop()
    }

    fun setAIVoice(ai: Int) {
        when (ai) {
            2 -> {
                textToSpeech.setSpeechRate(1.25f)
                textToSpeech.setPitch(1.1f)
            }
            else -> {
                textToSpeech.setSpeechRate(1.0f)
                textToSpeech.setPitch(0.5f)
            }
        }
    }

    fun destroy() {
        textToSpeech.shutdown()
    }

    interface AssistantListener {
        fun onInitCompleted()
        fun onStartSpeech()
        fun onSpeechCompleted()
        fun onSpeechError(utteranceId: String?, errorCode: Int?)
    }

    fun setUtteranceProgressListener() {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                listener.onStartSpeech()
            }

            override fun onDone(utteranceId: String?) {
                listener.onSpeechCompleted()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                // Handle error if needed, but preferably use onError(String, int)
                listener.onSpeechError(utteranceId, null)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                // Handle error based on errorCode
                listener.onSpeechError(utteranceId, errorCode)
            }
        })
    }

    // Function to show a message and prompt for download
    private fun showLanguageMissingData() {
        val message = "The selected language is not supported. Would you like to download it?"

        val dialog = AlertDialog.Builder(context)
        dialog.setMessage(message)
        dialog.setPositiveButton("Download") { _, _ ->
            openTTSLanguageDownloadSettings()
        }
        dialog.setNegativeButton("Cancel") { _, _ ->
            // Handle cancel action if needed
        }
        dialog.show()
    }

    private fun openTTSLanguageDownloadSettings() {
        val intent = Intent()
        intent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
        context.startActivity(intent)
    }

    fun saveTtsToFile(text: String, fileName: String, onComplete: (Boolean) -> Unit) {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val audioFile = File(directory, "$fileName.wav")
        val result = textToSpeech.synthesizeToFile(text, null, audioFile, UUID.randomUUID().toString())

        if (result == TextToSpeech.SUCCESS) {
            onComplete(true)
        } else {
            onComplete(false)
        }
    }
}