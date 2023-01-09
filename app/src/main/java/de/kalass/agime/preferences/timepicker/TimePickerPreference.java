package de.kalass.agime.preferences.timepicker;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TimePicker;

import com.google.common.base.Strings;

import java.util.Locale;

/**
 * Created by klas on 22.11.13.
 */
public class TimePickerPreference extends Preference implements TimePickerDialog.OnTimeSetListener {

    private static final String DEFAULT_VALUE = "00:00";
    private int _minute;
    private int _hour;

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        String value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readString();  // Change this to read the appropriate data type
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeString(value);  // Change this to write the appropriate data type
        }

        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public TimePickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimePickerPreference(Context context) {
        super(context);
    }

    public static int parseHour(String string) {
        return Integer.parseInt(string.substring(0, 2), 10);
    }

    public static int parseMinute(String string) {
        return Integer.parseInt(string.substring(3, 5), 10);
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Do something with the time chosen by the user
        _hour = hourOfDay;
        _minute = minute;
        persistString(serializeState());
    }

    private String serializeState() {
        return String.format(Locale.US, "%02d:%02d", _hour, _minute);
    }


    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if (restorePersistedValue) {
            // Restore existing state
            deserializeState(this.getPersistedString(DEFAULT_VALUE));
        } else {
            // Set default state from the XML attribute
            deserializeState((String) defaultValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        String defaultValue = a.getString(index);
        return Strings.isNullOrEmpty(defaultValue) ? DEFAULT_VALUE : defaultValue;
    }

    private void deserializeState(String persisted) {
        _minute = parseMinute(persisted);
        _hour = parseHour(persisted);
    }

    @Override
    protected void onClick() {
        // Create a new instance of TimePickerDialog and return it
        TimePickerDialog dlg = new TimePickerDialog(getContext(), this, _hour, _minute,
                DateFormat.is24HourFormat(getContext()));
        dlg.show();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent, use superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current setting value
        myState.value = serializeState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        deserializeState(myState.value);
    }
}
