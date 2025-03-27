package com.psn.myai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TextAPI {
    var error = ""
    var numberOfToken = 0
    var tokenInput = 0
    var tokenOutput = 0
    private val chatUrl = "https://api.openai.com/v1/chat/completions"
    private val claudeUrl = "https://api.anthropic.com/v1/messages"
    private val geminiUrl = "https://generativelanguage.googleapis.com"
    private var endSentence = ""
    private var systemPrompt = ""
    private var worldUpdateSystemPrompt = "You are an expert article summarizer. Summarize these articles and provide the user with the best answers. Including a few reference links.\n\nArticle"
    private val translatePrompt = "Please translate message from user to"

    private fun initSystemPrompt() {
        if(MyApp.gender=="girl"&&MyApp.appLanguage=="Thai") {
            endSentence = "End sentence with ค่ะ when reply in Thai or คะ after question."
        } else if(MyApp.gender=="cat"&&MyApp.appLanguage=="Thai") {
            endSentence = "End sentence with เมี้ยว or เหมียว when reply in Thai."
        } else if(MyApp.gender=="cat") {
            endSentence = "End sentence with meow!."
        } else if(MyApp.appLanguage=="Thai") {
            endSentence = "End sentence with ครับ when reply in Thai."
        }
        systemPrompt = "You are intelligence ${MyApp.gender} name Wandee. ${MyApp.personality} and you can understand ${MyApp.appLanguage} language very well. $endSentence It is now ${MyApp.currentDate.month} ${MyApp.currentDate.dayOfMonth}, ${MyApp.currentDate.year} ${MyApp.currentTime.hour}:${MyApp.currentTime.minute} O'clock"
    }

    private fun checkUsage(jsonObject: JSONObject) {
        if (jsonObject.has("usage")) {
            val usage = jsonObject.getJSONObject("usage")
            val inputToken = usage.optString("prompt_tokens","")
            val outputToken = usage.optString("completion_tokens","")
            tokenInput = inputToken.toInt()
            tokenOutput = outputToken.toInt()
            numberOfToken = tokenInput + tokenOutput
            if (jsonObject.has("model")) {
                val model = jsonObject.optString("model","")
                MyApp.latestUsage(numberOfToken.toString(),inputToken,outputToken,model)
            }
        }
    }

    private fun checkUsageC(jsonObject: JSONObject) {
        if (jsonObject.has("usage")) {
            val usage = jsonObject.getJSONObject("usage")
            val inputToken = usage.optString("input_tokens","")
            val outputToken = usage.optString("output_tokens","")
            tokenInput = inputToken.toInt()
            tokenOutput = outputToken.toInt()
            numberOfToken = tokenInput + tokenOutput
            if (jsonObject.has("model")) {
                val model = jsonObject.optString("model","")
                MyApp.latestUsage(numberOfToken.toString(),inputToken,outputToken,model)
            }
        }
    }

    private fun checkUsageG(response: GenerateContentResponse, gptModel: String = MyApp.gptModel) {
        val usage = response.usageMetadata
        if (usage != null) {
            tokenInput = usage.promptTokenCount
            tokenOutput = usage.candidatesTokenCount
            numberOfToken = tokenInput + tokenOutput
            MyApp.latestUsage(numberOfToken.toString(),tokenInput.toString(),tokenOutput.toString(),gptModel)
        }
    }

    private fun checkUsageAudio(jsonObject: JSONObject) {
        if (jsonObject.has("usageMetadata")) {
            val usage = jsonObject.getJSONObject("usageMetadata")
            val inputToken = usage.optString("promptTokenCount","")
            val outputToken = usage.optString("candidatesTokenCount","")
            if(!inputToken.isNullOrEmpty()) tokenInput = inputToken.toInt()
            if(!outputToken.isNullOrEmpty()) tokenOutput = outputToken.toInt()
            numberOfToken = tokenInput + tokenOutput
            MyApp.latestUsage(numberOfToken.toString(),inputToken,outputToken,MyApp.gptModel)
        }
    }

    private fun safetySetting(): List<SafetySetting> {
        val harassmentSafety = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH)
        val hateSpeechSafety = SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH)
        val sexualSafety = SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE)
        val dangerousSafety = SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)

        return listOf(harassmentSafety, hateSpeechSafety, sexualSafety, dangerousSafety)
    }

    private fun errorCheck(response: Response) {
        val responseData = response.body?.string()
        try {
            val jsonObject = JSONObject(responseData.toString())
            // Handle different HTTP status codes
            var errorMessage = ""
            if (jsonObject.has("error")) {
                val error = jsonObject.getJSONObject("error")
                errorMessage = error.optString("message","")
            }
            error = "Error code ${response.code}: $errorMessage"
        } catch (e: JSONException) {
            error = "Error code ${response.code}: $e"
        } catch (e: Exception) {
            error = "Error code ${response.code}: $e"
        }
    }

    private fun geminiError(text: String) {
        error = if(text.contains("400") && text.contains("User location is not supported")) {
            "${MyApp.gptModel} shared API key may not support in your location. Please use your own Google AI API key."
        } else {
            val text1 = text.substringAfter("{")
            val text2 = text1.substringBeforeLast("}")
            try {
                val jsonObject = JSONObject("{$text2}")
                val errorObj = jsonObject.getJSONObject("error")
                val errorCode = errorObj.optString("code", "")
                val errorMessage = errorObj.optString("message","")
                "Error code: $errorCode\nError message: $errorMessage"
            } catch (e: JSONException) {
                text
            }
        }
    }

    fun testAPI(context: Context, question: String, apiKey: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.getGPTList(context,0))
        json.put("messages", messagesArray)
        json.put("temperature", 0.3f)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(chatUrl)
            .post(body)
            .header("Authorization", "Bearer $apiKey")
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun chatGPT(question: String, callback: (String?) -> Unit) {
        initSystemPrompt()

        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", systemPrompt)
        messagesArray.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("temperature", MyApp.temperature)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(chatUrl)
            .post(body)
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun chatOpenAI(question: String, chatHistory: List<Message>, callback: (String?) -> Unit) {
        initSystemPrompt()

        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", systemPrompt)
        messagesArray.put(systemMessage)

        if(chatHistory.isNotEmpty()) {
            for(i in chatHistory.indices) {
                val role = chatHistory[i].sentBy
                val message = chatHistory[i].message
                val chatMessage = JSONObject()
                chatMessage.put("role",role)
                chatMessage.put("content",message)
                messagesArray.put(chatMessage)
            }
        }

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("temperature", MyApp.temperature)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(chatUrl)
            .post(body)
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun worldUpdate(question: String, information: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", "$worldUpdateSystemPrompt$information")
        messagesArray.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("temperature", 0.5f)
        json.put("max_tokens", 4000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(chatUrl)
            .post(body)
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun vision(base64Image: String, prompt: String, imageType: String, detail:String = "low", callback: (String?) -> Unit) {
        val messagesArray = JSONArray()
        val userMessage = JSONObject()
        userMessage.put("role", "user")

        val contentArray = JSONArray()
        val contentMessage = JSONObject()
        contentMessage.put("type", "text")
        contentMessage.put("text",prompt)
        contentArray.put(contentMessage)

        val contentImage = JSONObject()
        contentImage.put("type","image_url")
        val urlObject = JSONObject()
        urlObject.put("url","data:$imageType;base64,$base64Image")
        urlObject.put("detail",detail)
        contentImage.put("image_url",urlObject)
        contentArray.put(contentImage)

        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)
        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(chatUrl)
            .post(body)
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun translation(question: String, language: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", "$translatePrompt $language")
        messagesArray.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("temperature", 0.5f)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(chatUrl)
            .post(body)
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun search(topic: String, limit: Int, callback: (String?) -> Unit) {
        val url = "https://real-time-web-search.p.rapidapi.com/search"
        val queryArray = JSONArray()
        queryArray.put(topic)
        val json = JSONObject()
        json.put("queries", queryArray)
        val max = if(limit < 1) {
            1
        } else if(limit > 300){
            300
        } else {
            limit
        }
        json.put("limit", max.toString())

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("X-RapidAPI-Key", MyApp.rapidAPIkey)
            .header("X-RapidAPI-Host", "real-time-web-search.p.rapidapi.com")
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
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            val data = dataArray.getJSONObject(0)
                            val results = data.getJSONArray("results")
                            val maxData = results.length()
                            val reply = StringBuilder()
                            reply.append("\n\n")

                            for (i in 0 until maxData) {
                                val result = results.getJSONObject(i)
                                val title = result.optString("title", "")
                                val snippet = result.optString("snippet", "")
                                val urlText = result.optString("url", "")

                                reply.append("Title: $title\n\n")
                                    .append("$snippet\n\n")
                                    .append("Reference: $urlText\n\n\n")
                            }
                            callback(reply.toString())
                        } else {
                            callback("No data")
                        }
                    } catch (e: JSONException) {
                        error = "JSON parsing error: ${e.message}"
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error = "Network error: ${e.message}"
                callback(null)
            }
        })
    }

    suspend fun testGemini(context: Context, question: String, apiKey: String):String? {
        val config = generationConfig {
            temperature = 0.3f
            maxOutputTokens = 2000
            responseMimeType = "text/plain"
        }

        val generativeModel = GenerativeModel(
            modelName = MyApp.getGPTList(context,2),
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySetting(),
        )

        try {
            val response = generativeModel.generateContent(question)
            checkUsageG(response, MyApp.getGPTList(context,2))
            return response.text
        } catch (e:GoogleGenerativeAIException) {
            geminiError(e.message.toString())
            return null
        }
    }

    suspend fun gemini(question: String):String? {
        initSystemPrompt()

        val config = generationConfig {
            temperature = MyApp.temperature
            maxOutputTokens = 2000
            responseMimeType = "text/plain"
        }

        val generativeModel = GenerativeModel(
            modelName = MyApp.gptModel,
            apiKey = MyApp.geminiAPIkey,
            generationConfig = config,
            safetySettings = safetySetting(),
            systemInstruction = content { text(systemPrompt) },
            tools = listOf(Tool.CODE_EXECUTION)
        )

        try {
            val response = generativeModel.generateContent(question)
            checkUsageG(response)
            return response.text
        } catch (e:GoogleGenerativeAIException) {
            geminiError(e.message.toString())
            return null
        }
    }

    suspend fun geminiChat(question: String, chatHistory: List<Content>):String? {
        initSystemPrompt()

        val config = generationConfig {
            temperature = MyApp.temperature
            maxOutputTokens = 2000
            responseMimeType = "text/plain"
        }

        val generativeModel = GenerativeModel(
            modelName = MyApp.gptModel,
            apiKey = MyApp.geminiAPIkey,
            generationConfig = config,
            safetySettings = safetySetting(),
            systemInstruction = content { text(systemPrompt) },
            tools = listOf(Tool.CODE_EXECUTION)
        )

        try {
            val chat = generativeModel.startChat(chatHistory)
            val response = chat.sendMessage(question)
            checkUsageG(response)
            return response.text
        } catch (e:GoogleGenerativeAIException) {
            geminiError(e.message.toString())
            return null
        }
    }

    suspend fun geminiGame(systemPrompt:String, question: String, chatHistory: List<Content>):String? {
        val config = generationConfig {
            temperature = 1.5f
            maxOutputTokens = 8192
            responseMimeType = "text/plain"
        }

        val generativeModel = GenerativeModel(
            modelName = MyApp.gptModel,
            apiKey = MyApp.geminiAPIkey,
            generationConfig = config,
            safetySettings = safetySetting(),
            systemInstruction = content { text(systemPrompt) },
            tools = listOf(Tool.CODE_EXECUTION)
        )

        try {
            val chat = generativeModel.startChat(chatHistory)
            val response = chat.sendMessage(question)
            checkUsageG(response)
            return response.text
        } catch (e:GoogleGenerativeAIException) {
            geminiError(e.message.toString())
            return null
        }
    }

    fun fileG(context: Context, prompt: String, base64File: String, fileType: String, callback: (String?) -> Unit) {
        val url = if(MyApp.gptModel == MyApp.getGPTList(context,8)) {
            "$geminiUrl/v1alpha/models/${MyApp.gptModel}:generateContent?key=${MyApp.geminiAPIkey}"
        } else {
            "$geminiUrl/v1beta/models/${MyApp.gptModel}:generateContent?key=${MyApp.geminiAPIkey}"
        }
        val contentsArray = JSONArray()
        val content = JSONObject()
        content.put("role", "user")

        val partsArray = JSONArray()
        val inlineData = JSONObject()
        val fileObject = JSONObject()
        fileObject.put("mimeType", fileType)
        fileObject.put("data",base64File)
        inlineData.put("inlineData",fileObject)
        partsArray.put(inlineData)

        val textData = JSONObject()
        textData.put("text",prompt)
        partsArray.put(textData)
        content.put("parts",partsArray)
        contentsArray.put(content)

        val generativeObject = JSONObject()
        generativeObject.put("temperature", MyApp.temperature)
        generativeObject.put("maxOutputTokens", 2000)
        generativeObject.put("responseMimeType", "text/plain")

        val json = JSONObject()
        json.put("contents", contentsArray)
        json.put("generationConfig",generativeObject)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
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
                        val dataArray = jsonObject.getJSONArray("candidates")
                        val data = dataArray.getJSONObject(0)
                        val outputContent = data.getJSONObject("content")
                        val partArray = outputContent.getJSONArray("parts")
                        val part = partArray.getJSONObject(0)
                        val text = part.optString("text","")

                        callback(text)
                        checkUsageAudio(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun geminiThink(question: String, chatHistory: List<Message>, callback: (String?) -> Unit) {
        initSystemPrompt()
        val url = "$geminiUrl/v1alpha/models/${MyApp.gptModel}:generateContent?key=${MyApp.geminiAPIkey}"
        val contentsArray = JSONArray()
        val systemObject = JSONObject()
        val partObject = JSONObject()
        partObject.put("text",systemPrompt)
        systemObject.put("parts",partObject)

        if(chatHistory.isNotEmpty()) {
            for(i in chatHistory.indices) {
                val role = if(chatHistory[i].sentBy == "assistant") {
                    "model"
                } else {
                    chatHistory[i].sentBy
                }
                val message = chatHistory[i].message
                val content1 = JSONObject()
                content1.put("role",role)
                val textData1 = JSONObject()
                textData1.put("text",message)
                val partsArray1 = JSONArray()
                partsArray1.put(textData1)
                content1.put("parts",partsArray1)
                contentsArray.put(content1)
            }
        }

        val content2 = JSONObject()
        content2.put("role", "user")
        val textData2 = JSONObject()
        textData2.put("text",question)
        val partsArray2 = JSONArray()
        partsArray2.put(textData2)
        content2.put("parts",partsArray2)
        contentsArray.put(content2)

        val generativeObject = JSONObject()
        generativeObject.put("temperature", MyApp.temperature)
        generativeObject.put("maxOutputTokens", 2000)
        generativeObject.put("responseMimeType", "text/plain")

        val json = JSONObject()
        json.put("system_instruction", systemObject)
        json.put("contents", contentsArray)
        json.put("generationConfig",generativeObject)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
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
                        val dataArray = jsonObject.getJSONArray("candidates")
                        val data = dataArray.getJSONObject(0)
                        val outputContent = data.getJSONObject("content")
                        val partArray = outputContent.getJSONArray("parts")
                        val part = partArray.getJSONObject(0)
                        val text = part.optString("text","")

                        callback(text)
                        checkUsageAudio(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    suspend fun worldUpdateG(question: String, information: String):String? {
        val config = generationConfig {
            temperature = 0.5f
            maxOutputTokens = 4000
            responseMimeType = "text/plain"
        }

        val generativeModel = GenerativeModel(
            modelName = MyApp.gptModel,
            apiKey = MyApp.geminiAPIkey,
            generationConfig = config,
            safetySettings = safetySetting(),
            systemInstruction = content { text("$worldUpdateSystemPrompt$information") },
        )

        try {
            val response = generativeModel.generateContent(question)
            checkUsageG(response)
            return response.text
        } catch (e:GoogleGenerativeAIException) {
            geminiError(e.message.toString())
            return null
        }
    }

    fun geminiThinkWorldUpdate(question: String, information: String, callback: (String?) -> Unit) {
        val url = "$geminiUrl/v1alpha/models/${MyApp.gptModel}:generateContent?key=${MyApp.geminiAPIkey}"
        val systemObject = JSONObject()
        val partObject = JSONObject()
        partObject.put("text","$worldUpdateSystemPrompt$information")
        systemObject.put("parts",partObject)

        val contentJSONObject = JSONObject()
        val partObject2 = JSONObject()
        partObject2.put("text",question)
        contentJSONObject.put("parts",partObject2)

        val generativeObject = JSONObject()
        generativeObject.put("temperature", 0.5f)
        generativeObject.put("maxOutputTokens", 4000)
        generativeObject.put("responseMimeType", "text/plain")

        val json = JSONObject()
        json.put("system_instruction", systemObject)
        json.put("contents", contentJSONObject)
        json.put("generationConfig",generativeObject)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
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
                        val dataArray = jsonObject.getJSONArray("candidates")
                        val data = dataArray.getJSONObject(0)
                        val outputContent = data.getJSONObject("content")
                        val partArray = outputContent.getJSONArray("parts")
                        val part = partArray.getJSONObject(0)
                        val text = part.optString("text","")

                        callback(text)
                        checkUsageAudio(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    suspend fun translationG(question: String, language: String):String? {
        val config = generationConfig {
            temperature = 0.5f
            maxOutputTokens = 2000
            responseMimeType = "text/plain"
        }

        val generativeModel = GenerativeModel(
            modelName = MyApp.gptModel,
            apiKey = MyApp.geminiAPIkey,
            generationConfig = config,
            safetySettings = safetySetting(),
            systemInstruction = content { text("$translatePrompt $language") }
        )

        try {
            val response = generativeModel.generateContent(question)
            checkUsageG(response)
            return response.text
        } catch (e:GoogleGenerativeAIException) {
            geminiError(e.message.toString())
            return null
        }
    }

    fun geminiThinkTranslation(question: String, language: String, callback: (String?) -> Unit) {
        val url = "$geminiUrl/v1alpha/models/${MyApp.gptModel}:generateContent?key=${MyApp.geminiAPIkey}"
        val systemObject = JSONObject()
        val partObject = JSONObject()
        partObject.put("text","$translatePrompt $language")
        systemObject.put("parts",partObject)

        val contentJSONObject = JSONObject()
        val partObject2 = JSONObject()
        partObject2.put("text",question)
        contentJSONObject.put("parts",partObject2)

        val generativeObject = JSONObject()
        generativeObject.put("temperature", 0.5f)
        generativeObject.put("maxOutputTokens", 2000)
        generativeObject.put("responseMimeType", "text/plain")

        val json = JSONObject()
        json.put("system_instruction", systemObject)
        json.put("contents", contentJSONObject)
        json.put("generationConfig",generativeObject)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
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
                        val dataArray = jsonObject.getJSONArray("candidates")
                        val data = dataArray.getJSONObject(0)
                        val outputContent = data.getJSONObject("content")
                        val partArray = outputContent.getJSONArray("parts")
                        val part = partArray.getJSONObject(0)
                        val text = part.optString("text","")

                        callback(text)
                        checkUsageAudio(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun testClaude(context: Context, question: String, apiKey: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()

        val userMessage = JSONObject()
        userMessage.put("role", "user")

        val content = JSONObject()
        content.put("type","text")
        content.put("text",question)

        val contentArray = JSONArray()
        contentArray.put(content)

        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.getGPTList(context,4))
        json.put("messages", messagesArray)
        json.put("temperature", 0.3f)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(claudeUrl)
            .post(body)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
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
                        if (jsonObject.has("content")) {
                            val outputContentArray = jsonObject.getJSONArray("content")
                            if (outputContentArray.length() > 0) {
                                val outputContent = outputContentArray.getJSONObject(0)
                                val reply = outputContent.optString("text", "")
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your Anthropic API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsageC(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun claude(question: String, callback: (String?) -> Unit) {
        initSystemPrompt()

        val messagesArray = JSONArray()

        val userMessage = JSONObject()
        userMessage.put("role", "user")

        val content = JSONObject()
        content.put("type","text")
        content.put("text",question)

        val contentArray = JSONArray()
        contentArray.put(content)

        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("system", systemPrompt)
        json.put("messages", messagesArray)
        json.put("temperature", MyApp.temperature)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(claudeUrl)
            .post(body)
            .header("x-api-key", MyApp.claudeAPIkey)
            .header("anthropic-version", "2023-06-01")
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
                        if (jsonObject.has("content")) {
                            val outputContentArray = jsonObject.getJSONArray("content")
                            if (outputContentArray.length() > 0) {
                                val outputContent = outputContentArray.getJSONObject(0)
                                val reply = outputContent.optString("text", "")
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your Anthropic API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsageC(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun claudeChat(question: String, chatHistory: List<Message>, callback: (String?) -> Unit) {
        initSystemPrompt()

        val messagesArray = JSONArray()

        if(chatHistory.isNotEmpty()) {
            for(i in chatHistory.indices) {
                val role = chatHistory[i].sentBy
                val message = chatHistory[i].message
                val chatMessage = JSONObject()
                chatMessage.put("role",role)
                chatMessage.put("content",message)
                messagesArray.put(chatMessage)
            }
        }

        val userMessage = JSONObject()
        userMessage.put("role", "user")

        val content = JSONObject()
        content.put("type","text")
        content.put("text",question)

        val contentArray = JSONArray()
        contentArray.put(content)

        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("system", systemPrompt)
        json.put("messages", messagesArray)
        json.put("temperature", MyApp.temperature)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(claudeUrl)
            .post(body)
            .header("x-api-key", MyApp.claudeAPIkey)
            .header("anthropic-version", "2023-06-01")
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
                        if (jsonObject.has("content")) {
                            val outputContentArray = jsonObject.getJSONArray("content")
                            if (outputContentArray.length() > 0) {
                                val outputContent = outputContentArray.getJSONObject(0)
                                val reply = outputContent.optString("text", "")
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your Anthropic API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsageC(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun claudeVision(base64Image: String, prompt: String, imageType: String, callback: (String?) -> Unit) {
        initSystemPrompt()

        val messagesArray = JSONArray()

        val userMessage = JSONObject()
        userMessage.put("role", "user")

        val contentArray = JSONArray()
        val imageObject = JSONObject()
        imageObject.put("type","image")

        val sourceObject = JSONObject()
        sourceObject.put("type","base64")
        sourceObject.put("media_type",imageType)
        sourceObject.put("data",base64Image)
        imageObject.put("source",sourceObject)

        contentArray.put(imageObject)

        val textObject = JSONObject()
        textObject.put("type","text")
        textObject.put("text",prompt)

        contentArray.put(textObject)

        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("system", systemPrompt)
        json.put("messages", messagesArray)
        json.put("temperature", MyApp.temperature)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(claudeUrl)
            .post(body)
            .header("x-api-key", MyApp.claudeAPIkey)
            .header("anthropic-version", "2023-06-01")
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
                        if (jsonObject.has("content")) {
                            val outputContentArray = jsonObject.getJSONArray("content")
                            if (outputContentArray.length() > 0) {
                                val outputContent = outputContentArray.getJSONObject(0)
                                val reply = outputContent.optString("text", "")
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your Anthropic API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsageC(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun claudeWorldUpdate(question: String, information: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()

        val userMessage = JSONObject()
        userMessage.put("role", "user")

        val content = JSONObject()
        content.put("type","text")
        content.put("text",question)

        val contentArray = JSONArray()
        contentArray.put(content)

        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("system", "$worldUpdateSystemPrompt$information")
        json.put("messages", messagesArray)
        json.put("temperature", 0.5f)
        json.put("max_tokens", 4000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(claudeUrl)
            .post(body)
            .header("x-api-key", MyApp.claudeAPIkey)
            .header("anthropic-version", "2023-06-01")
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
                        if (jsonObject.has("content")) {
                            val outputContentArray = jsonObject.getJSONArray("content")
                            if (outputContentArray.length() > 0) {
                                val outputContent = outputContentArray.getJSONObject(0)
                                val reply = outputContent.optString("text", "")
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your Anthropic API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsageC(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun claudeTranslation(question: String, language: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()

        val userMessage = JSONObject()
        userMessage.put("role", "user")

        val content = JSONObject()
        content.put("type","text")
        content.put("text",question)

        val contentArray = JSONArray()
        contentArray.put(content)

        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("system", "$translatePrompt $language")
        json.put("messages", messagesArray)
        json.put("temperature", 0.5f)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(claudeUrl)
            .post(body)
            .header("x-api-key", MyApp.claudeAPIkey)
            .header("anthropic-version", "2023-06-01")
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
                        if (jsonObject.has("content")) {
                            val outputContentArray = jsonObject.getJSONArray("content")
                            if (outputContentArray.length() > 0) {
                                val outputContent = outputContentArray.getJSONObject(0)
                                val reply = outputContent.optString("text", "")
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        } else {
                            error = "Error: Your Anthropic API Key may be wrong or expire or server down!"
                            callback(null)
                        }
                        checkUsageC(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun crbSafety(context: Context, question: String, callback: (String?) -> Unit) {
        val systemPrompt = "You will review the user's message. If it violates the rules, reply with \"No. You cannot.\" Otherwise, respond with the same message the user sent but if it is none English then translate it to English. The rules are as follows:\n" +
                "1. In any way that violates any applicable national, federal, state, local or international law or regulation.\n" +
                "2. For the purpose of exploiting, harming or attempting to exploit or harm minors in any way; including but not limited to the solicitation, creation, acquisition, or dissemination of child exploitative content.\n" +
                "3. To generate or disseminate verifiably false information and/or content with the purpose of harming others.\n" +
                "4. To generate or disseminate personal identifiable information that can be used to harm an individual.\n" +
                "5. To harass, abuse, threaten, stalk, or bully individuals or groups of individuals.\n" +
                "6. To create non-consensual nudity or illegal pornographic content.\n" +
                "7. For fully automated decision making that adversely impacts an individual's legal rights or otherwise creates or modifies a binding, enforceable obligation.\n" +
                "8. Generating or facilitating large-scale disinformation campaigns."

        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", systemPrompt)
        messagesArray.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.getGPTList(context,9))
        json.put("messages", messagesArray)
        json.put("temperature", 0.2f)
        json.put("top_p", 1.0f)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(MyApp.CRB_URL)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.CRB_API_KEY}")
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun llama(question: String, callback: (String?) -> Unit) {
        initSystemPrompt()

        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", systemPrompt)
        messagesArray.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("temperature", MyApp.temperature)
        json.put("top_p", 1.0f)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(MyApp.llamaUrl)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.llamaAPIkey}")
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun llamaChat(question: String, chatHistory: List<Message>, callback: (String?) -> Unit) {
        initSystemPrompt()

        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", systemPrompt)
        messagesArray.put(systemMessage)

        if(chatHistory.isNotEmpty()) {
            for(i in chatHistory.indices) {
                val role = chatHistory[i].sentBy
                val message = chatHistory[i].message
                val chatMessage = JSONObject()
                chatMessage.put("role",role)
                chatMessage.put("content",message)
                messagesArray.put(chatMessage)
            }
        }

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("temperature", MyApp.temperature)
        json.put("top_p", 1.0f)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(MyApp.llamaUrl)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.llamaAPIkey}")
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun llamaVision(base64Image: String, prompt: String, imageType: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()
        val userMessage = JSONObject()
        userMessage.put("role", "user")

        val contentArray = JSONArray()
        val contentMessage = JSONObject()
        contentMessage.put("type", "text")
        contentMessage.put("text",prompt)
        contentArray.put(contentMessage)

        val contentImage = JSONObject()
        contentImage.put("type","image_url")
        val urlObject = JSONObject()
        urlObject.put("url","data:$imageType;base64,$base64Image")
        contentImage.put("image_url",urlObject)
        contentArray.put(contentImage)

        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)
        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(MyApp.llamaUrl)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.llamaAPIkey}")
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun llamaWorldUpdate(question: String, information: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", "$worldUpdateSystemPrompt$information")
        messagesArray.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("temperature", 0.5f)
        json.put("top_p", 1.0f)
        json.put("max_tokens", 4000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(MyApp.llamaUrl)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.llamaAPIkey}")
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }

    fun llamaTranslation(question: String, language: String, callback: (String?) -> Unit) {
        val messagesArray = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", "$translatePrompt $language")
        messagesArray.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", question)
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", MyApp.gptModel)
        json.put("messages", messagesArray)
        json.put("temperature", 0.5f)
        json.put("top_p", 1.0f)
        json.put("max_tokens", 2000)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(MyApp.llamaUrl)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.llamaAPIkey}")
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
                        if (jsonObject.has("choices")) {
                            val choicesArray = jsonObject.getJSONArray("choices")
                            if (choicesArray.length() > 0) {
                                val choice = choicesArray.getJSONObject(0)
                                val message = choice.getJSONObject("message")
                                val reply = message.optString("content", "") // Use optString to handle missing "content" field
                                callback(reply)
                            } else {
                                error = "Error: No message!"
                                callback(null)
                            }
                        }
                        checkUsage(jsonObject)
                    } catch (e: JSONException) {
                        error=e.toString()
                        callback(null)
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                callback(null)
            }
        })
    }
}