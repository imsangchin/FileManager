package com.asus.remote.utility;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.SearchResultFragment;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.adapter.ItemIcon;
import com.asus.filemanager.dialog.InfoDialogFragment;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.dialog.SearchDialogFragment;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.provider.ProviderUtility.Thumbnail;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.PathIndicator;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteAccountUtility.AccountInfo;
import com.asus.remote.utility.RemoteFileUtility.CopyTypeArgument;
import com.asus.remote.utility.RemoteFileUtility.RemoteUIAction;
import com.asus.remote.utility.RemoteFileUtility.ThumbnailEntry;
import com.asus.remote.utility.RemoteFileUtility.cloudStorage;
import com.asus.service.cloudstorage.common.HandlerCommand;
import com.asus.service.cloudstorage.common.HandlerCommand.ApplicationHandlerMsg;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.HandlerCommand.CopyArgument;
import com.asus.service.cloudstorage.common.HandlerCommand.ErrMsg;
import com.asus.service.cloudstorage.common.HandlerCommand.ResultCode;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.common.MsgObj.FileObj;
import com.asus.service.cloudstorage.common.MsgObj.StorageObj;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteClientHandler extends Handler{
    private final String TAG = "RemoteClientHandler";
    private WeakReference<Activity> mActivity;
    private static boolean isSearchRemoteFileIgnoreFolder = true;
    private ArrayList<cloudStorage> cloudStorageList = new ArrayList<cloudStorage>();
    private Map<String, List<RemoteVFile>>currentVFilesMap = new HashMap<String, List<RemoteVFile>>();
    public Map<String, String>rootsFileIdmMap = new HashMap<String, String>();
    private int mRefreshTockenAccount = 0;
    private static boolean mIsFromFreshToken = false;
    private static RemoteClientHandler remoteClientHandler;

    private RemoteClientHandler() {

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

    public static RemoteClientHandler getInstance(Activity mActivity)
    {
        if(remoteClientHandler==null)
            remoteClientHandler = new RemoteClientHandler();
        if(mActivity!=null)
            remoteClientHandler.setActivity(mActivity);
        return remoteClientHandler;
    }

    public void  onResume(){

    }

    public void refrashTokenInvalidate(MsgObj obj){
        RemoteFileUtility remoteFileUtility = RemoteFileUtility.getInstance(getActivity());
        if (remoteFileUtility.currentBackUpMsgObj != null){
            String msgId = remoteFileUtility.currentBackUpMsgObj.getMsgObj().getMsgId();
            if (msgId != null && msgId.equals(obj.getMsgId()) ) {
                RemoteAccountUtility.getInstance(getActivity()).refreshToken(obj.getStorageObj().getStorageType(),obj.getStorageObj().getStorageName());
            }
        }
    }

    public void setIsFromFreshToken(boolean isFromFreshToken){
        mIsFromFreshToken = isFromFreshToken;
    }
    @Override
    public void handleMessage(Message msg) {
        Log.d(TAG, "GET remote storage msg: " + msg.what);

        Bundle data;
        StorageObj storageObj = null;
        MsgObj msgObj;
        FileObj[] files;
        FileObj fileObj;
        FolderElement folderElement;
        ShortCutFragment shortcutFragment;
        FileListFragment fileListFragment;
        MoveToDialogFragment moveToDialogFragment;
        //WifiDirectSearchFragment wifiSearchFragment;
        String deviceName = "";
        String address;
        RemoteVFile remoteVFile = null;
        String actionArgument;

        // get remote storage data
        data = msg.getData();
        data.setClassLoader(MsgObj.class.getClassLoader());
        msgObj = (MsgObj) data.getParcelable(HandlerCommand.BUNDLE_KEY_MSGOBJ);
        RemoteFileUtility remoteFileUtility = RemoteFileUtility.getInstance(getActivity());
        // stop discover command maybe msgObj is null, but we can do the following thing
        if (msgObj == null && msg.what != ApplicationHandlerMsg.MSG_DISCOVERY_SERVICE_STOPED) {
            return;
        } else if (msgObj != null) {
            storageObj = msgObj.getStorageObj();
        }
        //++felix_zhang  show dialog let the user login  when token is invalidated
        Log.i(TAG,"errmsg code:" + msgObj.getErrMsg() + " resultcode:" + msgObj.getResultCode());
        if (msgObj.getResultCode()==ResultCode.ERROR && (msgObj.getErrMsg()==ErrMsg.TOKEN_IS_INVALIDATE  || msgObj.getErrMsg()==ErrMsg.AUTHENTICATION_FAIL)) {
        /*	CloudStorageTokenOutOfDateDialogFragment fragment = (CloudStorageTokenOutOfDateDialogFragment) mActivity.getFragmentManager().findFragmentByTag(CloudStorageTokenOutOfDateDialogFragment.TAG);
            if (fragment==null||!fragment.isVisible()||fragment.isHidden()) {
                fragment = CloudStorageTokenOutOfDateDialogFragment.newInstance(msgObj.getStorageObj().getStorageType(),msgObj.getStorageObj().getStorageName());
                fragment.show(mActivity.getFragmentManager(), CloudStorageTokenOutOfDateDialogFragment.TAG);
            }*/
            Log.d(TAG, "mIsFromFreshToken = " + mIsFromFreshToken);
            if(mRefreshTockenAccount > 0 || mIsFromFreshToken){
                mRefreshTockenAccount = 0;
                fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                if (fileListFragment != null) {
                fileListFragment.startScanFile(new LocalVFile(FileListFragment.DEFAULT_INDICATOR_FILE), ScanType.SCAN_CHILD);
                }

            }else{
                Log.d("RemoteAccountUtility", "Token invalid, refresh token.");
                refrashTokenInvalidate(msgObj);
                mRefreshTockenAccount ++ ;
            }
            mIsFromFreshToken = false;
            //RemoteFileUtility.removeMsgOjbFromMap(msgObj);

            return;
        }
        switch (msg.what) {
//            case ApplicationHandlerMsg.MSG_STORAGE_CHANGE:
//                Log.d(TAG, "GET MSG_STORAGE_CHANGE");
//                int state = storageObj.getState();
//                int type = storageObj.getStorageType();
//                deviceName = storageObj.getStorageName();
//                address =  storageObj.getStorageAddress();
//                Log.d(TAG, "get remote storage info => name: " + deviceName + " state: " + state + " type: " + type);
//                switch (type) {
//                case MsgObj.TYPE_GOOGLE_DRIVE_STORAGE:
//                case MsgObj.TYPE_DROPBOX_STORAGE:
//                case MsgObj.TYPE_BAIDUPCS_STORAGE:
//                case MsgObj.TYPE_SKYDRIVE_STORAGE:
//                case MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE:
//                    Log.d(TAG, "get remote storage info => resultcode: " + msgObj.getResultCode());
//                    fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
//                    if (msgObj.getResultCode() == ResultCode.SUCCESS) {
//                        if (fileListFragment != null) {
//                            if (state == StorageObj.State.CONNECTED) {
//                                if (!deviceName.equals("")) {
//                                    cloudStorageList.add(new cloudStorage(deviceName, address, storageObj.getStorageType()));
//                                }
//                                fileListFragment.CloudStorageLoadingComplete();
//                            } else if (state == StorageObj.State.DISCONNECTED) {
//                                // if current page is in the remote storage, we jump to the default local storage page
//                                for (int i=0 ; i<cloudStorageList.size() ; i++) {
//                                    if (cloudStorageList.get(i).cloudStorageName.equals(deviceName) &&
//                                            cloudStorageList.get(i).cloudStorageId.equals(address)) {
//                                        cloudStorageList.remove(i);
//                                        break;
//                                    }
//                                }
//                                fileListFragment.backToDefaultPath();
//                            }
//                        }
//
//                        if (!deviceName.equals("")) {
//                            shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//                            if (shortcutFragment != null) {
//                                shortcutFragment.addCloudStorageAccount(deviceName, address, type, state);
//                            }
//                        }
//                } else if (msgObj.getResultCode() == ResultCode.ERROR) {
//                        if (fileListFragment != null) {
//                            fileListFragment.CloudStorageLoadingComplete();
//                            Toast.makeText(mActivity, mActivity.getResources().getString(R.string.login_fail), Toast.LENGTH_SHORT).show();
//                        }
//                    }
//
//                    break;
//                }
//
//                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_FILE_THUMBNAIL:
                Log.d(TAG, "GET MSG_RESPONE_FILE_THUMBNAIL ");//12345

                fileObj = msgObj.getFileObjPath();
                if (msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_HOMECLOUD_STORAGE) {
                    fileObj.setFileParentPath(remoteFileUtility.addDeviceNameToParentPath(fileObj.getFileParentPath(), msgObj.getStorageObj().getDeviceId()));
                }
                remoteVFile = new RemoteVFile(fileObj, storageObj);

                int accountType = storageObj.getStorageType();
                String accountName = storageObj.getStorageName();
                String fileName = remoteVFile.getName();
                String parentPath = remoteVFile.removeStorageNameParentPath(remoteVFile.getStorageName());

                //google drive rename the thumbnail name
                String thumbnailName = (remoteVFile.getStorageType() == StorageType.TYPE_GOOGLE_DRIVE) ? (fileName + File.separator + remoteVFile.getFileID()): fileName;
                ThumbnailEntry tempEntry = new ThumbnailEntry(accountType, accountName, thumbnailName, parentPath);

                for (int i=0 ; i<remoteFileUtility.mThumbnailEntries.size() ; i++) {
                    if (remoteFileUtility.mThumbnailEntries.get(i).equals(tempEntry)) {
                        remoteFileUtility.mThumbnailEntries.remove(i);
                        break;
                    }
                }
                if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                    Bitmap bitmap = fileObj.getThumbnail();
                    if (bitmap == null) {
                        Log.w(TAG, "bitmap is null");
                    } else {
                        try {
                            bitmap = ItemIcon.resizeDownAndCropCenter(bitmap, ItemIcon.THUMBNAIL_TARGET_SIZE, true);
                            // save remote thumbnail to DB
                            ContentResolver cr = getActivity().getContentResolver();
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            byte[] bitmapStream = stream.toByteArray();
                            stream.close();
                            //Thumbnail.setThumbnailAndTime(cr, remoteVFile.getAbsolutePath(), bitmapStream, remoteVFile.lastModified());

                            //google drive rename the thumbnail path
                            String thumbnailPath = (remoteVFile.getStorageType() == StorageType.TYPE_GOOGLE_DRIVE) ? (remoteVFile.getAbsolutePath() + File.separator + remoteVFile.getFileID()) : remoteVFile.getAbsolutePath();
                            Thumbnail.setThumbnailAndTime(cr, thumbnailPath, bitmapStream, remoteVFile.lastModified());
                        } catch (Exception e) {
                            Log.e(TAG, "Save bitmap error:" + e);
                        }
                        // update file list when the current indicator is remote storage and the updated thumbnail is in the list
                        //if (PathIndicator.getIndicatorVFileType() != VFileType.TYPE_LOCAL_STORAGE) {
                            if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE) {
                            SearchResultFragment searchResultFragment = (SearchResultFragment) getActivity().getFragmentManager().findFragmentById(R.id.searchlist);
                            fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);

                            if (fileListFragment != null && fileListFragment.isVisible() && !fileListFragment.isScrolling()) {
                                VFile[] tempVFiles = fileListFragment.getFileList();
                                if (tempVFiles != null && tempVFiles.length > 0) {
                                    try {
                                        for (int i=0 ; i<tempVFiles.length ; i++) {
                                            if (remoteVFile.getAbsolutePath().equals(tempVFiles[i].getCanonicalPath())) {
                                                if (remoteFileUtility.mRemoteFileListForThumbnail!=null && remoteFileUtility.mRemoteFileListForThumbnail.length>0 && remoteFileUtility.mRemoteFileListForThumbnail[0].getStorageType()!=-1) {
                                                     fileListFragment.remoteUpdateThumbnail(remoteFileUtility.mRemoteFileListForThumbnail);
                                                }else {
                                                    Log.i(TAG,"fileListFragment.remoteUpdateThumbnail storageType==-1");
                                                }
                                                Log.i(TAG,"fileListFragment.remoteUpdateThumbnail");
                                                //Log.i("felix_zhang","remoteVFile.getAbsolutePath()"+remoteVFile.getAbsolutePath()+" tempVFiles[i].getCanonicalPath():"+tempVFiles[i].getCanonicalPath());
                                                break;
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else if (searchResultFragment != null && searchResultFragment.isVisible() && !searchResultFragment.isScrolling()) {
                                VFile[] tempVFiles = searchResultFragment.getFileList();
                                if (tempVFiles != null && tempVFiles.length > 0) {
                                    try {
                                        for (int i=0 ; i<tempVFiles.length ; i++) {
                                            if (remoteVFile.getAbsolutePath().equals(tempVFiles[i].getCanonicalPath())) {
                                                searchResultFragment.remoteUpdateThumbnail(remoteFileUtility.mRemoteFileListForThumbnail);
                                                break;
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "GET MSG_RESPONE_FILE_THUMBNAIL return ResultCode is error");
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_FOLDER_LIST:
            case ApplicationHandlerMsg.MSG_RESPONE_PARENT_FOLDER_LIST:
                Log.d(TAG, "GET MSG_RESPONE_FOLDER_LIST");
                RemoteDataEntry remoteDataEntry;
                if (msgObj.getMsgId()==null) {
                    Log.i(TAG, "get folder list msgid is null");
                    break;
                }

                Log.i(TAG,"msgid:"+msgObj.getMsgId()+" resultCode:"+msgObj.getResultCode());
                String msgid = msgObj.getMsgId();
                StorageObj storageObjTemp = msgObj.getStorageObj();
                if (msgObj.getResultCode()==ResultCode.SUCCESS) {
                            files = msgObj.getFileObjFiles();
                            fileObj = msgObj.getFileObjPath();
                            RemoteVFile[] remoteVFiles = new RemoteVFile[(files==null)?0:files.length];
                            ArrayList<RemoteVFile>remoteVFiles2 = new ArrayList<RemoteVFile>();
                            RemoteVFile tempFile = null;
                            String newParentPath = null;
                            if(files!=null&&files.length>0&&storageObjTemp.getStorageType()==MsgObj.TYPE_HOMECLOUD_STORAGE){
                                newParentPath = remoteFileUtility.addDeviceNameToParentPath(files[0].getFileParentPath(), storageObjTemp.getDeviceId());
                            }
                            if (files!=null) {
                                for(int i = 0;i< files.length;i++) {
                                    if(storageObjTemp.getStorageType()==MsgObj.TYPE_HOMECLOUD_STORAGE){
                                        files[i].setFileParentPath(newParentPath);
                                    }
                                    tempFile = new RemoteVFile(files[i], storageObjTemp);
                                    if (storageObjTemp.getStorageType() != MsgObj.TYPE_WIFIDIRECT_STORAGE) {
                                        tempFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);
                                    }

                                    // update file id
                                    if (files[i].getFileId() != null) {
                                        tempFile.setFileID(files[i].getFileId());
                                        tempFile.setParentFileID(files[i].getParentId());
                                    }
                                    remoteVFiles2.add(tempFile);
                                }
                            }
                            if (currentVFilesMap.get(msgid)==null) {
                                currentVFilesMap.put(msgid, remoteVFiles2);
                            }else {
                                currentVFilesMap.get(msgid).addAll(remoteVFiles2);
                            }

                            if(msgObj.getEndPage()){
                                String key  = remoteFileUtility.getFoldListMapKey(msgObj,msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_HOMECLOUD_STORAGE);
                                  remoteDataEntry =(RemoteDataEntry)(remoteFileUtility.mRemoteUpdateUIMap.isEmpty() ? null : remoteFileUtility.mRemoteUpdateUIMap.remove(key));
                                List<RemoteVFile>remoteVFilesList = currentVFilesMap.get(msgid);
                                  remoteVFiles = new RemoteVFile[remoteVFilesList.size()];
                                 for (int i = 0; i < remoteVFiles.length; i++) {
                                    remoteVFiles[i]=remoteVFilesList.get(i);
                                 }
                                 currentVFilesMap.remove(msgid);
                                if (remoteDataEntry!=null) {
                                     RemoteVFile file;
                                     switch (remoteDataEntry.getUInumber()) {
                                     case RemoteUIAction.FILE_LIST_UI:
                                         Log.d(TAG, "handle FILE_LIST_UI case");
                                         RemoteFileUtility.mRemoteFileListError = false;
                                         RemoteFileUtility.mHomeCloudFileListError = false;
                                         //mRemoteFileList = remoteVFiles;
                                         file = remoteDataEntry.getRemoteVFile();
                                         if ((file.getFileID() != null) && (file.getFileID().equals("root"))) {
                                            rootsFileIdmMap.put(file.getMsgObjType()+"_"+file.getStorageName(),fileObj.getFileId());
                                         }
                                         //if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE) {
                                             String currentRealFilePath = PathIndicator.getRealIndiatorPath();
                                             //Log.d("realPath:",currentRealFilePath);
                                             currentRealFilePath =PathIndicator.getIndicatorVfileStorageType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
                                            /*if (!((file.getStorageType()+"_"+file.getAbsolutePath()).startsWith(currentRealFilePath)) || SambaFileUtility.updateHostIp) {
                                                Log.d("realPath:","break");
                                                break;
                                            }*/
                                         //}

                                            //Log.d("realPath:",currentRealFilePath);
                                         if (remoteFileUtility.mRemoteFileListMap==null) {
                                             remoteFileUtility.mRemoteFileListMap=new HashMap<String, RemoteVFile[]>();
                                         }

                                         if (remoteVFiles != null && remoteVFiles.length > 0 && file.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                                             file = remoteVFiles[0].getParentFile();
                                             file.setFileID(remoteVFiles[0].getParentFileID()); // update the indicator file id
                                         }
                                         remoteFileUtility.mRemoteFileListMap.put(file.getAbsolutePath(), remoteVFiles);
                                         file.setStorageType(storageObjTemp.getStorageType());
                                         Log.e(TAG,"storageObjTemp.getStorageType():"+storageObjTemp.getStorageType());
                                         // update cloud storage path indicator vfile
                                         if(fileObj!=null&&fileObj.getFileId()!=null){
                                             file.setFileID(fileObj.getFileId());
                                             rootsFileIdmMap.put(file.getMsgObjType()+"_"+file.getStorageName(),fileObj.getFileId());
                                         }
                                         remoteFileUtility.mCurrentIndicatorVFile = file;
                                         fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                                         Log.d(TAG,"==((FileManagerActivity)mActivity).isMoveToDialogShowing()=="  + ((FileManagerActivity)getActivity()).isMoveToDialogShowing());
                                         if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()){
                                             MoveToDialogFragment moveToDialog  = (MoveToDialogFragment)getActivity().getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                                             if( moveToDialog != null && PathIndicator.getMoveToIndicatorVFileType() == file.getVFieType())
                                                 moveToDialog.startScanFile(file, ScanType.SCAN_CHILD , false);
                                         }else{
                                             if (fileListFragment != null && PathIndicator.getIndicatorVFileType() == file.getVFieType()) {
                                                 fileListFragment.startScanFile(file, ScanType.SCAN_CHILD , false);
                                             }
                                         }

                                         break;
                                     case RemoteUIAction.FOLDER_LIST_UI:
                                         Log.d(TAG, "handle FOLDER_LIST_UI case");
                                         remoteFileUtility.mRemoteFolderList = remoteVFiles;
                                         folderElement = remoteDataEntry.getFolderElement();
                                         remoteFileUtility.mRemoteFileListMap.put(folderElement.getFile().getAbsolutePath(), remoteVFiles);
//	                                     shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//	                                     if (shortcutFragment != null) {
//	                                         shortcutFragment.expandStroage(folderElement);
//	                                     }
                                         break;
                                     case RemoteUIAction.FOLDER_CHILD_LIST_UI:
                                         Log.d(TAG, "handle FOLDER_CHILD_LIST_UI case");
                                         remoteFileUtility.mRemoteFolderList = remoteVFiles;
                                         shortcutFragment = (ShortCutFragment) getActivity().getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//	                                     LinkedList<FolderElement> mFolderList = shortcutFragment.getFolderList();
                                         folderElement = remoteDataEntry.getFolderElement();
                                         remoteFileUtility.mRemoteFileListMap.put(folderElement.getFile().getAbsolutePath(), remoteVFiles);
//	                                     if (shortcutFragment != null) {
//	                                         shortcutFragment.expandChildFolder(mFolderList, folderElement, folderElement.isExpanded());
//	                                     }
                                         break;
                                     case RemoteUIAction.FILE_FOLDER_LIST_UI:
                                         Log.d(TAG, "handle FILE_FOLDER_LIST_UI case");
                                         remoteFileUtility.mRemoteFolderList = remoteVFiles;
                                         file = remoteDataEntry.getRemoteVFile();
                                         remoteFileUtility.mRemoteFileListMap.put(file.getAbsolutePath(), remoteVFiles);
//	                                     shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//	                                     if (shortcutFragment != null) {
//	                                         shortcutFragment.notifyIndicatorPathChange(file, true);
//	                                     }
                                         break;
                                     }
                                }
                                remoteFileUtility.removeMsgOjbFromMap(msgObj);
                       }

                }else if (msgObj.getResultCode()==ResultCode.ERROR) {
                    showErrorMsg(getActivity(), msgObj.getErrMsg());
                    currentVFilesMap.remove(msgid);
                    remoteFileUtility.removeMsgOjbFromMap(msgObj);
                    String key = remoteFileUtility.getFoldListMapKey(msgObj,msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_HOMECLOUD_STORAGE);
                      remoteDataEntry =
                            (RemoteDataEntry)(remoteFileUtility.mRemoteUpdateUIMap.isEmpty() ? null : remoteFileUtility.mRemoteUpdateUIMap.remove(key));
                      if (remoteDataEntry==null) {
                        return;
                       }
                     switch (remoteDataEntry.getUInumber()) {
                     case RemoteUIAction.FILE_LIST_UI:
                         fileObj = msgObj.getFileObjPath();
                         Log.d(TAG, "we stop the loading file list page");
                         RemoteFileUtility.mRemoteFileListError = true;
                         if(msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_HOMECLOUD_STORAGE){
                             RemoteFileUtility.mHomeCloudFileListError = true;
                         }else{
                             RemoteFileUtility.mHomeCloudFileListError = false;
                         }
                         RemoteVFile[] remoteVFiles = new RemoteVFile[0];
                        // mRemoteFileList = remoteVFiles;
                         RemoteVFile file = remoteDataEntry.getRemoteVFile();
                         String currentRealFilePath = PathIndicator.getRealIndiatorPath();
                         currentRealFilePath =PathIndicator.getIndicatorVfileStorageType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
                        /*if (!((file.getStorageType()+"_"+file.getAbsolutePath()).startsWith(currentRealFilePath)) || SambaFileUtility.updateHostIp) {
                            break;
                        }*/
                         if (fileObj.getFileId()!=null) {
                            file.setFileID(fileObj.getFileId());
                        }
                         remoteFileUtility.mRemoteFileListMap.put(file.getAbsolutePath(), remoteVFiles);
                         fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                         if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()){
                             MoveToDialogFragment moveToDialog  = (MoveToDialogFragment)getActivity().getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                             if( moveToDialog != null && PathIndicator.getIndicatorVFileType() == file.getVFieType())
                                 moveToDialog.startScanFile(file, ScanType.SCAN_CHILD , false);
                         }else{
                             if (fileListFragment != null && PathIndicator.getIndicatorVFileType() == file.getVFieType()) {
                                 fileListFragment.startScanFile(file, ScanType.SCAN_CHILD , false);
                             }
                         }
                         break;
                     case RemoteUIAction.FOLDER_LIST_UI:
                         Log.d(TAG, "we stop the loading folder list page");
                         Toast.makeText(getActivity(), "loading remote data fail", Toast.LENGTH_SHORT).show();
//                         shortcutFragment = (ShortCutFragment) getActivity().getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//                         if (shortcutFragment != null) {
//                             shortcutFragment.stopFolderListAnim();
//                         }
                         break;
                     case RemoteUIAction.FOLDER_CHILD_LIST_UI:
                         Log.d(TAG, "we stop the loading folder child list page");
                         Toast.makeText(getActivity(), "loading remote data fail", Toast.LENGTH_SHORT).show();
//                         shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//                         if (shortcutFragment != null) {
//                             shortcutFragment.stopFolderListAnim();
//                         }
                         break;
                     case RemoteUIAction.FILE_FOLDER_LIST_UI:
                         Log.d(TAG, "we stop the loading file and folder child list page");
                         Toast.makeText(getActivity(), "loading remote data fail", Toast.LENGTH_SHORT).show();
//                         shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//                         if (shortcutFragment != null) {
//                             shortcutFragment.stopFolderListAnim();
//                         }
                         break;
                     }
                }

                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_COPY_FILE_TO_CLOUD:
                Log.d(TAG, "GET MSG_RESPONE_COPY_FILE_TO_REMOTE");
                MsgObj srcMsgObjLocalToCloud = FileManagerApplication.msgMap.get(msgObj.getMsgId());
                if (srcMsgObjLocalToCloud==null) {
                    return;
                }
                actionArgument = srcMsgObjLocalToCloud.getArgument();
                int copyTypeToCloud = srcMsgObjLocalToCloud.getCopyType();
                if (actionArgument == null) {
                    actionArgument = CopyArgument.Copy;
                }
                switch (msgObj.getResultCode()) {
                case ResultCode.SUCCESS:
                if (copyTypeToCloud == CopyTypeArgument.LOCAL_TO_CLOUD || copyTypeToCloud == CopyTypeArgument.LOCAL_TO_DEVICE) {
                   // remoteFileUtility.dissMissRemoteCopyProgressDialog();
                     remoteFileUtility.dissMissCloudCopyProgressDialog();
                    fileObj = msgObj.getFileObjPath();
                    FileObj[] fileObjs= msgObj.getFileObjFiles();
                    if (fileObj != null) {
                        //++felix_zhang delete local file and update ShortCutFragment
                        //++tim_hu  msgObj.getArgument() is null
                        if (actionArgument.equals(CopyArgument.Move)) {
                            VFile[] deleteFiles = new VFile[fileObjs.length];
                            for (int i = fileObjs.length - 1 ; i >= 0 ; i--) {
                                File deleteFile = new File(fileObjs[i].getFullPath());
                                deleteFiles[i]= new VFile(fileObjs[i].getFullPath());

                                if (deleteFile.isDirectory()
                                        && deleteFile.list() != null) {
                                    // when we move a category folder may keep some files
                                    // in this folder which are not media files.
                                    // in this case we cannot delete this folder to avoid delete wrong files.
                                } else {
                                    remoteFileUtility.deleteFileAndPath(deleteFile);
                                }

                            }

                            fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);

                            if (fileListFragment != null
                                    && fileListFragment.belongToCategoryFromMediaStore()) {
                                fileListFragment.reScanFile();
                            }

//                    		shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//                    		if (shortcutFragment != null) {
//                              shortcutFragment.updateTreeFromDelete(deleteFiles);
//                    		}
                        }
                        ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
                        // we only update file list when current indicator is at remote storage
                        if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_REMOTE_STORAGE) {
                            remoteVFile = new RemoteVFile(fileObj, storageObj);
                            // update indicator path because remote storage use shared path
                            remoteVFile.SetIndicatorPath(PathIndicator.getRealIndiatorPath());
                        } else if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE) {
                            if (copyTypeToCloud == CopyTypeArgument.LOCAL_TO_CLOUD) {
                                remoteVFile = new RemoteVFile(srcMsgObjLocalToCloud.getFileObjPath(), srcMsgObjLocalToCloud.getStorageObj());
                            }else {
                                FileObj tempSrcFileObj = srcMsgObjLocalToCloud.getFileObjPath();
                                String fileParentPath = remoteFileUtility.addDeviceNameToParentPath(tempSrcFileObj.getFileParentPath(), srcMsgObjLocalToCloud.getStorageObj().getDeviceId());
                                FileObj tempFileObj = new FileObj(tempSrcFileObj.getFileName(), fileParentPath, tempSrcFileObj.getIsDirectory(), tempSrcFileObj.getFileSize(), tempSrcFileObj.getLastModified(), tempSrcFileObj.getFilePermission(), tempSrcFileObj.getHasChild());
                                remoteVFile = new RemoteVFile(tempFileObj, srcMsgObjLocalToCloud.getStorageObj());
                            }
                            remoteVFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);
                        }
                        remoteFileUtility.removeMsgOjbFromMap(srcMsgObjLocalToCloud);
//hide for moveToDialog                        
//                        if (remoteVFile != null) {
//                        	String currentRealFilePath = PathIndicator.getRealIndiatorPath();
//                        	currentRealFilePath = PathIndicator.getIndicatorVfileStorageType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
//                          	Log.d(TAG,"dst:"+(remoteVFile.getStorageType()+"_"+remoteVFile.getAbsolutePath()));
//
//                          	if(currentRealFilePath.equalsIgnoreCase(remoteVFile.getStorageType()+"_"+remoteVFile.getAbsolutePath())){
//                            fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
//                            if (fileListFragment != null) {
//                                fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD);
//                            }
//                          	}
//                        }
                    } else {
                        Log.w(TAG, "get MSG_RESPONE_COPY_FILE_TO_REMOTE but fileObj is null");
                        remoteFileUtility.removeMsgOjbFromMap(srcMsgObjLocalToCloud);
                    }

                } else if (copyTypeToCloud == CopyTypeArgument.CLOUD_OTHER_CLOUD|| copyTypeToCloud == CopyTypeArgument.CLOUD_TO_DEVICE
                        || copyTypeToCloud == CopyTypeArgument.DEVICE_TO_CLOUD || copyTypeToCloud == CopyTypeArgument.DEVICE_TO_OTHER_DEVICE || copyTypeToCloud == CopyTypeArgument.SAMB_TO_CLOUD)
                        {
                    if (actionArgument.equals(CopyArgument.Copy)) {
                        remoteFileUtility.dissMissRemoteCopyProgressDialog();
                        fileObj = msgObj.getFileObjPath();
                        FileObj[] fileObjs= msgObj.getFileObjFiles();
                        if (fileObj != null) {
                            //++felix_zhang delete local file and update ShortCutFragment
                            VFile[] deleteFiles = new VFile[fileObjs.length];
                                for (int i=0;i<fileObjs.length;i++) {
                                    File deleteFile = new File(fileObjs[i].getFullPath());
                                    deleteFiles[i]= new VFile(fileObjs[i].getFullPath());
                                    remoteFileUtility.deleteFileAndPath(deleteFile);
                                }
                            if (fileObjs!=null&&fileObjs.length>0) {
                                File destFile = new File((new File(fileObjs[0].getFullPath()).getParent()));
                                if (destFile!=null&&destFile.exists()) {
                                    destFile.delete();
                                }
                            }
//                        	 shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//                              if (shortcutFragment != null) {
//                                  shortcutFragment.updateTreeFromDelete(deleteFiles);
//                              }

                            // we only update file list when current indicator is at remote storage
                            //changeCloudInfo(srcMsgObjLocalToCloud, srcMsgObjLocalToCloud);
                            if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_REMOTE_STORAGE) {
                                remoteVFile = new RemoteVFile(srcMsgObjLocalToCloud.getFileObjPath(), msgObj.getStorageObj());
                                // update indicator path because remote storage use shared path
                                remoteVFile.SetIndicatorPath(PathIndicator.getRealIndiatorPath());
                            } else if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE) {
                            //} else {
                                int samb_to_cloud_Type = -1;
                                if (copyTypeToCloud == CopyTypeArgument.SAMB_TO_CLOUD) {
                                    if (msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_HOMECLOUD_STORAGE) {
                                        samb_to_cloud_Type = CopyTypeArgument.SAMB_TO_DEVICE;
                                    }else {
                                        samb_to_cloud_Type = CopyTypeArgument.SAMB_TO_CLOUD;
                                    }
                                }
                                if (copyTypeToCloud == CopyTypeArgument.CLOUD_OTHER_CLOUD || copyTypeToCloud == CopyTypeArgument.DEVICE_TO_CLOUD ||samb_to_cloud_Type == CopyTypeArgument.SAMB_TO_CLOUD) {
                                    remoteVFile = new RemoteVFile(srcMsgObjLocalToCloud.getFileObjPath(), msgObj.getStorageObj());
                                }else {
                                    FileObj tempSrcFileObj = srcMsgObjLocalToCloud.getFileObjPath();
                                    String newFileName = tempSrcFileObj.getFileName();
                                    if(tempSrcFileObj.getFileParentPath().equals(File.separator)
                                            && RemoteVFile.homeCloudDeviceInfoMap.get(srcMsgObjLocalToCloud.getStorageObj().getToDeviceId()) != null
                                            && newFileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(srcMsgObjLocalToCloud.getStorageObj().getToDeviceId()).getDeviceName())){
                                        newFileName = remoteFileUtility.removeDeviceNameFromFileName(newFileName, srcMsgObjLocalToCloud.getStorageObj().getToDeviceId());
                                    }
                                    String deviceId = srcMsgObjLocalToCloud.getStorageObj().getToDeviceId();
                                    if (samb_to_cloud_Type == CopyTypeArgument.SAMB_TO_DEVICE) {
                                        deviceId = srcMsgObjLocalToCloud.getStorageObj().getDeviceId();
                                    }
                                    String fileParentPath = remoteFileUtility.addDeviceNameToParentPath(tempSrcFileObj.getFileParentPath(), deviceId);
                                    FileObj tempFileObj = new FileObj(newFileName, fileParentPath, tempSrcFileObj.getIsDirectory(), tempSrcFileObj.getFileSize(), tempSrcFileObj.getLastModified(), tempSrcFileObj.getFilePermission(), tempSrcFileObj.getHasChild());
                                    tempFileObj.setParentId(tempSrcFileObj.getParentId());
                                    tempFileObj.setFileId(tempSrcFileObj.getFileId());
                                    if (samb_to_cloud_Type == CopyTypeArgument.SAMB_TO_DEVICE) {
                                        remoteVFile = new RemoteVFile(tempFileObj, srcMsgObjLocalToCloud.getStorageObj());
                                    }else {
                                        StorageObj tempStorage = new StorageObj(srcMsgObjLocalToCloud.getStorageObj().toJsonObject());
                                        if(tempStorage!=null){
                                        tempStorage.setStorageType(tempStorage.getmToOtherType());
                                        tempStorage.setStorageName(tempStorage.getmToStorageName());
                                        tempStorage.setDeviceId(tempStorage.getToDeviceId());
                                        remoteVFile = new RemoteVFile(tempFileObj, tempStorage);
                                        }
                                    }
                                }
                                remoteVFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);
                            }
                            ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
                            remoteFileUtility.removeMsgOjbFromMap(srcMsgObjLocalToCloud);
//hide for MoveToDialog
//                            if (remoteVFile != null) {
//                            	String currentRealFilePath = PathIndicator.getRealIndiatorPath();
//                            	currentRealFilePath = PathIndicator.getIndicatorVfileStorageType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
//                              	Log.d(TAG,"dst:"+(remoteVFile.getStorageType()+"_"+remoteVFile.getAbsolutePath()));
//
//                              	if(currentRealFilePath.equalsIgnoreCase(remoteVFile.getStorageType()+"_"+remoteVFile.getAbsolutePath())){
//                                fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
//                                if (fileListFragment != null) {
//                                    fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD);
//                                }
//                              	}
//                            }
                        } else {
                            Log.w(TAG, "get MSG_RESPONE_COPY_FILE_TO_REMOTE but fileObj is null");
                            remoteFileUtility.removeMsgOjbFromMap(srcMsgObjLocalToCloud);
                        }
                    } else if (actionArgument.equals(CopyArgument.Move)) {
                        int samb_to_cloud_Type = -1;
                        if (copyTypeToCloud == CopyTypeArgument.SAMB_TO_CLOUD) {
                            if (msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_HOMECLOUD_STORAGE) {
                                samb_to_cloud_Type = CopyTypeArgument.SAMB_TO_DEVICE;
                            }else {
                                samb_to_cloud_Type = CopyTypeArgument.SAMB_TO_CLOUD;
                            }
                        }
                        if (copyTypeToCloud == CopyTypeArgument.CLOUD_OTHER_CLOUD || copyTypeToCloud == CopyTypeArgument.DEVICE_TO_CLOUD ||samb_to_cloud_Type == CopyTypeArgument.SAMB_TO_CLOUD) {
                            remoteVFile = new RemoteVFile(srcMsgObjLocalToCloud.getFileObjPath(), msgObj.getStorageObj());
                        }else {
                            FileObj tempSrcFileObj = srcMsgObjLocalToCloud.getFileObjPath();
                            String newFileName = tempSrcFileObj.getFileName();
                            if(tempSrcFileObj.getFileParentPath().equals(File.separator)
                                    && RemoteVFile.homeCloudDeviceInfoMap.get(srcMsgObjLocalToCloud.getStorageObj().getToDeviceId()) != null
                                    && newFileName.equals(RemoteVFile.homeCloudDeviceInfoMap.get(srcMsgObjLocalToCloud.getStorageObj().getToDeviceId()).getDeviceName())){
                                newFileName = remoteFileUtility.removeDeviceNameFromFileName(newFileName, srcMsgObjLocalToCloud.getStorageObj().getToDeviceId());
                            }
                            String deviceId = srcMsgObjLocalToCloud.getStorageObj().getToDeviceId();
                            if (samb_to_cloud_Type == CopyTypeArgument.SAMB_TO_DEVICE) {
                                deviceId = srcMsgObjLocalToCloud.getStorageObj().getDeviceId();
                            }
                            String fileParentPath = remoteFileUtility.addDeviceNameToParentPath(tempSrcFileObj.getFileParentPath(), deviceId);
                            FileObj tempFileObj = new FileObj(newFileName, fileParentPath, tempSrcFileObj.getIsDirectory(), tempSrcFileObj.getFileSize(), tempSrcFileObj.getLastModified(), tempSrcFileObj.getFilePermission(), tempSrcFileObj.getHasChild());
                            tempFileObj.setParentId(tempSrcFileObj.getParentId());
                            tempFileObj.setFileId(tempSrcFileObj.getFileId());
                            if (samb_to_cloud_Type == CopyTypeArgument.SAMB_TO_DEVICE) {
                                remoteVFile = new RemoteVFile(tempFileObj, srcMsgObjLocalToCloud.getStorageObj());
                            }else {
                                StorageObj tempStorage = new StorageObj(srcMsgObjLocalToCloud.getStorageObj().toJsonObject());
                                if(tempStorage!=null){
                                tempStorage.setStorageType(tempStorage.getmToOtherType());
                                tempStorage.setStorageName(tempStorage.getmToStorageName());
                                tempStorage.setDeviceId(tempStorage.getToDeviceId());
                                remoteVFile = new RemoteVFile(tempFileObj, tempStorage);
                                }
                            }
                        }
                        remoteVFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);
                        remoteFileUtility.appendDstPathToMap(msgObj.getMsgId(), remoteVFile);

                        remoteFileUtility.setProgress(msgObj.getCopyTotalSize()*2/3, msgObj.getCopyTotalSize());
                        if (copyTypeToCloud == CopyTypeArgument.SAMB_TO_CLOUD) {
                            remoteFileUtility.sendDeleteSambFileWhenCopyFileFromSambToCloud(msgObj.getMsgId());
                        }else {
                            remoteFileUtility.deliverRemoteMsg((MsgObj)srcMsgObjLocalToCloud, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_DELETE_FILES,false,false,true);
                        }
                    }
                }
                    break;
                case ResultCode.ERROR:
                    showErrorMsg(getActivity(), msgObj.getErrMsg());
                    if (!remoteFileUtility.removeMsgOjbFromMap(msgObj)) {
                        return;
                    }

                    if (copyTypeToCloud == CopyTypeArgument.CLOUD_OTHER_CLOUD|| copyTypeToCloud == CopyTypeArgument.CLOUD_TO_DEVICE ||  copyTypeToCloud == CopyTypeArgument.DEVICE_TO_OTHER_DEVICE
                            || copyTypeToCloud == CopyTypeArgument.DEVICE_TO_CLOUD || copyTypeToCloud == CopyTypeArgument.DEVICE_TO_OTHER_DEVICE || copyTypeToCloud == CopyTypeArgument.SAMB_TO_CLOUD)
                    {
                         fileObj = msgObj.getFileObjPath();
                         FileObj[] fileObjs= msgObj.getFileObjFiles();
                         VFile[] deleteFiles = new VFile[fileObjs.length];
                        for (int i=0;i<fileObjs.length;i++) {
                            File deleteFile = new File(fileObjs[i].getFullPath()) ;
                            deleteFiles[i]= new VFile(fileObjs[i].getFullPath());
                            remoteFileUtility.deleteFileAndPath(deleteFile);
                        }
                        if (fileObjs!=null&&fileObjs.length>0) {
                        File destFile = new File((new File(fileObjs[0].getFullPath()).getParent()));
                        if (destFile!=null&&destFile.exists()) {
                                destFile.delete();
                            }
                        }
//                 		shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
//                       if (shortcutFragment != null) {
//                           shortcutFragment.updateTreeFromDelete(deleteFiles);
//                       }
                    }

                    remoteFileUtility.dissMissRemoteCopyProgressDialog();
                     remoteFileUtility.dissMissCloudCopyProgressDialog();
                    ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
                    break;
                case ResultCode.UPDATE_COPY_PROGRESS:
                    Log.v(TAG, "UPDATE_COPY_PROGRESS => copySize: " + msgObj.getCopySize() + " copyTotalSize: " + msgObj.getCopyTotalSize());
                    double copySize = msgObj.getCopySize();
                    double totalSize = msgObj.getCopyTotalSize();
                    if (copyTypeToCloud == CopyTypeArgument.LOCAL_TO_CLOUD ||copyTypeToCloud == CopyTypeArgument.LOCAL_TO_DEVICE ||copyTypeToCloud == CopyTypeArgument.INIT_VALUE) {
                        //remoteFileUtility.setProgress(copySize,totalSize);
                        remoteFileUtility.setCopyCloudProgress(msgObj.getCurrentFile(), msgObj.getFileCounter(), msgObj.getCurrentFileProgress(), msgObj.getCurrentFileSize());
                    }else if (copyTypeToCloud == CopyTypeArgument.CLOUD_OTHER_CLOUD || copyTypeToCloud == CopyTypeArgument.CLOUD_TO_DEVICE || copyTypeToCloud == CopyTypeArgument.DEVICE_TO_CLOUD || copyTypeToCloud ==CopyTypeArgument.DEVICE_TO_DEVICE || copyTypeToCloud == CopyTypeArgument.DEVICE_TO_OTHER_DEVICE ||copyTypeToCloud == CopyTypeArgument.SAMB_TO_CLOUD) {
                        int div = actionArgument.equals(CopyArgument.Move)?3:2;
                        remoteFileUtility.setProgress(totalSize/div+copySize/div, totalSize);
                    }
                    break;
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_COPY_FILE_FROM_CLOUD:
                Log.e("felix_zhang","MSG_CLOUD_RESPONE_COPY_FILE_FROM_CLOUD close bug resultedcode:"+msgObj.getResultCode());

                MsgObj srcMsgObjCloudToLocal = FileManagerApplication.msgMap.get(msgObj.getMsgId());
                Log.d("felix_zhang",msgObj.getMsgId());
                Log.d("felix_zhang","FileManagerApplication.msgMap size():"+FileManagerApplication.msgMap.size());
                if (srcMsgObjCloudToLocal==null) {
                    Log.d("felix_zhang","srcMsgObjCloudToLocal is null");
                    Log.d("felix_zhang","srcMsgObjCloudToLocal is null:"+(srcMsgObjCloudToLocal==null));
                    Log.d("felix_zhang","currentOperateMsgObj is null:"+(remoteFileUtility.currentOperateMsgObj==null));
                    //Log.d("felix_zhang","remoteFileUtility.currentOperateMsgObj.getMsgId()!=srcMsgObjCloudToLocal.getMsgId() is null:"+remoteFileUtility.currentOperateMsgObj.getMsgId()!=srcMsgObjCloudToLocal.getMsgId());
                    return;
                }

                actionArgument = srcMsgObjCloudToLocal.getArgument();
                int copyType = srcMsgObjCloudToLocal.getCopyType();
                if (actionArgument == null) {
                    actionArgument = CopyArgument.Copy;
                }
                switch (msgObj.getResultCode()) {
                case ResultCode.SUCCESS:
                    if (copyType==CopyTypeArgument.CLOUD_TO_LOCAL) {
                          if (actionArgument.equals(CopyArgument.Copy)) {
                              Log.d("felix_zhang","remoteFileUtility.dissMissCloudCopyProgressDialog()");
                             // remoteFileUtility.dissMissRemoteCopyProgressDialog();
                              remoteFileUtility.dissMissCloudCopyProgressDialog();
                              ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
                              fileObj = msgObj.getFileObjPath();
                              if (fileObj==null) {
                                fileObj = srcMsgObjCloudToLocal.getFileObjPath();
                            }
                              deviceName = msgObj.getStorageObj().getStorageName();
                              if (fileObj != null) {
                                /**ToastUtility.show(mActivity, R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
                              /*	shortcutFragment = (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
                                if (shortcutFragment != null) {
                                    shortcutFragment.updateTreeFromPaste(fileObj.getFullPath());
                                }*/
                                  //++felix_zhang
                                LocalVFile localVFile = new LocalVFile(fileObj.getFullPath());
                                  if (localVFile != null&&localVFile.exists()) {
                                    localVFile.setVFileType(VFileType.TYPE_LOCAL_STORAGE);
                                    //MediaProviderAsyncHelper.addFolder(localVFile, true);
                                    FileUtility.saveMediaFilesToProvider(srcMsgObjCloudToLocal,msgObj);

 /**                                 	String currentRealFilePath = PathIndicator.getRealIndiatorPath();
                                    currentRealFilePath = PathIndicator.getIndicatorVFileType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
       hide for moveToDialog         Log.d(TAG,"dst:"+(localVFile.getVFieType()+"_"+localVFile.getAbsolutePath()));

                                    if(currentRealFilePath.equalsIgnoreCase(localVFile.getVFieType()+"_"+localVFile.getAbsolutePath())){
                                      fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
                                      if (fileListFragment != null) {
                                          fileListFragment.startScanFile(localVFile, ScanType.SCAN_CHILD);
                                      }
                                    }**/
                                  }
                              } else {
                                  Log.w(TAG, "get MSG_RESPONE_COPY_FILE_TO_REMOTE but fileObj is null");
                              }
                              remoteFileUtility.removeMsgOjbFromMap(msgObj);
                          } else if (actionArgument.equals(RemoteFileUtility.REMOTE_PREVIEW_ACTION)) {
//                        	  remoteFileUtility.dissMissRemoteCopyProgressDialog();
                              remoteFileUtility.dissMissPreViewProgressDialog();
                              fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                              if(fileListFragment == null){
                                  Log.e(TAG, "REMOTE_PREVIEW_ACTION, fileListFragment = NULL");
                                  return;
                              }
                             // fileListFragment.CloudStorageLoadingComplete();

                              FileObj[] fileObjs = msgObj.getFileObjFiles();
                              fileObj = msgObj.getFileObjPath();
                              String fullpath = fileObj.getFullPath() + File.separator +fileObjs[0].getFileName();
                              VFile previewVFile = new VFile(fullpath);
                              Log.d("------fullpath--------",fullpath);

//                              if (!previewVFile.exists()) {
//                                  Log.w(TAG, previewVFile.getAbsolutePath() + " doesn't exist");
//                              }

                              Uri uri = ProviderUtility.MediaFiles.insertFile(getActivity().getContentResolver(), previewVFile);
                              remoteFileUtility.removeMsgOjbFromMap(msgObj);
                              fileListFragment.openCloudStorageFile(previewVFile);
                          } else if (actionArgument.equals(RemoteFileUtility.REMOTE_SHARE_ACTION)) {
                              fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                              if(fileListFragment == null){
                                  Log.e(TAG, "REMOTE_SHARE_ACTION, fileListFragment = NULL");
                                  return;
                              }
                              fileListFragment.CloudStorageLoadingComplete();

                              FileObj[] fileObjs = srcMsgObjCloudToLocal.getFileObjFiles();
                              VFile[] sharedVFiles = new VFile[fileObjs.length];
                              VFile dstVFile = new LocalVFile(getActivity().getExternalCacheDir(), ".cfile/");

                              for (int i=0 ; i<fileObjs.length ; i++) {
                                  sharedVFiles[i] = new VFile(dstVFile.getPath()+File.separator+fileObjs[i].getFileName());
                                  Uri uri = ProviderUtility.MediaFiles.insertFile(getActivity().getContentResolver(), sharedVFiles[i]);
                              }

                              remoteFileUtility.removeMsgOjbFromMap(msgObj);
                              fileListFragment.shareCloudStorageFiles(sharedVFiles);
                          }else if (actionArgument.equals(RemoteFileUtility.REMOTE_COPY_TO_REMOTE_ACTION)) {//copy files from remote to remote
                              fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                                if(fileListFragment == null){
                                    Log.e(TAG, "REMOTE_COPY_TO_REMOTE_ACTION, fileListFragment = NULL");
                                    return;
                                }
                                fileListFragment.CloudStorageLoadingComplete();

                                FileObj[] fileObjs = msgObj.getFileObjFiles();
                                fileObj = msgObj.getFileObjPath();
                                String fullpath = fileObj.getFullPath() + File.separator +fileObjs[0].getFileName();
                                VFile previewVFile = new VFile(fullpath);
                                Log.d("------fullpath--------",fullpath);

//                                if (!previewVFile.exists()) {
//                                    Log.w(TAG, previewVFile.getAbsolutePath() + " doesn't exist");
//                                }

                                Uri uri = ProviderUtility.MediaFiles.insertFile(getActivity().getContentResolver(), previewVFile);
                                //upload the copy file from remote to remote
                                fileListFragment.openCloudStorageFile(previewVFile);
                                remoteFileUtility.removeMsgOjbFromMap(msgObj);
                          }else if (actionArgument.equals(CopyArgument.Move)) {
                              remoteFileUtility.deliverRemoteMsg((MsgObj)srcMsgObjCloudToLocal, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_DELETE_FILES,false,false,true);
                              //remoteFileUtility.setProgress(msgObj.getCopyTotalSize()/2, msgObj.getCopyTotalSize());
                              remoteFileUtility.setCopyCloudProgress(msgObj.getCurrentFile(), msgObj.getFileCounter(), msgObj.getCurrentFileProgress(), msgObj.getCurrentFileSize());
                              remoteFileUtility.setCopyStatus(2);
                        }

                    }else if (copyType==CopyTypeArgument.CLOUD_OTHER_CLOUD || copyType == CopyTypeArgument.DEVICE_TO_DEVICE || copyType == CopyTypeArgument.DEVICE_TO_OTHER_DEVICE || copyType == CopyTypeArgument.CLOUD_TO_DEVICE || copyType == CopyTypeArgument.DEVICE_TO_CLOUD ) {
                        MsgObj msgObj2 = new MsgObj(new StorageObj(msgObj.getStorageObj().toJsonObject()));
                        msgObj2.setMsgId(msgObj.getMsgId());
                        //msgObj2.setFileObjPath(srcMsgObjLocalToCloud);
                        FileObj destCloudFile = srcMsgObjCloudToLocal.getFileObjPath();
                        LocalVFile localVFile = new LocalVFile(new File(new File(getActivity().getExternalCacheDir(),".cfile/"),srcMsgObjCloudToLocal.getMsgId()));
                        FileObj[] srcFileObjFiles = null;
                        if (localVFile!=null&&localVFile.exists()&&localVFile.isDirectory()) {
                            LocalVFile[] localVFiles = localVFile.listVFiles();
                            srcFileObjFiles = new FileObj[localVFiles.length];
                            for (int i =0;i< localVFiles.length;i++) {
                                srcFileObjFiles[i]=new FileObj(localVFiles[i].getName(), localVFiles[i].getParent(), localVFiles[i].isDirectory(), 4096, localVFiles[i].lastModified(), "DWR", true);
                            }
                        }
                        msgObj2.setFileObjFiles(srcFileObjFiles);
                        msgObj2.setFileObjPath(destCloudFile);
                        msgObj2.setCopyType(copyType);
//						if (actionArgument.equals(CopyArgument.Copy)) {
//							remoteFileUtility.setProgress(msgObj.getCopyTotalSize()/2, msgObj.getCopyTotalSize());
//						}else if(actionArgument.equals(CopyArgument.Move)){
//							remoteFileUtility.setProgress(msgObj.getCopyTotalSize()/3, msgObj.getCopyTotalSize());
//						}
                        if (remoteFileUtility.changeCloudInfo(srcMsgObjCloudToLocal,msgObj2)&&srcFileObjFiles!=null) {
                            remoteFileUtility.deliverRemoteMsg((MsgObj)msgObj2, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_TO_REMOTE,false,false,false);
                        }else {
                            remoteFileUtility.dissMissRemoteCopyProgressDialog();
                             ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
                             remoteFileUtility.removeMsgOjbFromMap(srcMsgObjCloudToLocal);
                        }
                    }else if (copyType == CopyTypeArgument.DEVICE_TO_LOCAL) {
                        if (!actionArgument.equals(CopyArgument.Move)) {
                            remoteFileUtility.dissMissRemoteCopyProgressDialog();
                            RemoteFileOperateResultHelper.OperateCopyDeviceToLocal(getActivity(),msgObj,srcMsgObjCloudToLocal,actionArgument);
                        }else {
                            remoteFileUtility.deliverRemoteMsg((MsgObj)srcMsgObjCloudToLocal, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_DELETE_FILES,false,false,true);
                            //remoteFileUtility.setProgress(msgObj.getCopyTotalSize()/2, msgObj.getCopyTotalSize());
                            remoteFileUtility.setCopyCloudProgress(msgObj.getCurrentFile(), msgObj.getFileCounter(), msgObj.getCurrentFileProgress(), msgObj.getCurrentFileSize());
                            remoteFileUtility.setCopyStatus(2);
                        }
                    }else if (copyType == CopyTypeArgument.CLOUD_TO_SAMB) {
                        LocalVFile localVFile = new LocalVFile(new File(new File(getActivity().getExternalCacheDir(),".cfile/"),srcMsgObjCloudToLocal.getMsgId()));
                        if (localVFile!=null&&localVFile.exists()&&localVFile.isDirectory()) {
                            LocalVFile[] localVFiles = localVFile.listVFiles();
                            SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.FILE_PASTE, localVFiles, srcMsgObjCloudToLocal.getFileObjPath().getFileParentPath(), actionArgument.equals(CopyArgument.Move), copyType, msgObj.getMsgId());
//							if (actionArgument.equals(CopyArgument.Copy)) {
//								remoteFileUtility.setProgress(msgObj.getCopyTotalSize()/2, msgObj.getCopyTotalSize());
//							}else if(actionArgument.equals(CopyArgument.Move)){
//								remoteFileUtility.setProgress(msgObj.getCopyTotalSize()/3, msgObj.getCopyTotalSize());
//							}
                        }
                    }
                    break;
                case ResultCode.ERROR:
                    showErrorMsg(getActivity(), msgObj.getErrMsg());
                    if (copyType==CopyTypeArgument.CLOUD_TO_LOCAL) {
                        if (actionArgument.equals(CopyArgument.Copy)||actionArgument.equals(CopyArgument.Move) || actionArgument.equals(RemoteFileUtility.REMOTE_PREVIEW_ACTION) || actionArgument.equals(RemoteFileUtility.REMOTE_SHARE_ACTION)) {
                            fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                            if(fileListFragment != null){
                                 fileListFragment.CloudStorageLoadingComplete();
   //	                 deleteCloudStorageCache();
                       //delete unfinished download file
                       FileObj[] fileObjs = msgObj.getFileObjFiles();
                     fileObj = msgObj.getFileObjPath();
                     if(fileObj != null){
                         String fullpath = fileObj.getFullPath() + File.separator +fileObjs[0].getFileName();
                       if(fullpath!=null && remoteFileUtility.deletFile(fullpath)){
                         Log.d(TAG, "delete unfinished file success "+fullpath);
                       }
                     }

                           // remoteFileUtility.dissMissRemoteCopyProgressDialog();
                       remoteFileUtility.dissMissCloudCopyProgressDialog();
                     if (actionArgument.equals(RemoteFileUtility.REMOTE_PREVIEW_ACTION)) {
//                         remoteFileUtility.dissMissRemoteCopyProgressDialog();
                         remoteFileUtility.dissMissPreViewProgressDialog();
                         ToastUtility.show(getActivity(), R.string.cloud_preview_action_fail, Toast.LENGTH_SHORT);
                                   }else if (actionArgument.equals(RemoteFileUtility.REMOTE_SHARE_ACTION)) {
                                         ToastUtility.show(getActivity(), R.string.cloud_share_action_fail, Toast.LENGTH_SHORT);
                                   }else {
                                         ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
                                   }
                              }
                        }

                    }else if (copyType==CopyTypeArgument.CLOUD_OTHER_CLOUD || copyType == CopyTypeArgument.CLOUD_TO_SAMB) {
                        LocalVFile cfile = new LocalVFile(getActivity().getExternalCacheDir(), ".cfile/");
                            LocalVFile deleteFile = new LocalVFile(new VFile(cfile, msgObj.getMsgId()+File.separator));
                            if (deleteFile!=null&&deleteFile.exists()) {
                                remoteFileUtility.deleteFileAndPath(deleteFile);
                            }
                        //cancel copy file
                        remoteFileUtility.sendCloudStorageMsg(msgObj.getStorageObj().getStorageName(), null, null, msgObj.getStorageObj().getStorageType(), CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL);

                        remoteFileUtility.dissMissRemoteCopyProgressDialog();
                        ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
                    }else if (copyType==CopyTypeArgument.DEVICE_TO_LOCAL) {
                        fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                        if(fileListFragment == null){
                            break;
                        }
                        fileListFragment.CloudStorageLoadingComplete();
//                        deleteCloudStorageCache();
                        //delete unfinished download file
                        FileObj[] fileObjs = msgObj.getFileObjFiles();
                        fileObj = msgObj.getFileObjPath();
                        String fullpath = fileObj.getFullPath() + File.separator +fileObjs[0].getFileName();
                        if(fullpath!=null && remoteFileUtility.deletFile(fullpath))
                            Log.d(TAG, "delete unfinished file success "+fullpath);
                        //remoteFileUtility.dissMissRemoteCopyProgressDialog();

                        if (actionArgument.equals(RemoteFileUtility.REMOTE_PREVIEW_ACTION)) {
//                        	RemoteFileUtility.dissMissRemoteCopyProgressDialog();
                             remoteFileUtility.dissMissPreViewProgressDialog();
                            ToastUtility.show(getActivity(), R.string.cloud_preview_action_fail, Toast.LENGTH_SHORT);
                        }else if (actionArgument.equals(RemoteFileUtility.REMOTE_SHARE_ACTION)) {
                            ToastUtility.show(getActivity(), R.string.cloud_share_action_fail, Toast.LENGTH_SHORT);
                        }else {
                            remoteFileUtility.dissMissCloudCopyProgressDialog();
                            ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
                        }
                  }
                    remoteFileUtility.removeMsgOjbFromMap(msgObj);
//
                    break;

                case ResultCode.UPDATE_COPY_PROGRESS:
                    Log.v(TAG, "UPDATE_COPY_PROGRESS => copySize: " + msgObj.getCopySize() + " copyTotalSize: " + msgObj.getCopyTotalSize());
                    double copySize = msgObj.getCopySize();
                    double totalSize = msgObj.getCopyTotalSize();
                    if (copyType==CopyTypeArgument.CLOUD_TO_LOCAL||copyType == CopyTypeArgument.DEVICE_TO_LOCAL || copyType == CopyTypeArgument.INIT_VALUE) {
                        if (actionArgument.equals(CopyArgument.Copy)) {
                            //remoteFileUtility.setProgress(copySize,totalSize);
                            remoteFileUtility.setCopyCloudProgress(msgObj.getCurrentFile(), msgObj.getFileCounter(), msgObj.getCurrentFileProgress(), msgObj.getCurrentFileSize());
                        }else if(actionArgument.equals(RemoteFileUtility.REMOTE_PREVIEW_ACTION)) {
                            remoteFileUtility.setPreViewProgress(copySize,totalSize);
                        }else {
                            //remoteFileUtility.setProgress(copySize/2,totalSize);
                            remoteFileUtility.setCopyCloudProgress(msgObj.getCurrentFile(), msgObj.getFileCounter(), msgObj.getCurrentFileProgress(), msgObj.getCurrentFileSize());
                            Log.e("felix_zhang","copysize:"+(copySize/2)+"totalSize:"+totalSize);
                        }
                    }else if (copyType==CopyTypeArgument.CLOUD_OTHER_CLOUD|| copyType == CopyTypeArgument.CLOUD_TO_DEVICE || copyType == CopyTypeArgument.DEVICE_TO_CLOUD||copyType == CopyTypeArgument.DEVICE_TO_DEVICE ||copyType == CopyTypeArgument.DEVICE_TO_OTHER_DEVICE || copyType == CopyTypeArgument.CLOUD_TO_SAMB) {
                        if (actionArgument.equals(CopyArgument.Copy)) {
                            remoteFileUtility.setProgress(copySize/2,totalSize);
                        }else {
                            remoteFileUtility.setProgress(copySize/3,totalSize);
                        }
                    }

                    break;
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_COPY_FILE_UPDATE_CLOUD:
                actionArgument = msgObj.getArgument();
                if (actionArgument == null) {
                    actionArgument = CopyArgument.Copy;
                }
                Log.d(TAG, "GET MSG_CLOUD_RESPONE_COPY_FILE_UPDATE_CLOUD action: " + actionArgument);

                switch (msgObj.getResultCode()) {
                case ResultCode.WARNING_COPY_FOLDER:
                case ResultCode.SUCCESS:

                    if (actionArgument.equals(CopyArgument.Copy) || actionArgument.equals(CopyArgument.Move)) {
                        remoteFileUtility.dissMissRemoteCopyProgressDialog();
                        fileObj = msgObj.getFileObjPath();
                        if (fileObj != null) {
                            if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                                ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
                            } else {
                                ToastUtility.show(getActivity(), R.string.toast_warning_copy_folders, Toast.LENGTH_SHORT);
                            }
                            if (actionArgument.equals(CopyArgument.Move)) {
                                fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                                if (fileListFragment != null) {
                                    fileListFragment.startScanFile(fileListFragment.getIndicatorFile(), ScanType.SCAN_CHILD);
                                }
                            }
                            // we only update file list when current indicator is at remote storage
//                            if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE) {
//                            	String newParentPath = null;
// hide for move to Dialog                           	if (msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_HOMECLOUD_STORAGE) {
//                            		newParentPath = remoteFileUtility.addDeviceNameToParentPath(fileObj.getFileParentPath(), msgObj.getStorageObj().getDeviceId());
//                            		fileObj.setFileParentPath(newParentPath);
//								}
//                                remoteVFile = new RemoteVFile(fileObj, storageObj);
//                                remoteVFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);
//                            }
//                            if (remoteVFile != null) {
//                                fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
//                                if (fileListFragment != null) {
//                                    fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD);
//                                }
//                            }
                        } else {
                            Log.w(TAG, "get MSG_RESPONE_COPY_FILE_TO_REMOTE but fileObj is null");
                        }
                    }
                    remoteFileUtility.removeMsgOjbFromMap(msgObj);
                    break;

                case ResultCode.ERROR:
                    remoteFileUtility.dissMissRemoteCopyProgressDialog();
                    ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
                    remoteFileUtility.removeMsgOjbFromMap(msgObj);
                    break;
                case ResultCode.UPDATE_COPY_PROGRESS:
                    Log.v(TAG, "UPDATE_COPY_PROGRESS => copySize: " + msgObj.getCopySize() + " copyTotalSize: " + msgObj.getCopyTotalSize());
                    double copySize = msgObj.getCopySize();
                    double totalSize = msgObj.getCopyTotalSize();
                    remoteFileUtility.setProgress(copySize,totalSize);
                    break;
                }
                break;


            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_FILE_INFO:
                Log.d(TAG, "GET MSG_RESPONE_FILE_INFO");

                if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                    fileObj = msgObj.getFileObjPath();
                    int fileSize = (int)fileObj.getFileSize();
                    int fileNum = (int)fileObj.getAllChildCount();
                    InfoDialogFragment infoDialog = (InfoDialogFragment) getActivity().getFragmentManager().findFragmentByTag("InfoDialogFragment");
                    if (infoDialog != null) {
                        infoDialog.updateRemoteFileInfo(fileSize, fileNum);
                    } else {
                        Log.w(TAG, "infoDialog is null when getting MSG_RESPONE_FILE_INFO msg");
                    }
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_FILE_URI:
                Log.d(TAG, "GET MSG_RESPONE_FILE_INFO");
                fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                if(fileListFragment == null){
                    Log.e(TAG, "MSG_CLOUD_RESPONE_FILE_URI, fileListFragment = NULL");
                    return;
                }
                fileListFragment.CloudStorageLoadingComplete();
                if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                    fileObj = msgObj.getFileObjPath();
                    String sourceUri = fileObj.getSourceUri();
                    int storageType = msgObj.getStorageObj().getStorageType();
                    String storageName = msgObj.getStorageObj().getStorageName();
                    if (storageType == MsgObj.TYPE_GOOGLE_DRIVE_STORAGE) {
                         String key = storageName+"_"+storageType;
                         AccountInfo info = RemoteAccountUtility.getInstance(getActivity()).accountsMap.get(key);
                        sourceUri += "&access_token=" + ((info==null)?"":info.getToken());
                    }

                    if(FileUtility.isAvailableOpenWithOtherApp(getActivity(), fileObj.getFileName()) > -1){
                        FileUtility.openMusicOrGalleryWithUri(getActivity(), fileObj.getFileName(), sourceUri,storageType,storageName);
                    }else{
                        SambaFileUtility.getInstance(getActivity()).playCloudHttpsMediaFile(msgObj);
                    }
     
                }else {
                    ToastUtility.show(getActivity(), R.string.open_fail);
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_GET_DEVICE_LIST:
                 fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                      if(fileListFragment == null){
                  Log.e(TAG, "MSG_CLOUD_RESPONE_GET_DEVICE_LIST, fileListFragment = NULL");
                  return;
              }
                if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                     Log.e(TAG, "MSG_CLOUD_RESPONE_GET_DEVICE_LIST, success!");
                    RemoteVFile.homeCloudDeviceInfoMap.clear();
                    StorageObj[] storages = msgObj.getStorageObjs();
                    int currentRealStorageType = PathIndicator.getIndicatorVfileStorageType();
                    if (currentRealStorageType != StorageType.TYPE_HOME_CLOUD) {
                        //remoteFileUtility.isShowDevicesList =false;
                        //fileListFragment.getActivity().invalidateOptionsMenu();
                        Log.d(TAG,"change path to other dir not handle the message");
                        break;
                    }

                    //storages = null;
                    if (storages!=null&&storages.length>0) {
                        VFile[] deviceVfiles = new VFile[storages.length];
                        RemoteVFile vFile =null;
                        for (int i = 0; i < storages.length; i++) {
                             vFile = new RemoteVFile("/"+storages[i].getUserId()+"/" +storages[i].getStorageName() , VFileType.TYPE_CLOUD_STORAGE, storages[i].getUserId(), StorageType.TYPE_HOME_CLOUD, storages[i].getStorageName());
                             vFile.setmDeviceStatus(storages[i].getState());
                             vFile.setmDeviceId(storages[i].getDeviceId());
                             vFile.setmUserId(storages[i].getUserId());
                             vFile.setFileID("root");
                             vFile.setmIsDirectory(true);
                             RemoteVFile.homeCloudDeviceInfoMap.put(storages[i].getDeviceId(), new HomeCloudDeviceInfo(storages[i].getUserId(), storages[i].getDeviceId(), storages[i].getStorageName(), storages[i].getState()));
                             deviceVfiles[i]=vFile;
                        }


                        RemoteFileUtility.isShowDevicesList = true;
                        if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()){
                            MoveToDialogFragment moveToDialog  = (MoveToDialogFragment)getActivity().getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                            moveToDialog.getHomeBoxDeviceListFinish(deviceVfiles);
                        }else if (fileListFragment != null) {
                            fileListFragment.getHomeBoxDeviceListFinish(deviceVfiles);
                        }
                    }else {
                        RemoteFileUtility.isShowDevicesList = true;
                        if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()){
                            MoveToDialogFragment moveToDialog  = (MoveToDialogFragment)getActivity().getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                            moveToDialog.getHomeBoxDeviceListFinish(new VFile[0]);
                        }else if (fileListFragment != null) {
                            fileListFragment.getHomeBoxDeviceListFinish(new VFile[0]);
                        }
                        ToastUtility.show(getActivity(), getActivity().getResources().getString(R.string.cloud_homebox_no_available_devices));
                    }
                }else {
                    RemoteFileUtility.isShowDevicesList = true;
                    if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()){
                        MoveToDialogFragment moveToDialog  = (MoveToDialogFragment)getActivity().getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                        moveToDialog.getHomeBoxDeviceListFinish(new VFile[0]);
                    }else if (fileListFragment != null) {
                        fileListFragment.getHomeBoxDeviceListFinish(new VFile[0]);
                    }
                    ToastUtility.show(getActivity(), getActivity().getResources().getString(R.string.cloud_homebox_get_devices_fail));
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_CREATE_FOLDER:
                Log.d(TAG, "GET MSG_RESPONE_CREATE_FOLDER");
                fileObj = msgObj.getFileObjPath();
                FileObj[] newFolderName = msgObj.getFileObjFiles();
                if (fileObj == null) {
                    Log.d(TAG, "get fileObj is null");
                    break;
                }
                if (newFolderName == null) {
                    Log.d(TAG, "get newFolderName is null");
                    break;
                }
                remoteVFile = new RemoteVFile(fileObj, storageObj);
                if (msgObj.getStorageObj().getStorageType() == MsgObj.TYPE_HOMECLOUD_STORAGE) {
                    fileObj.setFileParentPath(remoteFileUtility.addDeviceNameToParentPath(fileObj.getFileParentPath(), msgObj.getStorageObj().getDeviceId()));
                }
                remoteVFile.setVFileType(VFileType.TYPE_CLOUD_STORAGE);

                if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                    if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()){
                        moveToDialogFragment = (MoveToDialogFragment) getActivity().getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                        if (moveToDialogFragment != null) {
                            moveToDialogFragment.addComplete();
                            if (moveToDialogFragment.getIndicatorFile()!=null && moveToDialogFragment.getIndicatorFile().getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
                                ToastUtility.show(getActivity(), R.string.new_folder_success, newFolderName[0].getFileName());
                                remoteVFile = (RemoteVFile)moveToDialogFragment.getIndicatorFile();
                                moveToDialogFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD , false);
                            }
                        }
                    } else {
                        fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                        if (fileListFragment != null) {
                            fileListFragment.addComplete();
                            ToastUtility.show(getActivity(), R.string.new_folder_success, newFolderName[0].getFileName());
                            if (fileListFragment.getIndicatorFile()!=null && fileListFragment.getIndicatorFile().getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
                                remoteVFile = (RemoteVFile)fileListFragment.getIndicatorFile();
                                fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD , false);
                            }
                        }
                    }
                } else if (msgObj.getResultCode() == ResultCode.ERROR) {
                    fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                    if (fileListFragment != null) {
                        fileListFragment.addComplete();
                    }
                    ToastUtility.show(getActivity(), R.string.new_folder_fail, Toast.LENGTH_LONG);
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_DELETE_FILES:
                Log.d(TAG, "GET MSG_RESPONE_DELETE_FILES");
                MsgObj srcMsgObj = FileManagerApplication.msgMap.get(msgObj.getMsgId());

                if (srcMsgObj==null||remoteFileUtility.currentOperateMsgObj==null||srcMsgObj.getMsgId()!= remoteFileUtility.currentOperateMsgObj.getMsgId()) {
                    Log.d(TAG,"==========DELETE FAIL RETURN======= " + " srcMsgObj === " + srcMsgObj + " currentOperateMsgObj ==" + remoteFileUtility.currentOperateMsgObj);
                    return ;
                }else {
                    fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                    if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                        if (srcMsgObj.getCopyType()==CopyTypeArgument.CLOUD_OTHER_CLOUD || srcMsgObj.getCopyType() == CopyTypeArgument.DEVICE_TO_OTHER_DEVICE ||
                                srcMsgObj.getCopyType() == CopyTypeArgument.DEVICE_TO_DEVICE || srcMsgObj.getCopyType() == CopyTypeArgument.DEVICE_TO_CLOUD || srcMsgObj.getCopyType() == CopyTypeArgument.CLOUD_TO_DEVICE ) {
                            if (fileListFragment != null){
                                remoteFileUtility.dissMissRemoteCopyProgressDialog();
                                ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);

                                remoteVFile = (RemoteVFile)FileManagerApplication.msgDstPathMap.get(msgObj.getMsgId());
                                remoteFileUtility.removeDstPathFromMap(msgObj.getMsgId());

                                if (remoteVFile != null) {
//								String currentRealFilePath = PathIndicator.getRealIndiatorPath();
//								currentRealFilePath = PathIndicator.getIndicatorVfileStorageType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
                                Log.d(TAG,"dst:"+(remoteVFile.getStorageType()+"_"+remoteVFile.getAbsolutePath()));

//								if(currentRealFilePath.equalsIgnoreCase(remoteVFile.getStorageType()+"_"+remoteVFile.getAbsolutePath())){
                                fileListFragment.startScanFile(fileListFragment.getIndicatorFile(), ScanType.SCAN_CHILD , false);
//								}
                                }

                                /*if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE){
                                remoteVFile = (RemoteVFile)fileListFragment.getIndicatorFile();
                                fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD , false);
                                }*/
                            }
                        }else if (fileListFragment != null && srcMsgObj.getCopyType()==CopyTypeArgument.CLOUD_TO_LOCAL||srcMsgObj.getCopyType() == CopyTypeArgument.DEVICE_TO_LOCAL) {
                            //remoteFileUtility.dissMissRemoteCopyProgressDialog();
                            remoteFileUtility.dissMissCloudCopyProgressDialog();
                            ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
                             LocalVFile localVFile = new LocalVFile(srcMsgObj.getFileObjPath().getFullPath());
                             if (localVFile != null&&localVFile.exists()) {
                                localVFile.setVFileType(VFileType.TYPE_LOCAL_STORAGE);
                                //MediaProviderAsyncHelper.addFolder(localVFile, true);
                                FileUtility.saveMediaFilesToProvider(srcMsgObj,msgObj);

//                             	String currentRealFilePath = PathIndicator.getRealIndiatorPath();
//                             	currentRealFilePath = PathIndicator.getIndicatorVFileType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
//                             	Log.d(TAG,"dst:"+(localVFile.getVFieType()+"_"+localVFile.getAbsolutePath()));
//hide for MoveToDialog
//                             	if(currentRealFilePath.equalsIgnoreCase(localVFile.getVFieType()+"_"+localVFile.getAbsolutePath())){
                                fileListFragment.startScanFile(fileListFragment.getIndicatorFile(), ScanType.SCAN_CHILD , false);
//                             	}
                             }

                            /* ShortCutFragment shortCutFragment = (ShortCutFragment) getActivity().getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
                            /** ShortCutFragment shortCutFragment = (ShortCutFragment) getActivity().getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
                            if (shortCutFragment!=null) {
                                 shortCutFragment.updateTreeFromPaste(localVFile.getAbsolutePath());
                            }*/
                        }else if (srcMsgObj.getCopyType()==CopyTypeArgument.CLOUD_TO_SAMB) {
                                                    fileListFragment.pasteComplete(true);
                                                    fileListFragment.startScanFile(fileListFragment.getIndicatorFile(), ScanType.SCAN_CHILD , false);
                        }else if (srcMsgObj.getCopyType()==CopyTypeArgument.INIT_VALUE ) {
                            if (((FileManagerActivity)getActivity()).getIsShowSearchFragment()) {
                                SearchResultFragment mSFragment = (SearchResultFragment) getActivity().getFragmentManager().findFragmentById(R.id.searchlist);
                                if (mSFragment != null) {
                                    mSFragment.deleteComplete(false);
                                }
                                ToastUtility.show(getActivity(), R.string.delete_success_more, Toast.LENGTH_LONG);
//								((FileManagerActivity)getActivity()).reSearch(((FileManagerActivity)getActivity()).getSearchQueryKey());
                            }else {
                                if (fileListFragment != null) {
                                fileListFragment.deleteComplete();
                                                                fileListFragment.startScanFile(fileListFragment.getIndicatorFile(), ScanType.SCAN_CHILD , false);
//hide for MoveToDialog
//								if (fileListFragment.getIndicatorFile()!=null && fileListFragment.getIndicatorFile().getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
//								remoteVFile = (RemoteVFile)fileListFragment.getIndicatorFile();
//								fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD , false);
//								}
                                }
                                ToastUtility.show(getActivity(), R.string.delete_success_more, Toast.LENGTH_LONG);
                            }
                        }

                    } else if (msgObj.getResultCode() == ResultCode.ERROR) {
                        if (srcMsgObj.getCopyType()==CopyTypeArgument.CLOUD_OTHER_CLOUD||(srcMsgObj.getCopyType()==CopyTypeArgument.CLOUD_TO_LOCAL)|| srcMsgObj.getCopyType() == CopyTypeArgument.DEVICE_TO_OTHER_DEVICE ||
                                srcMsgObj.getCopyType() == CopyTypeArgument.DEVICE_TO_DEVICE || srcMsgObj.getCopyType() == CopyTypeArgument.DEVICE_TO_CLOUD || srcMsgObj.getCopyType() == CopyTypeArgument.CLOUD_TO_DEVICE) {

                            if (fileListFragment != null && srcMsgObj.getCopyType()==CopyTypeArgument.CLOUD_OTHER_CLOUD) {
                                remoteFileUtility.dissMissRemoteCopyProgressDialog();
                                LocalVFile cfile = new LocalVFile(getActivity().getExternalCacheDir(), ".cfile/");
                                LocalVFile deleteFile = new LocalVFile(new VFile(cfile, msgObj.getMsgId()+File.separator));
                                if (deleteFile!=null&&deleteFile.exists()) {
                                    remoteFileUtility.deleteFileAndPath(deleteFile);
                                }

                                remoteVFile = (RemoteVFile)FileManagerApplication.msgDstPathMap.get(msgObj.getMsgId());
                                remoteFileUtility.removeDstPathFromMap(msgObj.getMsgId());

//hide for MoveToDialog
//								if (remoteVFile != null) {
//								String currentRealFilePath = PathIndicator.getRealIndiatorPath();
//								currentRealFilePath = PathIndicator.getIndicatorVfileStorageType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
//								Log.d(TAG,"dst:"+(remoteVFile.getStorageType()+"_"+remoteVFile.getAbsolutePath()));
//
//								if(currentRealFilePath.equalsIgnoreCase(remoteVFile.getStorageType()+"_"+remoteVFile.getAbsolutePath())){
//								fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD , false);
//								}
//								}

                                /*if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE){
                                remoteVFile = (RemoteVFile)fileListFragment.getIndicatorFile();
                                fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD , false);
                                }*/
                            }else if (fileListFragment != null && srcMsgObj.getCopyType()==CopyTypeArgument.CLOUD_TO_LOCAL || srcMsgObj.getCopyType()==CopyTypeArgument.DEVICE_TO_LOCAL) {
                                remoteFileUtility.dissMissCloudCopyProgressDialog();
                                //delete file when download success but delete source file error
                                /*File file = new File(msgObj.getFileObjPath().getFullPath());
                                if (file!=null&&file.exists()) {
                                    remoteFileUtility.deleteFileAndPath(file);
                                }*/
                                /*if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_LOCAL_STORAGE){
                                LocalVFile localVFile = (LocalVFile)fileListFragment.getIndicatorFile();
                                fileListFragment.startScanFile(localVFile, ScanType.SCAN_CHILD , false);
                                }*/

                                LocalVFile localVFile = new LocalVFile(srcMsgObj.getFileObjPath().getFullPath());
                                 if (localVFile != null&&localVFile.exists()) {
                                    localVFile.setVFileType(VFileType.TYPE_LOCAL_STORAGE);
                                    //MediaProviderAsyncHelper.addFolder(localVFile, true);
                                    FileUtility.saveMediaFilesToProvider(srcMsgObj,msgObj);
//hide for MoveToDialog
//	                             	String currentRealFilePath = PathIndicator.getRealIndiatorPath();
//	                             	currentRealFilePath = PathIndicator.getIndicatorVFileType()+"_"+( (currentRealFilePath!=null&&currentRealFilePath.length()>0)?currentRealFilePath.substring(0, currentRealFilePath.length()-1):currentRealFilePath);
//	                             	Log.d(TAG,"dst:"+(localVFile.getVFieType()+"_"+localVFile.getAbsolutePath()));
//
//	                             	if(currentRealFilePath.equalsIgnoreCase(localVFile.getVFieType()+"_"+localVFile.getAbsolutePath())){
//	                             	fileListFragment.startScanFile(localVFile, ScanType.SCAN_CHILD , false);
//	                             	}
                                 }
                            }else {
                                remoteFileUtility.dissMissRemoteCopyProgressDialog();
                            }
                            ToastUtility.show(getActivity(), R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);

                        }else if (srcMsgObj.getCopyType()==CopyTypeArgument.CLOUD_TO_SAMB) {
                                                    ToastUtility.show(getActivity(), R.string.paste_fail, Toast.LENGTH_SHORT);
                                                    fileListFragment.pasteComplete(true);
                                                    fileListFragment.startScanFile(fileListFragment.getIndicatorFile(), ScanType.SCAN_CHILD , false);
                        }else {
                            if (fileListFragment != null) {
                                fileListFragment.deleteComplete();
                            }
                            ToastUtility.show(getActivity(), R.string.delete_fail, Toast.LENGTH_LONG);
                        }

                    }
                    remoteFileUtility.removeMsgOjbFromMap(msgObj);
                }

                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_RENAME_FILE:
                Log.d(TAG, "GET MSG_RESPONE_RENAME_FILE");
                fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                    if (((FileManagerActivity)getActivity()).getIsShowSearchFragment()) {
                        if (fileListFragment != null) {
                        fileListFragment.RenamingComplete();
                        }
                        //ToastUtility.show(getActivity(), R.string.rename_success, Toast.LENGTH_LONG);
                        ((FileManagerActivity)getActivity()).reSearch(((FileManagerActivity)getActivity()).getSearchQueryKey());
                    }else{
                        if (fileListFragment != null) {
                            fileListFragment.RenamingComplete();
                            if (fileListFragment.getIndicatorFile()!=null && fileListFragment.getIndicatorFile().getVFieType() == VFileType.TYPE_CLOUD_STORAGE){
                            remoteVFile = (RemoteVFile)fileListFragment.getIndicatorFile();
                            fileListFragment.startScanFile(remoteVFile, ScanType.SCAN_CHILD , false);
                            }
                        }
                    }
                    ToastUtility.show(getActivity(), R.string.rename_success, Toast.LENGTH_LONG);
                } else if (msgObj.getResultCode() == ResultCode.ERROR) {
                    if (fileListFragment != null) {
                        fileListFragment.RenamingComplete();
                    }
                    ToastUtility.show(getActivity(), R.string.rename_fail, Toast.LENGTH_LONG);
                }
                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_STORAGE_USAGE:
                Log.d(TAG, "GET MSG_RESPONE_STORAGE_USAGE");
                Log.v("Johnson", "get MSG_RESPONE_STORAGE_USAGE");

                if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                    long totalQuota = storageObj.getStorageQuota();
                    long usedQuota = storageObj.getStorageQuotaUsed();
                    fileListFragment = (FileListFragment) getActivity().getFragmentManager().findFragmentById(R.id.filelist);
                    if (fileListFragment != null) {
                        if (PathIndicator.getRealIndiatorPath().contains(storageObj.getStorageName())) {
                            fileListFragment.updateCloudStorageUsage(false, usedQuota, totalQuota);
                        }
                    }
                    SharedPreferences settings = getActivity().getSharedPreferences("settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putLong("totalQuota", totalQuota);
                    editor.putLong("usedQuota", usedQuota);
                    editor.commit();

                    if (getActivity() instanceof  FileManagerActivity){
                        ((FileManagerActivity)getActivity()).updateGDriveStorageUsage(storageObj.getStorageName(),totalQuota, usedQuota);
                    }
                } else if (msgObj.getResultCode() == ResultCode.ERROR) {
                    Toast.makeText(getActivity(), "storage usage update fail", Toast.LENGTH_SHORT).show();
                }

                break;
            case CloudStorageServiceHandlerMsg.MSG_CLOUD_RESPONE_SEARCH_FILES:
                Log.d(TAG, "GET MSG_RESPONE_SEARCH_FILES");

               // ArrayList<RemoteVFile> searchVFiles = new ArrayList<RemoteVFile>();
                if (msgObj.getMsgId()==null) {
                    Log.d(TAG, "search file msgid is null");
                    break;
                }

                String msgId = msgObj.getMsgId();
                  if (msgObj.getResultCode() == ResultCode.SUCCESS) {
                       Log.i(TAG,"search files :pageNum:"+msgObj.getPageNum()+"endpage:"+msgObj.getEndPage());
                            files = msgObj.getFileObjFiles();

                            if (currentVFilesMap.get(msgId)==null) {
                                currentVFilesMap.put(msgId, new ArrayList<RemoteVFile>());
                            }
                            String newParentPath = null;
                            if(files!=null&&files.length>0&&msgObj.getStorageObj().getStorageType()==MsgObj.TYPE_HOMECLOUD_STORAGE){
                                newParentPath = remoteFileUtility.addDeviceNameToParentPath(files[0].getFileParentPath(),msgObj.getStorageObj().getDeviceId());
                            }
                            if (files!=null) {
                                 for (int i=0 ; i<files.length ; i++) {
                                        //searchVFiles.add(new RemoteVFile(files[i], storageObj));
                                     if (msgObj.getStorageObj().getStorageType()==MsgObj.TYPE_HOMECLOUD_STORAGE) {
                                         files[i].setFileParentPath(newParentPath);
                                     }
                                     if (isSearchRemoteFileIgnoreFolder) {
                                        if (!files[i].getIsDirectory()) {
                                            currentVFilesMap.get(msgId).add(new RemoteVFile(files[i], storageObj));
                                        }
                                     }
                                }
                            }
                            if (msgObj.getEndPage()) {
                                Log.d(TAG,"search files success");
                                SearchDialogFragment searchDialogFragment = (SearchDialogFragment) getActivity().getFragmentManager().findFragmentByTag("SearchDialogFragment");
                                ArrayList<RemoteVFile> searchVFiles = new ArrayList<RemoteVFile>();
                                if(currentVFilesMap.get(msgId)!=null){
                                    searchVFiles.addAll(currentVFilesMap.get(msgId));
                                    currentVFilesMap.remove(msgId);
                                }
                                if (searchDialogFragment != null) {
                                    Log.d(TAG,"search files success searchVFiles size:"+searchVFiles.size());
                                    searchDialogFragment.sendSearchResult(searchVFiles);
                                }
                            }

                        } else if (msgObj.getResultCode() == ResultCode.ERROR) {
                            Log.d(TAG,"search files ResultCode.ERROR");
                            SearchDialogFragment searchDialogFragment = (SearchDialogFragment) getActivity().getFragmentManager().findFragmentByTag("SearchDialogFragment");
                            ArrayList<RemoteVFile> searchVFiles = new ArrayList<RemoteVFile>();
                              if (searchDialogFragment != null) {
                                    searchDialogFragment.sendSearchResult(searchVFiles);
                              }
                            currentVFilesMap.remove(msgId);
                        }
                break;
                case CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL:
                    Log.d(TAG, "CloudStorageServiceHandlerMsg.MSG_APP_COPY_FILE_CANCEL");
                    remoteFileUtility.dissMissRemoteCopyProgressDialog();
                    remoteFileUtility.currentOperateMsgObj=null;
                    remoteFileUtility.removeMsgOjbFromMap(msgObj);
                    Log.i("felix_zhang:","cannel msg msgid is:"+remoteFileUtility.currentOperateMsgObj.getMsgId());
            default:
                super.handleMessage(msg);
            }
        super.handleMessage(msg);

    }

    public void showErrorMsg(Context context,int errorCode){
        RemoteFileUtility.mRemoteAccessPermisionDeny = false;
        switch (errorCode) {
        case ErrMsg.INSUFFICIENT_STORAGE:
            ToastUtility.show(context, R.string.no_space_fail);
            break;
        case ErrMsg.BEYOND_SINGLE_FILESIZE_LIMITED:
            ToastUtility.show(context, R.string.cloud_upload_flie_limit_space);
            break;
        case ErrMsg.ACCESS_DENY:
            ToastUtility.show(context, R.string.permission_deny);
            RemoteFileUtility.mRemoteAccessPermisionDeny = true;
            break;
        case ErrMsg.FILE_NAME_ERROR:
            ToastUtility.show(context, R.string.cloud_flie_name_not_supported);
            break;
        default:
            break;
        }

    }

}
