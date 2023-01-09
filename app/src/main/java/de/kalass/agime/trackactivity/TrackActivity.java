package de.kalass.agime.trackactivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.Iterables;

import java.util.List;

import de.kalass.agime.R;
import de.kalass.agime.analytics.AnalyticsBaseCRUDActivity;
import de.kalass.agime.ongoingnotification.NotificationManagingService;
import de.kalass.android.common.activity.BaseCRUDFragment;
import de.kalass.android.common.activity.CRUDMode;
import de.kalass.android.common.util.TimeFormatUtil;

//import com.linearlistview.LinearListView;

public class TrackActivity extends AnalyticsBaseCRUDActivity {
    public static final String EXTRA_DAY_MILLIS = "dayMillis";
    public static final String EXTRA_STARTTIME_MILLIS = "starttimeMillis";
    public static final String EXTRA_ENDTIME_MILLIS = "endtimeMillis";

    private ServiceConnection _notificationServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // service is bound to control its foreground state, so there is nothing to do here
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // service is bound to control its foreground state, so there is nothing to do here
        }
    };

    @Override
    protected BaseCRUDFragment newCRUDFragment(CRUDMode mode) {
        final Intent intent = getIntent();
        final Bundle extras = intent == null ? null : intent.getExtras();

        final TrackedActivityFragment fragment = new TrackedActivityFragment();

        if (extras != null) {
            Bundle args = new Bundle();
            copyLong(extras, EXTRA_DAY_MILLIS, args, TrackedActivityFragment.EXTRA_DAY_MILLIS);
            copyLong(extras, EXTRA_STARTTIME_MILLIS, args, TrackedActivityFragment.EXTRA_STARTTIME_MILLIS);
            copyLong(extras, EXTRA_ENDTIME_MILLIS, args, TrackedActivityFragment.EXTRA_ENDTIME_MILLIS);
            fragment.setArguments(args);
        }

        return fragment;
    }

    protected void doOnStart() {
        Intent intent = new Intent(this, NotificationManagingService.class);
        startService(intent);
        bindService(intent, _notificationServiceConnection, BIND_ADJUST_WITH_ACTIVITY);
    }

    protected void doOnStop() {
        unbindService(_notificationServiceConnection);
    }

    private void copyLong(Bundle extras, String extrasKey, Bundle args, String argsKey) {
        if (extras.containsKey(extrasKey)) {
            args.putLong(argsKey, extras.getLong(extrasKey));
        }
    }


    @Override
    public void onEntityInserted(BaseCRUDFragment<?,?> fragment, long entityId, Object payload) {
        onEntityInsertedOrUpdated(entityId, (InsertOrUpdateTrackedActivityResult)payload);
    }

    @Override
    public void onEntityUpdated(BaseCRUDFragment<?, ?> fragment, long entityId, Object payload) {
        onEntityInsertedOrUpdated(entityId, (InsertOrUpdateTrackedActivityResult)payload);
    }

    private void onEntityInsertedOrUpdated(long entityId, InsertOrUpdateTrackedActivityResult payload) {
        Intent result = new Intent();
        result.putExtra(EXTRA_STARTTIME_MILLIS, payload.getStartTimeNewMillis());
        result.putExtra(EXTRA_ID, entityId);
        setResult(RESULT_OK, result);
        finish();
    }

}
