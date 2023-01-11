package de.kalass.android.common.support.datetime;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Build;
import androidx.fragment.app.FragmentManager;
import android.widget.DatePicker;

import org.joda.time.LocalDate;

/**
 * Created by klas on 22.12.13.
 */
public final class DatePickerSupport {

    private DatePickerSupport() {

    }

    private static class DatePickerAdapter implements

            DatePickerDialog.OnDateSetListener {
        private int _token;
        private final LocalDateSelectedListener _listener;

        DatePickerAdapter(LocalDateSelectedListener listener, int token) {
            _listener = listener;
            _token = token;
        }

        private LocalDate asLocalDate(int year, int month, int day) {
            return new LocalDate(year, month + 1, day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            _listener.onDateSelected(_token, asLocalDate(year, month, day));
        }
    }

    public interface LocalDateSelectedListener {
        void onDateSelected(int token, LocalDate localDate);
    }

    public static void showDatePickerDialog(
            Context context,
            FragmentManager fm,
            int token,
            LocalDateSelectedListener dateSelectedListener,
            LocalDate date
    ) {
        showDatePickerDialog(context, fm, token, dateSelectedListener, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
    }

    public static void showDatePickerDialog(
            Context context,
            FragmentManager fm,
            int token,
            String title,
            LocalDateSelectedListener dateSelectedListener,
            LocalDate date
    ) {
        showDatePickerDialog(context, fm, token, title, dateSelectedListener, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
    }


    public static void showDatePickerDialog( Context context,
                                             FragmentManager fm,
                                             int token,
                                             LocalDateSelectedListener dateSelectedListener,
                                             int year, int month, int day
    ) {

        // either too old (in theory supported, but in practice unstable), or so new that the backport is not needed
        final DatePickerDialog timePickerDialog =
                new DatePickerDialog(
                        context,
                        new DatePickerAdapter(dateSelectedListener, token),
                        year,
                        month,
                        day);
        timePickerDialog.show();
    }


    public static void showDatePickerDialog( Context context,
                                             FragmentManager fm,
                                             int token,
                                             String title,
                                             LocalDateSelectedListener dateSelectedListener,
                                             int year, int month, int day
    ) {
        final DatePickerDialog timePickerDialog;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            timePickerDialog = new DatePickerDialog(
                    context,
                    AlertDialog.THEME_HOLO_LIGHT,
                    new DatePickerAdapter(dateSelectedListener, token),
                    year,
                    month,
                    day);
        } else {
            timePickerDialog = new DatePickerDialog(
                    context,
                    //AlertDialog.THEME_HOLO_LIGHT,
                    new DatePickerAdapter(dateSelectedListener, token),
                    year,
                    month,
                    day);
        }
        timePickerDialog.setTitle(title);
        timePickerDialog.show();
    }

}
