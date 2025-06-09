package de.kalass.agime.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import de.kalass.agime.R;


/**
 * Utility class for handling edge-to-edge window insets on Android 15+. Provides consistent edge-to-edge behavior with
 * transparent system bars and proper inset handling for content positioning.
 */
public final class EdgeToEdgeHelper {

	private EdgeToEdgeHelper() {
		// Utility class
	}


	/**
	 * Configures edge-to-edge display for an activity. This method ensures that the activity content extends behind
	 * system bars while providing proper inset handling for content positioning and a colored status bar background.
	 * 
	 * @param activity The activity to configure
	 */
	public static void setupEdgeToEdge(@NonNull Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			setupEdgeToEdgeV35(activity);
		}
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			setupEdgeToEdgeV30(activity);
		}
		else {
			// For older versions, set status bar color directly for consistency
			setupStatusBarColorForOlderVersions(activity);
		}

		// Add colored status bar background for Android 11+ where we use transparent system bars
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			addStatusBarBackground(activity);
		}
	}


	@RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
	private static void setupEdgeToEdgeV35(Activity activity) {
		if (activity instanceof ComponentActivity) {

			EdgeToEdge.enable((ComponentActivity)activity, SystemBarStyle.dark(activity.getResources().getColor(R.color.primary_dark, activity.getTheme())));
		}
		else {

			// Android 15+ - use proper edge-to-edge API instead of deprecated setStatusBarColor
			WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

			// Make system bars transparent - this is the recommended approach for Android 15+
			activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
			activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);

			// The status bar background color will be handled by addStatusBarBackground() method
			// which creates a colored view behind the transparent status bar
		}
	}


	@RequiresApi(api = Build.VERSION_CODES.R)
	private static void setupEdgeToEdgeV30(Activity activity) {
		// Android 11+ - use WindowInsetsController with transparent system bars
		WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

		// Make system bars transparent for consistency with Android 15+ behavior
		activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
		activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);

		// The status bar background color will be handled by addStatusBarBackground() method
	}


	/**
	 * Sets status bar color for Android versions older than Android 11 (API < 30). This ensures consistent blue status
	 * bar appearance across all Android versions. For older versions, setStatusBarColor is still supported and not
	 * deprecated.
	 * 
	 * @param activity The activity to configure
	 */
	private static void setupStatusBarColorForOlderVersions(Activity activity) {
		// For older versions (< Android 11), setStatusBarColor is still supported and not deprecated
		// The existing theme configuration handles the rest
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.primary_dark));
		}
	}


	/**
	 * Applies system window insets to a toolbar or top-level view. With transparent status bar, the toolbar needs proper
	 * top padding/margin to avoid overlap with the status bar.
	 * 
	 * @param toolbar The toolbar or top-level view to apply insets to
	 */
	public static void applySystemWindowInsetsToToolbar(@NonNull View toolbar) {
		ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, windowInsets) -> {
			androidx.core.graphics.Insets systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

			// Apply top padding/margin to avoid status bar overlap
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)view.getLayoutParams();
			layoutParams.topMargin = systemBarInsets.top;
			layoutParams.leftMargin = systemBarInsets.left;
			layoutParams.rightMargin = systemBarInsets.right;
			view.setLayoutParams(layoutParams);

			// Return remaining insets for navigation bar to be handled by child views
			return new WindowInsetsCompat.Builder(windowInsets)
				.setInsets(WindowInsetsCompat.Type.systemBars(), androidx.core.graphics.Insets.of(
					0, // Left handled by toolbar margin
					0, // Top handled by toolbar margin
					0, // Right handled by toolbar margin
					systemBarInsets.bottom))
				.build();
		});
	}


	/**
	 * Applies system window insets to a content view that should avoid the navigation bar. This method ensures that the
	 * view has appropriate bottom padding to avoid overlap with the system navigation bar.
	 * 
	 * @param contentView The content view to apply insets to
	 */
	public static void applySystemWindowInsetsToContent(@NonNull View contentView) {
		ViewCompat.setOnApplyWindowInsetsListener(contentView, (view, windowInsets) -> {
			// Get the system bar insets
			androidx.core.graphics.Insets systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

			// Apply bottom padding for navigation bar, preserve existing padding for other sides
			view.setPadding(
				view.getPaddingLeft(),
				view.getPaddingTop(),
				view.getPaddingRight(),
				systemBarInsets.bottom);

			// Return consumed insets
			return WindowInsetsCompat.CONSUMED;
		});
	}


	/**
	 * Adds a colored status bar background to the activity. This creates a visual background behind the transparent
	 * status bar to achieve consistent blue status bar appearance across all activities, similar to how AgimeMainActivity
	 * uses DrawerLayout.setStatusBarBackgroundColor().
	 * 
	 * @param activity The activity to add status bar background to
	 */
	private static void addStatusBarBackground(@NonNull Activity activity) {
		// Skip if this is a DrawerLayout activity (like AgimeMainActivity) as it handles status bar color itself
		View decorView = activity.getWindow().getDecorView();
		if (decorView instanceof ViewGroup) {
			ViewGroup decorViewGroup = (ViewGroup)decorView;

			// Check if there's already a DrawerLayout in the content (skip if found)
			View contentView = decorViewGroup.findViewById(android.R.id.content);
			if (contentView instanceof ViewGroup) {
				ViewGroup contentViewGroup = (ViewGroup)contentView;
				for (int i = 0; i < contentViewGroup.getChildCount(); i++) {
					if (contentViewGroup.getChildAt(i) instanceof DrawerLayout) {
						return; // DrawerLayout handles status bar color, no need for background
					}
				}
			}

			// Create status bar background view
			View statusBarBackground = new View(activity);
			statusBarBackground.setId(View.generateViewId());
			// Set blue background color to match AgimeMainActivity's status bar
			statusBarBackground.setBackgroundColor(activity.getResources().getColor(R.color.primary_dark));

			// Create layout parameters for FrameLayout (DecorView is typically a FrameLayout)
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT,
					0 // Will be set by insets listener
			);
			params.gravity = android.view.Gravity.TOP;
			statusBarBackground.setLayoutParams(params);

			// Add window insets listener to size the background to status bar height
			ViewCompat.setOnApplyWindowInsetsListener(statusBarBackground, (view, windowInsets) -> {
				androidx.core.graphics.Insets systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());

				// Set height to status bar height
				ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
				layoutParams.height = systemBarInsets.top;
				view.setLayoutParams(layoutParams);

				return windowInsets;
			});

			// Add the background view directly to DecorView so it appears behind everything
			decorViewGroup.addView(statusBarBackground);
		}
	}
}
