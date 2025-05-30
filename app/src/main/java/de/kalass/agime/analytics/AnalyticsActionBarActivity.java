package de.kalass.agime.analytics;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import de.kalass.agime.R;
import de.kalass.agime.ongoingnotification.WorkManagerController;

/**
 * Created by klas on 07.01.14.
 */
public class AnalyticsActionBarActivity extends AppCompatActivity {


    protected void doOnStart() {
    }

    protected void doOnStop() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for pending permission dialogs
        WorkManagerController.checkPendingPermissionDialog(this);
    }
    
    /**
     * Shows a Snackbar with the given message and action
     * @param message The message to show
     * @param actionText The text for the action button (or null for no action)
     * @param action The action to perform when the button is clicked (or null)
     * @param duration How long to display the message
     */
    public void showSnackbar(String message, String action1Text, View.OnClickListener action1,
                           String action2Text, View.OnClickListener action2) {
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE);
        
        // First action (usually the main action)
        if (action1Text != null && action1 != null) {
            snackbar.setAction(action1Text, v -> {
                action1.onClick(v);
                snackbar.dismiss();
            });
        }
        
        // Second action (optional)
        if (action2Text != null && action2 != null) {
            snackbar.setActionTextColor(getResources().getColor(android.R.color.holo_blue_light));
            snackbar.setAction(action2Text, v -> {
                action2.onClick(v);
                snackbar.dismiss();
            });
        }
        
        // Customize appearance
        snackbar.setBackgroundTint(getResources().getColor(android.R.color.background_dark));
        snackbar.setTextColor(getResources().getColor(android.R.color.white));
        
        snackbar.show();
    }

    @Override
    public final void onStart() {
        super.onStart();
        doOnStart();
    }

    @Override
    public final void onStop() {
        super.onStop();
        doOnStop();
    }

}
