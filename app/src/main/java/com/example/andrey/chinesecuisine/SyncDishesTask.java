package com.example.andrey.chinesecuisine;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SyncDishesTask extends AsyncTask<Void, Integer, List<String>> {
    private final WeakReference<Activity> myActivity;
    private final WeakReference<DishListFragment> myFragment;

    private static String ONLINE_DB_URL = "http://kalegin-chinese-cuisine.appspot.com";
    private static String RECIPES_SUFFIX = "recipes";
    private static String VERSION_SUFFIX = "version";

    private void updateLocalDb(Context context, SharedPreferences sharedPreferences, RecipeReaderDbHelper dbHelper) {
        // Если текущая версия базы данных меньше той, что доступна на сервере, то
        // произведём обновление.
        int defaultDbVersion = context.getResources().getInteger(R.integer.default_db_version);
        int localDbVersion = sharedPreferences.getInt(context.getResources().getString(R.string.local_db_version), defaultDbVersion);
        int onlineDbVersion = getOnlineDbVersion(localDbVersion);

        if (onlineDbVersion > localDbVersion) {

            onlineDataToDb(dbHelper);

            //Сохраним информацию о версии локального набора данных
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(context.getResources().getString(R.string.local_db_version), onlineDbVersion);
            editor.commit();
        }
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

    private void onlineDataToDb(RecipeReaderDbHelper dbHelper) {
        InputStream is = null;
        HttpURLConnection conn = null;
        SQLiteDatabase db = null;

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

                // Gets the data repository in write mode
                db = dbHelper.getWritableDatabase();

                for (int i = 0; i < jDishArray.length(); i++) {
                    JSONObject oneRecipe = jDishArray.getJSONObject(i);
                    Dish dish = DishFromJSON(oneRecipe);

                    dbHelper.addDishToDB(dish, db);

                    publishProgress((int) ((i / (float) jDishArray.length()) * 100));
                }
                publishProgress(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (conn != null) conn.disconnect();
                if (db != null) db.close();
            }catch(Exception e){}
        }
    }

    protected void onProgressUpdate(Integer... progress) {
        ProgressBar progressBar = (ProgressBar) myActivity.get().findViewById(R.id.main_progress_bar);
        progressBar.setMax(100);
        progressBar.setProgress(progress[0]);
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
                return BitmapFactory.decodeStream(input);
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

    public SyncDishesTask(Activity activity, DishListFragment fragment) {
        myActivity = new WeakReference<>(activity);
        myFragment = new WeakReference<>(fragment);
    }

    protected List<String> doInBackground(Void... p) {
        RecipeReaderDbHelper dbHelper = new RecipeReaderDbHelper(myActivity.get());
        updateLocalDb(myActivity.get(), myActivity.get().getPreferences(Context.MODE_PRIVATE), dbHelper);
        return dbHelper.getDishNames();
    }

    protected void onPostExecute(List<String> result) {
        myFragment.get().dishChangesNotify(result);
    }
}
