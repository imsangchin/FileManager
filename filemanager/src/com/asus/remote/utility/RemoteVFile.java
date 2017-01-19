package com.asus.remote.utility;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import android.R.integer;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.VFile;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.common.MsgObj.FileObj;
import com.asus.service.cloudstorage.common.MsgObj.StorageObj;


public class RemoteVFile extends VFile implements Parcelable {

    private static final String TAG = "RemoteVFile";

    public static HashMap<String, HashMap<String, String>> AccountIdpathMap = new HashMap<String, HashMap<String, String>>();
    public static HashMap<String, String> IdPathMap =  new HashMap<String, String>();

    public static HashMap<String, HashMap<String, String>> AccountPathIdMap = new HashMap<String, HashMap<String, String>>();
    public static HashMap<String, String> PathIdMap =  new HashMap<String, String>();
    public static HashMap<String, HomeCloudDeviceInfo>homeCloudDeviceInfoMap = new HashMap<String, HomeCloudDeviceInfo>();

    private int mVFileType = VFileType.TYPE_REMOTE_STORAGE;

    public static final String DEFAULT_REMOTE_STORAGE_NAME = "default_name";
    private String mStorageName = DEFAULT_REMOTE_STORAGE_NAME;
    private String mParentPath = null;
    private String mAbsolutePath = null;
    private String mFileName = "";
    private double mFileSize = 0;
    private long mLastModified = 0;
    private boolean mIsDirectory = false;
    private boolean mHasChild = true;
    private boolean mHasThumbnail = false;
    private int mStorageState = -1;
    private String mStorageAddress;
    private String mRootSharedPath = null;
    private String mIndicatorPath;
    private String mFilePermission = "DRW";
    private String mFileID = "";
    private String mParentFileID = "";
    private String mParentName = "";
    private int mStorageType  = -1;
    private int mDeviceStatus =-1;
    private String mDeviceId ="";
    private String mUserId = "";
    //Add this string to solve indicator path issue.
    private boolean mFromFileListItenClick = false;

    public RemoteVFile(FileObj fileObj, StorageObj storageObj) {
        super(fileObj.getFullPath());
        mParentPath = fileObj.getFileParentPath();
        mFileName = fileObj.getFileName();
        mFileSize = fileObj.getFileSize();
        mLastModified = fileObj.getLastModified();
        mIsDirectory = fileObj.getIsDirectory();
        mHasChild = fileObj.getHasChild();
        mStorageName = storageObj.getStorageName();
        mRootSharedPath = fileObj.getRootShareFilePath();
        mFilePermission = fileObj.getFilePermission();
        mFileID = fileObj.getFileId();
        mHasThumbnail = fileObj.getHasThumbnail();
        mUserId = storageObj.getUserId();
        mDeviceId = storageObj.getDeviceId();
        mParentFileID = fileObj.getParentId();

        getIndicatorPath();


        // consider remote path = device name + / real path ....

			//fixed felix_zhang
			if ((mParentPath != null) && (!mParentPath.equals("") && !mParentPath.startsWith(File.separator + mStorageName))&&!mParentPath.equals(mStorageName)) {
		        mParentPath = File.separator + mStorageName + mParentPath;
		    } else if(mParentPath!=null&&mParentPath.startsWith(File.separator + mStorageName)){

		    }else {
		    	 mParentPath = File.separator + mStorageName;
			}



        setStorageType(storageObj.getStorageType());

        if (mStorageType>=StorageType.TYPE_ASUSWEBSTORAGE && mStorageType<=StorageType.TYPE_HOME_CLOUD) {
            mVFileType = VFileType.TYPE_CLOUD_STORAGE;
        }
    }

    public RemoteVFile(VFile file) {
        super(file);
        mParentPath = file.getParent();
        mFileName = file.getName();
        mFileSize = file.length();
        mLastModified = file.lastModified();
        mStorageName = getStorageName(file.getAbsolutePath());
        mIsDirectory = file.isDirectory();
        mVFileType = file.getVFieType();
        if (file instanceof RemoteVFile) {
            mHasThumbnail = ((RemoteVFile)file).getHasThumbnail();
            mStorageType = ((RemoteVFile)file).getStorageType();
            mDeviceId = ((RemoteVFile)file).getmDeviceId();
            mFileID = ((RemoteVFile)file).getFileID();
            mParentFileID = ((RemoteVFile)file).getParentFileID();
        }

    }

