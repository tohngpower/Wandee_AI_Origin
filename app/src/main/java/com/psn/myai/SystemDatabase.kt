package com.psn.myai

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class SystemDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "system_db.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "system_db_table"
        private const val COLUMN_ID = "id"
        private const val COLUMN_ITEM = "item"
        private const val COLUMN_DETAIL = "detail"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_ITEM TEXT, $COLUMN_DETAIL TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertQuestion(item: String, detail: String): Long {
        val db = writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_ITEM, item)
        contentValues.put(COLUMN_DETAIL, detail)
        var rowId: Long = -1 // Initialize with an invalid value
        try {
            rowId = db.insert(TABLE_NAME, null, contentValues)
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error inserting item: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error during insert: ${e.message}", e)
        }
        return rowId
    }

    fun replaceAnswer(item: String, newDetail: String): Int {
        val db = writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_DETAIL, newDetail)
        val whereClause = "$COLUMN_ITEM = ?"
        val whereArgs = arrayOf(item)
        var rowsUpdated = 0
        try {
            rowsUpdated = db.update(TABLE_NAME, contentValues, whereClause, whereArgs)
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error updating detail: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error during update: ${e.message}", e)
        }
        return rowsUpdated
    }

    fun deleteItem(item: String): Boolean {
        val db = writableDatabase
        val whereClause = "${COLUMN_ITEM}=?"
        val whereArgs = arrayOf(item)
        var success = false
        try {
            val result = db.delete(TABLE_NAME, whereClause, whereArgs)
            success = result > 0
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error deleting item: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle general exceptions
            Log.e("DatabaseError", "General error during item: ${e.message}", e)
        }
        return success
    }

    fun searchQuestion(query: String): String? {
        val db = readableDatabase
        val selectQuery = "SELECT $COLUMN_DETAIL FROM $TABLE_NAME WHERE $COLUMN_ITEM = ?"
        var answer:String? = null
        try {
            val cursor = db.rawQuery(selectQuery, arrayOf(query))
            if(cursor.moveToFirst()) {
                val i = cursor.getColumnIndex(COLUMN_DETAIL)
                if(i>-1) {
                    answer = cursor.getString(i)
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving detail: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle potential exceptions during database operations
            Log.e("DatabaseError", "General error retrieving detail", e)
        }
        return answer
    }

    fun searchDetail(detail: String): String? {
        val db = readableDatabase
        val selectQuery = "SELECT $COLUMN_ITEM FROM $TABLE_NAME WHERE $COLUMN_DETAIL = ?"
        var item:String? = null
        try {
            val cursor = db.rawQuery(selectQuery, arrayOf(detail))
            if(cursor.moveToFirst()) {
                val i = cursor.getColumnIndex(COLUMN_ITEM)
                if(i>-1) {
                    item = cursor.getString(i)
                }
            }
            cursor.close()
        } catch (e: SQLiteException) {
            // Handle database-specific errors
            Log.e("DatabaseError", "Error retrieving item: ${e.message}", e)
            // Optionally implement retry logic or error reporting to the user
        } catch (e: Exception) {
            // Handle potential exceptions during database operations
            Log.e("DatabaseError", "General error retrieving item", e)
        }
        return item
    }
}