package de.kalass.agime.backup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.kalass.agime.R;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.customfield.CustomFieldTypeModelQuery;
import de.kalass.agime.loader.TrackedActivitySyncLoader;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.android.common.DialogUtils;
import de.kalass.android.common.activity.ContentResolverUtil;
import de.kalass.android.common.adapter.AbstractListAdapter;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Shows the list of all Categories that were tracked.
 * Created by klas on 06.10.13.
 */
public class BackupRestoreListFragment extends ListFragment {
    private static final int POSITION_EXPORT_CSV = 0;
    private static final int POSITION_BACKUP = 1;
    private static final int POSITION_RESTORE = 2;

    private static final int ACTIVITY_CODE_RESTORE = 32;

    protected final class OptionsListAdapter extends AbstractListAdapter<String> {

        public OptionsListAdapter(Context context, int layoutResourceId, List<String> items) {
            super(context, layoutResourceId, layoutResourceId, items);
        }

        @Override
        protected View fillView(View view, String model, int position) {
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(model);
            return view;
        }
    }

    public BackupRestoreListFragment() {
        super();
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        Resources resources = getActivity().getResources();
        final OptionsListAdapter adapter = new OptionsListAdapter(getActivity(),
                android.R.layout.simple_list_item_1,
                ImmutableList.of(
                        resources.getString(R.string.action_export_csv),
                        resources.getString(R.string.action_backup),
                        resources.getString(R.string.action_restore)
                ));
        setListAdapter(adapter);
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case POSITION_BACKUP:
                openBackup();
                return;
            case POSITION_EXPORT_CSV:
                openCSVExport();
                return;
            case POSITION_RESTORE:
                openRestore();
                return;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case ACTIVITY_CODE_RESTORE:
                    onRestoreFileChoosen(data);
                    return;
            }
        }
    }

    private void onRestoreFileChoosen(final Intent data) {
        new AlertDialog.Builder(getActivity())
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // nothing to do
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.action_restore_confirm_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Uri selectedUri = data.getData();
                        new RestoreTask(getActivity()).execute(selectedUri);
                    }
                })
                .setTitle(R.string.action_restore_confirm_title)
                .setMessage(R.string.action_restore_confirm_message)
                .show();

    }


    private void openRestore() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(RestoreTask.MIME_TYPE);

        startActivityForResult(
                Intent.createChooser(intent, getResources().getString(R.string.action_restore_chooser)),
                ACTIVITY_CODE_RESTORE
        );
    }

    private static abstract class ExportTask<Param, Progress, D> extends ExportTaskSupport<Param, Progress, D> {
        private final String _filenamePrefix;
        private final String _filenameSuffix;

        ExportTask(Context context, String prefix, String suffix) {
            super(context);
            _filenamePrefix = prefix;
            _filenameSuffix = suffix;

        }

        @Override
        protected void prepareParentDir(File parentdir) {

            // cleanup exports dir, to make sure we are not running out of memory
            // at some time
            for (File f : parentdir.listFiles()) {
                if (!f.delete()) {
                    DialogUtils.showError(getContext(), R.string.error_backup_failed_cleanup);
                    return;
                }
            }

        }


        public String getTmpFileName(D data) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US);
            String timeStamp = sdf.format(System.currentTimeMillis());
            String tmpFileName =  "exports/" + _filenamePrefix + timeStamp + "." + _filenameSuffix;
            return tmpFileName;
        }

        @Override
        protected String getEmailTitle(D data) {
            CharSequence formattedTime = TimeFormatUtil.formatDateTime(getContext(), System.currentTimeMillis());
            return  "AgimeMainActivity Backup: " + formattedTime;
        }
    }

    /**
     * Task which implements exporting of data to some user-file using locally written files and email.
     * @param <Param>
     * @param <Progress>
     * @param <D>
     */
    public static abstract class ExportTaskSupport<Param, Progress, D> extends AsyncTask<Param, Progress, D> {

        private final Context _context;

        public ExportTaskSupport(Context context) {
            _context = context;
        }



        protected abstract void writeDataToFile(File targetFile, D data) throws IOException;

        protected Context getContext() {
            return _context;
        }

        protected abstract String getTmpFileName(D data);

        @Override
        protected final void onPostExecute(D data) {

            File file = new File(getContext().getExternalCacheDir(), getTmpFileName(data));
            String storageState = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
                DialogUtils.showError(getContext(),
                        R.string.error_backup_failed_storage_not_writeable_title,
                        R.string.error_backup_failed_storage_not_writeable);
                return;
            }
            try {

                File parentdir = file.getParentFile();
                if (!parentdir.exists() && !parentdir.mkdirs()) {
                    Log.e("AgimeMainActivity", "Failed to create backup directory " + parentdir);
                    DialogUtils.showError(getContext(), R.string.error_backup_failed_export_dir);
                    return;
                }
                prepareParentDir(parentdir);
                writeDataToFile(file, data);

            } catch (IOException e) {
                Log.e("AgimeMainActivity", "Backup failed", e);
                DialogUtils.showError(getContext(), R.string.error_backup_failed_export_dir);
                return;
            }

            email(getContext(), getEmailTitle(data), "", ImmutableList.<String>of(file.getAbsolutePath()));
        }

        protected void prepareParentDir(File parentdir) {};

        protected abstract String getEmailTitle(D data);
    }

    private void openBackup() {

        new AgimeBackupExportTask(getActivity()).execute();
    }

    static final class CSVExportData {
        private final List<TrackedActivityModel> _activities;
        private final List<CustomFieldTypeModel> _customFields;

        CSVExportData(List<TrackedActivityModel> activities, List<CustomFieldTypeModel> customFields) {
            _activities = activities;
            _customFields = customFields;
        }
    }

    private void openCSVExport() {

        new CSVBackupExportTask(getActivity()).execute();
    }

    public static void email(Context context, /*String emailTo, String emailCC,*/
                             String subject, String emailText, List<String> filePaths)
    {
        //need to "send multiple" to get more than one attachment
        final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType(CSVFileReaderWriter.MIME_TYPE);
        /*
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[]{emailTo});
        emailIntent.putExtra(android.content.Intent.EXTRA_CC,
                new String[]{emailCC});
                */
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        ArrayList<String> extraText = new ArrayList<String>();
        extraText.add(emailText);
        emailIntent.putExtra(Intent.EXTRA_TEXT, extraText);
        //has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();
        //convert from paths to Android friendly Parcelable Uri's
        for (String file : filePaths)
        {
            File fileIn = new File(file);
            Uri u = FileProvider.getUriForFile(context.getApplicationContext(), "de.kalass.agime.provider", fileIn);
            uris.add(u);
        }
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        Intent chooser = Intent.createChooser(emailIntent, context.getResources().getString(R.string.export_chooser));
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            for (Uri uri: uris) {
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        context.startActivity(chooser);
    }

    private static class AgimeBackupExportTask extends ExportTask<Void, Void, BackupData.PersonalBackup> {
        public AgimeBackupExportTask(FragmentActivity activity) {
            super(activity, "agime-backup-", "agime");
        }

        @Override
        protected BackupData.PersonalBackup doInBackground(Void... params) {
            return new FullExportLoader(getContext()).loadBackupData();
        }

        @Override
        protected void writeDataToFile(File file, BackupData.PersonalBackup data) throws IOException {
            final FileOutputStream out = new FileOutputStream(file);
            try {
                data.writeTo(out);
            } finally {
                out.close();
            }
        }
    }

    private class CSVBackupExportTask extends ExportTask<Void, Void, CSVExportData> {

        public CSVBackupExportTask(FragmentActivity activity) {
            super(activity, "agime-export-", "csv");
        }

        @Override
        protected CSVExportData doInBackground(Void... params) {
            final TrackedActivitySyncLoader loader = new TrackedActivitySyncLoader(getContext());
            // load all
            try {
                final List<TrackedActivityModel> trackedActivityModels = loader.query(
                        TrackedActivitySyncLoader.EARLIEST_START_TIME,
                        System.currentTimeMillis(), false /*no fake entries*/);

                final List<CustomFieldTypeModel> customFieldTypes = ContentResolverUtil.loadFromContentResolver(
                        getContext().getContentResolver(),
                        CustomFieldTypeModelQuery.READ,
                        CustomFieldTypeModelQuery.CONTENT_URI,
                        CustomFieldTypeModelQuery.PROJECTION, null, null, null);
                return new CSVExportData(trackedActivityModels, customFieldTypes);
            } finally {
              loader.close();
            }

        }

        @Override
        protected void writeDataToFile(File file, CSVExportData data) throws IOException {
            new CSVFileReaderWriter(new CSVFileReaderWriter.V1FileFormatFactory()).writeAll(file, data._customFields, data._activities);
        }
    }
}
