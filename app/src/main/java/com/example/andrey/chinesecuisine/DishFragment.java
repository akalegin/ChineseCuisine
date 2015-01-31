package com.example.andrey.chinesecuisine;

import android.app.Activity;
import android.support.v4.app.ListFragment;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;


public class DishFragment extends ListFragment {
    public static List<Dish> DISHES = new ArrayList<>();
    private OnDishSelectedListener mCallback;

    // The container Activity must implement this interface so the frag can deliver messages
    public interface OnDishSelectedListener {
        /** Called by HeadlinesFragment when a list item is selected */
        public void onDishSelected(int position);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need to use a different list item layout for devices older than Honeycomb
        int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;

        syncDishesWithNet();

        // Create an array adapter for the list view, using the Ipsum dishes array
        //setListAdapter(new ArrayAdapter<String>(getActivity(), layout, getResources().getStringArray(R.array.dishes)));
        setListAdapter(new ArrayAdapter<>(getActivity(), layout, getDishNames()));
    }

    private void syncDishesWithNet() {
        DISHES = DishDBNetUpdater.INSTANCE.getActual(getActivity());
    }

    String[] getDishNames() {
        String[] result = new String[DISHES.size()];
        for (int i = 0; i < DISHES.size(); ++i) {
            result[i] = DISHES.get(i).getName();
        }
        return result;
    }

    @Override
    public void onStart() {
        super.onStart();

        // When in two-pane layout, set the listview to highlight the selected list item
        // (We do this during onStart because at the point the listview is available.)
        if (getFragmentManager().findFragmentById(R.id.recipe_fragment) != null) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnDishSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDishSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Notify the parent activity of selected item
        mCallback.onDishSelected(position);

        // Set the item as checked to be highlighted when in two-pane layout
        getListView().setItemChecked(position, true);
    }
}
