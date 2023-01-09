package de.kalass.agime.overview.model;

import android.content.Context;

import de.kalass.agime.customfield.ActivityCustomFieldModel;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.model.TrackedActivityModel;

/**
 * Created by klas on 14.01.14.
 */
public class GroupHeaderTypes {
    public static final class ActivityType extends GroupHeaderType {

        public static final int GROUP_TYPE = ActivityTypeGroupHeader.GROUP_TYPE;

        public ActivityType(String title) {
            super(GROUP_TYPE, title);
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {
            return new ActivityTypeGroupHeader(trackedActivityModel.getActivityType());
        }
    }

    public static final class Category extends GroupHeaderType {

        public static final int GROUP_TYPE = CategoryGroupHeader.GROUP_TYPE;

        public Category(String title) {
            super(GROUP_TYPE, title);
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {
            return new CategoryGroupHeader(trackedActivityModel.getCategory());
        }
    }

    public static final class Project extends GroupHeaderType {

        public static final int GROUP_TYPE = ProjectGroupHeader.GROUP_TYPE;

        public Project(String title) {
            super(GROUP_TYPE, title);
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {
            return new ProjectGroupHeader(trackedActivityModel.getProject());
        }
    }

    public static final class Any extends GroupHeaderType {

        public static final int GROUP_TYPE = AnyGroupHeader.GROUP_TYPE;
        private final String _displayName;

        public Any(String title, String displayName) {
            super(GROUP_TYPE, title);
            _displayName = displayName;
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {
            return new AnyGroupHeader(_displayName);
        }
    }

    public static final class ByDay extends GroupHeaderType {

        public static final int GROUP_TYPE = ByDayGroupHeader.GROUP_TYPE;
        private final Context context;

        public ByDay(Context context, String title) {
            super(GROUP_TYPE, title);
            this.context = context;
        }

        public SortOrder getSortOrder() {
            return SortOrder.NATURAL;
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {

            return new ByDayGroupHeader(context, trackedActivityModel.getStarttimeDateTimeMinutes());
        }
    }
    public static final class ByWeek extends GroupHeaderType {

        public static final int GROUP_TYPE = ByWeekGroupHeader.GROUP_TYPE;
        private final Context context;

        public ByWeek(Context context, String title) {
            super(GROUP_TYPE, title);
            this.context = context;
        }

        public SortOrder getSortOrder() {
            return SortOrder.NATURAL;
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {

            return new ByWeekGroupHeader(context, trackedActivityModel.getStarttimeDateTimeMinutes());
        }
    }

    public static final class ByMonth extends GroupHeaderType {

        public static final int GROUP_TYPE = ByMonthGroupHeader.GROUP_TYPE;
        private final Context context;

        public ByMonth(Context context, String title) {
            super(GROUP_TYPE, title);
            this.context = context;
        }

        public SortOrder getSortOrder() {
            return SortOrder.NATURAL;
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {

            return new ByMonthGroupHeader(context, trackedActivityModel.getStarttimeDateTimeMinutes());
        }
    }
    public static final class ByYear extends GroupHeaderType {

        public static final int GROUP_TYPE = ByYearGroupHeader.GROUP_TYPE;
        private final Context context;

        public ByYear(Context context, String title) {
            super(GROUP_TYPE, title);
            this.context = context;
        }

        public SortOrder getSortOrder() {
            return SortOrder.NATURAL;
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {

            return new ByYearGroupHeader(context, trackedActivityModel.getStarttimeDateTimeMinutes());
        }
    }
    public static final class CustomFieldType extends GroupHeaderType {
        private final CustomFieldTypeModel _typeModel;

        public CustomFieldType(CustomFieldTypeModel typeModel) {
            super(CustomFieldTypeGroupHeader.getGroupTypeId(typeModel), typeModel.getName());
            _typeModel = typeModel;
        }

        public CustomFieldTypeModel getTypeModel() {
            return _typeModel;
        }

        @Override
        public GroupHeader apply(TrackedActivityModel trackedActivityModel) {
            for (ActivityCustomFieldModel m : trackedActivityModel.getCustomFieldData()) {
                if (m.getTypeModel().getId() == _typeModel.getId()) {
                    return new CustomFieldTypeGroupHeader(m.getTypeModel(), m.getValueModel());
                }
            }
            return new CustomFieldTypeGroupHeader(_typeModel, null);
        }
    }

}

