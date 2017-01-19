
package com.asus.filemanager.utility;

import android.os.Parcel;
import android.os.Parcelable;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteVFile;

public class FolderElement implements Parcelable {

    public static class StorageType {
        // you should also add resource to array.xml
        // number: 0 ~ 5
        public static int TYPE_INTERNAL_STORAGE;
        public static int TYPE_MICROSD_STORAGE;
        public static int TYPE_USBDISK1_STORAGE;
        public static int TYPE_USBDISK2_STORAGE;
        public static int TYPE_SDREADER_STORAGE;
        public static int TYPE_UNKNOWN_STORAGE;

        // remote storage type
        public static final int TYPE_REMOTE_STORAGE_TITLE = 6; // remote storage title
        public static final int TYPE_WIFI_DIRECT_STORAGE_TITLE = 7; // WiFi-Direct storage title
        public static final int TYPE_CLOUD_STORAGE_TITLE = 8; // cloud storage title
        public static final int TYPE_STARRED_TITLE = 9; // starred
        public static final int TYPE_RECENTLY_USED_TITLE = 10; // recently used
        public static final int TYPE_TRASH_CAN_TITLE = 11; // trash can
        public static final int TYPE_SAMBA_TITLE = 12; //samba title
        public static final int TYPE_SAMBA_ADD_TITLE = 13; //samba add title
        public static final int TYPE_SAMBA_ACCOUNT_TITLE = 14;// samba account title

        // remote storage name based on user named
        public static final int TYPE_WIFIDIRECT_STORAGE = 99;
        public static final int TYPE_ASUSWEBSTORAGE = 100;
        public static final int TYPE_SKYDRIVE = 101;
        public static final int TYPE_DROPBOX = 102;
        public static final int TYPE_BAIDUPCS = 103;
        public static final int TYPE_GOOGLE_DRIVE = 104;
        public static final int TYPE_NETWORK_PLACE = 105;
        public static final int TYPE_HOME_CLOUD = 106;

        public static final int TYPE_YANDEX = 107;

        public static final int TYPE_RECENT_FILES = 111;

        //samba storage type
        public static final int TYPE_SAMBA = 124;
    }

    private VFile mFile;
    private boolean mHasParent;
    private boolean mHasChild;
    private VFile mParent;
    private int mLevel;
    private boolean mExpanded;
    private int mStorageType;

    // +++ remote storage
    private String mStorageName = "";
    private String mStorageAddress = "";
    private int mStorageState;
    // ---

    public FolderElement(VFile file, int level, boolean expanded, int storageType) {

        mFile = file;
        // local storage case
        if (storageType <= StorageType.TYPE_SDREADER_STORAGE) {
            if (file.getParentFile() != null) {
                mParent = new VFile(file.getParentFile());
            }
            mHasParent = (mParent != null);
        } else {
            mParent = null;
            mHasParent = false;
        }

        mHasChild = hasChild(file);
        mLevel = level;
        mExpanded = expanded;
        mStorageType = storageType;
    }

    public void setStorageType(int storageType) {
        mStorageType = storageType;
    }

    public int getStorageType() {
        return mStorageType;
    }

    public VFile getFile() {
        return mFile;
    }

    public void setFile(VFile file) {
        mFile = file;
    }

    public boolean ishasParent() {
        return mHasParent;
    }

    public void sethasParent(boolean hasParent) {
        mHasParent = hasParent;
    }

    public boolean ishasChild() {
        return mHasChild;
    }

    public void sethasChild(boolean hasChild) {
        mHasChild = hasChild;
    }

    public VFile getParent() {
        return mParent;
    }

    public void setParent(VFile parent) {
        mParent = parent;
    }

    public int getLevel() {
        return mLevel;
    }

    public void setLevel(int level) {
        mLevel = level;
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
    }

    public void resetHasChild() {
        mHasChild = hasChild(mFile);
        if (mHasChild == false) {
            mExpanded = false;
        }
    }

    private boolean hasChild(VFile file) {
        boolean hasChild = false;
        VFile[] files = file.listVFiles();
        if (files != null) {
            for (VFile f : files) {
                if (f.isDirectory() && (FileListFragment.sShowHidden || !f.isHidden())) {
                    hasChild = true;
                    break;
                }
            }
        }
        return hasChild;
    }

    @Override
    public boolean equals(Object obj) {
        final FolderElement element = (FolderElement) obj;
        return element.getFile().getAbsolutePath().equals(this.getFile().getAbsolutePath());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeSerializable(mFile);
        out.writeInt(mHasParent ? 1 : 0);
        out.writeInt(mHasChild ? 1 : 0);
        out.writeSerializable(mParent);
        out.writeInt(mLevel);
        out.writeInt(mExpanded ? 1 : 0);
        out.writeInt(mStorageType);
    }

    public static final Parcelable.Creator<FolderElement> CREATOR = new Parcelable.Creator<FolderElement>() {
        public FolderElement createFromParcel(Parcel in) {
            return new FolderElement(in);
        }

        public FolderElement[] newArray(int size) {
            return new FolderElement[size];
        }
    };

    public FolderElement(Parcel in) {
        mFile = (VFile) in.readSerializable();
        mHasParent = (in.readInt() == 1);
        mHasChild = (in.readInt() == 1);
        mParent = (VFile) in.readSerializable();
        mLevel = in.readInt();
        mExpanded = (in.readInt() == 1);
        mStorageType = in.readInt();
    }

    public int compareTo(FolderElement element) {
        SortUtility.ComparaByFolderName comparator = new SortUtility.ComparaByFolderName(true);
        return comparator.compare(this, element);
    }

    // +++ remote storage function
    public void setStorageName(String storageName) {
        mStorageName = storageName;
    }

    public String getStorageName() {
        return mStorageName;
    }

    public void setStorageAddress(String storageAddress) {
        if (mFile.getVFieType() != VFileType.TYPE_LOCAL_STORAGE) {
            ((RemoteVFile)mFile).setStorageAddress(storageAddress);
        }
        mStorageAddress = storageAddress;
    }

    public String getStorageAddress() {
        return mStorageAddress;
    }

    public void setStorageState(int storageState) {
        if (mFile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
            ((RemoteVFile)mFile).setStorageState(storageState);
        }
        mStorageState = storageState;
    }

    public int getStorageState() {
        return mStorageState;
    }
    // ---
}
