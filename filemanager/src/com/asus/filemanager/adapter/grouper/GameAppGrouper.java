package com.asus.filemanager.adapter.grouper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.ExpandableGroupInfo;
import com.asus.filemanager.apprecommend.GameLaunchFile;
import com.asus.filemanager.utility.VFile;

import android.content.Context;

public class GameAppGrouper extends AbsFileGrouper {
    private static String TAG = "GameAppGrouper";

    public GameAppGrouper(Context context, VFile[] data) {
        super(context, data);
        startGroupProcess();
    }

    protected void startGroupProcess() {
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
        if (file == null || !(file instanceof GameLaunchFile)) {
            return null;
        }

        return new ExpandableGroupInfo(mContext.getResources().getString(R.string.group_title_installed),
                ExpandableGroupInfo.TitleType.INSTALLED_GAME);
    }
}
