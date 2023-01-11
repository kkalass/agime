package de.kalass.android.common.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import androidx.fragment.app.Fragment;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.common.collect.ImmutableList;

/**
 * Created by klas on 19.02.14.
 */
public class UnifiedContextBarSupport {

    public static <F extends Fragment & CABMultiChoiceCallback> void onCreateContextMenu(
            F fragment,
            ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        onCreateContextMenu(fragment.getActivity(), fragment, menu, v, menuInfo);
    }

    public static void onCreateContextMenu(Activity activity,
                                           CABMultiChoiceCallback callback,
                                           ContextMenu menu, View v,
                                           ContextMenu.ContextMenuInfo menuInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            callback.onCreateUnifiedContextBar(activity.getMenuInflater(), menu);
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            callback.onUnifiedContextBarSelectionChanged(menu, ImmutableList.of(info.id), ImmutableList.of(info.position));
        }
    }


    public static boolean onContextItemSelected(CABMultiChoiceCallback callback, MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        return callback.onUnifiedContextBarItemSelected(item.getItemId(), ImmutableList.of(info.id), ImmutableList.of(info.position));
    }

    public static <F extends Fragment & CABMultiChoiceCallback> void setup(F fragment, ListView listView) {
        setup(fragment, fragment, listView);
    }

    public static void setup(Fragment fragment, CABMultiChoiceCallback callback, ListView listView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setupV11(fragment, callback, listView);
        } else {
            // normal context menu
            fragment.registerForContextMenu(listView);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void setupV11(Fragment fragment, CABMultiChoiceCallback callback, ListView listView) {
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new CABMultiChoiceModeListener(fragment.getActivity(), callback));
    }
}
