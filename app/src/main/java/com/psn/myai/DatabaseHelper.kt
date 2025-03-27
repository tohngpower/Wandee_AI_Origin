package com.psn.myai

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "questions.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "questions_table"
        private const val COLUMN_ID = "id"
        private const val COLUMN_QUESTION = "question"
        private const val COLUMN_ANSWER = "answer"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_QUESTION TEXT, $COLUMN_ANSWER TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertQuestion(question: String, answer: String): Long {
        val db = writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_QUESTION, question)
        contentValues.put(COLUMN_ANSWER, answer)
        var rowId: Long = -1 // Initialize with an invalid value
        try {
            rowId = db.insert(TABLE_NAME, null, contentValues)
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error inserting question: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error during insert: ${e.message}", e)
        }
        return rowId
    }

    fun replaceAnswer(question: String, newAnswer: String): Int {
        val db = writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_ANSWER, newAnswer)
        val whereClause = "$COLUMN_QUESTION = ?"
        val whereArgs = arrayOf(question)
        var rowsUpdated = 0
        try {
            rowsUpdated = db.update(TABLE_NAME, contentValues, whereClause, whereArgs)
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error updating answer: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error during update: ${e.message}", e)
        }
        return rowsUpdated
    }

    // Method to delete a question and its corresponding answer from the database
    fun deleteQuestionAndAnswer(question: String): Boolean {
        val db = writableDatabase
        val whereClause = "$COLUMN_QUESTION=?"
        val whereArgs = arrayOf(question)
        var success = false
        try {
            val result = db.delete(TABLE_NAME, whereClause, whereArgs)
            success = result > 0
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error deleting question: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error during delete: ${e.message}", e)
        }
        return success
    }

    fun searchQuestion(query: String): String? {
        val db = readableDatabase
        val selectQuery = "SELECT $COLUMN_ANSWER FROM $TABLE_NAME WHERE $COLUMN_QUESTION = ?"
        var answer:String? = null
        try {
            val cursor = db.rawQuery(selectQuery, arrayOf(query))
            if(cursor.moveToFirst()) {
                val i = cursor.getColumnIndex(COLUMN_ANSWER)
                if(i>-1) {
                    answer = cursor.getString(i)
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving answer: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle potential exceptions during database operations
            Log.e("DatabaseError", "General error retrieving answer", e)
        }
        return answer
    }

    fun getAllQuestions(): MutableList<String> {
        val questions = mutableListOf<String>()
        val db = readableDatabase
        val selectAllQuery = "SELECT $COLUMN_QUESTION FROM $TABLE_NAME"
        try {
            val cursor = db.rawQuery(selectAllQuery, null)
            while (cursor.moveToNext()) {
                val i = cursor.getColumnIndex(COLUMN_QUESTION)
                if(i>-1) {
                    val query = cursor.getString(i)
                    questions.add(query)
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving all questions: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle potential exceptions during database operations
            Log.e("DatabaseError", "General error retrieving all questions", e)
        }
        return questions
    }

    fun getSearchQuestion(question: String): MutableList<String> {
        val questions = mutableListOf<String>()
        val db = readableDatabase
        val selectAllQuery = "SELECT $COLUMN_QUESTION FROM $TABLE_NAME"
        try {
            val cursor = db.rawQuery(selectAllQuery, null)
            while (cursor.moveToNext()) {
                val i = cursor.getColumnIndex(COLUMN_QUESTION)
                if(i>-1) {
                    val query = cursor.getString(i)
                    if(query.contains(question.trim())) {
                        questions.add(query)
                    }
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving searching questions: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle potential exceptions during database operations
            Log.e("DatabaseError", "General error retrieving searching questions", e)
        }
        return questions
    }
}