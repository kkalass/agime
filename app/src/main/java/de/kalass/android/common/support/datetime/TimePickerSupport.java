package de.kalass.android.common.support.datetime;

import android.app.TimePickerDialog;
import android.content.Context;
import androidx.fragment.app.FragmentManager;
import android.text.format.DateFormat;
import android.widget.TimePicker;


import org.joda.time.LocalTime;

/**
 * Created by klas on 22.12.13.
 */
public class TimePickerSupport {
    private static class TimePickerAdapter implements
            TimePickerDialog.OnTimeSetListener {
        private final int _token;
        private final LocalTimeSelectedListener _listener;

        TimePickerAdapter(int token, LocalTimeSelectedListener listener) {
            _listener = listener;
            _token = token;
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            _listener.onTimeSelected(_token, new LocalTime(hourOfDay, minute));
        }

    }

    public interface LocalTimeSelectedListener {
        void onTimeSelected(int token, LocalTime time);
    }

    public static void showTimePickerDialog(
            Context context,
            FragmentManager fm,
            int token,
            LocalTimeSelectedListener timeSelectedListener,
            int hour,
            int minute
    ) {

        final TimePickerDialog timePickerDialog =
                new TimePickerDialog(
                        context,
                        new TimePickerAdapter(token, timeSelectedListener),
                        hour,
                        minute,
                        DateFormat.is24HourFormat(context));
        timePickerDialog.show();
    }

    public static void showTimePickerDialog(
            Context context,
            FragmentManager fm,
            int token,
            LocalTimeSelectedListener timeSelectedListener,
            LocalTime time
    ) {
        showTimePickerDialog(context, fm, token, timeSelectedListener, time.getHourOfDay(), time.getMinuteOfHour());
    }
}
