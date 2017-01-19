
package com.asus.filemanager.adapter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Config;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.ShoppingCart;
import com.asus.filemanager.adapter.ExpandableFileListAdapter.SupportLayoutType;
import com.asus.filemanager.dialog.DropDialogFragment;
import com.asus.filemanager.dialog.GuildLinePopup;
import com.asus.filemanager.dialog.ItemSelectPopMenuDialog;
import com.asus.filemanager.dialog.PasteDialogFragment;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.ga.GaOpenFile;
import com.asus.filemanager.ga.GaPromote;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.ui.SlideListView.RemoveDirection;
import com.asus.filemanager.ui.SlideListView.SlideOutListener;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.ConstantsUtil.OpenType;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FileUtility.FileInfo;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.MimeMapUtility;
import com.asus.filemanager.utility.MimeMapUtility.FileType;
import com.asus.filemanager.utility.PathIndicator;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.ViewUtility;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.dialog.CloudStorageLoadingDialogFragment;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

//import com.asus.pen.PenLibrary;
//import com.asus.pen.provider.PenSettings;

public class FileListAdapter extends BaseAdapter implements OnClickListener, OnTouchListener, OnHoverListener ,
                                                            OnItemClickListener,OnItemLongClickListener, SlideOutListener, BasicFileListAdapter {

    private static final String TAG = "FileListAdapter";
    private static final boolean DEBUG = ConstantsUtil.DEBUG;
    private static final String DEFAULT_SHARED_PATH = (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN ?
            reflectionApis.getLegacyExternalStorageDirectory().getAbsolutePath() : Environment.getExternalStorageDirectory().getAbsolutePath());

    private VFile[] mFileArray;
    private Context mContext;
    private FileListFragment mFragment;
    private int mOrientation = 0;
    private boolean mIsAttachOp = false;
    private boolean mIsHiddenDate = false;
    private float mTouchX = -1;
    private ItemIcon mItemIcon;
    private Typeface mNormalType;
    private Typeface mBoldType;
    public int mSortType;
    private ContentResolver mCr;
    private String[] mSharedPaths;
    private String[] mShareParentPaths;
    private boolean mUpdateSharedPaths = false;

    private PopupBuilder mPopupBuilder;
    private boolean isPadMode = false;
    private boolean isPadMode2 = false;

    private CheckResult mSelectedResult;
    private CheckResult mFilesResult;

    //private String mDRMDirectorys = new String();

    private View mTmpSwipeitemView;
    public int mGridWidth;
    private ShoppingCart mShoppingCart;

    private class ViewHolder {
        CheckBox check;
        View container;
        ImageView icon;
        ImageView selectedIcon;
        RelativeLayout albumBackground;
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

    public FileListAdapter(FileListFragment fragment, VFile[] files, boolean isAttachOp) {
        mContext = fragment.getActivity();
        mFragment = fragment;

        mIsAttachOp = isAttachOp;

        mFileArray = files;

        mItemIcon = new ItemIcon(fragment.getActivity().getApplicationContext(), mFragment);
        mOrientation = fragment.getResources().getConfiguration().orientation;

        mNormalType = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        mBoldType = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);

        mPopupBuilder = new PopupBuilder(mFragment.getActivity());
        //++felix_zhang
        isPadMode = ((FileManagerActivity)mFragment.getActivity()).isPadMode();
        isPadMode2 = ((FileManagerActivity)mFragment.getActivity()).isPadMode2();

        mCr = mFragment.getActivity().getContentResolver();
    }

    public VFile[] getFiles() {
        return mFileArray;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    @Override
    public int getCount() {
        return (mFileArray == null) ? 0 : mFileArray.length;
    }

    @Override
    public Object getItem(int position) {
        return (mFileArray == null)  || (position >= mFileArray.length) ? null : mFileArray[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        boolean isListViewMode = ItemOperationUtility.getInstance().isListViewMode();

        int layoutType = getItemViewType(position);

        //dirty fix....we only support list mode for samba, TT651998
        if (SambaFileUtility.updateHostIp || RemoteFileUtility.isShowDevicesList)
            isListViewMode = true;

        if(mTmpSwipeitemView != null){
            mTmpSwipeitemView.setBackgroundColor(mTmpSwipeitemView.getResources().getColor(android.R.color.transparent));
            mTmpSwipeitemView = null;
        }

        if (convertView == null || ((!isListViewMode) && ((ViewHolder) convertView.getTag()).GridMode != ConstantsUtil.mGridMode)) {
            holder = new ViewHolder();
            if(isListViewMode){
                if(mFragment.getGridView().getVisibility() == 0){
                    mFragment.getGridView().setVisibility(View.VISIBLE);
                }
                convertView = mFragment.getActivity().getLayoutInflater().inflate(R.layout.file_list_item, parent, false);
                holder.check = (CheckBox) convertView.findViewById(R.id.file_list_item_check);
            }else{
                if (ConstantsUtil.mGridMode == ConstantsUtil.GRID_MODE_MEDIA){
                    convertView =  mFragment.getActivity().getLayoutInflater().inflate(R.layout.media_grid_item, parent, false);
                }else{
                convertView =  mFragment.getActivity().getLayoutInflater().inflate(R.layout.file_grid_item, parent, false);
            }
                holder.GridMode = ConstantsUtil.mGridMode;
            }

            holder.container = convertView.findViewById(R.id.file_list_item_container);
            holder.icon = (ImageView) convertView.findViewById(R.id.file_list_item_icon);
            holder.selectedIcon = (ImageView) convertView.findViewById(R.id.file_list_item_selected_icon);
            holder.albumBackground = (RelativeLayout) convertView.findViewById(R.id.album_background);
            //holder.icon_sharing = (ImageView) convertView.findViewById(R.id.file_list_item_sharing);
            holder.name = (TextView) convertView.findViewById(R.id.file_list_item_name);
            holder.size = (TextView) convertView.findViewById(R.id.file_list_item_size);
            holder.date = (TextView) convertView.findViewById(R.id.file_list_item_date);
            holder.time = (TextView) convertView.findViewById(R.id.file_list_item_time);
            holder.smallFolder = (ImageView)convertView.findViewById(R.id.file_list_item_small_folder);
            holder.favorite = (ImageView) convertView.findViewById(R.id.file_list_item_favorite);
            holder.childCount = (TextView) convertView.findViewById(R.id.child_count);
            holder.itemInfoContainer = (LinearLayout) convertView.findViewById(R.id.file_list_info_container);

            if(isListViewMode){
                if (holder.date != null) {
                    holder.date.setVisibility(View.VISIBLE);
                }
                if (holder.time != null) {
                    holder.time.setVisibility(View.VISIBLE);
                }
            }else{
                if (holder.date != null) {
                    holder.date.setVisibility(View.GONE);
                }
                if (holder.time != null) {
                    holder.time.setVisibility(View.GONE);
                }
            }

            if(WrapEnvironment.SUPPORT_FEATURE_ASUS_PEN){
                //hide airview
                disableAirView(holder.name);
            }

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.name.setTypeface(null);
        }

        if (mFileArray != null && position < mFileArray.length) {
            boolean isFolder = mFileArray[position].isDirectory();
            //Typeface typeface = isFolder == true ? mBoldType : mNormalType;
            Typeface typeface = mNormalType;
            if (holder.check != null) {
                holder.check.setTag(position);

                if (mIsAttachOp || (mFileArray[0].getParent()).equals(RemoteFileUtility.getInstance(mFragment.getActivity()).getHomeCloudRootPath())
                        || !mFragment.isInEditMode() || mFragment.isInSecondLayerEditMode()) {
                   // holder.check.setVisibility(View.INVISIBLE);
                    holder.check.setVisibility(View.GONE);
                }else if(mFragment.mIsMultipleSelectionOp && isFolder){
                    holder.check.setChecked(false);
                    holder.check.setEnabled(false);

                    holder.check.setVisibility(View.VISIBLE);
                }
                else {
                    holder.check.setVisibility(View.VISIBLE);
                    holder.check.setEnabled(true);
                    holder.check.setTag(position);
                    Log.e("test", "!: " + mShoppingCart.contains(mFileArray[position]));
                    holder.check.setChecked(mShoppingCart == null ? mFileArray[position].getChecked() : mShoppingCart.contains(mFileArray[position]));
                    holder.check.setOnClickListener(this);
                    holder.check.setOnTouchListener(this);
                }
            }

            int res = MimeMapUtility.getIconRes(mFileArray[position]);

            if (holder.container != null) {
                holder.container.setTag(position);
                //holder.container.setOnClickListener(this);
                //holder.container.setOnLongClickListener(this); // Johnson
                //holder.container.setOnTouchListener(this); // Johnson
                //holder.container.setLongClickable(mFragment.mActivity.isPadMode());

                // [S] YuAn, 20160726, TT840599
                // Workaround, disable this function for specific project ZB500KL.
                if(!WrapEnvironment.MODEL_NAME.contains("ASUS_X00AD_"))
                {
                    holder.container.setOnHoverListener(this);
                }
                // [E] YuAn, 20160726, TT840599

                //PenLibrary.setStylusIcon((View)holder.container, PenLibrary. STYLUS_ICON_FOCUS);
                if(WrapEnvironment.SUPPORT_FEATURE_ASUS_PEN){
                    setStylusIcon(holder.container);
                }
            }

            if (holder.icon != null) {
                holder.icon.setTag(mFileArray[position].getAbsolutePath());
                if (!mFragment.isMovingDivider()) {
                    mItemIcon.setIcon(mFileArray[position], holder.icon, true);
                }
            }

            if (holder.name != null) {
                holder.name.setTypeface(typeface);
                if (!mFragment.isMovingDivider()) {
                    VFile item = mFileArray[position];
                    // decide to show favorite name or name
                    String name = (mFragment.getmIndicatorFile() != null && mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_FAVORITE_FILE) && mFileArray[position].isFavoriteFile() ? item.getFavoriteName() : item.getName());
                    if (isListViewMode) {
                        holder.name.setText(name);
                        // Only occurs at pf500kl kitkat CN
                        if (isDeviceNamed_PF500KL_Kitkat_CN(mFragment.getActivity())) {
                            ellipsizeMultipleLine(holder.name, 1);
                        }
                    } else {
                        holder.name.setGravity(Gravity.CENTER_HORIZONTAL);
                        // the grid view directory should show the child count
                        if ((mFragment.isCategoryImage() || mFragment.isCategoryMusic() || mFragment.isCategoryVideo() )&& item.isDirectory()) {
                            name = String.format("%s (%d)", name, item.getChildCount());
                        }
                        holder.name.setText(name);

                        if (mFragment.isCategoryMedia()) {
                            holder.name.setGravity(Gravity.LEFT);
                            ellipsizeMultipleLine(holder.name, 1);
                        }else{
                            ellipsizeMultipleLine(holder.name, 2);
                        }
                    }
                    holder.name.requestLayout();
                }
                holder.name.setTag(position);
            }

            final Configuration conf = convertView.getResources().getConfiguration();

            if (holder.date != null) {
                holder.date.setClickable(false);
                holder.date.setOnTouchListener(null);
                java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(mFragment.getActivity());
                java.text.DateFormat shortTimeFormat = DateFormat.getTimeFormat(mFragment.getActivity());
                Date date = new Date(mFileArray[position].lastModified());
                String shortDate = shortDateFormat.format(date);
                if (shortDate.length() != 10) {
                    shortDate = fixedLengthShortDate(shortDate, "/");
                    shortDate = fixedLengthShortDate(shortDate, "-");
                }
                holder.date.setText(shortDate + " " + shortTimeFormat.format(date));
                holder.date.setTypeface(mNormalType/*typeface*/);
                // holder.date.setVisibility(View.VISIBLE);
                holder.date.setTag(position);
            }

            if (holder.size != null) {
                if (mOrientation == Configuration.ORIENTATION_LANDSCAPE && mFragment.mActivity.isPadMode()) {
                    holder.size.setVisibility(View.VISIBLE);
                    if (!mFragment.isMovingDivider()) {
                        if (!isFolder) {
                            holder.size.setText(FileUtility.bytes2String(mFragment.getActivity().getApplicationContext(), mFileArray[position].length(), 1));
                        } else {
                            holder.size.setText(null);
                        }
                        holder.size.setTag(position);
                    }
                } else {
                    holder.size.setVisibility(View.GONE);
                }
            }

            if (holder.smallFolder != null) {
                holder.smallFolder.setVisibility(mFragment.isCategoryMedia() && mFileArray[position].isDirectory() && isListViewMode ? View.VISIBLE : View.GONE);
            }
            if (!isListViewMode){
                if (mFragment.isCategoryMedia() || mFragment.belongToCategoryMedia()) {
                    if (mFileArray[position].isDirectory()) {
                        //folder
                        holder.icon.getLayoutParams().width = mGridWidth - ViewUtility.dp2px(mFragment.getActivity(),37);//111
                        holder.icon.getLayoutParams().height = mGridWidth - ViewUtility.dp2px(mFragment.getActivity(),37);//111;
                        if (holder.selectedIcon != null) {
                            holder.selectedIcon.getLayoutParams().width = mGridWidth -ViewUtility.dp2px(mFragment.getActivity(),37);//111;
                            holder.selectedIcon.getLayoutParams().height = mGridWidth -ViewUtility.dp2px(mFragment.getActivity(),37);//111;
                        }
                        if (holder.albumBackground != null) {
                            //holder.albumBackground.setBackgroundResource(R.drawable.asus_filemanager_bg_album);
                            holder.albumBackground.requestLayout();
                        }
                    } else {
                        //file
                        holder.icon.getLayoutParams().width = mGridWidth - 2;
                        holder.icon.getLayoutParams().height = mGridWidth - 2;
                        if (holder.selectedIcon != null){
                            holder.selectedIcon.getLayoutParams().width = mGridWidth - 2;
                            holder.selectedIcon.getLayoutParams().height = mGridWidth - 2;
                        }
                        convertView.setBackground(null);
                    }

                    View sdIndicator = convertView.findViewById(R.id.sd_indicator);
                    if (null != sdIndicator){
                        if (SafOperationUtility.getInstance().belongToInternalStorage(mFileArray[position].getAbsolutePath())){
                            sdIndicator.setVisibility(View.INVISIBLE);
                        }else{
                            sdIndicator.setVisibility(View.VISIBLE);
                        }
                    }
                    holder.icon.requestLayout();
                }else{
                    holder.icon.getLayoutParams().width = ViewUtility.dp2px(mFragment.getActivity(),102);
                    holder.icon.getLayoutParams().height = ViewUtility.dp2px(mFragment.getActivity(),102);
                    if (holder.selectedIcon != null) {
                        holder.selectedIcon.getLayoutParams().width = ViewUtility.dp2px(mFragment.getActivity(), 102) ;
                        holder.selectedIcon.getLayoutParams().height = ViewUtility.dp2px(mFragment.getActivity(), 102);
                    }
                    holder.icon.requestLayout();
                    convertView.setBackground(null);
                }
            }

            if (holder.favorite != null) {
                holder.favorite.setTag(position);
                holder.favorite.setVisibility(mFileArray[position].isFavoriteFile() ? View.VISIBLE : View.INVISIBLE);
            }
            if (holder.childCount != null) {
                holder.childCount.setTag(position);
                if (mFileArray[position].isDirectory()) {
                    int childCount = mFileArray[position].getChildCount();
                    String itemString = mFragment.getString(childCount > 1? R.string.items : R.string.item);
                    holder.childCount.setText(String.format("%s %s", String.valueOf(childCount), itemString));
                } else {
                    if (mFragment.belongToCategoryMusic()) {
                        holder.childCount.setText(convertSecondsToHMmSs(mFileArray[position].getChildCount()));
                    } else {
                        holder.childCount.setText(Formatter.formatFileSize(mFragment.getActivity(), mFileArray[position].length()));
                    }
                }
                // Since CloudStorage doesn't support getAllChildCount function yet, now we only show child count with local storage.
                if (mFileArray[position].getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
                    holder.childCount.setVisibility(View.VISIBLE);
                } else {
                    holder.childCount.setVisibility(mFileArray[position].isDirectory()? View.INVISIBLE : View.VISIBLE);
                }
            }
           if (holder.itemInfoContainer != null) {
                holder.itemInfoContainer.setTag(position);
            }
            convertView.setTag(R.id.file_list_item_icon, position);
//            convertView.setOnClickListener(this);
//            convertView.setOnLongClickListener(this); // Johnson
            convertView.setOnTouchListener(this); // Johnson
            // convertView.setLongClickable(true);
            // if(ListViewMode && holder.check != null){
            if (mFragment.isInEditMode() &&
                    !mFragment.isInSecondLayerEditMode() &&
                    (mShoppingCart == null ? mFileArray[position].getChecked() : mShoppingCart.contains(mFileArray[position]))) {// && holder.check.isChecked()){
                if (isListViewMode) {
                    convertView.setBackgroundColor(convertView.getResources().getColor(ThemeUtility.getItemSelectedBackgroundColor()));
                } else {
                    if (holder.selectedIcon != null) {
                        holder.selectedIcon.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                if (isListViewMode) {
                    convertView.setBackgroundColor(convertView.getResources().getColor(android.R.color.transparent));
                } else {
                    if (holder.selectedIcon != null) {
                        holder.selectedIcon.setVisibility(View.GONE);
                    }
                }
            }
            // }
        }

        if(isListViewMode){
            if(mFragment.getGridView().isShown()){
                mFragment.getGridView().setVisibility(View.GONE);
            }
        }else{
            if(mFragment.getListView().isShown()){
                mFragment.getListView().setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    public void updateAdapter(VFile[] files, boolean forceUpdate, int sortType, AdapterUpdateObserver observer) {
    /*    if (files!=null && files.length>0&&  files[0].getVFieType()==VFileType.TYPE_CLOUD_STORAGE) {
            for (VFile vFile : files) {
                Log.i("felix_zhang","updateAdapter:storageType:"+((RemoteVFile)vFile).getStorageType() +"fileid:"+((RemoteVFile)vFile).getFileID()+"deviceid:"+((RemoteVFile)vFile).getmDeviceId());
            }
        }*/

        if (mFileArray != null && mFileArray.length > 0 && files != null && files.length > 0) {
            if (!mFileArray[0].getAbsolutePath().equals(files[0].getAbsolutePath())) {
                forceUpdate = true;
            }

            if (mFileArray[0].getParent() != null && !mFileArray[0].getParent().equals(files[0].getParent())) {
                mItemIcon.clearCache();
            }

        }

        // +++ for remote storage thumbnail, we should keep the remote storage list
        //if (PathIndicator.getIndicatorVFileType() != VFileType.TYPE_LOCAL_STORAGE) {
        if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE) {
            FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
            fileListFragment.setRemoteThumbnailList(files);

            // update the checked status for remote storage
            if (mFileArray != null && mFileArray.length > 0 && files != null && files.length > 0 && mFileArray.length == files.length) {
                if (mFileArray[0].getParent() != null && mFileArray[0].getParent().equals(files[0].getParent())) {
                    for (int i=0 ; i<mFileArray.length ; i++) {
                        if (mFileArray[i].getAbsolutePath().equals(files[i].getAbsolutePath())) {
                            files[i].setChecked(mFileArray[i].getChecked());
                        }
                    }
                }
            }
        }
        // ---

        mSortType = sortType;
        mFileArray = files;

        if (forceUpdate) {
            notifyDataSetInvalidated();
        } else {
            notifyDataSetChanged();
        }

        if (observer != null) {
            observer.updateAdapterDone();
        }
    }

    public void updateAdapterResult(){
        mSelectedResult = null;
        mSelectedResult = getSelectedCount();
        mFilesResult = null;
        mFilesResult = getFilesCount();
    }

    public void onClick(View v) {
        int checkPosition;

        // +++ Johnson, don't do anything when user is moving divider
        if (mFragment.isMovingDivider()) {
            switch (v.getId()) {
                case R.id.file_list_item_check:
                    ((CheckBox)v).setChecked(false);
                    break;
            }
            return;
        }
        // ---
        switch (v.getId()) {
            case R.id.file_list_item_check:
                checkPosition = (Integer) v.getTag();
                updateEditItemStatus(checkPosition,((CheckBox) v).isChecked());
                break;
            case R.id.file_list_item_date:
                checkPosition = (Integer) v.getTag();
                mFragment.goToLocation(mFileArray[checkPosition].getParentFile(), false);
                break;
            default:
                break;
        }
    }

    // Reflect function called by action button
    public void onPopupActionButtonClick(View button, int position) {

        // record which file is clicked
        VFile f = null;
        try {
            if(button != null && position > -1){
                f = mFileArray[position];
            }else if (button != null && button.getTag(R.id.file_list_item_icon) != null) {
                    f = mFileArray[(Integer) button.getTag(R.id.file_list_item_icon)];
            }

        } catch (Exception e) {
            Log.w(TAG, "Fail to call onPopupActionButtonClick:" + e.toString());
        }

        final VFile chosenFile = f;
        ItemSelectPopMenuDialog dialog = ItemSelectPopMenuDialog.newInstance(chosenFile);
        dialog.show(mFragment.getFragmentManager(), "menu_dialog");

        // setup popup menu
//        PopupMenu popup = new PopupMenu(button.getContext(), button, Gravity.RIGHT);
//        popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
//        Menu menu = popup.getMenu();
//        if (mFragment.mIsAttachOp || mFragment.mIsMultipleSelectionOp) {
//            menu.findItem(R.id.menu_rename).setVisible(false);
//            menu.findItem(R.id.menu_copy).setVisible(false);
//            menu.findItem(R.id.menu_cut).setVisible(false);
//            menu.findItem(R.id.menu_delete).setVisible(false);
//            menu.findItem(R.id.menu_zip).setVisible(false);
//        }
//
//        menu.findItem(R.id.menu_open).setVisible(false);
//        menu.findItem(R.id.menu_favorite_add).setVisible(false);
//        menu.findItem(R.id.menu_favorite_remove).setVisible(false);
//        menu.findItem(R.id.menu_favorite_rename).setVisible(false);
//
//        boolean isShowInfoOnly = false;
//
//        //show info menu only for /sdcard,/Removable,/stotage
//        if(chosenFile == null || chosenFile.getAbsolutePath() == null){
//            return;
//        }
//        if (chosenFile.getAbsolutePath().equals(WrapEnvironment.getEpadExternalStorageDirectory().getAbsolutePath())
//                 ||chosenFile.getAbsolutePath().equals(WrapEnvironment.getEpadInternalStorageDirectory().getAbsolutePath())
//                 || chosenFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE && ((SambaVFile)chosenFile).getParentPath().equals(SambaFileUtility.getRootScanPath())
//                 || chosenFile.getParent().equals(RemoteFileUtility.getHomeCloudRootPath())) {
//            menu.findItem(R.id.menu_rename).setVisible(false);
//            menu.findItem(R.id.menu_copy).setVisible(false);
//            menu.findItem(R.id.menu_cut).setVisible(false);
//            menu.findItem(R.id.menu_delete).setVisible(false);
//            menu.findItem(R.id.menu_zip).setVisible(false);
//            isShowInfoOnly = true;
//        }
//
//        if(chosenFile.getVFieType() != VFileType.TYPE_LOCAL_STORAGE){
//            menu.findItem(R.id.menu_zip).setVisible(false);
//        }else{
//            if(mFragment.isInCategory())
//                menu.findItem(R.id.menu_zip).setVisible(false);
//
//            if(mFragment.getmIndicatorFile() != null && (mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_IMAGE_FILE) || mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_FAVORITE_FILE))){
//                menu.findItem(R.id.menu_rename).setVisible(false);
//                menu.findItem(R.id.menu_copy).setVisible(false);
//                menu.findItem(R.id.menu_cut).setVisible(false);
//                menu.findItem(R.id.menu_delete).setVisible(false);
//                isShowInfoOnly = true;
//
//                if(mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_FAVORITE_FILE)){
//                    menu.findItem(R.id.menu_open).setVisible(true);
//                    menu.findItem(R.id.menu_favorite_remove).setVisible(true);
//                    menu.findItem(R.id.menu_favorite_rename).setVisible(true);
//                }
//            }
//
//            boolean isFavoriteFolder = chosenFile.isFavoriteFile();
//            menu.findItem(R.id.menu_favorite_add).setVisible(!mFragment.isInCategory() && !isFavoriteFolder && chosenFile.isDirectory());
//            menu.findItem(R.id.menu_favorite_remove).setVisible(isFavoriteFolder);
//        }
//        // check parent file's shared folder settings, if it enable, we don't show the button
//        int shareType = -1;
///*        if (((FileManagerActivity) mFragment.getActivity()).hasWIFIDirect()) {
//            try {
//                if (f.getCanonicalPath().startsWith(DEFAULT_SHARED_PATH)) {
//                    if (mSharedPaths == null) {
//                        mSharedPaths = ShareFiles.getShareFiles(mCr, MsgObj.TYPE_WIFIDIRECT_STORAGE);
//                    }
//                    if (mSharedPaths != null && mShareParentPaths == null) {
//                        mShareParentPaths = getSharedParentPaths(mSharedPaths);
//                    }
//                    if (mSharedPaths != null) {
//                        for (int i=0 ; i<mSharedPaths.length ; i++) {
//                            if (f.getCanonicalPath().equals(mSharedPaths[i])) {
//                                popup.getMenu().findItem(R.id.menu_shared_folder).setTitle(R.string.unshared_folder);
//                                break;
//                            }
//                        }
//                    }
//                    if (mShareParentPaths != null && shareType == -1) {
//                        for (int i=0 ; i<mShareParentPaths.length ; i++) {
//                            if (f.getCanonicalPath().startsWith(mShareParentPaths[i])) {
//                                shareType = MsgObj.TYPE_WIFIDIRECT_STORAGE;
//                                break;
//                            }
//                        }
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        */
//
//        // remote storage file don't support rename action
//        if (f.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
//            popup.getMenu().findItem(R.id.menu_rename).setVisible(false);
//        }
///*
//        try {
//            // support local files have the shared feature at path: /sdcard/
//            // subfolder don't support the shared feature
//            if (mIsAttachOp || mFragment.mIsMultipleSelectionOp) {
//                popup.getMenu().findItem(R.id.menu_shared_folder).setVisible(false);
//            }
//
//            if (!((FileManagerActivity) mFragment.getActivity()).hasWIFIDirect()) {
//                popup.getMenu().findItem(R.id.menu_shared_folder).setVisible(false);
//            }
//
//            if (f.getVFieType() == VFileType.TYPE_REMOTE_STORAGE || !f.getCanonicalPath().startsWith(DEFAULT_SHARED_PATH) ||
//                    shareType == MsgObj.TYPE_WIFIDIRECT_STORAGE) {
//                popup.getMenu().findItem(R.id.menu_shared_folder).setVisible(false);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//*/
//        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//            public boolean onMenuItemClick(MenuItem item) {
//
//                VFile[] choseArray = new VFile[1];
//                choseArray[0] = chosenFile;
//                Activity mActivity = mFragment.getActivity();
//                switch (item.getItemId()) {
//                    case R.id.menu_rename:
//                        if(ItemOperationUtility.isItemContainDrm(choseArray,true,false)){
//                            if(mFragment != null){
//                                if(chosenFile.isFile()){
//                                    ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
//                                }else{
//                                    ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
//                                }
//                            }
//                        }else{
//                            if(SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(chosenFile.getAbsolutePath())){
//                                if(mActivity instanceof FileManagerActivity){
//                                    ((FileManagerActivity)mActivity).callSafChoose(SafOperationUtility.ACTION_RENAME);
//                                }
//                            }else{
//                                mFragment.showDialog(DialogType.TYPE_RENAME_DIALOG, chosenFile);
//                            }
//                        }
//                        return true;
//                    case R.id.menu_info:
//                        mFragment.showDialog(DialogType.TYPE_INFO_DIALOG, chosenFile);
//                        return true;
//                    case R.id.menu_copy:
//                        if(ItemOperationUtility.isItemContainDrm(choseArray,true,false)){
//                            if(mFragment != null){
//                                if(chosenFile.isFile()){
//                                    ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
//                                }else{
//                                    ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
//                                }
//                            }
//                        }else{
//                            mFragment.copyFileInPopup(choseArray,false);
//                        }
//                        return true;
//                    case R.id.menu_cut:
//                        if(ItemOperationUtility.isItemContainDrm(choseArray,true,false)){
//                            if(mFragment != null){
//                                if(chosenFile.isFile()){
//                                    ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
//                                }else{
//                                    ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
//                                }
//                            }
//                        }else{
//                            mFragment.cutFileInPopup(choseArray,false);
//                        }
//                        return true;
//                    case R.id.menu_delete:
//                        mFragment.deleteFileInPopup(choseArray);
//                        return true;
//                    /*case R.id.menu_shared_folder:
//                        // remote storage: shared folder
//                        try {
//                            mUpdateSharedPaths = true;
//                            int shareType = ShareFiles.getShareFile(mCr, chosenFile.getCanonicalPath());
//                            if (shareType != -1) {
//                                ShareFiles.removeShareFile(mCr, chosenFile.getCanonicalPath(), MsgObj.TYPE_WIFIDIRECT_STORAGE);
//                            } else {
//                                ShareFiles.insertShareFile(mCr, chosenFile.getCanonicalPath(), MsgObj.TYPE_WIFIDIRECT_STORAGE);
//                            }
//                            notifyDataSetChanged();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        return true;*/
//                    case R.id.menu_zip:
//                        VFile[] array = new VFile[1];
//                        array[0] = chosenFile;
//                        if(SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(chosenFile.getAbsolutePath())){
//                            if(mActivity instanceof FileManagerActivity){
//                                ((FileManagerActivity)mActivity).callSafChoose(SafOperationUtility.ACTION_ZIP);
//                            }
//                        }else{
//                            ZipDialogFragment.showZipDialog(mFragment, array, false);
//                        }
//                        return true;
//                    case R.id.menu_favorite_add:
//                        mFragment.addFavoriteFile(chosenFile);
//                        return true;
//                    case R.id.menu_favorite_remove:
//                        mFragment.removeFavoriteFile(choseArray, false);
//                        return true;
//                    case R.id.menu_open:
//                        mFragment.startScanFile(chosenFile, ScanType.SCAN_CHILD);
//                        return true;
//                    case R.id.menu_favorite_rename:
//                        mFragment.renameFavoriteFile(chosenFile);
//                        return true;
//                    default:
//                        return false;
//                }
//            }
//        });
//
//        VFile[] choseArray = new VFile[1];
//        choseArray[0] = chosenFile;
//        if(!isShowInfoOnly && ItemOperationUtility.isItemContainDrm(choseArray,true,false)){
//            if(mFragment != null){
//                ItemOperationUtility.disableItemMenu(menu.findItem(R.id.menu_cut),mFragment.getString(R.string.cut));
//                ItemOperationUtility.disableItemMenu(menu.findItem(R.id.menu_rename),mFragment.getString(R.string.rename));
//                ItemOperationUtility.disableItemMenu(menu.findItem(R.id.menu_copy),mFragment.getString(R.string.copy));
//            }
//        }
//        popup.show();
//
//        if(mFragment != null && mFragment.getActivity() != null && (mFragment.getActivity() instanceof FileManagerActivity)){
//            ((FileManagerActivity)mFragment.getActivity()).setPopupMenu(popup);
//        }
    }

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

    public void clearItemsSelected() {
        if (mFileArray != null) {
            for (int i = 0; i < mFileArray.length; i++) {
                mFileArray[i].setChecked(false);
            }
            mSelectedResult.count = 0;
            mSelectedResult.hasDir =false;
            mSelectedResult.dircount = 0;
        }
    }

    @Override
    public void setShoppingCart(ShoppingCart shoppingCart) {
        mShoppingCart = shoppingCart;
        if (mFileArray != null && mShoppingCart != null) {
            for (VFile vFile : mFileArray) {
                if (vFile.getChecked()) {
                    mShoppingCart.addVFile(vFile);
                }
            }
        }
    }

    @Override
    public void syncWithShoppingCart() {
        if (mFileArray != null && mShoppingCart != null) {
            for (VFile vFile : mFileArray) {
                if (mShoppingCart.contains(vFile)) {
                    vFile.setChecked(true);
                } else {
                    vFile.setChecked(false);
                }
            }
        }
    }

    public void setSelectAll() {
        if (mFileArray != null) {
            for (int i = 0; i < mFileArray.length; i++) {
                if(!mFragment.mIsMultipleSelectionOp || !mFileArray[i].isDirectory()){
                    mFileArray[i].setChecked(true);
                    mShoppingCart.addVFile(mFileArray[i]);
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
        }

        notifyDataSetInvalidated();
    }

    // +++ Johnson
    public void isHiddenDate(boolean hidden) {
        mIsHiddenDate = hidden;
    }

    // This is tentative drag & drop UI
    private static class ShadowBuilder extends DragShadowBuilder {
        private static Drawable sBackground;
        /** Paint information for the move message text */
        private static TextPaint sMessagePaint;
        /** Paint information for the message count */
        private static TextPaint sCountPaint;
        /** The x location of any touch event; used to ensure the drag overlay is drawn correctly */
        private static int sTouchX;

        /** Width of the draggable view */
        private final int mDragWidth;
        /** Height of the draggable view */
        private final int mDragHeight;

        private String mMessageText;
        private PointF mMessagePoint;

        private String mCountText;
        private PointF mCountPoint;
        private int mOldOrientation = Configuration.ORIENTATION_UNDEFINED;

        /** Margin applied to the right of count text */
        private static float sCountMargin;
        /** Margin applied to left of the message text */
        private static float sMessageMargin;
        /** Vertical offset of the drag view */
        private static int sDragOffset;

        private Bitmap mBitmap;
        private int mIconHeight;
        private int mIconWidth;
        private int mIcon_x_margin;
        private int mIcon_y_margin;
        private int mCount_x_margin;
        private int mCount_y_margin;
        private int mMessage_y_margin;
        private static int EXTRA_ICON_WIDTH = 13;

        public ShadowBuilder(View view, int count) {
//            super(view);
            Resources res = view.getResources();
            int newOrientation = res.getConfiguration().orientation;

            mDragWidth = view.getWidth();
            mDragHeight = (int)res.getDimension(R.dimen.drag_items_shadow_height); //view.getHeight();

            // TODO: Can we define a layout for the contents of the drag area?
            if (sBackground == null || mOldOrientation != newOrientation) {
                mOldOrientation = newOrientation;

                sBackground = res.getDrawable(R.drawable.asus_ep_filemanage_select_move_bg);
                sBackground.setBounds(0, 0, mDragWidth, mDragHeight);

                sDragOffset = (int)res.getDimension(R.dimen.message_list_drag_offset);

                sMessagePaint = new TextPaint();
                float messageTextSize;
                messageTextSize = res.getDimension(R.dimen.message_list_drag_message_font_size);
                sMessagePaint.setTextSize(messageTextSize);
                sMessagePaint.setTypeface(Typeface.DEFAULT_BOLD);
                sMessagePaint.setAntiAlias(true);
                sMessageMargin = res.getDimension(R.dimen.message_list_drag_message_right_margin);

                sCountPaint = new TextPaint();
                float countTextSize;
                countTextSize = res.getDimension(R.dimen.message_list_drag_count_font_size);
                sCountPaint.setTextSize(countTextSize);
                sCountPaint.setTypeface(Typeface.DEFAULT_BOLD);
                sCountPaint.setAntiAlias(true);
                sCountPaint.setColor(Color.WHITE);
                sCountMargin = res.getDimension(R.dimen.message_list_drag_count_left_margin);
            }

            // Calculate layout positions
            Rect b = new Rect();

            if (count <= 1) {
                mMessageText = res.getString(R.string.drag_items_one);
            }
            else {
                mMessageText = res.getString(R.string.drag_items_other);
            }

            sMessagePaint.getTextBounds(mMessageText, 0, mMessageText.length(), b);
            mMessagePoint = new PointF(mDragWidth - b.right - sMessageMargin,
                    (mDragHeight - b.top)/ 2);

            mCountText = Integer.toString(count);
            sCountPaint.getTextBounds(mCountText, 0, mCountText.length(), b);
            mCountPoint = new PointF(sCountMargin,
                    (mDragHeight - b.top) / 2);

            mIconHeight = (int)res.getDimension(R.dimen.drag_items_hint_icon_height);
            mIconWidth = (int)res.getDimension(R.dimen.drag_items_hint_icon_width);
            mIcon_x_margin = (int)res.getDimension(R.dimen.drag_items_hint_icon_x_margin);
            mIcon_y_margin = (int)res.getDimension(R.dimen.drag_items_hint_icon_y_margin);
            mCount_x_margin = (int)res.getDimension(R.dimen.drag_items_hint_count_x_margin);
            mCount_y_margin = (int)res.getDimension(R.dimen.drag_items_hint_count_y_margin);
            mMessage_y_margin = (int)res.getDimension(R.dimen.drag_items_hint_message_y_margin);

            // tune icon width
            for (int i=1 ; i<mCountText.length() ; i++) {
                mIconWidth += EXTRA_ICON_WIDTH;
            }
            mBitmap = get_ninepatch(R.drawable.asus_ep_filemanage_select_move_n, mIconWidth, mIconHeight, view);

        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            shadowSize.set(mDragWidth, mDragHeight);
            shadowTouchPoint.set(sTouchX, (mDragHeight / 2) + sDragOffset);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            super.onDrawShadow(canvas);
            sBackground.draw(canvas);
            canvas.drawText(mMessageText, mMessagePoint.x, mMessage_y_margin, sMessagePaint);
            canvas.drawBitmap(mBitmap, mIcon_x_margin, mIcon_y_margin, null);
            canvas.drawText(mCountText, mCount_x_margin, mCount_y_margin, sCountPaint);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        FileManagerActivity mFMActivity = (FileManagerActivity)mFragment.getActivity();
        if(mFMActivity == null){
            return false;
        }
        boolean isPadMode = mFMActivity.isPadMode();
        if(isPadMode){
            mFMActivity.searchViewIconified(true);
        }else{
            FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
            fileListFragment.collapseSearchView();
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mTouchX = event.getX();
        }

        if (v.getId() == R.id.file_list_item_date) {
            TextView t = (TextView) v;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                t.setBackgroundColor(Color.rgb(0xd1, 0xd1, 0xd1));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                t.setBackgroundDrawable(null);
            }
        }
        return false;
    }
    // ---

    private static Bitmap get_ninepatch(int id, int width, int height, View v) {
        Bitmap bitmap = BitmapFactory.decodeResource(v.getResources(), id);
        byte[] chunk = bitmap.getNinePatchChunk();
        NinePatchDrawable np_drawable = new NinePatchDrawable(bitmap, chunk, new Rect(), null);
        np_drawable.setBounds(0, 0, width, height);

        Bitmap output_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output_bitmap);
        np_drawable.draw(canvas);

        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        return output_bitmap;
    }

    public void onDrop(int position) {
        if (DEBUG) {
            Log.d(TAG, "onDrop:get Postion :" + position);
        }
        FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
        if (mFileArray == null || (position < 0 && position >= mFileArray.length) || mFileArray[position] == null) {
            Log.w(TAG, "mFileArray == null || mFileArray[checkPosition] == null when calling onDrop");
        } else if (mFileArray[position].isDirectory() && !mFileArray[position].getChecked()) {
            VFile file = mFileArray[position];
            // check the target folder and the files of EditPool
            VFile[] files = fileListFragment.getEditPoolFiles();
            if (files != null) {
                String dst = file.getAbsolutePath();
                // for remote storage
                if (file.getVFieType() != VFileType.TYPE_LOCAL_STORAGE &&
                        (files != null && files.length > 0 && files[0].getVFieType() != VFileType.TYPE_LOCAL_STORAGE)) {
                    Toast.makeText(mFragment.getActivity(), R.string.drop_incorrect_hint, Toast.LENGTH_SHORT).show();
                    return;
                }
                int dropAction = DropDialogFragment.DROP_ACTION_ALL;
                for (int i=0 ; i<files.length ; i++) {
                    String srcParent = files[i].getParentFile().getAbsolutePath();
                    String src = files[i].getAbsolutePath();
                    if (dst.equals(srcParent)) {
                        // now we don't do anything
                        break;
                    } else if (dst.equals(src) || (src != null && dst.startsWith(src))) {
                        Toast.makeText(mFragment.getActivity(), R.string.drop_incorrect_hint, Toast.LENGTH_SHORT).show();
                        fileListFragment.onDropSelectedItems();
                        break;
                    } else if (i == files.length - 1){
                        // +++ for remote storage copy case
                        if (file.getVFieType() == VFileType.TYPE_REMOTE_STORAGE || files[i].getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
                            dropAction = DropDialogFragment.DROP_ACTION_COPY_ONLY;
                        }
                        // ---
                        DropDialogFragment dropDialog = DropDialogFragment.newInstance(file, dropAction);
                        dropDialog.show(mFragment.getFragmentManager(), "DropDialogFragment");
                    }
                }
            }
        }
    }

    private String[] getSharedParentPaths(String[] paths) {
        int count = 0;
        for (int i=0 ; i<paths.length ; i++) {
            if (paths[i].contains(".")) {
                count++;
            }
        }
        String[] temp = new String[paths.length - count];
        count = 0;
        for (int i=0 ; i<paths.length ; i++) {
            if (!paths[i].contains(".")) {
                temp[count] = paths[i]+File.separator;
                count++;
            }
        }
        return temp;
    }

    public void clearCacheTag() {
        mPopupBuilder.clearCacheTag();
    }

    @Override
    public boolean onHover(View v, MotionEvent event) {
       // if (PenSettings.isAirViewInfoPreviewEnabled(v.getContext())){

        Object tag = v.getTag();
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
                int position = (Integer) tag;
                if(mFileArray != null && position < mFileArray.length){
                    mPopupBuilder.hoverEnter(event, tag, mFileArray[position], v);
                }

               break;
           case MotionEvent.ACTION_HOVER_MOVE:
              mPopupBuilder.hoverMove(event, tag);
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                mPopupBuilder.hoverExit(event, tag);
                break;
        }
      // }
       return true;
    }

    private class PopupBuilder {
        private Context mContext;
        private Object mCacheTag;
        private VFile mPopupFile;
        private int mPopupType;
        private View mView;

        private LinearLayout mPopupInfo = null;
        private ItemInfoView mItemInfoView = null;
        private ItemIcon mItemIcon = null;
        private Drawable mInfoBackground = null;
       private  Drawable mInfoBackground_image = null;
        public static final int POP_INFO = 0;
        public static final int POP_IMAGE = 1;

        private float mCacheX, mCacheY;
        private float enterX;

        private static final int MSG_POPUP_WINDOW = 0;

        private static final int MOVE_THREASHOLD = 15;
        private static final long POPUP_DELAY_MS = 200;

        private float mScale;
        private FileListPopupWindow mPopupWindow = null;

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case MSG_POPUP_WINDOW:
                        openPopup(mPopupFile, (int)mCacheX, (int)mCacheY, mPopupType, mCacheTag, mView);
                        break;
                }
            }
        };

        public PopupBuilder(Context ctx) {
            mContext = ctx;
            mPopupWindow = new FileListPopupWindow(ctx);
            mPopupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopupWindow.setTouchable(false);//Do not get any touch event
            mPopupWindow.setClippingEnabled(false);

            mItemIcon = new ItemIcon(ctx, true);
           mInfoBackground = ctx.getResources().getDrawable(R.drawable.asus_airview_photo_with_text_board);
           mInfoBackground_image = ctx.getResources().getDrawable(R.drawable.asus_airview_photo_with_text_photomask);
            mItemInfoView = new ItemInfoView();
            mPopupInfo = (LinearLayout) LayoutInflater.from(ctx).inflate(R.layout.popup_info, null);
            mPopupWindow.setClippingEnabled(true);
            mPopupWindow.setBackgroundDrawable(mInfoBackground);
            mScale = mContext.getResources().getDisplayMetrics().density;
        }

//++Felix_Zhang
        public Bitmap getRoundedCornerBitmap(Bitmap bitmap,float roundPx){

               Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                               .getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
               Canvas canvas = new Canvas(output);
            final int width = bitmap.getWidth();
            final int height = bitmap.getHeight();
               final int color = 0xff424242;
               final Paint paint = new Paint();

               paint.setAntiAlias(true);
               canvas.drawARGB(0, 0, 0, 0);
               paint.setColor(color);
            final Rect block = new Rect(0, (int)roundPx, width, height);
            canvas.drawRect(block, paint);
            final RectF rectF = new RectF(0, 0,  width , roundPx * 2);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

               paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            final Rect src = new Rect(0, 0, width, height);
            final Rect dst = src;
            canvas.drawBitmap(bitmap, src, dst, paint);
               if (bitmap!=null) {
                       bitmap.recycle();
                       bitmap=null;
               }
               return output;
         }


        private void openPopup(final VFile file, final int x, final int y, final int popMode, final Object tag, final View parent) {
            InfoCallback callback;
            if (popMode == POP_INFO) {
                callback = new InfoCallback() {

                    @Override
                    public void onGetInfo(Object info) {
                        //ItemInfoView will set FileInfo as info
                        if (mCacheTag != null && mCacheTag.equals(tag)) {
                            FileInfo fileInfo = (FileInfo) info;
                            final LinearLayout mPopupInfo = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.popup_info, null);

                            TextView size = (TextView) mPopupInfo.findViewById(R.id.info_file_size);
                            TextView num = (TextView) mPopupInfo.findViewById(R.id.info_file_num);
                            TextView fileName = (TextView) mPopupInfo.findViewById(R.id.info_file_name);
                            LinearLayout fileNumContainer = (LinearLayout) mPopupInfo.findViewById(R.id.file_num_container);
                            LinearLayout fileTypeContainer = (LinearLayout) mPopupInfo.findViewById(R.id.file_type_container);
                            fileName.setText(file.getName());
                            String sizeText = FileUtility.bytes2String(mContext, fileInfo.numSize, 2);
                            size.setText((fileInfo.numSize==0)?"0B":sizeText);
                            num.setText(String.valueOf(fileInfo.numFiles));
                            if (file.isDirectory()) {
                                fileNumContainer.setVisibility(View.VISIBLE);
                                fileTypeContainer.setVisibility(View.GONE);
                            } else {
                                int typeRes = MimeMapUtility.getTypeRes(file);
                                TextView fileType = (TextView) mPopupInfo.findViewById(R.id.info_file_type);
                                fileType.setText(typeRes);
                                fileNumContainer.setVisibility(View.GONE);
                                fileTypeContainer.setVisibility(View.VISIBLE);
                            }
                            mPopupWindow.dismiss();
                            mPopupWindow.setBackgroundDrawable(mInfoBackground);
                            mPopupWindow.setContentView(mPopupInfo);
                            mPopupWindow.showAtDropDown(parent, popMode,enterX);
                        }
                    }
                };
                mItemInfoView.setInfoView(file, true, callback);
                mPopupWindow.preparePopup(true);
                if (DEBUG) {
                    Log.d(TAG, "openPopup - popup info");
                }
            } else if (popMode == POP_IMAGE) {
                final LinearLayout popupImageContainer = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.popup_image, null);
                final ImageView popupImage = (ImageView) popupImageContainer.findViewById(R.id.preview);
                callback = new InfoCallback() {

                    @Override
                    public void onGetInfo(Object info) {
                        // ItemIcon will set bitmap as info
                        if (mCacheTag != null && mCacheTag.equals(tag)) {
                            Bitmap bp = (Bitmap) info;
                            int bound_minWidth = (int)((isPadMode?210:163)*mScale+0.5f);
                            int bound_minHeight = (int)((isPadMode?159:121)*mScale+0.5f);

                            int bound_PortHeight = (int)((isPadMode?297:206)*mScale+0.5f);
                            int bound_PortWidth = (int)((isPadMode?220:151)*mScale+0.5f);
                            int bound_landWidth = (int)((isPadMode?270:200)*mScale+0.5f);

                            int bit_height = bp.getHeight();
                            int bit_width = bp.getWidth();
                            if (bit_width <= bound_minWidth && bit_height <= bound_minHeight){
                                popupImage.setLayoutParams(new LinearLayout.LayoutParams(bit_width,bit_height));
                                popupImage.setImageBitmap(bp);
                            }else{
                               if (bit_height < bit_width){
                                       int new_h = (int)(((bound_landWidth*bit_height)/bit_width));
                                       popupImage.setLayoutParams(new LinearLayout.LayoutParams(bound_landWidth,new_h>0?new_h:1));
                                       popupImage.setImageBitmap(getRoundedCornerBitmap(bp,6f));
                               }else{
                                       int  new_w = (int)(((bound_PortHeight*bit_width)/bit_height));
                                       //new_w = new_w>bound_PortWidth?new_w:bound_PortWidth;
                                       popupImage.setLayoutParams(new LinearLayout.LayoutParams(new_w>0?new_w:1,bound_PortHeight));
                                       if (new_w<bound_PortWidth) {
                                           popupImage.setImageBitmap(bp);
                                       }else {
                                           popupImage.setImageBitmap(getRoundedCornerBitmap(bp,6f));
                                       }
                               }
                            }

                            //popupImage.setImageBitmap(bp);
                            popupImage.setScaleType(ImageView.ScaleType.FIT_XY);
                            mPopupWindow.dismiss();
                            mPopupWindow.setBackgroundDrawable(mInfoBackground);
                            mPopupWindow.setContentView(popupImageContainer);
                            TextView size = (TextView) popupImageContainer.findViewById(R.id.info_file_size);
                            TextView fileName = (TextView) popupImageContainer.findViewById(R.id.info_file_name);
                            TextView fileType = (TextView) popupImageContainer.findViewById(R.id.info_file_type);
                            int typeRes = MimeMapUtility.getTypeRes(file);
                            fileName.setText(file.getName());
                            size.setText(FileUtility.bytes2String(mContext, file.length(), 2));
                            fileType.setText(typeRes);
                            mPopupWindow.showAtDropDown(parent, popMode,enterX);
                        }
                    }
                };
                mItemIcon.setResizedIcon(file, popupImage, 1024, callback);
                mPopupWindow.preparePopup(true);
                if (DEBUG) {
                    Log.d(TAG, "openPopup - popup image");
                }
            }
        }

        public void hoverEnter(MotionEvent event, Object tag, VFile file, View anchor) {
            int fileType = MimeMapUtility.getFileType(file);
            int popupType;
            if (fileType == FileType.TYPE_IMAGE) {
                popupType = POP_IMAGE;
            } else {
                popupType = POP_INFO;
            }
            hoverEnter(event, tag, file, popupType, anchor);
        }

        public void hoverEnter(MotionEvent event, Object tag, VFile file, int type, View anchor) {
            mCacheTag = tag;
            mPopupFile = file;
            mPopupType = type;
            this.mView = anchor;
            enterX = event.getRawX();
            mCacheX = event.getRawX();
            mCacheY = event.getRawY();
            mPopupWindow.dismiss();
            mHandler.removeMessages(MSG_POPUP_WINDOW);
            mHandler.sendEmptyMessageDelayed(MSG_POPUP_WINDOW, POPUP_DELAY_MS);
            if (DEBUG) {
                Log.d(TAG, "hoverEnter - tag = " + tag);
            }
        }

        public void hoverMove(MotionEvent event, Object tag) {
            if (mCacheTag != null && mCacheTag.equals(tag) && !mPopupWindow.isShowing() && !mPopupWindow.isPrepareing()) {
                if((Math.abs(mCacheX - event.getRawX()) + Math.abs(mCacheY - event.getRawY())) > MOVE_THREASHOLD) {
                    mHandler.removeMessages(MSG_POPUP_WINDOW);
                    mCacheX = event.getRawX();
                    mCacheY = event.getRawY();
                    mHandler.sendEmptyMessageDelayed(MSG_POPUP_WINDOW, POPUP_DELAY_MS);
                    if (DEBUG) {
                        Log.d(TAG, "hoverMove - tag = " + tag);
                    }
                }
            }
            mPopupWindow.guidLineOnMOve(event);
        }

        public void hoverExit(MotionEvent event, Object tag) {
            if (mCacheTag != null && mCacheTag.equals(tag)) {
                mHandler.removeMessages(MSG_POPUP_WINDOW);
                mPopupWindow.dismiss();
                if (DEBUG) {
                    Log.d(TAG, "hoverExit - tag = " + tag);
                }
            }
        }

        public void clearCacheTag() {
            mCacheTag = null;
            mPopupWindow.dismiss();
            mHandler.removeMessages(MSG_POPUP_WINDOW);
            mPopupWindow.preparePopup(false);
            if (DEBUG) {
                Log.d(TAG, "clearCacheTag - mCacheTag = " + mCacheTag);
            }
        }

        private class FileListPopupWindow extends PopupWindow {
            private GuildLinePopup guidLinePopup;
            private Context mContext;
            private int xOffset, yOffset, yOffset_padding, airview_xmargin_left, airview_xmargin_land_left;
            private boolean isPreparePopup = false;
            private int orientation;
            private boolean isPort=true;
            private int statusBarHeight = 0;

            public FileListPopupWindow(Context ctx) {
                super(ctx);
                mContext = ctx;
                xOffset = mContext.getResources().getDimensionPixelSize(R.dimen.airview_xoffset);
                yOffset = mContext.getResources().getDimensionPixelSize(R.dimen.airview_yoffset);
                yOffset_padding = mContext.getResources().getDimensionPixelSize(R.dimen.airview_yoffset_padding);
                airview_xmargin_left = mContext.getResources().getDimensionPixelSize(R.dimen.airview_xmargin_left);
                airview_xmargin_left = mContext.getResources().getDimensionPixelSize(R.dimen.airview_xmargin_land_left);
                guidLinePopup= new GuildLinePopup(mContext);
                isPort=((mContext.getResources().getConfiguration().orientation)==Configuration.ORIENTATION_PORTRAIT);
                statusBarHeight = ((FileManagerActivity)mContext).getStatusBarH();
            }

            public void preparePopup ( boolean prepared) {
                isPreparePopup = prepared;
            }

            public boolean isPrepareing() {
                return isPreparePopup;
            }

            private void showGuideLinePopup(View v,int x,int y){
               guidLinePopup.setAnchorView(v);
               guidLinePopup.setPilotPoint(x,y);
               guidLinePopup.preparePopup();
               guidLinePopup.setLineVisible(true);
               guidLinePopup.showPopup();
            }


        //++Felix_Zhang make the left name align to the left
        private int dynamicSetTextViewWidth(View mPopupInfo){
            boolean isdirectory = false;
            int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

            LinearLayout  fileNumContainer = (LinearLayout) mPopupInfo.findViewById(R.id.file_num_container);
            if (fileNumContainer==null||fileNumContainer.getVisibility()==View.GONE) {
                 isdirectory = false;
            }else {
                 isdirectory = true;
            }
            TextView showSize = (TextView) mPopupInfo.findViewById(R.id.show_info_file_size);
            TextView showName = (TextView) mPopupInfo.findViewById(R.id.show_info_file_name);
            TextView showNum = null;
            int setWidth = 0;
            if (isdirectory) {
                showNum = (TextView) mPopupInfo.findViewById(R.id.show_info_file_num);
             }else {
                 showNum = (TextView) mPopupInfo.findViewById(R.id.show_info_file_type);
             }
             showSize.measure(spec, spec);
             showName.measure(spec, spec);
             showNum.measure(spec, spec);
             setWidth =Math.max(Math.max(showSize.getMeasuredWidth(), showName.getMeasuredWidth()), showNum.getMeasuredWidth()) ;

             showSize.setWidth(setWidth);
             showName.setWidth(setWidth);
             showNum.setWidth(setWidth);
             return setWidth;
        }

        private int dynamicSetTextViewWidth2(View mPopupInfo,int width){
              boolean isdirectory = false;

              LinearLayout  fileNumContainer = (LinearLayout) mPopupInfo.findViewById(R.id.file_num_container);
              if (fileNumContainer==null||fileNumContainer.getVisibility()==View.GONE) {
                    isdirectory = false;
              }else {
                    isdirectory = true;
              }
              TextView showSize = (TextView) mPopupInfo.findViewById(R.id.show_info_file_size);
              TextView showName = (TextView) mPopupInfo.findViewById(R.id.show_info_file_name);
              TextView showNum = null;
              int setWidth = 0;
              if (isdirectory) {
                    showNum = (TextView) mPopupInfo.findViewById(R.id.show_info_file_num);
              }else {
                    showNum = (TextView) mPopupInfo.findViewById(R.id.show_info_file_type);
              }
              showSize.setWidth(width);
              showName.setWidth(width);
              showNum.setWidth(width);
              return setWidth;

        }

        public void showAtDropDown(View anchor, int popMode,float enterX) {

            View contentView = getContentView();
            Drawable backgroundDrawable = getBackground();

            Rect paddings = new Rect();
            backgroundDrawable.getPadding(paddings);
                    //int largeTextViewW  = dynamicSetTextViewWidth(contentView);
            int width, height,newWidth,newHeight;
            int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            contentView.measure(spec, spec);
            width = contentView.getMeasuredWidth() + paddings.left + paddings.right;
            height = contentView.getMeasuredHeight() + paddings.top + paddings.bottom;
            int bound_PortWidth = (int)((isPadMode?220:151)*mScale+0.5f);
            int bound_landHeight = (int)((isPadMode?201:147)*mScale+0.5f);
            if (popMode == POP_IMAGE) {
                LinearLayout popupIMgContent = (LinearLayout) contentView.findViewById(R.id.popup_content_img);
                int imgHeight =((ImageView) popupIMgContent.findViewById(R.id.preview)).getMeasuredHeight();
                int imgWidth =((ImageView) popupIMgContent.findViewById(R.id.preview)).getMeasuredWidth();

                if (imgWidth<bound_PortWidth) {
                    imgWidth = bound_PortWidth;
                        }
                popupIMgContent.setLayoutParams(new LinearLayout.LayoutParams(imgWidth,imgHeight));
                    }
            setWidth(width);
            setHeight(height);

            final Rect displayFrame = new Rect();
            anchor.getWindowVisibleDisplayFrame(displayFrame);

            int anchorHeight = anchor.getMeasuredHeight();
            int anchorWidth = anchor.getMeasuredWidth();
            int locationOfAnchor[] = new int[2];
            anchor.getLocationInWindow(locationOfAnchor);
            int positionX, positionY,guidePopupX,guidePopupY;
            positionX = locationOfAnchor[0] - xOffset + 2*yOffset_padding;
            positionY = locationOfAnchor[1] + anchorHeight - yOffset;


            isPort=((mContext.getResources().getConfiguration().orientation)==Configuration.ORIENTATION_PORTRAIT);
            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            int windowWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
              int newContentWidth = windowWidth-locationOfAnchor[0]-paddings.left - paddings.right  - (isPort?airview_xmargin_left:(airview_xmargin_land_left+airview_xmargin_left)) -contentView.getPaddingLeft()-contentView.getPaddingRight()+ 2*yOffset_padding;
            if (width > newContentWidth||(popMode==POP_INFO)){
                LinearLayout popupContent = (LinearLayout) contentView.findViewById(R.id.popup_content);
                int newContentHeight = height - paddings.top - paddings.bottom-contentView.getPaddingTop()-contentView.getPaddingBottom();
                popupContent.setLayoutParams(new LinearLayout.LayoutParams(newContentWidth,newContentHeight));
                dismiss();
                setContentView(contentView);
            }
            contentView.measure(spec, spec);
            newWidth = contentView.getMeasuredWidth() + paddings.left + paddings.right;
            newHeight = contentView.getMeasuredHeight() + paddings.top + paddings.bottom;
            setWidth(newWidth);
            setHeight(newHeight);

            // If the airview is to be shown beyond the bottom of the window, show the airview
            // on top of the anchor view item.
            // ++ felixz_zhang if zhe popupWindow outside the window ,scrooll the listView to adapte the popupWindow
            // If the airview is to be shown beyond the bottom of the window, show the airview
            // on top of the anchor view item.
            dynamicSetTextViewWidth2(contentView,getWidth()/3);
            if (positionY  + height   > displayFrame.bottom) {
                          positionY = locationOfAnchor[1]  - height + yOffset - 2*yOffset_padding ;
                if (positionY - yOffset+yOffset_padding <= 0 ) {
                        ListView mListView = mFragment.getListView();
                        int locationOfListView[] = new int[2];
                        mListView.getLocationInWindow(locationOfListView);
                        int listViewPositionY = locationOfListView[1];
                        positionY = listViewPositionY - yOffset_padding;
                }
                //if (popMode == POP_INFO) {
                //positionY -= 2*yOffset_padding;
                          //}
                guidePopupY = getGuidePopupY(positionY, this.getHeight(), 2,popMode,screenHeight);
                //positionY+=((popMode==POP_IMAGE)?statusBarHeight/2 : statusBarHeight);
            }else {
                          positionY = positionY + 2 * yOffset_padding;
                      guidePopupY = getGuidePopupY(positionY, this.getHeight(), 1,popMode,screenHeight);
                    }
            guidePopupX = getGuidePopupX(positionX, this.getWidth(),popMode,windowWidth);
            //++ felix_Zhang

            if (!isPort&&popMode==POP_IMAGE) {
                          positionX = positionX + newContentWidth/2 - this.getWidth()/2;

                            positionY = screenHeight/2 -this.getHeight()/2;
                            guidePopupY=screenHeight/2;
                            if (enterX <=windowWidth/2) {
                                  guidePopupX=positionX+yOffset_padding;
                            }else {
                                  guidePopupX=positionX-yOffset_padding+this.getWidth();
                            }
                    }
                showAtLocation(anchor, Gravity.NO_GRAVITY, positionX, positionY);
                showGuideLinePopup(anchor, guidePopupX, guidePopupY);
        }

        //++Felix_Zhang  get the guidePopupWin start Location
        private int getGuidePopupX(int positionX,int width,int mode,int screenWidth){
            if (!isPort&&(POP_IMAGE==mode)) {
                          return (screenWidth - this.getWidth())/2;
                    }
                      return positionX  + (width/2);
        }

        private int getGuidePopupY(int positionY,int height,int type,int mode,int screenHeight){
            //bottom
            if (!isPort&&(POP_IMAGE==mode)) {
                          return (screenHeight - this.getHeight())/2;
                    }
            if (type == 1) {
                          return positionY  + yOffset_padding ;
                    }else if (type == 2) {//up
                          return positionY + height - yOffset_padding ;
                    }
            return 0;
        }

        public void guidLineOnMOve(MotionEvent event){
            if(guidLinePopup!=null) {
                guidLinePopup.hover(event);
                    }
        }

        @Override
        public void dismiss() {
            // TODO Auto-generated method stub
            guidLinePopup.dismissPopup();
            super.dismiss();
        }
    }
  }

    @Override
    public void onItemClick(AdapterView<?> AdapterView, View view, int position, long arg3) {
        // TODO Auto-generated method stub
        int checkPosition;

        switch (AdapterView.getId()) {
        //case R.id.file_list_item_container:
           case android.R.id.list:
              if(mTmpSwipeitemView != null && !mFragment.isItemMoving()){
                   mTmpSwipeitemView.setBackgroundColor(view.getResources().getColor(android.R.color.transparent));
                   mTmpSwipeitemView = null;
               }


            checkPosition = position;

            if (mFileArray == null || checkPosition >= mFileArray.length || mFileArray[checkPosition] == null) {
                Log.w(TAG, "mFileArray == null || mFileArray[checkPosition] == null");
            }else if(mFragment.isInEditMode() && !mFileArray[checkPosition].isDirectory()) {
                if (!mFragment.isInSecondLayerEditMode()) {
                    updateEditItemStatus(checkPosition,!mFileArray[checkPosition].getChecked());
                }
            } else if (mFileArray[checkPosition].isFavoriteFile() && !mFileArray[checkPosition].exists()) {
                ToastUtility.show(mFragment.getActivity(), R.string.linked_file_missing);
            }else if (mFileArray[checkPosition].isDirectory()) {
                 ItemOperationUtility.getInstance().setLastViewPosition(mFragment.getListView());

                 if(mFragment.getmIndicatorFile() != null && (mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_IMAGE_FILE)
                         || mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_MUSIC_FILE)
                         || mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_VIDEO_FILE)
                 )){
                     //scan category image
                     if(mFileArray[checkPosition].getBucketId() != 0){
                         LocalVFile imageFolder= new LocalVFile(mFragment.getmIndicatorFile().getAbsolutePath() + File.separator + mFileArray[checkPosition].getName(), VFileType.TYPE_CATEGORY_STORAGE);
                         imageFolder.setBucketId(mFileArray[checkPosition].getBucketId());
                         mFragment.startScanAlbums(imageFolder);
                         return;
                     }
                 }

                 if (mFileArray[checkPosition].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {

                     if(!((FileManagerApplication)mFragment.getActivity().getApplication()).isNetworkAvailable()) {
                        ((FileManagerActivity)mFragment.getActivity()).displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
//                         isFromClickDrawerClosed = true;
//                         mActivity.closeStorageDrawerList();
                         return;
                     }

                     RemoteVFile tempVFile = (RemoteVFile)mFileArray[checkPosition];
                     tempVFile.setFromFileListItenClick(true);
                    // RecentFileUtil.saveVfile(tempVFile, mFragment.getActivity());
                     mFragment.startScanFile(tempVFile, ScanType.SCAN_CHILD);
                 }else{
                     mFragment.startScanFile(mFileArray[checkPosition], ScanType.SCAN_CHILD);
                 }
            } else {
                if (mFileArray[checkPosition].getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
                    //mFragment.onDeselectAll();
//                    if (mFileArray[checkPosition].getName().toLowerCase().endsWith(".zip") && !mIsAttachOp && !mFragment.mIsMultipleSelectionOp) {
//                        VFile zipFile = mFileArray[checkPosition];
//                        String unZipName = zipFile.getNameNoExtension();
//                        long unZipSize = 0;
//                        String encode = ItemOperationUtility.getInstance().getSuitableEncoding(mFragment.getActivity());//mFragment.getString(R.string.default_encoding);
//                        String uriString = null;
//                        UnZipData unZipData = new UnZipData(zipFile, unZipName, unZipSize, encode, uriString);
//                        mFragment.showDialog(DialogType.TYPE_UNZIP_PREVIEW_DIALOG, unZipData);
//                    } else {

                        boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mFragment.getActivity()).isNeedToWriteSdToAppFolder(mFileArray[checkPosition].getAbsolutePath());
                        if((mFileArray[checkPosition].getName().toLowerCase().endsWith(".zip") || mFileArray[checkPosition].getName().toLowerCase().endsWith(".rar") )
                            && bNeedWriteToAppFolder){
                            WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                                .newInstance();
                            warnDialog.show(mFragment.getActivity().getFragmentManager(),
                                "WarnKKSDPermissionDialogFragment");
                        }else if((mFileArray[checkPosition].getName().toLowerCase().endsWith(".zip") || mFileArray[checkPosition].getName().toLowerCase().endsWith(".rar") )
                            && SafOperationUtility.getInstance(mFragment.getActivity()).isNeedToShowSafDialog(mFileArray[checkPosition].getAbsolutePath())){
                            ((FileManagerActivity)mFragment.getActivity()).callSafChoose(SafOperationUtility.ACTION_EXTRACT);
                        }else{
                            FileUtility.openFile(mFragment.getActivity(), mFileArray[checkPosition], mIsAttachOp, false, true, mFragment.isCategoryRecent());
                        }
//                    }
                } else if (mFileArray[checkPosition].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {

                    if(!((FileManagerApplication)mFragment.getActivity().getApplication()).isNetworkAvailable()) {
                       ((FileManagerActivity)mFragment.getActivity()).displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
                        return;
                    }
                    // open cloud storage file, we temporarily save it to cache

                    RemoteVFile tempVFile = (RemoteVFile)mFileArray[checkPosition];
                    int openType = FileUtility.isMultiMediaFile(mFragment.getActivity(), tempVFile.getName());
                    if (openType > OpenType.OPEN_TYPE_DEFAULT && tempVFile.getStorageType() != StorageType.TYPE_YANDEX && !(mIsAttachOp || mFragment.mIsMultipleSelectionOp)) {
                        FileUtility.openStreamFileWidth(mFragment.getActivity(), tempVFile,openType);
                        FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                        fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_RETRIEVING_URL);
                    }else {
                        VFile[] srcVFile = {tempVFile};
                        String account = tempVFile.getStorageName();
                        VFile dstVFile = new LocalVFile(mFragment.getActivity().getExternalCacheDir(), ".cfile/");
                        if (!dstVFile.exists()) {
                            dstVFile.mkdirs();
                        }
                        int type = tempVFile.getMsgObjType();
                        EditPool editPool = new EditPool();
                        editPool.setFile(tempVFile);
                        editPool.setTargetDataType(dstVFile.getVFieType());
                        editPool.setPasteDialogType(PasteDialogFragment.DIALOGTYPE_PREVIEW);
                         PasteDialogFragment pasteDialogFragment = PasteDialogFragment.newInstance(editPool);
                        try {
                            if(!pasteDialogFragment.isAdded()){
                                pasteDialogFragment.show(mFragment.getFragmentManager(), PasteDialogFragment.PREVIEW_DIALOG_PROCESS);
                            }
                            RemoteFileUtility.getInstance(mFragment.getActivity()).sendCloudStorageCopyMessage(account, srcVFile, dstVFile, type, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, RemoteFileUtility.REMOTE_PREVIEW_ACTION, false);
                        }catch (IllegalStateException illegalState){
                            Log.d(TAG, "open file may failed due to illegalState:");
                        }
                        /*
                        fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);*/
                    }

                } else if (mFileArray[checkPosition].getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                    SambaVFile sambaVFile = (SambaVFile)mFileArray[checkPosition];
                    SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
                    if(!(mFragment.mIsMultipleSelectionOp || mIsAttachOp) && sambaFileUtility.isMediaFile(sambaVFile.getAbsolutePath())){
                        sambaFileUtility.playMediaFileOnLine(sambaVFile.getAbsolutePath());
                    }else {
                        SambaVFile[] files = new SambaVFile[1];
                        files[0] = sambaVFile;
                        VFile dstVFile = new LocalVFile(mFragment.getActivity().getExternalCacheDir(), SambaFileUtility.SAMBA_CACHE_FOLDER + files[0].getIndicatorPath());
                        sambaFileUtility.sendSambaMessage(SambaMessageHandle.FILE_OPEN, files, dstVFile.getParentFile().getAbsolutePath(), false,-1,null);
                        if (!dstVFile.exists()) {
                            FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                            fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);
                        }
                    }
					
                }
            }
            GaOpenFile.getInstance()
                    .sendEvents(mFragment.getActivity(), mFileArray[checkPosition]);
            break;
           case R.id.content_gird:
               checkPosition = position;

               if (mFileArray == null || checkPosition >= mFileArray.length || mFileArray[checkPosition] == null) {
                   Log.w(TAG, "mFileArray == null || mFileArray[checkPosition] == null");
               } else if(mFragment.isInEditMode()) {
                   updateEditItemStatus(checkPosition,!mFileArray[checkPosition].getChecked());
               } else if (mFileArray[checkPosition].isFavoriteFile() && !mFileArray[checkPosition].exists()) {
                   ToastUtility.show(mFragment.getActivity(), R.string.linked_file_missing);
               } else if (mFileArray[checkPosition].isDirectory()) {
                    ItemOperationUtility.getInstance().setLastViewPosition(mFragment.getGridView());

                    if(mFragment.getmIndicatorFile() != null && (mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_IMAGE_FILE)
                         || mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_MUSIC_FILE)
                         || mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_VIDEO_FILE)
                    )){
                        //scan category image
                        if(mFileArray[checkPosition].getBucketId() != 0){
                            LocalVFile imageFolder= new LocalVFile(mFragment.getmIndicatorFile().getAbsolutePath() + File.separator + mFileArray[checkPosition].getName(), VFileType.TYPE_CATEGORY_STORAGE);
                            imageFolder.setBucketId(mFileArray[checkPosition].getBucketId());
                            mFragment.startScanAlbums(imageFolder);
                            return;
                        }
                    }

                    if (mFileArray[checkPosition].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {

                        if(!((FileManagerApplication)mFragment.getActivity().getApplication()).isNetworkAvailable()) {
                           ((FileManagerActivity)mFragment.getActivity()).displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
//                            isFromClickDrawerClosed = true;
//                            mActivity.closeStorageDrawerList();
                            return;
                        }

                        RemoteVFile tempVFile = (RemoteVFile)mFileArray[checkPosition];
                        tempVFile.setFromFileListItenClick(true);
                       // RecentFileUtil.saveVfile(tempVFile, mFragment.getActivity());
                        mFragment.startScanFile(tempVFile, ScanType.SCAN_CHILD);
                    }else{
                        mFragment.startScanFile(mFileArray[checkPosition], ScanType.SCAN_CHILD);
                    }
               } else {
                   if (mFileArray[checkPosition].getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
                       //mFragment.onDeselectAll();
//                       if (mFileArray[checkPosition].getName().toLowerCase().endsWith(".zip") && !mIsAttachOp && !mFragment.mIsMultipleSelectionOp) {
//                           VFile zipFile = mFileArray[checkPosition];
//                           String unZipName = zipFile.getNameNoExtension();
//                           long unZipSize = 0;
//                           String encode = ItemOperationUtility.getInstance().getSuitableEncoding(mFragment.getActivity());//mFragment.getString(R.string.default_encoding);
//                           String uriString = null;
//                           UnZipData unZipData = new UnZipData(zipFile, unZipName, unZipSize, encode, uriString);
//                           mFragment.showDialog(DialogType.TYPE_UNZIP_PREVIEW_DIALOG, unZipData);
//                       } else {
                           boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mFragment.getActivity()).isNeedToWriteSdToAppFolder(mFileArray[checkPosition].getAbsolutePath());
                           if((mFileArray[checkPosition].getName().toLowerCase().endsWith(".zip") || mFileArray[checkPosition].getName().toLowerCase().endsWith(".rar") )
                               && bNeedWriteToAppFolder){
                               WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                                   .newInstance();
                               warnDialog.show(mFragment.getActivity().getFragmentManager(),
                                   "WarnKKSDPermissionDialogFragment");
                            }else if((mFileArray[checkPosition].getName().toLowerCase().endsWith(".zip") || mFileArray[checkPosition].getName().toLowerCase().endsWith(".rar") )
                               && SafOperationUtility.getInstance(mFragment.getActivity()).isNeedToShowSafDialog(mFileArray[checkPosition].getAbsolutePath())){
                               ((FileManagerActivity)mFragment.getActivity()).callSafChoose(SafOperationUtility.ACTION_EXTRACT);
                           }else{
                               FileUtility.openFile(mFragment.getActivity(), mFileArray[checkPosition], mIsAttachOp, false, true, mFragment.isCategoryRecent());
                           }
//                       }
                   } else if (mFileArray[checkPosition].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {

                       if(!((FileManagerApplication)mFragment.getActivity().getApplication()).isNetworkAvailable()) {
                          ((FileManagerActivity)mFragment.getActivity()).displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG, VFileType.TYPE_CLOUD_STORAGE);
                           return;
                       }
                       // open cloud storage file, we temporarily save it to cache

                       RemoteVFile tempVFile = (RemoteVFile)mFileArray[checkPosition];
                       int openType = FileUtility.isMultiMediaFile(mFragment.getActivity(), tempVFile.getName());
                       if (openType > OpenType.OPEN_TYPE_DEFAULT && tempVFile.getStorageType() != StorageType.TYPE_YANDEX && !(mIsAttachOp || mFragment.mIsMultipleSelectionOp)) {
                           FileUtility.openStreamFileWidth(mFragment.getActivity(), tempVFile,openType);
                           FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                           fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_RETRIEVING_URL);
                       }else {
                           VFile[] srcVFile = {tempVFile};
                           String account = tempVFile.getStorageName();
                           VFile dstVFile = new LocalVFile(mFragment.getActivity().getExternalCacheDir(), ".cfile/");
                           if (!dstVFile.exists()) {
                               dstVFile.mkdirs();
                           }
                           int type = tempVFile.getMsgObjType();
                           EditPool editPool = new EditPool();
                           editPool.setFile(tempVFile);
                           editPool.setTargetDataType(dstVFile.getVFieType());
                           editPool.setPasteDialogType(PasteDialogFragment.DIALOGTYPE_PREVIEW);
                            PasteDialogFragment pasteDialogFragment = PasteDialogFragment.newInstance(editPool);
//                               pasteDialogFragment.show(mFragment.getFragmentManager(), "PasteDialogFragment");
                            pasteDialogFragment.show(mFragment.getFragmentManager(), PasteDialogFragment.PREVIEW_DIALOG_PROCESS);
                           RemoteFileUtility.getInstance(mFragment.getActivity()).sendCloudStorageCopyMessage(account, srcVFile, dstVFile, type, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, RemoteFileUtility.REMOTE_PREVIEW_ACTION, false);
                           /*
                           fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);*/
                       }

                   } else if (mFileArray[checkPosition].getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                       SambaVFile sambaVFile = (SambaVFile)mFileArray[checkPosition];
                       SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
                       if(!(mFragment.mIsMultipleSelectionOp || mIsAttachOp) && sambaFileUtility.isMediaFile(sambaVFile.getAbsolutePath())){
                           sambaFileUtility.playMediaFileOnLine(sambaVFile.getAbsolutePath());
                       }else {
                           SambaVFile[] files = new SambaVFile[1];
                           files[0] = sambaVFile;
                           VFile dstVFile = new LocalVFile(mFragment.getActivity().getExternalCacheDir(), SambaFileUtility.SAMBA_CACHE_FOLDER + files[0].getIndicatorPath());
                           sambaFileUtility.sendSambaMessage(SambaMessageHandle.FILE_OPEN, files, dstVFile.getParentFile().getAbsolutePath(), false,-1,null);
                           if (!dstVFile.exists()) {
                               FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                               fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);

                           }
                       }
                   }
               }
               GaOpenFile.getInstance()
                       .sendEvents(mFragment.getActivity(), mFileArray[checkPosition]);
               break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> itemView, View view, int position,
            long arg3) {
        // TODO Auto-generated method stub
        Log.d("Jack", "******* is moving***** " + mFragment.isItemMoving());

        if(mTmpSwipeitemView != null && !mFragment.isItemMoving()){
            mTmpSwipeitemView.setBackgroundColor(itemView.getResources().getColor(android.R.color.transparent));
            mTmpSwipeitemView = null;
        }

        if (mIsAttachOp || (mFragment.mIsMultipleSelectionOp && mFileArray[position].isDirectory()) ) {
            return false;
        }

        // FIXME:
        // We don't need this operation if we don't have the slide function
//        if(mFragment.isItemMoving()){
//            mFragment.setSliding(false);
//            return false;
//        }
        int checkPosition = position;
        switch (itemView.getId()) {
            //case R.id.file_list_item_container:
            case android.R.id.list:
                if (DEBUG) {
                    Log.d(TAG, "containerView:get Postion :" + checkPosition);
                }

                updateEditItemStatus(checkPosition,!mFileArray[checkPosition].getChecked());

                if(mFragment.mActivity.isPadMode()){
                    ClipData data = ClipData.newPlainText("Object", "Drag_Object");
                    if (mTouchX != -1) {
                        ShadowBuilder.sTouchX = (int)mTouchX;
                    } else {
                        ShadowBuilder.sTouchX = (int)((itemView.getLeft()+itemView.getRight())/2);
                    }
                    CheckResult selectResult = getSelectedCount();
                    itemView.startDrag(data, new ShadowBuilder(itemView,selectResult.count),(Object)itemView, 0);
                }
                return true;
            case R.id.content_gird:
                if (DEBUG) {
                    Log.d(TAG, "containerView:get Postion :" + checkPosition);
                }

                updateEditItemStatus(checkPosition,!mFileArray[checkPosition].getChecked());

                if(mFragment.mActivity.isPadMode()){
                    ClipData data = ClipData.newPlainText("Object", "Drag_Object");
                    if (mTouchX != -1) {
                        ShadowBuilder.sTouchX = (int)mTouchX;
                    } else {
                        ShadowBuilder.sTouchX = (int)((itemView.getLeft()+itemView.getRight())/2);
                    }
                    CheckResult selectResult = getSelectedCount();
                    itemView.startDrag(data, new ShadowBuilder(itemView,selectResult.count),(Object)itemView, 0);
                }
                return true;
            default:
                break;
        }
        return false;
    }

    private void disableAirView(View view) {
        try {
            Method m = view.getClass().getMethod("setAirViewEnabled", boolean.class);
            m.setAccessible(true);
            m.invoke(view, false);  // disable AirView
        } catch (Exception e) {
              e.printStackTrace();
        }
    }

    private void updateEditItemStatus(int checkPosition,boolean checked){

        if (mFileArray == null || checkPosition >= mFileArray.length || mFileArray[checkPosition] == null
                || mFileArray[checkPosition].getParent().equals(RemoteFileUtility.getInstance(mFragment.getActivity()).getHomeCloudRootPath())
                || (mFragment.mIsMultipleSelectionOp && mFileArray[checkPosition].isDirectory())) {
            Log.w(TAG, "mFileArray == null || mFileArray[checkPosition] == null");
            return;
        }else{
            mFileArray[checkPosition].setChecked(checked);

            if(mFileArray[checkPosition].getChecked()){
                mSelectedResult.count = mSelectedResult.count + 1;
                if(mFileArray[checkPosition].isDirectory()){
                    mSelectedResult.dircount = mSelectedResult.dircount + 1;
                    mSelectedResult.hasDir = true;
                }
            }else{
                mSelectedResult.count = mSelectedResult.count - 1;
                if(mFileArray[checkPosition].isDirectory()){
                    mSelectedResult.dircount = mSelectedResult.dircount - 1;
                    if(mSelectedResult.dircount < 1){
                        mSelectedResult.hasDir = false;
                    }
                }
            }


            notifyDataSetChanged();
            mFragment.updateEditMode(checkPosition);
        }
    }

    private void setStylusIcon(View view) {
        try {
            int iconHover = 10001;  //STYLUS_ICON_FOCUS

            /*Field f = view.getClass().getField("STYLE_STYLUS_HOVER");
            f.setAccessible(true);
            iconHover = ((Integer)f.get(null)).intValue();*/

            Method m = view.getClass().getMethod("setPreferedStylusIcon", int.class);  // Change to Drawable.class for the other variant
            m.setAccessible(true);
            m.invoke(view, iconHover);
        } catch (Exception e) {
              e.printStackTrace();
        }
    }
    public void SlideOutItem(RemoveDirection direction, int position, View itemView){
        onPopupActionButtonClick(itemView, position);
        itemView.setBackgroundColor(mFragment.getResources().getColor(ThemeUtility.getItemSelectedBackgroundColor()));
        if(mTmpSwipeitemView != null){
            mTmpSwipeitemView.setBackgroundColor(mFragment.getResources().getColor(android.R.color.transparent));
        }
        mTmpSwipeitemView = itemView;
    }

    private static String convertSecondsToHMmSs(long seconds) {
        long sec = seconds % 60;
        long min = (seconds / 60) % 60;
        long hour = (seconds / 3600) % 24;
        if (0 == hour) {
            return String.format("%02d:%02d", min, sec);
        }
        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    private static boolean isDeviceNamed_PF500KL_Kitkat_CN(Context context) {
        // model name: pf500kl
        // android version: 4.4.2
        // language and input: CN
        return (
            android.os.Build.MODEL.equals("ASUS_T00N") &&
            android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.KITKAT &&
            context.getResources().getConfiguration().locale.getCountry().equals(Locale.CHINA.getCountry())
        );
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

    // Don't use the android:ellipsize="middle" at the XML
    private static void ellipsizeMultipleLine(TextView textView, final int kMaxNumEllipsizeLine) {
        textView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right,
                    int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                if (v instanceof TextView) {
                    TextView text = (TextView)v;
                    Layout textLayout = text.getLayout();
                    int numNoEllipsizeLine = kMaxNumEllipsizeLine-1;
                    int numLine = textLayout.getLineCount();
                    String textContent = text.getText().toString();
                    StringBuilder strNoEllipsize = new StringBuilder();
                    StringBuilder strNeedToEllipsize = new StringBuilder();
                    for (int i = 0; i < numNoEllipsizeLine; ++i) {
                        strNoEllipsize.append(textContent.substring(
                            textLayout.getLineStart(i),
                            textLayout.getLineEnd(i)
                        ));
                    }
                    for (int i = numNoEllipsizeLine; i < numLine; ++i) {
                        strNeedToEllipsize.append(textContent.substring(
                            textLayout.getLineStart(i),
                            textLayout.getLineEnd(i)
                        ));
                    }
                    String strEllipsize = TextUtils.ellipsize(strNeedToEllipsize, text.getPaint(),
                            (float)text.getWidth(),
                            TextUtils.TruncateAt.MIDDLE).toString();

                    text.setText(strNoEllipsize + strEllipsize);
                    text.setLines(kMaxNumEllipsizeLine);
                }
            }
        });
    }

}
