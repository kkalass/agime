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

import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.regex.Pattern;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.android.common.provider.AbstractContentProvider;
import de.kalass.android.common.provider.CRUDContentItem;
import de.kalass.android.common.simpleloader.Weekdays;
import de.kalass.android.common.simpleloader.Weekdays.Weekday;
import edu.mit.mobile.android.content.ForeignKeyDBHelper;
import edu.mit.mobile.android.content.GenericDBHelper;

import static de.kalass.android.common.simpleloader.CursorUtil.putHourMinute;
import static de.kalass.android.common.simpleloader.CursorUtil.putLocalDate;
import static de.kalass.android.common.simpleloader.CursorUtil.putWeekdays;

/**
 * Provides application data for tracking health related occurrences and issues.
 *
 * Created by klas on 13.10.13.
 */
public class MCProvider extends AbstractContentProvider {

    private static final int DB_VERSION_ACTIVE_ONCE_ACQUISITION_TIME = 7;
    private static final int DB_VERSION_FIRST_USE_SUGGESTIONS = 7;

    private static final int DB_VERSION = 9;
    public static final String[] FIND_BY_NAME_PROJECTION = new String[]{CRUDContentItem.COLUMN_NAME_ID};
    public static final int FIND_BY_NAME_IDX_ID = 0;

