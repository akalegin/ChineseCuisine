package com.example.andrey.chinesecuisine;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import android.widget.Filter;

public class RecipeFilteredListAdapter<ImageObject> extends ArrayAdapter<ImageObject> {

    private List<ImageObject> objects;
    private WeakReference<Context> context;
    private Filter filter;

    public RecipeFilteredListAdapter(Context context, int resourceId, List<ImageObject> objects) {
        super(context, resourceId, objects);
        this.context = new WeakReference<>(context);
        this.objects = objects;
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public ImageObject getItem(int position) {
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;//objects.get(position).getId();
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new AppFilter<ImageObject>(objects);
        return filter;
    }

    public void setSelectedTags(List<String> selectedTags) {
        ((AppFilter<ImageObject>)getFilter()).setSelectedTags(selectedTags);
    }

    /**
     * Really it is String-only filter
     */
    private class AppFilter<T> extends Filter {

        private ArrayList<T> sourceObjects;
        private ArrayList<String> mSelectedTags = new ArrayList<>();

        public AppFilter(List<T> objects) {
            sourceObjects = new ArrayList<T>();
            synchronized (this) {
                sourceObjects.addAll(objects);
            }
        }

        public void setSelectedTags(List<String> selectedTags) {
            synchronized (this) {
                mSelectedTags.clear();
                mSelectedTags.addAll(selectedTags);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence chars) {
            FilterResults result = new FilterResults();

            RecipeReaderDbHelper dbHelper = new RecipeReaderDbHelper(context.get());

            synchronized (this) {
                List<String> filteredRecipes = dbHelper.getRecipesByTags(mSelectedTags);
                result.values = filteredRecipes;
                result.count = filteredRecipes.size();
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            // NOTE: this function is *always* called from the UI thread.
            ArrayList<T> filtered = (ArrayList<T>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0, l = filtered.size(); i < l; i++)
                add((ImageObject) filtered.get(i));
            notifyDataSetInvalidated();
        }
    }

}
