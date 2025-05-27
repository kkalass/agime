package de.kalass.android.common.activity;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;

import java.util.ArrayList;

import de.kalass.android.common.AbstractAsyncTask;
import de.kalass.android.common.DialogUtils;
import de.kalass.android.common.InProgressFragment;
import de.kalass.android.common.insertorupdate.InsertOrUpdate;
import de.kalass.android.common.insertorupdate.InsertOrUpdateResult;
import de.kalass.android.common.insertorupdate.Operations;
import de.kalass.android.common.provider.CRUDContentItem;
import de.kalass.android.common.simpleloader.AbstractLoader;
import de.kalass.android.common.simpleloader.ValueOrReference;
import de.kalass.android.common.util.Arrays2;
import de.kalass.agime.R;


/**
 * Base class for Fragments that implement insert/edit/view/delete for some type - including complex types that consist
 * of multiple entities within the same data provider.
 *
 * @param <C> Type of the view wrapper - extremely useful for storing shortcuts to child views and generally organizing
 *        access to your view
 * @param <D> The data loaded in the background and bound to the view when loading has finished
 */
public abstract class BaseCRUDFragment<C, D> extends Fragment implements LoaderManager.LoaderCallbacks<D>, BaseCRUDDBUtil.DeletionCallback {

	private static final String LOG_TAG = "BaseCRUDFragment";
	public static final int LOADER_ID_CRUD = 2;

	private static String ARGS_KEY_CRUD_MODE = "de.kalass.crud.mode";
	private static String ARGS_KEY_CRUD_URI = "de.kalass.crud.uri";

	private final int _layout;
	private final String _contentTypeDir;
	private final String _contentTypeItem;

	private CRUDMode _mode;
	private Uri _uri;
	private Long _id;

	private boolean _viewCreated;
	private D _data;
	private boolean _initialLoadCompleted;
	private boolean _deletionRequested;

	public boolean isDeletable() {
		return true;
	}

	public interface CRUDListener {

		void onEntityInserted(BaseCRUDFragment<?, ?> fragment, long entityId, Object payload);


		void onEntityUpdated(BaseCRUDFragment<?, ?> fragment, long entityId, Object payload);


		void onEntityDeleted(BaseCRUDFragment<?, ?> fragment, long entityId, Object payload);
	}

	public BaseCRUDFragment(
			int layout,
			String contentTypeDir,
			String contentTypeItem) {
		_layout = layout;
		_contentTypeDir = contentTypeDir;
		_contentTypeItem = contentTypeItem;
	}


	public final String getContentTypeDir() {
		return _contentTypeDir;
	}


