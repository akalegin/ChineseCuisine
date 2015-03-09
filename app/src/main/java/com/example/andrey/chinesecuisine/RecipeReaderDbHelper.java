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
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String UNIQUE = " UNIQUE";

    private static final String SQL_CREATE_RECIPES =
            "CREATE TABLE " + RecipeReaderContract.RecipeEntry.TABLE_NAME + " (" +
                    RecipeReaderContract.RecipeEntry._ID + " INTEGER PRIMARY KEY," +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS + TEXT_TYPE + COMMA_SEP +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS + TEXT_TYPE + COMMA_SEP +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE + " BLOB" +
                    " )";
    private static final String SQL_DELETE_RECIPES =
            "DROP TABLE IF EXISTS " + RecipeReaderContract.RecipeEntry.TABLE_NAME;

    private static final String SQL_CREATE_TAGS =
            "CREATE TABLE " + RecipeReaderContract.TagEntry.TABLE_NAME + " (" +
                    RecipeReaderContract.RecipeEntry._ID + " INTEGER PRIMARY KEY," +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP + UNIQUE + " (" +
                    RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME + ") ON CONFLICT IGNORE" +
                    ")";
    private static final String SQL_DELETE_TAGS =
            "DROP TABLE IF EXISTS " + RecipeReaderContract.TagEntry.TABLE_NAME;

    private static final String SQL_CREATE_RECIPES_TAGS =
            "CREATE TABLE " + RecipeReaderContract.RecipeTagEntry.TABLE_NAME + " (" +
                    RecipeReaderContract.RecipeTagEntry._ID + " INTEGER PRIMARY KEY," +
                    RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_RECIPE_ID + INTEGER_TYPE + COMMA_SEP +
                    RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_TAG_ID + INTEGER_TYPE +
                    " )";
    private static final String SQL_DELETE_RECIPES_TAGS =
            "DROP TABLE IF EXISTS " + RecipeReaderContract.RecipeTagEntry.TABLE_NAME;

    ContentValues myContentValues = new ContentValues();

    private List<String> mCachedTags = new ArrayList<>();

    public RecipeReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_RECIPES);
        db.execSQL(SQL_CREATE_TAGS);
        db.execSQL(SQL_CREATE_RECIPES_TAGS);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_RECIPES);
        db.execSQL(SQL_DELETE_TAGS);
        db.execSQL(SQL_DELETE_RECIPES_TAGS);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    public void addRecipeToDB(Recipe recipe, SQLiteDatabase db) {
        // Recipe itself
        myContentValues.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME, recipe.getName());
        myContentValues.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS, recipe.getIngredients());
        myContentValues.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS, recipe.getCookSteps());
        myContentValues.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE, getBitmapAsByteArray(recipe.getImage()));

        db.insert(RecipeReaderContract.RecipeEntry.TABLE_NAME,
                null,
                myContentValues);
        myContentValues.clear();

        Integer recipeID = getIDByName(db,
                RecipeReaderContract.RecipeEntry.TABLE_NAME,
                RecipeReaderContract.RecipeEntry._ID,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME,
                recipe.getName());

        // Tags
        for (String tag : recipe.getTags()) {
            myContentValues.put(RecipeReaderContract.TagEntry.COLUMN_NAME_NAME, tag);
            db.insert(RecipeReaderContract.TagEntry.TABLE_NAME,
                    null,
                    myContentValues);
            myContentValues.clear();

            // Recipe & Tag relation
            Integer tagID = getIDByName(db,
                    RecipeReaderContract.TagEntry.TABLE_NAME,
                    RecipeReaderContract.TagEntry._ID,
                    RecipeReaderContract.TagEntry.COLUMN_NAME_NAME,
                    tag);
            myContentValues.put(RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_RECIPE_ID, recipeID);
            myContentValues.put(RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_TAG_ID, tagID);
            db.insert(RecipeReaderContract.RecipeTagEntry.TABLE_NAME,
                    null,
                    myContentValues);
            myContentValues.clear();
        }
    }
    public List<String> getRecipeNames() {
        List<String> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME + " DESC";

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
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME)));
        }
        cursor.close();

        db.close();

        return result;
    }
    public List<Recipe> getAllRecipes() {
        List<Recipe> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                RecipeReaderContract.RecipeEntry._ID,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME + " DESC";

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

            String recipeName = cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME));

            Recipe recipe = new Recipe(recipeName,
                    cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS)),
                    bitmap,
                    getTagsByRecipeName(db, recipeName));
            result.add(recipe);
        }
        cursor.close();

        db.close();

        return result;
    }

    Integer getIDByName(SQLiteDatabase db, String tableName, String columnID, String columnName, String name) {
        Integer result = 0;

        String[] projection = {
                columnID,
                columnName
        };

        String[] names = {
                name
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = columnName + " DESC";

        Cursor cursor = db.query(
                tableName,  // The table to query
                projection,// The columns to return
                columnName + "=?",// The columns for the WHERE clause
                names,// The values for the WHERE clause
                null,// don't group the rows
                null,// don't filter by row groups
                sortOrder// The sort order
        );

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            result = cursor.getInt(cursor.getColumnIndexOrThrow(columnID));
        }
        cursor.close();

        return result;
    }

    public void cacheTags() {
        mCachedTags.clear();

        SQLiteDatabase db = getWritableDatabase();

        String[] projection = {
                RecipeReaderContract.TagEntry.COLUMN_NAME_NAME
        };

        String sortOrder = RecipeReaderContract.TagEntry.COLUMN_NAME_NAME + " DESC";
        Cursor cursor = db.query(
                RecipeReaderContract.TagEntry.TABLE_NAME,  // The table to query
                projection,// The columns to return
                null,// The columns for the WHERE clause
                null,// The values for the WHERE clause
                null,// don't group the rows
                null,// don't filter by row groups
                sortOrder// The sort order
        );

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            mCachedTags.add(cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.TagEntry.COLUMN_NAME_NAME)));
        }
        cursor.close();
        db.close();
    }

    public List<String> getAllTags() {
        return mCachedTags;
    }

    List<String> getTagsByRecipeName(SQLiteDatabase db, String recipeName) {
        List<String> result = new ArrayList<>();

        Integer recipeID = getIDByName(db,
                RecipeReaderContract.RecipeEntry.TABLE_NAME,
                RecipeReaderContract.RecipeEntry._ID,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME,
                recipeName);

        String[] projection = {
                RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_TAG_ID
        };

        String[] recipeIDs = {
                recipeID.toString()
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_TAG_ID + " DESC";

        Cursor cursor = db.query(
                RecipeReaderContract.RecipeTagEntry.TABLE_NAME,  // The table to query
                projection,// The columns to return
                RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_RECIPE_ID + "=?",// The columns for the WHERE clause
                recipeIDs,// The values for the WHERE clause
                null,// don't group the rows
                null,// don't filter by row groups
                sortOrder// The sort order
        );

        List<String> tagIDs = new ArrayList<>();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            tagIDs.add(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_TAG_ID))));
        }

        projection[0] = RecipeReaderContract.TagEntry.COLUMN_NAME_NAME;

        String whereStatement = null;
        String[] whereArgs = null;
        if (tagIDs.size() == 1) {
            whereStatement = RecipeReaderContract.TagEntry._ID + "=?";
            whereArgs = tagIDs.toArray(new String[tagIDs.size()]);
        } else if (tagIDs.size() > 1) {
            whereStatement = RecipeReaderContract.TagEntry._ID + " IN (?";
            for (int i = 1; i < tagIDs.size(); ++i) {
                whereStatement = whereStatement + ",?";
            }
            whereStatement = whereStatement + ")";
            whereArgs = tagIDs.toArray(new String[tagIDs.size()]);
        }

        sortOrder = RecipeReaderContract.TagEntry.COLUMN_NAME_NAME + " DESC";
        cursor = db.query(
                RecipeReaderContract.TagEntry.TABLE_NAME,  // The table to query
                projection,// The columns to return
                whereStatement,// The columns for the WHERE clause
                whereArgs,// The values for the WHERE clause
                null,// don't group the rows
                null,// don't filter by row groups
                sortOrder// The sort order
        );

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.TagEntry.COLUMN_NAME_NAME)));
        }
        cursor.close();

        return result;
    }

    List<String> getRecipeNamesByTags(List<String> tags) {
        List<String> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        List<String> tagIDList = new ArrayList<>();
        for (String tag : tags) {
            tagIDList.add(getIDByName(db,
                    RecipeReaderContract.TagEntry.TABLE_NAME,
                    RecipeReaderContract.TagEntry._ID,
                    RecipeReaderContract.TagEntry.COLUMN_NAME_NAME,
                    tag).toString());
        }

        String[] projection = {
                RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_RECIPE_ID
        };

        String[] tagIDs = tagIDList.toArray(new String[tagIDList.size()]);

        // How you want the results sorted in the resulting Cursor
        String sortOrder = RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_RECIPE_ID + " DESC";

        String whereStatement = null;
        String[] whereArgs = null;
        if (tagIDs.length == 1) {
            whereStatement = RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_TAG_ID + "=?";
            whereArgs = tagIDs;
        } else if (tagIDs.length > 1) {
            whereStatement = RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_TAG_ID + " IN (?";
            for (int i = 1; i < tagIDs.length; ++i) {
                whereStatement = whereStatement + ",?";
            }
            whereStatement = whereStatement + ")";
            whereArgs = tagIDs;
        }

        Cursor cursor = db.query(
                RecipeReaderContract.RecipeTagEntry.TABLE_NAME,  // The table to query
                projection,// The columns to return
                whereStatement,// The columns for the WHERE clause
                whereArgs,// The values for the WHERE clause
                null,// don't group the rows
                null,// don't filter by row groups
                sortOrder// The sort order
        );

        List<String> recipeIDs = new ArrayList<>();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            recipeIDs.add(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeTagEntry.COLUMN_NAME_RECIPE_ID))));
        }

        projection[0] = RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME;

        whereStatement = null;
        whereArgs = null;
        if (recipeIDs.size() == 1) {
            whereStatement = RecipeReaderContract.RecipeEntry._ID + "=?";
            whereArgs = recipeIDs.toArray(new String[recipeIDs.size()]);
        } else if (recipeIDs.size() > 1) {
            whereStatement = RecipeReaderContract.RecipeEntry._ID + " IN (?";
            for (int i = 1; i < recipeIDs.size(); ++i) {
                whereStatement = whereStatement + ",?";
            }
            whereStatement = whereStatement + ")";
            whereArgs = recipeIDs.toArray(new String[recipeIDs.size()]);
        }

        sortOrder = RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME + " DESC";
        cursor = db.query(
                RecipeReaderContract.RecipeEntry.TABLE_NAME,  // The table to query
                projection,// The columns to return
                whereStatement,// The columns for the WHERE clause
                whereArgs,// The values for the WHERE clause
                null,// don't group the rows
                null,// don't filter by row groups
                sortOrder// The sort order
        );

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME)));
        }
        cursor.close();
        db.close();

        return result;
    }

    public List<String> getRecipesByTags(List<String> tags) {
        return getRecipeNamesByTags(tags);
    }

    Recipe getRecipeByName(String name) {
        Recipe result = null;

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                RecipeReaderContract.RecipeEntry._ID,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS,
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE
        };

        String[] names = {
            name
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME + " DESC";

        Cursor cursor = db.query(
                RecipeReaderContract.RecipeEntry.TABLE_NAME,  // The table to query
                projection,// The columns to return
                RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME + "=?",// The columns for the WHERE clause
                names,// The values for the WHERE clause
                null,// don't group the rows
                null,// don't filter by row groups
                sortOrder// The sort order
        );

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            byte[] blob = cursor.getBlob(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE));
            Bitmap bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
            Recipe recipe = new Recipe(cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS)),
                    bitmap,
                    getTagsByRecipeName(db, name));
            cursor.close();
            result = recipe;
        }

        db.close();
        return result;
    }
    public static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }
}
