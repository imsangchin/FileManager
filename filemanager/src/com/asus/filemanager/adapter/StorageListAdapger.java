package com.asus.filemanager.adapter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.design.widget.NavigationView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.AddCloudAccountActivity;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerActivity.FragmentType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.SearchResultFragment;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.ga.GaBrowseFile;
import com.asus.filemanager.ga.GaCloudStorage;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.VolumeInfoUtility;
import com.asus.filemanager.utility.permission.PermissionManager;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.AsusAccountHelper;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.remote.utility.RemoteAccountUtility.AccountInfo;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.MsgObj;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

//import com.asus.filemanager.adapter.FolderTreeAdapter.StorageElement;

public class StorageListAdapger {
    private static final String TAG = "StorageListAdapger";

    private static final int STORAGETYPE_TITLE = 0;
    public static final int STORAGETYPE_LOCAL = 1;
    private static final int STORAGETYPE_CLOUD = 2;
    private static final int STORAGETYPE_ADD_GOOGLE_ACCOUNT = 3;
    private static final int STORAGETYPE_HOME = 4;
    public static final int STORAGETYPE_NETWORK = 5;
    private static final int STORAGETYPE_ADD_CLOUD_ACCOUNT = 6;
    private static final int STORAGETYPE_CLOUD_VERIZON = 7;

    public static final int INTERNAL_STORAGE = 0;
//    public static final int MICROSD = 1;
//    public static final int USBDISK1 = 2;
//    public static final int USBDISK2 = 3;
//    public static final int SDREADER = 4;
//    public static final int USBDISK3 = 5;
//    public static final int USBDISK4 = 6;
//    public static final int USBDISK5 = 7;

    public static class StorageItemElement {
        /**** Local storage ****/
        public Object storageVolume;
        public VFile vFile;
        /**** Local storage ****/

        public String storageTitle;

        public AccountInfo acountInfo;
        public int storageType;
        public int cloudStorageIndex;
    }

    private LinkedList<StorageItemElement> mStorageItemElementList = new LinkedList<StorageItemElement>();
    private ArrayList<Object> mLocalStorageElementList = new ArrayList<Object>();
    private ArrayList<VFile> mLocalStorageFile = new ArrayList<VFile>();
    private static ArrayList<AccountInfo> mConnectedCloudStorageAccountList;

//    private String[] mStorageTitle;
    private String[] mNetwork_StorageTitle;
    private String[] mCloud_StorageTitle;

    private NavigationView mNavigationView;

//    private TypedArray mStorageDrawable;
    // private TypedArray mCloudStorageDrawable;
    private Drawable[] mNetworkStorageDrawable;
    private Drawable[] mCloudStorageDrawable;
    private FileManagerActivity mActivity;
    private String mSelectedStorage;
    private VFile mSelectedLocalStorageFile;
    private boolean isFromClickDrawerClosed = false;
    public boolean mIsAttachOp = false;
    public boolean mIsMultipleSelectionOp = false;
    private int selectId = 0;

    public boolean isFromClickDrawerClosed() {
        return isFromClickDrawerClosed;
    }

    public void setFromClickDrawerClosed(boolean isFromClickDrawerClosed) {
        this.isFromClickDrawerClosed = isFromClickDrawerClosed;
    }

    public Drawable[] getDrawablesFromTypeArray(TypedArray array) {
        int total = array.length();
        Drawable[] drawables = new Drawable[total];
        for (int i = 0; i < total; i++) {
            drawables[i] = array.getDrawable(i);
        }
        return drawables;
    }

    public void updateAvailableCloudsTitle(String[] titles,
            Drawable[] cloudDrawables) {
        this.mCloud_StorageTitle = titles;
        this.mCloudStorageDrawable = cloudDrawables;
        updateStorageItemElementList();
    }

    public void setSelectedStorage(String selectedStorage, VFile vFile) {
        fixSelectId(selectedStorage,vFile);
        mSelectedStorage = selectedStorage;
        mSelectedLocalStorageFile = vFile;
        updateNavigationViewContent();
    }

    private void fixSelectId(String selectedStorage, VFile vFile)
    {
        //select local storage file
        if(vFile!=null) {
            //Home(0) LocalTitle(1) LocalFile(2..)
            if(mLocalStorageFile.contains(vFile));
                selectId = 2 + mLocalStorageFile.lastIndexOf(vFile);
        }else{
            //return to home fragment
            if (mActivity.getString(R.string.file_manager).equals(selectedStorage)){
                selectId = 0;
                return;
            }
            else {
                if(selectId ==0) {
                    //not homeFragment but attach on homeFragment (category, recycle), deselect all item
                    selectId = -1;
                    return;
                }
            }
            //return to internal storage
            if(mActivity.getString(R.string.internal_storage_title).equals(selectedStorage))
            {
                //Home(0) LocalTitle(1) LocalFile(2..)
                selectId = 2;
            }
        }
    }

    // ++ tim_hu
    public String getSeclectedStorage() {
        return mSelectedStorage;
    }

