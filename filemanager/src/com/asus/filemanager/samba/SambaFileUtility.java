package com.asus.filemanager.samba;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerActivity.FragmentType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.SearchResultFragment;
import com.asus.filemanager.adapter.DeviceListAdapter;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.dialog.PasteDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.samba.http.HTTPServerList;
import com.asus.filemanager.samba.provider.PcInfoDbHelper;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteFileUtility.CopyTypeArgument;
import com.asus.service.cloudstorage.common.MsgObj;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

//import android.support.v4.app.NotificationCompat;
//import android.support.v4.app.NotificationCompat.Builder;

public class SambaFileUtility {

    private static final String TAG = "SambaFileUtility";

    private static final String SAMBAHEADER = "smb://";
    public static String SCAN_ROOT_PATH = "/NetWork Place/";
    final String SAMBA_HANDLER = "samba_handler";
    private final  String SEC_PWD = "2013#11#16&theodore&hannah";
    private String mScanPath = "";
    private String mUserName;
    private String mPastFileName = "";

    private int mTotalSize = 0;
    private int mCurrentSize = 0;
    private final int SAMBA_NOTIFICATION_ID = 127;

    boolean isPasteCancel = false;
    public static boolean updateHostIp = false;
    public static boolean ScanFinish = true;
    public static boolean mLoginInIsProcessing = false;

    public WeakReference<Activity> mActivity;
    private  Handler mSendSambaMessageHandle;
    SambaMessageHandle mSambaMessageHandle;
    private SambaServer mRootSambaServer;
    private NotificationManager mManager = null;
    private Builder mBuilder = null;
    private EditPool mPasteEditPool = null;
    private SambaVFile mRootFile;
    private SambaItem mSelectItem = null;
    List<SambaServer> mServerList = new ArrayList<>();

    SharedPreferences.Editor mEdit = null;
    final String FM_AP_PACKAGE = "com.asus.filemanager";

    /*******for stream play media file*************/
    FileServer mFileServer = null;
    private static final String FILE_TYPE = "*/*";
    public static String HTTP_IP = "127.0.0.1";
    public static String SelectFilePath = "";
    public static int HTTP_PORT = 0;
    public static final String SAMBA_CACHE_FOLDER = ".sfile/";

    private static SambaFileUtility sambaFileUtility;

    private SambaFileUtility()
    {

    }

    private void setActivity(Activity mActivity)
    {
        this.mActivity = new WeakReference<>(mActivity);
    }

    public Activity getActivity()
    {
        if(mActivity!=null)
            return mActivity.get();
        return null;
    }

    public static SambaFileUtility getInstance(Activity mActivity)
    {
        if(sambaFileUtility == null)
            sambaFileUtility = new SambaFileUtility();
        if(mActivity != null)
            sambaFileUtility.setActivity(mActivity);
        return sambaFileUtility;
    }

