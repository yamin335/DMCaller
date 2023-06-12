package com.bdcom.appdialer.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bdcom.appdialer.models.CallHistory
import com.bdcom.appdialer.utils.AndroidUtil.Companion.getCurrentTime
import java.util.ArrayList

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE " + TABLE_NAME_CALL_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_CONTACT_NUMBER + " varchar(100), " +
                COLUMN_CONTACT_NAME + " varchar(100), " +
                COLUMN_START_DATE + " varchar(100), " +
                COLUMN_END_DATE + " varchar(100), " +
                COLUMN_CALL_DURATION + " varchar(100), " +
                COLUMN_CALL_STATUS + " INTEGER" + ") "
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME_CALL_HISTORY")
        onCreate(db)
    }

    fun insertCallHistory(number: String, name: String, callStatus: Int) {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(COLUMN_CONTACT_NUMBER, number)
        cv.put(COLUMN_CONTACT_NAME, name)
        cv.put(COLUMN_START_DATE, getCurrentTime())
        cv.put(COLUMN_CALL_STATUS, callStatus)
        db.insert(TABLE_NAME_CALL_HISTORY, null, cv)
    }

    fun updateLastCall(number: String, callDuration: String, callStatus: Int) {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(COLUMN_CALL_DURATION, callDuration)
        cv.put(COLUMN_END_DATE, getCurrentTime())
        cv.put(COLUMN_CALL_STATUS, callStatus)
        db.update(TABLE_NAME_CALL_HISTORY, cv, COLUMN_CONTACT_NUMBER + "=? and " +
                COLUMN_ID + "=(SELECT MAX(id) FROM " + TABLE_NAME_CALL_HISTORY + ")", arrayOf(number))
    }

    fun deleteCallHistory(id: String) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME_CALL_HISTORY, "$COLUMN_ID=$id", null)
    }

    fun getLastCall(): String? {
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_NAME_CALL_HISTORY WHERE $COLUMN_ID = (SELECT MAX(id) FROM $TABLE_NAME_CALL_HISTORY )"
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToLast()) {
            return cursor.getString(1)
        }
        return null
    }

    /**select * from <TABLE> where row_id = (select max(row_id) from <TABLE>);
     * Returns all the data from database
     * @return
     */
    private fun getAllCallHistory(): Cursor {
        val cursor: Cursor
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_NAME_CALL_HISTORY ORDER BY $COLUMN_ID DESC"
        cursor = db.rawQuery(query, null)
        return cursor
    }

    private fun getCallHistory(number: String): Cursor {
        val db = this.writableDatabase
        val query = "SELECT * FROM " + TABLE_NAME_CALL_HISTORY +
                " WHERE " + COLUMN_CONTACT_NUMBER + " = '" + number + "'" + " ORDER BY " + COLUMN_ID + " DESC"
        return db.rawQuery(query, null)
    }

    fun readCallHistory(number: String?, list: ArrayList<CallHistory>) {
        var cursor: Cursor? = null
        try {
            cursor = if (number.isNullOrEmpty()) {
                getAllCallHistory()
            } else {
                getCallHistory(number!!)
            }

            while (cursor.moveToNext()) {
                val history = CallHistory()
                history.contactId = cursor.getString(cursor.getColumnIndex(COLUMN_ID))
                history.contactNumber = cursor.getString(cursor.getColumnIndex(COLUMN_CONTACT_NUMBER))
                history.contactName = cursor.getString(cursor.getColumnIndex(COLUMN_CONTACT_NAME))
                history.dialStartDate = cursor.getString(cursor.getColumnIndex(COLUMN_START_DATE))
                history.callStatus = cursor.getInt(cursor.getColumnIndex(COLUMN_CALL_STATUS))
                history.callDuration = cursor.getString(cursor.getColumnIndex(COLUMN_CALL_DURATION))
                list.add(history)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (cursor != null && cursor.isClosed.not()) {
                cursor.close()
            }
        }
    }

    companion object {
        private const val DATABASE_NAME = "db"
        private const val DATABASE_VERSION = 5
        private const val TABLE_NAME_CALL_HISTORY = "callHistory"
        private const val COLUMN_ID = "id"
        private const val COLUMN_CONTACT_NUMBER = "contactNumber"
        private const val COLUMN_CONTACT_NAME = "contactName"
        private const val COLUMN_START_DATE = "startDate"
        private const val COLUMN_END_DATE = "endDate"
        private const val COLUMN_CALL_DURATION = "callDuration"
        private const val COLUMN_CALL_STATUS = "callStatus"
    }
}
