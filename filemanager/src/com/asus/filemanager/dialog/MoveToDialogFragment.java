package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.adapter.DeviceListAdapter;
import com.asus.filemanager.adapter.MoveToFileListAdapter;
import com.asus.filemanager.adapter.MoveToNaviAdapter;
import com.asus.filemanager.adapter.StorageListAdapger;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.ga.GaMoveToDialog;
import com.asus.filemanager.loader.ScanFileLoader;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaItem;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.PathIndicator;
import com.asus.filemanager.utility.SortUtility.SortType;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.RemoteDataEntry;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteFileUtility.RemoteUIAction;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.HandlerCommand.ListArgument;
import com.asus.service.cloudstorage.common.MsgObj;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MoveToDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<VFile[]>,OnClickListener,
DialogInterface.OnClickListener,OnKeyListener{

    private final static String TAG = "MoveToDialogFragment";

    public static final int MODE_MOVE_TO = 1;
    public static final int MODE_SINGLE_MOVE_TO_IN_CATEGORY_TOP = 2;
    public static final int MODE_MULTIPLE_MOVE_TO_IN_CATEGORY_TOP = 3;
    public static final int MODE_LOCAL_FOLDER_PICKER = 4;
    public static final int MODE_HIDDEN_ZONE = 5;
    public static final int MODE_NAVIGATION_NORMAL = 6;
    public static final int MODE_NAVIGATION_LOCAL_ONLY = 7;
    public static final int MODE_NAVIGATION_SAMBA_ONLY = 8;
    public static final int MODE_NAVIGATION_CLOUD_ONLY = 7;

    protected ListView mMainPageList = null;
    private ListView mContentList = null;
    protected View mContentContainer = null;
    private View mProgressContainer = null;
    private View mListContainer = null;
    private View mEmptyView;
    private ImageView mAddFolderButton;
    private TextView mStandardEmptyView;
    private LinearLayout mPathContainer;
    private ImageView mPathHome = null;
    private StorageListAdapger mNaviAdapter = null;
    private DeviceListAdapter mDeviceAdapter = null;
    private MoveToFileListAdapter mFileAdapter = null;
    private AlertDialog.Builder mBuilder = null;
    private MoveToPathIndicatorClickListener mPathIndicatorClickListener;
    private FileManagerActivity mActivity = null;
    private CharSequence mEmptyText;

    FileListFragment mFileListFrament = null;
    protected VFile mIndicatorFile;

    private String[] mFileFilter = null;
    private String mAccountNameStorageType = "";
    private boolean mIsBackEvent = false;

    public final static String DIALOG_TAG = "MoveToDialogFragment";

    private static final String KEY_SCAN_PATH = "scan_path";
    private static final String KEY_SCAN_TYPE = "scan_type";
    private static final String KEY_SORT_TYPE = "sort_type";
    private static final String KEY_VFILE_TYPE = "vfile_type"; // for remote storage
    private static final String KEY_HIDDEN_TYPE = "hidden_type";
    private static final String KEY_CHECK_POOL = "check_pool";
    private static final String KEY_FILE_FILTER = "file_filter";

    private int mSortType = SortType.SORT_NAME_DOWN;
    private static final int SCAN_FILE_LOADER = 427;

    public static final String DIALOG_TITLE =  "dialog_title";
    public static final String DIALOG_MODE = "dialog_mode";
    public static final String CURRENT_FOLDER = "current_folder";
    public static final String DIALOG_TYPE = "dialog_type";
    private static final int DIALOG_NAVI = 1;
    private static final int DIALOG_CONTENT = 2;

    public interface OnFolderSelectListener {
        public void onFolderSelected(VFile selectedVFile, Bundle data);
    }

    public static MoveToDialogFragment newInstance(Bundle args){
        MoveToDialogFragment moveToDialogFragment = new MoveToDialogFragment();
        moveToDialogFragment.setArguments(args);
        return moveToDialogFragment;
    }

    public  boolean isMoveToDialogShow(){
        return getDialog().isShowing();
    }


    //  @Override
    //  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    //          Bundle savedInstanceState){
    //      View layoutView = inflater.inflate(R.layout.filelist_fragment, null);
    //      ListView lv = (ListView)layoutView.findViewById(R.id.list_content);
    //      SimpleAdapter adapter = new SimpleAdapter(mcontext,getData(),R.layout.list_item,
    //              new String[]{"title","info","img"},
    //              new int[]{R.id.title,R.id.info,R.id.img});
    //      lv.setAdapter(adapter);
    //      return layoutView;
    //  }

    protected  DialogInterface.OnClickListener getDialogInterface(){
        return this;
    }
    @Override
    public Dialog onCreateDialog(Bundle saveInstanceState){
        super.onCreateDialog(saveInstanceState);
        //		setRetainInstance(true);

        Bundle data = getArguments();

        mActivity = (FileManagerActivity)getActivity();

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layoutView = inflater.inflate(R.layout.move_to_dialog_fragment, null);
        View titleView = inflater.inflate(R.layout.move_to_dialog_title, null);
        mMainPageList = (ListView)layoutView.findViewById(R.id.storageNavi);
        mPathHome = (ImageView)layoutView.findViewById(R.id.path_home);
        mPathHome.setOnClickListener(this);
        mPathContainer = (LinearLayout) layoutView.findViewById(R.id.pathContainer);
        mProgressContainer = layoutView.findViewById(R.id.progressContainer);
        mListContainer = layoutView.findViewById(R.id.listContainer);
        mStandardEmptyView = (TextView)layoutView.findViewById(android.R.id.empty);
        if (mStandardEmptyView == null) {
            mEmptyView = layoutView.findViewById(android.R.id.empty);
        } else {
            mStandardEmptyView.setVisibility(View.GONE);
        }
        mPathIndicatorClickListener = new MoveToPathIndicatorClickListener();

        if(mNaviAdapter == null){
            MoveToNaviAdapter moveToNaviAdapter = mActivity.getMoveToNaviAdapter();
            if (data != null) {
                VFile currentFolder = (VFile) data.getParcelable(CURRENT_FOLDER);
                moveToNaviAdapter.setCurrentFolder(currentFolder);
            }
            setNaviAdapterMode(moveToNaviAdapter, data.getInt(DIALOG_MODE));
            mMainPageList.setAdapter(moveToNaviAdapter);
        }
        mMainPageList.setOnItemClickListener(mActivity.getMoveToNaviAdapter());

        mContentContainer = layoutView.findViewById(R.id.file_content_view);
        mContentList = (ListView)layoutView.findViewById(R.id.content_list);
        setFileListAdapter();
        if (mEmptyView != null) {
            mContentList.setEmptyView(mEmptyView);
        } else if (mEmptyText != null) {
            mContentList.setEmptyView(mStandardEmptyView);
        }

        TextView title = (TextView) titleView.findViewById(android.R.id.title);//= new TextView(getActivity());
        title.setMinWidth(dip2px(100));
        title.setText(data == null ? null : data.getString(DIALOG_TITLE));
        title.setTextSize(25);
        // title.setGravity(Gravity.CENTER);
        //title.setTextColor(Color.BLACK);

        mAddFolderButton = (ImageView) titleView.findViewById(R.id.add_folder_action);
        mAddFolderButton.setOnClickListener(this);
        ThemeUtility.setItemIconColor(getActivity(), mAddFolderButton.getDrawable());

        mFileListFrament = (FileListFragment)getFragmentManager().findFragmentById(R.id.filelist);
        switchFragmentView();

        mBuilder = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId());
        mBuilder
        .setCustomTitle(titleView)
        .setNegativeButton(android.R.string.cancel, getDialogInterface())
        .setPositiveButton(android.R.string.ok, getDialogInterface());
        mBuilder.setView(layoutView);
        return mBuilder.create();
    }

    private void setNaviAdapterMode(MoveToNaviAdapter naviAdapter, int mode) {
        if (naviAdapter != null) {
            naviAdapter.setMode(mode);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resizeDialog();
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().setOnKeyListener(this);
        if(mMainPageList.isShown()){
            ((AlertDialog)getDialog()).getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart(){
        super.onStart();

        if(mDeviceAdapter!=null)
            mDeviceAdapter.notifyDataSetChanged();
        if(mFileAdapter!=null)
            mFileAdapter.notifyDataSetChanged();

        if (mIndicatorFile != null && (isRootOrMountDir(mIndicatorFile.getAbsolutePath()) || SambaFileUtility.SCAN_ROOT_PATH.equals(mIndicatorFile.getAbsolutePath()))) {
            showDialogPositiveButton(false);
        } else {
            showDialogPositiveButton(true);
        }
    }

    private int dip2px(float dp) {
        final float scale = mActivity.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private void resizeDialog(){
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.6f;
        params.height = ItemOperationUtility.getScreenHeight(mActivity)*9/10;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(params);
    }

    public void addComplete() {
        ((FileManagerActivity) mActivity)
                .closeDialog(DialogType.TYPE_ADD_NEW_FOLDER_PROGRESS);
    }

    public void switchFragmentView(){
        AlertDialog dialog = (AlertDialog)getDialog();
        if(mMainPageList.isShown()){
            mMainPageList.setVisibility(View.GONE);
            mContentContainer.setVisibility(View.VISIBLE);
            if (dialog != null) {
                dialog.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
            }
        }else{
            mMainPageList.setVisibility(View.VISIBLE);
            mContentContainer.setVisibility(View.GONE);
            mAddFolderButton.setVisibility(View.GONE);
            if (dialog != null) {
                dialog.getButton(Dialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            }
        }
        //    setListShown(false);
    }//SambaFileUtility.hideSambaDeviceListView();

    private void showAddFolderButton(boolean isShow) {
        if (mAddFolderButton != null) {
            mAddFolderButton.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    public void setDeviceListAdapter() {
        if (mDeviceAdapter == null) {
            mDeviceAdapter = new DeviceListAdapter(mFileListFrament);
        }
        mContentList.setAdapter(mDeviceAdapter);
        mContentList.setOnItemClickListener(mDeviceAdapter);
        showDialogPositiveButton(false);
        showAddFolderButton(false);
    }

    public void setFileListAdapter() {
        if (mFileAdapter == null) {
            mFileAdapter = new MoveToFileListAdapter(this, null);
        }
        mContentList.setAdapter(mFileAdapter);
        mContentList.setOnItemClickListener(mFileAdapter);
        RemoteFileUtility.isShowDevicesList = false;
        SambaFileUtility.updateHostIp = false;
        showDialogPositiveButton(true);
    }

    private void showDialogPositiveButton(boolean isVisible) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button buttonPositive = dialog.getButton(Dialog.BUTTON_POSITIVE);
            if (buttonPositive != null) {
                buttonPositive.setVisibility(isVisible ? View.VISIBLE
                        : View.GONE);
            }
        }
    }

    /***add for samba begin**********/
    public void SetScanHostIndicatorPath(String showName){
        PathIndicator.setSambaHostPathIndicator(mPathContainer, showName);
    }

    /******for HomeBox device list view******************/
    public void getHomeBoxDeviceListFinish(VFile[] device){
        setListShown(true);
        setDeviceListAdapter();
        mDeviceAdapter.updateHomeBoxDeviceAdapter(device);
        ShortCutFragment.currentTokenFile=null;
        updateNofileLayout(FileListFragment.NO_DEVICES);

    }

    public void setmIndicatorFile(VFile mIndicatorFile) {
        this.mIndicatorFile = mIndicatorFile;
    }

    public VFile getIndicatorFile() {
        return mIndicatorFile;
    }


    public void setListShown(boolean isShow){
        if (isShow) {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_out));
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_in));
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_in));
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_out));
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }

    public void ScanHostPcFinish(ArrayList<SambaItem> item){
        mDeviceAdapter.updateSambaHostAdapter(item);
    }

    public ListView getListView(){
        return mContentList;
    }

    public void startScanFile(VFile file, int scanType) {
        if(file != null && file.getVFieType() != VFileType.TYPE_LOCAL_STORAGE){
            if(!((FileManagerApplication)mActivity.getApplication()).isNetworkAvailable()) {
                mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
                return;
            }
        }
        startScanFile(file, scanType, true);
    }

    public void startScanFile(VFile file, int scanType, boolean scrollToSelect) {
        Log.d(TAG, "startScanFile");
        boolean isSamePath = false;

        showAddFolderButton(false);
        setListShown(false);

        if (file == null) {
            //setNeedRefreshToken(false);
            Log.e(TAG, "startScanFile, file == null");
            return;
        }else{
            if(mIndicatorFile != null && mIndicatorFile.getAbsolutePath().equals(file.getAbsolutePath())){
                isSamePath = true;
            }
        }
        if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
            Log.d(TAG,"scan file :" + ((RemoteVFile)file).getName());
        }else{
            Log.d(TAG,"scan file path :" + file.getAbsolutePath());
        }

        mIndicatorFile = file;

        String accountNameStorageType="";
        if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
            RemoteVFile tempFile = (RemoteVFile)file;
            String accountName = tempFile.getStorageName();
            int storageType = tempFile.getStorageType();
            accountNameStorageType = accountName + "_" + storageType;
            //Log.i("!!!!!!!!!!!!!!accountName!!!!!!!!!!!!!",""+accountName);
            //Log.i("!!!!!!!!!!!!!!storageType!!!!!!!!!!!!!",""+storageType);
            //Log.i("!!!!!!!!!!!!!!accountNameStorageType!!!!!!!!!!!!!",""+accountNameStorageType);
            //Log.i("!!!!!!!!!!!!!!getAbsolutePath!!!!!!!!!!!!!",""+tempFile.getAbsolutePath());
            //Log.i("!!!!!!!!!!!!!!getFileID!!!!!!!!!!!!!",""+tempFile.getFileID());
            if(!mAccountNameStorageType.equals(accountNameStorageType)){

                if(RemoteVFile.AccountIdpathMap.containsKey(accountNameStorageType)){

                    //RemoteVFile.IdPathMap = RemoteVFile.AccountIdpathMap.get(accountName + "_" + storageType);
                }else{
                    RemoteVFile.AccountIdpathMap.put(accountNameStorageType, new HashMap<String, String>());
                }
                if(RemoteVFile.AccountPathIdMap.containsKey(accountNameStorageType)){
                    //RemoteVFile.PathIdMap = RemoteVFile.AccountPathIdMap.get(accountName + "_" + storageType);
                }else{
                    RemoteVFile.AccountPathIdMap.put(accountNameStorageType, new HashMap<String, String>());
                }
                mAccountNameStorageType = accountNameStorageType;
            }

            if((tempFile.getFromFileListItenClick()) && (tempFile.getFileID() != null) && (!tempFile.getFileID().equals(""))){
                if (tempFile.getFileID().equals("root")) {
                    if (RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get( tempFile.getAbsolutePath())!=null) {
                        tempFile.setFileID(RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get(tempFile.getAbsolutePath()));
                    }
                }else {
                    RemoteVFile.AccountIdpathMap.get(accountNameStorageType).put(tempFile.getFileID(), tempFile.getAbsolutePath());
                    RemoteVFile.AccountPathIdMap.get(accountNameStorageType).put( tempFile.getAbsolutePath(),tempFile.getFileID());
                }
            }else{
                if(tempFile.getFileID() == null || tempFile.getFileID().equals("")){
                    tempFile.setFileID(RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get(tempFile.getAbsolutePath()));
                }else {
                    RemoteVFile.AccountPathIdMap.get(accountNameStorageType).put( tempFile.getAbsolutePath(),tempFile.getFileID());
                    tempFile.setAbsolutePath(RemoteVFile.AccountIdpathMap.get(accountNameStorageType).get(tempFile.getFileID()));
                }
            }
        }
        // update indicator path
        // Google Drive case, we should update the indicator path to parent path
        if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            RemoteVFile tempVFile = (RemoteVFile)file;
            if(mIsBackEvent){
                tempVFile = ((RemoteVFile)file).getParentFile();
                tempVFile.setFileID(((RemoteVFile)file).getParentFileID());
            }
            if (RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get( tempVFile.getAbsolutePath())!=null) {
                tempVFile.setFileID(RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get(tempVFile.getAbsolutePath()));
            }
            if(!(RemoteVFile.AccountIdpathMap.get(accountNameStorageType).get(tempVFile.getFileID()) == null)){
                tempVFile.setAbsolutePath(RemoteVFile.AccountIdpathMap.get(accountNameStorageType).get(tempVFile.getFileID()));
            }
            else{
                tempVFile.setAbsolutePath(tempVFile.getAbsolutePath());
                //Log.d("jack","file path:"+tempVFile.getAbsolutePath()+" file id:"+tempVFile.getFileID()+" file name:"+file.getName());
                //Log.d("Jack", "Get absolute path failed");
            }
            PathIndicator.setMoveToPathIndicator(mPathContainer, tempVFile, mPathIndicatorClickListener);
        } else {
            PathIndicator.setMoveToPathIndicator(mPathContainer, file, mPathIndicatorClickListener);
        }

        // set default file list layout
        updateNofileLayout(FileListFragment.NO_FILE);
        RemoteFileUtility remoteFileUtility = RemoteFileUtility.getInstance(mActivity);
        // remote storage case
        if (file.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
            // update action bar
            mActivity.invalidateOptionsMenu();
            // local device has no remote file list, we should query
            if (RemoteFileUtility.getInstance(mActivity).getRemoteFileList() == null) {
                remoteFileUtility.mRemoteUpdateUI.add(new RemoteDataEntry((RemoteVFile)file, RemoteUIAction.FILE_LIST_UI));
                remoteFileUtility.sendRemoteMessage(file, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FOLDER_LIST);
                //              if(!isListRefreshing())
                //                  setListShown(false);  /**need to add refresh***/

                return;
            }
            if (remoteFileUtility.isRemoteLoadingError()) {
                updateNofileLayout(FileListFragment.NETWORK_INVALID);
            }
        } else if (file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            // update action bar
            mActivity.invalidateOptionsMenu();
            // local device has no remote file list, we should query
            //if (RemoteFileUtility.getRemoteFileList() == null) {
            if (remoteFileUtility.mRemoteFileListMap.get(file.getAbsolutePath()) == null) {

                int type = ((RemoteVFile)file).getStorageType();
                Log.d(TAG, "startScanFile to query cloud storage type: " + type);
                int msgType = -1;
                switch(type) {
                case StorageType.TYPE_GOOGLE_DRIVE:
                    // TT-692257: Check if the version of the Google Play services installed on this device is not authentic.
                    int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mActivity);
                    if (status == ConnectionResult.SERVICE_INVALID) {
                        mActivity.displayDialog(DialogType.TYPE_GMS_ALERT_DIALOG, status);
                        SambaFileUtility.getInstance(getActivity()).hideSambaDeviceListView();
                        dismissAllowingStateLoss();
                        return;
                    }
                    msgType = MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
                    break;
                case StorageType.TYPE_DROPBOX:
                    msgType = MsgObj.TYPE_DROPBOX_STORAGE;
                    break;
                case StorageType.TYPE_BAIDUPCS:
                    msgType = MsgObj.TYPE_BAIDUPCS_STORAGE;
                    break;
                case StorageType.TYPE_SKYDRIVE:
                    msgType = MsgObj.TYPE_SKYDRIVE_STORAGE;
                    break;
                case StorageType.TYPE_ASUSWEBSTORAGE:
                    msgType = MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE;
                    break;
                case StorageType.TYPE_HOME_CLOUD:
                    msgType = MsgObj.TYPE_HOMECLOUD_STORAGE;
                    break;
                case StorageType.TYPE_YANDEX:
                    msgType = 9;
                    break;
                default:
                    Log.w(TAG, "you should define remote storage type");
                }

                if (mIsBackEvent) {
                    mIsBackEvent = false;
                    RemoteVFile tempVFile = ((RemoteVFile)file).getParentFile();
                    tempVFile.setFileID(RemoteVFile.AccountPathIdMap.get(accountNameStorageType).get(tempVFile.getAbsolutePath()));
                    String key = tempVFile.getAbsolutePath();
                    key = key.equals("")?File.separator+tempVFile.getStorageName():key;
                    key =  getmRemoteUpdateUIMapKey(key);
                    ///RemoteFileUtility.mRemoteUpdateUIMap.put(msgType+key, new RemoteDataEntry((RemoteVFile)file, RemoteUIAction.FILE_LIST_UI));
                    remoteFileUtility.mRemoteUpdateUIMap.put(msgType+key, new RemoteDataEntry(tempVFile, RemoteUIAction.FILE_LIST_UI));

                    //RemoteFileUtility.mRemoteUpdateUIMap.put(msgType+key,new RemoteDataEntry(tempVFile, RemoteUIAction.FILE_LIST_UI));
                    remoteFileUtility.sendCloudStorageMsg(((RemoteVFile)tempVFile).getStorageName(),
                            tempVFile, null, msgType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FOLDER_LIST,ListArgument.FolderList);
                } else {
                    // RemoteFileUtility.mRemoteUpdateUI.add(new RemoteDataEntry((RemoteVFile)file, RemoteUIAction.FILE_LIST_UI));
                    RemoteVFile tempRemoteVfile = (RemoteVFile)file;
                    String key = tempRemoteVfile.getAbsolutePath();
                    key = key.equals("")?File.separator+tempRemoteVfile.getStorageName():key;
                    key =  getmRemoteUpdateUIMapKey(key);
                    remoteFileUtility.mRemoteUpdateUIMap.put(msgType+key, new RemoteDataEntry((RemoteVFile)file, RemoteUIAction.FILE_LIST_UI));
                    int action = remoteFileUtility.getListUIAction();
                    String remoteAction = ListArgument.FolderList;
                    if (action != -1) {
                        switch(action) {
                        case StorageType.TYPE_STARRED_TITLE:
                            remoteAction = ListArgument.StartedList;
                            break;
                        case StorageType.TYPE_RECENTLY_USED_TITLE:
                            remoteAction = ListArgument.Recent;
                            break;
                        case StorageType.TYPE_TRASH_CAN_TITLE:
                            remoteAction = ListArgument.TrashCan;
                            break;
                        }
                    }

                    remoteFileUtility.sendCloudStorageMsg(((RemoteVFile)file).getStorageName(),
                            file, null, msgType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FOLDER_LIST, remoteAction);
                }

                // get cloud storage usage
                String accountName = ((RemoteVFile)file).getStorageName();

                return;
            }
            if(remoteFileUtility.isHomeCloudFileListError()){
                updateNofileLayout(FileListFragment.HOMECLOUD_ACCESS_EEROR);
            }else if (remoteFileUtility.isRemoteLoadingError()) {
                if(remoteFileUtility.isRemoteAccessPermisionDeny()){
                    updateNofileLayout(FileListFragment.PERMISION_DENY);
                }else{
                    updateNofileLayout(FileListFragment.NETWORK_INVALID);
                }
            }
        }else if (file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE){

        }

        // update cloud storage file id
        if (mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            ((RemoteVFile)mIndicatorFile).setFileID(((RemoteVFile)file).getFileID());
        }else if (mIndicatorFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {

        }

        PathIndicator.setMoveToPathIndicator(mPathContainer, file, mPathIndicatorClickListener);

        // when in mount or root folder, we will try to hide the add-folder
        // button
        if (isRootOrMountDir(mIndicatorFile.getAbsolutePath()) || mIndicatorFile.getAbsolutePath().equals(SambaFileUtility.getInstance(getActivity()).getRootScanPath())
                || mIndicatorFile.getAbsolutePath().equals(remoteFileUtility.getHomeCloudRootPath())){

            mActivity.isRootDir(true);
        }else{
            mActivity.isRootDir(false);
        }

        boolean isHideSystemFile = FileUtility.getIsHideSystemFile(mActivity);

        Bundle args = new Bundle();
        args.putString(KEY_SCAN_PATH, file.getAbsolutePath());
        args.putInt(KEY_SCAN_TYPE, scanType);
        args.putInt(KEY_SORT_TYPE, mSortType);
        args.putInt(KEY_VFILE_TYPE, file.getVFieType());
        args.putBoolean(KEY_HIDDEN_TYPE, isHideSystemFile);
        args.putStringArray(KEY_FILE_FILTER, mFileFilter);

        //keep checked items

        ShortCutFragment.currentTokenFile = null;

        LoaderManager manager = getLoaderManager();
        Loader<VFile[]> mFileLoader = manager.getLoader(SCAN_FILE_LOADER);

        Log.d(TAG,"===mFileLoader===" + mFileLoader + "===is same path =" + isSamePath);

        if(mFileLoader == null){
            manager.initLoader(SCAN_FILE_LOADER, args, this);
        }else{
            if(!isSamePath){
                mFileLoader.startLoading();
                manager.restartLoader(SCAN_FILE_LOADER, args, this);
            }
        }

        if (mIndicatorFile != null && isRootOrMountDir(mIndicatorFile.getAbsolutePath())) {
            showDialogPositiveButton(false);
        } else {
            showDialogPositiveButton(true);
        }
    }

    public class MoveToPathIndicatorClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // ---
            if(v.getTag() instanceof RemoteVFile){
                RemoteVFile tempVfile = (RemoteVFile)(v.getTag());
                if (tempVfile.getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    if((tempVfile.getName()==null||tempVfile.getName().equals(""))&&tempVfile.getAbsolutePath().equals(File.separator+tempVfile.getStorageName())){
                        RemoteFileUtility.getInstance(mActivity).sendCloudStorageMsg(tempVfile.getStorageName(), null, null, tempVfile.getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                        SetScanHostIndicatorPath(tempVfile.getStorageName());
                        return;
                    }
                }
            }
            if((v.getTag() instanceof VFile) && mIndicatorFile != null){
                startScanFile((VFile) v.getTag(), ScanType.SCAN_CHILD);
            }
        }

    }

    public void updateNofileLayout(int state) {
        switch (state) {
        case FileListFragment.NO_FILE:
            Resources res = getResources();
            String text = String.format(res.getString(R.string.select_folder_by_ok_button), res.getString(android.R.string.ok));
            setEmptyText(text);
            break;
        case FileListFragment.NETWORK_INVALID:
            setEmptyText(getText(R.string.remote_connected_error_hint));
            break;
        case FileListFragment.NO_DEVICES:
            setEmptyText(getText(R.string.cloud_homebox_no_available_devices));
            break;
        case FileListFragment.TOKEN_INVALIDATED:
            setEmptyText(getText(R.string.cloud_token_invalidate));
            break;
        case FileListFragment.PERMISION_DENY:
            setEmptyText(getText(R.string.permission_deny));
            break;

        case FileListFragment.ACCOUNT_INVALID:
            setEmptyText(getText(R.string.invalid_account));
            break;
        case FileListFragment.HOMECLOUD_ACCESS_EEROR:
            setEmptyText(getText(R.string.homecloud_access_error));
            break;
        }


    }

    public void setEmptyText(CharSequence text) {
        if (mStandardEmptyView == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        mStandardEmptyView.setText(text);
        if (mEmptyText == null) {
            mContentList.setEmptyView(mStandardEmptyView);
        }
        mEmptyText = text;
    }

    private String getmRemoteUpdateUIMapKey(String key){
        return key.endsWith("/")?key.substring(0,key.length()-1):key;
    }

    private static boolean isRootOrMountDir(String path) {
        return TextUtils.isEmpty(path)
                || path.equals(File.separator)
                || path.equals(WrapEnvironment.getEpadExternalStorageDirectory().getPath());
    }

    @Override
    public Loader<VFile[]> onCreateLoader(int id, Bundle args) {
        String scanPath = args.getString(KEY_SCAN_PATH);
        int scanType = args.getInt(KEY_SCAN_TYPE);
        int sortType = args.getInt(KEY_SORT_TYPE);
        int vfileType = args.getInt(KEY_VFILE_TYPE);
        boolean showHidden = args.getBoolean(KEY_HIDDEN_TYPE);
        EditPool checkPool = (EditPool) args.getSerializable(KEY_CHECK_POOL);

        String[] filter = args.getStringArray(KEY_FILE_FILTER);
        return new ScanFileLoader(mActivity, scanPath, scanType, sortType, vfileType, showHidden, checkPool, filter);
    }

    @Override
    public void onLoadFinished(Loader<VFile[]> loader, VFile[] data) {

        LoaderManager mManager = getLoaderManager();
        int id = loader.getId();
        mManager.destroyLoader(id);

        if(isRootFolder(mIndicatorFile) && data != null){
            ArrayList<VFile> dataList = new ArrayList<VFile>();

            for(VFile vfile: data){
                if(!vfile.getName().equals("APD") || !vfile.isDirectory()){
                    dataList.add(vfile);
                }
            }
            data = dataList.toArray(new VFile[dataList.size()]);
        }

        if(mIndicatorFile.getPath().equals("/Removable") && data != null){
            ArrayList<VFile> dataList = new ArrayList<VFile>();
            for(VFile vfile: data){
                if(!vfile.getName().equals("sdcard0") && !vfile.getName().equals("emulated") ){
                    dataList.add(vfile);
                }
            }
            data = dataList.toArray(new VFile[dataList.size()]);
        }

        // Only keep folder in MoveTo Dialog
        if (data != null) {
            ArrayList<VFile> dataList = new ArrayList<VFile>();
            for (VFile vFile : data) {
                if (vFile.isDirectory()) {
                    dataList.add(vFile);
                }
            }
            data = dataList.toArray(new VFile[dataList.size()]);
        }

        if(mContentList.getAdapter() != null && mContentList.getAdapter() instanceof DeviceListAdapter){
            setFileListAdapter();// add for samba
        }

        mFileAdapter.updateAdapter(data, false, mSortType);
        showAddFolderButton(true);
        updateAdapterResult();
    }

    private void updateAdapterResult() {
        Thread scanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mFileAdapter.updateAdapterResult();
                Handler handler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch(msg.what) {
                            case FileListFragment.MSG_LOAD_FINISHED_COMPLETE:
                                setListShown(true);
                        }
                    }
                };
                handler.sendMessage(handler.obtainMessage(FileListFragment.MSG_LOAD_FINISHED_COMPLETE));
            }
        });
        scanThread.start();
    }

    protected static boolean isRootFolder(VFile f) {
        return (f != null) && (f.getAbsolutePath().equals("/")) ||
                // consider remote storage path: /deviceName/
                ((f.getVFieType() == VFileType.TYPE_REMOTE_STORAGE || f.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) && (((RemoteVFile)f).getPath().equals("/" + ((RemoteVFile)f).getStorageName()) || ((RemoteVFile)f).getPath().equals("/")));
    }

    @Override
    public void onLoaderReset(Loader<VFile[]> arg0) {
        // TODO Auto-generated method stub
    }

    private void onBackPress(){

        //when on device view for samba
        if(SambaFileUtility.updateHostIp||RemoteFileUtility.isShowDevicesList){
            RemoteFileUtility.isShowDevicesList = false;
            SambaFileUtility.updateHostIp = false;
            switchFragmentView();
            return;
        }

        // handle cloud storage case
        if (mIndicatorFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
            // handle root folder case
            if (((RemoteVFile)mIndicatorFile).getName().equals("")) {
                switchFragmentView();
                return;
            } else {
                // update back button event, when using file id to get the list
                switch(((RemoteVFile)mIndicatorFile).getStorageType()) {
                case StorageType.TYPE_ASUSWEBSTORAGE:
                case StorageType.TYPE_SKYDRIVE:
                case StorageType.TYPE_GOOGLE_DRIVE:
                case StorageType.TYPE_HOME_CLOUD:
                    RemoteVFile tempHomeFile = (RemoteVFile)mIndicatorFile;
                    if(tempHomeFile.getStorageType()==StorageType.TYPE_HOME_CLOUD){
                        RemoteVFile tempParent = tempHomeFile.getParentFile();
                        if ((tempParent.getName()==null||tempParent.getName().equals(""))&&tempParent.getAbsolutePath().equals(File.separator+tempParent.getStorageName())) {
                            setListShown(false);
                            RemoteFileUtility.getInstance(mActivity).sendCloudStorageMsg(tempHomeFile.getStorageName(), null, null, tempHomeFile.getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                            SetScanHostIndicatorPath(tempParent.getStorageName());
                            return;
                        }
                    }
                    mIsBackEvent = true;
                    startScanFile(mIndicatorFile , ScanType.SCAN_CHILD);
                    break;
                case StorageType.TYPE_DROPBOX:
                case StorageType.TYPE_YANDEX:
                case StorageType.TYPE_BAIDUPCS:
                    if (mIndicatorFile.getAbsoluteFile().equals("/"+ ((RemoteVFile)mIndicatorFile).getStorageName() + "/") ||
                            mIndicatorFile.getName().equals("")) {
                        break;
                    }
                    startScanFile(mIndicatorFile.getParentFile() , ScanType.SCAN_CHILD);
                    break;
                }
            }
            return;
        }else if (mIndicatorFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE){
            //handle samba storage case
            SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(getActivity());
            if (sambaFileUtility.getRootScanPath().equals(mIndicatorFile.getAbsolutePath())){
                sambaFileUtility.startScanNetWorkDevice(false);
                return;
            }
            String indicatorPath = ((SambaVFile)mIndicatorFile).getIndicatorPath();
            if (indicatorPath != null){
                String[] folderStrs = indicatorPath.split(String.valueOf(File.separatorChar));
                if (folderStrs != null){
                    int smbcount = folderStrs.length;
                    String tmp = "";
                    for (int i=1;i < smbcount - 1;i++){
                        tmp += (folderStrs[i] + File.separatorChar);
                    }
                    if (!TextUtils.isEmpty(tmp)){
                        tmp.trim().substring(1);
                        VFile tempVFile = new SambaVFile(sambaFileUtility.getRootScanPath() + tmp);
                        startScanFile(tempVFile , ScanType.SCAN_CHILD);
                    }else{
                        VFile tempVFile = new SambaVFile(sambaFileUtility.getRootScanPath());
                        startScanFile(tempVFile , ScanType.SCAN_CHILD);
                    }
                }
            }
            return;
        }

        if (!isRootFolder(mIndicatorFile)) {
            startScanFile(mIndicatorFile.getParentFile() , ScanType.SCAN_CHILD);
        }else{
            switchFragmentView();
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
        case R.id.path_home:
            switchFragmentView();
            break;
        case R.id.add_folder_action:

            boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(mIndicatorFile.getAbsolutePath());
            if (bNeedWriteToAppFolder) {
                WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                    .newInstance();
                warnDialog.show(mActivity.getFragmentManager(),
                    "WarnKKSDPermissionDialogFragment");
                break;
            }
            if (SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(mIndicatorFile.getAbsolutePath())) {
                mActivity.callSafChoose(SafOperationUtility.ACTION_MKDIR);
                break;
            }
            GaMoveToDialog.getInstance()
                    .sendEvents(mActivity, GaMoveToDialog.CATEGORY_NAME,
                            GaMoveToDialog.ACTION_ADD_FOLDER, null, null);
            showDialog(DialogType.TYPE_ADD_NEW_FOLDER, mIndicatorFile);
        default:
            break;
        }
    }

    public void showDialog(int type, Object arg) {
        if (mActivity != null)
            mActivity.displayDialog(type, arg);
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        switch(button){
        case Dialog.BUTTON_POSITIVE:
            ItemOperationUtility itemOperationUtility = ItemOperationUtility.getInstance();
            // Tim
            // Crash issue found from Firebase console: Add null case to prevent exception
            if(mIndicatorFile == null) return;
            itemOperationUtility.setMoveToLastFileType(mActivity, mIndicatorFile.getVFieType());
            if (mIndicatorFile instanceof RemoteVFile) {
                // Cloud Storage case
                String type = String.valueOf(((RemoteVFile) mIndicatorFile).getMsgObjType());
                String storageName = ((RemoteVFile) mIndicatorFile).getStorageName();
                itemOperationUtility.setMoveToLastPath(mActivity, type);
                itemOperationUtility.setMoveToLastAccountName(mActivity, storageName);
            } else {
                // Local & Samba Storage case
                itemOperationUtility.setMoveToLastPath(mActivity, mIndicatorFile.getPath());
                itemOperationUtility.setMoveToLastAccountName(mActivity, null);
            }
            if (mActivity.getCurrentFragment() instanceof  OnFolderSelectListener) {
                ((OnFolderSelectListener)mActivity.getCurrentFragment()).onFolderSelected(mIndicatorFile, getArguments());
            }
            else {
                mFileListFrament.onFolderSelected(mIndicatorFile, getArguments());
            }
            break;
        case Dialog.BUTTON_NEGATIVE:
            SambaFileUtility.getInstance(getActivity()).hideSambaDeviceListView();
            dismissAllowingStateLoss();
            break;
        default:
            break;
        }

    }

    @Override
    public boolean onKey(DialogInterface dialog, int keycode, KeyEvent event) {
        // TODO Auto-generated method stub
        if(event.getAction() == KeyEvent.ACTION_UP && keycode == KeyEvent.KEYCODE_BACK){
            if(mMainPageList != null && mMainPageList.isShown()){
                return false;
            }
            onBackPress();
            return true;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeDialog();
    }

}
