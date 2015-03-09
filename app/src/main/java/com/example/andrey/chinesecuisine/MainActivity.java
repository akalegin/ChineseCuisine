package com.example.andrey.chinesecuisine;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity
        implements RecipeListFragment.OnRecipeDataChangedListener,
        RecipeFilterDialog.Listener {

    RecipeListFragment mListFragment;
    List<String> mTags = new ArrayList<>();
    RecipeFilterDialog mFilterDialog = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recipes_list);

        // Check whether the activity is using the layout version with
        // the fragment_container FrameLayout. If so, we must add the first fragment
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of ExampleFragment
            mListFragment = new RecipeListFragment();

            // In case this activity was started with special instructions from an Intent,
            // pass the Intent's extras to the fragment as arguments
            mListFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mListFragment).commit();
        } else {
            mListFragment = (RecipeListFragment)
                    getSupportFragmentManager().findFragmentById(R.id.recipe_list_fragment);
        }

        SyncRecipesTask task = new SyncRecipesTask(this);
        task.execute();
    }

    public void dishChangesNotify(Recipe.MainData result) {
        if (mListFragment != null) {
            mListFragment.recipeListChanged(result.recipeNames);
        }

        mTags.clear();
        mTags.addAll(result.tags);
    }

    public void onRecipeSelected(String recipeName) {
        // Capture the article fragment from the activity layout
        RecipeViewFragment recipeFrag = (RecipeViewFragment)
                getSupportFragmentManager().findFragmentById(R.id.recipe_view_fragment);

        if (recipeFrag != null) {
            // If article frag is available, we're in two-pane layout...

            // Call a method in the ArticleFragment to ic_update its content
            recipeFrag.updateRecipeView(recipeName);

        } else {
            // If the frag is not available, we're in the one-pane layout and must swap frags...

            // Create fragment and give it an argument for the selected article
            RecipeViewFragment newFragment = new RecipeViewFragment();
            Bundle args = new Bundle();
            args.putString(RecipeViewFragment.ARG_DISH_NAME, recipeName);
            newFragment.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                newSearch();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean newSearch() {
        if (mFilterDialog == null) {
            mFilterDialog = new RecipeFilterDialog();
        }
        mFilterDialog.setTags(mTags, this);

        mFilterDialog.show(getSupportFragmentManager(), "RecipeFilterDialog");

        return true;
    }

    public void onDialogPositiveClick(DialogFragment dialog, List<String> selectedTags) {
        mListFragment.applyFilter(selectedTags);
    }

    public void onDialogNegativeClick(DialogFragment dialog) {

    }
}