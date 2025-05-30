package de.kalass.agime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.nineoldandroids.view.ViewHelper;
import de.kalass.agime.util.EdgeToEdgeHelper;


/**
 * Borrowed heavily from https://github.com/ksoichiro/Android-ObservableScrollView Created by klas on 03.04.15.
 */
public class ResizableToolbarHelper {

	private final Activity _activity;
	private final View mOverlayView;
	private final FrameLayout mCustomToolbarView;
	private final View mContentFrame;
	private final int mShortAnimationDuration;

	private int maxCustomToolbarHeight;
	private ToolbarResizeCallback _callback;
	private int initialScrollY;
	private int currentHeight;
	private int scrollYDiff;
	private int baseScrollYDiff;
	private int minToobarHeight = -1;
	private Object scrollSource;
	private ValueAnimator _currentAnimator;
	private int statusBarHeight = 0;

	public int getCurrentToolbarHeight() {
		return currentHeight;
	}


	public int getMinToolbarHeight() {
		if (minToobarHeight < 0) {
			minToobarHeight = getActionBarSize();
		}
		return minToobarHeight;
	}


	public int getMaxToolbarHeight() {
		return maxCustomToolbarHeight;
	}

	public interface ResizableToolbarActivity {

		void setCustomToolbar(View customToolbar, ToolbarResizeCallback callback, int maxCustomToolbarHeight);


		int getCurrentToolbarHeight();


		int getMaxToolbarHeight();


		int getMinToolbarHeight();


		int getScreenHeight();


		void onScrollChanged(Object source, int scrollY, boolean first, boolean dragging);


		void resizeCustomToolbarSmoothly(Object source, int scrollY, int expectedSize, Runnable runnable);
	}

	public interface ToolbarResizeCallback {

		void adjustCustomToolbarHeight(int initialHeight, int currentHeight, int targetHeight);
	}

	ResizableToolbarHelper(Activity activity) {
		_activity = activity;
		mOverlayView = findViewById(R.id.overlay);
		mCustomToolbarView = (FrameLayout)findViewById(R.id.custom_toolbar);

		mContentFrame = findViewById(R.id.fragment_container);

		mShortAnimationDuration = getResources().getInteger(
			android.R.integer.config_shortAnimTime);

		// Set up inset listener to track status bar height for overlay positioning
		setupOverlayInsetsListener();
	}


	/**
	 * Sets up window insets listener to track status bar height for proper overlay positioning. The overlay needs to
	 * account for system window insets to coordinate with custom toolbar positioning.
	 */
	private void setupOverlayInsetsListener() {
		ViewCompat.setOnApplyWindowInsetsListener(mOverlayView, (view, windowInsets) -> {
			androidx.core.graphics.Insets systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
			statusBarHeight = systemBarInsets.top;
			android.util.Log.d("ResizableToolbarHelper", "Status bar height updated: " + statusBarHeight);
			return windowInsets;
		});
	}


	private Resources getResources() {
		return _activity.getResources();
	}


	protected int getActionBarSize() {
		TypedValue typedValue = new TypedValue();
		int[] textSizeAttr = new int[] {androidx.appcompat.R.attr.actionBarSize};
		int indexOfAttrTextSize = 0;
		TypedArray a = _activity.obtainStyledAttributes(typedValue.data, textSizeAttr);
		int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
		a.recycle();
		return actionBarSize;
	}


	public void setCustomToolbar(View customToolbar, ToolbarResizeCallback cb, int maxCustomToolbarHeight) {
		//frame.removeViewAt(0);
		this.maxCustomToolbarHeight = maxCustomToolbarHeight;
		currentHeight = maxCustomToolbarHeight;
		scrollSource = null;
		baseScrollYDiff = 0;
		initialScrollY = 0;
		scrollYDiff = 0;
		_callback = cb;

		ViewGroup.LayoutParams olp = mOverlayView.getLayoutParams();
		olp.height = maxCustomToolbarHeight;
		mOverlayView.setLayoutParams(olp);

		// Initial overlay positioning accounting for status bar height
		int trY = currentHeight - maxCustomToolbarHeight + statusBarHeight;
		ViewHelper.setTranslationY(mOverlayView, trY);

		mCustomToolbarView.removeAllViews();
		mCustomToolbarView.addView(customToolbar, 0);

		// Apply edge-to-edge insets to prevent status bar overlap
		android.util.Log.d("ResizableToolbarHelper", "Applying EdgeToEdge insets to custom toolbar: " + customToolbar.getClass().getSimpleName());
		EdgeToEdgeHelper.applySystemWindowInsetsToToolbar(customToolbar);
	}


	private View findViewById(int resId) {
		return _activity.findViewById(resId);
	}


