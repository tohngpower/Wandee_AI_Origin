package com.psn.myai

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.type.Content
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import java.util.regex.Pattern
import kotlin.random.Random

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        var instance: MyApp? = null
            private set

        var messageList: MutableList<Message> = mutableListOf()
        var contentList: MutableList<Content> = mutableListOf()
        private const val GEMINI_API_KEY = BuildConfig.Gemini_API_KEY
        const val GRQ_API_KEY = BuildConfig.Groq_API_KEY
        const val HF_KEY = BuildConfig.HF_TOKEN
        const val CRB_API_KEY = BuildConfig.Cerebras_KEY
        private const val TPH_API_KEY = BuildConfig.Typhoon_KEY
        var APIkey = ""
        var geminiAPIkey = ""
        var rapidAPIkey = BuildConfig.RAPID_API_KEY1
        var claudeAPIkey = ""
        var llamaAPIkey = ""
        var appLanguage = "Thai"
        var AI = 1
        const val TOTAL_AI = 2
        const val MAX_ERROR = 3
        var textAppearance = R.style.LargeTextAppearance
        var imageSize = "256x256"
        var imageRatio = 1.0f
        var gender = "girl"
        var personality = ""
        var imageData: String? = null
        var imageData2: String? = null
        var imagePrompt = ""
        var currentTime: LocalTime = LocalTime.now()
        var currentDate: LocalDateTime = LocalDateTime.now()
        var checkModel = ""
        var gptModel = "gpt-4o-mini"
        var imageModel = "dall-e-2"
        var temperature = 0.5f
        var isAppInBackground = true
        var notificationFlag = false
        var quotedMessage = ""
        var numberOfGPT = 0
        private const val GRQ_URL = "https://api.groq.com/openai/v1/chat/completions"
        const val CRB_URL = "https://api.cerebras.ai/v1/chat/completions"
        private const val TPH_URL = "https://api.opentyphoon.ai/v1/chat/completions"
        var llamaUrl = ""
        var isSpeaking = false
        var imageResId = R.drawable.bg

        private val ai011 = intArrayOf(
            R.drawable.robot_lip03,
            R.drawable.robot_lip01,
            R.drawable.robot_lip02,
            R.drawable.robot_lip01,
            R.drawable.robot_lip03
        )
        private val ai012 = intArrayOf(
            R.drawable.robot_eye03,
            R.drawable.robot_eye02,
            R.drawable.robot_eye01,
            R.drawable.robot_eye01,
            R.drawable.robot_eye02,
            R.drawable.robot_eye03
        )
        private val ai013 = R.drawable.robot_thinking01
        private val ai014 = intArrayOf(
            R.drawable.robot01,
            R.drawable.robot02,
            R.drawable.robot03,
            R.drawable.robot04,
            R.drawable.robot05,
            R.drawable.robot06,
            R.drawable.robot06,
            R.drawable.robot05,
            R.drawable.robot04,
            R.drawable.robot03,
            R.drawable.robot02,
            R.drawable.robot01
        )
        private val ai015 = R.drawable.robot_thinking01

        private val ai021 = intArrayOf(
            R.drawable.lumine_lip03,
            R.drawable.lumine_lip01,
            R.drawable.lumine_lip02,
            R.drawable.lumine_lip01,
            R.drawable.lumine_lip03
        )
        private val ai022 = intArrayOf(
            R.drawable.lumine_eye03,
            R.drawable.lumine_eye02,
            R.drawable.lumine_eye01,
            R.drawable.lumine_eye01,
            R.drawable.lumine_eye02,
            R.drawable.lumine_eye03
        )
        private val ai023 = R.drawable.lumine_thinking
        private val ai024 = intArrayOf(
            R.drawable.lumine01,
            R.drawable.lumine02,
            R.drawable.lumine03,
            R.drawable.lumine04,
            R.drawable.lumine05,
            R.drawable.lumine06,
            R.drawable.lumine06,
            R.drawable.lumine05,
            R.drawable.lumine04,
            R.drawable.lumine03,
            R.drawable.lumine02,
            R.drawable.lumine01
        )
        private val ai025 = R.drawable.lumine_thinking

        var imageResources = ai011
        var imageResources2 = ai012
        var imageThinking = ai013
        var imageBreathing = ai014
        var imageDatabase = ai015

        fun checkFontSize(systemDatabase: SystemDatabase): Int {
            val fontSize = systemDatabase.searchQuestion("font size")
            return when (fontSize) {
                null -> {
                    systemDatabase.insertQuestion("font size", "Small")
                    0
                }
                "Medium" -> {
                    textAppearance = R.style.MediumTextAppearance
                    1
                }
                "Small" -> {
                    textAppearance = R.style.SmallTextAppearance
                    2
                }
                else -> {
                    textAppearance = R.style.LargeTextAppearance
                    0
                }
            }
        }

        fun setAIResource(systemDatabase: SystemDatabase) {
            val theAI = systemDatabase.searchQuestion("AI")
            when (theAI) {
                "ai02" -> {
                    imageResources = ai021
                    imageResources2 = ai022
                    imageThinking = ai023
                    imageBreathing = ai024
                    imageDatabase = ai025
                    AI = 2
                }
                else -> {
                    systemDatabase.insertQuestion("AI", "ai01")
                    imageResources = ai011
                    imageResources2 = ai012
                    imageThinking = ai013
                    imageBreathing = ai014
                    imageDatabase = ai015
                    AI = 1
                }
            }
        }

        fun checkAIVoice(systemDatabase: SystemDatabase, assistant: AIAssistant) {
            val theAI = systemDatabase.searchQuestion("AI")
            when (theAI) {
                "ai02" -> {
                    assistant.setAIVoice(2)
                }
                else -> {
                    assistant.setAIVoice(1)
                }
            }
        }

        fun checkAIPersonal(systemDatabase: SystemDatabase) {
            val theAI = systemDatabase.searchQuestion("AI")
            when (theAI) {
                "ai02" -> {
                    gender = "girl"
                    personality = "You are helpful assistant and like to give short and clear answer"
                    temperature = 0.7f
                }
                else -> {
                    gender = "robot"
                    personality = "you are serious personality and like to explain more details"
                    temperature = 0.5f
                }
            }
        }

        fun getCurrentTimeOfDay(): String {
            currentTime = LocalTime.now()
            return when {
                currentTime.isAfter(LocalTime.of(5, 0)) && currentTime.isBefore(LocalTime.of(7,0)) -> "time1"
                currentTime.isAfter(LocalTime.of(7, 0)) && currentTime.isBefore(LocalTime.of(11,0)) -> "time2"
                currentTime.isAfter(LocalTime.of(11, 0)) && currentTime.isBefore(LocalTime.of(15,0)) -> "time3"
                currentTime.isAfter(LocalTime.of(15, 0)) && currentTime.isBefore(LocalTime.of(17,0)) -> "time4"
                currentTime.isAfter(LocalTime.of(17, 0)) && currentTime.isBefore(LocalTime.of(19,0)) -> "time5"
                currentTime.isAfter(LocalTime.of(19, 0)) && currentTime.isBefore(LocalTime.of(21,0)) -> "time6"
                currentTime.isAfter(LocalTime.of(21, 0)) && currentTime.isBefore(LocalTime.of(23,59)) -> "time7"
                else -> "time8"
            }
        }

        fun getRandomImageForInterval(interval: String): Int {
            val imageOptions = when (interval) {
                "time1" -> listOf(R.drawable.bg001, R.drawable.bg002, R.drawable.bg003)
                "time2" -> listOf(R.drawable.bg,R.drawable.bg004,R.drawable.bg005,R.drawable.bg006)
                "time3" -> listOf(R.drawable.bg,R.drawable.bg007,R.drawable.bg008,R.drawable.bg009)
                "time4" -> listOf(R.drawable.bg010, R.drawable.bg011, R.drawable.bg012)
                "time5" -> listOf(R.drawable.bg013, R.drawable.bg014, R.drawable.bg015)
                "time6" -> listOf(R.drawable.bg016, R.drawable.bg017, R.drawable.bg018)
                "time7" -> listOf(R.drawable.bg019, R.drawable.bg020, R.drawable.bg021)
                else -> listOf(R.drawable.bg022, R.drawable.bg023, R.drawable.bg024)
            }
            return imageOptions[Random.nextInt(imageOptions.size)]
        }

        fun setAppTheme(activity: ComponentActivity): Int {
            return when (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> {
                    activity.setTheme(R.style.Theme_MyAI)
                    0
                }
                else -> {
                    activity.setTheme(R.style.darkTheme_MyAI)
                    1
                }
            }
        }

        fun checkToken(databaseHelper: DatabaseHelper, textAPI: TextAPI, model: String = gptModel) {
            var detail = "$model last token"
            var token: Int
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, textAPI.numberOfToken.toString())
            } else {
                databaseHelper.replaceAnswer(detail, textAPI.numberOfToken.toString())
            }
            detail = "$model last input token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, textAPI.tokenInput.toString())
            } else {
                databaseHelper.replaceAnswer(detail, textAPI.tokenInput.toString())
            }
            detail = "$model last output token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, textAPI.tokenOutput.toString())
            } else {
                databaseHelper.replaceAnswer(detail, textAPI.tokenOutput.toString())
            }
            detail = "$model total input token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, textAPI.tokenInput.toString())
            } else {
                token = try {
                    databaseHelper.searchQuestion(detail).toString().toInt()
                } catch(e: NumberFormatException) {
                    0
                }
                textAPI.tokenInput += token
                databaseHelper.replaceAnswer(detail, textAPI.tokenInput.toString())
            }
            detail = "$model total output token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, textAPI.tokenOutput.toString())
            } else {
                token = try {
                    databaseHelper.searchQuestion(detail).toString().toInt()
                } catch(e: NumberFormatException) {
                    0
                }
                textAPI.tokenOutput += token
                databaseHelper.replaceAnswer(detail, textAPI.tokenOutput.toString())
            }
            detail = "$model total token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, textAPI.numberOfToken.toString())
            } else {
                textAPI.numberOfToken = textAPI.tokenOutput + textAPI.tokenInput
                databaseHelper.replaceAnswer(detail, textAPI.numberOfToken.toString())
            }
        }

        fun checkTokenVideo(databaseHelper: DatabaseHelper, uploader: VideoUploader, model: String = gptModel) {
            var detail = "$model last token"
            var token: Int
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, uploader.numberOfToken.toString())
            } else {
                databaseHelper.replaceAnswer(detail, uploader.numberOfToken.toString())
            }
            detail = "$model last input token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, uploader.tokenInput.toString())
            } else {
                databaseHelper.replaceAnswer(detail, uploader.tokenInput.toString())
            }
            detail = "$model last output token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, uploader.tokenOutput.toString())
            } else {
                databaseHelper.replaceAnswer(detail, uploader.tokenOutput.toString())
            }
            detail = "$model total input token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, uploader.tokenInput.toString())
            } else {
                token = try {
                    databaseHelper.searchQuestion(detail).toString().toInt()
                } catch(e: NumberFormatException) {
                    0
                }
                uploader.tokenInput += token
                databaseHelper.replaceAnswer(detail, uploader.tokenInput.toString())
            }
            detail = "$model total output token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, uploader.tokenOutput.toString())
            } else {
                token = try {
                    databaseHelper.searchQuestion(detail).toString().toInt()
                } catch(e: NumberFormatException) {
                    0
                }
                uploader.tokenOutput += token
                databaseHelper.replaceAnswer(detail, uploader.tokenOutput.toString())
            }
            detail = "$model total token"
            if (databaseHelper.searchQuestion(detail) == null) {
                databaseHelper.insertQuestion(detail, uploader.numberOfToken.toString())
            } else {
                uploader.numberOfToken = uploader.tokenOutput + uploader.tokenInput
                databaseHelper.replaceAnswer(detail, uploader.numberOfToken.toString())
            }
        }

        fun checkGPT(systemDatabase: SystemDatabase,context: Context):Int {
            val gptType: Int
            val gpt = systemDatabase.searchQuestion("gpt model")
            gptModel = when (gpt) {
                null -> {
                    val gptList: Array<String> = context.resources.getStringArray(R.array.GPT)
                    numberOfGPT = gptList.size
                    var index = Random.nextInt(0, numberOfGPT)
                    when (index) {
                        0 -> {
                            llamaAPIkey = GRQ_API_KEY
                            llamaUrl = GRQ_URL
                            gptType = 4
                            index = 5
                        }
                        1 -> {
                            llamaAPIkey = GRQ_API_KEY
                            llamaUrl = GRQ_URL
                            gptType = 5
                            index = 6
                        }
                        3 -> gptType = 2
                        4 -> {
                            llamaAPIkey = GRQ_API_KEY
                            llamaUrl = GRQ_URL
                            gptType = 4
                            index = 7
                        }
                        5 -> {
                            llamaAPIkey = GRQ_API_KEY
                            llamaUrl = GRQ_URL
                            gptType = 4
                        }
                        6 -> {
                            llamaAPIkey = GRQ_API_KEY
                            llamaUrl = GRQ_URL
                            gptType = 5
                        }
                        7 -> {
                            llamaAPIkey = GRQ_API_KEY
                            llamaUrl = GRQ_URL
                            gptType = 4
                        }
                        8 -> gptType = 6
                        9 -> {
                            llamaAPIkey = CRB_API_KEY
                            llamaUrl = CRB_URL
                            gptType = 5
                        }
                        10 -> {
                            llamaAPIkey = TPH_API_KEY
                            llamaUrl = TPH_URL
                            gptType = 5
                        }
                        11 -> {
                            llamaAPIkey = GRQ_API_KEY
                            llamaUrl = GRQ_URL
                            gptType = 5
                        }
                        else -> {
                            gptType = 2
                            index = 2
                        }
                    }
                    systemDatabase.insertQuestion("gpt model", gptList[index])
                    gptList[index]
                }
                getGPTList(context,0) -> {
                    gptType = 1 //OpenAI
                    getGPTList(context,0)
                }
                getGPTList(context,1) -> {
                    gptType = 1 //OpenAI
                    getGPTList(context,1)
                }
                getGPTList(context,3) -> {
                    gptType = 2 //GoogleAI
                    getGPTList(context,3)
                }
                getGPTList(context,4) -> {
                    gptType = 3 //ClaudeAI
                    getGPTList(context,4)
                }
                getGPTList(context,5) -> {
                    gptType = 4 //Meta Vision GRQ
                    llamaAPIkey = GRQ_API_KEY
                    llamaUrl = GRQ_URL
                    getGPTList(context,5)
                }
                getGPTList(context,6) -> {
                    gptType = 5 //Meta GRQ
                    llamaAPIkey = GRQ_API_KEY
                    llamaUrl = GRQ_URL
                    getGPTList(context,6)
                }
                getGPTList(context,7) -> {
                    gptType = 4 //Meta Vision GRQ
                    llamaAPIkey = GRQ_API_KEY
                    llamaUrl = GRQ_URL
                    getGPTList(context,7)
                }
                getGPTList(context,8) -> {
                    gptType = 6 //Gemini Think
                    getGPTList(context,8)
                }
                getGPTList(context,9) -> {
                    gptType = 5 //Meta CRB
                    llamaAPIkey = CRB_API_KEY
                    llamaUrl = CRB_URL
                    getGPTList(context,9)
                }
                getGPTList(context,10) -> {
                    gptType = 5 //Meta Typhoon
                    llamaAPIkey = TPH_API_KEY
                    llamaUrl = TPH_URL
                    getGPTList(context,10)
                }
                getGPTList(context,11) -> {
                    gptType = 5 //Deep seek GRQ
                    llamaAPIkey = GRQ_API_KEY
                    llamaUrl = GRQ_URL
                    getGPTList(context,11)
                }
                else -> {
                    gptType = 2
                    getGPTList(context,2)
                }
            }
            return gptType
        }

        fun checkNotification(context: Context, systemDatabase: SystemDatabase) {
            val note = systemDatabase.searchQuestion("notification")
            if (note == null) {
                systemDatabase.insertQuestion("notification", "OFF")
                notificationFlag = false
            } else if (note == "ON") {
                notificationFlag = true
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(
                        context,
                        "Please allow notifications in App permissions!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                notificationFlag = false
            }
        }

        fun checkAPIkey(context: Context, systemDatabase: SystemDatabase): Boolean {
            when (val key = systemDatabase.searchQuestion("apiKEY")) {
                null -> {
                    APIkey = "tempKEY"
                    systemDatabase.insertQuestion("apiKEY", APIkey)
                    val i = Intent(context, APIKeyPage::class.java)
                    //Change the activity.
                    context.startActivity(i)
                    return false
                }
                "tempKEY" -> {
                    val i = Intent(context, APIKeyPage::class.java)
                    //Change the activity.
                    context.startActivity(i)
                    return false
                }
                else -> {
                    APIkey = key
                    return true
                }
            }
        }

        fun checkOpenAIAPIkey(systemDatabase: SystemDatabase): Boolean {
            when (val key = systemDatabase.searchQuestion("apiKEY")) {
                null -> {
                    APIkey = "tempKEY"
                    systemDatabase.insertQuestion("apiKEY", APIkey)
                    return false
                }
                "tempKEY" -> {
                    return false
                }
                else -> {
                    APIkey = key
                    return true
                }
            }
        }

        fun checkGeminiAPIkey(systemDatabase: SystemDatabase): Boolean {
            when (val key = systemDatabase.searchQuestion("GeminiKEY")) {
                null -> {
                    geminiAPIkey = GEMINI_API_KEY
                    systemDatabase.insertQuestion("GeminiKEY", geminiAPIkey)
                    return false
                }
                GEMINI_API_KEY -> {
                    geminiAPIkey = GEMINI_API_KEY
                    return false
                }
                else -> {
                    geminiAPIkey = key
                    return true
                }
            }
        }

        fun checkClaudeAPIkey(systemDatabase: SystemDatabase): Boolean {
            when (val key = systemDatabase.searchQuestion("ClaudeKEY")) {
                null -> {
                    claudeAPIkey = "tempKEY"
                    systemDatabase.insertQuestion("ClaudeKEY", claudeAPIkey)
                    return false
                }
                "tempKEY" -> {
                    return false
                }
                else -> {
                    claudeAPIkey = key
                    return true
                }
            }
        }

        fun checkAppLanguage(systemDatabase: SystemDatabase, context: Context):Int {
            if (systemDatabase.searchQuestion("language") == null) {
                val locale = Locale.getDefault().toString()
                when(locale.substring(0..1)) {
                    "th" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,0))
                        appLanguage = getLanguageList(context,0)
                        return 1
                    }
                    "zh" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,2))
                        appLanguage = getLanguageList(context,2)
                        return 3
                    }
                    "ja" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,3))
                        appLanguage = getLanguageList(context,3)
                        return 4
                    }
                    "ms" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,4))
                        appLanguage = getLanguageList(context,4)
                        return 5
                    }
                    "tl" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,5))
                        appLanguage = getLanguageList(context,5)
                        return 6
                    }
                    "id" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,6))
                        appLanguage = getLanguageList(context,6)
                        return 7
                    }
                    "lo" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,7))
                        appLanguage = getLanguageList(context,7)
                        return 8
                    }
                    "ko" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,8))
                        appLanguage = getLanguageList(context,8)
                        return 9
                    }
                    "vi" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,9))
                        appLanguage = getLanguageList(context,9)
                        return 10
                    }
                    "es" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,10))
                        appLanguage = getLanguageList(context,10)
                        return 11
                    }
                    "fr" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,11))
                        appLanguage = getLanguageList(context,11)
                        return 12
                    }
                    "de" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,12))
                        appLanguage = getLanguageList(context,12)
                        return 13
                    }
                    "it" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,13))
                        appLanguage = getLanguageList(context,13)
                        return 14
                    }
                    "pt" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,14))
                        appLanguage = getLanguageList(context,14)
                        return 15
                    }
                    "ru" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,15))
                        appLanguage = getLanguageList(context,15)
                        return 16
                    }
                    "nl" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,16))
                        appLanguage = getLanguageList(context,16)
                        return 17
                    }
                    "sv" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,17))
                        appLanguage = getLanguageList(context,17)
                        return 18
                    }
                    "no" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,18))
                        appLanguage = getLanguageList(context,18)
                        return 19
                    }
                    "da" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,19))
                        appLanguage = getLanguageList(context,19)
                        return 20
                    }
                    "fi" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,20))
                        appLanguage = getLanguageList(context,20)
                        return 21
                    }
                    "pl" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,21))
                        appLanguage = getLanguageList(context,21)
                        return 22
                    }
                    "tr" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,22))
                        appLanguage = getLanguageList(context,22)
                        return 23
                    }
                    "he" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,23))
                        appLanguage = getLanguageList(context,23)
                        return 24
                    }
                    "ar" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,24))
                        appLanguage = getLanguageList(context,24)
                        return 25
                    }
                    "fa" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,25))
                        appLanguage = getLanguageList(context,25)
                        return 26
                    }
                    "hu" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,26))
                        appLanguage = getLanguageList(context,26)
                        return 27
                    }
                    "cs" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,27))
                        appLanguage = getLanguageList(context,27)
                        return 28
                    }
                    "sk" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,28))
                        appLanguage = getLanguageList(context,28)
                        return 29
                    }
                    "uk" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,29))
                        appLanguage = getLanguageList(context,29)
                        return 30
                    }
                    "ro" -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,30))
                        appLanguage = getLanguageList(context,30)
                        return 31
                    }
                    else -> {
                        systemDatabase.insertQuestion("language", getLanguageList(context,1))
                        appLanguage = getLanguageList(context,1)
                        return 2
                    }
                }
            } else {
                return when(systemDatabase.searchQuestion("language")) {
                    getLanguageList(context,0) -> {
                        appLanguage = getLanguageList(context,0)
                        1
                    }
                    getLanguageList(context,2) -> {
                        appLanguage = getLanguageList(context,2)
                        3
                    }
                    getLanguageList(context,3) -> {
                        appLanguage = getLanguageList(context,3)
                        4
                    }
                    getLanguageList(context,4) -> {
                        appLanguage = getLanguageList(context,4)
                        5
                    }
                    getLanguageList(context,5) -> {
                        appLanguage = getLanguageList(context,5)
                        6
                    }
                    getLanguageList(context,6) -> {
                        appLanguage = getLanguageList(context,6)
                        7
                    }
                    getLanguageList(context,7) -> {
                        appLanguage = getLanguageList(context,7)
                        8
                    }
                    getLanguageList(context,8) -> {
                        appLanguage = getLanguageList(context,8)
                        9
                    }
                    getLanguageList(context,9) -> {
                        appLanguage = getLanguageList(context,9)
                        10
                    }
                    getLanguageList(context,10) -> {
                        appLanguage = getLanguageList(context,10)
                        11
                    }
                    getLanguageList(context,11) -> {
                        appLanguage = getLanguageList(context,11)
                        12
                    }
                    getLanguageList(context,12) -> {
                        appLanguage = getLanguageList(context,12)
                        13
                    }
                    getLanguageList(context,13) -> {
                        appLanguage = getLanguageList(context,13)
                        14
                    }
                    getLanguageList(context,14) -> {
                        appLanguage = getLanguageList(context,14)
                        15
                    }
                    getLanguageList(context,15) -> {
                        appLanguage = getLanguageList(context,15)
                        16
                    }
                    getLanguageList(context,16) -> {
                        appLanguage = getLanguageList(context,16)
                        17
                    }
                    getLanguageList(context,17) -> {
                        appLanguage = getLanguageList(context,17)
                        18
                    }
                    getLanguageList(context,18) -> {
                        appLanguage = getLanguageList(context,18)
                        19
                    }
                    getLanguageList(context,19) -> {
                        appLanguage = getLanguageList(context,19)
                        20
                    }
                    getLanguageList(context,20) -> {
                        appLanguage = getLanguageList(context,20)
                        21
                    }
                    getLanguageList(context,21) -> {
                        appLanguage = getLanguageList(context,21)
                        22
                    }
                    getLanguageList(context,22) -> {
                        appLanguage = getLanguageList(context,22)
                        23
                    }
                    getLanguageList(context,23) -> {
                        appLanguage = getLanguageList(context,23)
                        24
                    }
                    getLanguageList(context,24) -> {
                        appLanguage = getLanguageList(context,24)
                        25
                    }
                    getLanguageList(context,25) -> {
                        appLanguage = getLanguageList(context,25)
                        26
                    }
                    getLanguageList(context,26) -> {
                        appLanguage = getLanguageList(context,26)
                        27
                    }
                    getLanguageList(context,27) -> {
                        appLanguage = getLanguageList(context,27)
                        28
                    }
                    getLanguageList(context,28) -> {
                        appLanguage = getLanguageList(context,28)
                        29
                    }
                    getLanguageList(context,29) -> {
                        appLanguage = getLanguageList(context,29)
                        30
                    }
                    getLanguageList(context,30) -> {
                        appLanguage = getLanguageList(context,30)
                        31
                    }
                    else -> {
                        appLanguage = getLanguageList(context,1)
                        2
                    }
                }
            }
        }

        fun getGPTList(context: Context,index: Int): String {
            val gptList: Array<String> = context.resources.getStringArray(R.array.GPT)
            numberOfGPT = gptList.size
            return gptList[index]
        }

        private fun getLanguageList(context: Context, index: Int): String {
            val languageList: Array<String> = context.resources.getStringArray(R.array.text_language)
            return languageList[index]
        }

        fun splitTextKeepLastDimension(text: String): String {
            val pattern1 = Regex("\\(256x256\\)")
            val pattern2 = Regex("\\(512x512\\)")
            val pattern3 = Regex("\\(1024x1024\\)")
            val pattern4 = Regex("\\(1792x1024\\)")
            val pattern5 = Regex("\\(1024x1792\\)")

            val lastPattern1 = pattern1.findAll(text).lastOrNull()?.range?.first ?: -1
            val lastPattern2 = pattern2.findAll(text).lastOrNull()?.range?.first ?: -1
            val lastPattern3 = pattern3.findAll(text).lastOrNull()?.range?.first ?: -1
            val lastPattern4 = pattern4.findAll(text).lastOrNull()?.range?.first ?: -1
            val lastPattern5 = pattern5.findAll(text).lastOrNull()?.range?.first ?: -1

            val splitText = if (lastPattern1!=-1) {
                imageRatio = 1.0f

                text.substring(0, lastPattern1).trim()
            } else if (lastPattern2!=-1) {
                imageRatio = 1.0f

                text.substring(0, lastPattern2).trim()
            } else if (lastPattern3!=-1) {
                imageRatio = 1.0f

                text.substring(0, lastPattern3).trim()
            } else if (lastPattern4!=-1) {
                imageRatio = 1024.0f/1792.0f

                text.substring(0, lastPattern4).trim()
            } else if (lastPattern5!=-1) {
                imageRatio = 1792.0f/1024.0f

                text.substring(0, lastPattern5).trim()
            } else {
                imageRatio = 1.0f

                text
            }
            return splitText
        }

        fun latestUsage(total:String, input:String, output:String, model:String) {
            checkModel = "<b><u>Latest usage data</u></b><br><br><b>$model</b><br>Total: <i>$total</i> tokens<br>Input: <i>$input</i> tokens<br>Output: <i>$output</i> tokens<br><br><br>"
        }

        fun isValidText(text: String): Boolean {
            val pattern = Pattern.compile("^[a-zA-Z0-9-_]+$") // Match English letters and numbers
            val matcher = pattern.matcher(text)
            return matcher.matches()
        }

        private fun replaceFraction(text: String): String {
            val regex = """\\frac\{([0-9]+)\}\{([0-9]+)\}""".toRegex()
            return regex.replace(text) { matchResult ->
                val numerator = matchResult.groupValues[1]
                val denominator = matchResult.groupValues[2]
                "<sup>$numerator</sup>/<sub>$denominator</sub>"
            }
        }

        fun makeTextStyle(text: String): String {
            return replaceFraction(text)
                .replace(Regex("###\\s+(.*?)\n"),"<h3>$1</h3>\n")
                .replace(Regex("##\\s+(.*?)\n"),"<h2>$1</h2>\n")
                .replace(Regex("#\\s+(.*?)\n"),"<h1>$1</h1>\n")
                .replace("\n* ","<br> • ")
                .replace("  - ","&nbsp;&nbsp;• ")
                .replace("  * ","&nbsp;&nbsp;• ")
                .replace("\n- ","<br> • ")
                .replace("\n","<br>")
                .replace(Regex("\\*\\*\\*(.*?)\\*\\*\\*"), "<b><i>$1</i></b>") // Replace ***text1*** with <b><i>text1</i></b>
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")        // Replace **text2** with <b>text2</b>
                .replace("\\(","(")
                .replace("\\)",")")
                .replace("\\times","x")
                .replace("\\div","/")
                .replace("\\[","[")
                .replace("\\]","]")
                .replace(Regex("\\\\boxed\\{([^}]*)\\}"),"<u><b>$1</b></u>")
        }

        fun makeBoxCodes(text: String, isDark: Boolean = true): String {
            return if(isDark) {
                makeTextStyle(text)
                    .replace(Regex("```(.*?)```"),"<blockquote><font color=\"aqua\">$1</font></blockquote>")
                    .replace("```","")
                    .replace("<think>","<blockquote><font color=\"aqua\">Think<br>")
                    .replace("</think>","</font></blockquote>")
            } else {
                makeTextStyle(text)
                    .replace(Regex("```(.*?)```"),"<blockquote><font color=\"blue\">$1</font></blockquote>")
                    .replace("```","")
                    .replace("<think>","Think<br><blockquote><font color=\"blue\">")
                    .replace("</think>","</font></blockquote>")
            }
        }

        fun levenshteinDistance(str1: String, str2: String): Int {
            val m = str1.length
            val n = str2.length
            // Create a matrix to store the distances
            val distances = Array(m + 1) { IntArray(n + 1) }
            // Initialize the first row and column of the matrix
            for (i in 0..m) {
                distances[i][0] = i
            }
            for (j in 0..n) {
                distances[0][j] = j
            }
            // Calculate the distances
            for (i in 1..m) {
                for (j in 1..n) {
                    val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                    distances[i][j] = minOf(
                        distances[i - 1][j] + 1,   // deletion
                        distances[i][j - 1] + 1,   // insertion
                        distances[i - 1][j - 1] + cost   // substitution
                    )
                }
            }
            return distances[m][n]
        }

        fun getMimeTypeFromFileName(fileName: String): String? {
            val extension = fileName.substringAfterLast(".")
            return when (extension) {
                "c" -> "text/x-c"
                "cpp" -> "text/x-c++"
                "css" -> "text/css"
                "csv" -> "text/csv"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "html" -> "text/html"
                "java" -> "text/x-java"
                "js" -> "text/javascript"
                "json" -> "application/json"
                "md" -> "text/markdown"
                "pdf" -> "application/pdf"
                "php" -> "text/x-php"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "py" -> "text/x-python"
                "rb" -> "text/x-ruby"
                "tex" -> "text/x-tex"
                "ts" -> "application/typescript"
                "txt" -> "text/plain"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "xml" -> "text/xml"
                "png" -> "image/png"
                "jpg" -> "image/jpeg"
                "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "heic" -> "image/heic"
                "heif" -> "image/heif"
                "mp4" -> "video/mp4"
                "m4a" -> "video/mp4"
                "mpeg" -> "video/mpeg"
                "mov" -> "video/mov"
                "avi" -> "video/avi"
                "x-flv" -> "video/x-flv"
                "mpg" -> "video/mpg"
                "webm" -> "video/webm"
                "wmv" -> "video/wmv"
                "3gpp" -> "video/3gpp"
                "wav" -> "audio/wav"
                "mp3" -> "audio/mp3"
                "aiff" -> "audio/aiff"
                "aac" -> "audio/aac"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                else -> null
            }
        }

        fun toggleRapidKey() {
            rapidAPIkey = if(rapidAPIkey == BuildConfig.RAPID_API_KEY1) {
                BuildConfig.RAPID_API_KEY2
            } else {
                BuildConfig.RAPID_API_KEY1
            }
        }
    }
}