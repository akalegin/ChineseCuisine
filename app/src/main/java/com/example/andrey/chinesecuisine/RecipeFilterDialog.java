package com.example.andrey.chinesecuisine;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class RecipeFilterDialog extends DialogFragment {
    public interface Listener {
        public void onDialogPositiveClick(DialogFragment dialog, List<String> selectedTags);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    Listener mListener;
    List<String> mTags = new ArrayList<>();
    List<String> mSelectedTags = new ArrayList<>();
    boolean[] mCheckedItems;
    boolean[] mLastCheckedItems;

    public RecipeFilterDialog() {
    }

    public void setTags(List<String> tags, Context context) {
        mTags.clear();
        mTags.add(context.getResources().getString(R.string.ALL));
        mTags.addAll(tags);
        if (mCheckedItems == null || mCheckedItems.length != mTags.size()) {
            mCheckedItems = new boolean[mTags.size()];
            mLastCheckedItems = new boolean[mTags.size()];
            for (int i = 0; i < mCheckedItems.length; ++i) {
                mCheckedItems[i] = true;
                mLastCheckedItems[i] = true;
            }
        }
    }

    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.recipe_filter, container, false);
    }*/

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title
        builder.setTitle(R.string.search_title)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(mTags.toArray(new String[mTags.size()]),
                        mCheckedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {

                                ListView list = ((AlertDialog) dialog).getListView();
                                if (isChecked) {
                                    if (which == 0) {
                                        for (int i = 0; i < list.getCount(); ++i) {
                                            list.setItemChecked(i, true);
                                            mCheckedItems[i] = true;
                                        }
                                        mSelectedTags.clear();
                                        mSelectedTags.addAll(mTags);
                                    } else
                                        mSelectedTags.add(mTags.get(which));
                                } else {
                                    if (which == 0) {
                                        for (int i = 0; i < list.getCount(); ++i) {
                                            list.setItemChecked(i, false);
                                            mCheckedItems[i] = false;
                                        }
                                        mSelectedTags.clear();
                                    } else
                                        mSelectedTags.remove(mTags.get(which));
                                }
                            }
                        })
                        // Set the action buttons
                .setPositiveButton(R.string.start_search, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the positive button event back to the host activity
                        List<String> selectedTags = new ArrayList<String>();
                        selectedTags.addAll(mSelectedTags);
                        selectedTags.remove(mTags.get(0));
                        mListener.onDialogPositiveClick(RecipeFilterDialog.this, selectedTags);
                        for (int i = 0; i < mCheckedItems.length; ++i) {
                            mLastCheckedItems[i] = mCheckedItems[i];
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(RecipeFilterDialog.this);
                        ListView list = ((AlertDialog) dialog).getListView();
                        mSelectedTags.clear();
                        for (int i = 0; i < mCheckedItems.length; ++i) {
                            mCheckedItems[i] = mLastCheckedItems[i];
                            list.setItemChecked(i, mLastCheckedItems[i]);
                            if (mLastCheckedItems[i]) {
                               mSelectedTags.add(mTags.get(i));
                            }
                        }
                    }
                });

        return builder.create();
    }


}
