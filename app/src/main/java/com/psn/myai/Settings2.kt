package com.psn.myai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateMargins

class Settings2 : ComponentActivity() {
    private lateinit var contentContainer: ConstraintLayout
    private lateinit var txtView: TextView
    private lateinit var ttSpeedText: TextView
    private lateinit var buttonSave: ImageButton
    private lateinit var textSizeSpinner: Spinner
    private lateinit var aLanguageSpinner: Spinner
    private lateinit var ttsVoiceSpinner: Spinner
    private lateinit var ttsSwitch: SwitchCompat
    private lateinit var ttsHDSwitch: SwitchCompat
    private lateinit var ttsSpeedBar: SeekBar
    private lateinit var ttsLayout: LinearLayout
    private val systemDatabase = SystemDatabase(this)
    private var ttsSwitchStatus = "OFF"
    private var ttsModel = "tts-1"
    private var previousAppLanguage: String? = null
    private var themeNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeNumber = MyApp.setAppTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings2)

        contentContainer = findViewById(R.id.settings2)
        txtView = findViewById(R.id.textView25)
        buttonSave = findViewById(R.id.button16)
        textSizeSpinner = findViewById(R.id.textAppearanceSpinner2)
        aLanguageSpinner = findViewById(R.id.languageSpinner2)
        ttSpeedText = findViewById(R.id.voiceSpeedText)
        ttsVoiceSpinner = findViewById(R.id.voiceSpinner)
        ttsSwitch = findViewById(R.id.tts_sw)
        ttsSpeedBar = findViewById(R.id.voiceSpeedBar)
        ttsLayout = findViewById(R.id.openai_tts)
        ttsHDSwitch = findViewById(R.id.audio_hd_sw)

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

        val textSizeAdapter = ArrayAdapter.createFromResource(this,R.array.text_appearances,android.R.layout.simple_spinner_item)
        val languageAdapter = ArrayAdapter.createFromResource(this,R.array.text_language,android.R.layout.simple_spinner_item)
        val voiceAdapter = ArrayAdapter.createFromResource(this,R.array.voice_list,android.R.layout.simple_spinner_item)
        textSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        voiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        textSizeSpinner.adapter = textSizeAdapter
        aLanguageSpinner.adapter = languageAdapter
        ttsVoiceSpinner.adapter = voiceAdapter
        textSizeSpinner.setSelection(MyApp.checkFontSize(systemDatabase))
        val languageNumber = MyApp.checkAppLanguage(systemDatabase,this)
        checkLanguage(languageNumber - 1)
        val textAppearance: Array<String> = resources.getStringArray(R.array.text_size_display)
        txtView.text = textAppearance[languageNumber - 1]
        checkOpenAITTS()

        textSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                MyApp.textAppearance = when (textSizeSpinner.selectedItem.toString()) {
                    "Medium" -> R.style.MediumTextAppearance
                    "Small" -> R.style.SmallTextAppearance
                    else -> R.style.LargeTextAppearance
                }
                txtView.setTextAppearance(MyApp.textAppearance)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing
            }
        }

        aLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                txtView.text = textAppearance[position]
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing
            }
        }

        ttsSwitch.setOnCheckedChangeListener { _,_ ->
            val sw = ttsSwitch.isChecked
            ttsLayout.isVisible = sw
            ttsSwitchStatus = if (sw) {
                "ON"
            } else {
                "OFF"
            }
        }

        ttsHDSwitch.setOnCheckedChangeListener { _,_ ->
            val sw = ttsHDSwitch.isChecked
            ttsModel = if (sw) {
                "tts-1-hd"
            } else {
                "tts-1"
            }
        }

        ttsSpeedBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val speed = progress/100.00f
                ttSpeedText.text = getString(R.string.voice_speed, speed)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Not needed for this example
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Not needed for this example
            }
        })

        buttonSave.setOnClickListener {
            onSave()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(checkChanging()) {
            saveDialog()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        systemDatabase.close()
        super.onDestroy()
    }

    private fun checkLanguage(languageNumber: Int) {
        previousAppLanguage = systemDatabase.searchQuestion("language")
        aLanguageSpinner.setSelection(languageNumber)
    }

    private fun checkOpenAITTS() {
        val apiKeyStatus = MyApp.checkOpenAIAPIkey(systemDatabase)
        ttsSwitch.isVisible = apiKeyStatus

        if(apiKeyStatus) {
            val ttsOn = systemDatabase.searchQuestion("tts switch")
            if(ttsOn == null) {
                systemDatabase.insertQuestion("tts switch","OFF")
                ttsSwitch.isChecked = false
                ttsLayout.isVisible = false
                ttsSwitchStatus = "OFF"
            } else {
                ttsSwitch.isChecked = ttsOn=="ON"
                ttsLayout.isVisible = ttsSwitch.isChecked
                ttsSwitchStatus = ttsOn
            }
        } else {
            ttsSwitch.isChecked = false
            ttsLayout.isVisible = false
            ttsSwitchStatus = "OFF"
        }

        val voiceName = systemDatabase.searchQuestion("voice name")
        if(voiceName == null) {
            systemDatabase.insertQuestion("voice name","nova")
            ttsVoiceSpinner.setSelection(4)
        } else {
            when(voiceName) {
                "echo" -> ttsVoiceSpinner.setSelection(1)
                "fable" -> ttsVoiceSpinner.setSelection(2)
                "onyx" -> ttsVoiceSpinner.setSelection(3)
                "nova" -> ttsVoiceSpinner.setSelection(4)
                "shimmer" -> ttsVoiceSpinner.setSelection(5)
                else -> ttsVoiceSpinner.setSelection(0)
            }
        }

        val audioModel = systemDatabase.searchQuestion("audio model")
        if(audioModel == null) {
            systemDatabase.insertQuestion("audio model","tts-1")
            ttsHDSwitch.isChecked = false
            ttsModel = "tts-1"
        } else {
            ttsHDSwitch.isChecked = audioModel=="tts-1-hd"
            ttsModel = audioModel
        }

        val voiceSpeed = systemDatabase.searchQuestion("voice speed")
        if(voiceSpeed == null) {
            systemDatabase.insertQuestion("voice speed","100")
            ttsSpeedBar.progress = 100
        } else {
            try {
                ttsSpeedBar.progress = voiceSpeed.toInt()
            } catch (e: NumberFormatException) {
                systemDatabase.replaceAnswer("voice speed","100")
                ttsSpeedBar.progress = 100
            }
        }
        val speed = ttsSpeedBar.progress/100.00f
        ttSpeedText.text = getString(R.string.voice_speed, speed)
    }

    private fun onSave() {
        systemDatabase.replaceAnswer("font size",textSizeSpinner.selectedItem.toString())
        systemDatabase.replaceAnswer("language",aLanguageSpinner.selectedItem.toString())
        systemDatabase.replaceAnswer("tts switch",ttsSwitchStatus)
        systemDatabase.replaceAnswer("voice name",ttsVoiceSpinner.selectedItem.toString())
        systemDatabase.replaceAnswer("audio model",ttsModel)
        systemDatabase.replaceAnswer("voice speed",ttsSpeedBar.progress.toString())

        if(previousAppLanguage != aLanguageSpinner.selectedItem.toString()) {
            val i = Intent(this, OnlyTextToSpeech::class.java)
            //Change the activity.
            startActivity(i)
        }
        finish()
    }

    private fun checkChanging(): Boolean {
        if(systemDatabase.searchQuestion("font size")==textSizeSpinner.selectedItem.toString())
            if(systemDatabase.searchQuestion("language")==aLanguageSpinner.selectedItem.toString())
                if(ttsSwitch.isVisible) {
                    if(systemDatabase.searchQuestion("tts switch")==ttsSwitchStatus)
                        if(systemDatabase.searchQuestion("voice name")==ttsVoiceSpinner.selectedItem.toString())
                            if(systemDatabase.searchQuestion("audio model")==ttsModel)
                                if(systemDatabase.searchQuestion("voice speed")==ttsSpeedBar.progress.toString())
                                    return false
                } else {
                    return false
                }

        return true
    }

    private fun saveDialog() {
        val builder = when(themeNumber) {
            0 -> AlertDialog.Builder(this)
            else -> AlertDialog.Builder(this,R.style.DarkAlertDialogTheme)
        }
        builder.setTitle("Save new settings?")
        builder.setNegativeButton("OK") { _, _ ->
            onSave()
        }
        builder.setPositiveButton("Cancel") { _, _ ->
            finish()
        }
        val dialog = builder.create()
        dialog.show()
    }
}