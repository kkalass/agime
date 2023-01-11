package de.kalass.agime.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import de.kalass.android.common.model.IViewModel;

/**
 * View Model for activity data.
 * Created by klas on 21.10.13.
 */
public class ProjectModel implements IViewModel {

    private final long _id;
    private final String _name;
    private final Integer _colorCode;
    private final LocalDate _activeUntilDate;

    public ProjectModel(long id, String name, Integer colorCode, Long activeUntilMillis) {
        this(id, name, colorCode, activeUntilMillis == null ? null : new LocalDate(activeUntilMillis));
    }

    public ProjectModel(long id, String name, Integer colorCode, LocalDate activeUntilDate) {
        _id = id;
        _name = name;
        _colorCode = colorCode;
        _activeUntilDate = activeUntilDate;
    }

    public long getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }


    public Integer getColorCode() {
        return _colorCode;
    }

    public boolean isInactive() {
        return _activeUntilDate != null && _activeUntilDate.isBefore(LocalDate.now());
    }

    public boolean isProjectDurationLimited() {
        return _activeUntilDate != null;
    }

    public LocalDate getActiveUntilDate() {
        return _activeUntilDate;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(_id)
                .addValue(_name)
                .addValue(_activeUntilDate)
                .add("projectDurationLimited", isProjectDurationLimited())
                .toString();
    }
}
