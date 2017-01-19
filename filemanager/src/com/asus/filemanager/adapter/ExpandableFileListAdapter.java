package com.asus.filemanager.adapter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.ShoppingCart;
import com.asus.filemanager.adapter.grouper.AbsFileGrouper;
import com.asus.filemanager.adapter.grouper.FileTypeGrouper;
import com.asus.filemanager.adapter.grouper.GameAppGrouper;
import com.asus.filemanager.adapter.grouper.InstalledAppGrouper;
import com.asus.filemanager.apprecommend.GameLaunchFile;
import com.asus.filemanager.apprecommend.RecommendUtils;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.ga.GaPromote;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExpandableFileListAdapter extends BaseExpandableListAdapter implements
        OnGroupClickListener, OnChildClickListener, OnItemLongClickListener, BasicFileListAdapter {

    private static String TAG = "ExpandableFileListAdapter";

    enum SupportLayoutType {
        LAYOUT_TYPE_FILE, LAYOUT_TYPE_GAME_LAUNCH
    }

    public class ViewHolder {
        CheckBox check;
        View container;
        ImageView icon;
        ImageView selectedIcon;
        //ImageView icon_sharing;
        LinearLayout itemInfoContainer;
        TextView name;
        TextView size;
        TextView date;
        TextView time;
        ImageView smallFolder;
        ImageView favorite;
        TextView childCount;
        int GridMode;
    }

    private Context mContext;
    private FileListFragment mFragment;

    private VFile[] mFileArray;
    private ItemIcon mItemIcon;
    private AbsFileGrouper mFileGroupHelper;
    private CheckResult mSelectedResult;
    private CheckResult mFilesResult;

    ExpandableGroupInfo[] mGroupFiles;
    Map<ExpandableGroupInfo, List<VFile>> mChildrenFiles = new HashMap<ExpandableGroupInfo, List<VFile>>();

    public ExpandableFileListAdapter(Context context, FileListFragment fragment, VFile[] files) {
        mContext = context;
        mFragment = fragment;

        mFileArray = files;
        mItemIcon = new ItemIcon(mContext, mFragment);
        mSelectedResult = getSelectedCount();

        updateGroupAndChildFiles(files);
    }

    public void updateAdapter(VFile[] files, boolean forceUpdate, int sortType, AdapterUpdateObserver observer) {
        mFileArray = files;
        new FileGrouperTask(observer, forceUpdate).execute(files);
    }

    public CheckResult getSelectedCount() {
        if(mSelectedResult != null)
            return mSelectedResult;

        int count = 0;
        boolean hasDir = false;
        int dircount = 0;
        CheckResult result = new CheckResult();
        if (mFileArray != null) {
            VFile[] mFileArrayClone = mFileArray.clone();

            for (int i = 0; i < mFileArrayClone.length; i++) {
                if (mFileArrayClone[i].getChecked()) {
                    count++;

                    if (mFileArrayClone[i].isDirectory()) {
                        hasDir = true;
                        dircount++;
                    }
                }
            }
        }
        result.count = count;
        result.hasDir = hasDir;
        result.dircount = dircount;
        return result;
    }

    public void setSelectAll() {
        if (mFileArray != null) {
            for (int i = 0; i < mFileArray.length; i++) {
                mFileArray[i].setChecked(true);
            }
        }
        if(!mFragment.mIsMultipleSelectionOp){
            mSelectedResult.count = mFileArray.length;
            mSelectedResult.hasDir = (mFilesResult.count == mFileArray.length) ? false : true;
            mSelectedResult.dircount = mFileArray.length - mFilesResult.count;
        }else{
            mSelectedResult.count = mFilesResult.count;
            mSelectedResult.hasDir = false;
            mSelectedResult.dircount = 0;
        }

        notifyDataSetInvalidated();
    }

    private void updateGroupAndChildFiles(VFile[] files) {
        if (mFragment.isCategoryDocument()) {
            mFileGroupHelper = new FileTypeGrouper(mContext, files);
        } else if (mFragment.isCategoryApk()) {
            mFileGroupHelper = new InstalledAppGrouper(mContext, files);
        } else if (mFragment.isCategoryGame()) {
            mFileGroupHelper = new GameAppGrouper(mContext, files);
        } else {
            mFileGroupHelper = new FileTypeGrouper(mContext, files);
        }
        mGroupFiles = (ExpandableGroupInfo[]) mFileGroupHelper.getGroupSet().toArray(
                new ExpandableGroupInfo[mFileGroupHelper.getGroupSet().size()]);
        mChildrenFiles = mFileGroupHelper.getMap();
    }

    private void updateEditItemStatus(VFile file) {
        if (file instanceof GameLaunchFile) {
            // Not support non-physical file in edit mode
            return;
        }

        file.setChecked(!file.getChecked());

        if (file.getChecked()) {
            mSelectedResult.count = mSelectedResult.count + 1;
            if (file.isDirectory()) {
                mSelectedResult.dircount = mSelectedResult.dircount + 1;
                mSelectedResult.hasDir = true;
            }
        } else {
            mSelectedResult.count = mSelectedResult.count - 1;
            if (file.isDirectory()) {
                mSelectedResult.dircount = mSelectedResult.dircount - 1;
                if (mSelectedResult.dircount < 1) {
                    mSelectedResult.hasDir = false;
                }
            }
        }

        notifyDataSetChanged();
        mFragment.updateEditMode();
    }

    public Object getChild(int groupPosition, int childPosition) {
        ExpandableGroupInfo groupInfo = mGroupFiles[groupPosition];
        return mChildrenFiles.get(groupInfo).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        ExpandableGroupInfo groupInfo = mGroupFiles[groupPosition];
        return mChildrenFiles.get(groupInfo).size();
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        ExpandableGroupInfo groupInfo = mGroupFiles[groupPosition];
        List<VFile> groups = mChildrenFiles.get(groupInfo);
        final VFile file = groups.get(childPosition);

        if (file instanceof GameLaunchFile) {
            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.game_launch_list_item, parent, false);
            }
            setupViewForGameLaunchFile(convertView, file);
        } else {
            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.file_list_item, parent, false);
            }
            setupViewForFile(convertView, file);
        }

        return convertView;
    }

    private void setupViewForGameLaunchFile(View convertView, VFile file) {
        if (!(file instanceof GameLaunchFile)) {
            return;
        }

        GameLaunchFile gameLaunchFile = (GameLaunchFile) file;
        TextView appName = (TextView) convertView.findViewById(R.id.game_launch_list_item_app_name);
        if (appName != null) {
            appName.setText(gameLaunchFile.getAppName());
        }
        TextView categoryName = (TextView) convertView.findViewById(R.id.game_launch_list_item_app_category);
        if (categoryName != null) {
            categoryName.setText(RecommendUtils.getCategoryName(mContext, gameLaunchFile.getAppCategory()));
        }
        ImageView icon = (ImageView) convertView.findViewById(R.id.game_launch_list_item_app_icon);
        if (icon != null) {
            mItemIcon.setIcon(file, icon, true);
        }
    }

    private void setupViewForFile(View convertView, final VFile file) {
        ViewHolder holder = new ViewHolder();
        holder.check = (CheckBox) convertView.findViewById(R.id.file_list_item_check);
        holder.container = convertView.findViewById(R.id.file_list_item_container);
        holder.icon = (ImageView) convertView.findViewById(R.id.file_list_item_icon);
        holder.selectedIcon = (ImageView) convertView.findViewById(R.id.file_list_item_selected_icon);
        //holder.icon_sharing = (ImageView) convertView.findViewById(R.id.file_list_item_sharing);
        holder.name = (TextView) convertView.findViewById(R.id.file_list_item_name);
        holder.size = (TextView) convertView.findViewById(R.id.file_list_item_size);
        holder.date = (TextView) convertView.findViewById(R.id.file_list_item_date);
        holder.time = (TextView) convertView.findViewById(R.id.file_list_item_time);
        holder.smallFolder = (ImageView)convertView.findViewById(R.id.file_list_item_small_folder);
        holder.favorite = (ImageView) convertView.findViewById(R.id.file_list_item_favorite);
        holder.childCount = (TextView) convertView.findViewById(R.id.child_count);
        holder.itemInfoContainer = (LinearLayout) convertView.findViewById(R.id.file_list_info_container);

        if (holder.check != null) {
            holder.check.setVisibility(mFragment.isInEditMode() ? View.VISIBLE : View.GONE);
            holder.check.setChecked(file.getChecked());
            holder.check.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateEditItemStatus(file);
                }
            });
            convertView.setBackgroundColor(file.getChecked() ?
                    convertView.getResources().getColor(ThemeUtility.getItemSelectedBackgroundColor()) :
                    convertView.getResources().getColor(android.R.color.transparent));
        }

        if (holder.icon != null) {
            holder.icon.setTag(file.getAbsolutePath());
            mItemIcon.setIcon(file, holder.icon, true);
        }

        if (holder.name != null) {
            holder.name.setText(file.getName());
        }

        if (holder.childCount != null) {
            holder.childCount.setText(Formatter.formatFileSize(mContext, file.length()));
        }

        if (holder.date != null) {
            holder.date.setClickable(false);
            holder.date.setOnTouchListener(null);
            java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(mContext);
            java.text.DateFormat shortTimeFormat = DateFormat.getTimeFormat(mContext);
            Date date = new Date(file.lastModified());
            String shortDate = shortDateFormat.format(date);
            if (shortDate.length() != 10) {
                shortDate = fixedLengthShortDate(shortDate, "/");
                shortDate = fixedLengthShortDate(shortDate, "-");
            }
            holder.date.setText(shortDate + " " + shortTimeFormat.format(date));
        }

        if (holder.size != null) {
            // TODO: FIX ME
            holder.size.setVisibility(View.GONE);
        }
    }

    public ExpandableGroupInfo getGroup(int groupPosition) {
        return mGroupFiles[groupPosition];
    }

    public int getGroupCount() {
        return mGroupFiles.length;
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.expandablelist_group, parent, false);
        }

        View groupSpace = convertView.findViewById(R.id.group_space);
        if (groupSpace != null) {
            // groupSpace.setVisibility(groupPosition == 0 ? View.GONE : View.VISIBLE);
        }

        TextView groupName = (TextView) convertView.findViewById(R.id.group_name);
        if (groupName != null) {
            ExpandableGroupInfo groupInfo = getGroup(groupPosition);
            int id = groupInfo.getId();
            String title = groupInfo.getTitle();
            if (id == ExpandableGroupInfo.TitleType.RECOMMENDED
                    || id == ExpandableGroupInfo.TitleType.RECOMMENDED_GAME) {
                groupName.setText(title);
            } else {
                groupName.setText(String.format("%s (%d)", title, getChildrenCount(groupPosition)));
            }
        }

        ImageView indicatorOpen = (ImageView) convertView.findViewById(R.id.indicator_open);

        if (indicatorOpen != null) {
            indicatorOpen.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            ThemeUtility.setItemIconColor(mContext, indicatorOpen.getDrawable());
        }

        ImageView indicatorClose = (ImageView) convertView.findViewById(R.id.indicator_close);
        if (indicatorClose != null) {
            indicatorClose.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            ThemeUtility.setItemIconColor(mContext, indicatorClose.getDrawable());
        }

        return convertView;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }

    private static String fixedLengthShortDate(String shortDate, String splitStr) {
        String[] dateVal = shortDate.split(splitStr);
        if (dateVal != null && dateVal.length != 1) {
            shortDate = "";
            for (String val: dateVal) {
                if (1 == val.length()) {
                    shortDate += "0" + val;
                } else {
                    shortDate += val;
                }
                shortDate += splitStr;
            }
            shortDate = shortDate.substring(0, shortDate.length()-1);
        }
        return shortDate;
    }

    @Override
    public boolean onGroupClick(ExpandableListView expandableListView, View v, int groupPosition, long id) {
        if (expandableListView.isGroupExpanded(groupPosition)) {
            mContext.getSharedPreferences(ExpandableGroupInfo.TAG_SUB_CATEGORY_PREFERENCE, Context.MODE_PRIVATE)
                    .edit().putBoolean(String.valueOf(getGroup(groupPosition).getId()), false).commit();
        } else {
            mContext.getSharedPreferences(ExpandableGroupInfo.TAG_SUB_CATEGORY_PREFERENCE, Context.MODE_PRIVATE)
                    .edit().putBoolean(String.valueOf(getGroup(groupPosition).getId()), true).commit();
        }
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        VFile selectedFile = (VFile) getChild(groupPosition, childPosition);

        if (mFragment.isInEditMode()) {
            updateEditItemStatus(selectedFile);
        } else {
            if (selectedFile instanceof GameLaunchFile) {
                openGameLaunchFile((GameLaunchFile) selectedFile);
            } else {
                openPhysicalFile(selectedFile);
            }
        }
        return true;
    }

    private void openGameLaunchFile(GameLaunchFile file) {
        String packageName = file.getAppPackageName();
        PackageManager manager = mContext.getPackageManager();
        Intent i = manager.getLaunchIntentForPackage(packageName);
        if (i == null) {
            return;
        }
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        mContext.startActivity(i);
    }

    private void openPhysicalFile(VFile file) {
        boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mFragment.getActivity()).isNeedToWriteSdToAppFolder(file.getAbsolutePath());
        if((file.getName().toLowerCase().endsWith(".zip") || file.getName().toLowerCase().endsWith(".rar") )
            && bNeedWriteToAppFolder){
            WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                .newInstance();
            warnDialog.show(mFragment.getActivity().getFragmentManager(),
                "WarnKKSDPermissionDialogFragment");
        }else if ((file.getName().toLowerCase().endsWith(".zip") || file.getName().toLowerCase().endsWith(".rar") )
            && SafOperationUtility.getInstance(mFragment.getActivity()).isNeedToShowSafDialog(file.getAbsolutePath())){
            ((FileManagerActivity) mFragment.getActivity()).callSafChoose(SafOperationUtility.ACTION_EXTRACT);
        }else{
            FileUtility.openFile(mFragment.getActivity(), file, false /*TODO*/, false, true, mFragment.isCategoryRecent());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> itemView, View view, int position,
            long id) {
        int groupPosition = ExpandableListView.getPackedPositionGroup(id);
        int childPosition = ExpandableListView.getPackedPositionChild(id);

        if (childPosition == -1) {
            // TODO: NOT SUPPORT SELECT GROUP YET
            // selectGroup(groupPosition);
            return true;
        }
        VFile selectedFile = (VFile) getChild(groupPosition, childPosition);

        updateEditItemStatus(selectedFile);
        return true;
    }

    @Override
    public boolean isItemsSelected() {
        boolean isItemSelected = false;
        if (mFileArray != null) {
            for (int i = 0; i < mFileArray.length; i++) {
                if (mFileArray[i].getChecked()) {
                    isItemSelected = true;
                    break;
                }
            }
        }
        return isItemSelected;
    }

    @Override
    public VFile[] getFiles() {
        return mFileArray;
    }

    @Override
    public void clearCacheTag() {
        // TODO
    }

    @Override
    public void updateAdapterResult() {
        mSelectedResult = null;
        mSelectedResult = getSelectedCount();
        mFilesResult = null;
        mFilesResult = getFilesCount();
    }

    @Override
    public void isHiddenDate(boolean hidden) {
        // May not use anymore
        // consider to delete this function both from FileListAdapter & ExpandableFileListAdapter
    }

    @Override
    public int getCount() {
        return (mFileArray == null) ? 0 : mFileArray.length;
    }

    @Override
    public void onDrop(int position) {
        // TODO
    }

    @Override
    public void setOrientation(int orientation) {
        // TODO
    }

    @Override
    public Object getItem(int position) {
        return (mFileArray == null) ? null : mFileArray[position];
    }

    @Override
    public CheckResult getFilesCount() {
        if(mFilesResult != null)
            return mFilesResult;
        int count = 0;
        boolean hasDir = false;
        CheckResult result = new CheckResult();
        if (mFileArray != null) {
            VFile[] mFileArrayClone = mFileArray.clone();

            for (int i = 0; i < mFileArrayClone.length; i++) {
                if (!mFileArrayClone[i].isDirectory()) {
                    count++;
                }
            }
        }
        result.count = count;
        result.hasDir = hasDir;
        return result;
    }

    @Override
    public void clearItemsSelected() {
        if (mFileArray != null) {
            for (int i = 0; i < mFileArray.length; i++) {
                mFileArray[i].setChecked(false);
            }
            mSelectedResult.count = 0;
            mSelectedResult.hasDir = false;
            mSelectedResult.dircount = 0;
        }
    }

    @Override
    public void setShoppingCart(ShoppingCart shoppingCart) {
        // TODO
    }

    @Override
    public void syncWithShoppingCart() {
        // TODO
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        VFile vFile = (VFile) getChild(groupPosition, childPosition);

        if (vFile instanceof GameLaunchFile) {
            return SupportLayoutType.LAYOUT_TYPE_GAME_LAUNCH.ordinal();
        } else {
            return SupportLayoutType.LAYOUT_TYPE_FILE.ordinal();
        }
    }

    @Override
    public int getChildTypeCount() {
        return SupportLayoutType.values().length;
    }

    public void expandGroupIfNeeded(ExpandableListView expandableListView) {
        int groupCount = getGroupCount();
        if (groupCount == 0) {
            return;
        }

        expandGroupBySettings(expandableListView, groupCount);
        expandRecommendGroupIfFirstUse(expandableListView, groupCount);
    }

    private void expandGroupBySettings(ExpandableListView expandableListView, int groupCount) {
        SharedPreferences sp = mContext.getSharedPreferences(
                ExpandableGroupInfo.TAG_SUB_CATEGORY_PREFERENCE, Context.MODE_PRIVATE);

        for (int i = 0; i < groupCount; i++) {
            if (sp.getBoolean(String.valueOf(getGroup(i).getId()), false)) {
                expandableListView.expandGroup(i);
            } else {
                expandableListView.collapseGroup(i);
            }
        }
    }

    private void expandRecommendGroupIfFirstUse(ExpandableListView expandableListView, int groupCount) {
        SharedPreferences sp = mContext.getSharedPreferences(
                ExpandableGroupInfo.TAG_SUB_CATEGORY_PREFERENCE, Context.MODE_PRIVATE);

        for (int i = 0; i < groupCount; i++) {
            if (getGroup(i).getId() == ExpandableGroupInfo.TitleType.RECOMMENDED
                    && !sp.contains(ExpandableGroupInfo.KEY_HAS_EXPAND_RECOMMEND_SECTION)) {
                expandableListView.expandGroup(i);
                sp.edit().putBoolean(
                        ExpandableGroupInfo.KEY_HAS_EXPAND_RECOMMEND_SECTION, true).commit();
                sp.edit().putBoolean(
                        String.valueOf(getGroup(i).getId()), true).commit();
            } else if (getGroup(i).getId() == ExpandableGroupInfo.TitleType.RECOMMENDED_GAME
                    && !sp.contains(ExpandableGroupInfo.KEY_HAS_EXPAND_RECOMMEND_SECTION_GAME)) {
                expandableListView.expandGroup(i);
                sp.edit().putBoolean(
                        ExpandableGroupInfo.KEY_HAS_EXPAND_RECOMMEND_SECTION_GAME, true).commit();
                sp.edit().putBoolean(
                        String.valueOf(getGroup(i).getId()), true).commit();
            }
        }
    }

    private class FileGrouperTask extends AsyncTask<VFile, Void, Void> {
        private AdapterUpdateObserver mObserver;
        private boolean mForceUpdate;

        public FileGrouperTask(AdapterUpdateObserver observer, boolean forceUpdate) {
            mObserver = observer;
            mForceUpdate = forceUpdate;
        }

        @Override
        protected Void doInBackground(VFile... files) {
            updateGroupAndChildFiles(files);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mForceUpdate) {
                notifyDataSetInvalidated();
            } else {
                notifyDataSetChanged();
            }

            if (mObserver != null) {
                mObserver.updateAdapterDone();
            }
        }
    }
}
