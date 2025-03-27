package com.psn.myai

import android.content.Context
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class AudioAPI(val context: Context) {
    var error = ""

    fun tts(voice: String, input: String, model: String, speed: Float, callback: (File?) -> Unit) {
        val client = OkHttpClient()

        // Create the JSON payload
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("input", input)
            put("voice", voice)
            put("speed", speed)
        }

        // Create the request body
        val requestBody = jsonPayload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        // Create the request
        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/speech")
            .addHeader("Authorization", "Bearer ${MyApp.APIkey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(600, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(600, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        // Make the request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                error = e.message.toString()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody != null) {
                        val file = File(context.cacheDir, "temp_audio.mp3")
                        val inputStream = responseBody.byteStream()

                        try {
                            val outputStream = FileOutputStream(file)
                            outputStream.use { out ->
                                inputStream.copyTo(out)
                            }
                            callback(file)
                        } catch (e: IOException) {
                            error = e.message.toString()
                            callback(null)
                        } finally {
                            inputStream.close()
                        }
                    }
                } else {
                    errorCheck(response)
                    callback(null)
                }
            }
        })
    }

    fun transcription(audioFile: File, languageCode: String, callback: (String?) -> Unit) {
        val url = "https://api.groq.com/openai/v1/audio/transcriptions"
        val model = if(languageCode == "en") {
            "distil-whisper-large-v3-en"
        } else {
            "whisper-large-v3-turbo"
        }
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "recording.${audioFile.absolutePath.substringAfterLast(".")}", audioFile.asRequestBody())
        requestBody.addFormDataPart("model", model)
        requestBody.addFormDataPart("language", languageCode)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${MyApp.GRQ_API_KEY}")
            .post(requestBody.build())
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(600, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(600, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val text = jsonObject.optString("text", "")
                        callback(text)
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

    fun translation(audioFile: File, callback: (String?) -> Unit) {
        val url = "https://api.groq.com/openai/v1/audio/translations"
        val model = "whisper-large-v3"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "recording.${audioFile.absolutePath.substringAfterLast(".")}", audioFile.asRequestBody())
        requestBody.addFormDataPart("model", model)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${MyApp.GRQ_API_KEY}")
            .post(requestBody.build())
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(600, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(600, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val text = jsonObject.optString("text", "")
                        callback(text)
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

    fun transcriptionWhisper(audioFile: File, languageCode: String, callback: (String?) -> Unit) {
        val url = "https://api.openai.com/v1/audio/transcriptions"
        val model = "whisper-1"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "recording.${audioFile.absolutePath.substringAfterLast(".")}", audioFile.asRequestBody())
        requestBody.addFormDataPart("model", model)
        requestBody.addFormDataPart("language", languageCode)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${MyApp.APIkey}")
            .post(requestBody.build())
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(600, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(600, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val text = jsonObject.optString("text", "")
                        callback(text)
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

    fun translationWhisper(audioFile: File, callback: (String?) -> Unit) {
        val url = "https://api.openai.com/v1/audio/translations"
        val model = "whisper-1"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "recording.${audioFile.absolutePath.substringAfterLast(".")}", audioFile.asRequestBody())
        requestBody.addFormDataPart("model", model)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${MyApp.APIkey}")
            .post(requestBody.build())
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(600, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(600, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        val text = jsonObject.optString("text", "")
                        callback(text)
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

    private fun errorCheck(response: Response) {
        val responseData = response.body?.string()
        val jsonObject = JSONObject(responseData.toString())
        // Handle different HTTP status codes
        var errorMessage = ""
        if (jsonObject.has("error")) {
            val error = jsonObject.getJSONObject("error")
            errorMessage = error.optString("message","")
        }
        error = "Error code ${response.code}: $errorMessage"
    }
}