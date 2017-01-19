package com.asus.remote.utility;

import com.asus.filemanager.utility.FolderElement;

public class RemoteDataEntry {
    private FolderElement mFolderElement;
    private int mUI_number;
    private boolean mIsExpand;
    private RemoteVFile mRemoteVFile;

    public RemoteDataEntry(FolderElement folderElement, RemoteVFile vfile, int value, boolean isExpand) {
        mRemoteVFile = vfile;
        mFolderElement = folderElement;
        mUI_number = value;
        mIsExpand = isExpand;
    }

    public RemoteDataEntry(FolderElement folderElement, int value, boolean isExpand) {
        mFolderElement = folderElement;
        mUI_number = value;
        mIsExpand = isExpand;
    }

    public RemoteDataEntry(FolderElement folderElement, int value) {
        mFolderElement = folderElement;
        mUI_number = value;
    }

    public RemoteDataEntry(RemoteVFile vfile, int value) {
        mRemoteVFile = vfile;
        mUI_number = value;
    }

    public FolderElement getFolderElement() {
        return mFolderElement;
    }

    public int getUInumber() {
        return mUI_number;
    }

    public boolean getIsExpand() {
        return mIsExpand;
    }

    public RemoteVFile getRemoteVFile() {
        return mRemoteVFile;
    }
}
