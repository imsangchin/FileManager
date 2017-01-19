package com.asus.remote.utility;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteFileUtility.CopyTypeArgument;
import com.asus.service.cloudstorage.common.HandlerCommand.CopyArgument;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.common.MsgObj.FileObj;

import java.io.File;

public class RemoteFileOperateResultHelper {
	public static final String TAG = "RemoteFileOperateResultHelper";
    private static void dissMissRemoteCopyProgressDialog(Activity mActivity) {
    	Log.e("felix_zhang","colse dissMissRemoteCopyProgressDialog");
        FileListFragment fileListFragment = getFileListFragment(mActivity);
        if(fileListFragment != null) {
            fileListFragment.pasteComplete();
        }
    }
    private static void dissMissCloudCopyProgressDialog(Activity mActivity) {
    	Log.e("felix_zhang","colse dissMissRemoteCopyProgressDialog");
    	FileListFragment fileListFragment = getFileListFragment(mActivity);
    	if(fileListFragment != null) {
    		fileListFragment.pasteCloudComplete();
    	}
    }
    public static ShortCutFragment getShortcutFragment(Activity mActivity){
    	return (ShortCutFragment) mActivity.getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
    }
    public static FileListFragment getFileListFragment(Activity mActivity){
    	return (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
    }
    /**
     * get the cloud to other cloud type
     * @param srcFile
     * @param destFile
     * @return
     */
    public static int getCloudToOtherCloudType(RemoteVFile srcFile,RemoteVFile destFile){
    	if (srcFile == null || destFile == null ) {
    		return CopyTypeArgument.INIT_VALUE;
		}
    	int srcType = srcFile.getStorageType();
    	int destType = destFile.getStorageType();
    	if (srcType == StorageType.TYPE_HOME_CLOUD && destType != StorageType.TYPE_HOME_CLOUD) {
			return CopyTypeArgument.DEVICE_TO_CLOUD;
		}else if (destType == StorageType.TYPE_HOME_CLOUD && srcType != StorageType.TYPE_HOME_CLOUD ) {
			return CopyTypeArgument.CLOUD_TO_DEVICE;
		}else if (destType != StorageType.TYPE_HOME_CLOUD && srcType != StorageType.TYPE_HOME_CLOUD ) {
			return CopyTypeArgument.CLOUD_OTHER_CLOUD;
		}else if(destType == StorageType.TYPE_HOME_CLOUD && srcType == StorageType.TYPE_HOME_CLOUD){
			if (srcFile.getmDeviceId().equals(destFile.getmDeviceId())) {
				return CopyTypeArgument.DEVICE_TO_DEVICE;
			}else {
				return CopyTypeArgument.DEVICE_TO_OTHER_DEVICE;
			}
		}
    	return CopyTypeArgument.INIT_VALUE;
    }
    /**
     * operate the copy  result of  DeviceToLocal
     * @param mActivity
     * @param msgObj
     * @param srcMsgObjCloudToLocal
     * @param actionArgument
     */
    public static void OperateCopyDeviceToLocal(Activity mActivity,MsgObj msgObj,MsgObj srcMsgObjCloudToLocal,String actionArgument){
    	FileObj fileObj;
    	FileListFragment fileListFragment;
        RemoteFileUtility remoteFileUtility = RemoteFileUtility.getInstance(null);
    	if (actionArgument.equals(CopyArgument.Copy)) {
           //dissMissRemoteCopyProgressDialog(mActivity);
    		dissMissCloudCopyProgressDialog(mActivity);
           fileObj = msgObj.getFileObjPath();
            if (fileObj==null) {
				fileObj = srcMsgObjCloudToLocal.getFileObjPath();
			}
           String  deviceName = msgObj.getStorageObj().getStorageName();
            if (fileObj != null) {
            	ToastUtility.show(mActivity, R.string.toast_drag_copied_items, Toast.LENGTH_SHORT);
//            	ShortCutFragment shortcutFragment = getShortcutFragment(mActivity);
//                   if (shortcutFragment != null) {
//                       shortcutFragment.updateTreeFromPaste(fileObj.getFullPath());
//                   }
                //++felix_zhang
                LocalVFile localVFile = new LocalVFile(fileObj.getFullPath());

                if (localVFile != null&&localVFile.exists()) {
                	localVFile.setVFileType(VFileType.TYPE_LOCAL_STORAGE);
                	FileUtility.saveMediaFilesToProvider(srcMsgObjCloudToLocal,msgObj);

                    fileListFragment = getFileListFragment(mActivity);
                    if (fileListFragment != null) {
                        fileListFragment.startScanFile(localVFile, ScanType.SCAN_CHILD);
                    }
                }
            } else {
                Log.w(TAG, "get MSG_RESPONE_COPY_FILE_TO_REMOTE but fileObj is null");
            }
        } else if (actionArgument.equals(RemoteFileUtility.REMOTE_PREVIEW_ACTION)) {
            fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
//            fileListFragment.CloudStorageLoadingComplete();
            remoteFileUtility.dissMissPreViewProgressDialog();
            FileObj[] fileObjs = msgObj.getFileObjFiles();
            fileObj = msgObj.getFileObjPath();
            String fullpath = fileObj.getFullPath() + File.separator +fileObjs[0].getFileName();
            VFile previewVFile = new VFile(fullpath);
            Log.d("------fullpath--------",fullpath);
            Uri uri = ProviderUtility.MediaFiles.insertFile(mActivity.getContentResolver(), previewVFile);
            remoteFileUtility.removeMsgOjbFromMapHelper(msgObj);
            fileListFragment.openCloudStorageFile(previewVFile);
        } else if (actionArgument.equals(RemoteFileUtility.REMOTE_SHARE_ACTION)) {
            fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
            fileListFragment.CloudStorageLoadingComplete();
            FileObj[] fileObjs = msgObj.getFileObjFiles();
            VFile[] sharedVFiles = new VFile[fileObjs.length];
            VFile dstVFile = new LocalVFile(mActivity.getExternalCacheDir(), ".cfile/");

            for (int i=0 ; i<fileObjs.length ; i++) {
                sharedVFiles[i] = new VFile(dstVFile.getPath()+File.separator+fileObjs[i].getFileName());
                Uri uri = ProviderUtility.MediaFiles.insertFile(mActivity.getContentResolver(), sharedVFiles[i]);
            }
            remoteFileUtility.removeMsgOjbFromMapHelper(msgObj);
            fileListFragment.shareCloudStorageFiles(sharedVFiles);
        }else if (actionArgument.equals(RemoteFileUtility.REMOTE_COPY_TO_REMOTE_ACTION)) {//copy files from remote to remote
        	  fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);
              fileListFragment.CloudStorageLoadingComplete();
              FileObj[] fileObjs = msgObj.getFileObjFiles();
              fileObj = msgObj.getFileObjPath();
              String fullpath = fileObj.getFullPath() + File.separator +fileObjs[0].getFileName();
              VFile previewVFile = new VFile(fullpath);
              Log.d("------fullpath--------",fullpath);
              Uri uri = ProviderUtility.MediaFiles.insertFile(mActivity.getContentResolver(), previewVFile);
              //upload the copy file from remote to remote
              fileListFragment.openCloudStorageFile(previewVFile);
              remoteFileUtility.removeMsgOjbFromMapHelper(msgObj);
        }
    }
}