    public MCProvider() {
        super (MCContract.CONTENT_AUTHORITY, DB_VERSION);
        registerContentItem(MCContract.Project.class, MCContract.Project.PATH);
        registerContentItem(MCContract.Category.class, MCContract.Category.PATH);
        registerContentItem(MCContract.ActivityType.class, MCContract.ActivityType.PATH);
        registerContentItem(MCContract.Activity.class, MCContract.Activity.PATH);

        registerContentItem(MCContract.RecurringAcquisitionTime.class, MCContract.RecurringAcquisitionTime.PATH);

        registerContentItem(MCContract.CustomFieldType.class, MCContract.CustomFieldType.PATH);
        registerContentItem(MCContract.CustomFieldValue.class, MCContract.CustomFieldValue.PATH);
        addChildDirAndItemUri(new ForeignKeyDBHelper(
                MCContract.CustomFieldType.class,
                MCContract.CustomFieldValue.class,
                MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID
        ), MCContract.CustomFieldType.PATH,  MCContract.CustomFieldValue.PATH);
        registerContentItem(MCContract.ActivityCustomFieldValue.class, MCContract.ActivityCustomFieldValue.PATH);
        registerContentItem(MCContract.ProjectCustomFieldType.class, MCContract.ProjectCustomFieldType.PATH);

        addDirUri(new GroupByHelper(MCContract.Activity.class), MCContract.Activity.PATH_SUGGESTION);


        addDirUri(new GenericDBHelper(MCContract.CustomFieldValue.class) {

            @Override
            public Cursor queryDir(SQLiteDatabase db, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
                String rawQuery = " SELECT " + Joiner.on(", ").join(projection) + " FROM " + getTable() +
                        " LEFT OUTER JOIN " + MCContract.ActivityCustomFieldValue.TABLE +
                        " ON " + MCContract.ActivityCustomFieldValue.TABLE + "." + MCContract.ActivityCustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_VALUE_ID + " = " + getTable() + "." + MCContract.CustomFieldValue._ID +
                        " WHERE " + selection + " GROUP BY " + Joiner.on(", ").join(projection) +
                        (sortOrder == null ? "" : " ORDER BY " + sortOrder);
                //Log.i("DB", "Activity Suggestion QUERY: " + rawQuery);
                return db.rawQuery(rawQuery, selectionArgs);
            }

        }, MCContract.CustomFieldValue.PATH_SUGGESTION);


    }

//FIXME
    //@Override
    protected void afterUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (inBetween(oldVersion, newVersion, DB_VERSION_ACTIVE_ONCE_ACQUISITION_TIME)) {
            onUpgradeCreateActiveOnceAcquisitionTime(db);
        }
        if (inBetween(oldVersion, newVersion, DB_VERSION_FIRST_USE_SUGGESTIONS)) {
            onUpgradeCreateFirstUseSuggestions(db);
        }
    }

    private void onUpgradeCreateFirstUseSuggestions(SQLiteDatabase db) {

        final long projectIdLeisure = insertProject(db, R.string.first_use_defaults_project_leisure, suggestProjectColorCode(1));
        final long projectIdWork = insertProject(db, R.string.first_use_defaults_project_work, suggestProjectColorCode(2));

        final long categoryIdCommunication = insertCategory(db, R.string.first_use_defaults_category_communication, R.color.category1_default);
        final long categoryIdBreak = insertCategory(db, R.string.first_use_defaults_category_break, R.color.category2_default);
        final long categoryIdAkquisition = insertCategory(db, R.string.first_use_defaults_category_acquisition, R.color.category3_default);
        final long categoryIdSelfOrganization = insertCategory(db, R.string.first_use_defaults_category_selforganization, R.color.category4_default);

        final long activityTypeIdTryAgime = insertActivityType(db, R.string.first_use_defaults_activity_type_try_agime, categoryIdSelfOrganization);
        final long activityTypeIdCreateAngebot = insertActivityType(db, R.string.first_use_defaults_activity_type_create_angebot, categoryIdAkquisition);
        final long activityTypeIdMeeting = insertActivityType(db, R.string.first_use_defaults_activity_type_meeting, categoryIdCommunication);
        final long activityTypeIdEMails = insertActivityType(db, R.string.first_use_defaults_activity_type_emails, categoryIdCommunication);
        final long activityTypeIdPhone = insertActivityType(db, R.string.first_use_defaults_activity_type_phone, categoryIdCommunication);
        final long activityTypeIdCoffee = insertActivityType(db, R.string.first_use_defaults_activity_type_coffee, categoryIdBreak);
        final long activityTypeIdLunch = insertActivityType(db, R.string.first_use_defaults_activity_type_lunch, categoryIdBreak);

        insertActivitySuggestion(db, 0, activityTypeIdCreateAngebot, projectIdWork);
        insertActivitySuggestion(db, 1, activityTypeIdEMails, projectIdWork);
        insertActivitySuggestion(db, 2, activityTypeIdMeeting, projectIdWork);
        insertActivitySuggestion(db, 3, activityTypeIdPhone, projectIdWork);
        insertActivitySuggestion(db, 4, activityTypeIdLunch, projectIdLeisure);
        insertActivitySuggestion(db, 5, activityTypeIdCoffee, projectIdLeisure);
        insertActivitySuggestion(db, 6, activityTypeIdTryAgime, projectIdWork);
    }

    private int suggestProjectColorCode(int id) {
        return ColorSuggestion.suggestProjectColor(getContext().getResources(), id, null);
    }

    private Long findIdByName(SQLiteDatabase db, String table, String nameColumnName, String value) {
        final Cursor cursor = db.query(table, FIND_BY_NAME_PROJECTION, nameColumnName + " = ?", new String[]{value}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getLong(FIND_BY_NAME_IDX_ID);
        } finally {
            cursor.close();
        }
    }
    private long insertProject(SQLiteDatabase db, int nameResource, int colorCode) {
        ContentValues values = newContentValues();
        final Resources resources = getContext().getResources();

        final String name = resources.getString(nameResource).trim();
        Long oldId = findIdByName(db, MCContract.Project.TABLE, MCContract.Project.COLUMN_NAME_NAME, name);
        if (oldId != null) {
            return oldId;
        }
        values.put(MCContract.Project.COLUMN_NAME_NAME, name);
        values.put(MCContract.Project.COLUMN_NAME_COLOR_CODE, colorCode);
        //values.put(MCContract.Project.COLUMN_NAME_ACTIVE_UNTIL_MILLIS, (Long)null);
        return db.insertOrThrow(MCContract.Project.TABLE, null, values);
    }

    private long insertActivityType(SQLiteDatabase db, int nameResource, long categoryId) {
        ContentValues values = newContentValues();
        final Resources resources = getContext().getResources();

        final String name = resources.getString(nameResource).trim();
        Long oldId = findIdByName(db, MCContract.ActivityType.TABLE, MCContract.ActivityType.COLUMN_NAME_NAME, name);
        if (oldId != null) {
            return oldId;
        }

        values.put(MCContract.ActivityType.COLUMN_NAME_NAME, name);
        values.put(MCContract.ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID, categoryId);
        return db.insertOrThrow(MCContract.ActivityType.TABLE, null, values);
    }

    private long insertCategory(SQLiteDatabase db, int nameResource, int colorResource) {
        ContentValues values = newContentValues();
        final Resources resources = getContext().getResources();

        final String name = resources.getString(nameResource).trim();
        Long oldId = findIdByName(db, MCContract.Category.TABLE, MCContract.Category.COLUMN_NAME_NAME, name);
        if (oldId != null) {
            return oldId;
        }

        values.put(MCContract.Category.COLUMN_NAME_NAME, name);
        values.put(MCContract.Category.COLUMN_NAME_COLOR_CODE, resources.getColor(colorResource));
        return db.insertOrThrow(MCContract.Category.TABLE, null, values);
    }

    private long insertActivitySuggestion(SQLiteDatabase db, int counter, long activityTypeId, long projectId) {
        ContentValues values = newContentValues();

        values.put(MCContract.Activity.COLUMN_NAME_START_TIME, 1 + 2*counter);
        values.put(MCContract.Activity.COLUMN_NAME_END_TIME, 2 + 2*counter);
        values.put(MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID, activityTypeId);
        values.put(MCContract.Activity.COLUMN_NAME_PROJECT_ID, projectId);
        return db.insertOrThrow(MCContract.Activity.TABLE, null, values);
    }

    private ContentValues newContentValues() {
        ContentValues r = new ContentValues();
        long now = System.currentTimeMillis();
        r.put(CRUDContentItem.COLUMN_NAME_CREATED_AT, now);
        r.put(CRUDContentItem.COLUMN_NAME_MODIFIED_AT, now);
        return r;
    }
    private void onUpgradeCreateActiveOnceAcquisitionTime(SQLiteDatabase db) {
        if (countAcquisitionTimes(db) == 0) {
            // new user. Remember: until DB version 7 there was an insert statement for DB version 4
            // that created the default entry. But now we create this entry in code. Users that upgraded
            // to DB version 4 before, will not get the new entries - but that is fine, since the new entry
            // is about first use.
            LocalDate today = new LocalDate();
            LocalTime now = new LocalTime();
            boolean needsSpecialEntryForToday = now.getHourOfDay() != 9;

            final LocalTime standardStartTime = new LocalTime(9, 0);
            final LocalTime standardEndTime = new LocalTime(18, 0);
            final EnumSet<Weekday> standardWeekdays = EnumSet.of(Weekday.MO, Weekday.TUE, Weekday.WED, Weekday.THU, Weekday.FR);

            ContentValues r = newContentValues();
            putLocalDate(r, MCContract.RecurringAcquisitionTime.COLUMN_NAME_ACTIVE_ONCE_DATE, null);
            putLocalDate(r, MCContract.RecurringAcquisitionTime.COLUMN_NAME_INACTIVE_UNTIL, needsSpecialEntryForToday ? today.plusDays(1) : null);

            putHourMinute(r, MCContract.RecurringAcquisitionTime.COLUMN_NAME_START_TIME, standardStartTime);
            putHourMinute(r, MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME, standardEndTime);
            putWeekdays(r, MCContract.RecurringAcquisitionTime.COLUMN_NAME_WEEKDAY_PATTERN, standardWeekdays);

            db.insertOrThrow(MCContract.RecurringAcquisitionTime.TABLE, null, r);

            if (needsSpecialEntryForToday) {
                ContentValues r2 = newContentValues();

                // round the start time to some reasonable value at least 6 minutes in the past and use it as the new suggestion
                LocalTime newNow = now.minusMinutes(6);
                LocalTime startTime = new LocalTime(newNow.getHourOfDay(), 5*(newNow.getMinuteOfHour()/ 5));
                // end time is a little bit more involved: on a normal Weekday use the later one of
                // standard end time and suggested end Time
                LocalTime suggestedEndTime =
                        new LocalTime(startTime.getHourOfDay(), 30*(startTime.getMinuteOfHour() / 30)).plusMinutes(60);

                boolean isStandardWeekday = standardWeekdays.contains(Weekdays.getWeekday(today));
                LocalTime endTime = isStandardWeekday && standardEndTime.isAfter(suggestedEndTime) ? standardEndTime : suggestedEndTime;

                putLocalDate(r2, MCContract.RecurringAcquisitionTime.COLUMN_NAME_ACTIVE_ONCE_DATE, today);
                putLocalDate(r2, MCContract.RecurringAcquisitionTime.COLUMN_NAME_INACTIVE_UNTIL, null);
                putHourMinute(r2, MCContract.RecurringAcquisitionTime.COLUMN_NAME_START_TIME, startTime);
                putHourMinute(r2, MCContract.RecurringAcquisitionTime.COLUMN_NAME_END_TIME, endTime);
                putWeekdays(r2, MCContract.RecurringAcquisitionTime.COLUMN_NAME_WEEKDAY_PATTERN, EnumSet.noneOf(Weekday.class));

                db.insertOrThrow(MCContract.RecurringAcquisitionTime.TABLE, null, r2);

            }
        }
    }

    private int countAcquisitionTimes(SQLiteDatabase db) {
        final Cursor cursor = db.query(MCContract.RecurringAcquisitionTime.TABLE, new String[]{"count(*)"}, null, null, null, null, null);
        if (cursor == null) {
            return 0;
        }
        try {
            if (!cursor.moveToFirst()) {
                return 0;
            }
            final int count = cursor.getInt(0);
            return count;
        } finally {
            cursor.close();
        }
    }

    private static class GroupByHelper extends GenericDBHelper {
        private static final Pattern FUNCTION_PATTERN = Pattern.compile(".*\\(.*\\).*");
        private static final Predicate<String> IS_COLUMN_NAME = new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input != null && ! FUNCTION_PATTERN.matcher(input).matches();
            }
        };

        public GroupByHelper(java.lang.Class<? extends edu.mit.mobile.android.content.ContentItem> contentItem) {
            super(contentItem);
        }

        @Override
        public Cursor queryDir(SQLiteDatabase db, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            Iterable<String> groupByItems = Iterables.filter(Arrays.asList(projection), IS_COLUMN_NAME);
            String rawQuery = " SELECT " + Joiner.on(", ").join(projection) + " FROM " + getTable() +
                    " WHERE " + selection + " GROUP BY " + Joiner.on(", ").join(groupByItems) +
                    (sortOrder == null ? "" : " ORDER BY " + sortOrder);
            //Log.i("DB", "GroubByHelper QUERY: " + rawQuery);
            return db.rawQuery(rawQuery, selectionArgs);
        }


    }
}
