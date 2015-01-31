package com.example.andrey.chinesecuisine;

import android.graphics.Bitmap;

public class Dish  {
    private String myName;
    private String myIngredients;
    private String myCookSteps;
    private Bitmap myImage;

    public Dish(String name, String ingredients, String cookSteps, Bitmap image) {
        this.myName = name;
        this.myIngredients = ingredients;
        this.myCookSteps = cookSteps;
        this.myImage = image;
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
}
