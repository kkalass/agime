package de.kalass.agime.backup;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import de.kalass.agime.R;
import de.kalass.agime.SettingsActivity;

/**
 * Implements backup to Google Drive
 * Created by klas on 13.02.14.
 */
public class GoogleDriveBackupHelper implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, BackupHelper {
    private static final String LOG_TAG = "GoogleDriveBackupHelper";

    public static final Predicate<Metadata> BACKUP_FILE_FILTER = new BackupFileFilter<Metadata>() {
        @Override
        public String getName(@Nonnull Metadata metadata) {
            return metadata.getTitle();
        }
    };

    public static final Ordering<Metadata> BACKUP_FILENAME_COMPARATOR_NEWEST_FIRST = new BackupFileOrdering<Metadata>() {
        @Override
        public String getName(@Nonnull Metadata metadata) {
            return metadata.getTitle();
        }
    };

    public static final int TIMEOUT_SECONDS = 60*10;
    private final Context _context;
    private final String _backupFileName;
    private BackupData.PersonalBackup _backupData;
    private GoogleApiClient _gac;

    private final CountDownLatch _latch;


    GoogleDriveBackupHelper(Context context, String backupFileName) {

        _context = context;
        _backupFileName = backupFileName;

        _latch = new CountDownLatch(1);

    }


    @Override
    public boolean prepare() {
        return BackupPreferences.isBackupToDrive(_context);
    }

