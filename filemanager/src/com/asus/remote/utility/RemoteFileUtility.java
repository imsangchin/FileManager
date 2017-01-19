package com.asus.remote.utility;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.editor.EditResult;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.PathIndicator;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.service.cloudstorage.common.AWSMsgObjHelper;
import com.asus.service.cloudstorage.common.HandlerCommand;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.HandlerCommand.CopyArgument;
import com.asus.service.cloudstorage.common.HandlerCommand.DropBoxThumbnailSize;
import com.asus.service.cloudstorage.common.HandlerCommand.SkyDriveThumbnailSize;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.common.MsgObj.FileObj;
import com.asus.service.cloudstorage.common.MsgObj.StorageObj;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class RemoteFileUtility {
    private static final boolean DEBUG = false;
    public static final String TAG = "RemoteFileUtility";
    public static final String appName = "FileManager";


    private String mConnectedWifiDirectStorageName = ""; // used for saving connected remote storage
    private String mConnectedWifiDirectStorageAddress = "";
    private String mCurrentCloudStorageId = ""; // used for saving the current cloud storage
    private String mCurrentAccount = "";
    private String mCurrentToken = null;
    private String mHomeCloudRootPath = "";
    public static final String REMOTE_PREVIEW_ACTION = "remote_preview_action";
    public static final String REMOTE_SHARE_ACTION = "remote_share_action";
    public static final String REMOTE_COPY_TO_REMOTE_ACTION = "remote_copy_to_remote_action";


    public RemoteVFile[] mRemoteFileListForThumbnail = null;
    public RemoteVFile[] mRemoteFolderList = null;
    private RemoteVFile[] mRemoteFileList = null;

    public LinkedList<RemoteDataEntry> mRemoteUpdateUI = new LinkedList<RemoteDataEntry>();
    public ArrayList<ThumbnailEntry> mThumbnailEntries = new ArrayList<ThumbnailEntry>();
    private ArrayList<cloudStorage> cloudStorageList = new ArrayList<cloudStorage>();

    //fixed felix_zhang
    public Map<String, RemoteVFile[]> mRemoteFileListMap = new HashMap<String, RemoteVFile[]>();
    public HashMap<String, RemoteDataEntry> mRemoteUpdateUIMap = new HashMap<String, RemoteDataEntry>();

    public WeakReference<Activity> mActivity;
    public MsgObj currentOperateMsgObj = null;
    public MsgObjBackUpEntry currentBackUpMsgObj = null;
    public RemoteVFile mCurrentIndicatorVFile;

    private static int mCurrentCloudStorageType = -1;
    private static int mListUIAction = -1;

    public static boolean mRemoteFileListError = false;
    public static boolean mRemoteAccessPermisionDeny = false;
    public static boolean mHomeCloudFileListError = false;
    public static boolean isShowDevicesList = false;

    private static RemoteFileUtility remoteFileUtility;

    private RemoteFileUtility() {

    }

    public static RemoteFileUtility getInstance(Activity mActivity) {
        if (remoteFileUtility == null)
            remoteFileUtility = new RemoteFileUtility();
        if (mActivity != null)
            remoteFileUtility.setActivity(mActivity);
        return remoteFileUtility;
    }

    private void setActivity(Activity mActivity) {
        this.mActivity = new WeakReference<Activity>(mActivity);
    }

    private Activity getActivity() {
        if (mActivity != null)
            return mActivity.get();
        return null;
    }

    public static class cloudStorage {
        public String cloudStorageName = "";
        public String cloudStorageId = "";
        public int cloudStorageType = -1;
        public boolean isExpandMyFavorit = false;

        public cloudStorage(String storageName, String storageAddress, int storageType) {
            cloudStorageName = storageName;
            cloudStorageId = storageAddress;
            cloudStorageType = storageType;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof cloudStorage)) return false;
            cloudStorage entry = (cloudStorage) object;
            if (cloudStorageType == entry.cloudStorageType && cloudStorageId.equals(entry.cloudStorageId)
                    && cloudStorageName.equals(entry.cloudStorageName)) {
                return true;
            } else {
                return false;
            }
        }

        public void setExpandMyFavorite(boolean value) {
            isExpandMyFavorit = value;
        }

        public boolean isExpandMyFavorite() {
            return isExpandMyFavorit;
        }
    }

    public static class ThumbnailEntry {
        private int mType;
        private String mAccountId;
        private String mFileName;
        private String mFileParentPath;

        public ThumbnailEntry(int type, String accountId, String fileName, String fileParentPath) {
            mType = type;
            mAccountId = accountId;
            mFileName = fileName;
            mFileParentPath = fileParentPath;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ThumbnailEntry)) return false;
            ThumbnailEntry entry = (ThumbnailEntry) object;
            if (mType == entry.mType && mAccountId.equals(entry.mAccountId) && mFileName.equals(entry.mFileName)
                    && mFileParentPath.equals(entry.mFileParentPath)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static class RemoteUIAction {
        public static final int FOLDER_LIST_UI = 0;
        public static final int FILE_LIST_UI = 1;
        public static final int FOLDER_CHILD_LIST_UI = 2;
        public static final int FILE_FOLDER_LIST_UI = 3; // update file and folder list at the same time
    }

    public void deleteFileAndPath(File file) {
        if (file != null && file.exists()) {
            VFile delFile = new VFile(file);
            EditorUtility.doDeleteFile(delFile, false);
            //     if (file.isDirectory()) {
            //       File[] tempFiles = file.listFiles();
            //       for (File temp : tempFiles) {
            //         deleteFileAndPath(temp);
            //       }
            //
            //       if(delFile.delete()){
            //         MediaProviderAsyncHelper.deleteFile(delFile);
            //       }
            //     }else {
            //       if(delFile.delete()){
            //         MediaProviderAsyncHelper.deleteFile(delFile);
            //       }
            //     }
        }
    }

    public RemoteVFile[] getRemoteFileList() {
        return mRemoteFileList;
    }

    public void clearRemoteFileList() {
        mRemoteFileList = null;
    }

    public RemoteVFile[] getRemoteFolderList() {
        return mRemoteFolderList;
    }

    public void clearRemoteFolderList() {
        mRemoteFolderList = null;
    }

    public void sendRemoteMessage(VFile vfile, int what) {
        StorageObj storageObj;
        FileObj fileObj;
        MsgObj msgObj;
        String deviceName = "";
        String deviceAddress = "";

        switch (what) {
            case CloudStorageServiceHandlerMsg.MSG_APP_UPDATE_DEVICE:
            case CloudStorageServiceHandlerMsg.MSG_APP_START_DISCOVER_SERVICE:
            case CloudStorageServiceHandlerMsg.MSG_APP_STOP_DISCOVER_SERVICE:
                Log.d(TAG, "sendRemoteMessage: " + what);
                try {
                    FileManagerActivity aActivity = (FileManagerActivity) getActivity();
                    Messenger remoteService = aActivity.getRemoteService();
                    if (remoteService != null) {
                        Message msg = Message.obtain(null, what);
                        msg.replyTo = ((FileManagerActivity) getActivity()).getRemoteClientMessenger();
                        remoteService.send(msg);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE:
                Log.d(TAG, "sendRemoteMessage MSG_APP_CONNECT_DEVICE");

                if (vfile != null) {
                    if (vfile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
                        deviceName = ((RemoteVFile) vfile).getStorageName();
                        deviceAddress = ((RemoteVFile) vfile).getStorageAddress();
                    }
                    Log.d(TAG, "try to connect device -> deviceName: " + deviceName + " deviceAddress: " + deviceAddress);
                    storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, deviceName, deviceAddress, StorageObj.State.CONNECTED, mCurrentToken);

                    // try to connect device, some of file info are fake because we don't have their info
                    fileObj = new FileObj(vfile.getName(), ((RemoteVFile) vfile).removeStorageNameParentPath(deviceName)
                            , true, 4096, 900000000, "DRW", true);
                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjPath(fileObj);

                    deliverRemoteMsg((MsgObj) msgObj, what);
                } else {
                    Log.d(TAG, "vfile is null when sending MSG_APP_CONNECT_DEVICE");
                }

                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_DISCONNECT_DEVICE:
                Log.d(TAG, "sendRemoteMessage MSG_APP_DISCONNECT_DEVICE");

                // make sure the connected remote storage exist
                if (!mConnectedWifiDirectStorageName.isEmpty()) {
                    Log.d(TAG, "disconnect connect -> deviceName: " + mConnectedWifiDirectStorageName + " deviceAddress: " + mConnectedWifiDirectStorageAddress);
                    // clear saved state
                    mConnectedWifiDirectStorageName = "";
                    mConnectedWifiDirectStorageAddress = "";
                    // ---

                    storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, mConnectedWifiDirectStorageName, mConnectedWifiDirectStorageAddress, StorageObj.State.CONNECTED, mCurrentToken);
                    msgObj = new MsgObj(storageObj);

                    deliverRemoteMsg((MsgObj) msgObj, what);
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_CANCEL_CONNECT_DEVICE:
                Log.d(TAG, "sendRemoteMessage MSG_APP_CANCEL_CONNECT_DEVICE");

                if (vfile != null) {
                    if (vfile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
                        deviceName = ((RemoteVFile) vfile).getStorageName();
                        deviceAddress = ((RemoteVFile) vfile).getStorageAddress();
                    }
                    Log.d(TAG, "cancel connect -> deviceName: " + deviceName + " deviceAddress: " + deviceAddress);
                    storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, deviceName, deviceAddress, StorageObj.State.CONNECTED, mCurrentToken);

                    // try to disconnect device, file info is not important thus we sent fake info
                    fileObj = new FileObj(vfile.getName(), ((RemoteVFile) vfile).removeStorageNameParentPath(deviceName)
                            , true, 4096, 900000000, "DRW", true);
                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjPath(fileObj);

                    deliverRemoteMsg((MsgObj) msgObj, what);
                } else {
                    Log.d(TAG, "vfile is null when sending MSG_APP_CANCEL_CONNECT_DEVICE");
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FOLDER_LIST:
                Log.d(TAG, "sendRemoteMessage MSG_APP_REQUEST_FOLDER_LIST");

                //Log.d(TAG, "request file list: " + vfile.getAbsolutePath());
                Log.d(TAG, "request file list -> deviceName: " + mConnectedWifiDirectStorageName + " deviceAddress: " + mConnectedWifiDirectStorageAddress);
                deviceName = mConnectedWifiDirectStorageName;
                deviceAddress = mConnectedWifiDirectStorageAddress;
                storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, deviceName, deviceAddress, StorageObj.State.CONNECTED, mCurrentToken);
                // Now file size is fake because we don't query the detailed information
                fileObj = new FileObj(vfile.getName(), ((RemoteVFile) vfile).removeStorageNameParentPath(deviceName),
                        vfile.isDirectory(), 4096, vfile.lastModified(), ((RemoteVFile) vfile).getPermission(), ((RemoteVFile) vfile).isHasChild());
                msgObj = new MsgObj(storageObj);
                msgObj.setFileObjPath(fileObj);

                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_THUMBNAIL:
                Log.d(TAG, "sendRemoteMessage MSG_APP_REQUEST_FILE_THUMBNAIL");

                //Log.d(TAG, "request file thumbnail: " + vfile.getAbsolutePath());
                Log.d(TAG, "request file thumbnail -> deviceName: " + mConnectedWifiDirectStorageName + " deviceAddress: " + mConnectedWifiDirectStorageAddress);
                deviceName = mConnectedWifiDirectStorageName;
                deviceAddress = mConnectedWifiDirectStorageAddress;

                storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, deviceName, deviceAddress, StorageObj.State.CONNECTED, mCurrentToken);
                fileObj = new FileObj(vfile.getName(), ((RemoteVFile) vfile).removeStorageNameParentPath(deviceName),
                        vfile.isDirectory(), 4096, vfile.lastModified(), ((RemoteVFile) vfile).getPermission(), ((RemoteVFile) vfile).isHasChild());
                msgObj = new MsgObj(storageObj);
                msgObj.setFileObjPath(fileObj);

                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL:
                Log.d(TAG, "sendRemoteMessage MSG_APP_COPY_FILE_CANCEL");
                deviceName = mConnectedWifiDirectStorageName;
                deviceAddress = mConnectedWifiDirectStorageAddress;
                storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, deviceName, deviceAddress, StorageObj.State.CONNECTED, mCurrentToken);
                msgObj = new MsgObj(storageObj);

                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_INFO:
                Log.d(TAG, "sendRemoteMessage MSG_APP_REQUEST_FILE_INFO");

                deviceName = mConnectedWifiDirectStorageName;
                deviceAddress = mConnectedWifiDirectStorageAddress;
                Log.d(TAG, "get file info thus send deviceName: " + deviceName + " deviceAddress: " + deviceAddress);

                storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, deviceName, deviceAddress, StorageObj.State.CONNECTED, mCurrentToken);
                fileObj = new FileObj(vfile.getName(), ((RemoteVFile) vfile).removeStorageNameParentPath(deviceName),
                        vfile.isDirectory(), 4096, vfile.lastModified(), ((RemoteVFile) vfile).getPermission(), ((RemoteVFile) vfile).isHasChild());
                msgObj = new MsgObj(storageObj);
                msgObj.setFileObjPath(fileObj);

                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
        }
    }

    public void deliverRemoteMsg(MsgObj msgObj, int what, boolean isSaveToMsgMap, boolean isNeedAppendUUID, boolean isNeedChangeCurrentMsgObj) {
        msgObj = appendMsgIDOtMsgObj(msgObj, isSaveToMsgMap, isNeedAppendUUID);
        if (isNeedChangeCurrentMsgObj) {
            currentOperateMsgObj = msgObj;
            Log.d(TAG, "currentOperateMsgObj msgid:" + msgObj.getMsgId());
        }

        deliverRemoteMsg(msgObj, what);
    }

    public void deliverRemoteMsg(MsgObj msgObj, int what) {
        Log.d(TAG, "deliverRemoteMsg");
        Message msg = Message.obtain(null, what);
        if (msg != null) {
            try {
                Bundle data = new Bundle();
                msgObj.setAppName(getActivity().getResources().getString(R.string.file_manager));
                data.putParcelable(HandlerCommand.BUNDLE_KEY_MSGOBJ, msgObj);
                msg.replyTo = ((FileManagerActivity) getActivity()).getRemoteClientMessenger();
                msg.setData(data);
                Log.i(TAG + "felix_zhang client replyTo", String.valueOf(msg.replyTo));
                Messenger cloudServiceMessenger = ((FileManagerActivity) getActivity()).getRemoteService();
                if (cloudServiceMessenger != null) {
                    currentBackUpMsgObj = new MsgObjBackUpEntry(msgObj, what);
                    if ((what == CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_URI || what == CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_STORAGE_USAGE) &&
                            msgObj.getStorageObj() != null && msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_GOOGLE_DRIVE_STORAGE &&
                            !RemoteAccountUtility.isTokenFromRefresh) {
                        //.refreshDriveToken(msgObj.getStorageObj().getStorageType(),msgObj.getStorageObj().getStorageName());
                        new RefreshDriveTokenThread(cloudServiceMessenger, msg, msgObj).start();
                    } else {
                        Log.d(TAG, "cloudServiceMessenger msgobjid:" + msgObj.getMsgId());
                        cloudServiceMessenger.send(msg);
                    }
                } else {
                    Log.d(TAG, "cloudServiceMessenger is null, waiting 2 seconds for Cloudstorage ini...");
                    WaiteCloudStorageTask waiteCloudStorageTask = new WaiteCloudStorageTask(msg);
                    waiteCloudStorageTask.execute();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Message is null when sending remote message: " + what);
        }
    }

    static class RefreshDriveTokenThread extends Thread {
        private Messenger mCloudServiceMessenger;
        private Message mMsg;
        private MsgObj mMsgObj;

        public RefreshDriveTokenThread(Messenger cloudServiceMessenger, Message msg, MsgObj msgObj) {
            mCloudServiceMessenger = cloudServiceMessenger;
            mMsg = msg;
            mMsgObj = msgObj;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            String token = null;
            if (mMsgObj.getStorageObj() != null && mMsgObj.getStorageObj().getAccount() != null) {
                token = (String) mMsgObj.getStorageObj().getAccount();
            }

            if (RemoteAccountUtility.getInstance(null).isAvailableDriveToken(token)) {
                Log.d(TAG, "drive token is available");
                try {
                    mCloudServiceMessenger.send(mMsg);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "drive token is expired");
                RemoteAccountUtility.getInstance(null).refreshDriveToken(mMsgObj.getStorageObj().getStorageType(), mMsgObj.getStorageObj().getStorageName());
            }
        }

    }

    static class WaiteCloudStorageTask extends AsyncTask<Void, Integer, Integer> {

        private Message msg1;

        WaiteCloudStorageTask(Message msg) {
            msg1 = msg;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Integer doInBackground(Void... params) {
            int i = 0;
            while (i < 20 && ((FileManagerActivity) RemoteFileUtility.getInstance(null).getActivity()).getRemoteService() == null) {
                try {
                    Thread.sleep(100);
                    i++;
                    Log.d(TAG, "Waiting time = " + i * 100 + "ms");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            try {
                Messenger cloudServiceMessenger = ((FileManagerActivity) RemoteFileUtility.getInstance(null).getActivity()).getRemoteService();
                if (cloudServiceMessenger != null) {
                    cloudServiceMessenger.send(msg1);
                } else {
                    Log.e(TAG, "Time out, cloudServiceMessenger is null");
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

    public void sendRemoteCopyMessage(VFile[] src, String dst, int what) {
        StorageObj storageObj;
        FileObj fileObj = null;
        MsgObj msgObj;
        FileObj[] fileObjs;
        String deviceName = "";
        String deviceAddress = "";


        switch (what) {
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_TO_REMOTE:
                Log.d(TAG, "sendRemoteCopyMessage MSG_APP_REQUEST_COPY_FILE_TO_REMOTE");
                // copy: local to remote, we hope the source is from local
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
                    Log.d(TAG, "copy to remote -> deviceName: " + mConnectedWifiDirectStorageName + " deviceAddress: " + mConnectedWifiDirectStorageAddress);
                    deviceName = mConnectedWifiDirectStorageName;
                    deviceAddress = mConnectedWifiDirectStorageAddress;
                    storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, deviceName, deviceAddress, StorageObj.State.CONNECTED, mCurrentToken);
                    fileObjs = new FileObj[src.length];
                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        fileObjs[i] = new FileObj(src[i].getName(), src[i].getParent(),
                                src[i].isDirectory(), 4096, src[i].lastModified(), "DRW", true);
                        //Log.d(TAG, "copy from: " + fileObjs[i].getFullPath());
                    }
                    // some info are fake because they are not important
                    RemoteVFile remoteVFile = new RemoteVFile(dst);
                    //Log.d(TAG, "copy to remote folder name: " + remoteVFile.getName() + " and parent path: " +remoteVFile.getRealParentPath());
                    fileObj = new FileObj(remoteVFile.getName(), remoteVFile.getRealParentPath(), true, 4096, 900000000, "DRW", true);
                    //Log.d(TAG, "to dst: " +fileObj.getFullPath());
                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setFileObjPath(fileObj);
                } else {
                    Log.d(TAG, "copy from local to remote but source error");
                    break;
                }

                deliverRemoteMsg((MsgObj) msgObj, what);

                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE:
                Log.d(TAG, "sendRemoteCopyMessage MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE");
                // copy: remote to local, we hope the source is from remote
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
                    Log.d(TAG, "copy from remote -> deviceName: " + mConnectedWifiDirectStorageName + " deviceAddress: " + mConnectedWifiDirectStorageAddress);
                    deviceName = mConnectedWifiDirectStorageName;
                    deviceAddress = mConnectedWifiDirectStorageAddress;
                    storageObj = new StorageObj(MsgObj.TYPE_WIFIDIRECT_STORAGE, deviceName, deviceAddress, StorageObj.State.CONNECTED, mCurrentToken);
                    fileObjs = new FileObj[src.length];
                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        fileObjs[i] = new FileObj(src[i].getName(), ((RemoteVFile) src[i]).removeStorageNameParentPath(deviceName)
                                , true, 4096, 900000000, "DRW", true);
                        //Log.d(TAG, "copy: " + fileObjs[i].getFullPath());
                    }
                    LocalVFile vfile = new LocalVFile(dst);
                    if (vfile != null && vfile.exists()) {
                        // some info are fake because they are not important
                        fileObj = new FileObj(vfile.getName(), vfile.getParent(), true, 4096, 900000000, "DRW", true);
                        //Log.d(TAG, "from src: " +fileObj.getFullPath());
                    } else {
                        Log.w(TAG, "copy from remote to local but dst file error");
                    }
                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setFileObjPath(fileObj);
                } else {
                    Log.d(TAG, "copy from remote to local but source error");
                    break;
                }

                deliverRemoteMsg((MsgObj) msgObj, what);

                break;
        }
    }

    public void setPreViewProgress(double copySize, double copyTotalSize) {
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null) {
            fileListFragment.getHandler().removeMessages(FileListFragment.MSG_REMOTE_PRIVIEW_PROG_SIZE);
            EditResult editResult = new EditResult();
            editResult.copySize = copySize;
            editResult.copyTotalSize = copyTotalSize;
            Message msg = fileListFragment.getHandler().obtainMessage(FileListFragment.MSG_REMOTE_PRIVIEW_PROG_SIZE, editResult);
            fileListFragment.getHandler().sendMessage(msg);
        }
    }

    public void setProgress(double copySize, double copyTotalSize) {
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null) {
            fileListFragment.getHandler().removeMessages(FileListFragment.MSG_PASTE_REMOTE_PROG_SIZE);
            EditResult editResult = new EditResult();
            editResult.copySize = copySize;
            editResult.copyTotalSize = copyTotalSize;
            Message msg = fileListFragment.getHandler().obtainMessage(FileListFragment.MSG_PASTE_REMOTE_PROG_SIZE, editResult);
            fileListFragment.getHandler().sendMessage(msg);
        }
    }

    public void setCopyCloudProgress(int currentFileNum, int totalFileNum, double currentFileCopySize, double currentFileTotalSize) {
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null) {
            fileListFragment.getHandler().removeMessages(FileListFragment.MSG_PASTE_CLOUD_PROG_SIZE);
            EditResult editResult = new EditResult();
            editResult.currentFileCopySize = currentFileCopySize;
            editResult.currentFileTotalSize = currentFileTotalSize;
            editResult.fileCurrentCount = currentFileNum;
            editResult.fileTotalCount = totalFileNum;
            Message msg = fileListFragment.getHandler().obtainMessage(FileListFragment.MSG_PASTE_CLOUD_PROG_SIZE, editResult);
            fileListFragment.getHandler().sendMessage(msg);
        }
    }

    public void setCopyStatus(int status) {
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null) {
            fileListFragment.getHandler().removeMessages(FileListFragment.MSG_PASTE_CLOUD_COPY_STATUS);
            EditResult editResult = new EditResult();
            editResult.copyStatus = status;
            Message msg = fileListFragment.getHandler().obtainMessage(FileListFragment.MSG_PASTE_CLOUD_COPY_STATUS, editResult);
            fileListFragment.getHandler().sendMessage(msg);
        }
    }

    public void dissMissRemoteCopyProgressDialog() {
        Log.e("felix_zhang", "colse dissMissRemoteCopyProgressDialog");
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null) {
            fileListFragment.pasteComplete();
            //fileListFragment.pasteCloudComplete();
        }
    }

    public void dissMissPreViewProgressDialog() {
        ((FileManagerActivity) getActivity()).closeDialog(DialogType.TYPE_PREVIEW_PROCESS_DIALOG);
    }

    public void dissMissCloudCopyProgressDialog() {
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        Log.d("felix_zhang", "dissMissCloudCopyProgressDialog");
        if (fileListFragment != null) {
            //fileListFragment.pasteComplete();
            Log.d("felix_zhang", "dissMissCloudCopyProgressDialog fileListFragment is not null");
            fileListFragment.pasteCloudComplete();
        } else {
            Log.d("felix_zhang", "dissMissCloudCopyProgressDialog fileListFragment is null");
        }
    }

    public String getConnectedWifiDirectStorageName() {
        return mConnectedWifiDirectStorageName;
    }

    public void setRemoteThumbnailList(VFile[] files) {
        if (files != null) {
            mRemoteFileListForThumbnail = new RemoteVFile[files.length];
            for (int i = 0; i < files.length; i++) {
                mRemoteFileListForThumbnail[i] = new RemoteVFile(files[i]);
            }
        }
    }

    public boolean isRemoteLoadingError() {
        return mRemoteFileListError;
    }

    public boolean isRemoteAccessPermisionDeny() {
        return mRemoteAccessPermisionDeny;
    }

    public boolean isHomeCloudFileListError() {
        return mHomeCloudFileListError;
    }

    // --- remote storage function

    // +++ Add cloud storage function
    public void sendCloudStorageMsg(String account, VFile srcVFile, VFile[] targetVFiles, int type, int what) {
        sendCloudStorageMsg(account, srcVFile, targetVFiles, type, what, "");
    }

    private boolean isNeedRefreshToken = false;

    public void setNeedRefreshToken(boolean isNeed) {
        isNeedRefreshToken = isNeed;
    }

    public boolean getNeedRefreshToken() {
        return isNeedRefreshToken;
    }

    public void sendCloudStorageMsg(String account, VFile srcVFile, VFile[] targetVFiles, int type, int what, String argument) {
        StorageObj storageObj;
        MsgObj msgObj;
        String fileName;
        String fileParentPath;
        FileObj fileObj;


        String cloudStorageId = "";
        String currentToken = null;
        if (what != CloudStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE /*&& what != CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_STORAGE_USAGE */ && what != CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL) {
            if (!account.equals("")) {
                mCurrentAccount = account;
                String key = account + "_" + type;
                RemoteAccountUtility.AccountInfo info = RemoteAccountUtility.getInstance(null).accountsMap.get(key);
                if (info != null) {
                    currentToken = info.getToken();
                } else {
                    currentToken = null;
                }

                if (currentToken == null || (currentToken.equals("") && type != MsgObj.TYPE_GOOGLE_DRIVE_STORAGE)) {
                    Log.d(TAG, "currentToken is null");
                    return;
                }
                for (int i = 0; i < cloudStorageList.size(); i++) {
                    if (cloudStorageList.get(i).cloudStorageName.equals(account) &&
                            cloudStorageList.get(i).cloudStorageType == type) {
                        cloudStorageId = cloudStorageList.get(i).cloudStorageId;
                        break;
                    }
                }
            } else {
                account = mCurrentAccount;
                cloudStorageId = mCurrentCloudStorageId;
                type = mCurrentCloudStorageType;

            }
        }

        mCurrentCloudStorageId = cloudStorageId;
        mCurrentCloudStorageType = type;

        Log.d(TAG, "want to sendCloudStorageMsg: " + what + " to storage type: " + type);
        //Log.d(TAG, "sendCloudStorageMsg: " + what + " to " + account + " and it's id: " + cloudStorageId);

        switch (what) {
            case CloudStorageServiceHandlerMsg.MSG_APP_UPDATE_DEVICE:
                Log.d(TAG, "sendCloudStorageMsg MSG_APP_UPDATE_DEVICE");
                try {
                    Messenger remoteService = ((FileManagerActivity) getActivity()).getRemoteService();
                    if (remoteService != null) {
                        Message msg = Message.obtain(null, what);
                        msg.replyTo = ((FileManagerActivity) getActivity()).getRemoteClientMessenger();
                        remoteService.send(msg);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_CONNECT_DEVICE:
            case CloudStorageServiceHandlerMsg.MSG_APP_DISCONNECT_DEVICE:
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_STORAGE_USAGE:
                storageObj = new StorageObj(type, account, cloudStorageId, StorageObj.State.CONNECTED, currentToken);
                msgObj = new MsgObj(storageObj);
                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FOLDER_LIST:
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_PARENT_FOLDER_LIST:
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_SEARCH_FILES:

                if (srcVFile == null) {
                    Log.w(TAG, "query file is null when do sendCloudStorageMsg: " + what);
                    return;
                }

                // consider file path: /account name/XXX...
                fileName = ((RemoteVFile) srcVFile).getName();
                fileParentPath = ((RemoteVFile) srcVFile).removeStorageNameParentPath(((RemoteVFile) srcVFile).getStorageName());
                storageObj = new StorageObj(type, account, cloudStorageId, StorageObj.State.CONNECTED, currentToken);
                if (((RemoteVFile) srcVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    storageObj.setUserId(((RemoteVFile) srcVFile).getmUserId());
                    storageObj.setDeviceId(((RemoteVFile) srcVFile).getmDeviceId());
                    if (fileParentPath.equals(File.separator)
                            && RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()) != null
                            && fileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()).getDeviceName())) {
                        fileName = removeDeviceNameFromFileName(fileName, ((RemoteVFile) srcVFile).getmDeviceId());
                    }
                    fileParentPath = removeDeviceNameFromParentPath(fileParentPath, ((RemoteVFile) srcVFile).getmDeviceId());
                }
                msgObj = new MsgObj(storageObj);
                fileObj = new FileObj(fileName, fileParentPath, true, 4096, 900000000, "DRW", true);
                fileObj.setFileId(((RemoteVFile) srcVFile).getFileID());

                Log.d(TAG, "FileManager query folder name: " + fileName + " and parent path: " + fileParentPath
                        + " its fileId: " + fileObj.getFileId() + " and action: " + argument);

                msgObj.setFileObjPath(fileObj);
                msgObj.setArgument(argument);
                if (what == CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_SEARCH_FILES && (type == MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE || type == MsgObj.TYPE_HOMECLOUD_STORAGE)) {
                    msgObj = AWSMsgObjHelper.createSearchFileMsgObj(msgObj.getStorageObj(), argument);
                    msgObj.setFileObjPath(fileObj);
                }
                deliverRemoteMsg((MsgObj) msgObj, what, true, true, false);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST:
                //Jack for refresh test home cloud token
                //currentToken = "https://aae-sgweb886.asuscloud.com@@60FD0C91-A054-4F35-845A-9F90FDF6A984@@0714ad2a68dda4bca75379e22c9bfede@@36ddb16645f97f0516dcf58d9@@8aec092949e747ebac8e9843f27ab685@@2@@1@@2@@aae-sgsip886-2.asuscloud.com:5061@@aae-sgsip886-1.asuscloud.com:5061@@aae-sgstun886-1.asuscloud.com@@60.199.253.114:80@@60.199.253.113:80@@25f9e794323b453885f5181f1b624d0b";
                storageObj = new StorageObj(type, account, currentToken);
                msgObj = new MsgObj(storageObj);
                msgObj.setArgument("1");
                deliverRemoteMsg((MsgObj) msgObj, what, true, true, false);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_CREATE_FOLDER:
                Log.d(TAG, "sendCloudStorageMsg MSG_APP_REQUEST_CREATE_FOLDER");
                if (srcVFile == null) {
                    Log.w(TAG, "file is null when do sendCloudStorageMsg: " + what);
                    return;
                }
                fileName = ((RemoteVFile) srcVFile).getName();
                fileParentPath = ((RemoteVFile) srcVFile).removeStorageNameParentPath(((RemoteVFile) srcVFile).getStorageName());
                storageObj = new StorageObj(type, account, cloudStorageId, StorageObj.State.CONNECTED, currentToken);
                if (((RemoteVFile) srcVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    storageObj.setUserId(((RemoteVFile) srcVFile).getmUserId());
                    storageObj.setDeviceId(((RemoteVFile) srcVFile).getmDeviceId());
                    if (fileParentPath.equals(File.separator)
                            && RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()) != null
                            && fileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()).getDeviceName())) {
                        fileName = removeDeviceNameFromFileName(fileName, ((RemoteVFile) srcVFile).getmDeviceId());
                    }
                    fileParentPath = removeDeviceNameFromParentPath(fileParentPath, ((RemoteVFile) srcVFile).getmDeviceId());
                }
                msgObj = new MsgObj(storageObj);
                fileObj = new FileObj(fileName, fileParentPath, true, 4096, 900000000, "DRW", true);

                fileObj.setFileId(((RemoteVFile) srcVFile).getFileID());
                msgObj.setFileObjPath(fileObj);

                Log.d(TAG, "create folder name: " + targetVFiles[0].getName() + " in the folder: " + fileName
                        + " and the folder parent path: " + fileParentPath + " its parent id: " + fileObj.getFileId());

                FileObj[] newFolder = new FileObj[1];
                newFolder[0] = new FileObj(targetVFiles[0].getName(), (fileParentPath.equals(File.separator) ? "" : fileParentPath) + "/" + fileName, true, 4096, 900000000, "DRW", true);
                msgObj.setFileObjFiles(newFolder);

                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_DELETE_FILES:
                Log.d(TAG, "sendCloudStorageMsg MSG_APP_REQUEST_DELETE_FILES");

                storageObj = new StorageObj(type, account, cloudStorageId, StorageObj.State.CONNECTED, currentToken);
                if (targetVFiles != null && targetVFiles.length > 0) {
                    if (((RemoteVFile) targetVFiles[0]).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        storageObj.setUserId(((RemoteVFile) targetVFiles[0]).getmUserId());
                        storageObj.setDeviceId(((RemoteVFile) targetVFiles[0]).getmDeviceId());
                    }
                }
                msgObj = new MsgObj(storageObj);

                FileObj[] deleteFiles = new FileObj[targetVFiles.length];
                String parentPath = ((RemoteVFile) targetVFiles[0]).removeStorageNameParentPath(((RemoteVFile) targetVFiles[0]).getStorageName());
                if (((RemoteVFile) targetVFiles[0]).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    storageObj.setUserId(((RemoteVFile) targetVFiles[0]).getmUserId());
                    storageObj.setDeviceId(((RemoteVFile) targetVFiles[0]).getmDeviceId());
                    parentPath = removeDeviceNameFromParentPath(parentPath, ((RemoteVFile) targetVFiles[0]).getmDeviceId());
                }
                for (int i = 0; i < targetVFiles.length; i++) {
                    deleteFiles[i] = new FileObj(targetVFiles[i].getName(), parentPath, targetVFiles[i].isDirectory(), 4096, 900000000, "DRW", true);
                    deleteFiles[i].setFileId(((RemoteVFile) targetVFiles[i]).getFileID());
                    Log.d(TAG, "delete file: " + targetVFiles[i].getName() + " parent path: " + parentPath
                            + " id: " + ((RemoteVFile) targetVFiles[i]).getFileID());
                }
                msgObj.setFileObjFiles(deleteFiles);
                deliverRemoteMsg(msgObj, what, true, true, true);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_RENAME_FILE:
                Log.d(TAG, "sendCloudStorageMsg MSG_APP_REQUEST_RENAME_FILE");
                if (srcVFile == null) {
                    Log.w(TAG, "file is null when do sendCloudStorageMsg: " + what);
                    return;
                }
                fileName = ((RemoteVFile) srcVFile).getName();
                fileParentPath = ((RemoteVFile) srcVFile).removeStorageNameParentPath(((RemoteVFile) srcVFile).getStorageName());
                storageObj = new StorageObj(type, account, cloudStorageId, StorageObj.State.CONNECTED, currentToken);
                if (((RemoteVFile) srcVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    storageObj.setUserId(((RemoteVFile) srcVFile).getmUserId());
                    storageObj.setDeviceId(((RemoteVFile) srcVFile).getmDeviceId());
                    if (fileParentPath.equals(File.separator)
                            && RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()) != null
                            && fileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()).getDeviceName())) {
                        fileName = removeDeviceNameFromFileName(fileName, ((RemoteVFile) srcVFile).getmDeviceId());
                    }
                    fileParentPath = removeDeviceNameFromParentPath(fileParentPath, ((RemoteVFile) srcVFile).getmDeviceId());
                }
                msgObj = new MsgObj(storageObj);
                fileObj = new FileObj(fileName, fileParentPath, srcVFile.isDirectory(), 4096, 900000000, "DRW", true);
                fileObj.setFileId(((RemoteVFile) srcVFile).getFileID());
                msgObj.setFileObjPath(fileObj);
                Log.d(TAG, "use file name: " + targetVFiles[0].getName() + " to rename original file: " + fileName
                        + " and the folder parent path: " + fileParentPath + " its id: " + ((RemoteVFile) srcVFile).getFileID());
                FileObj[] renameFile = new FileObj[1];
                renameFile[0] = new FileObj(targetVFiles[0].getName(), fileParentPath, true, 4096, 900000000, "DRW", true);
                msgObj.setFileObjFiles(renameFile);
                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_THUMBNAIL:
                Log.d(TAG, "sendCloudStorageMsg MSG_APP_REQUEST_FILE_THUMBNAIL");
                fileName = ((RemoteVFile) srcVFile).getName();
                fileParentPath = ((RemoteVFile) srcVFile).removeStorageNameParentPath(((RemoteVFile) srcVFile).getStorageName());//12345
                long lastModified = srcVFile.lastModified();

                //google drive rename the thumbnail name
                String thumbnailName = (((RemoteVFile) srcVFile).getStorageType() == StorageType.TYPE_GOOGLE_DRIVE) ? (fileName + File.separator + ((RemoteVFile) srcVFile).getFileID()) : fileName;
                ThumbnailEntry tempEntry = new ThumbnailEntry(type, account, thumbnailName, fileParentPath);

                //ThumbnailEntry tempEntry = new ThumbnailEntry(type, account, fileName, fileParentPath);
                if (mThumbnailEntries.size() > 0) {
                    for (int i = 0; i < mThumbnailEntries.size(); i++) {
                        if (mThumbnailEntries.get(i).equals(tempEntry)) {
                            return;
                        }
                    }
                    mThumbnailEntries.add(tempEntry);
                } else {
                    mThumbnailEntries.add(tempEntry);
                }

                Log.d(TAG, "request file thumbnail: " + fileName);
                storageObj = new StorageObj(type, account, cloudStorageId, StorageObj.State.CONNECTED, currentToken);
                if (((RemoteVFile) srcVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    storageObj.setUserId(((RemoteVFile) srcVFile).getmUserId());
                    storageObj.setDeviceId(((RemoteVFile) srcVFile).getmDeviceId());
                    if (fileParentPath.equals(File.separator)
                            && RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()) != null
                            && fileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()).getDeviceName())) {
                        fileName = removeDeviceNameFromFileName(fileName, ((RemoteVFile) srcVFile).getmDeviceId());
                    }
                    fileParentPath = removeDeviceNameFromParentPath(fileParentPath, ((RemoteVFile) srcVFile).getmDeviceId());
                }
                msgObj = new MsgObj(storageObj);
                fileObj = new FileObj(fileName, fileParentPath, true, 4096, lastModified, "DRW", true);

                fileObj.setFileId(((RemoteVFile) srcVFile).getFileID());
                Log.i(TAG, "get request file thumbnail fileid:" + ((RemoteVFile) srcVFile).getFileID());
                msgObj.setFileObjPath(fileObj);
                msgObj.setArgument(getThumbnailStandard(type));

                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL:
                Log.d(TAG, "sendCloudStorageMsg MSG_APP_COPY_FILE_CANCEL");
                if (null != currentOperateMsgObj && null != currentOperateMsgObj.getMsgId()) {
                    msgObj = currentOperateMsgObj;
                    Log.i("felix_zhang:", "MSG_APP_COPY_FILE_CANCEL ");
                    deliverRemoteMsg((MsgObj) msgObj, what);
                    removeMsgOjbFromMap(currentOperateMsgObj);
                    currentOperateMsgObj = null;
                    dissMissRemoteCopyProgressDialog();
                }
                break;

            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_INFO:
                Log.d(TAG, "sendRemoteMessage MSG_APP_REQUEST_FILE_INFO");

                fileName = ((RemoteVFile) srcVFile).getName();
                fileParentPath = ((RemoteVFile) srcVFile).removeStorageNameParentPath(((RemoteVFile) srcVFile).getStorageName());

                Log.d(TAG, "query file info: " + fileName + " parebt path: " + fileParentPath + " id: " + ((RemoteVFile) srcVFile).getFileID());

                storageObj = new StorageObj(type, account, cloudStorageId, StorageObj.State.CONNECTED, currentToken);
                if (((RemoteVFile) srcVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    storageObj.setUserId(((RemoteVFile) srcVFile).getmUserId());
                    storageObj.setDeviceId(((RemoteVFile) srcVFile).getmDeviceId());
                    if (fileParentPath.equals(File.separator)
                            && RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()) != null
                            && fileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()).getDeviceName())) {
                        fileName = removeDeviceNameFromFileName(fileName, ((RemoteVFile) srcVFile).getmDeviceId());
                    }
                    fileParentPath = removeDeviceNameFromParentPath(fileParentPath, ((RemoteVFile) srcVFile).getmDeviceId());
                }
                msgObj = new MsgObj(storageObj);
                fileObj = new FileObj(fileName, fileParentPath, srcVFile.isDirectory(), 4096, srcVFile.lastModified(), "DRW", true);
                fileObj.setFileId(((RemoteVFile) srcVFile).getFileID());
                msgObj.setFileObjPath(fileObj);
                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_URI:
                Log.d(TAG, "sendRemoteMessage MSG_APP_REQUEST_FILE_URI");

                fileName = ((RemoteVFile) srcVFile).getName();
                fileParentPath = ((RemoteVFile) srcVFile).removeStorageNameParentPath(((RemoteVFile) srcVFile).getStorageName());

                Log.d(TAG, "query file uri: " + fileName + " parebt path: " + fileParentPath + " id: " + ((RemoteVFile) srcVFile).getFileID());

                storageObj = new StorageObj(type, account, cloudStorageId, StorageObj.State.CONNECTED, currentToken);
                if (((RemoteVFile) srcVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                    storageObj.setUserId(((RemoteVFile) srcVFile).getmUserId());
                    storageObj.setDeviceId(((RemoteVFile) srcVFile).getmDeviceId());
                    if (fileParentPath.equals(File.separator)
                            && RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()) != null
                            && fileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()).getDeviceName())) {
                        fileName = removeDeviceNameFromFileName(fileName, ((RemoteVFile) srcVFile).getmDeviceId());
                    }
                    fileParentPath = removeDeviceNameFromParentPath(fileParentPath, ((RemoteVFile) srcVFile).getmDeviceId());
                }
                msgObj = new MsgObj(storageObj);
                FileObj[] fileObjs = new FileObj[1];

                fileObj = new FileObj(fileName, fileParentPath, srcVFile.isDirectory(), srcVFile.length(), srcVFile.lastModified(), "DRW", true);
                fileObj.setFileId(((RemoteVFile) srcVFile).getFileID());
                fileObjs[0] = fileObj;
                msgObj.setFileObjFiles(fileObjs);
                deliverRemoteMsg((MsgObj) msgObj, what);
                break;
        }
    }

    public void sendCloudStorageCopyMessage(String account, VFile[] src, VFile dstVFile, int type, int what, String action, boolean isAlreadyExpanded) {
        StorageObj storageObj;
        FileObj fileObj = null;
        MsgObj msgObj;
        FileObj[] fileObjs;
        boolean stop = false;
        String currentToken = null;

        String cloudStorageId = "";
        for (int i = 0; i < cloudStorageList.size(); i++) {
            if (cloudStorageList.get(i).cloudStorageName.equals(account) &&
                    cloudStorageList.get(i).cloudStorageType == type) {
                cloudStorageId = cloudStorageList.get(i).cloudStorageId;
                break;
            }
        }
        String key = account + "_" + type;
        RemoteAccountUtility.AccountInfo info = RemoteAccountUtility.getInstance(null).accountsMap.get(key);
        if (info != null) {
            currentToken = info.getToken();
        } else {
            currentToken = null;
        }

        if (currentToken == null || (currentToken.equals("") && type != MsgObj.TYPE_GOOGLE_DRIVE_STORAGE)) {
            Log.i(TAG, "currentToken is null");
            return;
        }
        //Log.d(TAG, "sendCloudStorageCopyMessage: " + what + " to " + account + " and it's id: " + cloudStorageId);
        mCurrentCloudStorageId = cloudStorageId;
        mCurrentCloudStorageType = type;
        if (dstVFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE && ((RemoteVFile) dstVFile).getMsgObjType() != MsgObj.TYPE_DROPBOX_STORAGE) {
            String fileId = ((RemoteVFile) dstVFile).getFileID();
            if (fileId != null && fileId.equals("root")) {
                fileId = RemoteClientHandler.getInstance(null).rootsFileIdmMap.get(((RemoteVFile) dstVFile).getMsgObjType() + "_" + ((RemoteVFile) dstVFile).getStorageName());
                if ("root".equalsIgnoreCase(fileId) && ((RemoteVFile) dstVFile).getMsgObjType() == 9) {
                    fileId = "";
                }
                ((RemoteVFile) dstVFile).setFileID(fileId);
            }
        }

        switch (what) {
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_TO_REMOTE:
                Log.d(TAG, "sendRemoteCopyMessage MSG_APP_REQUEST_COPY_FILE_TO_REMOTE");
                // copy: local to remote, we hope the source is from local
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {

                    storageObj = new StorageObj(type, account, "", StorageObj.State.CONNECTED, currentToken);

                    if (((RemoteVFile) dstVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        storageObj.setUserId(((RemoteVFile) dstVFile).getmUserId());
                        storageObj.setDeviceId(((RemoteVFile) dstVFile).getmDeviceId());
                    }

                    fileObjs = new FileObj[src.length];

                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        double fileSize = 4096;
                        boolean isDirectory = src[i].isDirectory();
                        Log.d(TAG, "file name: " + src[i].getName() + " isDirectory: " + isDirectory);
                        if (!isDirectory) {
                            fileSize = src[i].length();
                        }
                        fileObjs[i] = new FileObj(src[i].getName(), src[i].getParent(),
                                isDirectory, fileSize, src[i].lastModified(), "DRW", true);
                        //Log.d(TAG, "copy from: " + fileObjs[i].getFullPath());
                    }
                    // some info are fake because they are not important

                    // only Dropbox doesn't use file id
                    if (((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_DROPBOX && ((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_BAIDUPCS) {
                        fileObj = new FileObj(dstVFile.getName(), dstVFile.getParent(), true, 4096, 900000000, "DRW", true);
                        fileObj.setFileId(((RemoteVFile) dstVFile).getFileID());
                    } else {
                        fileObj = new FileObj(((RemoteVFile) dstVFile).getName(), ((RemoteVFile) dstVFile).removeStorageNameParentPath(((RemoteVFile) dstVFile).getStorageName()), true, 4096, 900000000, "DRW", true);
                    }
                    //Log.d(TAG, "copy to remote -> deviceName: " + account
                    //    + " and folder path: " + ((RemoteVFile)dstVFile).removeStorageNameParentPath(((RemoteVFile)dstVFile).getStorageName())
                    //    + " and folder id: " + ((RemoteVFile)dstVFile).getFileID());

                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setFileObjPath(fileObj);
                    msgObj.setIsAlreadyExpanded(isAlreadyExpanded);

                    msgObj.setArgument(action);
                    msgObj.setCopyType(CopyTypeArgument.LOCAL_TO_CLOUD);
                } else {
                    Log.d(TAG, "copy from local to remote but source error");
                    break;
                }
                deliverRemoteMsg((MsgObj) msgObj, what, true, true, true);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE:
                Log.d(TAG, "sendRemoteCopyMessage MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE");
                // copy: remote to local, we hope the source is from remote
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                    //Log.d(TAG, "copy from remote -> deviceName: " + account);
                    storageObj = new StorageObj(type, account, "", StorageObj.State.CONNECTED, currentToken);
                    //add homeCloud need userid and deviceid

                    fileObjs = new FileObj[src.length];
                    String parentPath = ((RemoteVFile) src[0]).removeStorageNameParentPath(((RemoteVFile) src[0]).getStorageName());
                    if (src != null && src.length > 0 && ((RemoteVFile) src[0]).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        parentPath = getCloudRealFileParentPath(parentPath, (RemoteVFile) src[0], storageObj);
                    }
                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        fileObjs[i] = new FileObj(src[i].getName(), parentPath, src[i].isDirectory(), 4096, 900000000, "DRW", true);
                        fileObjs[i].setFileId(((RemoteVFile) src[i]).getFileID());
                        fileObjs[i].setParentId(((RemoteVFile) src[i]).getParentFileID());
                        Log.d(TAG, "copy file: " + src[i].getName() + " and parent path: " + parentPath + " and file id: " + ((RemoteVFile) src[i]).getFileID());
                    }
                    if (true == stop) {//if have same file,stop download
                        stop = false;
                        break;
                    }

                    LocalVFile vfile = new LocalVFile(dstVFile.getAbsolutePath());
                    if (vfile != null && vfile.exists()) {
                        // some info are fake because they are not important
                        fileObj = new FileObj(vfile.getName(), vfile.getParent(), true, 4096, 900000000, "DRW", true);
                        //Log.d(TAG, "to dst: " +fileObj.getFullPath());
                    } else {
                        Log.w(TAG, "copy from remote to local but dst file error");
                    }
                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setFileObjPath(fileObj);
                    msgObj.setArgument(action);
                    msgObj.setCopyType(CopyTypeArgument.CLOUD_TO_LOCAL);
                    if (src != null && src.length > 0 && ((RemoteVFile) src[0]).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        msgObj.setCopyType(CopyTypeArgument.DEVICE_TO_LOCAL);
                    }
                } else {
                    Log.d(TAG, "copy from remote to local but source error");
                    break;
                }
                deliverRemoteMsg((MsgObj) msgObj, what, true, true, true);

                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_REMOTE_OTHER_REMOTE:
                Log.d(TAG, "sendRemoteCopyMessage MSG_APP_REQUEST_COPY_FILE_REMOTE_OTHER_REMOTE");
                // copy: remote to other remote
                MsgObj msgObj2 = null;
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                    //Log.d(TAG, "copy from remote -> deviceName: " + account);
                    storageObj = new StorageObj(type, account, "", StorageObj.State.CONNECTED, currentToken);
                    storageObj.setmToOtherType(((RemoteVFile) dstVFile).getMsgObjType());
                    storageObj.setmToStorageName(((RemoteVFile) dstVFile).getStorageName());
                    //add homeCloud need userid and deviceid
                    if (src != null && src.length > 0 && ((RemoteVFile) src[0]).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        storageObj.setUserId(((RemoteVFile) src[0]).getmUserId());
                        storageObj.setDeviceId(((RemoteVFile) src[0]).getmDeviceId());
                    }
                    fileObjs = new FileObj[src.length];
                    String parentPath = ((RemoteVFile) src[0]).removeStorageNameParentPath(((RemoteVFile) src[0]).getStorageName());
                    String destParentPath = dstVFile.getParent();
                    if (((RemoteVFile) dstVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        storageObj.setUserId(((RemoteVFile) dstVFile).getmUserId());
                        storageObj.setToDeviceId(((RemoteVFile) dstVFile).getmDeviceId());
                        String parentDestPath = ((RemoteVFile) dstVFile).removeStorageNameParentPath(((RemoteVFile) dstVFile).getStorageName());
                        destParentPath = removeDeviceNameFromParentPath(parentDestPath, ((RemoteVFile) dstVFile).getmDeviceId());
                    }
                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        fileObjs[i] = new FileObj(src[i].getName(), parentPath, src[i].isDirectory(), 4096, 900000000, "DRW", true);
                        fileObjs[i].setFileId(((RemoteVFile) src[i]).getFileID());
                        fileObjs[i].setParentId(((RemoteVFile) src[i]).getParentFileID());
                        Log.d(TAG, "copy file: " + src[i].getName() + " and parent path: " + parentPath + " and file id: " + ((RemoteVFile) src[i]).getFileID());
                    }

                    // only Dropbox doesn't use file id
                    if (((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_DROPBOX && ((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_BAIDUPCS) {
                        fileObj = new FileObj(dstVFile.getName(), destParentPath, true, 4096, 900000000, "DRW", true);
                        fileObj.setFileId(((RemoteVFile) dstVFile).getFileID());
                        fileObj.setParentId(((RemoteVFile) dstVFile).getParentFileID());
                    } else {
                        fileObj = new FileObj(((RemoteVFile) dstVFile).getName(), ((RemoteVFile) dstVFile).removeStorageNameParentPath(((RemoteVFile) dstVFile).getStorageName()), true, 4096, 900000000, "DRW", true);
                    }
                    //Log.d(TAG, "copy to remote -> deviceName: " + ((RemoteVFile)dstVFile).getAbsolutePath()
                    //   + " and folder path: " + ((RemoteVFile)dstVFile).removeStorageNameParentPath(((RemoteVFile)dstVFile).getStorageName())
                    //   + " and folder id: " + ((RemoteVFile)dstVFile).getFileID());

                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setFileObjPath(fileObj);
                    msgObj.setArgument(action);
                    int copyType = RemoteFileOperateResultHelper.getCloudToOtherCloudType((RemoteVFile) src[0], (RemoteVFile) dstVFile);
                    if (copyType == CopyTypeArgument.CLOUD_TO_DEVICE) {
                        msgObj.getStorageObj().setToDeviceId(((RemoteVFile) dstVFile).getmDeviceId());
                    } else if (copyType == CopyTypeArgument.DEVICE_TO_CLOUD) {
                        msgObj.getStorageObj().setDeviceId(((RemoteVFile) src[0]).getmDeviceId());
                    }
                    msgObj.setCopyType(copyType);
                    //msgObj.setCopyType(CopyTypeArgument.CLOUD_OTHER_CLOUD);
                    msgObj = appendMsgIDOtMsgObj(msgObj, true, true);
                    LocalVFile destFile = getCloudToOtherCloudCasheDir(msgObj, getActivity());

                    msgObj2 = new MsgObj(msgObj.getStorageObj());
                    msgObj2.setArgument(action);
                    msgObj2.setFileObjFiles(msgObj.getFileObjFiles());
                    msgObj2.setMsgId(msgObj.getMsgId());
                    msgObj2.setFileObjPath(new FileObj(destFile.getName(), destFile.getParent(), destFile.isDirectory(), 4096, 900000000, "DRW", true));
                    msgObj2.setCopyType(copyType);
                } else {
                    Log.d(TAG, "copy from remote to remote but source error");
                    break;
                }
                //changeCloudInfo(msgObj, msgObj);
                deliverRemoteMsg((MsgObj) msgObj2, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, false, false, true);
                break;
            case FileListFragment.MSG_APP_REQUEST_COPY_FILE_DEVICE_TO_OTHER_DEVICE:
                // copy: remote to other remote
                MsgObj msgObjDevice = null;
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                    //Log.d(TAG, "copy from remote -> deviceName: " + account);
                    storageObj = new StorageObj(type, account, "", StorageObj.State.CONNECTED, currentToken);
                    storageObj.setmToOtherType(((RemoteVFile) dstVFile).getMsgObjType());
                    storageObj.setmToStorageName(((RemoteVFile) dstVFile).getStorageName());
                    storageObj.setUserId(((RemoteVFile) dstVFile).getmUserId());
                    storageObj.setDeviceId(((RemoteVFile) dstVFile).getmDeviceId());
                    storageObj.setToDeviceId(((RemoteVFile) dstVFile).getmDeviceId());
                    //add homeCloud need userid and deviceid
                    if (src != null && src.length > 0) {
                        storageObj.setUserId(((RemoteVFile) src[0]).getmUserId());
                        storageObj.setDeviceId(((RemoteVFile) src[0]).getmDeviceId());
                    }

                    fileObjs = new FileObj[src.length];
                    String srcParentPath = ((RemoteVFile) src[0]).removeStorageNameParentPath(((RemoteVFile) src[0]).getStorageName());
                    String destParentPath = (((RemoteVFile) dstVFile).removeStorageNameParentPath(((RemoteVFile) dstVFile).getStorageName()));
                    srcParentPath = removeDeviceNameFromParentPath(srcParentPath, ((RemoteVFile) src[0]).getmDeviceId());
                    destParentPath = removeDeviceNameFromParentPath(destParentPath, ((RemoteVFile) dstVFile).getmDeviceId());
                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        fileObjs[i] = new FileObj(src[i].getName(), srcParentPath, src[i].isDirectory(), 4096, 900000000, "DRW", true);
                        fileObjs[i].setFileId(((RemoteVFile) src[i]).getFileID());
                        fileObjs[i].setParentId(((RemoteVFile) src[i]).getParentFileID());
                        Log.d(TAG, "copy file: " + src[i].getName() + " and parent path: " + srcParentPath + " and file id: " + ((RemoteVFile) src[i]).getFileID());
                    }

                    // only Dropbox doesn't use file id
                    fileObj = new FileObj(dstVFile.getName(), destParentPath, true, 4096, 900000000, "DRW", true);
                    fileObj.setFileId(((RemoteVFile) dstVFile).getFileID());
                    fileObj.setParentId(((RemoteVFile) dstVFile).getParentFileID());

                    //Log.d(TAG, "copy to remote -> deviceName: " + ((RemoteVFile)dstVFile).getAbsolutePath()
                    //   + " and folder path: " + ((RemoteVFile)dstVFile).removeStorageNameParentPath(((RemoteVFile)dstVFile).getStorageName())
                    //   + " and folder id: " + ((RemoteVFile)dstVFile).getFileID());

                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setFileObjPath(fileObj);
                    msgObj.setArgument(action);
                    msgObj.setCopyType(CopyTypeArgument.DEVICE_TO_OTHER_DEVICE);
                    //msgObj.setCopyType(CopyTypeArgument.CLOUD_OTHER_CLOUD);
                    msgObj = appendMsgIDOtMsgObj(msgObj, true, true);
                    LocalVFile destFile = getCloudToOtherCloudCasheDir(msgObj, getActivity());

                    msgObjDevice = new MsgObj(msgObj.getStorageObj());
                    msgObjDevice.setArgument(action);
                    msgObjDevice.setFileObjFiles(msgObj.getFileObjFiles());
                    msgObjDevice.setMsgId(msgObj.getMsgId());
                    msgObjDevice.setFileObjPath(new FileObj(destFile.getName(), destFile.getParent(), destFile.isDirectory(), 4096, 900000000, "DRW", true));
                } else {
                    Log.d(TAG, "copy from remote to remote but source error");
                    break;
                }
                //changeCloudInfo(msgObj, msgObj);
                deliverRemoteMsg((MsgObj) msgObjDevice, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, false, false, true);
                break;
            case CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_UPDATE_REMOTE:
                Log.d(TAG, "sendRemoteCopyMessage MSG_APP_REQUEST_COPY_FILE_UPDATE_REMOTE");
                // copy: remote to remote
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                    //Log.d(TAG, "copy from remote -> deviceName: " + account);
                    storageObj = new StorageObj(type, account, "", StorageObj.State.CONNECTED, currentToken);
                    //add homeCloud need userid and deviceid

                    fileObjs = new FileObj[src.length];
                    String parentPath = ((RemoteVFile) src[0]).removeStorageNameParentPath(((RemoteVFile) src[0]).getStorageName());
                    String destParentPath = null;
                    if (((RemoteVFile) dstVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        storageObj.setUserId(((RemoteVFile) dstVFile).getmUserId());
                        storageObj.setDeviceId(((RemoteVFile) dstVFile).getmDeviceId());
                        parentPath = getCloudRealFileParentPath(parentPath, (RemoteVFile) src[0], storageObj);
                        destParentPath = ((RemoteVFile) dstVFile).removeStorageNameParentPath(((RemoteVFile) dstVFile).getStorageName());
                        destParentPath = removeDeviceNameFromParentPath(destParentPath, ((RemoteVFile) dstVFile).getmDeviceId());
                    }
                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        fileObjs[i] = new FileObj(src[i].getName(), parentPath, src[i].isDirectory(), 4096, 900000000, "DRW", true);
                        fileObjs[i].setFileId(((RemoteVFile) src[i]).getFileID());
                        Log.d(TAG, "copy file: " + src[i].getName() + " and parent path: " + parentPath + " and file id: " + ((RemoteVFile) src[i]).getFileID());
                    }

                    // only Dropbox doesn't use file id
                    if (((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_DROPBOX && ((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_BAIDUPCS && ((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_YANDEX) {
                        if (((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_HOME_CLOUD) {
                            fileObj = new FileObj(dstVFile.getName(), dstVFile.getParent(), true, 4096, 900000000, "DRW", true);
                        } else {
                            fileObj = new FileObj(dstVFile.getName(), destParentPath, true, 4096, 900000000, "DRW", true);
                        }
                        if (((RemoteVFile) dstVFile).getStorageType() == StorageType.TYPE_SKYDRIVE) {
                            String fileId = ((RemoteVFile) dstVFile).getFileID();
                            if (fileId != null && fileId.equals("root")) {
                                String fileIdKey = ((RemoteVFile) dstVFile).getMsgObjType() + "_" + ((RemoteVFile) dstVFile).getStorageName();
                                if (RemoteClientHandler.getInstance(null).rootsFileIdmMap.get(fileIdKey) != null) {
                                    fileObj.setFileId(RemoteClientHandler.getInstance(null).rootsFileIdmMap.get(fileIdKey));
                                }
                            } else {
                                fileObj.setFileId(fileId);
                            }
                        } else {
                            fileObj.setFileId(((RemoteVFile) dstVFile).getFileID());
                        }

                    } else {
                        fileObj = new FileObj(((RemoteVFile) dstVFile).getName(), ((RemoteVFile) dstVFile).removeStorageNameParentPath(((RemoteVFile) dstVFile).getStorageName()), true, 4096, 900000000, "DRW", true);
                    }
                    //Log.d(TAG, "copy to remote -> deviceName: " + ((RemoteVFile)dstVFile).getAbsolutePath()
                    //    + " and folder path: " + ((RemoteVFile)dstVFile).removeStorageNameParentPath(((RemoteVFile)dstVFile).getStorageName())
                    //    + " and folder id: " + ((RemoteVFile)dstVFile).getFileID());

                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setFileObjPath(fileObj);

                    msgObj.setArgument(action);
                    msgObj.setCopyType(CopyTypeArgument.CLOUD_TO_THE_CLOUD);
                } else {
                    Log.d(TAG, "copy from remote to remote but source error");
                    break;
                }
                deliverRemoteMsg((MsgObj) msgObj, what, true, true, true);
                break;

            case FileListFragment.MSG_APP_REQUEST_COPY_FILE_CLOUD_TO_SAMB:
                Log.d(TAG, "sendRemoteCopyMessage MSG_APP_REQUEST_COPY_FILE_CLOUD_TO_SAMB");
                MsgObj tempSambObj = null;
                // copy: remote to samb, we hope the source is from remote
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                    storageObj = new StorageObj(type, account, "", StorageObj.State.CONNECTED, currentToken);
                    //add homeCloud need userid and deviceid

                    fileObjs = new FileObj[src.length];
                    String parentPath = ((RemoteVFile) src[0]).removeStorageNameParentPath(((RemoteVFile) src[0]).getStorageName());
                    if (src != null && src.length > 0 && ((RemoteVFile) src[0]).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        parentPath = getCloudRealFileParentPath(parentPath, (RemoteVFile) src[0], storageObj);
                    }
                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        fileObjs[i] = new FileObj(src[i].getName(), parentPath, src[i].isDirectory(), 4096, 900000000, "DRW", true);
                        fileObjs[i].setFileId(((RemoteVFile) src[i]).getFileID());
                        fileObjs[i].setParentId(((RemoteVFile) src[i]).getParentFileID());
                        Log.d(TAG, "copy file: " + src[i].getName() + " and parent path: " + parentPath + " and file id: " + ((RemoteVFile) src[i]).getFileID());
                    }
                    if (true == stop) {//if have same file,stop download
                        stop = false;
                        break;
                    }
                    fileObj = new FileObj(((SambaVFile) dstVFile).getName(), ((SambaVFile) dstVFile).getAbsolutePath(), ((SambaVFile) dstVFile).isDirectory(), ((SambaVFile) dstVFile).length(), ((SambaVFile) dstVFile).lastModified(), "DRW", true);
                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setArgument(action);
                    msgObj.setCopyType(CopyTypeArgument.CLOUD_TO_SAMB);
                    msgObj.setFileObjPath(fileObj);
                    msgObj = appendMsgIDOtMsgObj(msgObj, true, true);
                    LocalVFile destFile = getCloudToOtherCloudCasheDir(msgObj, getActivity());
                    tempSambObj = new MsgObj(msgObj.getStorageObj());
                    tempSambObj.setMsgId(msgObj.getMsgId());
                    tempSambObj.setFileObjFiles(msgObj.getFileObjFiles());
                    tempSambObj.setArgument(action);
                    tempSambObj.setCopyType(CopyTypeArgument.CLOUD_TO_SAMB);
                    tempSambObj.setFileObjPath(new FileObj(destFile.getName(), destFile.getParent(), destFile.isDirectory(), 4096, 900000000, "DRW", true));
                } else {
                    Log.d(TAG, "copy from remote to local but source error");
                    break;
                }
                deliverRemoteMsg((MsgObj) tempSambObj, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, false, false, true);

                break;
            case FileListFragment.MSG_APP_REQUEST_COPY_FILE_SAMB_TO__CLOUD:
                fileObjs = new FileObj[src.length];
                if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                    for (int i = 0; i < src.length; i++) {
                        // some info are fake because they are not important
                        SambaVFile tempFile = ((SambaVFile) src[i]);
                        fileObjs[i] = new FileObj(tempFile.getName(), tempFile.getParentPath(), tempFile.isDirectory(), tempFile.length(), tempFile.lastModified(), "DRW", true);

                    }
                }
                if (dstVFile != null && dstVFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                    //Log.d(TAG, "copy from remote -> deviceName: " + account);
                    storageObj = new StorageObj(type, account, "", StorageObj.State.CONNECTED, currentToken);
                    String destParentPath = dstVFile.getParent();
                    if (((RemoteVFile) dstVFile).getStorageType() == StorageType.TYPE_HOME_CLOUD) {
                        storageObj.setUserId(((RemoteVFile) dstVFile).getmUserId());
                        storageObj.setDeviceId(((RemoteVFile) dstVFile).getmDeviceId());
                        String parentDestPath = ((RemoteVFile) dstVFile).removeStorageNameParentPath(((RemoteVFile) dstVFile).getStorageName());
                        destParentPath = removeDeviceNameFromParentPath(parentDestPath, ((RemoteVFile) dstVFile).getmDeviceId());
                    }
                    // only Dropbox doesn't use file id
                    if (((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_DROPBOX && ((RemoteVFile) dstVFile).getStorageType() != StorageType.TYPE_BAIDUPCS) {
                        fileObj = new FileObj(dstVFile.getName(), destParentPath, true, 4096, 900000000, "DRW", true);
                        fileObj.setFileId(((RemoteVFile) dstVFile).getFileID());
                        fileObj.setParentId(((RemoteVFile) dstVFile).getParentFileID());
                    } else {
                        fileObj = new FileObj(((RemoteVFile) dstVFile).getName(), ((RemoteVFile) dstVFile).removeStorageNameParentPath(((RemoteVFile) dstVFile).getStorageName()), true, 4096, 900000000, "DRW", true);
                    }
                    msgObj = new MsgObj(storageObj);
                    msgObj.setFileObjPath(fileObj);
                    msgObj.setFileObjFiles(fileObjs);
                    msgObj.setArgument(action);
                    msgObj.setCopyType(CopyTypeArgument.SAMB_TO_CLOUD);
                    msgObj = appendMsgIDOtMsgObj(msgObj, true, true);
                    currentOperateMsgObj = msgObj;
                    LocalVFile destFile = getCloudToOtherCloudCasheDir(msgObj, getActivity());
                    SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.FILE_PASTE, src, destFile.getAbsolutePath(), action == CopyArgument.Move, CopyTypeArgument.SAMB_TO_CLOUD, msgObj.getMsgId());
                } else {
                    Log.d(TAG, "copy from remote to remote but source error");
                    break;
                }
                break;

        }

    }


    public void sendCopyFileFromCloudToSamba(String msgId, boolean isOk) {
        MsgObj msgObj = FileManagerApplication.msgMap.get(msgId);
        if (msgObj != null) {
            if (!isOk) {
                ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
            } else {
                if (msgObj.getArgument() != null && msgObj.getArgument().equals(CopyArgument.Move)) {
                    deliverRemoteMsg((MsgObj) msgObj
                            , CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_DELETE_FILES, false, false, false);
                    return;
                } else {
                    ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
                }
            }
            dissMissRemoteCopyProgressDialog();
            removeMsgOjbFromMap(msgObj);
            LocalVFile localVFile = new LocalVFile(new File(new File(getActivity().getExternalCacheDir(), ".cfile/"), msgObj.getMsgId()));
            if (localVFile != null && localVFile.exists()) {
                deleteFileAndPath(localVFile);
            }
        }

    }

    public void sendCopyFileFromSambToCloudMsg(String msgId, boolean isOk) {
        MsgObj msgObj = FileManagerApplication.msgMap.get(msgId);
        if (msgObj == null) {
            return;
        }
        if (!isOk) {
            // error handle
            dissMissRemoteCopyProgressDialog();
            removeMsgOjbFromMap(msgObj);
            LocalVFile localVFile = new LocalVFile(new File(new File(getActivity().getExternalCacheDir(), ".cfile/"), msgObj.getMsgId()));
            if (localVFile != null && localVFile.exists()) {
                deleteFileAndPath(localVFile);
            }
        } else {
            LocalVFile localVFile = new LocalVFile(new File(new File(getActivity().getExternalCacheDir(), ".cfile/"), msgObj.getMsgId()));
            FileObj[] srcFileObjFiles = null;
            if (localVFile != null && localVFile.exists() && localVFile.isDirectory()) {
                LocalVFile[] localVFiles = localVFile.listVFiles();
                srcFileObjFiles = new FileObj[localVFiles.length];
                for (int i = 0; i < localVFiles.length; i++) {
                    srcFileObjFiles[i] = new FileObj(localVFiles[i].getName(), localVFiles[i].getParent(), localVFiles[i].isDirectory(), 4096, localVFiles[i].lastModified(), "DWR", true);
                }
            }
            //msgObj.setFileObjFiles(srcFileObjFiles);

            if (srcFileObjFiles != null) {
                MsgObj temp = new MsgObj(msgObj.getStorageObj());
                temp.setMsgId(msgObj.getMsgId());
                temp.setFileObjPath(msgObj.getFileObjPath());
                temp.setArgument(msgObj.getArgument());
                temp.setCopyType(msgObj.getCopyType());
                temp.setFileObjFiles(srcFileObjFiles);
                deliverRemoteMsg((MsgObj) temp
                        , CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_TO_REMOTE, false, false, false);
            }
        }
    }

    public void sendDeleteSambFileWhenCopyFileFromSambToCloud(String msgId) {
        MsgObj msgObj = FileManagerApplication.msgMap.get(msgId);
        String actionArgument = (msgObj.getArgument() == null || msgObj.getArgument().equals("")) ? CopyArgument.Copy : CopyArgument.Move;
        if (actionArgument.equals(CopyArgument.Move)) {
            FileObj[] deleteFileObjs = msgObj.getFileObjFiles();
            // SambaVFile[] tempSambaVFiles = new SambaVFile[deleteFileObjs.length];
            String parentPath = null;
            if (deleteFileObjs != null && deleteFileObjs.length > 0) {
                parentPath = deleteFileObjs[0].getFileParentPath();
                String[] nameList = new String[deleteFileObjs.length];
                for (int i = 0; i < deleteFileObjs.length; i++) {
                    nameList[i] = deleteFileObjs[i].getFileName();
                    //tempSambaVFiles[i] = new SambaVFile(deleteFileObjs[i].getFileParentPath()+deleteFileObjs[i].getFileName(), deleteFileObjs[i].getIsDirectory(), deleteFileObjs[i].getFileSize(), deleteFileObjs[i].getFileParentPath(), deleteFileObjs[i].getFileName(), deleteFileObjs[i].getLastModified());
                }
                SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.FILE_DELETE, parentPath, null, null, nameList, null, 0, CopyTypeArgument.SAMB_TO_CLOUD, msgObj.getMsgId());
            }
        }
    }

    public void sendDelteFileCompleteWhenCopyFileFromSambToCloud(String msgId, boolean isOk) {
        MsgObj msgObj = FileManagerApplication.msgMap.get(msgId);
        if (!isOk) {
            ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
        } else {
            ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
        }
        dissMissRemoteCopyProgressDialog();
        removeMsgOjbFromMap(msgObj);
        FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
        if (fileListFragment != null) {
            if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE) {
                RemoteVFile remoteVFile = (RemoteVFile) fileListFragment.getIndicatorFile();
                fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD, false);
            } else if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_SAMBA_STORAGE) {
                fileListFragment.startScanFile(fileListFragment.getIndicatorFile(), ScanType.SCAN_CHILD, false);
            }
        }
    }

    // Delete the previewed/shared files of cloud storages which were saved in folder of ".cfile"
    // under the external cache directory for cloud storage preview feature
    public void deleteCloudStorageCache() {
        if (FileUtility.isExternalStorageAvailable() && getActivity().getExternalCacheDir() != null) {
            LocalVFile cfile = new LocalVFile(getActivity().getExternalCacheDir(), ".cfile/");
            if (!cfile.exists()) {
                try {
                    cfile.mkdirs();
                    new LocalVFile(cfile, ".nomedia").createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                LocalVFile[] cacheFiles = new LocalVFile(cfile).listVFiles();
                if (cacheFiles != null) {
                    for (LocalVFile file : cacheFiles) {
                        if (!file.getName().equals(".nomedia")) {
                            if (DEBUG)
                                Log.i(TAG, "delete " + file.getName());
                            file.delete();
                        }
                    }
                }
            }
        }
    }

    private void clearThumbnailEntries() {
        if (mThumbnailEntries.size() > 0) {
            for (int i = mThumbnailEntries.size() - 1; i >= 0; i--) {
                mThumbnailEntries.remove(i);
            }
        }
    }

    public RemoteVFile getPathIndicatorVFile() {
        return mCurrentIndicatorVFile;
    }

    public ArrayList<cloudStorage> getCloudStorageList() {
        return cloudStorageList;
    }

    public cloudStorage getCloudStorageItem(cloudStorage storage) {
        cloudStorage targetStorage = null;
        if (cloudStorageList != null && cloudStorageList.size() > 0) {
            for (int i = 0; i < cloudStorageList.size(); i++) {
                if (cloudStorageList.get(i).equals(storage)) {
                    targetStorage = cloudStorageList.get(i);
                    break;
                }
            }
        }
        return targetStorage;
    }

    public void setListUIAction(int value) {
        mListUIAction = value;
    }

    public int getListUIAction() {
        return mListUIAction;
    }

    public boolean hasFile(String path, String original) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            File[] temp = file.listFiles();
            for (int i = 0; i < temp.length; i++) {
                Log.d("=====temp[i].getName()========", temp[i].getName());
                Log.d("=====original========", original);
                if (temp[i].getName().contentEquals(original)) {
                    return true;
                }
            }
        }
        return false;
    }

    //++felix_zhang
    private void deleteFiles(VFile[] deleFiles) {
        if (deleFiles != null && deleFiles.length > 0) {
            for (VFile vFile : deleFiles) {
                if (vFile != null && vFile.exists()) {
                    if (vFile.isDirectory()) {
                        deleteFiles(vFile.listVFiles());
                    } else {
                        vFile.delete();
                    }
                }

            }
        }
    }

    public boolean deletFile(String fullpath) {
        File file = new File(fullpath);
        if (file.isDirectory())
            return false;
        return file.delete();
    }

    public synchronized boolean removeMsgOjbFromMap(MsgObj msgObj) {
        Log.d("felix_zhang", "removeMsgOjbFromMap(MsgObj msgObj) msgid:" + msgObj.getMsgId());
        if (msgObj != null && msgObj.getMsgId() != null) {
            if (currentOperateMsgObj != null && currentOperateMsgObj.getMsgId().equals(msgObj.getMsgId())) {
                currentOperateMsgObj = null;
            }
            return FileManagerApplication.msgMap.remove(msgObj.getMsgId()) != null;
        }
        return false;
    }

    public void removeMsgOjbFromMapHelper(MsgObj msgObj) {
        removeMsgOjbFromMap(msgObj);
    }

    private synchronized MsgObj appendMsgIDOtMsgObj(MsgObj msgObj, boolean isSaveToMsgMap, boolean isNeedAppendUUID) {
        if (isNeedAppendUUID) {
            String uuid = getMsgUUID();
            msgObj.setMsgId(uuid);
            if (isSaveToMsgMap) {
                FileManagerApplication.msgMap.put(uuid, msgObj);
            }
        }
        return msgObj;
    }

    public synchronized boolean removeDstPathFromMap(String msgId) {
        Log.d(TAG, "removeDstPathFromMap msgId:" + msgId);
        if (msgId != null) {
            return FileManagerApplication.msgDstPathMap.remove(msgId) != null;
        }
        return false;
    }

    public synchronized VFile appendDstPathToMap(String msgId, VFile dstPath) {
        if (msgId != null) {
            return FileManagerApplication.msgDstPathMap.put(msgId, dstPath);
        }
        return null;
    }

    private String getMsgUUID() {
        return UUID.randomUUID().toString();
    }

    private LocalVFile getCloudToOtherCloudCasheDir(MsgObj msgObj, Activity mActivity) {
        LocalVFile dest = null;
        if (msgObj != null && msgObj.getMsgId() != null) {
            File cacheDir = mActivity.getExternalCacheDir();
            if (cacheDir == null) {
                cacheDir = new File(WrapEnvironment.getEpadInternalStorageDirectory().getPath());
            }
            LocalVFile cfile = new LocalVFile(cacheDir, ".cfile/");
            if (!cfile.exists()) {
                cfile.mkdirs();
            }
            if (cfile != null && cfile.exists()) {
                dest = new LocalVFile(new VFile(cfile, msgObj.getMsgId() + File.separator));
                if (!dest.exists()) {
                    try {
                        dest.mkdirs();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Log.i(TAG, "create cloud to cloud cash file fail!");
                    }
                }
            }
        }
        return dest;
    }

    public boolean changeCloudInfo(MsgObj src, MsgObj dest) {
        String toStorageName = src.getStorageObj().getmToStorageName();
        String toDeviceId = src.getStorageObj().getToDeviceId();
        int toCloudType = src.getStorageObj().getmToOtherType();
        String toAccount = RemoteAccountUtility.getInstance(null).accountsMap.get(toStorageName + "_" + toCloudType).getToken();
        if (toStorageName != null && toCloudType > 0 && toAccount != null) {
            dest.getStorageObj().setAccount(toAccount);
            dest.getStorageObj().setmToStorageName(dest.getStorageObj().getStorageName());
            dest.getStorageObj().setmToOtherType(dest.getStorageObj().getStorageType());
            dest.getStorageObj().setToDeviceId(dest.getStorageObj().getDeviceId());
            dest.getStorageObj().setDeviceId(toDeviceId);
            dest.getStorageObj().setStorageName(toStorageName);
            dest.getStorageObj().setStorageType(toCloudType);
            return true;
        } else {
            return false;
        }
    }

    public void setHomeCloudRootPath(String rootPath) {
        mHomeCloudRootPath = rootPath;
    }

    public String getHomeCloudRootPath() {
        return mHomeCloudRootPath;
    }

    public void scanHomeCloudDevicesFile(RemoteVFile file) {
        RemoteFileUtility.isShowDevicesList = false;
        if (((FileManagerActivity) getActivity()).isMoveToDialogShowing()) {
            MoveToDialogFragment moveToFragment = (MoveToDialogFragment) getActivity().getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
            moveToFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
        } else {
            FileListFragment fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
            if (fileListFragment != null && file != null) {
                fileListFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
            }
        }
        setHomeCloudRootPath(file.getAbsolutePath());
    }

    public String getFoldListMapKey(MsgObj msgObj, boolean isHomeCloud) {
        FileObj fileObj = msgObj.getFileObjPath();
        if (isHomeCloud) {
            fileObj.setFileParentPath(addDeviceNameToParentPath(fileObj.getFileParentPath(), msgObj.getStorageObj().getDeviceId()));
        }
        String key = fileObj.getFullPath();
        String tempHead = msgObj.getStorageObj().getStorageType() + File.separator + msgObj.getStorageObj().getStorageName();
        key = key.equals(File.separator) ? tempHead : tempHead + key;
        key = key.length() > tempHead.length() && key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
        return key;
    }

    public String removeDeviceNameFromFileName(String fileName, String deviceId) {
        HomeCloudDeviceInfo info = RemoteVFile.homeCloudDeviceInfoMap.get(deviceId);
        if (info == null) {
            return fileName;
        }
        String deviceName = info.getDeviceName();
        if (deviceName == null || deviceName.length() <= 0) {
            return fileName;
        }
        if (null == fileName || fileName.equals("") || fileName.equals(File.separator)) {
            return fileName;
        } else {
            fileName = fileName.replace(deviceName, "");
        }
        return fileName;
    }

    public String addDeviceNameToParentPath(String parentPath, String deviceId) {
        HomeCloudDeviceInfo info = RemoteVFile.homeCloudDeviceInfoMap.get(deviceId);
        if (info == null) {
            return parentPath;
        }
        String deviceName = info.getDeviceName();
        if (deviceName == null || deviceName.length() <= 0) {
            return parentPath;
        }
        if (null == parentPath || parentPath.equals("") || parentPath.equals(File.separator)) {
            parentPath = File.separator + deviceName;
        } else {
            parentPath = File.separator + deviceName + parentPath;
        }
        return parentPath;
    }

    public String getCloudRealFileParentPath(String fileParentPath, RemoteVFile srcVFile, StorageObj storageObj) {
        storageObj.setUserId(((RemoteVFile) srcVFile).getmUserId());
        storageObj.setDeviceId(((RemoteVFile) srcVFile).getmDeviceId());
        fileParentPath = removeDeviceNameFromParentPath(fileParentPath, ((RemoteVFile) srcVFile).getmDeviceId());
        return fileParentPath;
    }

    public String getCloudRealFileName(String fileParentPath, String fileName, RemoteVFile srcVFile, StorageObj storageObj) {
        if (fileParentPath.equals(File.separator)
                && RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()) != null
                && fileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(((RemoteVFile) srcVFile).getmDeviceId()).getDeviceName())) {
            fileName = removeDeviceNameFromFileName(fileName, ((RemoteVFile) srcVFile).getmDeviceId());
        }
        return fileName;
    }

    public String removeDeviceNameFromParentPath(String parentPath, String deviceId) {
        HomeCloudDeviceInfo info = RemoteVFile.homeCloudDeviceInfoMap.get(deviceId);
        if (info == null) {
            return parentPath;
        }
        String deviceName = info.getDeviceName();
        if (deviceName == null || deviceName.length() <= 0) {
            return parentPath;
        }
        if (null == parentPath || parentPath.equals("") || parentPath.equals(File.separator)) {
            parentPath = File.separator;
        } else {
            parentPath = parentPath.replace(File.separator + deviceName, File.separator).replace(File.separator + File.separator, File.separator);

        }
        return parentPath;
    }

    public String getThumbnailStandard(int msgType) {
        String argument = "";
        switch (msgType) {
            case MsgObj.TYPE_BAIDUPCS_STORAGE:
                argument = "100x100";
                break;
            case MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE:
                argument = "100x100";
                break;
            case MsgObj.TYPE_DROPBOX_STORAGE:
                argument = DropBoxThumbnailSize.ICON_128x128;
                break;
            case MsgObj.TYPE_GOOGLE_DRIVE_STORAGE:
                argument = "";
                break;
            case MsgObj.TYPE_SKYDRIVE_STORAGE:
                argument = SkyDriveThumbnailSize.SMALL_96x96;
                break;
            case MsgObj.TYPE_HOMECLOUD_STORAGE:
                argument = "1";
                break;
            case 9:
                argument = "100x100";
                break;

            default:
                break;
        }
        return argument;
    }

    public static class CopyTypeArgument {
        public static int INIT_VALUE = -1;
        public static int CLOUD_TO_THE_CLOUD = 1;
        public static int SAMB_TO_CLOUD = 2;
        public static int CLOUD_TO_LOCAL = 3;
        public static int LOCAL_TO_CLOUD = 4;
        public static int CLOUD_TO_SAMB = 5;
        public static int CLOUD_OTHER_CLOUD = 6;
        public static int CLOUD_TO_DEVICE = 7;
        public static int DEVICE_TO_CLOUD = 8;
        public static int DEVICE_TO_DEVICE = 9;
        public static int DEVICE_TO_LOCAL = 10;
        public static int LOCAL_TO_DEVICE = 11;
        public static int DEVICE_TO_OTHER_DEVICE = 12;
        public static int DEVICE_TO_SAMB = 13;
        public static int SAMB_TO_DEVICE = 14;
    }
}
