package com.psn.myai

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.lang.Exception

class DatabaseImage(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val systemDatabase = SystemDatabase(context)

    companion object {
        private const val DATABASE_NAME = "imageGEN.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "imageGEN_table"
        private const val COLUMN_ID = "id"
        private const val COLUMN_PROMPT = "prompt"
        private const val COLUMN_IMAGE_DATA = "image_data"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_PROMPT TEXT, $COLUMN_IMAGE_DATA TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Step 2: When a request comes in, check if an image exists for the given prompt
    fun loadImageForPrompt(prompt: String): String? {
        val db = readableDatabase
        val query = "SELECT image_data FROM imageGEN_table WHERE prompt = ?"
        var imageD:String? = null

        try {
            val cursor = db.rawQuery(query, arrayOf(prompt))
            if(cursor.moveToFirst()) {
                val i = cursor.getColumnIndex("image_data")
                if(i>-1) {
                    imageD = cursor.getString(i)
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving image: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle potential exceptions during database operations
            Log.e("DatabaseError", "General error retrieving image", e)
        }
        return imageD
    }

    // Method to delete a prompt and its corresponding image from the database
    fun deletePromptAndImage(prompt: String): Boolean {
        val imagePath = loadImageForPrompt(prompt)
        if(imagePath is String) {
            try {
                val file = File(imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: IOException) {
                Log.e("FileError", "Error during deleting file", e)
            }
        }
        val db = writableDatabase
        val whereClause = "$COLUMN_PROMPT=?"
        val whereArgs = arrayOf(prompt)
        var success = false
        try {
            val result = db.delete(TABLE_NAME, whereClause, whereArgs)
            success = result > 0
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error deleting prompt and image: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error during delete prompt and image: ${e.message}", e)
        }
        return success
    }

    fun insertPromptAndImagePathToDB(prompt: String, imagePath: String): Boolean {
        val db = writableDatabase
        val query = "INSERT INTO imageGEN_table (prompt, image_data) VALUES (?, ?)"
        val statement = db.compileStatement(query)
        statement.bindString(1, prompt)
        statement.bindString(2, imagePath)
        val result = statement.executeInsert()

        return result != (-1).toLong()
    }

    // Step 4: If the image doesn't exist, download it and save it to the database
    fun saveImageToDatabase(prompt: String, imageUrl: String, assistantAPI:Boolean = false, callback: (String) -> Unit) {
        if(assistantAPI) {
            downloadFile(imageUrl) { imagePath ->
                if (!imagePath.isNullOrEmpty()) {
                    // Handle the downloaded image data here
                    val db = writableDatabase
                    val query = "INSERT INTO imageGEN_table (prompt, image_data) VALUES (?, ?)"
                    val statement = db.compileStatement(query)
                    statement.bindString(1, prompt)
                    statement.bindString(2, imagePath)
                    val result = statement.executeInsert()

                    // Check if the insertion was successful and inform the callback
                    val isSuccess = result != (-1).toLong()
                    if (isSuccess) {
                        callback("Image generating success!")
                    } else {
                        callback(imageUrl)
                    }
                } else {
                    // Handle the case where image download failed
                    callback(imageUrl)
                }
            }
        } else {
            downloadImage(imageUrl) { imagePath ->
                if (!imagePath.isNullOrEmpty()) {
                    // Handle the downloaded image data here
                    val db = writableDatabase
                    val query = "INSERT INTO imageGEN_table (prompt, image_data) VALUES (?, ?)"
                    val statement = db.compileStatement(query)
                    statement.bindString(1, prompt)
                    statement.bindString(2, imagePath)
                    val result = statement.executeInsert()

                    // Check if the insertion was successful and inform the callback
                    val isSuccess = result != (-1).toLong()
                    if (isSuccess) {
                        callback("Image generating success!")
                    } else {
                        callback(imageUrl)
                    }
                } else {
                    // Handle the case where image download failed
                    callback(imageUrl)
                }
            }
        }
    }

    private fun downloadImage(url: String, callback: (String?) -> Unit) {
        val backgroundThread = Thread {
            try {
                // Your image download code here
                val imageUrl = URL(url)
                val connection: HttpURLConnection = imageUrl.openConnection() as HttpURLConnection
                connection.connect()

                val input: InputStream = connection.inputStream
                val bitmap: Bitmap = BitmapFactory.decodeStream(input)

                val directory = File(context.filesDir, "picture")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val count = systemDatabase.searchQuestion("image count")
                var imageCount: Long = if(count==null) {
                    systemDatabase.insertQuestion("image count","0")
                    0
                } else {
                    try {
                        count.toLong()
                    } catch (e: NumberFormatException) {
                        0
                    }
                }
                val file = File(directory, "$imageCount.png")
                imageCount++
                systemDatabase.replaceAnswer("image count",imageCount.toString())

                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                callback(file.absolutePath)
            } catch (e: IOException) {
                Log.e("FileError", "Error during saving image file: ${e.message}", e)
                // Call the callback with null to indicate an error
                callback(null)
            } catch (e: Exception) {
                Log.e("FileError", "General error during saving image file: ${e.message}", e)
                // Call the callback with null to indicate an error
                callback(null)
            }
        }
        backgroundThread.start()
    }

    private fun downloadFile(url: String, callback: (String?) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${MyApp.APIkey}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle errors, e.g., show an error message
                Log.e("DownloadError", "Failed to download file: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Save the downloaded content to a file
                    val directory = File(context.filesDir, "picture")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    var imageCount: Long = if(systemDatabase.searchQuestion("image count")==null) {
                        systemDatabase.insertQuestion("image count","0")
                        0
                    } else {
                        systemDatabase.searchQuestion("image count")!!.toLong()
                    }
                    val file = File(directory, "$imageCount.png")
                    imageCount++
                    systemDatabase.replaceAnswer("image count",imageCount.toString())
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback(file.absolutePath)
                } else {
                    // Handle non-successful responses, e.g., show an error message
                    Log.e("DownloadError", "Download failed: ${response.code}")
                    callback(null)
                }
            }
        })
    }

    fun getAllPrompts(): List<String> {
        val prompts = mutableListOf<String>()
        val db = readableDatabase
        val query = "SELECT $COLUMN_PROMPT FROM $TABLE_NAME"
        try {
            val cursor = db.rawQuery(query, null)
            while (cursor.moveToNext()) {
                val i = cursor.getColumnIndex(COLUMN_PROMPT)
                if(i>-1) {
                    val prompt = cursor.getString(i)
                    prompts.add(prompt)
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving prompts: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error retrieving prompts: ${e.message}", e)
        }
        return prompts
    }

    fun getSearchPrompts(prompt: String): List<String> {
        val prompts = mutableListOf<String>()
        val db = readableDatabase
        val allQuery = "SELECT $COLUMN_PROMPT FROM $TABLE_NAME"
        try {
            val cursor = db.rawQuery(allQuery, null)
            while (cursor.moveToNext()) {
                val i = cursor.getColumnIndex(COLUMN_PROMPT)
                if(i>-1) {
                    val query = cursor.getString(i)
                    if(query.contains(prompt.trim())) {
                        prompts.add(query)
                    }
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving searching prompts: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error retrieving searching prompts: ${e.message}", e)
        }
        return prompts
    }

    fun searchPromptFromImage(imagePath: String): String {
        val db = readableDatabase
        val query = "SELECT $COLUMN_PROMPT FROM $TABLE_NAME WHERE $COLUMN_IMAGE_DATA = ?"
        var prompt = ""
        try {
            val cursor = db.rawQuery(query, arrayOf(imagePath))
            if(cursor.moveToFirst()) {
                val i = cursor.getColumnIndex(COLUMN_PROMPT)
                if(i>-1) {
                    prompt = cursor.getString(i)
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving prompt: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error retrieving prompt: ${e.message}", e)
        }
        return prompt
    }
}