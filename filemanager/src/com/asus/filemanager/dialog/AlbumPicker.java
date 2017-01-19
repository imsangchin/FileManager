package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.BaseActivity;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.adapter.ItemIcon;
import com.asus.filemanager.loader.AlbumPickerLoader;
import com.asus.filemanager.utility.BucketEntry;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.remote.utility.RemoteVFile;

import java.util.ArrayList;
import java.util.Arrays;

public class AlbumPicker extends BaseActivity implements LoaderManager.LoaderCallbacks<BucketEntry[]> {
    private static final String TAG = "AlbumPicker";
    private static boolean DEBUG = true;

    private static final int REQUEST_CHOOSE_PHOTO = 1;
    private static final int SCAN_FILE_LOADER = 0;

    public static final int SELECT_ALBUM_MODE = 1;
    public static final int SELECT_PHOTO_MODE = 2;
    public static final int SELECT_PICASA_PHOTO_MODE = 3;
    public static final String KEY_ALBUM_ID = "album-bucket-id";
    public static final String KEY_SCAN_MOUNT = "scan_mount";
    public static final String KEY_SCAN_MODE = "scan_mode";
    public static final String KEY_ALBUM_PATH = "album-path";
    public static final String KEY_ALBUM_NAME = "album-name";
    public static final String KEY_ALBUM_MODE = "album-mode";
    public static final String KEY_SELECT_PHOTO_MODE = "select_photo_mode";
    public static final String KEY_SELECT_ALL_PHOTOS = "select_all_photos";
    public static final String KEY_UNMOUNT = "unmount";
    public static final String KEY_SINGLE_CHOICE = "single_choice";
    public static final String KEY_NUMBER_LIMIT = "number_limit";
    public static final String KEY_ENABLE_PICASA = "enable_picasa"; // check sync success or not
    public static final String KEY_INCLUDE_PICASA = "include_picasa";
    public static final String KEY_PICASA_PHOTOS_ID = "picasa_photos_id";
    public static final String KEY_GET_PICASA = "get_picasa"; // for external user uses

    private boolean mIsAlbumMode = true;
    private boolean mIsSingleChoice = false;
    private boolean[] mMountDevice;
    private boolean mEnablePicasa = false;
    private boolean mReScanPicasa = false;
    private boolean mIncludePicasa = false;
    private AlbumPickerDialog mDialogFragment;
    private AlbumPickerAdapter mAdapter;
    private BucketEntry[] mBucketEntries;
    private ItemIcon mItemIcon;
    private int mNumberLimit;
    private String mFinalBucketID;
    private String mFinalFolderName;
    private String[] mEnviroment;
    private StorageManager mStorageManager;

    static final String[] PROJECTION =  {
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    };

    static final String[] PROJECTION_FOR_BUCKET =  {
        MediaStore.Images.Media.DATA,
    };

    // +++ for picasa
    public static final String PICASA_ACTION_SYNC = "com.android.gallery3d.picasa.action.SYNC";
    public static final String KEY_SELECT_PHOTOS = "select-photos";

    // Message sent to PicasaService
    private static final int MSG_REQUEST_SYNC = 99;

    // Message received from PicasaService
    private static final int SYNC_RESULT_SUCCESS = 0;
    private static final int SYNC_RESULT_CANCELLED = 1;
    private static final int SYNC_RESULT_ERROR = 2;

    private static final int MSG_REGISTER_CLIENT = 1;
    private static final int MSG_UNREGISTER_CLIENT = 2;

    private Messenger mService;

