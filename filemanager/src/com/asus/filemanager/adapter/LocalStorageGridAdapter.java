package com.asus.filemanager.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.adapter.StorageListAdapger.StorageItemElement;
import com.asus.filemanager.ga.GaBrowseFile;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.ui.RoundProgressBar;
import com.asus.filemanager.utility.CreateShortcutUtil;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VolumeInfoUtility;
import com.asus.filemanager.utility.reflectionApis;

import java.io.IOException;
import java.util.LinkedList;

import static android.R.attr.textColor;

public class LocalStorageGridAdapter  extends BaseAdapter implements OnItemClickListener, OnClickListener, OnTouchListener {
    private static final String TAG = LocalStorageGridAdapter.class.getSimpleName();

//    private static final int INTERNAL_STORAGE = 0;
//    private static final int MICROSD = 1;
//    private static final int USBDISK1 = 2;
//    private static final int USBDISK2 = 3;
//    private static final int SDREADER = 4;
//    private static final int USBDISK3 = 5;
//    private static final int USBDISK4 = 6;
//    private static final int USBDISK5 = 7;

    private FileManagerActivity mActivity;
//    private String[] mStorageTitle;
//    private TypedArray mStorageDrawable;
    private int mProgressImageSize;
    private LinkedList<StorageItemElement> mStorageItemElementList = new LinkedList<>();

    private boolean mWillShowAnimation;

    private class ViewHolder {
        View container;
        TextView name;
        RoundProgressBar progress;
    }

    public LocalStorageGridAdapter(FileManagerActivity activity) {
        mActivity = activity;
//        mStorageTitle = activity.getResources().getStringArray(R.array.storage_title);
//        mStorageDrawable = activity.getResources().obtainTypedArray(R.array.local_storage_icon);
        mProgressImageSize = (int) activity.getResources().getDimension(R.dimen.category_local_storage_grid_item_round_progress_bar_image_size);
        mWillShowAnimation = true;
    }

    public void createShortcutByView(View targetView) {
        ViewHolder holder = (ViewHolder)targetView.getTag();
        ClickIconItem iconItem = (ClickIconItem) holder.container.getTag();
        CreateShortcutUtil.createFolderShortcut(mActivity, iconItem.file.getPath(), iconItem.storageName);
    }
    public ClickIconItem getClickedIconItem(View targetView) {
        ViewHolder holder = (ViewHolder)targetView.getTag();
        return (ClickIconItem) holder.container.getTag();
    }

