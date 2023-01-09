package de.kalass.agime.overview.model;

import java.util.Collection;

import de.kalass.agime.model.TrackedActivityModel;

/**
 * Created by klas on 27.03.15.
 */
public interface TrackedActivityModelContainer {
    Iterable<TrackedActivityModel> getActivities();
}