    public StorageListAdapger(FileManagerActivity activity) {
        mActivity = activity;
//        mStorageTitle = activity.getResources().getStringArray(
//                R.array.storage_title);
//        mStorageDrawable = activity.getResources().obtainTypedArray(
//                R.array.storage_icon);
        mNetwork_StorageTitle = activity.getResources().getStringArray(
                R.array.network_storage_title);
        mCloud_StorageTitle = activity.getResources().getStringArray(
                R.array.cloud_storage_title);
        // mCloudStorageDrawable =
        // activity.getResources().obtainTypedArray(R.array.cloud_storage_icon);
        mNetworkStorageDrawable = getDrawablesFromTypeArray(activity
                .getResources().obtainTypedArray(R.array.network_storage_icon));
        mNetworkStorageDrawable[0] = activity.getResources().getDrawable(
                R.drawable.asus_ic_network_place);
        mCloudStorageDrawable = getDrawablesFromTypeArray(activity
                .getResources().obtainTypedArray(R.array.cloud_storage_icon));

        mConnectedCloudStorageAccountList = new ArrayList<AccountInfo>();

        final StorageManager mStorageManager = (StorageManager) activity
                .getSystemService(Context.STORAGE_SERVICE);
        ArrayList<Object> storageVolume;

        storageVolume = ((FileManagerApplication) activity.getApplication())
                .getStorageVolume();
        VFile[] tmpVFiles = ((FileManagerApplication) mActivity
                .getApplication()).getStorageFile();
        for (int i = 0; i < storageVolume.size(); i++) {
            if (mStorageManager != null
                    && reflectionApis
                            .getVolumeState(mStorageManager, storageVolume.get(i)).equals(
                                    Environment.MEDIA_MOUNTED)) {
                mLocalStorageElementList.add(storageVolume.get(i));
                mLocalStorageFile.add(tmpVFiles[i]);
            }

        }

        if (mActivity.getIntent().getAction() != null
                && mActivity.getIntent().getAction()
                        .equals(Intent.ACTION_GET_CONTENT)) {
            mIsAttachOp = true;
        }

        if (mActivity.getIntent().getAction() != null
                && mActivity.getIntent().getAction()
                        .equals(FileManagerActivity.ACTION_MULTIPLE_SELECTION)) {
            mIsMultipleSelectionOp = true;
        }

    }

    public void setNavigationView(NavigationView navigationView) {
        Log.d(TAG, "setNavigationView");
        mNavigationView = navigationView;
        mNavigationView.setItemIconTintList(null);
        //setting up selected item listener
        mNavigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem menuItem) {
                    int id = menuItem.getItemId();
                    Log.d(TAG, "onNavigationItemSelected id=" + id);
                    if (id < 0) {
                        Log.e(TAG, "invalid menu item id");
                        return false;
                    }
                    //reset isFromStorageAnalyzer flag
                    mActivity.isFromStorageAnalyzer = false;

                    StorageItemElement item = mStorageItemElementList.get(id);
                    mSelectedStorage = item.storageTitle;

                    if(isSelectableItem(mActivity,mSelectedStorage))
                        selectId = id;

                    // FIXME:
                    // workaround for navigation view setChecked not work issue
                    updateNavigationViewContent();

                    /********** add for GoBackToSelectItem **********/
                    ItemOperationUtility.getInstance().resetScrollPositionList();

                    // setItemBackgroundAndFont(view, true);
                    FileListFragment fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
                    fileListFragment.updateCloudStorageUsage(false, 0, 0);
                    fileListFragment.finishEditMode();
                    SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(mActivity);
                    if (item.storageType == STORAGETYPE_HOME) {
                        mSelectedLocalStorageFile = null;
                        sambaFileUtility.hideSambaDeviceListView();
                        sambaFileUtility.stopOnlinePlayServer();// stop Http Server
                        mActivity.showSearchFragment(FileManagerActivity.FragmentType.HOME_PAGE, true);
                        GaBrowseFile.getInstance().sendEvents(mActivity, GaBrowseFile.CATEGORY_NAME,
                                GaBrowseFile.ACTION_BROWSE_FROM_DRAWER, "HOME", null);
                    } else if (item.storageType == STORAGETYPE_LOCAL) {
                        mSelectedLocalStorageFile = item.vFile;
                        sambaFileUtility.hideSambaDeviceListView();
                        startScanFile(item.vFile);
                        mActivity.setActionBarTitle(item.storageTitle);
                        sambaFileUtility.stopOnlinePlayServer();// stop Http Server
                        GaBrowseFile.getInstance().sendEvents(mActivity, GaBrowseFile.CATEGORY_NAME,GaBrowseFile.ACTION_BROWSE_FROM_DRAWER, item.vFile.getName(), null);
                    } else if (item.storageType == STORAGETYPE_NETWORK) {
                        mSelectedLocalStorageFile = null;
                        if (!((FileManagerApplication) mActivity.getApplication())
                                .isWifiConnected()) {
                            mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG,
                                    VFileType.TYPE_CLOUD_STORAGE);
                            // isFromClickDrawerClosed = true;
                            // mActivity.closeStorageDrawerList();
                            return false;
                        }

                        if (!ItemOperationUtility.getInstance().checkCtaPermission(
                                mActivity)) {
                            // ToastUtility.show(mActivity,
                            // mActivity.getResources().getString(R.string.network_cta_hint));
                            return false;
                        }

                        sambaFileUtility.startOnlinePlayServer();// start Http Server
                        fileListFragment.enablePullReFresh(true);
                        sambaFileUtility.startScanNetWorkDevice(false);
                        GaBrowseFile.getInstance().sendEvents(mActivity, GaBrowseFile.CATEGORY_NAME,GaBrowseFile.ACTION_BROWSE_FROM_DRAWER, GaBrowseFile.LABEL_NETWORK_PLACE, null);
                        GaCloudStorage.getInstance().sendEvents(mActivity, GaCloudStorage.CATEGORY_NAME,
                                GaCloudStorage.ACTION_OPEN_NETWORK_PLACE, null, null);
                    } else if (item.storageType == STORAGETYPE_CLOUD) {
                        mSelectedLocalStorageFile = null;
                        if (!((FileManagerApplication) mActivity.getApplication())
                                .isNetworkAvailable()) {
                            mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG,
                                    VFileType.TYPE_CLOUD_STORAGE);
                            // isFromClickDrawerClosed = true;
                            // mActivity.closeStorageDrawerList();
                            return false;
                        }