    public RemoteVFile(String path) {
        super(path);
        mParentPath = getFolderPath(path);
        mFileName = getFolderName(path);
        mStorageName = getStorageName(path);
    }

    /*
     * This is function only for initializing cloud storage account
     * The file Name can be empty when it is the root folder
     */
    public RemoteVFile(String path, int vfileType, String storageName, int storageType, String fileName) {
        super(path);
        mParentPath = "/" + storageName;
        mFileName = fileName;
        mStorageName = storageName;
        mVFileType = vfileType;
        mStorageType = storageType;
    }


    public RemoteVFile(String path, long lastModified, int fileType) {
        super(path);
        if (fileType == VFileType.TYPE_PICASA_STORAGE) {
            mAbsolutePath = path;
            mLastModified = lastModified;
        }
        mVFileType = fileType;
    }

    public RemoteVFile[] listVFiles() {
        RemoteVFile[] result = null;

        return result;
    }

    public RemoteVFile[] listVFiles(Context context) {
        RemoteVFile[] result = null;
        result = new RemoteVFile[1];
        result[0] = new RemoteVFile("");

        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        Log.e("DUMP_PARCEL_WRITE", "abs: " + getAbsolutePath() + ", path: " + getPath());
        out.writeString(/*getAbsolutePath()*/getPath());
        out.writeInt(mChecked ? 1 : 0);
    }

    public static final Parcelable.Creator<RemoteVFile> CREATOR = new Parcelable.Creator<RemoteVFile>() {
        public RemoteVFile createFromParcel(Parcel in) {
            return new RemoteVFile(in);
        }

        public RemoteVFile[] newArray(int size) {
            return new RemoteVFile[size];
        }
    };

    public RemoteVFile(Parcel in) {
        super(in.readString());
        Log.e("DUMP_PARCEL_READ", "getPath: " + getPath());
        mChecked = (in.readInt() == 1);
    }

    public int getVFieType() {
        return mVFileType;
    }

    public void setFromFileListItenClick(boolean is){
    	mFromFileListItenClick = is;
    }

    public boolean getFromFileListItenClick(){
    	return mFromFileListItenClick;
    }

    public void setAbsolutePath(String path){
    	mAbsolutePath = path;
    }

    public int getmDeviceStatus() {
		return mDeviceStatus;
	}

	public void setmDeviceStatus(int mDeviceStatus) {
		this.mDeviceStatus = mDeviceStatus;
	}

	public String getmAbsolutePath(){
    	return mAbsolutePath;
    }

    public String getStorageName() {
        if (mStorageName.equals(DEFAULT_REMOTE_STORAGE_NAME)) {
            String path = getAbsolutePath();
            for (int i=1 ; i<path.length() ; i++) {
                if (path.charAt(i) == File.separatorChar) {
                    mStorageName = path.substring(1, i);
                    break;
                }
            }
        }
        return mStorageName;
    }

    public String getStorageName(String path) {
        String name = DEFAULT_REMOTE_STORAGE_NAME;
        for (int i=1 ; i<path.length() ; i++) {
            if (path.charAt(i) == File.separatorChar) {
                name = path.substring(1, i);
                break;
            }
        }
        return name;
    }

    public String removeStorageNamePath(String deviceName) {
        String path = this.getAbsolutePath();
        if (path.length() > deviceName.length()) {
            int length = deviceName.length();
            path = path.substring(length + 1);
        }
        if (path.equals("")) {
            path = "/";
        }
        return path;
    }

    public String removeStorageNameParentPath(String deviceName) {
        String path = this.getParent();
        if (path.length() > deviceName.length()) {
            int length = deviceName.length();
            path = path.substring(length + 1);
        }
        if (path.equals("")||path.equals(File.separator)) {
            path = File.separator;
        }
        return path;
    }



