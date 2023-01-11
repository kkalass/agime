package de.kalass.agime.trackactivity;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.LocalDate;

import java.util.concurrent.atomic.AtomicReference;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.loader.ProjectModelQuery;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.provider.CRUDContentItem;

/**
* Created by klas on 21.01.14.
*/
class ProjectAutocompleteAdapter extends SimpleCursorAdapter
        implements SimpleCursorAdapter.CursorToStringConverter {
    private final Context _context;
    private AtomicReference<LocalDate> currentDayRef;

    public ProjectAutocompleteAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1, null, new String[]{MCContract.Project.COLUMN_NAME_NAME}, new int[]{android.R.id.text1}, 0);
        _context = context;
        this.currentDayRef = new AtomicReference<LocalDate>();
        setCursorToStringConverter(this);
    }

    public void setDate(LocalDate date) {
        this.currentDayRef.set(date);
    }
    @Override
    public CharSequence convertToString(Cursor cursor) {
        return cursor.getString(ProjectModelQuery.COLUMN_IDX_NAME);
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        LocalDate date = currentDayRef.get();
        Long millis = date == null ? LocalDate.now().toDateTimeAtStartOfDay().getMillis() : date.toDateTimeAtStartOfDay().getMillis();
        return ContentResolverUtil.queryByName(
                _context.getContentResolver(),
                constraint,
                ProjectModelQuery.URI,
                // Normal selection - match name
                ProjectModelQuery.COLUMN_NAME_NAME + " like ? AND (" + ProjectModelQuery.COLUMN_NAME_ACTIVE_UNTIL_MILLIS + " IS NULL OR " + ProjectModelQuery.COLUMN_NAME_ACTIVE_UNTIL_MILLIS + " >= ? )",
                new String[]{"%" + constraint + "%", millis + ""},
                // select all - if the entered name is matched fully
                ProjectModelQuery.COLUMN_NAME_ACTIVE_UNTIL_MILLIS + " IS NULL OR " + ProjectModelQuery.COLUMN_NAME_ACTIVE_UNTIL_MILLIS + " >= ? ",
                new String[]{millis + ""},
                ProjectModelQuery.COLUMN_NAME_NAME + " asc",
                ProjectModelQuery.COLUMN_IDX_NAME,
                CRUDContentItem.COLUMN_NAME_ID,
                ProjectModelQuery.READ_ID,
                ProjectModelQuery.PROJECTION
        );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        Cursor cursor = (Cursor)getItem(position);
        int colorCodeColumn = ProjectModelQuery.COLUMN_IDX_COLOR;
        Integer colorCode = cursor.isNull(colorCodeColumn) ? null : cursor.getInt(colorCodeColumn);

        final Resources resources = _context.getResources();
        view.setBackgroundColor(ColorSuggestion.suggestProjectColor(
                resources, (int) getItemId(position), colorCode));
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(resources.getColor(R.color.project_text));
        }

        return view;
    }
}
