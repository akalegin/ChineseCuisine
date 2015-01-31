package com.example.andrey.chinesecuisine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
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

    public List<Dish> getActual() {
        List<Dish> result = new ArrayList<>();

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

                    result.add(dish);
                }
            }
        } catch (Exception e){

        } finally {
            try{if(is != null)is.close();}catch(Exception e){}
        }

        return result;
    }

    public Dish DishFromJSON(JSONObject jObject) {
        String dishName = "";
        List<String> ingredients = new ArrayList<>();
        List<String> cookSteps = new ArrayList<>();
        Bitmap dishImage = null;

        try {
            dishName = jObject.getString("dish");

            JSONArray jIngredientsArray = jObject.getJSONArray("ingredients");

            for (int i = 0; i < jIngredientsArray.length(); ++i)
            {
                try {
                    ingredients.add(jIngredientsArray.getString(i));
                } catch (JSONException e) {
                    // Oops
                }
            }

            JSONArray jSequenceOfActions = jObject.getJSONArray("sequence_of_actions");

            for (int i = 0; i < jSequenceOfActions.length(); ++i)
            {
                try {
                    cookSteps.add(jSequenceOfActions.getString(i));
                } catch (JSONException e) {
                    // Oops
                }
            }

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
