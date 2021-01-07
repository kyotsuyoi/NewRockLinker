package com.rocklinker.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBaseCurrentList extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "rocklinker.db";
    public static final String TABLE_NAME = "current_playlist";

    public DataBaseCurrentList(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void dropTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    public void createTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(id INTEGER PRIMARY KEY, uri TEXT, file_name TEXT, artist TEXT, title TEXT)"
        );
    }

    public boolean insert(String URI, String fileName, String artist, String title) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues contentValues = new ContentValues();
            contentValues.put("uri", URI);
            contentValues.put("file_name", fileName);
            contentValues.put("artist", artist);
            contentValues.put("title", title);

            db.insert(TABLE_NAME, null, contentValues);
            return true;
        }catch (Exception e){
            e.getMessage();
            return false;
        }
    }

    public boolean delete(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = "id = ?";
        String[] whereArgs = new String[]{id + ""};
        try {
            db.delete(TABLE_NAME, where, whereArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean update(int id, String listName, String jsonString) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("", listName);
        db.update(TABLE_NAME, contentValues, "id = ? ", new String[] {String.valueOf(id)} );
        return true;
    }

    public Cursor getData(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM "+TABLE_NAME+" ORDER BY id DESC", null);
        return res;
    }

    public Cursor getDataFromID(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE id = " + id, null);
        return res;
    }
}
