package de.kalass.agime.trackactivity;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

import de.kalass.agime.acquisitiontime.AcquisitionTimeInstance;
import de.kalass.agime.acquisitiontime.AcquisitionTimeOps;
import de.kalass.agime.acquisitiontime.AcquisitionTimes;
import de.kalass.agime.acquisitiontime.RecurringDAO;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.insertorupdate.InsertOrUpdate;
import de.kalass.android.common.insertorupdate.Operations;
import de.kalass.android.common.simpleloader.ContentProviderOperationUtil;
import de.kalass.android.common.simpleloader.ValueOrReference;
import de.kalass.android.common.util.LongUtil;

import static de.kalass.android.common.simpleloader.CursorUtil.getIndex;
/**
 * Performs an insert or update of a tracked activity, automatically creating project, category
 * and activity type entries (as well as custom field type entries) if needed.
 * The new entry will always have precendence over existing entries and will adjust them if applicable.
 *
 * Created by klas on 21.01.14.
 */
public class InsertOrUpdateTrackedActivity extends InsertOrUpdate<InsertOrUpdateInput, InsertOrUpdateTrackedActivityResult> {

    public static final String LOG_TAG = "LOG";
    private final long _fragmentStartTimeMillis;

    InsertOrUpdateTrackedActivity(Context context, long fragmentStartTimeMillis) {
        super(context, MCContract.CONTENT_AUTHORITY);
        _fragmentStartTimeMillis = fragmentStartTimeMillis;
    }

    @Override
    protected Long getId(InsertOrUpdateInput input) {
        return input.entityId;
    }

    @Override
    protected Operations<InsertOrUpdateTrackedActivityResult> createOperations(
            boolean isInsert, Long id, InsertOrUpdateInput input, long now
    ) {
        Context context = getContext();
        // TODO: currently we do not check wether or not the category is set for the activity type. should we?
        // Preconditions: Category, Project, Activity Type
        final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        final ValueOrReference categoryId = new GetOrInsertCategoryByName(context, input.categoryColor)
                .addInsertOperation(ops, 0, now, input.categoryTypeId, input.categoryName);
        final ValueOrReference projectId = new GetOrInsertProjectByName(context, input.projectColor)
                .addInsertOperation(ops, 0, now, input.projectId, input.projectName);
        final ValueOrReference activityTypeId = new GetOrInsertActivityTypeByName(context, categoryId)
                .addInsertOperation(ops, 0, now, input.activityTypeId, input.activityTypeName);

        // Main Entry for Tracked Activity
        final ContentProviderOperation mainOperation = createInsertOrUpdateTrackedActivity(
                id, activityTypeId, projectId, now,
                input.details, input.startTimeMillis, input.endTimeMillis,
                input.originalInsertDurationMillis, input.originalUpdateDurationMillis, input.originalUpdateCount
        );

        final ValueOrReference mainOperationReference = ContentProviderOperationUtil.add(ops, mainOperation);
        final ValueOrReference trackedActivityId = isInsert ? mainOperationReference : ValueOrReference.ofValueNonnull(id);

        // Further details about the tracked activity
        for (InsertOrUpdateOperationFactory factory : input.additional) {
            final int numPreviousOperations = ops.size();
            ops.addAll(factory.createOperationsInBackground(context, numPreviousOperations, trackedActivityId));
        }

        boolean addedAcquisitionTimeEntry = addAcquisitionTimeEntry(input, now, ops);

        List<EntryToAdjust> entriesToAdjust = findEntriesToAdjust(trackedActivityId, input.startTimeMillis, input.endTimeMillis);
        // adjust existing entries to "make room" for the new entry
        final List<SplitResultWithOperations> splitResultWithOperations = shortenConflicting(
                entriesToAdjust, ops,
                new EntryToAdjust(trackedActivityId,  input.startTimeMillis, input.endTimeMillis, projectId, activityTypeId, input.details),
                1
        );

        final List<SplitResult> splitResult = new ArrayList<SplitResult>(splitResultWithOperations.size());
        for (SplitResultWithOperations ro: splitResultWithOperations) {

            splitResult.add(ro.result);
        }

        return Operations.getInstance(ops, mainOperationReference, new InsertOrUpdateTrackedActivityResult(splitResult, input.startTimeMillis, addedAcquisitionTimeEntry));
    }

