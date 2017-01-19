package com.asus.filemanager.utility;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.asus.filemanager.samba.SambaVFile;
import com.asus.remote.utility.RemoteVFile;

public class VFile extends File implements Parcelable{

    protected boolean mChecked = false;
    protected boolean mHasDRM = false;
    protected boolean isRecentFile = false;

    private int mVFileType = VFileType.TYPE_LOCAL_STORAGE;
    private int mBucketId = 0;
    private int mChildCount = 0;
    private long mLastModified = 0;

    private String mFavoriteName = null;
    protected boolean mHasRestrictFiles = false;
    protected List<LocalVFile> mRestrictFiles = new ArrayList<LocalVFile>();

    private long length = 0;
    private float inStoragePercentage = 0;
    private String mMD5;
    private int groupId;

    private int mCategoryItem = -1;

    public static class VFileType {
        public static final int TYPE_LOCAL_STORAGE = 0;
        public static final int TYPE_REMOTE_STORAGE = 1; // for WIFI-direct storage case only
        public static final int TYPE_PICASA_STORAGE = 2;
        public static final int TYPE_CLOUD_STORAGE = 3;
        public static final int TYPE_SAMBA_STORAGE = 4;
        public static final int TYPE_CATEGORY_STORAGE = 5;
        public static final int TYPE_RECOMMEND_APP = 6;
        public static final int TYPE_NATIVE_AD = 7;
        public static final int TYPE_GAME_LAUNCH = 8;
    }

    public static class FileListScanAction {
        public static final int SCAN_NORMAL = 0;
        public static final int SCAN_STARRED = 1;
        public static final int SCAN_RECENTLY_USED = 2;
        public static final int SCAN_TRASH_CAN = 3;
    }

    private static final String TAG = "VFile";

    public VFile(String path) {
        super(path);
    }

    public VFile(String path, int type) {
        super(path);
        mVFileType = type;
    }

    public VFile(String path, int type, int categoryItem) {
        super(path);
        mVFileType = type;
        mCategoryItem = categoryItem;
    }

    public VFile(File dir, String name) {
        super(dir, name);
    }

    public VFile(String dirPath, String name) {
        super(dirPath, name);
    }

    public VFile(URI uri) {
        super(uri);
    }

    public VFile(File file) {
        super(file.getAbsolutePath());
    }

    public VFile(File file, int type) {
        super(file.getAbsolutePath());
        mVFileType = type;
    }

    public boolean getChecked() {
        return mChecked;
    }

    public void setChecked(boolean b) {
        mChecked = b;
    }

    public boolean getHasDRM() {
        return mHasDRM;
    }

    public void setHasDRM(boolean b) {
        mHasDRM = b;
    }

    public boolean getHasRetrictFiles() {
        return mHasRestrictFiles;
    }

    @Override
    public boolean setLastModified(long lastModified) {
        // fake date for recently open db
        mLastModified = lastModified;
        return true;
    }

    @Override
    public long lastModified() {
        if (mLastModified != 0) {
            return mLastModified;
        }
        return super.lastModified();
    }

/*
    public String getFolderPath() {
        int p = 0;
        p = getAbsolutePath().lastIndexOf('/');
        if (p < 0) {
            p = 0;
        }
        return getAbsolutePath().substring(0, p);
    }

    public String getAttrFull() {
        String s = "[DFHRWEA]";
        if (!isDirectory())
            s = s.replace('D', '-');
        if (!isFile())
            s = s.replace('F', '-');
        if (!isHidden())
            s = s.replace('H', '-');
        if (!canRead())
            s = s.replace('R', '-');
        if (!canWrite())
            s = s.replace('W', '-');
        if (!exists())
            s = s.replace('E', '-');
        if (!isAbsolute())
            s = s.replace('A', '-');
        return s;
    }
*/

    public boolean isRecentFile() {
        return isRecentFile;
    }