    @Override
    public int getCount() {
        return mStorageItemElementList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void resume() {
        mWillShowAnimation = true;
        notifyDataSetChanged();
    }

    public void updateAdapter(LinkedList<StorageItemElement> storageItemElementList, boolean forceUpdate){
        mStorageItemElementList = storageItemElementList;

        if (forceUpdate) {
            notifyDataSetInvalidated();
        } else {
            notifyDataSetChanged();
        }
    }

    @SuppressWarnings("ResourceAsColor")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        VFile storageFile;
        Drawable drawable;
        ClickIconItem iconItem = new ClickIconItem();
        StorageItemElement storageItemElement;
        if (convertView == null) {
            convertView = mActivity.getLayoutInflater().inflate(
                    R.layout.local_storage_grid_item, parent, false);
            holder = new ViewHolder();
            holder.container = convertView.findViewById(R.id.storage_list_item_container);
            holder.progress = (RoundProgressBar) convertView.findViewById(R.id.roundProgressBar);
            holder.name = (TextView) convertView.findViewById(R.id.storage_list_item_name);
            convertView.setTag(holder);
            convertView.setLongClickable(true);
        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.container.setVisibility(View.VISIBLE);
        }

        storageItemElement = mStorageItemElementList.get(position);
        storageFile = storageItemElement.vFile;
        if(storageItemElement.storageType == StorageListAdapger.STORAGETYPE_LOCAL){
            String storagePath = null;
            try {
                if(storageFile != null)
                    storagePath = FileUtility.getCanonicalPath(storageFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            holder.name.setText((VolumeInfoUtility.getInstance(mActivity).findStorageTitleByStorageVolumeAndPath( mStorageItemElementList.get(position).storageVolume,storagePath)));
            drawable = (VolumeInfoUtility.getInstance(mActivity).getSVGIconByStorageVolume(mStorageItemElementList.get(position).storageVolume));
            // set icon to home line icon color
            if(drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(mActivity.getResources().getColor(R.color.home_line_icon), PorterDuff.Mode.SRC_ATOP);
            }
            //drawable = (VolumeInfoUtility.getInstance(mActivity).findStorageIconByStorageVolume( mStorageItemElementList.get(position).storageVolume,R.array.local_storage_icon));

            //disable unmounted item
            final StorageManager mStorageManager = (StorageManager) mActivity.getSystemService(Context.STORAGE_SERVICE);
            boolean unmounted = storageItemElement.storageVolume != null && !reflectionApis.getVolumeState(mStorageManager, storageItemElement.storageVolume).equals(Environment.MEDIA_MOUNTED);

            int textColorId = R.color.category_local_storage_name_font_color;
            if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
                textColorId = R.color.dark_theme_text_color;
            }
            int textColor = unmounted ? ContextCompat.getColor(mActivity, R.color.category_local_storage_name_font_color_disable) :
                    ContextCompat.getColor(mActivity, textColorId);
            holder.name.setTextColor(textColor);

            if(holder.progress != null)
            {
                // Make sure user can only see mounted storage and deal with it.
                if(!unmounted)
                {
                    holder.container.setVisibility(View.VISIBLE);
                    holder.name.setVisibility(View.VISIBLE);
                    holder.progress.setVisibility(View.VISIBLE);

                    holder.progress.setEnabled(mActivity, true);
                    holder.progress.setMax(1000);

                    long totalSpace = 0;
                    long usedSpace = 0;

                    if (storageFile != null)
                    {
                        totalSpace = storageFile.getTotalSpace();
                        usedSpace = (totalSpace - storageFile.getUsableSpace());
                    }

                    double progress = (totalSpace == 0) ? 0 : (usedSpace * holder.progress.getMax() / totalSpace);

                    String usedText = FileUtility.bytes2String(mActivity, usedSpace, 2);
                    String totalText = FileUtility.bytes2String(mActivity, totalSpace, 2);
                    // Bitmap bitmap = drawableToBitmap(bitmapDrawable, mProgressImageSize, unmounted);
                    // Because unmounted is always false in this condition.

                    Double width = mProgressImageSize * 0.8;
                    Bitmap bitmap = drawableToBitmap(drawable, width.intValue(), mProgressImageSize, false);

                    holder.progress.setBitmap(bitmap);
                    holder.progress.setUsedText(usedText);
                    holder.progress.setTotalText(totalText);
                    holder.progress.setProgress((int) progress);

                    if (mWillShowAnimation)
                    {
                        startRoundProgressBarAnimation(holder.progress, usedSpace, totalSpace, 1000);
                    }
                }
                else
                {
                    Log.i(TAG, "getView, storage is unmounted, not show ui,");

                    // Storage is not mounted, let user can not see related ui.
                    holder.container.setVisibility(View.GONE);
                    holder.name.setVisibility(View.GONE);
                    holder.progress.setVisibility(View.GONE);
                }
            }

            convertView.setOnClickListener(unmounted ? null : this);
            convertView.setOnTouchListener(unmounted ? null : this);

            iconItem.itemtype = ClickIconItem.LOCAL_STORAGE;
            iconItem.index = position;
            iconItem.file = storageFile;
            iconItem.storageName = holder.name.getText().toString();
       }
        holder.container.setTag(iconItem);

        return convertView;
    }

    private void startRoundProgressBarAnimation(final RoundProgressBar bar, final long usedSpace, final long totalSpace, int duration) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, usedSpace);
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float current = (Float)animation.getAnimatedValue();
                bar.setUsedText(FileUtility.bytes2String(bar.getContext(), current, 2));
                bar.setProgress(totalSpace == 0 ? 0 : (int)(current * bar.getMax() / totalSpace));
            }
        });
        animator.start();
    }

    private Bitmap drawableToBitmap(Drawable drawable, int width, int height, boolean unmounted){
        Bitmap.Config config = unmounted ? Bitmap.Config.ALPHA_8 : Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        if(drawable != null){
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
        }
        return bitmap;
    }

    @Override
    public void onItemClick(AdapterView<?> AdapterView, View view, int position, long arg3) {
        notifyDataSetInvalidated();

        ClickIconItem itemIcon = (ClickIconItem) view.findViewById(R.id.storage_list_item_container).getTag();

       /**********add for GoBackToSelectItem**********/
       ItemOperationUtility.getInstance().resetScrollPositionList();

        //setItemBackgroundAndFont(view, true);
        FileListFragment fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);

        if (fileListFragment != null && itemIcon != null) {
            fileListFragment.updateCloudStorageUsage(false, 0, 0);
            fileListFragment.finishEditMode();

            mActivity.showSearchFragment(FileManagerActivity.FragmentType.NORMAL_SEARCH, false);
            fileListFragment.startScanFile(itemIcon.file, ScanType.SCAN_CHILD , false);
        }
        mActivity.closeStorageDrawerList();
    }

    @Override
    public void onClick(View view) {
        mWillShowAnimation = false;
        notifyDataSetInvalidated();

        ClickIconItem itemIcon = (ClickIconItem) view.findViewById(R.id.storage_list_item_container).getTag();

       /**********add for GoBackToSelectItem**********/
       ItemOperationUtility.getInstance().resetScrollPositionList();

        //setItemBackgroundAndFont(view, true);
        FileListFragment fileListFragment = (FileListFragment) mActivity.getFragmentManager().findFragmentById(R.id.filelist);

        if (fileListFragment != null && itemIcon != null) {
            fileListFragment.updateCloudStorageUsage(false, 0, 0);
            fileListFragment.finishEditMode();

            mActivity.showSearchFragment(FileManagerActivity.FragmentType.NORMAL_SEARCH, false);
            fileListFragment.startScanFile(itemIcon.file, ScanType.SCAN_CHILD , false);

            String label = itemIcon.file.getName();

            GaBrowseFile.getInstance().sendEvents(mActivity, GaBrowseFile.CATEGORY_NAME,
                    GaBrowseFile.ACTION_BROWSE_FROM_HOMEPAGE, label, null);
        }
        mActivity.closeStorageDrawerList();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (v.findViewById(R.id.roundProgressBar) != null) {
            v.findViewById(R.id.roundProgressBar).invalidate();
        }
        return false;
    }

}
