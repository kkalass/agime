package de.kalass.agime.activitytype;

import android.content.ContentProviderOperation;
import android.content.Context;

import java.util.ArrayList;

import de.kalass.agime.provider.MCContract;
import de.kalass.agime.trackactivity.GetOrInsertCategoryByName;
import de.kalass.android.common.insertorupdate.InsertOrUpdate;
import de.kalass.android.common.insertorupdate.Operations;
import de.kalass.android.common.simpleloader.ContentProviderOperationUtil;
import de.kalass.android.common.simpleloader.ValueOrReference;

/**
 * Performs an insert or update of a activity type, automatically creating  category
 * entries if needed.
 *
 * Created by klas on 21.01.14.
 */
public class InsertOrUpdateActivityType extends InsertOrUpdate<InsertOrUpdateInput, Void> {

    InsertOrUpdateActivityType(Context context) {
        super(context, MCContract.CONTENT_AUTHORITY);
    }

    @Override
    protected Long getId(InsertOrUpdateInput input) {
        return input.entityId;
    }

    @Override
    protected Operations<Void> createOperations(boolean isInsert, Long id, InsertOrUpdateInput input, long now) {

        // Preconditions: Category, Project, Activity Type
        final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        final ValueOrReference categoryId = new GetOrInsertCategoryByName(getContext(), input.categoryColor)
                .addInsertOperation(ops, 0, now, input.categoryTypeId, input.categoryName);

        // Main Entry for Tracked Activity
        final ContentProviderOperation mainOperation = createInsertOrUpdateActivityType(
                id, categoryId, now, input.activityTypeName
        );

        final ValueOrReference mainOperationReference = ContentProviderOperationUtil.add(ops, mainOperation);

        return Operations.getInstance(ops, mainOperationReference);
    }


    protected ContentProviderOperation createInsertOrUpdateActivityType(
            Long activityTypeId,
            ValueOrReference categoryId,
            long now,
            String name
    ) {
        final ContentProviderOperation.Builder builder =
                createInsertOrUpdateBuilder(MCContract.ActivityType.CONTENT_URI, activityTypeId, now)
                .withValue(MCContract.ActivityType.COLUMN_NAME_NAME, name);
        categoryId.appendSelf(builder, MCContract.ActivityType.COLUMN_NAME_ACTIVITY_CATEGORY_ID);

        return builder.build();
    }


}