    public void setRecentFile(boolean isRecentFile) {
        this.isRecentFile = isRecentFile;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getAbsolutePath());
        out.writeInt(mChecked ? 1 : 0);
    }

    public static final Parcelable.Creator<VFile> CREATOR = new Parcelable.Creator<VFile>() {
        public VFile createFromParcel(Parcel in) {
            return new VFile(in);
        }

        public VFile[] newArray(int size) {
            return new VFile[size];
        }
    };

    public VFile(Parcel in) {
        super(in.readString());
        mChecked = (in.readInt() == 1);
    }

    public int getVFieType() {
        return mVFileType;
    }

    public String getExtensiontName() {
        if (this.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            return new LocalVFile(this.getPath()).getExtensiontName();
        } else {
            return new RemoteVFile(this).getExtensiontName();
        }
    }

    public String getNameNoExtension() {
        if (this.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            return new LocalVFile(this.getPath()).getNameNoExtension();
        } else {
            return new RemoteVFile(this).getNameNoExtension();
        }
    }

    public VFile[] listVFiles() {
        if (this.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            if (mHasRestrictFiles) {
                if (mRestrictFiles.size() != 0) {
                    VFile[] vFiles = new VFile[mRestrictFiles.size()];
                    mRestrictFiles.toArray(vFiles);
                    return vFiles;
                } else {
                    return null;
                }
            } else {
                return new LocalVFile(this.getPath()).listVFiles();
            }
        } else {
            return null;
        }
    }

    public String getAttrSimple() {
        if (this.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            return new LocalVFile(this.getPath()).getAttrSimple();
        } else {
            return new RemoteVFile(this).getAttrSimple();
        }
    }

    @Override
    public VFile getParentFile() {
        if (this.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
            File file = super.getParentFile();
            if (file != null) {
                return new LocalVFile(file);
            } else {
                return null;
            }
        }else if (this.getVFieType() == VFileType.TYPE_SAMBA_STORAGE){
            return new SambaVFile(this).getParentFile();
        }else if (this.getVFieType() == VFileType.TYPE_CATEGORY_STORAGE){
            int p = getAbsolutePath().lastIndexOf('/');
            if (p > 0) {
                String path = getAbsolutePath().substring(0, p);
                return new LocalVFile(path, getVFieType());
            } else {
                return new LocalVFile("/", getVFieType());
            }
        }
        else {
            return new RemoteVFile(this).getParentFile();
        }
    }

    public void setVFileType(int type) {
        mVFileType = type;
    }

    public int getBucketId() {
        return mBucketId;
    }

    public void setBucketId(int b) {
        mBucketId = b;
    }

    public int getChildCount() {
        return mHasRestrictFiles? mRestrictFiles.size() : mChildCount;
    }

    public void setChildCount(int b) {
        mChildCount = b;
    }

    public String getFavoriteName() {
        return mFavoriteName;
    }

    public void setFavoriteName(String b) {
        mFavoriteName = b;
    }

    public boolean isFavoriteFile() {
        return mFavoriteName != null;
    }

    public List<LocalVFile> getRestrictFiles() {
        return mRestrictFiles;
    }

    public void setRestrictFiles(List<LocalVFile> restrictFiles) {
        mHasRestrictFiles = true;
        mRestrictFiles = restrictFiles;
    }

    public void removeFromRestrictFiles(VFile vFile) {
        if (mRestrictFiles != null) {
            mRestrictFiles.remove(vFile);
        }
    }

    @Override
    public long length() {
        if (length != 0  || isDirectory()) {
            return length;
        } else {
            return super.length();
        }
    }

    public void setLength(long length) {
        this.length = length;
    }

    public float getInStoragePercentage() {
        return inStoragePercentage;
    }

    public void setInStoragePercentage(float inStoragePercentage) {
        this.inStoragePercentage = inStoragePercentage;
    }

    public void setCategoryItem(int categoryItem) {
        mCategoryItem = categoryItem;
    }

    public int getCategoryItem() {
        return mCategoryItem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VFile) {
            if (mVFileType == VFileType.TYPE_CATEGORY_STORAGE && (getCategoryItem() != -1) && (((VFile)obj).getCategoryItem() != -1))  {
                return mCategoryItem == ((VFile) obj).getCategoryItem();
            }
        }
        return super.equals(obj);
    }

    public boolean alwaysPermanentlyDelete() {
        return getVFieType() != VFileType.TYPE_LOCAL_STORAGE;
    }

    /**
     * @return default null
     */
    public String getMD5() {
        return mMD5;
    }

    public void setMD5(String mMD5) {
        this.mMD5 = mMD5;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public boolean isSearchable() {
        return true;
    }

}
