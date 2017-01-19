package com.asus.filemanager.activity;

import android.util.Log;

import com.asus.filemanager.adapter.BasicFileListAdapter;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wesley_Lee on 2016/12/8.
 */

public class ShoppingCart {
    private static final String TAG = "ShoppingCart";
    ArrayList<VFile> mData = new ArrayList<VFile>();

    public void addVFile(VFile vFile) {
        Log.e("TEST", "type = " + (vFile instanceof LocalVFile ? "true" : "false"));
        mData.add(vFile);
        Log.e(TAG, "add: " + mData.size());
    }

    public void addVFiles(VFile[] vFiles) {
        for (VFile vFile : vFiles) {
            mData.add(vFile);
        }
    }

    public void addVFiles(List<VFile> vFiles) {
        mData.addAll(vFiles);
    }

    public void removeVFile(VFile vFile) {
        mData.remove(vFile);
        Log.e(TAG, "remove: " + mData.size());
    }

    public void removeVFiles(VFile[] vFiles) {
        for (VFile vFile : vFiles) {
            mData.remove(vFile);
        }
    }

    public void removeVFiles(List<VFile> vFiles) {
        mData.removeAll(vFiles);
    }

    public void setFiles(ArrayList<VFile> list) {
        mData = list;
    }

    public ArrayList<VFile> getFiles() {
        return mData;
    }

    public int getSize() {
        return mData.size();
    }

    public BasicFileListAdapter.CheckResult getSelectedCount() {
        BasicFileListAdapter.CheckResult result = new BasicFileListAdapter.CheckResult();
        result.count = mData.size();
        for (VFile vFile : mData) {
            if (vFile.isDirectory()) {
                result.hasDir = true;
                result.dircount++;
            }
        }
        return result;
    }

    public boolean contains(VFile vFile) {
        return mData.contains(vFile);
    }
}
