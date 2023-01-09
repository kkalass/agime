package de.kalass.android.common.activity;

import android.content.Context;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import org.joda.time.DateTime;

import de.kalass.android.common.widget.AutoCompleteSpinner;

import static com.google.common.base.Preconditions.checkNotNull;

/**
* Created by klas on 20.01.14.
*/
public abstract class BaseViewWrapper {
    public final View view;

    public BaseViewWrapper(View view) {
        this.view = Preconditions.checkNotNull(view, "cannot create a view wrapper with a null view");
    }

    protected View getView(int id) {
        return checkNotNull(view.findViewById(id));
    }

    protected Button getButton(int id) {
        return get(Button.class, id);
    }

    protected EditText getEditText(int id) {
        return get(EditText.class, id);
    }

    protected TextView getTextView(int id) {
        return get(TextView.class, id);
    }

    protected AutoCompleteSpinner getAutoCompleteSpinner(int id) {
        return get(AutoCompleteSpinner.class, id);
    }

    protected Spinner getSpinner(int id) {
        return get(Spinner.class, id);
    }

    protected LinearLayout getLinearLayout(int id) {
        return get(LinearLayout.class, id);
    }

    protected <T extends View> T get(Class<T> cls, int id) {
        return cls.cast(checkNotNull(view.findViewById(id)));
    }

    protected <T extends Adapter> void setAdapter(AdapterView<? super T> view, T adapter) {
        if (view.getAdapter() == null) {
            view.setAdapter(adapter);
        }
    }

    protected <T extends ListAdapter & Filterable> void setAdapter(AutoCompleteTextView view, T adapter) {
        if (view.getAdapter() == null) {
            view.setAdapter(adapter);
        }
    }


    protected Context getContext() {
        return view.getContext();
    }
}
