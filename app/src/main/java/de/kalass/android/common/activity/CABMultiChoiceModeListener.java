package de.kalass.android.common.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.Set;

import de.kalass.agime.R;

/**
* Created by klas on 18.02.14.
*/
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class CABMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {
    private final Context context;
    private final CABMultiChoiceCallback callback;
    private final Set<Long> selectedEntityIds = new HashSet<Long>();
    private final Set<Integer> selectedPositions = new HashSet<Integer>();

    public CABMultiChoiceModeListener(Context context, CABMultiChoiceCallback callback) {
        this.context = context;
        this.callback = callback;
    }


    private Set<Long> getSelectedIds() {
        // Note: we are not using getListView().getCheckedItemIds()
        // because that one is only valid if getListAdapter().hasStableIds() returns true
        return selectedEntityIds;
    }

    @Override
    public void onItemCheckedStateChanged(android.view.ActionMode mode, int position,
                                          long id, boolean checked) {
        // Here you can do something when items are selected/de-selected,
        // such as update the title in the CAB
        if (checked) {
            selectedEntityIds.add(id);
            selectedPositions.add(position);
        } else {
            selectedEntityIds.remove(id);
            selectedPositions.remove(id);
        }

        callback.onUnifiedContextBarSelectionChanged(mode.getMenu(),
                ImmutableList.copyOf(selectedEntityIds),
                ImmutableList.copyOf(selectedPositions));

        final int size = selectedEntityIds.size();
        mode.setTitle(context.getResources().getQuantityString(R.plurals.cab_title, size, size));
    }

    @Override
    public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
        // Respond to clicks on the actions in the CAB

        if (callback.onUnifiedContextBarItemSelected(item.getItemId(),
                ImmutableList.copyOf(getSelectedIds()),
                ImmutableList.copyOf(selectedPositions)
        )) {
            mode.finish();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
        // Inflate the menu for the CAB
        callback.onCreateUnifiedContextBar(mode.getMenuInflater(), menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(android.view.ActionMode mode) {
        // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
        selectedEntityIds.clear();
        selectedPositions.clear();
    }

    @Override
    public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
        // Here you can perform updates to the CAB due to
        // an invalidate() request
        return false;
    }
}
