package com.example.andrey.chinesecuisine;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SyncDishesTask extends AsyncTask<Void, Integer, Boolean> {
    private final WeakReference<Activity> myActivity;
    private final WeakReference<DishFragment> myFragment;

    private static String ONLINE_DB_URL = "http://kalegin-chinese-cuisine.appspot.com";
    private static String RECIPES_SUFFIX = "recipes";
    private static String VERSION_SUFFIX = "version";

    private void updateLocalDb(Context context, SharedPreferences sharedPreferences) {
        // Если текущая версия базы данных меньше той, что доступна на сервере, то
        // произведём обновление.
        int defaultDbVersion = context.getResources().getInteger(R.integer.default_db_version);
        int localDbVersion = sharedPreferences.getInt(context.getResources().getString(R.string.local_db_version), defaultDbVersion);
        int onlineDbVersion = getOnlineDbVersion(localDbVersion);

        if (onlineDbVersion > localDbVersion) {
            List<Dish> dishList = dishListFromNet();

            RecipeReaderDbHelper mDbHelper = new RecipeReaderDbHelper(context);

            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            Log.d("DishDBNetUpdater", "DB version is " + db.getVersion());

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();

            for (Dish dish : dishList) {
                Log.d("DishDBNetUpdater", "Dish name is " + dish.getName());
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

            //Сохраним информацию о версии локального набора данных
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(context.getResources().getString(R.string.local_db_version), onlineDbVersion);
            editor.commit();
        }
    }

    public static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    private int getOnlineDbVersion(int localDbVersion) {
        InputStream is = null;
        HttpURLConnection conn = null;

        try {
            String strURL = ONLINE_DB_URL + "/" + VERSION_SUFFIX;
            URL url = new URL(strURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
                String contentAsString = readIt(is).replaceAll("[^\\d]", "");
                return Integer.parseInt(contentAsString);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if(is != null)is.close();
                if(conn != null) conn.disconnect();
            } catch(Exception e) {}
        }

        return localDbVersion;
    }

    private List<Dish> dishListFromNet() {
        List<Dish> dishList = new ArrayList<>();

        InputStream is = null;
        HttpURLConnection conn = null;

        try {
            String strURL = ONLINE_DB_URL + "/" + RECIPES_SUFFIX;
            URL url = new URL(strURL);
            conn = (HttpURLConnection) url.openConnection();

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

                for (int i = 0; i < jDishArray.length(); i++) {
                    JSONObject oneRecipe = jDishArray.getJSONObject(i);
                    Dish dish = DishFromJSON(oneRecipe);

                    dishList.add(dish);

                    publishProgress((int) ((i / (float) jDishArray.length()) * 100));
                }
                publishProgress(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(is != null) is.close();
                if(conn != null) conn.disconnect();
            }catch(Exception e){}
        }

        return  dishList;
    }

    protected void onProgressUpdate(Integer... progress) {
        ProgressBar progressBar = (ProgressBar) myActivity.get().findViewById(R.id.main_progress_bar);
        progressBar.setMax(100);
        progressBar.setProgress(progress[0]);
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

    public List<Dish> getActual(Context context, SharedPreferences sharedPreferences) {
        updateLocalDb(context, sharedPreferences);
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
            e.printStackTrace();
        }

        return new Dish(dishName, ingredients, cookSteps, dishImage);
    }

    Bitmap downloadImage(String imageUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
        try {
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream input = conn.getInputStream();
                Bitmap result = BitmapFactory.decodeStream(input);
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            conn.disconnect();
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

    public SyncDishesTask(Activity activity, DishFragment fragment) {
        myActivity = new WeakReference<>(activity);
        myFragment = new WeakReference<>(fragment);
    }

    protected Boolean doInBackground(Void... p) {
        DishFragment.DISHES = getActual(myActivity.get(), myActivity.get().getPreferences(Context.MODE_PRIVATE));
        return Boolean.TRUE;
    }

    protected void onPostExecute(Boolean result) {
        myFragment.get().dishChangesNotify();
    }
}