                        if (!ItemOperationUtility.getInstance().checkCtaPermission(
                                mActivity)) {
                            // ToastUtility.show(mActivity,
                            // mActivity.getResources().getString(R.string.network_cta_hint));
                            return false;
                        }

                        boolean isMounted = (item.acountInfo != null);
                        int msgObjType = RemoteAccountUtility.getInstance(mActivity).findMsgObjTypeByCloudTitle(mActivity, item.storageTitle);
                        int storageType = RemoteAccountUtility.getInstance(mActivity).findStorageTypeByCloudTitle(mActivity, item.storageTitle);
                        String gaBrowseLabel = findGaBrowseFileCloudLableByMsgObjType(msgObjType);
                        // TT-692257: Check if the version of the Google Play services installed on this device is not authentic.
                        if (MsgObj.TYPE_GOOGLE_DRIVE_STORAGE == msgObjType) {
                            int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mActivity);
                            if (status == ConnectionResult.SERVICE_INVALID) {
                            mActivity.displayDialog(DialogType.TYPE_GMS_ALERT_DIALOG, status);
                                return false;
                            }
                        }
                        if (gaBrowseLabel != null) {
                            if (isMounted) {
                                sambaFileUtility.hideSambaDeviceListView();
                                openCloudStorage(item, storageType);
                            } else {
                                RemoteAccountUtility.getInstance(mActivity).addAccount( msgObjType);
                            }
                            GaBrowseFile.getInstance().sendEvents(mActivity, GaBrowseFile.CATEGORY_NAME,
                                    GaBrowseFile.ACTION_BROWSE_FROM_DRAWER, gaBrowseLabel, null);
                        } else {
                            Log.e(TAG, "invalid GaBrowseFile cloud lable");
                            return false;
                        }
                    } else if (item.storageType == STORAGETYPE_ADD_CLOUD_ACCOUNT) {
                        if (!ItemOperationUtility.getInstance().checkCtaPermission(
                                mActivity)) {
                            ToastUtility.show(mActivity, mActivity.getResources()
                                    .getString(R.string.network_cta_hint));
                            return false;
                        }
                        if (!((FileManagerApplication) mActivity.getApplication())
                                .isNetworkAvailable()) {
                            mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG,
                                    VFileType.TYPE_CLOUD_STORAGE);
                        } else {
                            mActivity.startActivity(new Intent(mActivity, AddCloudAccountActivity.class));
                        }

                    } else if (item.storageType == STORAGETYPE_CLOUD_VERIZON) {
                        Intent actionVerizonFile = WrapEnvironment.getVerizonActionFileIntent(mActivity);
                        if(actionVerizonFile!=null)
                            mActivity.startActivity(actionVerizonFile);
                    }

                    isFromClickDrawerClosed = true;
                    mActivity.closeStorageDrawerList();
                    return true;
                }
            }
        );
    }

    private static String findGaBrowseFileCloudLableByMsgObjType(int msgObjType) {
        switch (msgObjType) {
        case MsgObj.TYPE_HOMECLOUD_STORAGE:
            return GaBrowseFile.LABEL_ASUS_HOMECLOUD;
        case MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE:
            return GaBrowseFile.LABEL_ASUS_WEBSTORAGE;
        case MsgObj.TYPE_DROPBOX_STORAGE:
            return GaBrowseFile.LABEL_DROPBOX;
        case MsgObj.TYPE_BAIDUPCS_STORAGE:
            return GaBrowseFile.LABEL_BAIDU;
        case MsgObj.TYPE_GOOGLE_DRIVE_STORAGE:
            return GaBrowseFile.LABEL_GOOGLE_DRIVE;
        case MsgObj.TYPE_SKYDRIVE_STORAGE:
            return GaBrowseFile.LABEL_ONEDRIVE;
        case MsgObj.TYPE_YANDEX_STORAGE:
            return GaBrowseFile.LABEL_YANDEX;
        }
        return null;
    }

    private void openCloudStorage(StorageItemElement item, int storageType) {
        Log.d(TAG, "Open Cloud " + item.storageTitle);

        GaCloudStorage.getInstance().sendEvents(mActivity, GaCloudStorage.CATEGORY_NAME,
                item.storageTitle, null, null);

        // if (itemIcon.getAccountInfo() != null) {
        // Log.d(TAG, "AccountName = " +
        // itemIcon.getAccountInfo().getAccountName());
        // }
        SearchResultFragment searchResultFragment = (SearchResultFragment) mActivity
                .getFragmentManager().findFragmentById(R.id.searchlist);
        if (searchResultFragment != null && searchResultFragment.isVisible()
                && searchResultFragment.getSearchItem() != null) {
            Log.d(TAG, "searchResultFragment collapseActionView");
            searchResultFragment.getSearchItem().collapseActionView();
        }
        AccountInfo accountInfo = item.acountInfo;


        if (null != accountInfo && accountInfo.getAccountType().compareTo(AsusAccountHelper.ASUS_GOOGLE_ACCOUNT_TYPE)== 0){
            RemoteAccountUtility.getInstance(mActivity).getGDriveStorageUsage(mActivity.getApplicationContext(),0);
        }
        RemoteVFile cloudRootVFile = new RemoteVFile("/"
                + accountInfo.getAccountName(), VFileType.TYPE_CLOUD_STORAGE,
                accountInfo.getAccountName(), storageType, "");
        cloudRootVFile.setStorageName(accountInfo.getAccountName());
        cloudRootVFile.setFileID("root");
        cloudRootVFile.setFromFileListItenClick(true);
        if (!RemoteAccountUtility.getInstance(mActivity).validateToken(cloudRootVFile)) {
            Log.d(TAG, "valideToken failed, to get token");
            ShortCutFragment.currentTokenFile = cloudRootVFile;
            if (cloudRootVFile instanceof RemoteVFile
                    && (cloudRootVFile.getStorageType() == StorageType.TYPE_HOME_CLOUD)) {
                FileListFragment fileListFragment = (FileListFragment) mActivity
                        .getFragmentManager().findFragmentById(R.id.filelist);
                if (fileListFragment != null) {
                    fileListFragment.setRemoteVFile(cloudRootVFile);
                }
            }
            RemoteAccountUtility.getInstance(mActivity).getToken(cloudRootVFile);
            return;
        } else {
            ShortCutFragment.currentTokenFile = null;
            if (cloudRootVFile instanceof RemoteVFile
                    && (cloudRootVFile.getStorageType() == StorageType.TYPE_HOME_CLOUD)) {
                FileListFragment fileListFragment = (FileListFragment) mActivity
                        .getFragmentManager().findFragmentById(R.id.filelist);
                if (fileListFragment != null) {
                    fileListFragment.enablePullReFresh(true);
                    fileListFragment.setRemoteVFile(cloudRootVFile);
                    // Dont't erase the last path (homepage)
                    // fileListFragment.setmIndicatorFile(cloudRootVFile);
                    fileListFragment.setListShown(false);
                    fileListFragment.SetScanHostIndicatorPath(cloudRootVFile
                            .getStorageName());
                    RemoteFileUtility.getInstance(mActivity)
                            .sendCloudStorageMsg(
                                    (cloudRootVFile).getStorageName(),
                                    null,
                                    null,
                                    (cloudRootVFile).getMsgObjType(),
                                    CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                }
                if (item.acountInfo != null) {
                    mSelectedStorage = item.storageTitle + item.acountInfo.getAccountName();
                } else {
                    mSelectedStorage = item.storageTitle;
                }
                mActivity.setActionBarTitle(item.storageTitle);
                return ;
            }
        }
        startScanFile(cloudRootVFile, true);

        if (item.acountInfo != null) {
            mSelectedStorage = item.storageTitle + item.acountInfo.getAccountName();
        } else {
            mSelectedStorage = item.storageTitle;
        }
        mActivity.setActionBarTitle(item.storageTitle);
    }

    private void startScanFile(VFile file, boolean isNeedRefreshToken) {
        startScanFile(file);
        /*
         * Log.d(TAG, "startScanFile"); if (file != null) { if (!file.exists())
         * { return; } FileListFragment fragment = (FileListFragment)
         * mActivity.getFragmentManager().findFragmentById(R.id.filelist); if
         * (fragment != null) {
         * mActivity.showSearchFragment(FragmentType.NORMAL_SEARCH, false);
         * //fragment.setNeedRefreshToken(isNeedRefreshToken);
         * fragment.startScanFile(file, ScanType.SCAN_CHILD , false); } }
         */
    }

    public void updateLocalStorageList(ArrayList<Object> storageVolume) {
        final StorageManager mStorageManager = (StorageManager) mActivity
                .getSystemService(Context.STORAGE_SERVICE);
        mLocalStorageElementList.clear();
        mLocalStorageFile.clear();
        VFile[] tmpVFiles = ((FileManagerApplication) mActivity
                .getApplication()).getStorageFile();
        for (int i = 0; i < storageVolume.size(); i++) {
            if (mStorageManager != null
                    && reflectionApis
                            .getVolumeState(mStorageManager, storageVolume.get(i)).equals(
                                    Environment.MEDIA_MOUNTED)) {
                mLocalStorageElementList.add(storageVolume.get(i));
                mLocalStorageFile.add(tmpVFiles[i]);
            }
        }
        updateStorageItemElementList();
        if (mSelectedLocalStorageFile != null) {
            boolean mIsSelectedStorageRomoved = true;
            for (int i = 0; i < mLocalStorageFile.size(); i++) {
                if (mSelectedLocalStorageFile.getPath().equals(
                        mLocalStorageFile.get(i).getPath())) {
                    mIsSelectedStorageRomoved = false;
                    break;
                }
            }
            if (mIsSelectedStorageRomoved == true) {
                mActivity.setActionBarTitle(mActivity.getResources().getString(
                        R.string.internal_storage_title));
            }
        }

    }

    public void updateCloudStorageAccountList(AccountInfo cloudStorageAccount) {
        boolean isAccountExisted = false;
        String accountNameStorageType = cloudStorageAccount.getAccountName()
                + cloudStorageAccount.getStorageType();
        for (AccountInfo accountInfo : mConnectedCloudStorageAccountList) {
            if (accountNameStorageType.equals(accountInfo.getAccountName()
                    + accountInfo.getStorageType())) {
                isAccountExisted = true;
                break;
            }
        }
        if (!isAccountExisted) {
            mConnectedCloudStorageAccountList.add(cloudStorageAccount);
            updateStorageItemElementList();
        }
        Log.d(TAG, "mConnectedCloudStorageAccountList size = "
                + mConnectedCloudStorageAccountList.size());

    }

    public void removeCloudStorageAccountList(AccountInfo info) {
        if (mConnectedCloudStorageAccountList.remove(info))
            updateStorageItemElementList();
    }

    public void clearCloudStorageAccountList() {
        mConnectedCloudStorageAccountList.clear();
        updateStorageItemElementList();
    }

    // Notes:
    // ensure mStorageItemElementList each element's storageItemElement.storageTitle is NOT null
    public void updateStorageItemElementList() {
        StorageItemElement storageItemEelement = new StorageItemElement();

        mStorageItemElementList.clear();

        storageItemEelement = new StorageItemElement();
        storageItemEelement.storageTitle = mActivity.getResources().getString(
                R.string.drawer_home);
        storageItemEelement.storageType = STORAGETYPE_HOME;
        mStorageItemElementList.add(storageItemEelement);

        storageItemEelement = new StorageItemElement();
        storageItemEelement.storageTitle = mActivity.getResources().getString(
                R.string.storage_type_local);
        storageItemEelement.storageType = STORAGETYPE_TITLE;
        mStorageItemElementList.add(storageItemEelement);

        for (int i = 0; i < mLocalStorageElementList.size(); i++) {
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageVolume = mLocalStorageElementList.get(i);
            storageItemEelement.storageTitle = VolumeInfoUtility.getInstance(mActivity).findStorageTitleByStorageVolume(storageItemEelement.storageVolume);
            storageItemEelement.vFile = mLocalStorageFile.get(i);
            storageItemEelement.storageType = STORAGETYPE_LOCAL;
            mStorageItemElementList.add(storageItemEelement);
        }

        storageItemEelement = new StorageItemElement();
        storageItemEelement.storageTitle = mActivity.getResources().getString(
                R.string.storage_type_cloud_att);
        storageItemEelement.storageType = STORAGETYPE_TITLE;
        mStorageItemElementList.add(storageItemEelement);

        if (mNetwork_StorageTitle != null) {
            for (int i = 0; i < mNetwork_StorageTitle.length; i++) {
                storageItemEelement = new StorageItemElement();
                storageItemEelement.storageTitle = mNetwork_StorageTitle[i];
                storageItemEelement.storageType = STORAGETYPE_NETWORK;
                storageItemEelement.cloudStorageIndex = i;
                mStorageItemElementList.add(storageItemEelement);
            }
        }

        if (WrapEnvironment.isVerizonEnable(mActivity)) {
            // title
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageTitle = mActivity.getResources()
                    .getString(R.string.storage_type_cloud);
            storageItemEelement.storageType = STORAGETYPE_TITLE;
            mStorageItemElementList.add(storageItemEelement);
            //
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageTitle = WrapEnvironment
                    .getVerizonLabel(mActivity);
            storageItemEelement.storageType = STORAGETYPE_CLOUD_VERIZON;
            mStorageItemElementList.add(storageItemEelement);
        }

        if (WrapEnvironment.isSupportCloud()) {
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageTitle = mActivity.getResources()
                    .getString(R.string.storage_type_cloud);
            storageItemEelement.storageType = STORAGETYPE_TITLE;
            mStorageItemElementList.add(storageItemEelement);

            if (mCloud_StorageTitle != null) {
                for (int i = 0; i < mCloud_StorageTitle.length; i++) {
                    ArrayList<AccountInfo> accountInfoList = getAccountInfo(
                            mActivity, mCloud_StorageTitle[i]);
                    if (null == accountInfoList || !accountInfoList.isEmpty()) {
                        for (AccountInfo temAccountInfo : accountInfoList) {
                            storageItemEelement = new StorageItemElement();
                            storageItemEelement.storageTitle = mCloud_StorageTitle[i];
                            storageItemEelement.acountInfo = temAccountInfo;
                            storageItemEelement.storageType = STORAGETYPE_CLOUD;
                            storageItemEelement.cloudStorageIndex = i;
                            mStorageItemElementList.add(storageItemEelement);
                        }
                    }
                }
            }

            // add drawer add cloud account
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageTitle = mActivity.getResources()
                    .getString(R.string.add_cloud_storage_dialog_title);
            storageItemEelement.storageType = STORAGETYPE_ADD_CLOUD_ACCOUNT;
            mStorageItemElementList.add(storageItemEelement);
        }
        updateNavigationViewContent();
    }

    private static void addItem(Context context, SubMenu submenu, int id, Drawable iconRes, String title) {
        MenuItem item = submenu.add(Menu.NONE, id, Menu.NONE, title);
        item.setIcon(iconRes);
        item.setCheckable(true);
    }

    private static void addItem(Context context, Menu menu, int id, Drawable iconRes, String title) {
        MenuItem item = menu.add(Menu.NONE, id, Menu.NONE, title);
        item.setIcon(iconRes);
        item.setCheckable(true);
    }

    private static SubMenu addSubMenu(Context context, Menu menu, int id, String title) {
        return menu.addSubMenu(Menu.NONE, id, Menu.NONE, makeCharSequenceWithColor(title, context.getResources().getColor(R.color.drawer_submenu_title)));
    }

    private static SpannableString makeCharSequenceWithColor(CharSequence text, int color) {
        SpannableString spanString = new SpannableString(text);
        spanString.setSpan(new ForegroundColorSpan(color), 0, spanString.length(), 0); //fix the color to white
        return spanString;
    }

    private static void resetAllCheckedToFalse(NavigationView navigationView) {
        Log.d(TAG, "resetAllCheckedToFalse");
        if (null == navigationView) {
            return ;
        }
        Menu menu = navigationView.getMenu();
        int size = menu.size();
        int subMenuSize = 0;
        MenuItem menuItem = null;
        SubMenu subMenu = null;
        // reset all
        for (int i = 0; i < size; ++i) {
            menuItem = menu.getItem(i);
            if (menuItem != null) {
                subMenu = menuItem.getSubMenu();
                if (subMenu != null) {
                    subMenuSize = subMenu.size();
                    for (int j = 0; j < subMenuSize; ++j) {
                        menuItem = subMenu.getItem(j);
                        menuItem.setChecked(false);
                    } // end of for loop
                } else {
                    menuItem.setChecked(false);
                }
            } // end of if
        } // end of for loop
    } // end of method

    private static void invalidateNavigationView(Context context, NavigationView navigationView) {
        Log.d(TAG, "invalidateNavigationView");
        List<Integer> ids = new ArrayList<Integer>();
        List<Drawable> icons = new ArrayList<Drawable>();
        List<String> titles = new ArrayList<String>();
        Menu menu = navigationView.getMenu();
        if (menu != null) {
            // ensure the UI(menu) and content(mStorageItemElementList) is sync
            int size = menu.size();
            int subMenuSize = 0;
            MenuItem menuItem = null;
            SubMenu subMenu = null;
            for (int i = 0; i < size; ++i) {
                menuItem = menu.getItem(i);
                if (menuItem != null) {
                    subMenu = menuItem.getSubMenu();
                    if (subMenu != null) {
                        menuItem = subMenu.getItem();
                        ids.add(menuItem.getItemId());
                        icons.add(null);
                        titles.add(menuItem.getTitle().toString());

                        subMenuSize = subMenu.size();
                        for (int j = 0; j < subMenuSize; ++j) {
                            menuItem = subMenu.getItem(j);
                            ids.add(menuItem.getItemId());
                            icons.add(menuItem.getIcon());
                            titles.add(menuItem.getTitle().toString());
                        } // end of for loop
                    } else {
                        ids.add(menuItem.getItemId());
                        icons.add(menuItem.getIcon());
                        titles.add(menuItem.getTitle().toString());
                    }
                } // end of if
            } // end of for loop
            menu.clear();

            int id = 0;
            Drawable icon = null;
            String title = null;
            size = ids.size();
            subMenu = null;
            for (int i = 0; i < size; ++i) {
                id = ids.get(i);
                icon = icons.get(i);
                title = titles.get(i);
                if (null == subMenu) {
                    if (null == icon) {
                        subMenu = addSubMenu(context, menu, id, title);
                    } else {
                        addItem(context, menu, id, icon, title);
                    }
                } else {
                    if (null == icon) {
                        subMenu = addSubMenu(context, menu, id, title);
                    } else {
                        addItem(context, subMenu, id, icon, title);
                    }
                }
            }
        } // end of if
    } // end of method

    private void updateNavigationViewContent() {
        Log.d(TAG, "updateNavigationViewContent");
        if (null == mStorageItemElementList) {
            Log.e(TAG, "mStorageItemElementList is null");
            return ;
        }
        if (null == mNavigationView) {
            Log.e(TAG, "mNavigationView is null");
            return ;
        }
        Menu menu = mNavigationView.getMenu();
        SubMenu lastSubMenu = null;

        // clear all item
        menu.clear();
        // update the content
        StorageItemElement storageItemElement = null;
        for (int i = 0; i < mStorageItemElementList.size(); ++i) {
            storageItemElement = mStorageItemElementList.get(i);
            if (storageItemElement.storageType == STORAGETYPE_TITLE) {
                lastSubMenu = addSubMenu(mActivity, menu, i,
                    storageItemElement.storageTitle
                );
            } else if (storageItemElement.storageType == STORAGETYPE_HOME) {
                // add item "home" without submenu
                addItem(mActivity, menu, i,
                    mActivity.getResources().getDrawable(R.drawable.asus_ic_home),
                    storageItemElement.storageTitle
                );
            } else if (storageItemElement.storageType == STORAGETYPE_LOCAL) {
                addItem(mActivity, lastSubMenu, i,
                        VolumeInfoUtility.getInstance(mActivity).findStorageIconByStorageVolume(storageItemElement.storageVolume,R.array.storage_icon),
                    storageItemElement.storageTitle
                );
            } else if (storageItemElement.storageType == STORAGETYPE_NETWORK) {
                addItem(mActivity, lastSubMenu, i,
                    mNetworkStorageDrawable[storageItemElement.cloudStorageIndex],
                    storageItemElement.storageTitle
                );
            } else if (storageItemElement.storageType == STORAGETYPE_CLOUD) {
                Drawable icon = mCloudStorageDrawable[storageItemElement.cloudStorageIndex];
                // check the account is mount or not via storageItemElement.acountInfo
                String title = (storageItemElement.acountInfo != null ?
                    storageItemElement.acountInfo.getAccountName() :
                    storageItemElement.storageTitle
                );
                addItem(mActivity, lastSubMenu, i, icon, title);
            } else if (storageItemElement.storageType == STORAGETYPE_ADD_CLOUD_ACCOUNT) {
                addItem(mActivity, lastSubMenu, i,
                    mActivity.getResources().getDrawable(R.drawable.asus_ic_add),
                    storageItemElement.storageTitle
                );
            } else if(storageItemElement.storageType == STORAGETYPE_CLOUD_VERIZON) {
                addItem(mActivity, lastSubMenu, i
                        , WrapEnvironment.getVerizonIcon(mActivity)
                        ,storageItemElement.storageTitle
                );
            }
        } // end of for loop

        // highlight the selected item
        highlightNavigationViewSelectedItem();
    } // end of method

    private static boolean isSelectableItem(Context context, String item) {
        if (null == item || (item != null && item.equals(context.getString(R.string.add_cloud_storage_dialog_title)))) {
            return false;
        }
        return true;
    }

    private void highlightNavigationViewSelectedItem() {
        Log.d(TAG, "highlightNavigationViewItem");
        if (null == mStorageItemElementList) {
            Log.e(TAG, "mStorageItemElementList is null");
            return ;
        }
        if (null == mNavigationView) {
            Log.e(TAG, "mNavigationView is null");
            return ;
        }
        if (!isSelectableItem(mActivity, mSelectedStorage)) {
            Log.w(TAG, "ignore highlight the unselectable item");
            return ;
        }

        Menu menu = mNavigationView.getMenu();
        if (menu != null) {
            //TT-827849
            MenuItem menuItem = menu.findItem(selectId);
            if(menuItem!=null)
                selectedMenuItem(menuItem, mSelectedStorage);
                            }
//        if (menu != null) {
//            int size = menu.size();
//            int subMenuSize = 0;
//            MenuItem menuItem = null;
//            SubMenu subMenu = null;
//            for (int i = 0; i < size; ++i) {
//                menuItem = menu.getItem(i);
//                if (menuItem != null) {
//                    subMenu = menuItem.getSubMenu();
//                    if (subMenu != null) {
//                        subMenuSize = subMenu.size();
//                        for (int j = 0; j < subMenuSize; ++j) {
//                            menuItem = subMenu.getItem(j);
//                            if (selectedMenuItem(menuItem, mSelectedStorage)) {
//                                return ;
//                            }
//                        } // end of for loop
//                    } else if (selectedMenuItem(menuItem, mSelectedStorage)) {
//                        return ;
//                    }
//                } // end of if
//            } // end of for loop
//        } // end of if
    } // end of method

    private static void invertMenuItemIcon(MenuItem item) {
        ColorMatrix colorMatrix_Inverted = new ColorMatrix(new float[] {
            -1,  0,  0,  0, 255,
            0, -1,  0,  0, 255,
            0,  0, -1,  0, 255,
            0,  0,  0,  1,   0
        });
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(colorMatrix_Inverted);
        Drawable icon = item.getIcon().getConstantState().newDrawable().mutate();
        icon.setColorFilter(cf);
        item.setIcon(icon);
    }

    private void setCheckedItem(int checkedItemId) {
        MenuItem menuItem = mNavigationView.getMenu().findItem(checkedItemId);
        menuItem.setChecked(true);
        // ensure the UI(menu) and content(mStorageItemElementList) is sync
        StorageItemElement storageItem = mStorageItemElementList.get(menuItem.getItemId());
        if (storageItem.storageType != STORAGETYPE_CLOUD && storageItem.storageType != STORAGETYPE_CLOUD_VERIZON) {
            invertMenuItemIcon(menuItem);
        } else {
            // FIXME:
            // onedrive icon need to update, then we can remove this.
            int msgObjType = RemoteAccountUtility.getInstance(mActivity).findMsgObjTypeByCloudTitle(mActivity, storageItem.storageTitle);
            if (MsgObj.TYPE_SKYDRIVE_STORAGE == msgObjType) {
                invertMenuItemIcon(menuItem);
            }
        }
        menuItem.setTitle(makeCharSequenceWithColor(
            menuItem.getTitle(), mActivity.getResources().getColor(R.color.drawer_selected_text)
        ));
    }

    private boolean selectedMenuItem(MenuItem menuItem, String selectedStorage) {
        if (menuItem.getItemId() >= mStorageItemElementList.size()) {
            return false;
        }
        if (menuItem.isChecked()) {
            return false;
        }
        // workaround for navigation view setChecked not work issue
        invalidateNavigationView(mActivity, mNavigationView);
        setCheckedItem(menuItem.getItemId());
        return true;
//        String itemId = null;
//        if (menuItem.getItemId() >= mStorageItemElementList.size()) {
//            return false;
//        }
//        // ensure the UI(menu) and content(mStorageItemElementList) is sync
//        StorageItemElement storageItem = mStorageItemElementList.get(menuItem.getItemId());
//        // FIXME:
//        // ensure storageItem.storageTitle is NOT null
//        if (storageItem.acountInfo != null) {
//            itemId = storageItem.storageTitle + storageItem.acountInfo.getAccountName();
//        } else {
//            itemId = storageItem.storageTitle;
//        }
//
//        int selectedItemId = 0;
//        // homepage will show "file manager"
//        if (mActivity.getString(R.string.file_manager).equals(selectedStorage)) {
//            if (mActivity.getString(R.string.drawer_home).equals(itemId)) {
//                if (menuItem.isChecked()) {
//                    return false;
//                }
//                // FIXME:
//                // workaround for navigation view setChecked not work issue
//                selectedItemId = menuItem.getItemId();
//                invalidateNavigationView(mActivity, mNavigationView);
//                setCheckedItem(selectedItemId);
//                return true;
//            }
//        } else if (itemId.equals(selectedStorage)) {
//            if (menuItem.isChecked()) {
//                return false;
//            }
//            // FIXME:
//            // workaround for navigation view setChecked not work issue
//            selectedItemId = menuItem.getItemId();
//            invalidateNavigationView(mActivity, mNavigationView);
//            setCheckedItem(selectedItemId);
//            return true;
//        }
//        return false;
    }

    public static ArrayList<AccountInfo> getAccountInfo(Context context, String cloudTitle) {
        int storageType = RemoteAccountUtility.getInstance(null).findMsgObjTypeByCloudTitle(context, cloudTitle);
        ArrayList<AccountInfo> accountInfoList = new ArrayList<AccountInfo>();
        for (AccountInfo tmpAccountInfo : mConnectedCloudStorageAccountList) {
            if (tmpAccountInfo.getStorageType() == storageType) {
                accountInfoList.add(tmpAccountInfo);
            }
        }
        return accountInfoList;
    }

    private void startScanFile(VFile file) {
        if (file != null) {
            if (!file.exists()) {
                return;
            }
            FileListFragment fragment = (FileListFragment) mActivity
                    .getFragmentManager().findFragmentById(R.id.filelist);
            if (fragment != null) {
                mActivity.showSearchFragment(FragmentType.NORMAL_SEARCH,
                        false);
                fragment.startScanFile(file, ScanType.SCAN_CHILD, false);
            }
        }
    }

    // private boolean getLinkToNetPermission(){
    // boolean accept =
    // ItemOperationUtility.getInstance().checkCtaPermission(mActivity);
    // if (accept) {
    // return true;
    // }
    // else {
    // return false;
    // }
    // }
}