    public String getFolderPath(String path) {
        String subPath = "";
        if (!path.equals("/")) {
            int p = 0;
            p = path.lastIndexOf('/');
            if (p == 0) {
                subPath = "/";
            } else if (p > 0){
                int number = 0;
                for (int i=0 ; i<path.length() ; i++) {
                    if (path.charAt(i) == File.separatorChar) {
                        number++;
                    }
                }
                // case: /device_name/folder/
                if (number > 2) {
                    if (p == path.length()-1 && path.length() > 1) {
                        path = path.substring(0, path.length() - 1);
                    }
                    p = path.lastIndexOf('/');
                    subPath = path.substring(0, p+1);
                } else {
                    subPath = path.substring(0, p);
                }
            }
        }

        if(subPath.length() > 1 && subPath.endsWith("/")){
        	subPath =  subPath.substring(0, subPath.length() - 1);
        }

        return subPath;
    }

    public String getFolderName(String path) {
        String name = "";
        int p = -1;
        p = path.lastIndexOf('/');

        // case: /device_name/folder/
        if (p == path.length()-1 && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        // case: /device_name/folder
        p = path.lastIndexOf('/');
        if (p > -1) {
            name = path.substring(p+1, path.length());
        }
        if (path.equals("/" + name)) {
            name = "";
        }
        return name;
    }

    @Override
    public boolean exists() {
        return true; // should be fixed ???
    }

    @Override
    public String getAbsolutePath() {
        // picasa path is URL
        if (mVFileType == VFileType.TYPE_PICASA_STORAGE) {
            return mAbsolutePath;
        }

        // WiFi-Direct path is /device name/path
        String path = "";
        if (mParentPath != null && mFileName != null) {
            if (mParentPath.equals("/") ) {
//                path = mParentPath + mFileName;
            	if(mFileName.equals(""))
            		path = "/" + mStorageName;
            	else
            		path = "/" + mStorageName + "/" + mFileName;
            } else {
                if (mParentPath.length() > 0 && mParentPath.substring(mParentPath.length()-1, mParentPath.length()).equals("/")) {
                	if(mFileName.equals(""))
                		path = mParentPath.substring(0, mParentPath.length()-1);
                	else
                		path = mParentPath + mFileName;
                } else {
                	if(mFileName.equals(""))
                		path = mParentPath ;
                	else
                		path = mParentPath + "/" + mFileName;
                }
            }
        }
        return path;
    }

    @Override
    public String getParent() {
        String path = "";
        if (mParentPath != null) {
            path = mParentPath;
        }
        return path;
    }

    @Override
    public RemoteVFile getParentFile() {
        RemoteVFile remoteVFile = null ;
        String path = "";
        if (mParentPath != null) {
            path = mParentPath;
        }

        if (!TextUtils.isEmpty(path)) {
            remoteVFile = new RemoteVFile(path);
            String folderPath = remoteVFile.getFolderPath(path);
            String folderName = remoteVFile.getFolderName(path);

            remoteVFile.setVFileData(folderPath, folderName);
            remoteVFile.setStorageName(this.getStorageName());
            remoteVFile.setVFileType(this.getVFieType());
            remoteVFile.setStorageType(this.getStorageType());
            remoteVFile.setmUserId(this.getmUserId());
            remoteVFile.setmDeviceId(this.getmDeviceId());
            remoteVFile.setmDeviceStatus(this.getmDeviceStatus());
            remoteVFile.setFileID(this.getParentFileID());
        }

        if (remoteVFile != null && remoteVFile.getStorageType() == StorageType.TYPE_WIFIDIRECT_STORAGE
                && this.getRootSharedPath() != null && this.getStorageName() != null) {
            if (remoteVFile.getAbsolutePath().startsWith("/" + this.getStorageName() + this.getRootSharedPath())) {
                remoteVFile.setRootSharedPath(this.getRootSharedPath());
            }
        }

        return remoteVFile;
    }

    public void setRootSharedPath(String path) {
        mRootSharedPath = path;
    }

    public String getRootSharedPath() {
        return mRootSharedPath;
    }

    public void setVFileData(String path, String name) {
        mParentPath = path;
        mFileName = name;
    }

    @Override
    public boolean isDirectory() {
        return mIsDirectory;
    }

    public void setmIsDirectory(boolean mIsDirectory) {
		this.mIsDirectory = mIsDirectory;
	}

	public String getmDeviceId() {
		return mDeviceId;
	}

	public void setmDeviceId(String mDeviceId) {
		this.mDeviceId = mDeviceId;
	}


	public String getmUserId() {
		return mUserId;
	}

	public void setmUserId(String mUserId) {
		this.mUserId = mUserId;
	}

	@Override
    public boolean isHidden() {
        if (mFileName == null) {
            return false;
        }
        return getName().startsWith(".");
    }

    @Override
    public String getName() {
        return mFileName;
    }

    public String setName(String name) {
        return mFileName = name;
    }

    @Override
    public String getPath() {
    	String path = "";
    	if(mFileName.equals("")){
            if (mParentPath != null) {
                path = mParentPath;
            } else {
                Log.e("DUMP_GETPATH_1", super.getPath());
                return super.getPath();
            }
    	}else{
    		path = mParentPath + (mParentPath.endsWith("/") ? mFileName : "/" + mFileName);
    	}
        Log.e("DUMP_GETPATH_2", path + ", parent: " + mParentPath + ", FileName: " + mFileName);
        return path;
    }

    @Override
    public long lastModified() {
        return mLastModified;
    }

    @Override
    public long length() {
        return (long)mFileSize;
    }

    public boolean isHasChild() {
        return mHasChild;
    }

    @Override
    public boolean canRead() {
        return mFilePermission.contains("R");
    }

    @Override
    public boolean canWrite() {
        return mFilePermission.contains("W");
    }

    @Override
    public File[] listFiles() {
        return null;
    }

    public String getPermission() {
        return mFilePermission;
    }

    public String getNameNoExtension() {
        String name = getName();
        int p = 0;
        if (name != null) {
            p = getName().length();
        }
        if (!isDirectory()) {
            p = getName().lastIndexOf('.');
            if (p <= 0) {
                p = getName().length();
            }
        }
        return getName().substring(0, p);
    }

    public String getExtensiontName() {
        int p = 0;
        if (!isDirectory()) {
            p = getName().lastIndexOf('.');
            if (p < 0) {
                p = 0;
            } else {
                p++;
            }
        }
        return getName().substring(p);
    }

    public String getAttrSimple() {
        String s = "DRW";
        if (!isDirectory())
            s = s.replace('D', '-');
        if (!canRead())
            s = s.replace('R', '-');
        if (!canWrite())
            s = s.replace('W', '-');
        return s;
    }

    public void setStorageAddress(String storageAddress) {
        mStorageAddress = storageAddress;
    }

    public String getStorageAddress() {
        return mStorageAddress;
    }

    public void setStorageState(int storageState) {
        mStorageState = storageState;
    }

    public int getStorageState() {
        return mStorageState;
    }

    public String getIndicatorPath() {
        String rootSharedFolderName = null;

        if (mRootSharedPath != null) {

            int index = -1;
            for (int i=0 ; i<mRootSharedPath.length() ; i++) {
                if (mRootSharedPath.charAt(i) == File.separatorChar) {
                    index = i;
                }
            }
            if (index != -1) {
                rootSharedFolderName = mRootSharedPath.substring(index + 1, mRootSharedPath.length());
                String subPath = null;
                for (int i=0 ; (i + rootSharedFolderName.length()-1) < this.getAbsolutePath().length() ; i++) {
                    String temp = this.getAbsolutePath().substring(i, i+rootSharedFolderName.length());
                    if (temp.equals(rootSharedFolderName)) {
                        subPath = this.getAbsolutePath().substring(i-1);
                        break;
                    }
                }
                if (subPath != null) {
                    mIndicatorPath = "/" + mStorageName + subPath;
                }
            }
        } else if (mIndicatorPath == null){
            mIndicatorPath = "/" + mStorageName;
        }
        return mIndicatorPath;
    }

    public void SetIndicatorPath(String path) {
        mIndicatorPath = path;
    }

    public String getRealParentPath() {
        String path = this.getAbsolutePath();
        String parentPath = "";
        int first_index = -1;
        int last_index = -1;
        for (int i=1 ; i<path.length() ; i++) {
            if (path.charAt(i) == File.separatorChar) {
                first_index = i;
                break;
            }
        }
        for (int i=1 ; i<path.length() ; i++) {
            if (path.charAt(i) == File.separatorChar) {
                last_index = i;
            }
        }
        if (first_index == last_index) {
            parentPath = "" + File.separatorChar;
        } else {
            parentPath = path.substring(first_index, last_index);
        }

        return parentPath;
    }

    public String getFileID() {
        return mFileID;
    }

    public void setFileID(String id) {
        mFileID = id;
    }

    public void setStorageName(String name) {
        mStorageName = name;
    }

    public void setVFileType(int vfileType) {
        mVFileType = vfileType;
    }

    public void setParentFileID(String id) {
        mParentFileID = id;
    }

    public String getParentFileID() {
        return mParentFileID;
    }

    public String getParentName() {
        return mParentName;
    }

    public void setParentName(String name) {
        mParentName = name;
    }

    public void setStorageType(int storageType) {
        switch(storageType) {
        case MsgObj.TYPE_WIFIDIRECT_STORAGE:
            mStorageType = StorageType.TYPE_WIFIDIRECT_STORAGE;
            break;
        case MsgObj.TYPE_GOOGLE_DRIVE_STORAGE:
            mStorageType = StorageType.TYPE_GOOGLE_DRIVE;
            break;
        case MsgObj.TYPE_DROPBOX_STORAGE:
            mStorageType = StorageType.TYPE_DROPBOX;
            break;
        case MsgObj.TYPE_BAIDUPCS_STORAGE:
        	mStorageType = StorageType.TYPE_BAIDUPCS;
        	break;
        case MsgObj.TYPE_SKYDRIVE_STORAGE:
            mStorageType = StorageType.TYPE_SKYDRIVE;
            break;
        case MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE:
            mStorageType = StorageType.TYPE_ASUSWEBSTORAGE;
            break;
         case MsgObj.TYPE_HOMECLOUD_STORAGE:
        	 mStorageType = StorageType.TYPE_HOME_CLOUD;
        	 break;
         case 9:
             mStorageType = StorageType.TYPE_YANDEX;
             break;
        default:
            mStorageType = storageType;
        }
    }

    public int getStorageType() {
        return mStorageType;
    }

    public int getMsgObjType() {
        switch(this.getStorageType()) {
        case StorageType.TYPE_WIFIDIRECT_STORAGE:
            return MsgObj.TYPE_WIFIDIRECT_STORAGE;
        case StorageType.TYPE_GOOGLE_DRIVE:
            return MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
        case StorageType.TYPE_DROPBOX:
            return MsgObj.TYPE_DROPBOX_STORAGE;
        case StorageType.TYPE_BAIDUPCS:
        	return MsgObj.TYPE_BAIDUPCS_STORAGE;
        case StorageType.TYPE_SKYDRIVE:
            return MsgObj.TYPE_SKYDRIVE_STORAGE;
        case StorageType.TYPE_ASUSWEBSTORAGE:
            return MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE;
        case StorageType.TYPE_HOME_CLOUD:
        	return MsgObj.TYPE_HOMECLOUD_STORAGE;
        case StorageType.TYPE_YANDEX:
        	return 9;
        default:
            return MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
        }
    }

    public static int getMsgObjType(int type) {
        switch(type) {
        case StorageType.TYPE_WIFIDIRECT_STORAGE:
            return MsgObj.TYPE_WIFIDIRECT_STORAGE;
        case StorageType.TYPE_GOOGLE_DRIVE:
            return MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
        case StorageType.TYPE_DROPBOX:
            return MsgObj.TYPE_DROPBOX_STORAGE;
        case StorageType.TYPE_BAIDUPCS:
        	return MsgObj.TYPE_BAIDUPCS_STORAGE;
        case StorageType.TYPE_SKYDRIVE:
            return MsgObj.TYPE_SKYDRIVE_STORAGE;
        case StorageType.TYPE_ASUSWEBSTORAGE:
            return MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE;
        case StorageType.TYPE_HOME_CLOUD:
        	return MsgObj.TYPE_HOMECLOUD_STORAGE;
        case StorageType.TYPE_YANDEX:
        	return 9;
        default:
            return MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof  RemoteVFile) {
            return getPath().equals(((RemoteVFile) obj).getPath());
        } else {
            return false;
        }
    }

    public boolean getHasThumbnail() {
        return mHasThumbnail;
    }
    // ---
}