    /**
     * If the activity is tracked for "today" and there is no acquisition time instance covering the
     * start time or end time of the activity, insert one
     * @param input
     * @param ops
     */
    private boolean addAcquisitionTimeEntry(InsertOrUpdateInput input, long now, ArrayList<ContentProviderOperation> ops) {
        final List<RecurringDAO.Data> acquisitionTimeCandidates = input.acquisitionTimeCandidates;
        final DateTime startTime = new DateTime(input.startTimeMillis);
        final DateTime endTime = new DateTime(input.endTimeMillis);
        return adjustAcquisitionTime(acquisitionTimeCandidates, startTime, endTime, now, ops);
    }

    public static boolean adjustAcquisitionTime(
            List<RecurringDAO.Data> acquisitionTimeCandidates,
            DateTime trackedActivityStartTime,
            DateTime trackedActivityEndTime,
            long now,
            ArrayList<ContentProviderOperation> ops
    ) {
        final AcquisitionTimes startAcquisitionTimes = AcquisitionTimes.fromRecurring(
                acquisitionTimeCandidates, trackedActivityStartTime
        );

        final AcquisitionTimes endAcquisitionTimes = AcquisitionTimes.fromRecurring(
                acquisitionTimeCandidates, trackedActivityEndTime
        );
        if (startAcquisitionTimes.hasCurrent() && endAcquisitionTimes.hasCurrent()) {
            // usual case - all is fine.
            return false;
        }

        LocalDate today = new LocalDate();
        final LocalDate startTimeLocalDate = trackedActivityStartTime.toLocalDate();
        final LocalDate endTimeLocalDate = trackedActivityEndTime.toLocalDate();
        if (!startTimeLocalDate.equals(today) || !endTimeLocalDate.equals(today)) {
            // inserting entries for previous days
            return false;
        }

        if (!startAcquisitionTimes.hasCurrent()) {
             ops.add(AcquisitionTimeOps.insert(today, now, trackedActivityStartTime.toLocalTime(), startAcquisitionTimes.getNext()));
            return true;
        } else if (!endAcquisitionTimes.hasCurrent()) {
            // starttime is within acquisition time, end time not - so we need to either update
            // the acquisition time to end at end time, or insert a new one from the curren t template
            // time that starts when the starttime acquisition time ended.
            final AcquisitionTimeInstance startTimeCurrent = startAcquisitionTimes.getCurrent();
            Preconditions.checkArgument(today.equals(startTimeCurrent.day));
            final LocalTime endTimeLocalTime = trackedActivityEndTime.toLocalTime();
            final List<RecurringDAO.Data> activeOnceItems = startTimeCurrent.getActiveOnceItems();
            if (activeOnceItems.isEmpty()) {
                // create a new one that starts at the original start time and ends with the tracked
                // activity
                ops.add(AcquisitionTimeOps.insert(today, now, startTimeCurrent.startTime, endTimeLocalTime));
            } else {
                // adjust existing ones to end at the new tracked activity end time
                for (RecurringDAO.Data existing: activeOnceItems) {

                    final Uri itemUri = ContentUris.withAppendedId(MCContract.RecurringAcquisitionTime.CONTENT_URI, existing.getId());
                    ops.add(AcquisitionTimeOps.assertQuery(today, itemUri));
                    ops.add(AcquisitionTimeOps.updateEndTime(itemUri, endTimeLocalTime, now));
                }
            }
            return true;
        } else {
            // starttime is within acquisition time, end time is also covered by an acquisition time - should not have exited earlier, but does no harm
            Log.w(LOG_TAG, "both start acquisition time and end acquisition time have a 'current' entry - code should not have gotten so far");
            return false;
        }
    }




