package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.BaseActivity;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.adapter.ItemIcon;
import com.asus.filemanager.loader.AlbumPickerLoader;
import com.asus.filemanager.utility.BucketEntry;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteVFile;

import java.util.ArrayList;
import java.util.Arrays;

public class PhotoPicker extends BaseActivity implements LoaderManager.LoaderCallbacks<BucketEntry[]> {
    private static final String TAG = "PhotoPicker";
    private static boolean DEBUG = true;

    private static final int SCAN_FILE_LOADER = 1;
    private static ViewGroup mPreView = null;
    private static int mPreChoice = -1;
    private static int mNumberLimit;

    private boolean mIsAlbumMode = true;
    private boolean mIsSingleChoice = false;
    private boolean mEnablePicasa = false;
    private Boolean[] mChecked = null;
    private BucketEntry[] mBucketEntries;
    private ItemIcon mItemIcon;
    private PhotoPickerAdapter mAdapter;
    private PhotoPickerDialog mDialogFragment;
    private String mAlbumID;
    private String[] mEnviroment;
    private int mSelectMode;

    static final String[] PROJECTION =  {
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    };

    private boolean[] mMountDevice;

    private StorageManager mStorageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // +++ Willie
        // Set theme to Asus theme style for PadFone
        // The Asus theme only be applied if the resource id has been retrieved.
        int themeAsusDialogId = ThemeUtility.sThemeAsusDialogId;
        if (themeAsusDialogId != 0) {
            setTheme(themeAsusDialogId);
            Theme theme = getTheme();
            theme.applyStyle(R.style.PhotoSelectionStyle, true);
        }
        // ---

        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_select_base);

        mIsAlbumMode = this.getIntent().getBooleanExtra(AlbumPicker.KEY_ALBUM_MODE, false);
        mIsSingleChoice = this.getIntent().getBooleanExtra(AlbumPicker.KEY_SINGLE_CHOICE, false);
        mNumberLimit = this.getIntent().getIntExtra(AlbumPicker.KEY_NUMBER_LIMIT, -1);
        mAlbumID = this.getIntent().getStringExtra(AlbumPicker.KEY_ALBUM_ID);
        mSelectMode = this.getIntent().getIntExtra(AlbumPicker.KEY_SELECT_PHOTO_MODE, AlbumPicker.SELECT_PHOTO_MODE);
        mEnablePicasa = this.getIntent().getBooleanExtra(AlbumPicker.KEY_ENABLE_PICASA, false);

        mItemIcon = new ItemIcon(getApplicationContext());
        mAdapter = new PhotoPickerAdapter(this);

        initEnviroment();
        initPhotoData();
        showDialog();
    }

    void showDialog() {
        // Create the fragment and show it as a dialog
        mDialogFragment = PhotoPickerDialog.newInstance(getResources().getString(R.string.select_photo));
        mDialogFragment.show(getFragmentManager(), "select_photo_dialog");

    }

    private void initPhotoData() {
        if (mStorageManager == null) {
            mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        }

        for (int i = 0 ; i < mEnviroment.length ; i++) {
            if (reflectionApis.getVolumeState(mStorageManager,mEnviroment[i]).equals(Environment.MEDIA_MOUNTED)) {
                if (DEBUG) Log.v(TAG , mEnviroment[i] + " is mounted");
                mMountDevice[i] = true;
            } else {
                if (DEBUG) Log.v(TAG , mEnviroment[i] + " is unmounted");
                mMountDevice[i] = false;
            }
        }

        startScanPhotos(mSelectMode);
    }

    public static class PhotoPickerDialog extends DialogFragment implements
            AdapterView.OnItemClickListener{
        private AlertDialog mAlertDialog;
        private AdapterView<BaseAdapter> mAdapterView;
        private String mOkString;
        private View mProgressBar;
        private int mSelectCount = 0;
        private boolean mIsScrollingList = false;

        public static PhotoPickerDialog newInstance(String title) {
            PhotoPickerDialog dialog = new PhotoPickerDialog();
            Bundle args = new Bundle();
            args.putString("title", title);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            /* On orientation changes, the dialog is effectively "dismissed" so this is called
             * when the activity is no longer associated with this dying dialog fragment. We
             * should just safely ignore this case by checking if getActivity() returns null
             */
            if (getActivity() != null) {
                ((PhotoPicker)getActivity()).doSelectCancelClick();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            String title = getArguments().getString("title");
            LayoutInflater factory = LayoutInflater.from(getActivity());
            final View messegeView = factory.inflate(R.layout.photo_select_list, null);

            mAdapterView = (AdapterView<BaseAdapter>) messegeView.findViewById(R.id.photo_selecton_list);
            mAdapterView.setAdapter(((PhotoPicker)getActivity()).getAlbumPickerAdapter());
            mAdapterView.setOnItemClickListener(this);
            GridView gridView = (GridView) messegeView.findViewById(R.id.photo_selecton_list);
            gridView.setOnScrollListener(new OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                        mIsScrollingList = false;
                        ((PhotoPicker)getActivity()).updateLists();
                    } else {
                        mIsScrollingList = true;
                    }
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem,
                        int visibleItemCount, int totalItemCount) {
                }
            });