    @Override
    public void start(BackupData.PersonalBackup data) {
        _backupData = data;
        _gac = new GoogleApiClient.Builder(_context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        _gac.connect();
    }

    @Override
    public void finish() {
        // Important: ensure that the backup service call does not end before the drive backup
        // is done. Will also cleanup the google drive stuff
        try {
            waitForFinished();
        } catch (InterruptedException e) {
            // this is probably a programming error, but it is neither important enough to
            // disrupt the user, nor important enough to justify breaking the entire backup service
            Log.e(LOG_TAG, "Drive Backup did not finish within time", e);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        final FinishSelfContinuation continuation = new FinishSelfContinuation();

        final DriveId backupFolderId = BackupPreferences.getBackupToDriveFolderId(_context);
        if (backupFolderId == null) {
            createBackupInNewBackupFolder(_context, continuation, _gac, _backupData, _backupFileName);
        } else {
            final DriveFolder folder = Drive.DriveApi.getFolder(_gac, backupFolderId);
            folder.getMetadata(_gac)
                    .setResultCallback(new CreateBackupInValidOrNewFolder(_context, continuation, _gac, _backupData, _backupFileName, folder));
        }
        
    }

    @Override
    public void onConnectionSuspended(final int i) {
        finishSelf();
    }

    private void finishSelf() {
        _latch.countDown();
    }

    public void waitForFinished() throws InterruptedException {
        _latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (_gac != null) {
            _gac.disconnect();
            _gac = null;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(LOG_TAG, "Connection to drive failed: " + connectionResult);
        BackupPreferences.setBackupToDriveLinked(_context, false);
        if (connectionResult.hasResolution()) {
            // theoretically, we could pass connectionResult.getResolution() here.
            // but how do we set the drive connection back, if the user should choose to connect?
            // => send the user to the settings
            ServiceUtil.showServiceError(_context, SettingsActivity.class,
                    R.string.backup_failure_drive_connection_title, R.string.backup_failure_drive_connection_details
            );
        } else {
            ServiceUtil.showServiceError(_context, SettingsActivity.class,
                    R.string.backup_failure_drive_connection_title, R.string.backup_failure_drive_connection_details);
        }
        finishSelf();
    }



    private static void createBackupInNewBackupFolder(
            final Context context,
            final Continuation continuation,
            final GoogleApiClient gac,
            final BackupData.PersonalBackup backupData,
            final String backupFileName
    ) {

        Drive.DriveApi.getRootFolder(gac)
                .createFolder(gac, new MetadataChangeSet.Builder().setTitle("Agime " + BackupHelperUtil.getDeviceFolderName()).build())
                .setResultCallback(new CreateBackupInNewFolder(context, continuation, gac, backupData, backupFileName));
    }



    private static class CreateBackupInNewFolder implements ResultCallback<DriveFolder.DriveFolderResult> {
        private final Context context;
        private final Continuation continuation;
        private final GoogleApiClient gac;
        private final BackupData.PersonalBackup backupData;
        private final String backupFileName;

        public CreateBackupInNewFolder(
                Context context, Continuation continuation,
                GoogleApiClient gac,
                BackupData.PersonalBackup backupData,
                String backupFileName) {
            this.context = context;
            this.continuation = continuation;
            this.gac = gac;
            this.backupData = backupData;
            this.backupFileName = backupFileName;
        }

        @Override
        public void onResult(DriveFolder.DriveFolderResult result) {
            if (continuation.finishOnError("initial create folder", result.getStatus())) {
                return;
            }
            final DriveFolder folder = result.getDriveFolder();
            final DriveId driveId = folder.getDriveId();

            BackupPreferences.setBackupToDriveFolderId(context, driveId);

            folder.getMetadata(gac).setResultCallback(new CreateBackupInValidOrNewFolder(context, continuation, gac, backupData, backupFileName, folder));
        }
    }

    private static class CreateBackupInValidOrNewFolder implements ResultCallback<DriveResource.MetadataResult> {
        private final Context context;
        private final Continuation continuation;
        private final GoogleApiClient gac;
        private final BackupData.PersonalBackup backupData;
        private final String backupFileName;
        private final DriveFolder folder;

        public CreateBackupInValidOrNewFolder(
                Context context, Continuation continuation,
                GoogleApiClient gac,
                BackupData.PersonalBackup backupData,
                String backupFileName,
                DriveFolder folder
        ) {
            this.context = context;
            this.continuation = continuation;
            this.gac = gac;
            this.backupData = backupData;
            this.backupFileName = backupFileName;
            this.folder = folder;
        }

        @Override
        public void onResult(DriveResource.MetadataResult metadataResult) {
            if (continuation.finishOnError("backupFolder:metadata", metadataResult.getStatus())) {
                return;
            }
            final Metadata metadata = metadataResult.getMetadata();

            if (metadata.isTrashed() || !metadata.isEditable() || !metadata.isFolder()) {
                // create a new Backup Folder, because the user would not be able
                // to retrieve the new data from the old folder
                if (Log.isLoggable(LOG_TAG, Log.INFO)) {
                    Log.i(LOG_TAG, "The old backup folder was trashed, not editable or a folder - writing to a new folder instead of " + metadata);
                }
                createBackupInNewBackupFolder(context, continuation, gac, backupData, backupFileName);
            } else {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                    Log.d(LOG_TAG, "Creating Bckup in" + metadata.toString());
                }
                folder.listChildren(gac)
                        .setResultCallback(new CreateBackupInValidFolder(context, continuation, gac, folder, backupData, backupFileName));
            }
        }
    }

    private static class CreateBackupInValidFolder implements ResultCallback<DriveApi.MetadataBufferResult> {
        private final Context context;
        private final Continuation continuation;
        private final GoogleApiClient gac;
        private final DriveFolder folder;
        private final BackupData.PersonalBackup backupData;
        private final String backupFileName;

        public CreateBackupInValidFolder(
                Context context,
                Continuation continuation,
                GoogleApiClient gac,
                DriveFolder folder,
                BackupData.PersonalBackup backupData,
                String backupFileName
        ) {
            this.context = context;
            this.continuation = continuation;
            this.gac = gac;
            this.folder = folder;
            this.backupData = backupData;
            this.backupFileName = Preconditions.checkNotNull(backupFileName);
        }

        private List<MetadataCopy> getSortedBackupFiles(MetadataBuffer b) {
            final Iterable<Metadata> filteredFiles = Iterables.filter(b, BACKUP_FILE_FILTER);
            List<Metadata> m = BACKUP_FILENAME_COMPARATOR_NEWEST_FIRST.sortedCopy(filteredFiles);
            final ImmutableList.Builder<MetadataCopy> c = ImmutableList.builder();
            for (Metadata metadata: m) {
                if (!metadata.isTrashed()) {
                    c.add(new MetadataCopy(metadata));
                }
            }
            return c.build();
        }

        @Override
        public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
            if (continuation.finishOnError("createBackupInFolder:childrenRetrieved", metadataBufferResult.getStatus())) {
                return;
            }

            final MetadataBuffer b = metadataBufferResult.getMetadataBuffer();
            try {
                storeBackupIfNeeded(b);
            }finally {
                b.close();
            }
        }


        private void storeBackupIfNeeded(MetadataBuffer b) {
            Log.i(LOG_TAG, "Number of children in backup folder: " + b.getCount());
            final List<MetadataCopy> backupFiles = getSortedBackupFiles(b);
            Log.i(LOG_TAG, "Number of backups in backup folder: " + backupFiles.size());
            if (!backupFiles.isEmpty()) {
                // Do not store the file if it has not changed
                final MetadataCopy latest = backupFiles.get(0);
                final DriveFile latestFile = Drive.DriveApi.getFile(gac, latest.getDriveId());
                latestFile.open(gac, DriveFile.MODE_READ_ONLY, null).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    private BackupData.PersonalBackup read(DriveApi.DriveContentsResult contentsResult) {
                        final DriveContents contents = contentsResult.getDriveContents();
                        InputStream inputStream = contents.getInputStream();
                        try {
                            BackupData.PersonalBackup r = BackupData.PersonalBackup.parseFrom(inputStream);
                            inputStream.close();
                            return r;
                        } catch (IOException e) {
                            Log.w(LOG_TAG, "Latest backup could not be read", e);
                            return null;
                        }
                    }

                    @Override
                    public void onResult(DriveApi.DriveContentsResult contentsResult) {
                        final BackupData.PersonalBackup latestBackup = read(contentsResult);
                        if (BackupHelperUtil.canSkip(latestBackup, backupData)) {
                            Log.i(LOG_TAG, "Skipping backup, because nothing changed since " + latestBackup.getLatestInsertOrUpdateMillis());

                            continuation.finishSuccess();
                        } else {
                            storeBackup(context, backupData, backupFileName, continuation, folder, gac, backupFiles);
                        }
                    }
                });
            } else {
                storeBackup(context, backupData, backupFileName, continuation, folder, gac, backupFiles);
            }
        }


    }

    /**
     * Metadata apparently is some live-reloading stuff, at least if it was retrieved via a buffer.
     * We will need to access the information later, so we need to copy the relevant data
     */
    private static class MetadataCopy {
        private final String title;
        private final DriveId id;
        MetadataCopy(Metadata m) {
            title = m.getTitle();
            id = m.getDriveId();
        }

        DriveId getDriveId() {
            return id;
        }

        String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).addValue(title).addValue(id).toString();
        }
    }

    private static void storeBackup(Context context, BackupData.PersonalBackup backupData, String backupFileName, Continuation continuation, DriveFolder folder, GoogleApiClient gac, List<MetadataCopy> backupFiles) {
        final MetadataCopy metadata = findFileToOverride(context, backupFiles, backupFileName);
        if (metadata == null) {
            Drive.DriveApi.newDriveContents(gac)
                    .setResultCallback(new CreateBackupInNewContent(continuation, gac, backupFileName, backupData, folder));
        } else {
            final DriveFile file = Drive.DriveApi.getFile(gac, metadata.getDriveId());
            if (!backupFileName.equals(metadata.getTitle())) {
                // change the name
                final MetadataChangeSet changeset = new MetadataChangeSet.Builder().setTitle(backupFileName).setMimeType(RestoreTask.MIME_TYPE).build();
                file.updateMetadata(gac, changeset).setResultCallback(new CreateBackupAfterMetadataUpdate(continuation, gac, backupData, file, metadata));
            } else {
                file.open(gac, DriveFile.MODE_WRITE_ONLY, null)
                        .setResultCallback(new CreateBackupInExistingContent(continuation, gac, file, metadata, backupData));
            }
        }
    }
    private static MetadataCopy findFileToOverride(Context context, List<MetadataCopy> backupFiles, String backupFileName) {
        for (MetadataCopy m : backupFiles) {
            if (backupFileName.equalsIgnoreCase(m.getTitle())) {
                Log.i(LOG_TAG, "Found existing backup file: " + backupFileName + ", will replace with new data");
                return m;
            }
        }
        if (backupFiles.isEmpty() || backupFiles.size() < BackupPreferences.getBackupNumFiles(context)) {
            // create a new file, please
            return null;
        }
        // override the oldest one
        return backupFiles.get(backupFiles.size() - 1);
    }

    private static class CreateBackupAfterMetadataUpdate implements ResultCallback<DriveResource.MetadataResult> {
        private final Continuation continuation;
        private final GoogleApiClient gac;
        private final BackupData.PersonalBackup backupData;
        private final DriveFile file;
        private final MetadataCopy metadata;

        public CreateBackupAfterMetadataUpdate(Continuation continuation, GoogleApiClient gac, BackupData.PersonalBackup backupData, DriveFile file, MetadataCopy metadata) {
            this.continuation = continuation;
            this.gac = gac;
            this.backupData = backupData;
            this.file = file;
            this.metadata = metadata;
        }

        @Override
        public void onResult(DriveResource.MetadataResult metadataResult) {
            file.open(gac, DriveFile.MODE_WRITE_ONLY, null)
                    .setResultCallback(new CreateBackupInExistingContent(continuation, gac, file, metadata, backupData));
        }
    }

    private static class CreateBackupInNewContent implements ResultCallback<DriveApi.DriveContentsResult> {
        private final Continuation continuation;
        private final GoogleApiClient gac;
        private final String _backupFileName;
        private final BackupData.PersonalBackup _backupData;
        private final DriveFolder folder;

        private CreateBackupInNewContent(
                Continuation continuation,
                GoogleApiClient gac,
                String backupFileName,
                BackupData.PersonalBackup backupData,
                DriveFolder folder
        ) {
            this.continuation = continuation;
            this.gac = gac;
            _backupFileName = backupFileName;
            _backupData = backupData;
            this.folder = folder;
        }

        @Override
        public void onResult(DriveApi.DriveContentsResult contentsResult) {
            if (continuation.finishOnError("createBackupInFolder:childrenRetrieved:newContentsFailed", contentsResult.getStatus())) {
                return;
            }
            final DriveContents contents = contentsResult.getDriveContents();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(_backupFileName)
                    //.setStarred(true)
                    .setMimeType(RestoreTask.MIME_TYPE)
                    .build();

            final OutputStream outputStream = contents.getOutputStream();
            try {
                _backupData.writeTo(outputStream);
            } catch (IOException e) {
                Log.w(LOG_TAG, "IOError while writing new file: ", e);
                continuation.finishWithError(R.string.backup_failure_drive_title, R.string.backup_failure_drive_details);
                return;
            }

            folder.createFile(
                    gac, changeSet, contents
            ).setResultCallback(new AfterCreateBackupInNewFile(continuation, gac));

        }
    }

    private static class AfterCreateBackupInNewFile implements ResultCallback<DriveFolder.DriveFileResult> {
        private final Continuation continuation;
        private final GoogleApiClient gac;

        private AfterCreateBackupInNewFile(Continuation continuation, GoogleApiClient gac) {
            this.continuation = continuation;
            this.gac = gac;
        }

        @Override
        public void onResult(DriveFolder.DriveFileResult driveFileResult) {
            if (continuation.finishOnError("onCreateFile", driveFileResult.getStatus())) {
                return;
            }
            //final DriveResource.MetadataResult await = driveFileResult.getDriveFile().getMetadata(gac).await();
            //Log.i(LOG_TAG, "Backup stored to a new drive file successfully " + await.getMetadata().getTitle());
            //Log.i(LOG_TAG, "Backup stored to a new drive file successfully ");
            driveFileResult.getDriveFile().getMetadata(gac).setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult metadataResult) {
                    if (continuation.finishOnError("post new file creation", metadataResult.getStatus())) {
                        return;
                    }
                    Log.i(LOG_TAG, "Backup stored to a new drive file successfully " + metadataResult.getMetadata());
                    continuation.finishSuccess();
                }
            });

        }
    }

    private static class CreateBackupInExistingContent implements ResultCallback<DriveApi.DriveContentsResult> {
        private final Continuation continuation;
        private final GoogleApiClient gac;
        private final DriveFile existingFile;
        private final MetadataCopy metadata;
        private final BackupData.PersonalBackup _backupData;

        public CreateBackupInExistingContent(
                Continuation continuation,
                GoogleApiClient gac,
                DriveFile existingFile,
                MetadataCopy metadata,
                BackupData.PersonalBackup backupData
        ) {
            this.continuation = continuation;
            this.gac = gac;
            this.existingFile = existingFile;
            this.metadata = metadata;
            _backupData = backupData;
        }

        @Override
        public void onResult(DriveApi.DriveContentsResult contentsResult) {
            if (continuation.finishOnError("onOpen", contentsResult.getStatus())) {
                return;
            }
            final DriveContents contents = contentsResult.getDriveContents();
            final OutputStream outputStream = contents.getOutputStream();
            try {
                _backupData.writeTo(outputStream);
            } catch (IOException e) {
                Log.w(LOG_TAG, "IOError while writing new file: ", e);
                continuation.finishWithError(R.string.backup_failure_drive_title, R.string.backup_failure_drive_details);
                contents.discard(gac);
                return;
            }
            contents.commit(gac, null);
            continuation.finishSuccess();
        }
    }


    private class FinishSelfContinuation implements Continuation {

        @Override
        public void finishSuccess() {
            // we should not need to trigger syncing, google drive should do it when appropriate
            /*
            Drive.DriveApi.requestSync(_gac).addResultCallback(new DriveApi.OnSyncFinishCallback() {
                @Override
                public void onSyncFinish(Status status) {
                    if (finishOnError("sync after successfull operation", status)) {
                        return;
                    }
                    Log.i(LOG_TAG, "sync finished after successfull operation ");
                    finishSelf();
                }
            });
            */
            Log.i(LOG_TAG, "successfull operation ");
            finishSelf();

        }

        @Override
        public void finishWithError(int msgTitleId, int msgTextId) {
            ServiceUtil.showServiceError(_context, SettingsActivity.class, msgTitleId, msgTextId);
            finishSelf();
        }

        public boolean finishOnError(String msg, Status status) {
            if (status.isSuccess()) {
                return false;
            }
            Log.w(LOG_TAG, msg + " failed: " + status);
            if (status.hasResolution()) {
                ServiceUtil.showServiceError(_context, SettingsActivity.class,
                        R.string.backup_failure_drive_title, R.string.backup_failure_drive_details);
            } else {
                ServiceUtil.showServiceError(_context, status.getResolution(),
                        R.string.backup_failure_drive_title, R.string.backup_failure_drive_details);
            }
            finishSelf();
            return true;
        }

    }
}
