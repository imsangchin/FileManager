package com.asus.filemanager.adapter.grouper;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.asus.filemanager.adapter.ExpandableGroupInfo;
import com.asus.filemanager.utility.VFile;

import android.content.Context;

public abstract class AbsFileGrouper {

    protected Context mContext;
    protected VFile[] mData;
    protected Map<ExpandableGroupInfo, List<VFile>> mResultList;

    public AbsFileGrouper(Context context, VFile[] data) {
        mContext = context;
        mData = data;
        mResultList = new TreeMap<ExpandableGroupInfo, List<VFile>>();
    }

    abstract protected void startGroupProcess();

    public Set<ExpandableGroupInfo> getGroupSet() {
        return mResultList.keySet();
    }

    public Set<Entry<ExpandableGroupInfo, List<VFile>>> getEntrySet() {
        return mResultList.entrySet();
    }

    public Map<ExpandableGroupInfo, List<VFile>> getMap() {
        return mResultList;
    }
}
