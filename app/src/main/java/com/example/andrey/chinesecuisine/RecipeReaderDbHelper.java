package com.example.andrey.chinesecuisine;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
}
