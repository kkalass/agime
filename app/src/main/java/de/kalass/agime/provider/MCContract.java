/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kalass.agime.provider;

import android.content.ContentResolver;
import android.net.Uri;

import de.kalass.android.common.provider.CRUDContentItem;
import de.kalass.android.common.provider.ContentUris2;
import edu.mit.mobile.android.content.DBTable;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.BooleanColumn;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.DBForeignKeyColumn;
import edu.mit.mobile.android.content.column.DatetimeColumn;
import edu.mit.mobile.android.content.column.IntegerColumn;
import edu.mit.mobile.android.content.column.TextColumn;

/**
 * Field and table name constants for
 * {@link de.kalass.agime.provider.MCProvider}.
 */
public final class MCContract {

    /**
     * Categories of Activities. This would typically be something like "Projectmanagement",
     * "Software development" etc.
     */
    @DBTable(Category.TABLE)
    public static class Category implements CRUDContentItem {
        public static final String TABLE = "category";

        /**
         * Category name.
         */
        @DBColumn(type = TextColumn.class, notnull = true)
        public static final String COLUMN_NAME_NAME = "name";

        /**
         * A color code to use for this type of columns
         */
        @DBColumn(type = IntegerColumn.class, notnull = false)
        public static final String COLUMN_NAME_COLOR_CODE = "color_code";


        // //////////////////////////////////////////////////////
        public static final String PATH = "category";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "trackpoint" resources.
         */
        public static final Uri CONTENT_URI = contentUri(PATH);

    }

    /**
     * Projects for organizing Activities. For example "leisure" and "work"
     */
    @DBTable(Project.TABLE)
    public static class Project implements CRUDContentItem {

        public static final String TABLE = "project";

        /**
         * Project name.
         */
        @DBColumn(type = TextColumn.class, notnull = true)
        public static final String COLUMN_NAME_NAME = "name";

        /**
         * A color code to use for this type of columns
         */
        @DBColumn(type = IntegerColumn.class, notnull = false)
        public static final String COLUMN_NAME_COLOR_CODE = "color_code";


        @DBColumn(type = DatetimeColumn.class, notnull = false)
        public static final String COLUMN_NAME_ACTIVE_UNTIL_MILLIS = "active_until";

        // //////////////////////////////////////////////////////
        public static final String PATH = "project";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "trackpoint" resources.
         */
        public static final Uri CONTENT_URI = contentUri(PATH);

    }

    /**
     * An activity type - not an actual occurrence of an activity.
     */
    @DBTable(ActivityType.TABLE)
    public static class ActivityType implements CRUDContentItem {
        public static final String TABLE = "activity_type";

        /**
         * Name of the activity.
         */
        @DBColumn(type = TextColumn.class, notnull = true)
        public static final String COLUMN_NAME_NAME = "name";

        /**
         * The type of activity that was tracked. Note that it is perfectly fine
         * to not assign a category to an activity type!
         */
        @DBForeignKeyColumn(parent = Category.class, notnull = false)
        public static final String COLUMN_NAME_ACTIVITY_CATEGORY_ID = "activity_category_id";


        // //////////////////////////////////////////////////////
        public static final String PATH = "activity_type";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "trackpoint" resources.
         */
        public static final Uri CONTENT_URI = contentUri(PATH);

    }

    /**
     * An activity - actual occurrence of an activity.
     */
    @DBTable(Activity.TABLE)
    public static class Activity implements CRUDContentItem {
        public static final String TABLE = "activity";

        /**
         * Description added by the user.
         */
        @DBColumn(type = TextColumn.class, notnull = true)
        public static final String COLUMN_NAME_DETAILS = "details";

        /**
         * The type of activity that was tracked. Note that it is perfectly fine
         * to not assign an activity type to a tracked activity!
         */
        @DBForeignKeyColumn(parent = ActivityType.class, notnull = false)
        public static final String COLUMN_NAME_ACTIVITY_TYPE_ID = "activity_type_id";


        /**
         * The type of activity that was tracked. Note that it is perfectly fine
         * to not assign a project to a tracked activity!
         */
        @DBForeignKeyColumn(parent = Project.class, notnull = false)
        public static final String COLUMN_NAME_PROJECT_ID = "project_id";

        /**
         * Starttime of the activity.
         *
         * When querying the activities, please bear in mind, that an activity may end a different
         * day then it started.
         *
         * If you query day x, you should determine the very exact day_starttime and day_endtime of that
         * day. Then you ask for all activities like this:
         *
         * where (starttime_at < day_endtime AND endtime_at >= day_starttime)
         */
        @DBColumn(type = DatetimeColumn.class, notnull = true)
        public static final String COLUMN_NAME_START_TIME = "starttime_at";

