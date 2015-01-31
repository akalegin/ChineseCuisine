package com.example.andrey.chinesecuisine;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DishDBNetUpdater {

    public static final DishDBNetUpdater INSTANCE = new DishDBNetUpdater();

    private static String ONLINE_DB_URL = "http://kalegin-chinese-cuisine.appspot.com";
    private static String RECIPES_SUFFIX = "recipes";

    private DishDBNetUpdater() {

    }

    private void updateLocalDb(Context context) {
        List<Dish> dishList = dishListFromNet();

        RecipeReaderDbHelper mDbHelper = new RecipeReaderDbHelper(context);

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();

        for (Dish dish : dishList) {
            values.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_DISH, dish.getName());
            values.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_INGREDIENTS, dish.getIngredients());
            values.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_COOK_STEPS, dish.getCookSteps());
            values.put(RecipeReaderContract.RecipeEntry.COLUMN_NAME_IMAGE, getBitmapAsByteArray(dish.getImage()));

            db.insert(RecipeReaderContract.RecipeEntry.TABLE_NAME,
                    null,
                    values);
            values.clear();
        }

        db.close();
    }

    public static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    private List<Dish> dishListFromNet() {
        List<Dish> dishList = new ArrayList<>();

        InputStream is = null;

        try {
            String strURL = ONLINE_DB_URL + "/" + RECIPES_SUFFIX;
            URL url = new URL(strURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();

                String contentAsString = readIt(is);

                JSONObject jObject = new JSONObject(contentAsString);
                JSONArray jDishArray = jObject.getJSONArray("recipes");

                for (int i = 0; i < jDishArray.length(); i++)
                {
                    JSONObject oneRecipe = jDishArray.getJSONObject(i);
                    Dish dish = DishFromJSON(oneRecipe);

                    dishList.add(dish);
                }
            }
        } catch (Exception e){

        } finally {
            try{if(is != null)is.close();}catch(Exception e){}
        }

        return  dishList;
    }

    private List<Dish> localDbToDishList(Context context) {
        List<Dish> result = new ArrayList<>();

        RecipeReaderDbHelper mDbHelper = new RecipeReaderDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

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
            Bitmap bitmap = BitmapFactory.decodeByteArray(blob , 0, blob.length);
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

    public List<Dish> getActual(Context context) {
        updateLocalDb(context);
        return localDbToDishList(context);
    }

    public Dish DishFromJSON(JSONObject jObject) {
        String dishName = "";
        String ingredients = "";
        String cookSteps = "";
        Bitmap dishImage = null;

        try {
            dishName = jObject.getString("dish");
            ingredients = jObject.getString("ingredients");
            cookSteps = jObject.getString("cook_steps");

            String dishImageUrl = jObject.getString("image_url");
            dishImage = downloadImage(ONLINE_DB_URL + "/" + dishImageUrl);
        } catch (Exception e){

        }

        return new Dish(dishName, ingredients, cookSteps, dishImage);
    }

    Bitmap downloadImage(String imageUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream input = conn.getInputStream();
                return BitmapFactory.decodeStream(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String readIt(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 1024);
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null)
        {
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }
}
