package com.example.andrey.chinesecuisine;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class RecipeReaderDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "RecipeReader.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RecipeReaderContract.RecipeEntry.TABLE_NAME + " (" +
                    RecipeReaderContract.RecipeEntry._ID + " INTEGER PRIMARY KEY," +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH + TEXT_TYPE + COMMA_SEP +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS + TEXT_TYPE + COMMA_SEP +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS + TEXT_TYPE + COMMA_SEP +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE + " BLOB" +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RecipeReaderContract.RecipeEntry.TABLE_NAME;

    ContentValues myContentValues = new ContentValues();

    public RecipeReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    public void addDishToDB(Dish dish, SQLiteDatabase db) {
        myContentValues.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH, dish.getName());
        myContentValues.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS, dish.getIngredients());
        myContentValues.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS, dish.getCookSteps());
        myContentValues.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE, getBitmapAsByteArray(dish.getImage()));

        db.insert(RecipeReaderContract.RecipeEntry.TABLE_NAME,
                null,
                myContentValues);
        myContentValues.clear();
    }
    public List<String> getDishNames() {
        List<String> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH + " DESC";

        Cursor cursor = db.query(
                RecipeReaderContract.RecipeEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH)));
        }
        cursor.close();

        db.close();

        return result;
    }
    public List<Dish> getAllDishes() {
        List<Dish> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                RecipeReaderContract.RecipeEntry._ID,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH + " DESC";

        Cursor cursor = db.query(
                RecipeReaderContract.RecipeEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            byte[] blob = cursor.getBlob(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE));
            Bitmap bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
            Dish dish = new Dish(cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS)),
                    bitmap);
            result.add(dish);
        }
        cursor.close();

        db.close();

        return result;
    }
    Dish getDishByName(String name) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                RecipeReaderContract.RecipeEntry._ID,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE
        };

        String[] names = {
            name
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH + " DESC";

        Cursor cursor = db.query(
                RecipeReaderContract.RecipeEntry.TABLE_NAME,  // The table to query
                projection,// The columns to return
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH + "=?",// The columns for the WHERE clause
                names,// The values for the WHERE clause
                null,// don't group the rows
                null,// don't filter by row groups
                sortOrder// The sort order
        );

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            byte[] blob = cursor.getBlob(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE));
            Bitmap bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
            Dish dish = new Dish(cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS)),
                    bitmap);
            cursor.close();
            return dish;
        }
        return null;
    }
    public static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }
}
