package com.example.andrey.chinesecuisine;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class RecipeFragment extends Fragment {
    final static String ARG_DISH_NAME = "DISH_NAME";
    String mCurrentDishName = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
            mCurrentDishName = savedInstanceState.getString(ARG_DISH_NAME);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.recipe_view, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        // During startup, check if there are arguments passed to the fragment.
        // onStart is a good place to do this because the layout has already been
        // applied to the fragment at this point so we can safely call the method
        // below that sets the article text.
        Bundle args = getArguments();
        if (args != null) {
            // Set article based on argument passed in
            updateRecipeView(args.getString(ARG_DISH_NAME));
        } else if (!mCurrentDishName.equals("")) {
            // Set article based on saved instance state defined during onCreateView
            updateRecipeView(mCurrentDishName);
        }
    }

    public void updateRecipeView(String dishName) {
        AsyncTask<String, Integer, Dish> dishExtractTask = new AsyncTask<String, Integer, Dish>(){
            protected Dish doInBackground(String... p) {

                RecipeReaderDbHelper dbHelper = new RecipeReaderDbHelper(getActivity());

                return dbHelper.getDishByName(p[0]);
            }

            protected void onPostExecute(Dish currentDish) {
                Resources res = getResources();
                TextView recipe = (TextView) getActivity().findViewById(R.id.recipe);

                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("<H1>");
                stringBuilder.append(getString(R.string.ingredients));
                stringBuilder.append("</H1>");
                stringBuilder.append("<br>");

                stringBuilder.append(currentDish.getIngredients());

                stringBuilder.append("<br>");
                stringBuilder.append("<H1>");
                stringBuilder.append(getString(R.string.cook_steps));
                stringBuilder.append("</H1>");
                stringBuilder.append("<br>");

                stringBuilder.append(currentDish.getCookSteps());

                recipe.setText(Html.fromHtml(stringBuilder.toString()));

                ImageView dishImageView = (ImageView) getActivity().findViewById(R.id.dinner_is_served);
                if (currentDish.getImage() != null) {
                    //dishImageView.setImageDrawable(res.obtainTypedArray(R.array.dish_img_array).getDrawable(position));
                    dishImageView.setImageDrawable(new BitmapDrawable(res, currentDish.getImage()));
                    dishImageView.setVisibility(View.VISIBLE);
                } else {
                    dishImageView.setVisibility(View.INVISIBLE);
                }
            }

        };
        dishExtractTask.execute(dishName);

        mCurrentDishName = dishName;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current article selection in case we need to recreate the fragment
        outState.putString(ARG_DISH_NAME, mCurrentDishName);
    }

}