//            mAdapterView.setEmptyView(view.findViewById(R.id.empty));
            mProgressBar = (View) messegeView.findViewById(R.id.photo_selection_progress);

            mOkString = ((PhotoPicker)getActivity()).getResources().getString(android.R.string.ok);
            String back = ((PhotoPicker)getActivity()).getResources().getString(R.string.back_button_label);

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

            int themeAsusDialogAlertId = ThemeUtility.sThemeAsusDialogAlertId;

            mAlertDialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(title)
            .setPositiveButton(mOkString, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    ((PhotoPicker) getActivity()).doSelectCompletedClick();
                }
            })
            .setNegativeButton(back, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    ((PhotoPicker) getActivity()).doSelectCancelClick();
                }
            }).create();

            mAlertDialog.setView(messegeView, 0, 0, 0, 0);

            return mAlertDialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (mAlertDialog.getButton(Dialog.BUTTON_POSITIVE) != null) {
                mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(mSelectCount > 0);
            } else {
                if (DEBUG) Log.v(TAG , "alerDialog get positive button is null when calling onStart");
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }

        public void selectedCount(int count, boolean singleChoice) {
            if (mAlertDialog.getButton(Dialog.BUTTON_POSITIVE) != null) {
                mSelectCount = count;
                if (count > 0) {
                    mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
                    if (!singleChoice) {
                        mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setText(mOkString + "(" + count +")");
                    }
                } else {
                    mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
                    mAlertDialog.getButton(Dialog.BUTTON_POSITIVE).setText(mOkString);
                }
            } else {
                if (DEBUG) Log.v(TAG , "alerDialog get positive button is null when calling selectedCount");
            }
        }

        public void setProgreess(boolean need) {
            mProgressBar.setVisibility(need ? View.VISIBLE : View.INVISIBLE);
            mAdapterView.setVisibility(need ? View.INVISIBLE : View.VISIBLE);
        }

        public boolean isScrolling() {
            return mIsScrollingList;
        }
    }

    public void doSelectCompletedClick() {
        ArrayList<String> localFilePaths = new ArrayList<String>();
        ArrayList<String> picasaFileIDs = new ArrayList<String>();
        boolean isIncludePicasa = false;
        for (int i=0 ; i<mChecked.length ; i++) {
            if (mChecked[i]) {
                int fileType = mBucketEntries[i].fileType;
                if (fileType == BucketEntry.LOCALFILE) {
                    localFilePaths.add(mBucketEntries[i].data);
                } else if (fileType == BucketEntry.PICASAFILE) {
                    picasaFileIDs.add(mBucketEntries[i].bucketId);
                    isIncludePicasa = true;
                }
            }
        }
        Intent intent = new Intent();
        intent.putStringArrayListExtra(Intent.EXTRA_STREAM, localFilePaths);
        intent.putExtra(AlbumPicker.KEY_INCLUDE_PICASA, isIncludePicasa);
        intent.putStringArrayListExtra(AlbumPicker.KEY_PICASA_PHOTOS_ID, picasaFileIDs);
        this.setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public void doSelectCancelClick() {
        Intent intent = new Intent();
        this.setResult(Activity.RESULT_FIRST_USER, intent);
        finish();
    }

    public class PhotoPickerAdapter extends BaseAdapter implements ListAdapter, OnClickListener {
        private final LayoutInflater mInflater;
        private Context mContext;
        private int mSelectedCount = 0;

        private class ViewHolder {
            ImageView thumbnail;
            ViewGroup root;
            int location;
        }

        @SuppressWarnings("unchecked")
        public PhotoPickerAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContext = context;
        }

        public int getCount() {
            if (mBucketEntries != null && mBucketEntries.length > 0) {
                return mBucketEntries.length;
            }
            return 0;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            View pressedView;
            View normalView;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.photo_picker_entry, parent, false);

                holder = new ViewHolder();
                holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
                holder.root = (ViewGroup) convertView.findViewById(R.id.photo_picker_item_root);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (holder.thumbnail != null) {
                if (holder.thumbnail.getTag() != mBucketEntries[position].data) {
                    holder.thumbnail.setTag(mBucketEntries[position].data);
                    holder.thumbnail.setImageResource(R.drawable.asus_ep_photo_picker_default_icon);
                }
                if (!mDialogFragment.isScrolling() && mBucketEntries[position].fileType == BucketEntry.PICASAFILE){
                    RemoteVFile picasaVFile = new RemoteVFile(mBucketEntries[position].data, mBucketEntries[position].lastModifiedTime, VFileType.TYPE_PICASA_STORAGE);
                    mItemIcon.setIcon(picasaVFile, holder.thumbnail, false);
                } else if (mBucketEntries[position].fileType == BucketEntry.LOCALFILE) {
                    LocalVFile vFile = new LocalVFile(mBucketEntries[position].data);
                    mItemIcon.setIcon(vFile, holder.thumbnail, false);
                }
            }

            if (holder.root != null) {
                holder.root.setOnClickListener(this);
                holder.location = position;

                pressedView = holder.root.findViewById(R.id.photo_picker_item_pressed);
                normalView = holder.root.findViewById(R.id.photo_picker_item_normal);

                if (mChecked != null && mChecked[position]) {
                    if (mIsSingleChoice) {
                        mPreView = holder.root;
                    }
                    pressedView.setVisibility(View.VISIBLE);
                    normalView.setVisibility(View.INVISIBLE);
                } else {
                    pressedView.setVisibility(View.INVISIBLE);
                    normalView.setVisibility(View.VISIBLE);
                }
            }

            return convertView;
        }

        @Override
        public void onClick(View v) {
            ViewHolder holder = (ViewHolder)v.getTag();
            ViewGroup root = (ViewGroup) v;
            View pressedView = root.findViewById(R.id.photo_picker_item_pressed);
            View normalView = root.findViewById(R.id.photo_picker_item_normal);

            if (mChecked == null) {
                Log.d(TAG, "mChecked is null when calling onClick");
            }

            if (mIsSingleChoice) {
                if (mSelectedCount == 1) {
                    if (mChecked[holder.location]) {
                        pressedView.setVisibility(View.INVISIBLE);
                        normalView.setVisibility(View.VISIBLE);
                        mChecked[holder.location] = false;
                        mPreView = null;
                        mSelectedCount--;
                    } else {
                        if (mPreView != null) {
                            mPreView.findViewById(R.id.photo_picker_item_pressed).setVisibility(View.INVISIBLE);
                            mPreView.findViewById(R.id.photo_picker_item_normal).setVisibility(View.VISIBLE);
                            mChecked[mPreChoice] = false;
                        }
                        pressedView.setVisibility(View.VISIBLE);
                        normalView.setVisibility(View.INVISIBLE);
                        mChecked[holder.location] = true;
                        mPreView = root;
                        mPreChoice = holder.location;
                    }
                } else {
                    pressedView.setVisibility(View.VISIBLE);
                    normalView.setVisibility(View.INVISIBLE);
                    mChecked[holder.location] = true;
                    mPreView = root;
                    mPreChoice = holder.location;
                    mSelectedCount++;
                }
            } else {
                if (mChecked[holder.location]) {
                    pressedView.setVisibility(View.INVISIBLE);
                    normalView.setVisibility(View.VISIBLE);
                    mChecked[holder.location] = false;
                    mSelectedCount--;
                } else {
                    if (mNumberLimit > 0) {
                        if (mSelectedCount == mNumberLimit) {
                            ToastUtility.show(mContext, R.string.photo_selection_limit_number_toast, String.valueOf(mSelectedCount));
                            return;
                        }
                    }
                    pressedView.setVisibility(View.VISIBLE);
                    normalView.setVisibility(View.INVISIBLE);
                    mChecked[holder.location] = true;
                    mSelectedCount++;
                }
            }

            if (mDialogFragment != null) {
                mDialogFragment.selectedCount(mSelectedCount, mIsSingleChoice);
            } else {
                Log.d(TAG, "mDialogFragment is null when calling onClick");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addDataScheme("file");
        registerReceiver(mReviver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReviver);
    }

    public BroadcastReceiver mReviver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                if (DEBUG) Log.v(TAG, "get ACTION_MEDIA_UNMOUNTED");
                initPhotoData();
                updateLists();
            } else if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
                if (DEBUG) Log.v(TAG, "get ACTION_MEDIA_MOUNTED");
                initPhotoData();
                updateLists();
            } else if (intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                if (DEBUG) Log.v(TAG, "get ACTION_MEDIA_SCANNER_FINISHED");
                initPhotoData();
                updateLists();
            }
        }
    };

    public void updateLists() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetInvalidated();
            mAdapter.notifyDataSetChanged();
        }
    }

    private void startScanPhotos(int selectMode) {
        Bundle args = new Bundle();
        args.putBooleanArray(AlbumPicker.KEY_SCAN_MOUNT, mMountDevice);
        args.putBoolean(AlbumPicker.KEY_SCAN_MODE, mIsAlbumMode);
        args.putInt(AlbumPicker.KEY_SELECT_PHOTO_MODE, selectMode);

        if (mDialogFragment != null) {
            mDialogFragment.setProgreess(true);
        }
        getLoaderManager().destroyLoader(SCAN_FILE_LOADER);
        getLoaderManager().restartLoader(SCAN_FILE_LOADER, args, this);
    }

    private PhotoPickerAdapter getAlbumPickerAdapter() {
        return mAdapter;
    }

    @Override
    public Loader<BucketEntry[]> onCreateLoader(int id, Bundle args) {
        boolean isAlbumMode = args.getBoolean(AlbumPicker.KEY_SCAN_MODE);
        boolean[] isMount = args.getBooleanArray(AlbumPicker.KEY_SCAN_MOUNT);
        int selectMode = args.getInt(AlbumPicker.KEY_SELECT_PHOTO_MODE);

        return new AlbumPickerLoader(this, isMount, isAlbumMode, mAlbumID, selectMode, mEnablePicasa);
    }

    @Override
    public void onLoadFinished(Loader<BucketEntry[]> loader, BucketEntry[] data) {
        if (data != null) {
            mBucketEntries = data;
            updateLists();
        }

        if (mDialogFragment != null) {
            mDialogFragment.setProgreess(false);
        }

        if (mBucketEntries != null && mBucketEntries.length > 0) {
            mChecked = new Boolean[mBucketEntries.length];

            for (int i=0 ; i<mChecked.length ; i++) {
                mChecked[i] = false;
            }
        } else if (mBucketEntries == null) {
            Intent intent = new Intent();
            intent.putExtra(AlbumPicker.KEY_UNMOUNT, true);
            this.setResult(Activity.RESULT_FIRST_USER, intent);
            finish();
        }
    }

    @Override
    public void onLoaderReset(Loader<BucketEntry[]> loader) {
    }

    private void initEnviroment() {
        mEnviroment = ((FileManagerApplication)this.getApplication()).getStorageVolumePaths();
        if (mEnviroment != null) {
            mMountDevice = new boolean[mEnviroment.length];
            Arrays.fill(mMountDevice, Boolean.FALSE);
        } else {
            Log.w(TAG, "mEnviroment is null when calling initEnviroment()");
        }
    }
}