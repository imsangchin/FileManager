package com.asus.filemanager.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.HttpServerActivity;
import com.asus.filemanager.activity.TutorialActivity;
import com.asus.filemanager.filesystem.LocalStorage;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filetransfer.filesystem.IInputFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yenju_Lai on 2015/12/10.
 */
public class ExternalStorageAccessAdapter extends BaseExpandableListAdapter implements ExpandableListView.OnGroupClickListener,ExpandableListView.OnChildClickListener {

    private HttpServerActivity httpServerActivity;
    ArrayList<IInputFile> storageList = new ArrayList<>();
    TextView title;

    public ExternalStorageAccessAdapter(HttpServerActivity context) {
        this.httpServerActivity = context;
        title = new TextView(context);
        title.setText("無下列外部儲存裝置存取權限：");
    }
    public void updateStorageList(List<IInputFile> allStorage) {
        storageList.clear();
        for (IInputFile storage : allStorage)
            if (!((LocalStorage)storage).isLocalStorage()
                    && SafOperationUtility.getInstance().isNeedToShowSafDialog(storage.getPath()))
                storageList.add(storage);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getGroupCount() {
        return storageList.size() > 0? 1 : 0;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return storageList.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        return title;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) httpServerActivity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.access_storage_item, parent, false);
        }
        TextView textView = (TextView) convertView.findViewById(R.id.storage_name);
        textView.setText(storageList.get(childPosition).getName());
        CheckBox accessible = (CheckBox) convertView.findViewById(R.id.storage_accessible);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {

    }

    @Override
    public void onGroupCollapsed(int groupPosition) {

    }

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        return 0;
    }

    @Override
    public long getCombinedGroupId(long groupId) {
        return 0;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        httpServerActivity.acquireExternalStoragePermission();
        return true;
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        return parent.isGroupExpanded(groupPosition);
    }
}