    private Handler mPicasaHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case SYNC_RESULT_SUCCESS:
                    if (DEBUG) Log.d(TAG, "request picasa sync and then get message: SYNC_RESULT_SUCCESS");
                    mReScanPicasa = true;
                    if (mBucketEntries != null) {
                        startScanPhotos(false);
                    }
                    break;
                case SYNC_RESULT_CANCELLED:
                    if (DEBUG) Log.d(TAG, "request picasa sync and then get message: SYNC_RESULT_CANCELLED");
                    mReScanPicasa = false;
                    break;
                case SYNC_RESULT_ERROR:
                    if (DEBUG) Log.e(TAG, "request picasa sync and then get message: SYNC_RESULT_ERROR");
                    mReScanPicasa = false;
                    break;
            }
        }
    };

    private Messenger mMessenger = new Messenger(mPicasaHandler);;
    // ---

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

        mIsAlbumMode = this.getIntent().getBooleanExtra(KEY_ALBUM_MODE, false);
        mIsSingleChoice = this.getIntent().getBooleanExtra(KEY_SINGLE_CHOICE, false);
        mNumberLimit = this.getIntent().getIntExtra(KEY_NUMBER_LIMIT, -1);
        mEnablePicasa = this.getIntent().getBooleanExtra(KEY_GET_PICASA, false);

        mItemIcon = new ItemIcon(getApplicationContext());
        mAdapter = new AlbumPickerAdapter(this);

        initEnviroment();
        initPhotoData();
        showDialog();

        if (mEnablePicasa) {
            mReScanPicasa = true;
            doBindService();
        }
    }

    void showDialog() {
        // Create the fragment and show it as a dialog
        String title;
        if (mIsAlbumMode) {
            title = getResources().getString(R.string.select_album);
        } else {
            title = getResources().getString(R.string.select_photo);
        }
        mDialogFragment = AlbumPickerDialog.newInstance(title);
        mDialogFragment.show(getFragmentManager(), "select_album_dialog");
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

        startScanPhotos(true);
    }

    // handle album mode only
    public void handleIntentResult() {
        Intent intent = new Intent();
        intent.putExtra(KEY_INCLUDE_PICASA, mIncludePicasa);

        if (mFinalBucketID != null) {
            // return bucket_id(local) or album_id(picasa)
            intent.putExtra(KEY_ALBUM_PATH, mFinalBucketID);
            mFinalBucketID = null;
        }
        if (mFinalFolderName != null) {
            intent.putExtra(KEY_ALBUM_NAME, mFinalFolderName);
            mFinalFolderName = null;
        }

        this.setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public void triggerPhotoPicker(String bucketID, int selectMode) {
        ComponentName comp = new ComponentName("com.asus.filemanager", "com.asus.filemanager.dialog.PhotoPicker");
        Intent intent = new Intent();
        intent.setComponent(comp);
        intent.setAction("android.intent.action.VIEW");
        intent.putExtra(KEY_ALBUM_ID, bucketID);
        intent.putExtra(KEY_SINGLE_CHOICE, mIsSingleChoice);
        intent.putExtra(KEY_NUMBER_LIMIT, mNumberLimit);
        intent.putExtra(KEY_SELECT_PHOTO_MODE, selectMode);
        intent.putExtra(KEY_ENABLE_PICASA, mEnablePicasa);

        startActivityForResult(intent, REQUEST_CHOOSE_PHOTO);
    }

    public static class AlbumPickerDialog extends DialogFragment implements
            AdapterView.OnItemClickListener{
        private boolean mIsScrollingList = false;
        private AlertDialog mAlertDialog;
        private AdapterView<BaseAdapter> mAdapterView;
        private View mProgressBar;
        private BucketEntry[] mBucketEntriesTemp;

        public static AlbumPickerDialog newInstance(String title) {
            AlbumPickerDialog dialog = new AlbumPickerDialog();
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
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            String title = getArguments().getString("title");
            LayoutInflater factory = LayoutInflater.from(getActivity());
            final View view = factory.inflate(R.layout.photo_select_list, null);

            mAdapterView = (AdapterView<BaseAdapter>) view.findViewById(R.id.photo_selecton_list);
            mAdapterView.setAdapter(((AlbumPicker)getActivity()).getAlbumPickerAdapter());
            mAdapterView.setOnItemClickListener(this);
            GridView gridView = (GridView) view.findViewById(R.id.photo_selecton_list);
            gridView.setOnScrollListener(new OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                        mIsScrollingList = false;
                        ((AlbumPicker)getActivity()).updateLists();
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
            mProgressBar = (View) view.findViewById(R.id.photo_selection_progress);

            String ok = ((AlbumPicker)getActivity()).getResources().getString(android.R.string.ok);
            String cancel = ((AlbumPicker)getActivity()).getResources().getString(R.string.cancel);

            int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
            int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
            int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
            int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

            mAlertDialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(title)
            .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    ((AlbumPicker) getActivity()).doSelectCancelClick();
                    }
             }).create();

            mAlertDialog.setView(view, 0, 0, 0, 0);

            return mAlertDialog;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mBucketEntriesTemp = ((AlbumPicker)getActivity()).getBucketEntries();
            String finalBucketID = mBucketEntriesTemp[position].bucketId;
            String finalFolderName = mBucketEntriesTemp[position].bucketName;

            ((AlbumPicker)getActivity()).setFinalBucketID(finalBucketID);
            ((AlbumPicker)getActivity()).setFinalFolderName(finalFolderName);

            int fileType = mBucketEntriesTemp[position].fileType;
            boolean albumMode = ((AlbumPicker)getActivity()).isAlbumMode();
            if (albumMode) {
                ((AlbumPicker)getActivity()).setIncludePisasa(fileType == BucketEntry.PICASAFILE);
                ((AlbumPicker)this.getActivity()).handleIntentResult();
            } else {
                if (finalBucketID.equals(KEY_SELECT_ALL_PHOTOS)) {
                    // consider all photos case
                    ((AlbumPicker)this.getActivity()).triggerPhotoPicker(KEY_SELECT_ALL_PHOTOS, SELECT_PHOTO_MODE);
                } else {
                    if (fileType == BucketEntry.LOCALFILE) {
                        ((AlbumPicker)this.getActivity()).triggerPhotoPicker(finalBucketID, SELECT_PHOTO_MODE);
                    } else if (fileType == BucketEntry.PICASAFILE) {
                        ((AlbumPicker)this.getActivity()).triggerPhotoPicker(finalBucketID, SELECT_PICASA_PHOTO_MODE);
                    }
                }
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
    }

    public void doSelectCancelClick() {
    }

    public class AlbumPickerAdapter extends BaseAdapter implements ListAdapter {
        private static final String LOG_TAG = "AlbumPickerAdapter";

        private final LayoutInflater mInflater;
        private Context mContext;

        private class ViewHolder {
            TextView title;
            TextView number;
            ImageView thumbnail;
            ImageView icon;
        }

        private int[] mIconRes = {
            R.drawable.frame_overlay_gallery_folder,
            R.drawable.frame_overlay_gallery_picasa
        };

        @SuppressWarnings("unchecked")
        public AlbumPickerAdapter(Context context) {
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
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.album_picker_entry, parent, false);

                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.number = (TextView) convertView.findViewById(R.id.photo_number);
                holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
                holder.icon = (ImageView) convertView.findViewById(R.id.photo_selection_folder);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (holder.thumbnail != null) {
                if (holder.thumbnail.getTag() != mBucketEntries[position].data) {
                    holder.thumbnail.setTag(mBucketEntries[position].data);
                    holder.thumbnail.setImageResource(R.drawable.asus_ep_album_picker_default_icon);
                }
                if (!mDialogFragment.isScrolling() && mBucketEntries[position].fileType == BucketEntry.PICASAFILE){
                    RemoteVFile picasaVFile = new RemoteVFile(mBucketEntries[position].data, mBucketEntries[position].lastModifiedTime, VFileType.TYPE_PICASA_STORAGE);
                    mItemIcon.setIcon(picasaVFile, holder.thumbnail, false);
                } else if (mBucketEntries[position].fileType == BucketEntry.LOCALFILE) {
                    LocalVFile vFile = new LocalVFile(mBucketEntries[position].data);
                    mItemIcon.setIcon(vFile, holder.thumbnail, false);
                }
            }

            if (holder.title != null) {
                holder.title.setText(mBucketEntries[position].bucketName);
            }

            if (holder.number != null) {
                holder.number.setText(Integer.toString(mBucketEntries[position].number));
            }

            if (holder.icon != null) {
                holder.icon.setImageResource(mIconRes[mBucketEntries[position].fileType]);
            }

            return convertView;
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

            }
        }
    };

    public void updateLists() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetInvalidated();
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG) Log.d(TAG, "onActivityResult get requestCode: " + requestCode + " resultCode: " + resultCode);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CHOOSE_PHOTO) {
            if (data !=null) {
                ArrayList<String> localFilePaths = data.getStringArrayListExtra(Intent.EXTRA_STREAM);
                Intent intent = new Intent();
                if (mFinalFolderName != null) {
                    intent.putExtra(KEY_ALBUM_NAME, mFinalFolderName);
                }
                // return file paths
                intent.putStringArrayListExtra(Intent.EXTRA_STREAM, localFilePaths);

                // +++ handle picasa
                boolean isIncludePicasa = data.getBooleanExtra(KEY_INCLUDE_PICASA, false);
                ArrayList<String> picasaFileIDs = new ArrayList<String>();
                intent.putExtra(KEY_INCLUDE_PICASA, isIncludePicasa);
                if (isIncludePicasa) {
                    picasaFileIDs = data.getStringArrayListExtra(KEY_PICASA_PHOTOS_ID);
                }
                // return photos id
                intent.putExtra(KEY_PICASA_PHOTOS_ID, picasaFileIDs);
                // ---

                this.setResult(Activity.RESULT_OK, intent);
                finish();

            }
            return;
        } else if (resultCode == RESULT_FIRST_USER) {
            boolean isUnmount = false;
            if (data != null) {
                isUnmount = data.getBooleanExtra(KEY_UNMOUNT, false);
            }
            if (isUnmount) {
                initPhotoData();
            }
        } else if (resultCode == RESULT_CANCELED) {
            finish(); // we direct finish the activity
        }
    }

    private void startScanPhotos(boolean needProg) {
        Bundle args = new Bundle();
        args.putBooleanArray(KEY_SCAN_MOUNT, mMountDevice);
        args.putBoolean(KEY_SCAN_MODE, mIsAlbumMode);
        args.putBoolean(KEY_ENABLE_PICASA, mEnablePicasa);

        if (needProg) {
            if (mDialogFragment != null) {
                mDialogFragment.setProgreess(true);
            }
        }

        getLoaderManager().destroyLoader(SCAN_FILE_LOADER);
        getLoaderManager().restartLoader(SCAN_FILE_LOADER, args, this);
    }

    private boolean isAlbumMode() {
        return mIsAlbumMode;
    }

    private AlbumPickerAdapter getAlbumPickerAdapter() {
        return mAdapter;
    }

    private void setFinalBucketID(String value) {
        mFinalBucketID = value;
    }

    private void setFinalFolderName(String value) {
        mFinalFolderName = value;
    }

    private BucketEntry[] getBucketEntries() {
        return mBucketEntries;
    }

    private void setIncludePisasa(boolean isInclude) {
        mIncludePicasa = isInclude;
    }

    @Override
    public Loader<BucketEntry[]> onCreateLoader(int id, Bundle args) {
        boolean isAlbumMode = args.getBoolean(KEY_SCAN_MODE);
        boolean[] isMount = args.getBooleanArray(KEY_SCAN_MOUNT);
        boolean enblePicasa = args.getBoolean(KEY_ENABLE_PICASA);

        return new AlbumPickerLoader(this, isMount, isAlbumMode, "", SELECT_ALBUM_MODE, enblePicasa);
    }

    @Override
    public void onLoadFinished(Loader<BucketEntry[]> loader, BucketEntry[] data) {
        if (data != null) {
            mBucketEntries = data;
            if (mReScanPicasa) {
                startScanPhotos(false);
                mReScanPicasa = false;
                return;
            }
            updateLists();
        }
        if (mDialogFragment != null) {
            mDialogFragment.setProgreess(false);
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

    // +++ for picasa
    private ServiceConnection mPicasaConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DEBUG) Log.d(TAG, "Connect to Gallery2 is onServiceConnected");
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                syncPicasaAlbums();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (DEBUG) Log.d(TAG, "Connect to Gallery2 is onServiceDisconnected");
            mService = null;
        }
    };

    public void doBindService() {
        if (DEBUG) Log.d(TAG, "doBindService to Gallery2");
        Intent intent = new Intent();
        intent.setClassName("com.android.gallery3d", "com.android.gallery3d.picasa.PicasaService");
        intent.putExtra(KEY_SELECT_PHOTOS, true);
        bindService(intent, mPicasaConnection, Context.BIND_AUTO_CREATE);
    }

    public void doUnbindService() {
        if (mService != null) {
            try {
                Message msg = Message.obtain(null, MSG_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }
        unbindService(mPicasaConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mEnablePicasa) {
            doUnbindService();
        }
    }

    private void syncPicasaAlbums() {
        if (mService != null) {
            try {
                Message msg = Message.obtain(mPicasaHandler, MSG_REQUEST_SYNC);
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    // ---
}