package de.kalass.agime.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


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
	 * system bars while providing proper inset handling for content positioning.
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
		// For older versions, the existing theme configuration is sufficient
	}


	@RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
	private static void setupEdgeToEdgeV35(Activity activity) {
		// Android 15+ - explicit edge-to-edge configuration
		activity.getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

		// Let the theme handle transparent status bar configuration
		// The layout flags ensure content extends behind system bars
	}


	@RequiresApi(api = Build.VERSION_CODES.R)
	private static void setupEdgeToEdgeV30(Activity activity) {
		// Android 11+ - use WindowInsetsController
		activity.getWindow().setDecorFitsSystemWindows(false);

		// Let the theme handle transparent status bar configuration
		// The window flag ensures content extends behind system bars
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
}
