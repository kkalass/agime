package de.kalass.android.common.widget;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;

import com.google.common.base.Preconditions;

/**
 * An AutoCompleteTextView that can be queried for the selected item.
 *
 * Created by klas on 06.11.13.
 */
public class AutoCompleteSpinner extends AutoCompleteTextView {



    public interface OnItemSetListener {
        /**
         * @param position the position of the item in the list
         * @param itemId the Id of the item
         * @param userSelectedExplicitely the user did explicitely select the item, not just implicitely e.g. by typing the text
         */
        void onItemSet(AutoCompleteSpinner spinner, boolean userSelectedExplicitely, int position, long itemId);
        void onItemReset(AutoCompleteSpinner spinner);
    }

    private final class ActivityTypeSelectionListener
            implements AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener, TextWatcher {
        private final ListAdapter _adapter;
        private long _id;
        private int _position;
        private boolean _itemSet;

        ActivityTypeSelectionListener(ListAdapter adapter) {
            _adapter = Preconditions.checkNotNull(adapter);
        }

        public boolean isCurrentItemSet() {
            return _itemSet;
        }

        public long getCurrentItemId() {
            return _id;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            setItem(true, position, id);
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            setItem(true, position, id);
        }

        private void setItem(boolean userSelectedExplicitely, int position, long id) {
            _id = id;
            _position = position;
            _itemSet = true;
            OnItemSetListener listener = getOnItemSetListener();
            if (listener != null) {
                listener.onItemSet(AutoCompleteSpinner.this, userSelectedExplicitely, position, id);
            }
        }

        private void resetItem() {
            _itemSet = false;
            _id = -1;
            _position = -1;
            OnItemSetListener listener = getOnItemSetListener();
            if (listener != null) {
                listener.onItemReset(AutoCompleteSpinner.this);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            resetItem();
        }

        private String _textBeforeChange = null;
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            _textBeforeChange = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String newString = s.toString().trim();
            if (_textBeforeChange == null || !_textBeforeChange.equals(newString)) {
                Integer existingPosition = getMatchingItemPosition(newString);
                if (existingPosition != null) {
                    int pos = existingPosition.intValue();
                    long id = _adapter.getItemId(pos);
                    setItem(false /*implicit match*/, pos, id);
                } else {
                    resetItem();
                }

                _textBeforeChange = null;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }

        private Integer getMatchingItemPosition(String value) {
            ListAdapter adapter = getAdapter();
            if (!(adapter instanceof Filterable)) {
                return null;
            }
            Filter f = ((Filterable)adapter).getFilter();

            int count = adapter.getCount();
            for (int i = 0; i < count; i++) {
                Object item = adapter.getItem(i);
                CharSequence result = f.convertResultToString(item);
                if (result != null && value.equals(result.toString().trim())) {
                    return i;
                }
            }
            return null;
        }
    }

    private OnItemSetListener _onItemSetListener;
    private ActivityTypeSelectionListener _activityTypeSelectionListener;

    public AutoCompleteSpinner(Context context) {
        super(context);
    }

    public AutoCompleteSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoCompleteSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void setupListener(ListAdapter adapter) {
        _activityTypeSelectionListener = new ActivityTypeSelectionListener(adapter);
        setOnItemSelectedListener(_activityTypeSelectionListener);
        setOnItemClickListener(_activityTypeSelectionListener);
        addTextChangedListener(_activityTypeSelectionListener);
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forceShowDropDown();
            }
        });
        _activityTypeSelectionListener.resetItem();
    }

    /**
     *
     * @return -1 if no item is currently set
     */
    public long getCurrentItemId() {
        if (!_activityTypeSelectionListener._itemSet) {
            return -1;
        }
        return _activityTypeSelectionListener._id;
    }

    /*
    public Object getCurrentItem() {
        SimpleListAdapter adapter = getAdapter();
        if (!_activityTypeSelectionListener._itemSet) {
            return null;
        }
        long position = _activityTypeSelectionListener._position;
        long currentId = adapter.getItemId(_activityTypeSelectionListener._position);
        if (currentId != _activityTypeSelectionListener._id) {
            throw new IllegalStateException("Stale state: current Item is " + currentId
                    + ", expected " + _activityTypeSelectionListener._id);
        }
        Object item = adapter.getItem(_activityTypeSelectionListener._position);
        return item;
    }
*/
    public void setCurrentItem(long id, String textValue) {
        if (id < 0) {
            _activityTypeSelectionListener.resetItem();
        } else {
            /* Marker that the value was set programmatically and will probably not be present
             * in the adapter that contains the suggestions
             */
            final int position  = -1;
            _activityTypeSelectionListener.setItem(true, position, id);
        }
        setText(textValue);
    }


    @Override
    public <T extends ListAdapter & Filterable> void setAdapter(T adapter) {
        super.setAdapter(adapter);
        setupListener(adapter);
    }

    public void setOnItemSetListener(OnItemSetListener listener) {
        _onItemSetListener = listener;
    }

    public OnItemSetListener getOnItemSetListener() {
        return _onItemSetListener;
    }
    @Override
    public boolean enoughToFilter() {
        return true;
    }


    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            forceShowDropDown();
        }
    }

    public void forceShowDropDown() {
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter.getCount() > 0) {
            // has been filled before
            String currentText = getText().toString();
            setText(currentText);
        } else {
            performFiltering(getText(), 0);
        }
        selectAll();
    }
}
