package com.asus.filemanager.adapter.grouper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.ExpandableGroupInfo;
import com.asus.filemanager.utility.VFile;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class InstalledAppGrouper extends AbsFileGrouper {
    private static String TAG = "InstalledAppGrouper";

    private PackageManager mPackageManager;

    public InstalledAppGrouper(Context context, VFile[] data) {
        super(context, data);
        startGroupProcess();
    }

    protected void startGroupProcess() {
        mPackageManager = mContext.getPackageManager();

        if (mData == null) {
            return;
        }

        List<VFile> list = null;
        ExpandableGroupInfo groupInfo;
        for (VFile file : mData) {
            groupInfo = getGroupInfo(file);
            if (groupInfo == null) {
                continue;
            }
            if (mResultList.containsKey(groupInfo)) {
                mResultList.get(groupInfo).add(file);
            } else {
                list = new ArrayList<VFile>();
                list.add(file);
                mResultList.put(groupInfo, list);
            }
        }
    }

    private ExpandableGroupInfo getGroupInfo(File file) {
        if (file == null) {
            return null;
        }

        PackageInfo pmi = mPackageManager.getPackageArchiveInfo(file.getPath(), PackageManager.GET_ACTIVITIES);
        if (pmi == null) {
            return new ExpandableGroupInfo(mContext.getResources().getString(R.string.group_title_others),
                    ExpandableGroupInfo.TitleType.OTHERS);
        }

        if (isInstalledPackage(pmi.packageName)) {
            return new ExpandableGroupInfo(mContext.getResources().getString(R.string.group_title_installed),
                    ExpandableGroupInfo.TitleType.INSTALLED);
        }

        return new ExpandableGroupInfo(mContext.getResources().getString(R.string.group_title_not_installed),
                ExpandableGroupInfo.TitleType.NOT_INSTALLED);
    }

    private boolean isInstalledPackage(String packageName) {
        try {
            mPackageManager.getApplicationInfo(packageName, 0);
            return true;
        } catch (NameNotFoundException e) {
        }
        return false;
    }
}
