package com.example.andrey.chinesecuisine;

import android.provider.BaseColumns;

public class RecipeReaderContract {
    public RecipeReaderContract() {}

    public static abstract class RecipeEntry implements BaseColumns {
        public static final String TABLE_NAME = "recipes";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_INGREDIENTS = "ingredients";
        public static final String COLUMN_NAME_COOK_STEPS = "cook_steps";
        public static final String COLUMN_NAME_IMAGE = "image";
    }

    public static abstract class TagEntry implements BaseColumns {
        public static final String TABLE_NAME = "tags";
        public static final String COLUMN_NAME_NAME = "name";
    }

    public static abstract class RecipeTagEntry implements BaseColumns {
        public static final String TABLE_NAME = "recipes_tags";
        public static final String COLUMN_NAME_RECIPE_ID = "recipe_id";
        public static final String COLUMN_NAME_TAG_ID = "tag_id";
    }
}