    protected ContentProviderOperation createInsertOrUpdateTrackedActivity(
        Long trackedActivityId,
        ValueOrReference activityTypeId,
        ValueOrReference projectId,
        long now,
        String details,
        long startTimeMillis,
        long endTimeMillis,
        Long originalInsertDurationMillis,
        Long originalUpdateDurationMillis,
        Long originalUpdateCount
    ) {
        ContentProviderOperation.Builder builder = createInsertOrUpdateBuilder(MCContract.Activity.CONTENT_URI, trackedActivityId, now);
        activityTypeId.appendSelf(builder, MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID);
        projectId.appendSelf(builder, MCContract.Activity.COLUMN_NAME_PROJECT_ID);

        builder.withValue(MCContract.Activity.COLUMN_NAME_START_TIME, startTimeMillis);
        builder.withValue(MCContract.Activity.COLUMN_NAME_END_TIME, endTimeMillis);
        builder.withValue(MCContract.Activity.COLUMN_NAME_DETAILS, details);

        if (trackedActivityId == null) {
            long insertDuration = now - _fragmentStartTimeMillis;
            builder.withValue(MCContract.Activity.COLUMN_NAME_INSERT_DURATION_MILLIS, insertDuration);
            builder.withValue(MCContract.Activity.COLUMN_NAME_UPDATE_COUNT, 0);
            builder.withValue(MCContract.Activity.COLUMN_NAME_UPDATE_DURATION_MILLIS, 0);
        } else {
            // do not change insert duration
            long newUpdateDuration = now - _fragmentStartTimeMillis;
            long updateDurationMillis = newUpdateDuration + LongUtil.nullToZero(originalUpdateDurationMillis);
            long updateCount = 1 + LongUtil.nullToZero(originalUpdateCount);

            builder.withValue(MCContract.Activity.COLUMN_NAME_UPDATE_COUNT, updateCount);
            builder.withValue(MCContract.Activity.COLUMN_NAME_UPDATE_DURATION_MILLIS, updateDurationMillis);

        }
        return builder.build();
    }


    static final class SplitQuery {
        public static final Uri CONTENT_URI = MCContract.Activity.CONTENT_URI;
        public static final String COLUMN_NAME_ID = MCContract.Activity._ID;
        public static final String COLUMN_NAME_END_TIME = MCContract.Activity.COLUMN_NAME_END_TIME;
        public static final String COLUMN_NAME_START_TIME = MCContract.Activity.COLUMN_NAME_START_TIME;
        public static final String COLUMN_NAME_ACTIVITY_TYPE_ID = MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID;
        public static final String COLUMN_NAME_PROJECT_ID = MCContract.Activity.COLUMN_NAME_PROJECT_ID;
        public static final String COLUMN_NAME_DETAILS = MCContract.Activity.COLUMN_NAME_DETAILS;

        public static final String[] PROJECTION = new String[] {
                COLUMN_NAME_ID,
                COLUMN_NAME_START_TIME,
                COLUMN_NAME_END_TIME,
                COLUMN_NAME_ACTIVITY_TYPE_ID,
                COLUMN_NAME_PROJECT_ID,
                COLUMN_NAME_DETAILS
        };
        public static final int IDX_ID = getIndex(PROJECTION, COLUMN_NAME_ID);
        public static final int IDX_START_TIME = getIndex(PROJECTION, COLUMN_NAME_START_TIME);
        public static final int IDX_END_TIME = getIndex(PROJECTION, COLUMN_NAME_END_TIME);
        public static final int IDX_ACTIVITY_TYPE_ID = getIndex(PROJECTION, COLUMN_NAME_ACTIVITY_TYPE_ID);
        public static final int IDX_PROJECT_ID = getIndex(PROJECTION, COLUMN_NAME_PROJECT_ID);
        public static final int IDX_DETAILS = getIndex(PROJECTION, COLUMN_NAME_DETAILS);
    }

    private static final class SplitResultWithOperations {
        final SplitResult result;
        final List<ContentProviderOperation> operations;

        private SplitResultWithOperations(SplitResult result, List<ContentProviderOperation> operations) {
            this.result = result;
            this.operations = operations;
        }

        public static SplitResultWithOperations forResult(SplitResult result) {
            return new SplitResultWithOperations(result, ImmutableList.<ContentProviderOperation>of());
        }

        public static SplitResultWithOperations forResult(SplitResult result, ContentProviderOperation op) {
            return new SplitResultWithOperations(result, ImmutableList.of(op));
        }

        public static SplitResultWithOperations forResult(SplitResult result, ContentProviderOperation op1, ContentProviderOperation op2) {
            return new SplitResultWithOperations(result, ImmutableList.of(op1, op2));
        }
    }

