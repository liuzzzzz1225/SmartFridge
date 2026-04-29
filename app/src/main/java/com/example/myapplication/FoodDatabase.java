package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class FoodDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "FoodDB";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "foods";

    public FoodDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "expiry_date TEXT, " +
                "quantity INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addFood(Food food) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // 先查找是否存在相同名称的食材
        Cursor cursor = db.query(TABLE_NAME, null,
                "name = ?", new String[]{food.getName()},
                null, null, null);
        
        ContentValues values = new ContentValues();
        values.put("name", food.getName());
        values.put("expiry_date", food.getExpiryDate());
        values.put("quantity", food.getQuantity());
        
        // 如果存在相同名称的食材，添加新记录而不是更新
        db.insert(TABLE_NAME, null, values);
        
        cursor.close();
        db.close();
    }

    public List<Food> getAllFood() {
        List<Food> foodList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " ORDER BY expiry_date ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Food food = new Food(
                    cursor.getInt(0),     // id
                    cursor.getString(1),   // name
                    cursor.getString(2),   // expiry_date
                    cursor.getInt(3)       // quantity
                );
                foodList.add(food);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return foodList;
    }

    public List<Food> searchFood(String query) {
        List<Food> foodList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 使用参数化查询来防止SQL注入
        String[] selectionArgs = new String[]{"%" + query + "%"};
        Cursor cursor = db.query(TABLE_NAME, null,
                "name LIKE ?", selectionArgs,
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Food food = new Food(
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("expiry_date")),
                    cursor.getInt(cursor.getColumnIndex("quantity"))
                );
                foodList.add(food);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return foodList;
    }

    public void updateFood(int id, Food food) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", food.getName());
        values.put("expiry_date", food.getExpiryDate());
        values.put("quantity", food.getQuantity());
        
        db.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteFood(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }
} 