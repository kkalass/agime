package de.kalass.android.common.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.google.common.base.Preconditions;

/**
 * Spinner with some enhancements.
 *
 * The default spinner will not notify the selection listener if the user clicked on the currently
 * selected item. This class <b>will</b> notify for all clicks.
 *
 * Created by klas on 06.12.13.
 */
public class KKSpinner extends Spinner {

    private OnItemSelectedListener _listener;

    public KKSpinner(Context context) {
        super(context);
    }

    /*
    public KKSpinner(Context context, int mode) {
        super(context, mode);
    }

    public KKSpinner(Context context, AttributeSet attrs, int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
    }
    */

    public KKSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KKSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnItemSelectedByClickListener(OnItemSelectedListener listener) {
        _listener = listener;
    }

    @Override
    public void setSelection(final int position) {


        super.setSelection(position);

        /**
         * Really Evil Code: Because the Spinner does not support on item click listeners,
         * there is no clean way to be notified when the user selects an item even if
         * it is the currently selected item.
         *
         * The workaround here is to create a special listener and to call it always when
         * setSelection was called.
         *
         * To be a little bit safe from race conditions, delay calling of the listener.
         */
        if (_listener != null) {
            final OnItemSelectedListener l = _listener;
            post(new Runnable() {
                @Override
                public void run() {
                    Log.i("KKSpinner", "force call on item selected " + position);
                    View v = getSelectedView();
                    l.onItemSelected(KKSpinner.this, v, position, getAdapter().getItemId(position));
                }
            });
        }

    }
}
