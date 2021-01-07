package com.rocklinker.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "rocklinker.db";
    public static final String TABLE_NAME = "playlist";

    public DataBase(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + "(id INTEGER PRIMARY KEY, list_name TEXT, uri TEXT, file_name TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insert(String listName, String URI, String fileName) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues contentValues = new ContentValues();
            contentValues.put("list_name", listName);
            contentValues.put("uri", URI);
            contentValues.put("file_name", fileName);

            db.insert(TABLE_NAME, null, contentValues);
            return true;
        }catch (Exception e){
            e.getMessage();
            return false;
        }
    }

    /*private String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        String str = dateFormat.format(date);
        return str;
    }*/

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
        contentValues.put("list_name", listName);
        contentValues.put("json_string", jsonString);
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

    public boolean isExistList(String listName){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT id FROM " + TABLE_NAME + " WHERE list_name = " + listName, null);
        Cursor c = res;
        return false;
    }
}
