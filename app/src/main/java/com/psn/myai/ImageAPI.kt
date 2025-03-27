package com.psn.myai

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImageAPI {
    var error = ""
    private val timeOut = 600L

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

    fun imageGeneration(prompt: String, imageURL: (String?) -> Unit) {
        val url = "https://api.openai.com/v1/images/generations"

        val json = JSONObject()
        json.put("model", MyApp.imageModel)
        json.put("prompt", prompt)
        json.put("n", 1)
        json.put("size", MyApp.imageSize)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(timeOut, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(timeOut, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val data = dataArray.getJSONObject(0) // Get the first object in the array
                                val img = data.optString("url", "") // Use optString to handle missing "url" field
                                imageURL(img)
                            } else {
                                error = "Error: Zero image data!"
                                imageURL(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            imageURL(null)
                        }
                    } catch (e: JSONException) {
                        error=e.toString()
                        imageURL(null)
                    }
                } else {
                    errorCheck(response)
                    imageURL(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                imageURL(null)
            }
        })
    }

    fun imageEdition(imageFile: File, maskFile: File?, prompt: String, size: String, imageURL: (String?) -> Unit) {
        val url = "https://api.openai.com/v1/images/edits"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "image.png", imageFile.asRequestBody())
        maskFile?.let {
            requestBody.addFormDataPart("mask", "mask.png", it.asRequestBody())
        }
        requestBody.addFormDataPart("prompt", prompt)
        requestBody.addFormDataPart("size", size)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${MyApp.APIkey}")
            .post(requestBody.build())
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(timeOut, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(timeOut, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val data = dataArray.getJSONObject(0) // Get the first object in the array
                                val img = data.optString("url", "") // Use optString to handle missing "url" field
                                imageURL(img)
                            } else {
                                error = "Error: Zero image data!"
                                imageURL(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            imageURL(null)
                        }
                    } catch (e: JSONException) {
                        error=e.toString()
                        imageURL(null)
                    }
                } else {
                    errorCheck(response)
                    imageURL(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                imageURL(null)
            }
        })
    }

    fun imageVariation(imageFile: File, size: String, imageURL: (String?) -> Unit) {
        val url = "https://api.openai.com/v1/images/variations"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "image.png", imageFile.asRequestBody())
        requestBody.addFormDataPart("size", size)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${MyApp.APIkey}")
            .post(requestBody.build())
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(timeOut, TimeUnit.SECONDS)
            .writeTimeout(timeOut, TimeUnit.SECONDS)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseData.toString())
                        if (jsonObject.has("data")) {
                            val dataArray = jsonObject.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val data = dataArray.getJSONObject(0)
                                val img = data.optString("url", "")
                                imageURL(img)
                            } else {
                                error = "Error: Zero image data!"
                                imageURL(null)
                            }
                        } else {
                            error = "Error: Your OpenAI API Key may be wrong or expire or server down!"
                            imageURL(null)
                        }
                    } catch (e: JSONException) {
                        error=e.toString()
                        imageURL(null)
                    }
                } else {
                    errorCheck(response)
                    imageURL(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                error=e.toString()
                imageURL(null)
            }
        })
    }

    fun flux1Dev(context: Context, prompt: String, callback: (File?) -> Unit) {
        val url = "https://api-inference.huggingface.co/models/XLabs-AI/flux-RealismLora"

        val json = JSONObject()
        json.put("inputs", prompt)

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${MyApp.HF_KEY}")
            .build()

        val client = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS) // Increase the connect timeout
            .readTimeout(timeOut, TimeUnit.SECONDS)    // Increase the read timeout
            .writeTimeout(timeOut, TimeUnit.SECONDS)   // Increase the write timeout
            .build()
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
                        val file = File(context.cacheDir, "temp_pic.png")
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
}