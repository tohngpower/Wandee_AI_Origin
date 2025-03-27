package com.psn.myai

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONException
import java.util.concurrent.TimeUnit

class VideoUploader(private val videoPath: String) {
    private val client = OkHttpClient().newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS)
        .build()
    private val baseUrl = "https://generativelanguage.googleapis.com"
    var error = ""
    var numberOfToken = 0
    var tokenInput = 0
    var tokenOutput = 0

    suspend fun uploadAndGenerateContent(prompt: String, mimeType: String): String? {
        val numBytes = File(videoPath).length()
        val displayName = videoPath

        val uploadUrl = initiateUpload(mimeType, numBytes, displayName) ?: return null

        val fileInfo = uploadVideo(uploadUrl, numBytes)
        if (fileInfo == null) {
            error = "Failed to upload video"
            return null
        }

        val fileObject = fileInfo.getJSONObject("file")
        var state = fileObject.getString("state")
        val fileUri = fileObject.getString("uri")
        while (state.contains("PROCESSING", true)) {
            delay(1000)
            state = getVideoState(fileUri)
        }
        if ((state != "ACTIVE") && (state != "ERROR")){
            error = "Video processing failed. Final state: $state"

            return null
        } else if(state == "ERROR") {
            return null
        }

        return generateContent(prompt, fileUri, mimeType)
    }

    private suspend fun getMimeType(filePath: String): String? = withContext(Dispatchers.IO){
        try {
            Runtime.getRuntime().exec("file -b --mime-type $filePath").inputStream.bufferedReader().readLine()
        } catch (e: Exception) {
            error = "Error getting MIME type: ${e.message}"
            null
        }
    }

    private suspend fun initiateUpload(mimeType: String?, numBytes: Long, displayName: String): String? = withContext(Dispatchers.IO){
        val requestBody = """{"file": {"display_name": "$displayName"}}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/upload/v1beta/files?key=${MyApp.geminiAPIkey}")
            .addHeader("X-Goog-Upload-Protocol", "resumable")
            .addHeader("X-Goog-Upload-Command", "start")
            .addHeader("X-Goog-Upload-Header-Content-Length", numBytes.toString())
            .addHeader("X-Goog-Upload-Header-Content-Type", mimeType ?: "application/octet-stream") // Default if mimeType is null
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            response.header("X-Goog-Upload-URL")
        } catch (e: IOException) {
            error = "Error initiating upload: ${e.message}"
            null
        }
    }

    private suspend fun uploadVideo(uploadUrl: String, numBytes: Long): JSONObject? = withContext(Dispatchers.IO) {
        val mimeType = getMimeType(videoPath) // Call getMimeType here
        val requestBody = object : RequestBody() {
            override fun contentType() = mimeType?.toMediaType()  // Use the retrieved mimeType
            override fun contentLength() = numBytes
            override fun writeTo(sink: BufferedSink) {  // No longer needs to be suspend
                File(videoPath).source().use { source -> sink.writeAll(source) }
            }
        }

        val request = Request.Builder()
            .url(uploadUrl)
            .addHeader("Content-Length", numBytes.toString())
            .addHeader("X-Goog-Upload-Offset", "0")
            .addHeader("X-Goog-Upload-Command", "upload, finalize")
            .put(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            JSONObject(response.body?.string() ?: "{}")
        } catch (e: IOException) {
            error = "Error uploading video: ${e.message}"
            null
        } catch (e: Exception) {
            error = "Error parsing response to JSON: ${e.message}"
            null
        }
    }

    private suspend fun getVideoState(fileUrl: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$fileUrl?key=${MyApp.geminiAPIkey}") //Corrected URL
            .get().build()
        try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            json.getString("state")
        } catch (e: Exception) {
            error = "Error retrieving video state: ${e.message}"
            "ERROR" // Return a distinct error state
        }
    }

    private suspend fun generateContent(prompt: String, fileUri: String, mimeType: String?): String? = withContext(Dispatchers.IO) {
        val contentsArray = JSONArray()
        val content = JSONObject()
        content.put("role", "user")

        val partsArray = JSONArray()
        val fileData = JSONObject()
        val fileObject = JSONObject()
        fileObject.put("mime_type", mimeType)
        fileObject.put("file_uri",fileUri)
        fileData.put("file_data",fileObject)
        partsArray.put(fileData)

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
            .url("$baseUrl/v1beta/models/${MyApp.gptModel}:generateContent?key=${MyApp.geminiAPIkey}")
            .post(body)
            .build()

        try {
            val responseData = client.newCall(request).execute().body?.string()
            try {
                Log.d("Video response","$responseData")
                val jsonObject = JSONObject(responseData.toString())
                val dataArray = jsonObject.getJSONArray("candidates")
                val data = dataArray.getJSONObject(0)
                val outputContent = data.getJSONObject("content")
                val partArray = outputContent.getJSONArray("parts")
                val part = partArray.getJSONObject(0)
                val text = part.optString("text","")
                checkUsageVideo(jsonObject)
                text
            } catch (e: JSONException) {
                error=e.message.toString()
                null
            }
        } catch (e: IOException) {
            error = "Error generating content: ${e.message}"
            null
        }
    }

    private fun checkUsageVideo(jsonObject: JSONObject) {
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
}