        /**
         * Endtime of the activity.
         */
        @DBColumn(type = DatetimeColumn.class, notnull = true)
        public static final String COLUMN_NAME_END_TIME = "endtime_at";

        /**
         * Migration: old tracked activities that were created before the tracking time was
         * tracked, will be null. Automatically inserted examples will be null as well.
         */
        @DBColumn(type = IntegerColumn.class, notnull = false)
        public static final String COLUMN_NAME_INSERT_DURATION_MILLIS = "insert_duration_millis";

        /**
         * Migration: old tracked activities that were created before the tracking time was
         * tracked, will be null. Automatically inserted examples will be null as well.
         */
        @DBColumn(type = IntegerColumn.class, notnull = false)
        public static final String COLUMN_NAME_UPDATE_DURATION_MILLIS = "update_duration_millis";

        /**
         * Migration: old tracked activities that were created before the tracking time was
         * tracked, will be null. Automatically inserted examples will be null as well.
         */
        @DBColumn(type = IntegerColumn.class, notnull = false)
        public static final String COLUMN_NAME_UPDATE_COUNT = "update_count";

        // //////////////////////////////////////////////////////
        public static final String PATH = "activity";
        public static final String PATH_SUGGESTION = "activity/suggestion";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "Activity" resources.
         */
        public static final Uri CONTENT_URI = contentUri(PATH);

        /**
         * Fully qualified URI for suggestion resource based on Activity
         */
        public static final Uri CONTENT_URI_SUGGESTION = contentUri(PATH_SUGGESTION);

        /**
         * Earliest start time is a hack: there are a couple of system entries
         * that will be created during DB creation to enable default suggestions. Those
         * default suggestions will all use a time that is earlier than this timestamp.
         */
        public static final long EARLIEST_START_TIME = 100;
    }

    /**
     * An recurring acquisition time, can be viewed as a pattern for automatically started
     * acquisition times.
     *
     */
    @DBTable(RecurringAcquisitionTime.TABLE)
    public static class RecurringAcquisitionTime implements CRUDContentItem {
        public static final String TABLE = "recurring_acquisition_time";

        /**
         * Starttime of the acquisition time of a day determined by COLUMN_NAME_WEEKDAY_PATTERN as a string, like "09:00"
         *
         */
        @DBColumn(type = TextColumn.class, notnull = true)
        public static final String COLUMN_NAME_START_TIME = "starttime";

        /**
         * Endtime of the acquisition time of a day determined by COLUMN_NAME_WEEKDAY_PATTERN as a string, like "18:00"
         */
        @DBColumn(type = TextColumn.class, notnull = true)
        public static final String COLUMN_NAME_END_TIME = "endtime";

        /**
         * Bitmask for the weekdays that shall be measured. Use {@link de.kalass.android.common.simpleloader.Weekdays}
         * to serialize to or from Sets of an enum type "Weekday".
         */
        @DBColumn(type = IntegerColumn.class)
        public static final String COLUMN_NAME_WEEKDAY_PATTERN = "weekdays";

        /**
         * recurring acquisition times can be active or inactive. If a user deactivates a pattern,
         * he is asked for the time when it should be reactivated automatically.
         */
        @DBColumn(type = DatetimeColumn.class)
        public static final String COLUMN_NAME_INACTIVE_UNTIL = "inactive_until";

        /**
         * special type of 'recurring' acquisition time: If this field is set, the weekdays column
         * is ignored, and the acquisition time is only valid on the date specified here
         */
        @DBColumn(type = DatetimeColumn.class)
        public static final String COLUMN_NAME_ACTIVE_ONCE_DATE = "active_once_date";


        // //////////////////////////////////////////////////////
        public static final String PATH = "recurring_acquisition_time";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "Activity" resources.
         */
        public static final Uri CONTENT_URI = contentUri(PATH);

    }


    /**
     * A type definition for a custom Field.
     *
     * Custom fields will be embedded in the activity tracking editor - if available.
     */
    @DBTable("custom_field_type")
    public static class CustomFieldType implements CRUDContentItem {

        @DBColumn(type = TextColumn.class, notnull = true)
        public static final String COLUMN_NAME_NAME = "name";

        /**
         * if set, then this custom field type will be available for all types of projects,
         * including "no project".
         */
        @DBColumn(type = BooleanColumn.class, defaultValue = "true")
        public static final String COLUMN_NAME_ANY_PROJECT = "any_project";