	public void resizeCustomToolbarSmoothly(Object source, int scrollY, int currentHeight, Runnable runnable) {
		if (this.currentHeight != currentHeight) {
			//Log.i("Frg", "*** RESIZE SMOOTHLY TO " + currentHeight + "*** scrollY: " + scrollY + ", FROM " + this.currentHeight);
			this.scrollSource = null;// scroll source is currently used to detect first scroll after a page change - thus we set it to null here to not interfere with that detection
			baseScrollYDiff = 0;
			scrollYDiff = maxCustomToolbarHeight - currentHeight;
			initialScrollY = scrollY;

			doResizeCustomToolbarSmoothly(currentHeight, runnable);

		}
		else {
			runnable.run();
		}
	}


	private void doResizeCustomToolbarSmoothly(int targetCurrentHeight, Runnable runnable) {
		int startCurrentHeight = this.currentHeight;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			doAnimateResizeToolbar(targetCurrentHeight, startCurrentHeight, runnable);

		}
		else {
			// FIXME: theoretically, we can animate with nineoldandroids. But in practice,
			// there were problems in animating the date/time dialogs on older Androids,
			// and since I currently cannot test 2.3 any more, I skip animations for old androids.
			resizeCustomToolbar(targetCurrentHeight);
			mCustomToolbarView.post(runnable);

		}
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void doAnimateResizeToolbar(int targetCurrentHeight, int startCurrentHeight, final Runnable finishCb) {
		// If there's an _currentAnimator in progress, cancel it
		// immediately and proceed with this one.
		if (_currentAnimator != null) {
			_currentAnimator.cancel();
		}
		ValueAnimator animator = ValueAnimator.ofInt(startCurrentHeight, targetCurrentHeight);
		animator.setDuration(mShortAnimationDuration);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				// FIXME: theoretically, we can animate with nineoldandroids. -> see FIXME above
				if (Build.VERSION.SDK_INT >= 11) {
					resizeCustomToolbar((int)animation.getAnimatedValue());
				}
			}
		});
		animator.addListener(new AnimatorListenerAdapter() {

			@Override
			public void onAnimationEnd(Animator animation) {
				_currentAnimator = null;
				mCustomToolbarView.post(finishCb);

			}


			@Override
			public void onAnimationCancel(Animator animation) {
				_currentAnimator = null;
				mCustomToolbarView.post(finishCb);
			}
		});
		animator.setInterpolator(new DecelerateInterpolator());
		animator.start();
		_currentAnimator = animator;
	}


	private void resizeCustomToolbar(int currentHeight) {

		//Log.i("Frg", "scrollY: " + scrollY + " , first: " + first + ", dragging: " + dragging);
		this.currentHeight = currentHeight;

		// Calculate overlay translation accounting for status bar height to coordinate with custom toolbar positioning
		int trY = currentHeight - maxCustomToolbarHeight + statusBarHeight;
		ViewHelper.setTranslationY(mOverlayView, trY);

		android.util.Log.d("ResizableToolbarHelper", "Resizing toolbar - currentHeight: " + currentHeight +
				", maxHeight: " + maxCustomToolbarHeight + ", statusBarHeight: " + statusBarHeight + ", trY: " + trY);

		_callback.adjustCustomToolbarHeight(maxCustomToolbarHeight, currentHeight, getMinToolbarHeight());

	}


	public void onScrollChanged(Object source, int scrollY, boolean first, boolean dragging) {
		if (first || !com.google.common.base.Objects.equal(this.scrollSource, source)) {
			initialScrollY = scrollY;
			baseScrollYDiff = scrollYDiff;
			//Log.i("Frg", "***FIRST***");
		}
		this.scrollSource = source;
		scrollYDiff = Math.min(maxCustomToolbarHeight - getMinToolbarHeight(), Math.max(baseScrollYDiff + scrollY - initialScrollY, 0));

		//Log.i("Frg", "scrollYDiff: " + scrollYDiff + " , scrollY: " + scrollY+ " , initialScrollY: " + initialScrollY + ", first: " + first + ", dragging: " + dragging);

		//int lastCurrentHeight = currentHeight;
		int currentHeight = Math.max(0, Math.min(maxCustomToolbarHeight - scrollYDiff, maxCustomToolbarHeight));
		//Log.i("Frg", "scrollYDiff: " + scrollYDiff + " , currentHeight: " + currentHeight+ " , maxCustomToolbarHeight: " + maxCustomToolbarHeight + " , scrollY: " + scrollY+ " , initialScrollY: " + initialScrollY + ", first: " + first + ", dragging: " + dragging);
		resizeCustomToolbar(currentHeight);
	}

}
