package de.kalass.agime;

import org.joda.time.LocalDate;

/**
 * Created by klas on 13.01.14.
 */
public interface LocalDateSpanning {
    String ARG_INITIAL_DATE_MILLIS = "argsInitialDateMillis";

    LocalDate getInitialDate();
    LocalDate getStartDate();
    LocalDate getEndDate();
}
