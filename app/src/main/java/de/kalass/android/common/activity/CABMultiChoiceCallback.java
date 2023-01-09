package de.kalass.android.common.activity;

import android.view.Menu;
import android.view.MenuInflater;

import java.util.List;

/**
* Created by klas on 19.02.14.
*/
public interface CABMultiChoiceCallback {

    boolean onUnifiedContextBarItemSelected(int itemId, List<Long> entityIds, List<Integer> selectedPositions);

    void onUnifiedContextBarSelectionChanged(Menu menu, List<Long> newEntityIds, List<Integer> selectedPositions);

    void onCreateUnifiedContextBar(MenuInflater menuInflater, Menu menu);
}
