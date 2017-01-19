
package com.asus.filemanager.utility;

import java.util.ArrayList;
import java.util.Arrays;

public class UnZipPreviewData {

    private String mPath;
    private String mName;
    private String mUTF8Path;
    private String mUTF8Name;
    private int mId = -1;
    private UnZipPreviewData mParent;
    private ArrayList<UnZipPreviewData> mChild;
    private boolean mSorted = false;

    private long mCompressedSize;
    private long mSize;
    private long mModifiedTime;
    private long mCrc;

    public UnZipPreviewData(String path, int id) {
        this.mPath = new String(path);
        this.mId = id;
        this.mChild = new ArrayList<UnZipPreviewData>();

        String[] subPath;
        if (isFolder()) {
            subPath = mPath.substring(0, mPath.length() - 1).split("/");
        } else {
            subPath = mPath.split("/");
        }
        setName(subPath[subPath.length-1]);
    }

    public String getPath() {
        return this.mPath;
    }

    public String getName() {
        return this.mName;
    }

    public String getPathNoSuffixSlash() {
        if (isFolder()) {
            return mPath.substring(0, mPath.length() - 1);
        } else {
            return mPath;
        }
    }

    public String getParentPath() {
        if (getPathNoSuffixSlash().lastIndexOf("/") > 0) {
            return getPathNoSuffixSlash().substring(0, getPathNoSuffixSlash().lastIndexOf("/") + 1);
        } else {
            return null;
        }
    }

    public String getUTF8Path() {
        return this.mUTF8Path;
    }

    public String getUTF8Name() {
        return this.mUTF8Name;
    }

    public String getExtensionName() {
        int p = 0;
        if (!isFolder()) {
            p = getName().lastIndexOf('.');
            if (p < 0) {
                p = 0;
            } else {
                p++;
            }
        }
        return getName().substring(p);
    }

    public int getId() {
        return this.mId;
    }

    public UnZipPreviewData getParent() {
        return this.mParent;
    }

    public ArrayList<UnZipPreviewData> getChild() {
        return this.mChild;
    }

    public long getCompressedSize() {
        return this.mCompressedSize;
    }

    public long getSize() {
        return this.mSize;
    }

    public long getTime() {
        return this.mModifiedTime;
    }

    public long getCrc() {
        return this.mCrc;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setUTF8Path(String path) {
        this.mUTF8Path = path;

        String[] subPath;
        if (isFolder()) {
            subPath = mUTF8Path.substring(0, mUTF8Path.length() - 1).split("/");
        } else {
            subPath = mUTF8Path.split("/");
        }
        setUTF8Name(subPath[subPath.length-1]);
    }

    public void setUTF8Name(String name) {
        this.mUTF8Name = name;
    }

    public void setParent(UnZipPreviewData parent) {
        this.mParent = parent;
    }

    public void addChild(UnZipPreviewData child) {
        this.mChild.add(child);
    }

    public void setInfo(long cSize, long size, long time, long crc) {
        this.mCompressedSize = cSize;
        this.mSize = size;
        this.mModifiedTime = time;
        this.mCrc = crc;
    }

    public void setCompressedSize(long cSize) {
        this.mCompressedSize = cSize;
    }

    public void setSize(long size) {
        this.mSize = size;
    }

    public void setSort(boolean sort) {
        this.mSorted = sort;
    }

    public boolean isFolder() {
        if (mPath.endsWith("/")) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isSorted() {
        return this.mSorted;
    }

    public void sortChild() {
        UnZipPreviewData[] temp = mChild.toArray(new UnZipPreviewData[mChild.size()]);
        Arrays.sort(temp, new SortUtility.ComparaByPreviewName(true));
        mChild = new ArrayList<UnZipPreviewData>(Arrays.asList(temp));
    }
}
