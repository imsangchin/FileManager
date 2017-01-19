package com.asus.remote.utility;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.provider.ProviderUtility.MountAccounts;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.dialog.CloudStorageMountHintDialogFragment;
import com.asus.remote.dialog.RemoteWiFiTurnOnDialogFragment;
import com.asus.service.AccountAuthenticator.helper.TokenUtils;
import com.asus.service.cloudstorage.common.HandlerCommand;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.common.MsgObj.StorageObj;
import android.os.Build;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;




public class RemoteAccountUtility {

    private static final String TAG = RemoteAccountUtility.class.getSimpleName();

    public static final int ACCOUNT_MSG_WHAT = 2049;
    public Map<String, AccountInfo> accountsMap = new HashMap<String, AccountInfo>();
    public Map<String, Integer> tokenTypeMsgTypeMap = new HashMap<String, Integer>();
    private WeakReference<Activity> mActivity = null;
    private AsusAccountHelper mAsusAccountHelper;
    public Map<Integer, String[]> clouds = new HashMap<Integer, String[]>();
    public static boolean isTokenFromLogin = false;
    public static boolean isTokenFromRefresh = false;
    public String[] cloudTitles;
    public Drawable[] cloudDrawables;
    public Drawable[] dialog_cloudDrawables;

    public static List<String> availableShowClouds = new ArrayList<String>();
    public List<String> GDriveAccounts = new ArrayList<String>();
    public long [] totalGDriveQuotas;
    private static boolean DEBUG = ConstantsUtil.DEBUG;

    private static RemoteAccountUtility remoteAccountUtility;

    private RemoteAccountUtility() {

    }

    private void setActivity(Activity mActivity)
    {
        this.mActivity = new WeakReference<Activity>(mActivity);
    }

    public Activity getActivity()
    {
        if(mActivity!=null)
            return mActivity.get();
        return null;
    }

    public static RemoteAccountUtility getInstance(Activity mActivity)
    {
        if(remoteAccountUtility==null)
            remoteAccountUtility = new RemoteAccountUtility();
        if(mActivity!=null)
            remoteAccountUtility.setActivity(mActivity);
        return remoteAccountUtility;
    }
    
    static {
        availableShowClouds.add(CloudType.BAIDU);
        availableShowClouds.add(CloudType.DROPBOX);
        availableShowClouds.add(CloudType.GOOGLEDRIVE);
        availableShowClouds.add(CloudType.HOMEBOX);
        availableShowClouds.add(CloudType.HOMECLOUD);
        availableShowClouds.add(CloudType.SKYDRIVE);
        availableShowClouds.add(CloudType.WEBSTORAGE);
        availableShowClouds.add(CloudType.YANDEX);
    }

    private TempParameters mTempParameters;

    public class TempParameters {
        public AsusAccountHelper ASUS_ACCOUNT_HELPER;
        public Map<String, AccountInfo> ACCOUNT_MAP;
        public Map<String, Integer> TOKEN_TYPE_MSG_TYPE_MAP;
    }

    public void loadParameters() {
        Log.d(TAG, "loadParameters");
        if (mTempParameters != null) {
            mAsusAccountHelper = mTempParameters.ASUS_ACCOUNT_HELPER;
            tokenTypeMsgTypeMap = mTempParameters.TOKEN_TYPE_MSG_TYPE_MAP;
            accountsMap = mTempParameters.ACCOUNT_MAP;

            mTempParameters = null;
        }
    }

    public void saveParameters() {
        Log.d(TAG, "saveParameters");
        mTempParameters = new TempParameters();
        mTempParameters.ASUS_ACCOUNT_HELPER = mAsusAccountHelper;
        mTempParameters.TOKEN_TYPE_MSG_TYPE_MAP = tokenTypeMsgTypeMap;
        mTempParameters.ACCOUNT_MAP = accountsMap;
    }

    public void getShowClouds(List<String> clouds) {
        if (clouds != null) {
            List<String> outClouds = new ArrayList<String>();
            for (String tempCloud : clouds) {
                if (!availableShowClouds.contains(tempCloud)) {
                    outClouds.add(tempCloud);
                }
            }
            if (outClouds.size() > 0) {
                clouds.removeAll(outClouds);
            }
        }
    }