	public final String getContentTypeItem() {
		return _contentTypeItem;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		_mode = extractMode(args);
		_uri = Uri.parse(args.getString(ARGS_KEY_CRUD_URI));

		if (_mode != CRUDMode.INSERT) {
			_id = ContentUris.parseId(_uri);
			ContentResolverUtil.assertSameContentType(getActivity(), _uri, _contentTypeItem);
		}
		else {
			_id = null;
			ContentResolverUtil.assertSameContentType(getActivity(), _uri, _contentTypeDir);
		}

	}


	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(LOADER_ID_CRUD, null, this);
	}


	public abstract Loader<D> createLoader(int id, Bundle arg);


	public Loader<D> onCreateLoader(int id, Bundle arg) {
		final Loader<D> result = createLoader(id, arg);
		if (result instanceof AbstractLoader) {
			Log.i(LOG_TAG, "preloading data synchronously ");
			((AbstractLoader<D>)result).preload();
		}
		return result;
	}


	public ContentResolver getContentResolver() {
		return getPrivateContext().getContentResolver();
	}


	@Override
	public void onLoadFinished(Loader<D> loader, D data) {
		int loaderId = loader.getId();
		if (loaderId == LOADER_ID_CRUD) {
			_initialLoadCompleted = true;
			if (_viewCreated) {
				if (!_deletionRequested) {
					onBindView(getWrappedView(), data);
				}
				else {
					Log.i(LOG_TAG, "Skipping bind view because deletion of the entity is ongoing.");
				}
			}
			else {
				Log.v(LOG_TAG, "onLoadFinished: wait for view creation");
				// wait for view creation
				_data = data;
			}
		}
	}


	@Override
	public void onLoaderReset(Loader<D> loader) {
		// clear references to data
		_data = null;
	}


	private final Context getPrivateContext() {
		return getActivity();
	}


	protected C getWrappedView() {
		return getWrappedView(getView());
	}


	@Override
	public void onAttach(Activity activity) {
		Preconditions.checkArgument(activity instanceof CRUDListener, "Can only be put into an activity that implements CRUDListener");
		super.onAttach(activity);
	}


	protected CRUDListener getCRUDListener() {
		return (CRUDListener)getActivity();
	}


	/**
	 * Bind the data to the view. Note that data may be null, most commonly when in insert mode. But wether or not data is
	 * null, is determined by the implemenation of the data loading
	 */
	protected abstract void onBindView(C view, D data);


	protected abstract void save();


	protected void performSaveOrUpdateAsync(final ContentProviderOperation operation) {
		performSaveOrUpdateAsync(0, operation);
	}


	protected void performSaveOrUpdateAsync(final int mainEntityOperationIndex,
			final ContentProviderOperation... ops) {
		assertIsSaveOrUpdate();
		Preconditions.checkArgument(ops.length > mainEntityOperationIndex && mainEntityOperationIndex >= 0);
		ContentProviderOperation op = ops[mainEntityOperationIndex];
		if (!_uri.equals(op.getUri())) {
			throw new IllegalArgumentException(
					"The operation at index " + mainEntityOperationIndex +
							" is expected to be  the main entity operation, but was: " + op.getUri() + " - " + op);
		}

		ArrayList<ContentProviderOperation> opsList = Arrays2.asArrayList(ops);

		performSaveOrUpdateAsync(opsList, new ArrayListInsertOrUpdate(getPrivateContext(), _uri, mainEntityOperationIndex, getEntityId()));
	}


	protected String getEntityTypeName() {
		return ((Object)this).getClass().getSimpleName();
	}


	public void onEntityUpdated(long entityId, Object payload) {
		final CRUDListener crudListener = getCRUDListener();
		if (crudListener == null) {
			return;
		}
		crudListener.onEntityUpdated(this, entityId, payload);
	}


	public void onEntityDeleted(long entityId, Object payload) {
		final CRUDListener crudListener = getCRUDListener();
		if (crudListener == null) {
			return;
		}
		crudListener.onEntityDeleted(this, entityId, payload);
	}


	public void onEntityInserted(long entityId, Object payload) {
		final CRUDListener crudListener = getCRUDListener();
		if (crudListener == null) {
			return;
		}
		crudListener.onEntityInserted(this, entityId, payload);
	}


	/**
	 * Creates a ContentProviderOperation for inserting or updating the main entity of this fragment, ensuring that common
	 * data like "modified at" or "created at" is provided.
	 */
	protected ContentProviderOperation createSaveOrUpdateOperation(
			CRUDMode mode, ContentValues values, long now) {
		Preconditions.checkState(mode == CRUDMode.EDIT || mode == CRUDMode.INSERT, "Mode must be edit or insert");
		values.put(CRUDContentItem.COLUMN_NAME_MODIFIED_AT, now);

		final ContentProviderOperation.Builder builder;
		if (mode == CRUDMode.EDIT) {
			builder = ContentProviderOperation.newUpdate(getUri());
			builder.withExpectedCount(1);
		}
		else {
			values.put(CRUDContentItem.COLUMN_NAME_CREATED_AT, now);
			builder = ContentProviderOperation.newInsert(getUri());
		}
		return builder.withValues(values).build();
	}


	protected ContentProviderOperation createDeletionOperation(Uri entityUri) {
		return ContentProviderOperation.newDelete(entityUri).withExpectedCount(1).build();
	}


	protected void delete() {
		assertCanDelete();
		ContentProviderOperation operation = createDeletionOperation(getUri());
		BaseCRUDDBUtil.performDeletionAsync(
			getPrivateContext(), this, getUri().getAuthority(),
			R.plurals.action_delete_title,
			R.string.action_delete_message,
			1,
			operation);
	}


	@Override
	public void onDeletionStart() {
		_deletionRequested = true;
	}


	@Override
	public void onDeletionSuccess() {
		onEntityDeleted(_id, null);
	}


	protected void assertCanDelete() {
		long id = getId();
		Preconditions.checkState(id >= 0, "Cannot delete item without id - is this an update?");
	}


	protected void assertIsSaveOrUpdate() {
		Preconditions.checkState(_mode == CRUDMode.INSERT || _mode == CRUDMode.EDIT,
			"Cannot save or update");
	}


	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(_layout, container, false);
	}


	protected abstract C onWrapView(View view);


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		C children = Preconditions.checkNotNull(onWrapView(view));
		view.setTag(children);

		_viewCreated = true;
		if (_initialLoadCompleted) {
			onBindView(getWrappedView(view), _data);
		}
		else {
			// waiting for the data :-(
		}
	}


	protected C getWrappedView(View view) {
		return Preconditions.checkNotNull((C)view.getTag());
	}


	protected CRUDMode getMode() {
		return _mode;
	}


	/**
	 * @return If #getMode() returns CRUDMode.INSERT, then the directory URI, else the item uri
	 */
	protected Uri getUri() {
		return _uri;
	}


	public Long getEntityId() {
		return _id;
	}


	private static CRUDMode extractMode(Bundle args) {
		int value = args.getInt(ARGS_KEY_CRUD_MODE);
		return CRUDMode.values()[value];
	}


	public static <T extends BaseCRUDFragment> Bundle setCRUDArguments(Bundle args, CRUDMode mode, Uri uri) {
		args = args == null ? new Bundle() : args;
		args.putInt(ARGS_KEY_CRUD_MODE, mode.ordinal());
		args.putString(ARGS_KEY_CRUD_URI, uri.toString());
		return args;
	}


	protected <I, Result> void performSaveOrUpdateAsync(I input, InsertOrUpdate<I, Result> insertOrUpdate) {
		newInsertOrUpdateTask(insertOrUpdate).execute(input);
	}


	protected <I, Result> InsertOrUpdateAsyncTask<I, Result> newInsertOrUpdateTask(final InsertOrUpdate<I, Result> insertOrUpdate) {
		return new InsertOrUpdateAsyncTask<I, Result>(this, insertOrUpdate);
	}

	protected static class InsertOrUpdateAsyncTask<I, Re> extends AbstractAsyncTask<I, InsertOrUpdateResult<Re>> {

		private final InsertOrUpdate<I, Re> _insertOrUpdate;

		private final String _fragmentTag;
		private final int _fragmentId;

		public InsertOrUpdateAsyncTask(BaseCRUDFragment fragment, InsertOrUpdate<I, Re> insertOrUpdate) {
			super(fragment.getActivity());
			_fragmentId = fragment.getId();
			_fragmentTag = fragment.getTag();

			_insertOrUpdate = insertOrUpdate;
			setUseProgressDialog(true);
		}


		@Override
		protected Bundle newProgressDialogArguments() {
			Bundle bundle = super.newProgressDialogArguments();
			bundle.putString(InProgressFragment.ARG_TITLE, getContext().getString(R.string.async_task_progress_title));
			bundle.putString(InProgressFragment.ARG_MESSAGE, getContext().getString(R.string.async_task_progress_message));
			return bundle;
		}


		@Override
		protected Result<InsertOrUpdateResult<Re>> performInBackground(I... params) throws Exception {

			Preconditions.checkArgument(params.length == 1);
			I input = params[0];
			return Result.forSuccess(_insertOrUpdate.execute(input));
		}


		private BaseCRUDFragment findBaseCRUDFragment(FragmentActivity currentActivity) {

			FragmentManager manager = currentActivity.getSupportFragmentManager();
			Fragment fragmentByTag = manager.findFragmentByTag(_fragmentTag);
			if (fragmentByTag instanceof BaseCRUDFragment) {
				return (BaseCRUDFragment)fragmentByTag;
			}
			return (BaseCRUDFragment)manager.findFragmentById(_fragmentId);
		}


		@Override
		protected void onSuccess(Result<InsertOrUpdateResult<Re>> r) {
			InsertOrUpdateResult<Re> result = r.getResults();
			BaseCRUDFragment fragment = findBaseCRUDFragment(getCurrentActivity());
			if (fragment != null) {
				if (result.isInsert()) {
					fragment.onEntityInserted(result.getId(), result.get());
				}
				else {
					fragment.onEntityUpdated(result.getId(), result.get());
				}
			}
		}


		@Override
		protected void onError(Result<InsertOrUpdateResult<Re>> result) {
			Log.e(LOG_TAG, "BaseCRUDFragment.save failed ", result.getException());
			DialogUtils.showError(getContext(), R.string.error_dialog_save_failed_message);
		}

	}

	private static class ArrayListInsertOrUpdate extends InsertOrUpdate<ArrayList<ContentProviderOperation>, Object> {

		private final int mainEntityOperationIndex;
		private final Long id;

		public ArrayListInsertOrUpdate(Context context, Uri uri, int mainEntityOperationIndex, Long id) {
			super(context, uri.getAuthority());
			this.mainEntityOperationIndex = mainEntityOperationIndex;
			this.id = id;
		}


		@Override
		protected Long getId(ArrayList<ContentProviderOperation> input) {
			return id;
		}


		@Override
		protected Operations<Object> createOperations(boolean isInsert, Long id, ArrayList<ContentProviderOperation> input, long now) {

			return Operations.getInstance(input, ValueOrReference.ofReference(mainEntityOperationIndex), null);
		}
	}
}