        // //////////////////////////////////////////////////////
        public static final String PATH = "custom_field_type";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "Activity" resources.
         */
        public static final Uri CONTENT_URI = contentUri(PATH);

    }

    /**
     * A mapping from Project to custom field types: associates custom field types with
     * projects - custom fields should only be used for those projects referenced here.
     */
    @DBTable(ProjectCustomFieldType.TABLE)
    public static class ProjectCustomFieldType implements CRUDContentItem {
        public static final String TABLE = "project_custom_field_type";

        @DBForeignKeyColumn(parent = Project.class, notnull = true)
        public static final String COLUMN_NAME_PROJECT_ID = "project_id";

        /**
         * The type of custom field to which this value belongs.
         */
        @DBForeignKeyColumn(parent = CustomFieldType.class, notnull = true)
        public static final String COLUMN_NAME_CUSTOM_FIELD_TYPE_ID = "custom_field_type_id";

        // //////////////////////////////////////////////////////
        public static final String PATH = "project_custom_field_type";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "ProjectCustomFieldType" resources.
         */
        public static final Uri CONTENT_URI = contentUri(PATH);

    }

    /**
     * A value for a custom Field.
     */
    @DBTable(CustomFieldValue.TABLE)
    @UriPath(CustomFieldValue.PATH)
    public static class CustomFieldValue implements CRUDContentItem {
        public static final String TABLE = "custom_field_value";

        @DBColumn(type = TextColumn.class, notnull = true)
        public static final String COLUMN_NAME_VALUE = "field_value";

        /**
         * The type of custom field to which this value belongs.
         */
        @DBForeignKeyColumn(parent = CustomFieldType.class, notnull = true)
        public static final String COLUMN_NAME_CUSTOM_FIELD_TYPE_ID = "custom_field_type_id";



        // //////////////////////////////////////////////////////
        public static final String PATH = "custom_field_value";
        public static final String PATH_SUGGESTION = "custom_field_value/suggestion";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "Activity" resources.
         *
         * First append the id of the parent CustomFieldType, then append the ID of the custom field value
         */
        public static final Uri CONTENT_URI = contentUri(PATH);
        public static final Uri CONTENT_URI_SUGGESTION = contentUri(PATH_SUGGESTION);
        /**
         * Get the directory URI for values of the given Type
         * @param typeId
         * @return
         */
        public static final Uri getDirUriForType(long typeId) {
            return ContentUris2.getDirUriFromParent(MCContract.CustomFieldType.CONTENT_URI, typeId, PATH);
        }

        public static final Uri getTypeUriFromDirURI(Uri uri) {
            return ContentUris2.getParentUriFromDirUri(uri, PATH);
        }

    }

    /**
     * A mapping from Activity to custom field value: associates custom field values with
     * tracked activities.
     */
    @DBTable(ActivityCustomFieldValue.TABLE)
    public static class ActivityCustomFieldValue implements CRUDContentItem {
        public static final String TABLE = "activity_custom_field_value";

        /**
         * The type of activity that was tracked. Note that it is perfectly fine
         * to not assign a project to a tracked activity!
         */
        @DBForeignKeyColumn(parent = Activity.class, notnull = true)
        public static final String COLUMN_NAME_TRACKED_ACTIVITY_ID = "activity_id";

        /**
         * The type of custom field to which this value belongs.
         */
        @DBForeignKeyColumn(parent = CustomFieldValue.class, notnull = true)
        public static final String COLUMN_NAME_CUSTOM_FIELD_VALUE_ID = "custom_field_value_id";


        // //////////////////////////////////////////////////////
        public static final String PATH = "activity_custom_field_value";

        /**
         * MIME type for lists of entries.
         */
        public static final String CONTENT_TYPE_DIR = contentTypeDirectory(PATH);
        /**
         * MIME type for individual entries.
         */
        public static final String CONTENT_TYPE_ITEM = contentTypeItem(PATH);

        /**
         * Fully qualified URI for "Activity" resources.
         */
        public static final Uri CONTENT_URI = contentUri(PATH);

    }

    private MCContract() {
    }

    /**
     * Content provider authority.
     */
    public static final String CONTENT_AUTHORITY = "de.kalass.agime";

    private static String contentTypeDirectory(String entityName) {
        return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.de.kalass.agime." + entityName;
    }

    private static String contentTypeItem(String entityName) {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.de.kalass.agime." + entityName;
    }

    private static Uri contentUri(String path) {
        return ProviderUtils.toContentUri(CONTENT_AUTHORITY, path);
    }

}