    public void initAvaliableCloudsInfo() {

        clouds.clear();
        List<String> avaliableCloud = null;
        if(mAsusAccountHelper != null) {
            avaliableCloud = mAsusAccountHelper.getAvailableClouds();
        }
        //getShowClouds(avaliableCloud);
      /* for (String temp:avaliableCloud) {
        Log.d("AsusAccountHelper", "show cloud:"+temp);
       }*/
        int count = 0;
        if (avaliableCloud != null) {
            count = avaliableCloud.size();
        }
        cloudTitles = new String[count];
        cloudDrawables = new Drawable[count];
        dialog_cloudDrawables = new Drawable[count];

        if(getActivity()==null)
            return;
//     cloudTitles[0] = mActivity.getResources().getString(R.string.networkplace_storage_title);
//     cloudDrawables[0] = mActivity.getResources().getDrawable(R.drawable.asus_ic_network_place);
//     dialog_cloudDrawables[0] = mActivity.getResources().getDrawable(R.drawable.dialog_asus_ic_network);
        for (int i = 0; i < count; i++) {
            String cloudName = avaliableCloud.get(i);
            if (cloudName.trim().equalsIgnoreCase(CloudType.BAIDU)) {
                clouds.put(MsgObj.TYPE_BAIDUPCS_STORAGE, new String[]{
                    AsusAccountHelper.ASUS_BAIDU_ACCOUNT_TYPE, AsusAccountHelper.ASUS_BAIDU_AUTHTOKEN_TYPE});
                tokenTypeMsgTypeMap.put(AsusAccountHelper.ASUS_BAIDU_AUTHTOKEN_TYPE, MsgObj.TYPE_BAIDUPCS_STORAGE);
                cloudTitles[i] = getActivity().getResources().getString(R.string.baidu_storage_title);
                cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_baiducloud);
                dialog_cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_baiducloud);
            } else if (cloudName.trim().equalsIgnoreCase(CloudType.WEBSTORAGE)) {
                clouds.put(MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE, new String[]{
                    AsusAccountHelper.ASUS_SERVICE_ACCOUNT_TYPE, AsusAccountHelper.TOKEN_TYPE_AWS,});
                tokenTypeMsgTypeMap.put(AsusAccountHelper.TOKEN_TYPE_AWS, MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE);
                cloudTitles[i] = getActivity().getResources().getString(R.string.asuswebstorage_storage_title);
                cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_webstorage);
                dialog_cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_webstorage);
            } else if (cloudName.trim().equalsIgnoreCase(CloudType.HOMEBOX) || cloudName.trim().equalsIgnoreCase(CloudType.HOMECLOUD)) {
                clouds.put(MsgObj.TYPE_HOMECLOUD_STORAGE, new String[]{AsusAccountHelper.ASUS_SERVICE_ACCOUNT_TYPE, AsusAccountHelper.TOKEN_TYPE_AAE});
                tokenTypeMsgTypeMap.put(AsusAccountHelper.TOKEN_TYPE_AAE, MsgObj.TYPE_HOMECLOUD_STORAGE);
                cloudTitles[i] = getActivity().getResources().getString(R.string.asushomebox_storage_title);
                cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_homebox);
                dialog_cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_homebox);
            } else if (cloudName.trim().equalsIgnoreCase(CloudType.SKYDRIVE)) {
                clouds.put(MsgObj.TYPE_SKYDRIVE_STORAGE, new String[]{
                    AsusAccountHelper.ASUS_SKYDRIVE_ACCOUNT_TYPE, AsusAccountHelper.ASUS_SKYDRIVE_AUTHTOKEN_TYPE});
                tokenTypeMsgTypeMap.put(AsusAccountHelper.ASUS_SKYDRIVE_AUTHTOKEN_TYPE, MsgObj.TYPE_SKYDRIVE_STORAGE);
                cloudTitles[i] = getActivity().getResources().getString(R.string.skydrive_storage_title);
                cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_skydrive);
                dialog_cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.dialog_asus_ic_skydrive);
            } else if (cloudName.trim().equalsIgnoreCase(CloudType.DROPBOX)) {
                clouds.put(MsgObj.TYPE_DROPBOX_STORAGE, new String[]{
                    AsusAccountHelper.ASUS_DROPBOX_ACCOUNT_TYPE, AsusAccountHelper.ASUS_DROPBOX_AUTHTOKEN_TYPE});
                tokenTypeMsgTypeMap.put(AsusAccountHelper.ASUS_DROPBOX_ACCOUNT_TYPE, MsgObj.TYPE_DROPBOX_STORAGE);
                cloudTitles[i] = getActivity().getResources().getString(R.string.dropbox_storage_title);
                cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_dropbox);
                dialog_cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_dropbox);
            } else if (cloudName.trim().equalsIgnoreCase(CloudType.GOOGLEDRIVE)) {
                clouds.put(MsgObj.TYPE_GOOGLE_DRIVE_STORAGE, new String[]{AsusAccountHelper.ASUS_GOOGLE_ACCOUNT_TYPE, AsusAccountHelper.ASUS_GOOGLE_AUTHTOKEN_TYPE});
                tokenTypeMsgTypeMap.put(AsusAccountHelper.ASUS_GOOGLE_AUTHTOKEN_TYPE, MsgObj.TYPE_GOOGLE_DRIVE_STORAGE);
                cloudTitles[i] = getActivity().getResources().getString(R.string.googledrive_storage_title);
                cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_gogdrive);
                dialog_cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_gogdrive);
            } else if (cloudName.trim().equalsIgnoreCase(CloudType.YANDEX)) {
                clouds.put(9, new String[]{AsusAccountHelper.ASUS_YANDEX_ACCOUNT_TYPE, AsusAccountHelper.ASUS_YANDEX_AUTHTOKEN_TYPE});
                tokenTypeMsgTypeMap.put(AsusAccountHelper.ASUS_YANDEX_AUTHTOKEN_TYPE, 9);
                cloudTitles[i] = getActivity().getResources().getString(R.string.yandex_storage_title);
                cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_yandex);
                dialog_cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_yandex);
            } else {
                //assure cloudTitles[i] not null
                cloudTitles[i] = getActivity().getResources().getString(R.string.asuswebstorage_storage_title);
                cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_webstorage);
                dialog_cloudDrawables[i] = getActivity().getResources().getDrawable(R.drawable.asus_ic_webstorage);
            }
        }
        Log.d("felix_zhang", "execute initAvaliableCloudsInfo");
        ((FileManagerActivity) getActivity()).updateAvaliableClouds(cloudTitles, cloudDrawables, dialog_cloudDrawables);
        initAccounts();
        ((FileManagerActivity) getActivity()).setFromFirst(false);
        initAccountsChange(false);
        ((FileManagerActivity) getActivity()).validateIsTheLocaleChangeedOnResume();
        ((FileManagerActivity) getActivity()).addSavedTokenToAccountsMap();
        GDriveAccounts = getRegisteredAccounts(MsgObj.TYPE_GOOGLE_DRIVE_STORAGE, getActivity());
        if (null != GDriveAccounts){
            if (null == totalGDriveQuotas || totalGDriveQuotas.length != GDriveAccounts.size()) {
                totalGDriveQuotas = new long[GDriveAccounts.size()];
                for (int i=0;i<totalGDriveQuotas.length;i++){
                    totalGDriveQuotas[i]  = -1;
                }
            }
        }
    }

    public boolean showWifiTurnOnDialogIfNeed() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (null == activeNetworkInfo || !activeNetworkInfo.isConnected()) {
            RemoteWiFiTurnOnDialogFragment remoteWiFiTurnOnDialogFragment = RemoteWiFiTurnOnDialogFragment.newInstance((Integer) VFileType.TYPE_CLOUD_STORAGE);
            remoteWiFiTurnOnDialogFragment.show(getActivity().getFragmentManager(), "RemoteWiFiTurnOnDialogFragment");
            return true;
        }
        return false;
    }

    public Drawable findCloudDrawable(Context context, String title) {
        String baidu = context.getResources().getString(R.string.baidu_storage_title);
        String dropbox = context.getResources().getString(R.string.dropbox_storage_title);
        String homebox = context.getResources().getString(R.string.asushomebox_storage_title);
        String aws = context.getResources().getString(R.string.asuswebstorage_storage_title);
        String skydrive = context.getResources().getString(R.string.skydrive_storage_title);
        String googledrive = context.getResources().getString(R.string.googledrive_storage_title);
        String yandex = context.getResources().getString(R.string.yandex_storage_title);

        if (title == null) {
            return null;
        }

        if (title.equals(baidu)) {
            return context.getResources().getDrawable(R.drawable.asus_ic_baiducloud);
        } else if (title.equals(dropbox)) {
            return context.getResources().getDrawable(R.drawable.asus_ic_dropbox);
        } else if (title.equals(homebox)) {
            return context.getResources().getDrawable(R.drawable.asus_ic_homebox);
        } else if (title.equals(aws)) {
            return context.getResources().getDrawable(R.drawable.asus_ic_webstorage);
        } else if (title.equals(skydrive)) {
            return context.getResources().getDrawable(R.drawable.dialog_asus_ic_skydrive);
        } else if (title.equals(googledrive)) {
            return context.getResources().getDrawable(R.drawable.asus_ic_gogdrive);
        } else if (title.equals(yandex)) {
            return context.getResources().getDrawable(R.drawable.asus_ic_yandex);
        }

        return null;
    }

    public List<String> getAvailableLoginCloudTitles(Context context) {
        List<String> availableLoginClouds = new ArrayList<String>();
        for (Entry<Integer, String[]> entry : clouds.entrySet()) {
            String[] cloudType = entry.getValue();
            Integer msgObjType = entry.getKey();
            TokenUtils util = TokenUtils.getInstance(getActivity());
            String[] aucloudAccountList = util.getAccountNamesByType(cloudType[0]);
            if (MsgObj.TYPE_GOOGLE_DRIVE_STORAGE == msgObjType || null == aucloudAccountList || 0 == aucloudAccountList.length) {
                availableLoginClouds.add(findCloudTitleByMsgObjType(context, msgObjType));
            } else if ((MsgObj.TYPE_GOOGLE_DRIVE_STORAGE != msgObjType) && (null != aucloudAccountList)) {
                for (int i = 0; i < aucloudAccountList.length; i++) {
                    if (isMountable(cloudType[0], getActivity()) &&
                        !MountAccounts.isMounted(getActivity().getContentResolver(), cloudType[0], cloudType[1], aucloudAccountList[0])) {
                        //drive and dropbox need mount, and we always show google drive
                        availableLoginClouds.add(findCloudTitleByMsgObjType(context, msgObjType));
                        break;
                    }
                }
            }
        }
        return availableLoginClouds;
    }

    public List<Entry<String, Integer>> getLoginedAccountNameAndMsgObjType(Context context) {
        List<Entry<String, Integer>> loginedAccount = new ArrayList<Entry<String, Integer>>();
        for (Entry<Integer, String[]> entry : clouds.entrySet()) {
            String[] cloudType = entry.getValue();
            Integer msgObjType = entry.getKey();
            TokenUtils util = TokenUtils.getInstance(getActivity());
            String[] aucloudAccountList = util.getAccountNamesByType(cloudType[0]);
            if (aucloudAccountList != null) {
                for (String accountName : aucloudAccountList) {
                    loginedAccount.add(new AbstractMap.SimpleEntry<String, Integer>(accountName, msgObjType));
                }
            }
        }
        return loginedAccount;
    }

    public int findMsgObjTypeByCloudTitle(Context context, String title) {
        String baidu = context.getResources().getString(R.string.baidu_storage_title);
        String dropbox = context.getResources().getString(R.string.dropbox_storage_title);
        String homebox = context.getResources().getString(R.string.asushomebox_storage_title);
        String aws = context.getResources().getString(R.string.asuswebstorage_storage_title);
        String skydrive = context.getResources().getString(R.string.skydrive_storage_title);
        String googledrive = context.getResources().getString(R.string.googledrive_storage_title);
        String yandex = context.getResources().getString(R.string.yandex_storage_title);

        if (null == title) {
            return MsgObj.TYPE_UNKNOWN_STORAGE;
        }
        if (title.equals(baidu)) {
            return MsgObj.TYPE_BAIDUPCS_STORAGE;
        }
        if (title.equals(dropbox)) {
            return MsgObj.TYPE_DROPBOX_STORAGE;
        }
        if (title.equals(homebox)) {
            return MsgObj.TYPE_HOMECLOUD_STORAGE;
        }
        if (title.equals(aws)) {
            return MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE;
        }
        if (title.equals(skydrive)) {
            return MsgObj.TYPE_SKYDRIVE_STORAGE;
        }
        if (title.equals(googledrive)) {
            return MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
        }
        if (title.equals(yandex)) {
            return MsgObj.TYPE_YANDEX_STORAGE;
        }
        return MsgObj.TYPE_UNKNOWN_STORAGE;
    }

    public static int findStorageTypeByCloudTitle(Context context, String title) {
        String baidu = context.getResources().getString(R.string.baidu_storage_title);
        String dropbox = context.getResources().getString(R.string.dropbox_storage_title);
        String homebox = context.getResources().getString(R.string.asushomebox_storage_title);
        String aws = context.getResources().getString(R.string.asuswebstorage_storage_title);
        String skydrive = context.getResources().getString(R.string.skydrive_storage_title);
        String googledrive = context.getResources().getString(R.string.googledrive_storage_title);
        String yandex = context.getResources().getString(R.string.yandex_storage_title);

        if (null == title) {
            return StorageType.TYPE_UNKNOWN_STORAGE;
        }
        if (title.equals(baidu)) {
            return StorageType.TYPE_BAIDUPCS;
        }
        if (title.equals(dropbox)) {
            return StorageType.TYPE_DROPBOX;
        }
        if (title.equals(homebox)) {
            return StorageType.TYPE_HOME_CLOUD;
        }
        if (title.equals(aws)) {
            return StorageType.TYPE_ASUSWEBSTORAGE;
        }
        if (title.equals(skydrive)) {
            return StorageType.TYPE_SKYDRIVE;
        }
        if (title.equals(googledrive)) {
            return StorageType.TYPE_GOOGLE_DRIVE;
        }
        if (title.equals(yandex)) {
            return StorageType.TYPE_YANDEX;
        }
        return StorageType.TYPE_UNKNOWN_STORAGE;
    }

    public String findCloudTitleByMsgObjType(Context mContex, int msgObjType) {
        switch (msgObjType) {
            case MsgObj.TYPE_GOOGLE_DRIVE_STORAGE:
                return mContex.getResources().getString(R.string.googledrive_storage_title);
            case MsgObj.TYPE_DROPBOX_STORAGE:
                return mContex.getResources().getString(R.string.dropbox_storage_title);
            case MsgObj.TYPE_BAIDUPCS_STORAGE:
                return mContex.getResources().getString(R.string.baidu_storage_title);
            case MsgObj.TYPE_SKYDRIVE_STORAGE:
                return mContex.getResources().getString(R.string.skydrive_storage_title);
            case MsgObj.TYPE_HOMECLOUD_STORAGE:
                return mContex.getResources().getString(R.string.asushomebox_storage_title);
            case MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE:
                return mContex.getResources().getString(R.string.asuswebstorage_storage_title);
            case 9:
                return mContex.getResources().getString(R.string.yandex_storage_title);

            default:
                break;
        }

        return "";
    }

    public void onResume() {

    }

    public void init() {
        //if(mActivity == null || mAsusAccountHelper == null){
        if (mAsusAccountHelper != null) {
            mAsusAccountHelper = null;
        }
        mAsusAccountHelper = new CloudAsusAccountHelper(getActivity());
        mAsusAccountHelper.onActivityCreated();
        //}

    }

    public void initParams() {
        mAsusAccountHelper = null;
    }

    public void destory() {
        if (mAsusAccountHelper != null) {
            mAsusAccountHelper.onDestroy();
//     mAsusAccountHelper = null;
        }
        mActivity = null;
    }

    public void addGoogleAccount() {
        isTokenFromLogin = true;
        String[] accountType = clouds.get(MsgObj.TYPE_GOOGLE_DRIVE_STORAGE);
        if (accountType != null) {
            mAsusAccountHelper.login(accountType[0], accountType[1]);
            Log.d("AsusAccountHelper", "mAsusAccountHelper add googleAccount");
        }
    }

    public List<String> getAvailableClouds() {
        return mAsusAccountHelper.getAvailableClouds();
    }
   /*public static void addAccount(int cloudType){
       AccountManager mAccountManager = AccountManager.get(mActivity);
       String[] accountType = clouds.get(cloudType);
       final Account availableAccounts[] = mAccountManager.getAccountsByType(accountType[0]);
        if (availableAccounts != null && availableAccounts.length > 0 && cloudType != MsgObj.TYPE_GOOGLE_DRIVE_STORAGE) {
            final Account account = availableAccounts[0];
            //Log.i(TAG, "init account:accountName:" + account.name);
            final String key = account.name + "_" + cloudType;
            AccountInfo info = new AccountInfo(accountType[0],
                    accountType[1], account.name);
            info.setStorageType(cloudType);
            accountsMap.put(key, info);
            info.setState(StorageObj.State.CONNECTED);
            Log.d("AsusAccountHelper","addAccount");
            sendUpdateShortCutUi(info,ShortCutFragment.MSG_UPDATE_UI);
        }else {
            isTokenFromLogin = true;
//          SambaFileUtility.updateHostIp = false;
//          RemoteFileUtility.isShowDevicesList = false;
//          FileManagerActivity.isSearchIng = false;
            mAsusAccountHelper.login(accountType[0], accountType[1]);
            Log.d("AsusAccountHelper","mAsusAccountHelper.login");
        }
      // mAsusAccountHelper.login(accountType[0], accountType[1]);
   }*/

    public void addAccount(int cloudType) {
        if (clouds != null) {
            String[] accountType = clouds.get(cloudType);
            if (accountType != null) {
                if (isMountable(accountType[0], getActivity()) && (getUnmountedAccounts(cloudType, getActivity()).size() > 0)) {
                    CloudStorageMountHintDialogFragment removeCloudStorageDialog = CloudStorageMountHintDialogFragment.newInstance(cloudType);
                    removeCloudStorageDialog.show(getActivity().getFragmentManager(), "CloudStorageMountHintDialogFragment");
                } else {
                    isTokenFromLogin = true;
                    mAsusAccountHelper.login(accountType[0], accountType[1]);
                    Log.d("AsusAccountHelper", "mAsusAccountHelper.login");
                }
            }
        }
    }

    public List<String> getUnmountedAccounts(int cloudType, Context context) {
        List<String> unmountedAccounts = new ArrayList<String>();

        if (clouds != null && context != null) {
            String[] accountType = clouds.get(cloudType);
            if (accountType != null) {
                TokenUtils util = TokenUtils.getInstance(context);
                String[] accountNames = util.getAccountNamesByType(accountType[0]);

                for (int i = 0; accountNames != null && i < accountNames.length; i++) {
                    if (isMountable(accountType[0], context) && !MountAccounts.isMounted(context.getContentResolver(), accountType[0], accountType[1], accountNames[i])) {
                        unmountedAccounts.add(accountNames[i]);
                    }
                }
            }
        }
        return unmountedAccounts;
    }
    public List<String> getRegisteredAccounts(int cloudType, Context context) {
        List<String> Accounts = new ArrayList<String>();

        if (clouds != null && context != null) {
            String[] accountType = clouds.get(cloudType);
            if (accountType != null) {
                TokenUtils util = TokenUtils.getInstance(context);
                String[] accountNames = util.getAccountNamesByType(accountType[0]);

                for (int i = 0; accountNames != null && i < accountNames.length; i++) {
                    if (!isMountable(accountType[0], context)){
                        Accounts.add(accountNames[i]);
                    }
                }
            }
        }
        return Accounts;
    }
    public boolean isMountable(String accountType, Context context) {
        TokenUtils util = TokenUtils.getInstance(context);
        return !WrapEnvironment.isAZSEnable(context) && util.hasAuthenticator(accountType);
    }

    public void mountAccount(int cloudType, String account_name, Context context) {
        if (DEBUG)
            Log.d(TAG, "mount account " + account_name);
        if (clouds == null || context == null)
            return;

        String[] accountType = clouds.get(cloudType);

        isTokenFromLogin = true;
        if (account_name != null && !account_name.equals("")) {
            Log.d(TAG, "Go to getToken by name.");
            mAsusAccountHelper.getAuthTokenByName(account_name, accountType[0], accountType[1]);
        } else {
            Log.d(TAG, "Go to getToken.");
            mAsusAccountHelper.getAuthToken(accountType[0], accountType[1]);
        }
    }

    public void removeAccount(Context context, int msgObjType, String account_name) {
        if (mAsusAccountHelper != null && clouds != null && context != null) {
            String[] accountType = clouds.get(msgObjType);
            if (accountType != null && context != null) {
                TokenUtils mTokenUtils = TokenUtils.getInstance(context);
                if (isMountable(accountType[0], context)) {
                    //Account managed in settings
                    if (DEBUG)
                        Log.d(TAG, "unmount account " + account_name);
                    MountAccounts.removeAccounts(context.getContentResolver(), accountType[0], accountType[1], account_name);
                    removeAccountFromAccountsMap(accountType[0], account_name);

                } else {
                    if (DEBUG)
                        Log.d(TAG, "logout account " + account_name);
                    mTokenUtils.logout(accountType[0], accountType[1]);
                    //initAccountsChange(false);
                    removeAccountFromAccountsMap(accountType[0], account_name);

                    //Dataprovider support interface to clean user data , called by filemanager or other applications.
                    Intent intent=null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent = new Intent("asus.filemanager.LOGIN_ACCOUNTS_CHANGED");
                    } else {
                        intent = new Intent("android.accounts.LOGIN_ACCOUNTS_CHANGED");
                    }
                    String names[] = mTokenUtils.getAccountNamesByType(accountType[0]);
                    intent.putExtra("packagesName", context.getPackageName());
                    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType[0]);
                    intent.putExtra(AccountManager.KEY_AUTH_TOKEN_LABEL, accountType[1]);
                    if (names != null) {
                        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, names[0]);
                    }
                    context.sendBroadcast(intent);
                }
            }
        }
    }

    public void removeAccountFromAccountsMap(String accountType, String account_name) {
        //remove all accounts have same accountType
        Map<String, AccountInfo> tempMap = new HashMap<String, AccountInfo>();

        for (Entry<String, AccountInfo> entry : accountsMap.entrySet()) {
            AccountInfo info = entry.getValue();
            if (info != null) {
                if (info.accountType.equals(accountType) && info.accountName.equals(account_name)) {
                    sendUpdateShortCutUi(info, ShortCutFragment.MSG_REMOVE_UI);
                } else {
                    tempMap.put(entry.getKey(), info);
                }
            }
        }
        accountsMap = tempMap;
    }

    public void refreshToken(int cloudType, String accountName) {
        String[] accountType = clouds.get(cloudType);
        if (mAsusAccountHelper == null) {
            Log.d(TAG, "mAsusAccountHelper is null");
            return;
        }

        isTokenFromRefresh = true;
        // mAsusAccountHelper.refreshAuthToken(accountType[0], accountType[1]);
        if (accountName != null && accountName.trim().length() > 0) {
            mAsusAccountHelper.refreshAuthTokenByName(accountName, accountType[0], accountType[1]);
            Log.d("AsusAccountHelper", "mAsusAccountHelper.refreshAuthTokenByName");
        } else {
            mAsusAccountHelper.refreshAuthToken(accountType[0], accountType[1]);
            Log.d("AsusAccountHelper", "mAsusAccountHelper.refreshAuthToken");
        }
    }

    public boolean isAvailableDriveToken(String token) {
        URL url;
        try {
            url = new URL(AsusAccountHelper.ASUS_GOOGLE_DRIVE_VERIFY_URL + token);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            int serverCode = con.getResponseCode();
            con.disconnect();
            if (serverCode == 400 || serverCode == 401) {
                //expired
                return false;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    //google drive token would changed when timeout
    public void refreshDriveToken(int cloudType, String accountName) {
        String[] accountType = clouds.get(cloudType);
        isTokenFromRefresh = true;
        // mAsusAccountHelper.refreshAuthToken(accountType[0], accountType[1]);
        if (accountName != null && accountName.trim().length() > 0) {
            mAsusAccountHelper.getAuthTokenByName(accountName, accountType[0], accountType[1]);
            Log.d("AsusAccountHelper", "mAsusAccountHelper.refreshAuthTokenByName");
        } else {
            mAsusAccountHelper.getAuthToken(accountType[0], accountType[1]);
            Log.d("AsusAccountHelper", "mAsusAccountHelper.refreshAuthToken");
        }
    }

    public void remoteAccountToken(int cloudType) {
        AccountManager mAccountManager = AccountManager.get(getActivity());
        String[] accountType = clouds.get(cloudType);
        final Account availableAccounts[] = mAccountManager.getAccountsByType(accountType[0]);
        if (availableAccounts != null && availableAccounts.length > 0) {
            for (int i = 0; i < availableAccounts.length; i++) {
                Account account = availableAccounts[i];
                //Log.i(TAG, "init account:accountName:" + account.name);
                String key = account.name + "_" + cloudType;
                AccountInfo info = accountsMap.get(key);
                if (info != null) {
                    info.setToken(null);
                    accountsMap.put(key, info);
                }
            }
        }

    }

    public void initAccounts() {
        Log.i(TAG, "initAccounts");

        //AccountManager mAccountManager = AccountManager.get(mActivity);
        accountsMap.clear();

        for (Entry<Integer, String[]> entry : clouds.entrySet()) {
            String[] cloudType = entry.getValue();
            Integer type = entry.getKey();

            TokenUtils util = TokenUtils.getInstance(getActivity());
            String[] aucloudAccountList = util.getAccountNamesByType(cloudType[0]);
            for (int i = 0; aucloudAccountList != null && i < aucloudAccountList.length; i++) {
                String account_name = aucloudAccountList[i];
                String key = account_name + "_" + type;
                AccountInfo info = new AccountInfo(cloudType[0], cloudType[1], account_name);
                info.setStorageType(type);
                accountsMap.put(key, info);
                info.setState(StorageObj.State.CONNECTED);
            }


         /*final Account availableAccounts[] = mAccountManager.getAccountsByType(cloudType[0]);
            if (availableAccounts != null && availableAccounts.length > 0) {
                for (int i = 0; i < availableAccounts.length; i++) {
                     Account account = availableAccounts[i];
                    //Log.i(TAG, "init account:accountName:" + account.name);
                     String key = account.name + "_" + type;
                    AccountInfo info = new AccountInfo(cloudType[0],cloudType[1], account.name);
                    info.setStorageType(type);
                    accountsMap.put(key, info);
                    info.setState(StorageObj.State.CONNECTED);
                    //sendUpdateShortCutUi(info,ShortCutFragment.MSG_UPDATE_UI);
                }

            }*/
        }
    }

    public void initAccountsChange(boolean isFromFirst) {
        //getAvailableClouds();
        if (isFromFirst) {
            return;
        }

        Log.d("AsusAccountHelper", "getAvailableClouds()");
        if (getActivity() == null) {
            return;
        }
        //AccountManager mAccountManager = AccountManager.get(getActivity());
        Map<String, AccountInfo> tempMap = new HashMap<String, AccountInfo>();
        for (Entry<Integer, String[]> entry : clouds.entrySet()) {
            String[] cloudType = entry.getValue();
            Integer type = entry.getKey();

            TokenUtils util = TokenUtils.getInstance(getActivity());
            String[] aucloudAccountList = util.getAccountNamesByType(cloudType[0]);
            for (int i = 0; aucloudAccountList != null && i < aucloudAccountList.length; i++) {
                String account_name = aucloudAccountList[i];

                if (isMountable(cloudType[0], getActivity()) && !MountAccounts.isMounted(getActivity().getContentResolver(), cloudType[0], cloudType[1], account_name)) {
                    //drive and dropbox need mount
                    continue;
                }

                String key = account_name + "_" + type;
                AccountInfo info = accountsMap.get(key);
                if (info != null) {
                    tempMap.put(key, info);
                } else {
                    info = new AccountInfo(cloudType[0], cloudType[1], account_name);
                    info.setStorageType(type);
                    info.setState(StorageObj.State.CONNECTED);
                    tempMap.put(key, info);
                }
                sendUpdateShortCutUi(info, ShortCutFragment.MSG_UPDATE_UI);
            }

              /*Account availableAccounts[] = mAccountManager.getAccountsByType(cloudType[0]);
                if (availableAccounts != null && availableAccounts.length > 0) {
                    for (int i = 0; i < availableAccounts.length; i++) {
                        Account account = availableAccounts[i];
                        //Log.i(TAG, "init account:accountName:" + account.name);
                        String key = account.name + "_" + type;
                        AccountInfo info = accountsMap.get(key);
                        if (info!=null) {
                            tempMap.put(key, info);
                        }else {
                            info = new AccountInfo(cloudType[0],cloudType[1], account.name);
                            info.setStorageType(type);
                            info.setState(StorageObj.State.CONNECTED);
                            tempMap.put(key, info);
                        }
                        sendUpdateShortCutUi(info,ShortCutFragment.MSG_UPDATE_UI);
                    }

                }*/
        }
        accountsMap = tempMap;
        //sendUpdateShortCutUi(null,ShortCutFragment.MSG_COMPLETE_UPDATE_UI);

    }

    public boolean isLoginAccountRemoved(Context context, String accountType, String accountName) {
        if (accountType == null || accountName == null || accountName.length() == 0) {
            return true;
        }

        TokenUtils util = TokenUtils.getInstance(context);
        String[] aucloudAccountList = util.getAccountNamesByType(accountType);
        if (aucloudAccountList == null || aucloudAccountList.length == 0) {
            Log.d(TAG, "isLoginAccountRemoved getAccountNamesByType.length == 0");
            return true;
        } else {
            for (int i = 0; i < aucloudAccountList.length; i++) {
                if (accountName.equals(aucloudAccountList[i])) {
                    return false;
                }
            }
        }

        Log.d(TAG, "isLoginAccountRemoved");
        return true;
    }

    public boolean hasAuthenticator(Context context, int cloudType) {
        boolean hasAuth = true;
        if (clouds != null) {
            String[] accountType = clouds.get(cloudType);
            if (accountType != null && accountType.length > 0) {
                TokenUtils util = TokenUtils.getInstance(context);
                hasAuth = util.hasAuthenticator(accountType[0]);
            }
        }
        return hasAuth;
    }

    public boolean validateToken(RemoteVFile file) {
        if (file == null) {
            return false;
        }
        String key = file.getStorageName() + "_" + file.getMsgObjType();
        if (accountsMap.get(key) != null && accountsMap.get(key).getToken() != null) {
            Log.d(TAG, "validateToken true");
            //Log.d(TAG, "validateToken true token:"+accountsMap.get(key).getToken());
            return true;
        }
        return false;
    }

    public void getToken(RemoteVFile file) {
        //getAvailableClouds();
        String[] cloudType = clouds.get(file.getMsgObjType());
        String accountName = file.getStorageName();
        if (accountName != null && !accountName.equals("")) {
            Log.d(TAG, "Go to getToken by name.");
            mAsusAccountHelper.getAuthTokenByName(accountName, cloudType[0], cloudType[1]);
        } else {
            Log.d(TAG, "Go to getToken.");
            mAsusAccountHelper.getAuthToken(cloudType[0], cloudType[1]);
        }
    }

    private static class CloudAsusAccountHelper extends AsusAccountHelper {
        private String TAG = "RemoteAccountUtility";

        public CloudAsusAccountHelper(Context context) {
            super(context);
        }

        @Override
        protected void HandleTokenRequest(Bundle bnd) {
            if (bnd == null) {
                return;
            }
            String command = bnd.getString(AsusAccountHelper.KEY_TOKEN_ACTION_COMMAND);
            String accountType = bnd.getString(AccountManager.KEY_ACCOUNT_TYPE);
            String accountName = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
            String authToken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
            String authTokenType = bnd.getString(AccountManager.KEY_AUTHENTICATOR_TYPES);
            String key_fresh_token = bnd.getString("key_fresh_token");

            if (DEBUG)
                Log.i(TAG, "command:" + command);

            if (DEBUG)
                Log.d(TAG, "key_fresh_token = " + key_fresh_token);

            if (key_fresh_token != null && key_fresh_token.equals("true")) {
                RemoteClientHandler.getInstance(null).setIsFromFreshToken(true);
            } else {
                RemoteClientHandler.getInstance(null).setIsFromFreshToken(false);
            }

            if (authTokenType == null)
                authTokenType = "";

            int msgObjType, storageType = -1;
            if (accountName == null || accountType == null || authToken == null) {
                Log.e(TAG, "accountName == null? " + (accountName == null));
                Log.e(TAG, "accountType == null? " + (accountType == null));
                Log.e(TAG, "authToken == null? " + (authToken == null));
                return;
            }
            if (AsusAccountHelper.TOKEN_ACTION_GET.equals(command) || AsusAccountHelper.TOKEN_ACTION_ISLOGIN.equals(command)) {
                String key = null;
                Log.i(TAG, "command:AsusAccountHelper.TOKEN_ACTION_GET, command = " + command + ", isTokenFromRefresh = " + isTokenFromRefresh + ", isTokenFromLogin = " + isTokenFromLogin + ", pendingIndex = " + pendingIndex);
                RemoteAccountUtility remoteAccountUtility = RemoteAccountUtility.getInstance(null);
                if (accountType.equals(AsusAccountHelper.ASUS_DROPBOX_ACCOUNT_TYPE)) {
                    key = accountName + "_" + remoteAccountUtility.tokenTypeMsgTypeMap.get(AsusAccountHelper.ASUS_DROPBOX_ACCOUNT_TYPE);
                    msgObjType = remoteAccountUtility.tokenTypeMsgTypeMap.get(AsusAccountHelper.ASUS_DROPBOX_ACCOUNT_TYPE);
                } else {
                    key = accountName + "_" + remoteAccountUtility.tokenTypeMsgTypeMap.get(authTokenType);
                    msgObjType = remoteAccountUtility.tokenTypeMsgTypeMap.get(authTokenType);
                }
                AccountInfo info = remoteAccountUtility.accountsMap.get(key);
                if (info != null) {
                    info.setToken(authToken);
                    remoteAccountUtility.accountsMap.put(key, info);
                } else {
                    info = new AccountInfo(accountType, authTokenType, accountName);
                    info.setToken(authToken);
                    info.setStorageType(msgObjType);
                    remoteAccountUtility.accountsMap.put(key, info);
                }
                //Log.d(TAG, info.toString());
                if (isTokenFromRefresh) {
                    Log.d(TAG, "isTokenFromRefresh:" + isTokenFromRefresh);
                    RemoteFileUtility remoteFileUtility = RemoteFileUtility.getInstance(null);
                    if (remoteFileUtility != null) {
                        Log.d(TAG, "currentBackUpMsgObj is not null");
                        MsgObj temMsgObj = remoteFileUtility.currentBackUpMsgObj.getMsgObj();
                        StorageObj temStorageObj = temMsgObj.getStorageObj();
                        if (authToken != null && authToken.length() > 0 && temStorageObj.getStorageName().equals(accountName) && temStorageObj.getStorageType() == msgObjType) {
                            temMsgObj.getStorageObj().setAccount(authToken);
                            remoteFileUtility.deliverRemoteMsg(temMsgObj, remoteFileUtility.currentBackUpMsgObj.getWhat());
                            Log.d(TAG, "msgWhat:" + remoteFileUtility.currentBackUpMsgObj.getWhat());
                            Log.d(TAG, "isTokenFromRefresh new token...");
                            remoteFileUtility.currentBackUpMsgObj = null;
                        }
                    }
                    isTokenFromRefresh = false;
                    return;
                }
                if (isTokenFromLogin) {
                    switch (msgObjType) {
                        case MsgObj.TYPE_GOOGLE_DRIVE_STORAGE:
                            storageType = StorageType.TYPE_GOOGLE_DRIVE;
                            break;
                        case MsgObj.TYPE_DROPBOX_STORAGE:
                            storageType = StorageType.TYPE_DROPBOX;
                            break;
                        case MsgObj.TYPE_BAIDUPCS_STORAGE:
                            storageType = StorageType.TYPE_BAIDUPCS;
                            break;
                        case MsgObj.TYPE_SKYDRIVE_STORAGE:
                            storageType = StorageType.TYPE_SKYDRIVE;
                            break;
                        case MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE:
                            storageType = StorageType.TYPE_ASUSWEBSTORAGE;
                            break;
                        case MsgObj.TYPE_HOMECLOUD_STORAGE:
                            storageType = StorageType.TYPE_HOME_CLOUD;
                            break;
                        case 9:
                            storageType = StorageType.TYPE_YANDEX;
                            break;
                    }
                    Log.d(TAG, "storageType:" + storageType);
                    RemoteVFile cloudRootVFile = new RemoteVFile("/" + accountName, VFileType.TYPE_CLOUD_STORAGE, accountName, storageType, "");
                    cloudRootVFile.setStorageName(accountName);
                    cloudRootVFile.setFileID("root");
                    cloudRootVFile.setFromFileListItenClick(true);
                    ShortCutFragment.currentTokenFile = cloudRootVFile;
                    isTokenFromLogin = false;

                    if (remoteAccountUtility.getActivity() != null && remoteAccountUtility.isMountable(accountType, remoteAccountUtility.getActivity()) && !MountAccounts.isMounted(remoteAccountUtility.getActivity().getContentResolver(), accountType, authTokenType, accountName)) {
                        //drive and dropbox need mount

                        //insert into mount account table
                        MountAccounts.insertAccounts(remoteAccountUtility.getActivity().getContentResolver(), accountType, authTokenType, accountName);

                        remoteAccountUtility.sendUpdateShortCutUi(info, ShortCutFragment.MSG_UPDATE_UI);
                    }
                }
                if (pendingIndex != -1){
                    remoteAccountUtility.getGDriveStorageUsage(remoteAccountUtility.getActivity(), pendingIndex);
                }

                remoteAccountUtility.sendUpdateShortCutUi(null, ShortCutFragment.MSG_TOKEN_SCAN_FILE);
            } else {
                Log.i(TAG, "command:AsusAccountHelper. other");
            }
        }
    }

    public void sendUpdateShortCutUi(AccountInfo info, int what) {
        if (getActivity() == null) {
            return;
        }
        ShortCutFragment shortcutFragment = (ShortCutFragment) (getActivity().getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag));
        if (shortcutFragment != null) {
            Handler handler = shortcutFragment.getShortCutHandler();
            if (handler != null) {
                Message msg = handler.obtainMessage(what, info);
                handler.sendMessage(msg);
            } else {
                Log.i(TAG, "handler is null");
            }
        } else {
            Log.i(TAG, "shortcutFragment is null");
        }
    }

    /*
    List<String> mountedAccounts = getMountedAccounts(MsgObj.TYPE_GOOGLE_DRIVE_STORAGE, getActivity());
    getGDriveStorageUsage();
    */
    /* index: zero based*/
    private static int pendingIndex = -1;
    public void getGDriveStorageUsage(Context aContext, int index) {
        Log.d("GDrive", "checking account index " + index);
        TokenUtils util = TokenUtils.getInstance(aContext);
        String[] AccountType = clouds.get(MsgObj.TYPE_GOOGLE_DRIVE_STORAGE);
        String[] driveAccount = util.getAccountNamesByType(AccountType[0]);
        if (null != driveAccount && driveAccount.length > index) {
            Log.d("GDrive", "driveAccount length" + driveAccount.length);
            String key = driveAccount[index] + "_" + MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
            //if (isMountable(AsusAccountHelper.ASUS_GOOGLE_ACCOUNT_TYPE, aContext) && MountAccounts.isMounted(aContext.getContentResolver(), AsusAccountHelper.ASUS_GOOGLE_ACCOUNT_TYPE, AsusAccountHelper.ASUS_GOOGLE_AUTHTOKEN_TYPE, driveAccount[index])) {
            if (!isMountable(AsusAccountHelper.ASUS_GOOGLE_ACCOUNT_TYPE, aContext) ){
                if (accountsMap.get(key) != null && accountsMap.get(key).getToken() != null) {
                    Log.d("GDrive", "setting pendingIndex to " + index);
                    RemoteAccountUtility.pendingIndex = index;
                    RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(driveAccount[index], null, null, MsgObj.TYPE_GOOGLE_DRIVE_STORAGE, HandlerCommand.CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_STORAGE_USAGE);
                }else{
                    //we can't get toekn from accountsMap
                    Log.d("GDrive", "setting pendingIndex to " + index);
                    RemoteAccountUtility.pendingIndex = index;
                    mAsusAccountHelper.getAuthTokenByName(driveAccount[index], AsusAccountHelper.ASUS_GOOGLE_ACCOUNT_TYPE, AsusAccountHelper.ASUS_GOOGLE_AUTHTOKEN_TYPE);
                }
            }
            /*
            String key = file.getStorageName() + "_" + file.getMsgObjType();
            if (accountsMap.get(key) != null && accountsMap.get(key).getToken() != null) {

            }
            RemoteVFile cloudRootVFile = new RemoteVFile("/"
                + accountInfo.getAccountName(), VFileType.TYPE_CLOUD_STORAGE,
                accountInfo.getAccountName(), storageType, "");
            cloudRootVFile.setStorageName(accountInfo.getAccountName());
            cloudRootVFile.setFileID("root");
            cloudRootVFile.setFromFileListItenClick(true);
            if (!RemoteAccountUtility.validateToken(cloudRootVFile)) {
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
                RemoteAccountUtility.getToken(cloudRootVFile);
            */
            /*
            for (int i = 0; i < driveAccount.length; i++) {
                String key = driveAccount[i] + "_" + MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
                if (isMountable(AsusAccountHelper.ASUS_GOOGLE_ACCOUNT_TYPE, mActivity) && MountAccounts.isMounted(mActivity.getContentResolver(), AsusAccountHelper.ASUS_GOOGLE_ACCOUNT_TYPE, AsusAccountHelper.ASUS_GOOGLE_AUTHTOKEN_TYPE, driveAccount[i])) {
                    RemoteFileUtility.sendCloudStorageMsg(driveAccount[i], null, null, MsgObj.TYPE_GOOGLE_DRIVE_STORAGE, HandlerCommand.CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_STORAGE_USAGE);
                }
            }
            */
        }
    }
    public static class AccountInfo {
        private String accountType;
        private String authTokenType;
        private String accountName;
        private String errorMsg;
        private String token;
        private int state = 0;
        private int storageType;
        private String address;

        public AccountInfo() {

        }

        public AccountInfo(String accountType, String authTokenType,
                           String accountName) {
            super();
            this.accountType = accountType;
            this.authTokenType = authTokenType;
            this.accountName = accountName;
        }

        public String getAccountType() {
            return accountType;
        }

        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }

        public String getAuthTokenType() {
            return authTokenType;
        }

        public void setAuthTokenType(String authTokenType) {
            this.authTokenType = authTokenType;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public int getStorageType() {
            return storageType;
        }

        public void setStorageType(int storageType) {
            this.storageType = storageType;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        @Override
        public String toString() {
            return "AccountInfo [accountType=" + accountType + ", authTokenType="
                + authTokenType + ", accountName=" + accountName
                + ", errorMsg=" + errorMsg + ", token=" + token + ", state="
                + state + ", storageType=" + storageType + ", address="
                + address + "]";
        }

    }

    public static class CloudType {
        public static final String BAIDU = "baidu";
        public static final String WEBSTORAGE = "webstorage";
        public static final String HOMEBOX = "homebox";
        public static final String HOMECLOUD = "homecloud";
        public static final String SKYDRIVE = "onedrive";
        public static final String DROPBOX = "dropbox";
        public static final String GOOGLEDRIVE = "googledrive";
        public static final String YANDEX = "yandex";

    }


}
