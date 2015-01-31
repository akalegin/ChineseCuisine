package com.example.andrey.chinesecuisine;

import android.provider.BaseColumns;

public class RecipeReaderContract {
    public RecipeReaderContract() {}

    public static abstract class RecipeEntry implements BaseColumns {
        public static final String TABLE_NAME = "recipes";
        public static final String COLUMN_NAME_DISH = "dish";
        public static final String COLUMN_NAME_INGREDIENTS = "ingredients";
        public static final String COLUMN_NAME_COOK_STEPS = "cook_steps";
        public static final String COLUMN_NAME_IMAGE = "image";
    }
}
