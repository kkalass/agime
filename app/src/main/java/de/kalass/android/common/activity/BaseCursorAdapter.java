package de.kalass.android.common.activity;

import android.content.Context;
import android.database.Cursor;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by klas on 08.01.14.
 * @param <W> The java type of a container that wraps the relevant parts of the view instance for easy access
 */
public abstract class BaseCursorAdapter<W> extends CursorAdapter {

    private final Context _context;
    private final LayoutInflater _inflater;
    private final int _layout;

    /**
     * @param context The context
     * @param flags Flags used to determine the behavior of the adapter; may
     * be any combination of {@link #FLAG_AUTO_REQUERY} and
     * {@link #FLAG_REGISTER_CONTENT_OBSERVER}.
     */
    public BaseCursorAdapter(Context context, int layout, int flags) {
        super(context, null, flags);
        _layout = layout;
        _inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _context = context;
    }

    protected Context getContext() {
        return _context;
    }


    protected abstract W onWrapView(View view);

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view =  _inflater.inflate(_layout, parent, false);
        setWrappedView(view, onWrapView(view));
        return view;
    }

    protected final void setWrappedView(View view, W wrapped) {
        view.setTag(wrapped);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        bindWrappedView(getWrappedView(view), context, cursor);
    }

    public abstract void bindWrappedView(W view, Context context, Cursor cursor);

    protected final W getWrappedView(View view) {
        return (W)view.getTag();
    }
}
