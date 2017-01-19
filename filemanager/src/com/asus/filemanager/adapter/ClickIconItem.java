package com.asus.filemanager.adapter;

import com.asus.filemanager.utility.VFile;
import com.asus.remote.utility.RemoteAccountUtility.AccountInfo;

public class ClickIconItem {
    public static final int LOCAL_STORAGE = 0;
    public static final int CLOUD_STORAGE = 1;
    public static final int ADD_GOOGLE_ACCOUNT = 2;
    public static final int HOME = 3;
    public static final int NETWORK_STORAGE = 4;
    public static final int ADD_CLOUD_ACCOUNT = 5;
    public static final int HTTP_SERVER = 6;

    int itemtype;
    int index;
    int cloudStorageIndex;
    public VFile file;
    public String storageName;
    boolean mounted;
    AccountInfo acountInfo;

    public int getItemType() {
        return itemtype;
    }

    private int getIndex() {
        return index;
    }

    private int getCloudStorageIndex() {
        return cloudStorageIndex;
    }

    private boolean getMounted() {
        return mounted;
    }

    private String getStorageName() {
        return storageName;
    }

    private AccountInfo getAccountInfo() {
        return acountInfo;
    }

    private VFile getFile() {
        return file;
    }
}
