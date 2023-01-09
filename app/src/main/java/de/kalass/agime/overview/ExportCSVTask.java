package de.kalass.agime.overview;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.kalass.agime.backup.CSVFileReaderWriter;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.overview.model.GroupHeaderType;
import de.kalass.agime.overview.model.GroupHeaderTypes;
import de.kalass.agime.overview.model.OverviewConfiguration;
import de.kalass.android.common.AbstractAsyncTask;

/**
 * Created by klas on 14.07.15.
 */
public final class ExportCSVTask extends AbstractAsyncTask<ExportCSVInput, Void> {
    private static final String LOG_TAG = "ExportCSV";

    public ExportCSVTask(FragmentActivity activity) {
        super(activity);
    }

    @Override
    protected Result<Void> performInBackground(ExportCSVInput... params) throws Exception {
        Preconditions.checkArgument(params.length == 1);
        ExportCSVInput input = params[0];
        Uri outputUri = Preconditions.checkNotNull(input.outputUri, "output uri must not be null");

        try {
            ParcelFileDescriptor pfd = getContext().getContentResolver().
                    openFileDescriptor(outputUri, "w");

            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());

            CSVFileReaderWriter writer = new CSVFileReaderWriter(new CSVFileReaderWriter.V2FileFormatFactory(getCurrentActivity(), input.csvFields));

            writer.writeAll(fileOutputStream, input.customFields, input.activities);

            fileOutputStream.close();
            pfd.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Failed to write CSV", e);
        }
        return null;
    }

}
