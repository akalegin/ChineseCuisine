package com.example.andrey.chinesecuisine;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class Recipe
{
    public static class MainData {
        public MainData(List<String> recipeNames, List<String> tags) {
            this.recipeNames = recipeNames;
            this.tags = tags;
        }
        public List<String> recipeNames;
        public List<String> tags;
    }

    private String myName;
    private String myIngredients;
    private String myCookSteps;
    private Bitmap myImage;
    private List<String> myTags = new ArrayList<>();

    public Recipe(String name, String ingredients, String cookSteps, Bitmap image, List<String> tags) {
        this.myName = name;
        this.myIngredients = ingredients;
        this.myCookSteps = cookSteps;
        this.myImage = image;
        this.myTags.addAll(tags);
    }


    public String getName() {
        return myName;
    }

    public Bitmap getImage() {
        return myImage;
    }

    public String getIngredients() {
        return myIngredients;
    }

    public String getCookSteps() {
        return myCookSteps;
    }

    public List<String> getTags() { return myTags; }
}
