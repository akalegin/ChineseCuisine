package com.example.andrey.chinesecuisine;

import android.graphics.Bitmap;
import java.util.List;

public class Dish  {
    private String myName;
    private List<String> myIngredients;
    private List<String> myCookSteps;
    private Bitmap myImage;

    public Dish(String name, List<String> ingredients, List<String> cookSteps, Bitmap image) {
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

    public List<String> getIngredients() {
        return myIngredients;
    }

    public List<String> getCookSteps() {
        return myCookSteps;
    }
}