    private Handler mCallbackHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if(getActivity() == null) {
                return;
            }
            Bundle bundle = msg.getData();
            String parentPath = bundle.getString(SambaMessageHandle.SOURCE_PARENT_PATH);
            FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
            String errorMsg = bundle.getString(SambaMessageHandle.ERROR_MESSAGE);
            int copyType = bundle.getInt(SambaMessageHandle.SAMBA_REMOTE_PASTE,-1);
            String msgId = bundle.getString(SambaMessageHandle.MSGOBJ_ID,null);
            int pastType = bundle.getInt(SambaMessageHandle.PASTE_TYPE, SambaMessageHandle.COPY);
            switch(msg.what) {
            case SambaMessageHandle.ACTION_SUCCESS:
                //success
                switch(msg.arg1) {
                    case SambaMessageHandle.LOGIN_WITHOUT_PASSWORD:
                        //StoreSuccessHostInfo();
                        setIsLoginProcessing(false);
                        hideSambaDeviceListView();
                        startScanSambaServerFile();
                        break;
                    case SambaMessageHandle.MSG_SAMBA_LOGIN:
                        hideSambaDeviceListView();
                        StoreSuccessHostInfo();
                        startScanSambaServerFile();
                        break;
                    case SambaMessageHandle.ADD_NEW_FOLDER:
                        String foldername = bundle.getString(SambaMessageHandle.NEW_NAME);

                        if (((FileManagerActivity)getActivity()).isMoveToDialogShowing() ||
                                ((FileManagerActivity)getActivity()).isAddFolderDialogShowing()) {
                            MoveToDialogFragment moveToDialogFragment = (MoveToDialogFragment) getActivity()
                                    .getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                            if (moveToDialogFragment != null) {
                                moveToDialogFragment.addComplete();
                                ToastUtility.show(getActivity(), R.string.new_folder_success, foldername);
                                SambaVFile file = new SambaVFile(parentPath);
                                moveToDialogFragment.startScanFile(file, ScanType.SCAN_CHILD);
                            }
                            if (fileListFragment != null) {
                                fileListFragment.addComplete();
                                ToastUtility.show(getActivity(), R.string.new_folder_success, foldername);
                                SambaVFile file = new SambaVFile(parentPath);
                                fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
                            }
                        }
                        break;
                    case SambaMessageHandle.FILE_RENAME:
                        if (fileListFragment != null) {
                            fileListFragment.RenamingComplete();
                            if(((FileManagerActivity)getActivity()).getIsShowSearchFragment()) {
                                ((FileManagerActivity)getActivity()).reSearch(((FileManagerActivity) getActivity()).getSearchQueryKey());
                            } else {
                                SambaVFile file = new SambaVFile(parentPath);
                                fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
                            }
                        }
                        break;
                    case SambaMessageHandle.FILE_DELETE:
                        if (copyType > -1 && msgId != null) {
                            if (copyType == CopyTypeArgument.SAMB_TO_CLOUD) {
                                RemoteFileUtility.getInstance(getActivity()).sendDelteFileCompleteWhenCopyFileFromSambToCloud(msgId, true);
                                break;
                            }
                        }
                        if(((FileManagerActivity)getActivity()).getIsShowSearchFragment()) {
                            SearchResultFragment mSFragment = (SearchResultFragment) getActivity().getFragmentManager().findFragmentById(R.id.searchlist);
                            if(mSFragment != null) {
                                mSFragment.deleteComplete(false);
                            }
//                          ((FileManagerActivity)getActivity()).reSearch(getActivity().getSearchQueryKey());
                        } else {
                            if (fileListFragment != null) {
                                fileListFragment.deleteComplete();
                                SambaVFile file = new SambaVFile(parentPath);
                                fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
                            }
                        }
                        break;
                    case SambaMessageHandle.FILE_PASTE:
                        if (copyType > -1 && msgId != null) {
                            if (copyType == CopyTypeArgument.SAMB_TO_CLOUD) {
                                RemoteFileUtility.getInstance(getActivity()).sendCopyFileFromSambToCloudMsg(msgId, true);
                            } else if (copyType == CopyTypeArgument.CLOUD_TO_SAMB) {
                                RemoteFileUtility.getInstance(getActivity()).sendCopyFileFromCloudToSamba(msgId,true);
                                if (pastType == SambaMessageHandle.COPY) {
                                     SambaVFile file = new SambaVFile(parentPath);
                                     fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
                                }
                            }
                        } else {
                            if (fileListFragment != null) {
                                fileListFragment.pasteComplete(true);
                                if (parentPath != null) {
                                    VFile sourceIndicator = new VFile(parentPath);
                                    if(sourceIndicator.equals(fileListFragment.getIndicatorPath())) {
                                        if (parentPath.startsWith(SambaMessageHandle.SMB)){
                                            SambaVFile file = new SambaVFile(parentPath);
                                            fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
                                        } else {
                                            LocalVFile file = new LocalVFile(parentPath);
                                            fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
                                        }
                                    }
                                }
                            }
                        }
                        clearNotification(true);
                        isPasteCancel = false;
                        if(!ItemOperationUtility.isReadyToPaste) {
                            clearEditPool();
                        }
                        break;
                    case SambaMessageHandle.FILE_OPEN:
                        if (fileListFragment != null) {
                            fileListFragment.CloudStorageLoadingComplete();
                            String[] mNameList = bundle.getStringArray(SambaMessageHandle.SELECT_NAME_LIST);
                            if(mNameList == null) break;
                            LocalVFile file = new LocalVFile(parentPath + "/" + mNameList[0]);
//                          if (file.getName().toLowerCase().endsWith(".zip")) {
//                                VFile zipFile = file;
//                                String unZipName = zipFile.getNameNoExtension();
//                                long unZipSize = 0;
//                                String encode = getActivity().getString(R.string.default_encoding);
//                                String uriString = null;
//                                UnZipData unZipData = new UnZipData(zipFile, unZipName, unZipSize, encode, uriString);
//                                fileListFragment.showDialog(DialogType.TYPE_UNZIP_PREVIEW_DIALOG, unZipData);
//                            } else {
                                FileUtility.openFile(getActivity(), file, false, true, false);
//                            }
                        }
                        break;
                    case SambaMessageHandle.FILE_DOWNLOAD:
                        if (fileListFragment != null) {
                            fileListFragment.pasteCloudComplete();
                        }
                        break;
                    case SambaMessageHandle.FILE_SHARE:
                        if (fileListFragment != null) {
                            fileListFragment.CloudStorageLoadingComplete();
                            String[] mNameList = bundle.getStringArray(SambaMessageHandle.SELECT_NAME_LIST);
                            if(mNameList != null) {
                                int len = mNameList.length;
                                VFile[] shareArray = new VFile[len];
                                for (int i = 0; i < len; i++) {
                                    shareArray[i] = new LocalVFile(parentPath + "/" + mNameList[i]);
                                }
                                FileUtility.shareFile(getActivity(), shareArray, false);
                            }
                        }
                        break;
                }
                break;
            case SambaMessageHandle.ACTION_FAILED:
                //failed
                switch(msg.arg1) {
                    case SambaMessageHandle.LOGIN_WITHOUT_PASSWORD:
                        if(updateHostIp) {
                            refresFileList(true);
                            setIsLoginProcessing(false);
                            ScanLanNetworkPC scanThread = ScanLanNetworkPC.getInstance(getActivity().getApplicationContext());
                            scanThread.updateListAdapter();
                            if(errorMsg != null && errorMsg.contains(SambaMessageHandle.NO_SHARE_FILE_ERROR)) {
                                ToastUtility.show(getActivity(), getActivity().getString(R.string.no_share_folder));
                            } else {
//                             String selectIp = mRootSambaServer.getIpAddress();
                                AddSambaStorageDialogFragment loginDialog = AddSambaStorageDialogFragment.newInstance();
                                if(getActivity() != null && isActivityOnTheFront(getActivity())) {
                                    loginDialog.show(getActivity().getFragmentManager(), "AddSambaStorageDialogFragment");
                                }
                            }
                        }
                        break;
                    case SambaMessageHandle.MSG_SAMBA_LOGIN:
                        refresFileList(true);
                        mSelectItem = null;
                        ScanLanNetworkPC scanThread = ScanLanNetworkPC.getInstance(getActivity().getApplicationContext());
                        scanThread.updateListAdapter();
//                      String selectIp = mRootSambaServer.getIpAddress();
                        AddSambaStorageDialogFragment smbStorageDialog = AddSambaStorageDialogFragment.newInstance();
                        if(isActivityOnTheFront(getActivity())) {
                           smbStorageDialog.show(getActivity().getFragmentManager(), "AddSambaStorageDialogFragment");
                               if(errorMsg != null && errorMsg.contains("Failed to connect")) {
                                   ToastUtility.show(getActivity(), R.string.networkplace_access_error, Toast.LENGTH_LONG);
                               } else {
                                   ToastUtility.show(getActivity(), R.string.samba_login_error, Toast.LENGTH_LONG);
                               }
                        }
                        break;
                    case SambaMessageHandle.ADD_NEW_FOLDER:
                        if (fileListFragment != null) {
                            fileListFragment.addComplete();
                            if(errorMsg != null && errorMsg.equalsIgnoreCase("Access is denied.")) {
                                ToastUtility.show(getActivity(), R.string.permission_deny, Toast.LENGTH_LONG);
                            } else {
                                ToastUtility.show(getActivity(), R.string.new_folder_fail, Toast.LENGTH_LONG);
                            }
                        }
                        break;
                    case SambaMessageHandle.FILE_RENAME:
                        if (fileListFragment != null) {
                            fileListFragment.RenamingComplete();
                            if(errorMsg != null && errorMsg.equalsIgnoreCase("Access is denied.")) {
                                ToastUtility.show(getActivity(), R.string.permission_deny, Toast.LENGTH_LONG);
                            } else {
                                ToastUtility.show(getActivity(), R.string.rename_fail, Toast.LENGTH_LONG);
                            }
                        }
                        break;
                    case SambaMessageHandle.FILE_DELETE:
                        if (copyType > -1 && msgId != null) {
                            if (copyType == CopyTypeArgument.SAMB_TO_CLOUD) {
                                RemoteFileUtility.getInstance(getActivity()).sendDelteFileCompleteWhenCopyFileFromSambToCloud(msgId, false);
                                break;
                            }
                        }
                        if(((FileManagerActivity)getActivity()).getIsShowSearchFragment()) {
                            SearchResultFragment mSFragment = (SearchResultFragment) getActivity()
                                    .getFragmentManager().findFragmentById(R.id.searchlist);
                            if(mSFragment != null) {
                                mSFragment.deleteComplete(true);
                            }
                        } else if (fileListFragment != null) {
                            fileListFragment.deleteComplete();
                        }

                        if(errorMsg != null && errorMsg.equalsIgnoreCase("Access is denied.")) {
                            ToastUtility.show(getActivity(), R.string.permission_deny, Toast.LENGTH_LONG);
                        } else {
                            ToastUtility.show(getActivity(), R.string.delete_fail, Toast.LENGTH_LONG);
                        }
                        break;
                    case SambaMessageHandle.FILE_PASTE:
                        if (copyType > -1 && msgId != null) {
                            if (copyType == CopyTypeArgument.SAMB_TO_CLOUD) {
                                RemoteFileUtility.getInstance(getActivity()).sendCopyFileFromSambToCloudMsg(msgId, false);
                            } else if (copyType == CopyTypeArgument.CLOUD_TO_SAMB) {
                                RemoteFileUtility.getInstance(getActivity()).sendCopyFileFromSambToCloudMsg(msgId,false);
                            }
                        } else {
                            if (fileListFragment != null) {
                                fileListFragment.pasteComplete(true);
                                if(errorMsg != null && errorMsg.equals(SambaMessageHandle.NO_SPACE)) {
                                    ToastUtility.show(getActivity(),R.string.no_space_fail,Toast.LENGTH_LONG);
                                } else if(errorMsg != null && errorMsg.equalsIgnoreCase("Access is denied.")) {
                                    ToastUtility.show(getActivity(), R.string.permission_deny, Toast.LENGTH_LONG);
                                } else {
                                    ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_LONG);
                                }
                            }
                        }
                        isPasteCancel = false;
                        clearNotification(false);
                        clearEditPool();
                        break;
                    case SambaMessageHandle.FILE_OPEN:
                        if (fileListFragment != null) {
                            fileListFragment.CloudStorageLoadingComplete();
                            ToastUtility.show(getActivity(), R.string.open_fail, Toast.LENGTH_LONG);
                        }
                        break;
                    case SambaMessageHandle.FILE_SHARE:
                        if (fileListFragment != null) {
                            fileListFragment.CloudStorageLoadingComplete();
                            ToastUtility.show(getActivity(), R.string.open_fail, Toast.LENGTH_LONG);
                        }
                        break;
                }
                break;
            case SambaMessageHandle.ACTION_UPDATE:
                long currentSize = bundle.getLong(SambaMessageHandle.PASTE_PROGRESS_SIZE);
                long total_size = bundle.getLong(SambaMessageHandle.PASTE_TOTAL_SIZE);
                mTotalSize = (int)(total_size / 1024);
                mCurrentSize = (int) (currentSize / 1024);
                String fileName = bundle.getString(SambaMessageHandle.PASTE_CURRENT_FILE_NAME);
                mPastFileName = fileName;
                PasteDialogFragment pasteDialogFragment = (PasteDialogFragment) getActivity().getFragmentManager().findFragmentByTag("PasteDialogFragment");
                if (pasteDialogFragment != null) {
                    int percent = (int)(((double)currentSize / (double)total_size) * 100);
                    pasteDialogFragment.setProgress(percent, (double)currentSize, (double)total_size);
                }
                updateNotificationBar(fileName, 0, mTotalSize, mCurrentSize, false);
                break;
            }
        }
    };

    //++felix
    @SuppressWarnings("unused")
    public void operateResultOfCopyFromCloudToSamba(boolean isOk, String parentPath) {

//      if(isOk){
//          ToastUtility.show(getActivity(), R.string.paste_success, Toast.LENGTH_SHORT,parentPath);
//      }else {
        if(!isOk)
            ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
//      }

        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);

        if (fileListFragment != null) {
            fileListFragment.pasteComplete(true);
            if (parentPath != null && parentPath.startsWith(SambaMessageHandle.SMB)) {
                SambaVFile file = new SambaVFile(parentPath);
                fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
            }
        }
    }

    public void init(Activity activity) {
        mSambaMessageHandle = new SambaMessageHandle(SAMBA_HANDLER);
        mSambaMessageHandle.start();
        mSendSambaMessageHandle = new Handler(mSambaMessageHandle.getLooper(),mSambaMessageHandle);
        mSambaMessageHandle.setCallbackHandler(mCallbackHandler);
        SCAN_ROOT_PATH = File.separator + activity.getString(R.string.networkplace_storage_title) + File.separator;
    }

    @SuppressWarnings("UnusedParameters")
    public void onResume(boolean IsAttach, Activity activity) {
//      mFileServer = new FileServer();
//      mFileServer.start();
        if(mManager != null && mBuilder != null && getActivity() != null && !IsAttach){
            PasteDialogFragment pasteDialogFragment = (PasteDialogFragment) getActivity().getFragmentManager().findFragmentByTag("PasteDialogFragment");
            if (pasteDialogFragment == null) {
                pasteDialogFragment = PasteDialogFragment.newInstance(getEditPoll());
                if (pasteDialogFragment != null) {
                    pasteDialogFragment.show(getActivity().getFragmentManager(), "PasteDialogFragment");
                    int percent = (int)(((double)mCurrentSize / (double)mTotalSize) * 100);
                    pasteDialogFragment.setInitProgressByNotification(getActivity(), percent, (double)mCurrentSize, (double)mTotalSize);
                }
            }
            updateNotificationBar(mPastFileName, 0, mTotalSize, mCurrentSize, false);
        }
    }

    public void destroy(boolean IsAttach) {
        updateHostIp = false;
        if(!IsAttach) {
            mSendSambaMessageHandle.removeCallbacks(mSambaMessageHandle);
            if(mSambaMessageHandle != null) {
                mSambaMessageHandle.interrupt();
                mSambaMessageHandle = null;
            }

            stopOnlinePlayServer();
            clearNotification(true);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void sendSambaMessage(int what, SambaVFile[] array, int copyType, String msgId) {
        String parentPath;
        if (array.length > 0) {
            String[] nameList = new String[array.length];
            String indicatorPath = array[0].getIndicatorPath();
            String rootPath = getRootScanPath();
            String[] folderStrs = indicatorPath.trim().substring(1).split("/");
            String tmp = "";
            int smbcount = folderStrs.length;
            if (smbcount <= 1) {

            } else {
                for (int i=0;i < smbcount - 1;i++) {
                    tmp += (folderStrs[i] + File.separatorChar);
                }
            }
            parentPath = rootPath + tmp;
            for (int i = 0; i < array.length; i++) {
                nameList[i] = array[i].getName();
            }
            sendSambaMessage(what, parentPath, null, null, nameList, null, 0,copyType,msgId);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void sendSambaMessage(int what, VFile[] files, String destPath, boolean isDelete, int copyType, String msgId) {
        if (files != null && files.length > 0) {
            mTotalSize = 0;
            mCurrentSize = 0;
            String srcParentPath = null;
            String[] nameList = new String[files.length];
            String indicatorPath;
            if (files[0] instanceof SambaVFile) {
                indicatorPath = ((SambaVFile)files[0]).getIndicatorPath();
                String rootPath;
                String srcParent = ((SambaVFile)files[0]).getParentPath();
                for (SambaServer server : mServerList) {
                    if (files[0].getAbsolutePath().startsWith(server.getSMBURL())) {
                        rootPath = server.getSMBURL();
                        indicatorPath = files[0].getAbsolutePath().replaceFirst(rootPath, "/");
                        break;
                    }
                }
                String[] folderStrs = indicatorPath.trim().substring(1).split("/");
//              String tmp = "";
                int smbcount = folderStrs.length;
                if (smbcount <= 1) {

                } else {
//                  for (int i=0;i < smbcount - 1;i++){
//                      tmp += (folderStrs[i] + File.separatorChar);
//                  }
                }
//              srcParentPath = rootPath + tmp;
                srcParentPath = srcParent;
            } else if (files[0] instanceof LocalVFile) {
                srcParentPath = files[0].getParent();
            }

            for (int i = 0; i < files.length; i++) {
                nameList[i] = files[i].getName();
                mTotalSize += files[i].length();
            }

            sendSambaMessage(what, srcParentPath, null, null, nameList, destPath, isDelete ? SambaMessageHandle.CUT:SambaMessageHandle.COPY, copyType, msgId);
        }
    }

    public void sendSambaMessage(int what, String path, String name, String orName, String[] array, String destPath, int type, int copyType, String msgId) {
        Bundle bundle = new Bundle();
        Message msg = new Message();
        bundle.putString(SambaMessageHandle.SOURCE_PARENT_PATH, path);
        bundle.putString(SambaMessageHandle.NEW_NAME, name);
        switch (what) {
            case SambaMessageHandle.MSG_SAMBA_LOGIN:
                break;
            case SambaMessageHandle.ADD_NEW_FOLDER:
                msg.what = SambaMessageHandle.ADD_NEW_FOLDER;
                break;
            case SambaMessageHandle.FILE_RENAME:
                bundle.putString(SambaMessageHandle.SELECT_NAME, orName);
                msg.what = SambaMessageHandle.FILE_RENAME;
                break;
            case SambaMessageHandle.FILE_DELETE:
                msg.what = SambaMessageHandle.FILE_DELETE;
                bundle.putStringArray(SambaMessageHandle.SELECT_NAME_LIST, array);
                if (copyType>0) {
                    bundle.putInt(SambaMessageHandle.SAMBA_REMOTE_PASTE, copyType);
                }
                if (msgId != null && msgId.length()>0) {
                    bundle.putString(SambaMessageHandle.MSGOBJ_ID, msgId);
                }
                break;
            case SambaMessageHandle.FILE_PASTE:
                msg.what = SambaMessageHandle.FILE_PASTE;
                bundle.putStringArray(SambaMessageHandle.SELECT_NAME_LIST, array);
                bundle.putString(SambaMessageHandle.DEST_PARENT_PATH, destPath);
                bundle.putInt(SambaMessageHandle.PASTE_TYPE, type);
                if (copyType>0) {
                    bundle.putInt(SambaMessageHandle.SAMBA_REMOTE_PASTE, copyType);
                }
                if (msgId != null && msgId.length()>0) {
                    bundle.putString(SambaMessageHandle.MSGOBJ_ID, msgId);
                }
                isPasteCancel = false;
                initNotificationBar();
                break;
            case SambaMessageHandle.FILE_PASTE_CANCEL:
                isPasteCancel = true;
                clearNotification(true);
                break;
            case SambaMessageHandle.FILE_OPEN:
                msg.what = SambaMessageHandle.FILE_OPEN;
                bundle.putStringArray(SambaMessageHandle.SELECT_NAME_LIST, array);
                bundle.putString(SambaMessageHandle.DEST_PARENT_PATH, destPath);
                bundle.putInt(SambaMessageHandle.PASTE_TYPE, type);
                break;
            case SambaMessageHandle.FILE_SHARE:
                msg.what = SambaMessageHandle.FILE_SHARE;
                bundle.putStringArray(SambaMessageHandle.SELECT_NAME_LIST, array);
                bundle.putString(SambaMessageHandle.DEST_PARENT_PATH, destPath);
                bundle.putInt(SambaMessageHandle.PASTE_TYPE, type);
                break;
            case SambaMessageHandle.FILE_DOWNLOAD:
                msg.what = SambaMessageHandle.FILE_DOWNLOAD;
                bundle.putStringArray(SambaMessageHandle.SELECT_NAME_LIST, array);
                bundle.putString(SambaMessageHandle.DEST_PARENT_PATH, destPath);
                bundle.putInt(SambaMessageHandle.PASTE_TYPE, type);
                break;
        }

        if (!isPasteCancel) {
            msg.setData(bundle);
            if(msg.what != SambaMessageHandle.FILE_DOWNLOAD)
                mSendSambaMessageHandle.sendMessageAtFrontOfQueue(msg);
            else
                mSendSambaMessageHandle.sendMessage(msg);
        }
    }

    public boolean getIsPasteCancel() {
        return isPasteCancel;
    }

     @SuppressWarnings("unused")
     void cancelPaste() {
        SambaMessageHandle handlethread = new SambaMessageHandle(SAMBA_HANDLER);
        handlethread.start();
        Handler handler = new Handler(handlethread.getLooper(),handlethread);
        Bundle bundle = new Bundle();
        Message msg = new Message();
        msg.what = SambaMessageHandle.FILE_PASTE_CANCEL;
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    public void LoginInWindowsServer(String domain, String pcname, String ip, String username, String password, int what, boolean save) {
        SambaServer sambaserver = new SambaServer(domain,ip,username,password);
        mRootSambaServer = sambaserver;
        String LoginUrl = sambaserver.getSMBURL();
        Bundle bundle = new Bundle();
        bundle.putString(SambaMessageHandle.LOGIN_URL, LoginUrl);
        Message msg = new Message();
        msg.what = what;
        msg.setData(bundle);
        mSendSambaMessageHandle.sendMessage(msg);
        refresFileList(false);
        if(save) {
            mSelectItem = null;
            mSelectItem = new SambaItem(pcname,ip,username,password);
        }
        prepareStoreLoginIp(ip);
    }

    public void checkSavedMsgIfChanged() {
        String LoginUrl = mRootSambaServer.getSMBURL();
        Bundle bundle = new Bundle();
        bundle.putString(SambaMessageHandle.LOGIN_URL, LoginUrl);
        Message msg = new Message();
        msg.what = SambaMessageHandle.MSG_SAMBA_LOGIN;
        msg.setData(bundle);
        mSendSambaMessageHandle.sendMessage(msg);
        refresFileList(false);
    }

    public void refresFileList(boolean isrefres) {
        if(getActivity() == null) {
            return;
        }
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()) {
            MoveToDialogFragment moveToFragment = (MoveToDialogFragment) getActivity()
                    .getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
            moveToFragment.setListShown(isrefres);
        } else if (fileListFragment != null) {
            fileListFragment.setListShown(isrefres);
        }
    }

    public void startScanSambaServerFile() {
        SambaServer sambaserver = mRootSambaServer;
        mServerList.add(sambaserver);
        setRootScanPath(sambaserver.getSMBURL());
        mUserName = sambaserver.getUsername();
        SambaVFile file = new SambaVFile(getRootScanPath());
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()) {
            MoveToDialogFragment moveToFragment = (MoveToDialogFragment) getActivity()
                    .getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
            moveToFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
        } else if (fileListFragment != null) {
            fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
        }
    }

    public void setRoot(SmbFile file) {
        try {
            mRootFile = new SambaVFile(file.getPath(), file.isDirectory(), file.getContentLength(),
                    file.getParent(), file.getName(), file.getLastModified());
        } catch (SmbException e) {
            e.printStackTrace();
        }
    }

    public void setSambaScanPath() {
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()) {
            MoveToDialogFragment moveToFragment = (MoveToDialogFragment) getActivity()
                    .getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
            moveToFragment.SetScanHostIndicatorPath(SambaFileUtility.SCAN_ROOT_PATH);
            moveToFragment.setmIndicatorFile(new SambaVFile(SambaFileUtility.SCAN_ROOT_PATH));
        } else if (fileListFragment != null) {
            fileListFragment.SetScanHostIndicatorPath(SambaFileUtility.SCAN_ROOT_PATH);
            // Dont't erase the last path (homepage)
            // fileListFragment.setmIndicatorFile(new SambaVFile(SambaFileUtility.SCAN_ROOT_PATH));
        }
    }

    public void updateListViewByHostPc(ArrayList<SambaItem> hostIp) {
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()) {
            MoveToDialogFragment moveToFragment = (MoveToDialogFragment) getActivity()
                    .getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
            moveToFragment.ScanHostPcFinish(hostIp);
        } else if (fileListFragment != null) {
            fileListFragment.ScanHostPcFinish(hostIp);
        }
    }

    public SambaVFile getRoot() {
        return mRootFile;
    }

    public String getRootScanPath() {
        return mScanPath;
    }

    public void setRootScanPath(String path) {
        mScanPath = path;
    }

    public String getPcUserName() {
        return mUserName;
    }

    private void StoreSuccessHostInfo() {
        PcInfoDbHelper.saveAccountInfo(mSelectItem);
        if(mEdit != null) {
            mEdit.commit();
        }
        mEdit = null;
        mSelectItem = null;
    }

    @SuppressLint("CommitPrefEdits")
    private void prepareStoreLoginIp(String ip) {
        SharedPreferences Spf = getActivity().getSharedPreferences("MyPrefsFile", 0);
        if(Spf == null) {
            return;
        }
        mEdit = Spf.edit();
        mEdit.putString("samba_host_ip", ip);
    }


    public String getTheLastTimeLoginIp() {
        String ip = "";
        SharedPreferences Spf = getActivity().getSharedPreferences("MyPrefsFile", 0);
        if(Spf != null) {
            ip = Spf.getString("samba_host_ip", "");
        }
        return ip;
    }

    public boolean LastTimeLoginSuccess(SambaItem item) {
        boolean hasRecord = false;
        String ip = item.getIpAddress();
        String username = item.getAccount();
        String password = item.getPassword();

        if(!TextUtils.isEmpty(username)) {
            mRootSambaServer = new SambaServer("",ip,username,password);
            prepareStoreLoginIp(ip);
            hasRecord = true;
        }

        return hasRecord;
    }

    public void setDeviceListAdapter() {
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()) {
            MoveToDialogFragment moveToFragment = (MoveToDialogFragment) getActivity()
                    .getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
            moveToFragment.setDeviceListAdapter();
        } else {
            if(fileListFragment != null) {
                fileListFragment.setDeviceListAdapter();
                fileListFragment.setDeviceType(DeviceListAdapter.DEVICE_SAMAB);
            }
        }
    }

    public void startScanNetWorkDevice(boolean forceRefresh) {
        if(getActivity() == null) {
            Log.d(TAG,"==startScanNetWorkDevice=mActivity == null=");
            return;
        }

        if(!((FileManagerApplication)getActivity().getApplication()).isWifiConnected()) {
            ((FileManagerActivity)getActivity()).displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
            return;
        }

        if(!((FileManagerActivity)getActivity()).isShowFileListFragment()) {
            ((FileManagerActivity)getActivity()).showSearchFragment(FragmentType.NORMAL_SEARCH, false);
        }
        updateHostIp = true;
        setDeviceListAdapter();
        if(!((FileManagerActivity)getActivity()).isMoveToDialogShowing()) {
            ((FileManagerActivity)getActivity()).setActionBarTitle(
                    getActivity().getResources().getString(R.string.networkplace_storage_title));
        }
        setSambaScanPath();
        ScanLanNetworkPC scanThread = ScanLanNetworkPC.getInstance(getActivity().getApplicationContext());
        if(scanThread.isNeedToRescanPc() || forceRefresh) {
            ScanFinish = false;
            refresFileList(false);
            scanThread.startScanPc();
            // Change a new way.
            // scanThread.startScanPc(SAMBAHEADER);
        } else {
            scanThread.updateLastLoginAccount();
            scanThread.updateListAdapter();
        }
    }

    public void hideSambaDeviceListView() {
        updateHostIp = false;
    }

    public boolean isIpSameArea(String savedIp) {
        WifiAdmin mAdmin = new WifiAdmin(getActivity());
        String current = mAdmin.getCurrentIPAddress();
        boolean same = false;
        try {
            if(TextUtils.isEmpty(savedIp)) {
                return false;
            }
            if(!TextUtils.isEmpty(savedIp) && !TextUtils.isEmpty(current)) {
                int index = current.lastIndexOf(".");
                int wIndex = savedIp.lastIndexOf(".");
                String subPad = current.substring(0, index);
                String subWin = savedIp.substring(0, wIndex);
                if(subPad.equals(subWin)) {
                    same = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return same;
    }

    public void hideRefresh() {
        if(getActivity() == null || getActivity().getFragmentManager() == null ) {
            return;
        }
        FileListFragment fileListFragment = (FileListFragment) getActivity()
                .getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null) {
            fileListFragment.hideRefresh();
        }
    }

    public boolean isMediaFile(String filePath) {
        filePath = filePath.toLowerCase();
        boolean isMediaFile = false;
        if(filePath.endsWith(".mp3") || filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
            isMediaFile = true;
        }
        return isMediaFile;
    }

    public String getFileType(String uri)
    {
        if (uri == null)
        {
            return FILE_TYPE;
        }

        uri = uri.toLowerCase();
        if (uri.endsWith(".mp3"))
        {
            return "audio/mpeg";
        }

        if (uri.endsWith(".mp4"))
        {
            return "video/mp4";
        }

        return FILE_TYPE;
    }

    public void playMediaFileOnLine(String filePath) {
        String httpReq = "http://" + HTTP_IP + ":" + HTTP_PORT + "/smb=";

        SelectFilePath = filePath;
        int index = filePath.lastIndexOf(File.separator);
        String fileName = filePath.substring(index + 1);
//      try {
//          path = URLEncoder.encode(path, "UTF-8");
//      } catch (UnsupportedEncodingException e) {
//          e.printStackTrace();
//      }

        String url = httpReq + fileName;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(url);
        if(fileName.endsWith(".mp3")) {
            intent.setDataAndType(uri, "audio/mp3");
        } else if(filePath.endsWith(".mp4")) {
            intent.setDataAndType(uri, "video/mp4");
        } else {
            intent.setDataAndType(uri, "video/*");
        }
        try {
            getActivity().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException: playMediaFileOnLine failed");
            ToastUtility.show(getActivity(), R.string.open_fail);
        }
    }

    public static MsgObj mSelectMsgObj = null;

    @SuppressWarnings("ConstantConditions")
    public void playCloudHttpsMediaFile(MsgObj fileObj) {

        Uri uri;
        mSelectMsgObj = fileObj;
        String filename = fileObj.getFileObjPath().getFileName();
        Intent intent = new Intent(Intent.ACTION_VIEW);

        if(fileObj.getStorageObj().getStorageType() == MsgObj.TYPE_DROPBOX_STORAGE) {
            uri = Uri.parse(fileObj.getFileObjPath().getSourceUri());
        } else {
            String httpReq = "http://" + HTTP_IP + ":" + HTTP_PORT + "/cloud=";
            String cloudType = getStorageNameByType(fileObj.getStorageObj().getStorageType());
            String Reurl = httpReq + cloudType + filename;
            uri = Uri.parse(Reurl);
        }

        String fileExtension = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
        boolean hasMimeType = false;
        if (fileExtension != null) {
            fileExtension = fileExtension.toLowerCase();
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            Log.d(TAG,"===mimeType===" + mimeType);
            if(mimeType != null){
                intent.setDataAndType(uri, mimeType);
                hasMimeType = true;
            }
        } else {
            String mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(filename);
            if(mediaFile_mime != null) {
                Log.d(TAG,"===mediaFile_mime===" + mediaFile_mime);
                intent.setDataAndType(uri, mediaFile_mime);
                hasMimeType = true;
            }
        }

        if(!hasMimeType) {
            ToastUtility.show(getActivity(), R.string.open_fail);
            return;
        }

        intent.putExtra(Intent.EXTRA_TITLE, filename);

        if (getActivity() != null ) {
            try {
             Log.d(TAG, "" + intent);
                getActivity().startActivity(intent);
            } catch (ActivityNotFoundException  e) {
                ToastUtility.show(getActivity(), R.string.open_fail);
            }
        }
    }

    private String getStorageNameByType(int type) {
        String name = "";
        switch(type) {
            case MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE:
                name = "ASUSWebStorage/";
                break;
            case MsgObj.TYPE_GOOGLE_DRIVE_STORAGE:
                name = "Drive/";
                break;
            case MsgObj.TYPE_HOMECLOUD_STORAGE:
                name = "ASUSHomeCloud/";
                break;
            case MsgObj.TYPE_SKYDRIVE_STORAGE:
                name = "OneDrive/";
                break;
            case MsgObj.TYPE_BAIDUPCS_STORAGE:
                name = "BaiDuCloud/";
                break;
        }
        return name;
    }

    public void tryToLoginWithoutPassword(SambaItem item) {
        setIsLoginProcessing(true);
        SambaServer sambaserver = new SambaServer("", item.getIpAddress(), item.getPcName(), "");
        mRootSambaServer = sambaserver;
        final String url = sambaserver.getSMBURL();
        Bundle bundle = new Bundle();
        bundle.putString(SambaMessageHandle.LOGIN_URL, url);
        Message msg = new Message();
        msg.what = SambaMessageHandle.LOGIN_WITHOUT_PASSWORD;
        msg.setData(bundle);
        mSendSambaMessageHandle.sendMessage(msg);
        refresFileList(false);
    }

    public boolean isActivityOnTheFront(Context context) {
        boolean isRunning = false;
        String mPkgName = "";
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasklist = am.getRunningTasks(1);
        if(tasklist != null) {
            RunningTaskInfo info = tasklist.get(0);
            if (info!=null && info.topActivity != null) {
                mPkgName = info.topActivity.getPackageName();
            }
        }
        if(mPkgName.startsWith(FM_AP_PACKAGE)) {
            isRunning = true;
        }
        return isRunning;
    }

    public SmbFile[] fileterSystemFileForSamba(SmbFile[] result) {
        ArrayList<SmbFile> Files = new ArrayList<>();
        SmbFile[] subFiles = null;

        for(int i = 0;i < result.length ; i++) {
            String name = result[i].getName();
            if(!name.contains("$")) {
                Files.add(result[i]);
            }
        }

        if(Files.size() > 0) {
            subFiles = new SmbFile[Files.size()];
            for(int num = 0; num < Files.size(); num ++) {
                subFiles[num] = Files.get(num);
            }
        }
        return subFiles;
    }

    private void initNotificationBar() {
        if(mManager == null || mBuilder == null) {
            mManager = (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new Builder(getActivity());
        }

        Intent mShowDialogIntent = new Intent();
        mShowDialogIntent.setAction(Intent.ACTION_MAIN);
        mShowDialogIntent.setClass(getActivity(), FileManagerActivity.class);
        mShowDialogIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        PendingIntent mIntent = PendingIntent.getActivity(getActivity(), 0, mShowDialogIntent, 0);

        mBuilder.setSmallIcon(R.drawable.asus_ic_network_place)
                .setProgress(100, 0, false)
                .setContentTitle(getActivity().getResources().getString(R.string.paste_progress))
                .setContentIntent(mIntent);
        mManager.notify(SAMBA_NOTIFICATION_ID, mBuilder.build());
    }

    private void clearNotification(boolean success) {
        if((mManager == null || mBuilder == null) && getActivity() != null) {
            mManager = (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new Builder(getActivity());
        }
        if(!success) {
            mBuilder.setSubText(getActivity().getResources().getString(R.string.paste_fail));
        }
        if(mManager != null) {
            mManager.cancel(SAMBA_NOTIFICATION_ID);
        }
        mManager = null;
        mBuilder = null;
    }

    @SuppressWarnings("UnusedParameters")
    private void updateNotificationBar(String fileName, int type, int total, int currentSize, boolean change) {
        if(change && mManager != null && mBuilder != null) {
            mManager.cancel(SAMBA_NOTIFICATION_ID);
            Intent mShowDialogIntent = new Intent();
            mShowDialogIntent.setAction(Intent.ACTION_MAIN);
            mShowDialogIntent.setClass(getActivity(), FileManagerActivity.class);
            mShowDialogIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            PendingIntent mIntent = PendingIntent.getActivity(getActivity(), 0, mShowDialogIntent, 0);

            mManager = (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new Builder(getActivity());
            mBuilder.setSmallIcon(R.drawable.asus_ic_network_place)
                    .setContentIntent(mIntent);
        }

        if(mManager == null || mBuilder == null) {
            mManager = (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new Builder(getActivity());
            mBuilder.setSmallIcon(R.drawable.asus_ic_network_place);
        }
        mBuilder.setContentTitle(fileName)
                .setProgress(total, currentSize, false)
                .setContentText(getActivity().getResources().getString(R.string.paste_progress));
        mManager.notify(SAMBA_NOTIFICATION_ID, mBuilder.build());
    }

    public void restorePasteEditPool(EditPool pool) {
        if(mPasteEditPool == null){
            mPasteEditPool = new EditPool();
        }
        mPasteEditPool = pool;
    }

    public void clearEditPool() {
        if(mPasteEditPool != null) {
            mPasteEditPool.clear();
        }
    }

    private EditPool getEditPoll() {
        return mPasteEditPool;
    }

    public void setIsLoginProcessing(boolean isProcessing) {
        mLoginInIsProcessing = isProcessing;
    }

    public boolean getIsLoginProcessing() {
        return mLoginInIsProcessing;
    }

    public void startOnlinePlayServer() {
        if(mFileServer == null) {
            mFileServer = new FileServer();
            mFileServer.start();
        }
    }

    public void stopOnlinePlayServer() {
        if(mFileServer != null) {
            HTTPServerList httpServerList = mFileServer.getHttpServerList();
            httpServerList.stop();
            httpServerList.close();
            httpServerList.clear();
            mFileServer.interrupt();
            mFileServer = null;
        }
    }

    public void showLoginDialog() {
        AddSambaStorageDialogFragment loginDialog = AddSambaStorageDialogFragment.newInstance();
        if(getActivity() != null && isActivityOnTheFront(getActivity())) {
            loginDialog.show(getActivity().getFragmentManager(), "AddSambaStorageDialogFragment");
        }
    }

    public boolean isLoginAnonymous() {
        boolean result = true;
        if(getRoot() == null)
            return true;

        String path = getRoot().getAbsolutePath();
        if(path.contains("@")) {
            result = false;
        }
        return result;
    }

    public boolean isBuilderEmpty() {
        return mBuilder == null;
    }
}