    private List<EntryToAdjust> findEntriesToAdjust(
            ValueOrReference entityId, long startTimeNew, long endTimeNew) {
        Long entityIdValue = entityId.isReference() ? null : entityId.getValue();
        String selection = SplitQuery.COLUMN_NAME_END_TIME + " >= ? AND " + SplitQuery.COLUMN_NAME_START_TIME + " <= ? ";
        String[] selectionArgs = new String[entityIdValue != null ? 3 : 2];
        selectionArgs[0] = Long.toString(startTimeNew);
        selectionArgs[1] = Long.toString(endTimeNew);
        if (entityIdValue != null) {
            selection += " AND " + SplitQuery.COLUMN_NAME_ID + " != ? ";
            selectionArgs[2] = Long.toString(entityIdValue);
        }
        return ContentResolverUtil.loadFromContentResolver(getContext(),
                EntryToAdjust.READER,
                SplitQuery.CONTENT_URI,
                SplitQuery.PROJECTION,
                selection,
                selectionArgs,
                SplitQuery.COLUMN_NAME_ACTIVITY_TYPE_ID + " asc " // ordering
        );
    }

    private List<SplitResultWithOperations> shortenConflicting(
            List<EntryToAdjust> entriesToAdjust,
            ArrayList<ContentProviderOperation> ops,
            EntryToAdjust newEntry, int depth) {

        Log.i(LOG_TAG, "conflictingEntities " + entriesToAdjust.size());

        if (entriesToAdjust.isEmpty()) {
            return ImmutableList.of();
        }

        List<SplitResultWithOperations> splitResults = new ArrayList<SplitResultWithOperations>();
        List<SplitResultWithOperations> fullyWithin = new ArrayList<SplitResultWithOperations>();
        for (EntryToAdjust entryToAdjust : entriesToAdjust) {
            SplitResultWithOperations splitResult = splitOrShortenEntry(
                    ops,
                    newEntry.startTime, newEntry.endTime, entryToAdjust, depth
            );
            //Log.i(LOG_TAG, "SplitResultType for " + idC + " is "  + splitResult.result.type);
            if (splitResult.result.type == SplitType.FAILED_LIES_FULLY_WITHIN) {
                fullyWithin.add(splitResult);
            } else if (splitResult.result.type != SplitType.NOTHING) {
                splitResults.add(splitResult);
            }
        }

        //
        // we need to turn logic around, using the fully within entities as starting point to
        // split the new entry
        if (depth < 2) {
            // initial seed: split the new entry
            ArrayList<EntryToAdjust> newEntriesToAdjust = new ArrayList<EntryToAdjust>();
            newEntriesToAdjust.add(newEntry);

            for (SplitResultWithOperations resultWithOperations : fullyWithin) {
                Preconditions.checkArgument(resultWithOperations.result.type == SplitType.FAILED_LIES_FULLY_WITHIN);
                for (EntryToAdjust r: resultWithOperations.result.entries) {

                    List<SplitResultWithOperations> innerResults = shortenConflicting(
                            newEntriesToAdjust, ops, r, depth + 1
                    );
                    splitResults.addAll(innerResults);

                    // use the newly split items as input for the next splitting
                    newEntriesToAdjust = new ArrayList<EntryToAdjust>();
                    for (SplitResultWithOperations innerResult : innerResults) {
                        for (EntryToAdjust e : innerResult.result.entries) {
                            newEntriesToAdjust.add(e);
                        }
                    }

                }
            }
        } else {
            splitResults.addAll(fullyWithin);
        }

        return splitResults;
    }

