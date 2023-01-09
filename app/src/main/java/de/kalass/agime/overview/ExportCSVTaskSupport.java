package de.kalass.agime.overview;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.kalass.agime.backup.BackupRestoreListFragment;
import de.kalass.agime.backup.CSVFileReaderWriter;

/**
 * Created by klas on 14.07.15.
 */
public final class ExportCSVTaskSupport extends BackupRestoreListFragment.ExportTaskSupport<ExportCSVInput, Void, ExportCSVInput> {
    private static final String LOG_TAG = "ExportCSV";
    private final String filename;

    public ExportCSVTaskSupport(FragmentActivity activity, String filename) {
        super(activity);
        this.filename = filename;
    }


    @Override
    protected String getEmailTitle(ExportCSVInput data) {
        return filename;
    }

    @Override
    protected String getTmpFileName(ExportCSVInput data) {
        return filename;
    }

    @Override
    protected ExportCSVInput doInBackground(ExportCSVInput[] params) {
        Preconditions.checkArgument(params.length == 1, "Need to be called with exactly one parameter");
        return params[0];
    }

    @Override
    protected void writeDataToFile(File targetFile, ExportCSVInput input) throws IOException {
        CSVFileReaderWriter writer = new CSVFileReaderWriter(new CSVFileReaderWriter.V2FileFormatFactory(getContext(), input.csvFields));

        writer.writeAll(targetFile, input.customFields, input.activities);

    }
}
