package de.kalass.agime.customfield;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.kalass.agime.R;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.activity.BaseCRUDDBUtil;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.simpleloader.CursorFkt;
import de.kalass.android.common.simpleloader.CursorUtil;
import de.kalass.android.common.simpleloader.DBSelectionUtil;
import de.kalass.android.common.util.StringUtil;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by klas on 18.02.14.
 */
public class CustomFieldTypeEditorDBUtil {
    static final class DeletionPreparationData {
        private final Collection<Long> valueIds;

        DeletionPreparationData(Collection<Long> valueIds) {
            this.valueIds = valueIds;
        }
    }

    private static class PrepareDeletionAsyncTask extends AsyncTask<Void, Void, DeletionPreparationData> {
        private final List<Long> entityIds;
        private final BaseCRUDDBUtil.DeletionCallback callback;
        private final Context context;
        private final String name;

        private PrepareDeletionAsyncTask(Context context, BaseCRUDDBUtil.DeletionCallback callback,
                                         List<Long> entityIds, String name) {
            this.context = context;
            this.callback = callback;
            this.entityIds = checkNotNull(entityIds);
            Preconditions.checkArgument(!entityIds.isEmpty());
            this.name = name;
        }

        @Override
        protected DeletionPreparationData doInBackground(Void... params) {

            DBSelectionUtil.Builder sBuilder = DBSelectionUtil.builder()
                    .in(MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID, entityIds);

            final List<Long> valueids = ContentResolverUtil.loadFromContentResolver(
                    context,
                    CursorFkt.newLongGetter(0 /*only field in projection*/),
                    MCContract.CustomFieldValue.CONTENT_URI,
                    new String[]{MCContract.CustomFieldValue._ID},
                    sBuilder.buildSelection(),
                    sBuilder.buildArgs(),
                    null);
            return new DeletionPreparationData(valueids);
        }

        @Override
        protected void onPostExecute(DeletionPreparationData deletionPreparationData) {
            delete(deletionPreparationData);
        }

        protected void delete(DeletionPreparationData deletionPreparationData) {


            // Delete all references to custom field values
            List<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(1 + (deletionPreparationData.valueIds.size()/100));
            for (List<Long> valueIds : Iterables.partition(deletionPreparationData.valueIds, 100)) {
                ops.add(BaseCRUDDBUtil
                        .newDeleteAllOperation(
                                MCContract.ActivityCustomFieldValue.CONTENT_URI,
                                MCContract.ActivityCustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_VALUE_ID,
                                valueIds
                        ));
            }

            // delete the custom field values
            ops.add(BaseCRUDDBUtil.newDeleteAllOperation(
                    MCContract.CustomFieldValue.CONTENT_URI,
                    MCContract.CustomFieldValue.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID,
                    entityIds
            ));

            // delete associations between project and custom type
            ops.add(BaseCRUDDBUtil.newDeleteAllOperation(
                    MCContract.ProjectCustomFieldType.CONTENT_URI,
                    MCContract.ProjectCustomFieldType.COLUMN_NAME_CUSTOM_FIELD_TYPE_ID,
                    entityIds
            ));

            // delete the custom field type
            ops.add(BaseCRUDDBUtil.newDeleteAllOperation(
                    MCContract.CustomFieldType.CONTENT_URI,
                    entityIds
            ));
            String message = StringUtil.formatOptional(context.getString(R.string.custom_field_type_delete_message), name);

            BaseCRUDDBUtil.performDeletionAsync(
                    context, callback, MCContract.CONTENT_AUTHORITY,
                    StringUtil.formatOptional(context.getString(R.string.custom_field_type_delete_title), name),
                    message,
                    ops.toArray(new ContentProviderOperation[ops.size()]));
        }
    }

    public static void delete(
            Context context,
            BaseCRUDDBUtil.DeletionCallback callback,
            String name,
            List<Long> entityIds
    ) {

        new PrepareDeletionAsyncTask(context, callback, entityIds, name).execute();
    }
}
