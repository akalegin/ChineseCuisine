package com.example.andrey.chinesecuisine;

import android.app.Activity;
import android.support.v4.app.ListFragment;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

public class RecipeListFragment extends ListFragment {
    private OnRecipeDataChangedListener mCallback;
    private List<String> mDishNames = new ArrayList<>();

    // The container Activity must implement this interface so the frag can deliver messages
    public interface OnRecipeDataChangedListener {
        /** Called by HeadlinesFragment when a list item is selected */
        public void onRecipeSelected(String dishName);
    }

    public void applyFilter(List<String> selectedTags) {
        ((RecipeFilteredListAdapter<String>)getListAdapter()).setSelectedTags(selectedTags);
        ((RecipeFilteredListAdapter<String>)getListAdapter()).getFilter().filter(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need to use a different list item layout for devices older than Honeycomb
        int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;

        setListAdapter(new RecipeFilteredListAdapter<>(getActivity(), layout, mDishNames));

        // Create an array adapter for the list view, using the Ipsum dishes array
        //setListAdapter(new RecipeFilteredListAdapter<String>(getActivity(), layout, getResources().getStringArray(R.array.dishes)));
    }

    public void recipeListChanged(List<String> recipeNames) {
        mDishNames.clear();
        mDishNames.addAll(recipeNames);
        ((RecipeFilteredListAdapter<String>)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();

        // When in two-pane layout, set the listview to highlight the selected list item
        // (We do this during onStart because at the point the listview is available.)
        if (getFragmentManager().findFragmentById(R.id.recipe_view_fragment) != null) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnRecipeDataChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDishSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, final int position, long id) {
        mCallback.onRecipeSelected(mDishNames.get(position));
        // Set the item as checked to be highlighted when in two-pane layout
        getListView().setItemChecked(position, true);
    }
}