    /**
     * the entry represented by the cursor will be either split or shortened, such that
     * it does not ly within the bounds of startTimeNew and/or endTimeNew
     */
    private SplitResultWithOperations splitOrShortenEntry(ArrayList<ContentProviderOperation> ops,
                                                          long startTimeNew, long endTimeNew,
                                                          EntryToAdjust entryToAdjust, int depth) {
        final long startTimeC = entryToAdjust.startTime;
        final long endTimeC = entryToAdjust.endTime;

        long now = System.currentTimeMillis();
        boolean endsAfterNewStarts = endTimeC > startTimeNew;
        boolean endsAfterNewEnds = endTimeC > endTimeNew;
        boolean startsBeforeNewStarts = startTimeC < startTimeNew;
        boolean startsAfterNewStarts = startTimeC >= startTimeNew;
        boolean startsBeforeNewEnds = startTimeC < endTimeNew;

        if (startsBeforeNewStarts && endsAfterNewStarts) {
            // started before start of new, but ends after start of new activity: move end time
            // to start time of new activity
            final EntryToAdjust updatedEntry = entryToAdjust.withEndTime(startTimeNew);
            final ContentProviderOperation update = ContentProviderOperationUtil
                    .newItemUpdate(MCContract.Activity.CONTENT_URI, updatedEntry.id)
                    .withValue(MCContract.Activity.COLUMN_NAME_END_TIME, updatedEntry.endTime)
                    .withValue(MCContract.Activity.COLUMN_NAME_MODIFIED_AT, now)
                    .withExpectedCount(1)
                    .build();
            ops.add(update);
            if (endsAfterNewEnds) {
                // the copy starts when the new entry ends
                EntryToAdjust insertedEntry = entryToAdjust
                        .withId(null /*new item, not yet inserted into operation list*/)
                        .withStartTime(endTimeNew);

                // FIXME copy custom field value references!
                // we need to insert a new copy!
                final ContentProviderOperation.Builder insertBuilder = ContentProviderOperation
                        .newInsert(MCContract.Activity.CONTENT_URI)
                        .withValue(MCContract.Activity.COLUMN_NAME_START_TIME, insertedEntry.startTime)
                        .withValue(MCContract.Activity.COLUMN_NAME_END_TIME, insertedEntry.endTime)
                        .withValue(MCContract.Activity.COLUMN_NAME_MODIFIED_AT, now)
                        .withValue(MCContract.Activity.COLUMN_NAME_CREATED_AT, now)
                        .withValue(MCContract.Activity.COLUMN_NAME_DETAILS, insertedEntry.activityDetails);

                insertedEntry.activityTypeId.appendSelf(insertBuilder, MCContract.Activity.COLUMN_NAME_ACTIVITY_TYPE_ID);

                insertedEntry.projectId.appendSelf(insertBuilder, MCContract.Activity.COLUMN_NAME_PROJECT_ID);

                final ContentProviderOperation insert = insertBuilder.build();

                final ValueOrReference insertRef = ContentProviderOperationUtil.add(ops, insert);
                // set the reference to the freshly inserted item
                insertedEntry = insertedEntry.withId(insertRef);

                final SplitResult r = SplitResult.ofType(SplitType.SPLIT, updatedEntry, insertedEntry, depth);
                return SplitResultWithOperations.forResult(r, update, insert);
            }

            final SplitResult r = SplitResult.ofType(SplitType.SHORTENED_PREVIOUS, updatedEntry, depth);
            return SplitResultWithOperations.forResult(r, update);
        }

        if (startsAfterNewStarts && startsBeforeNewEnds && endsAfterNewEnds) {
            // move start time of old entry to the end of new
            final EntryToAdjust updatedEntry = entryToAdjust.withStartTime(endTimeNew);
            final ContentProviderOperation update = ContentProviderOperationUtil
                    .newItemUpdate(MCContract.Activity.CONTENT_URI, updatedEntry.id)
                    .withValue(MCContract.Activity.COLUMN_NAME_START_TIME, updatedEntry.startTime)
                    .withValue(MCContract.Activity.COLUMN_NAME_MODIFIED_AT, now)
                    .withExpectedCount(1)
                    .build();
            ops.add(update);
            final SplitResult r =  SplitResult.ofType(SplitType.SHORTENED_NEXT, updatedEntry, depth);
            return SplitResultWithOperations.forResult(r, update);
        }

        if (startsAfterNewStarts && startsBeforeNewEnds && !endsAfterNewEnds) {
            // the old entry completely lies within the bounds of the new entry
            // Opt 1 - "new is always right": delete the old one. CON: cannot undelete - very destructive
            // Opt 2 - execute these adjustments before saving the entry, halting here and
            //         asking the user to choose other times
            // Opt 3 - execute these adjustments before saving the entry, halting here and
            //         asking the user to confirm that the old entry will be lost
            // Opt 4 - "least destructive": split the new entry, telling the user about the split
            return SplitResultWithOperations.forResult(SplitResult.ofType(SplitType.FAILED_LIES_FULLY_WITHIN, entryToAdjust, depth));
        }
        return SplitResultWithOperations.forResult(SplitResult.ofType(SplitType.NOTHING, entryToAdjust, depth));
    }


}
