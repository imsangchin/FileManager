/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.asus.filemanager.activity;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.asus.filemanager.R;
import com.asus.filemanager.R.id;
import com.asus.filemanager.adapter.ImagePagerAdapter;
import com.asus.filemanager.adapter.ImagePagerAdapter.ImagePagerListener;
import com.asus.filemanager.adapter.ThumbnailImageAdapter;
import com.asus.filemanager.adapter.listpopupAdapter;
import com.asus.filemanager.dialog.delete.DeleteDialogFragment;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility.DirectoryType;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneVFile;
import com.asus.filemanager.ga.GaPhotoViewer;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.editor.Editable;
import com.asus.filemanager.provider.OpenFileProvider;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.ui.ItemClickSupport;
import com.asus.filemanager.ui.PreCachingLayoutManager;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

/**
 * Lock/Unlock button is added to the ActionBar.
 * Use it to temporarily disable ViewPager navigation in order to correctly interact with ImageView by gestures.
 * Lock/Unlock state of ViewPager is saved and restored on configuration changes.
 *
 * Julia Zudikova
 */

public class ViewPagerActivity extends BaseActivity implements OnPageChangeListener, ImagePagerListener, View.OnClickListener, MediaScannerConnection.OnScanCompletedListener, AdapterView.OnItemClickListener, Editable {

    private static final String TAG = ViewPagerActivity.class.getSimpleName();

    private ViewPager mViewPager;
    private RecyclerView mRecyclerView;
    //    private ViewPager mThumbnailViewPager;
    private static final int LOW_MemoryCache = 4 * 1024 * 1024;
    private static final int HIGH_MemoryCache = 16 * 1024 * 1024;
    private TextView mTextViewCurrent;
    private boolean mFullScreen;
    private TextView textViewColorful;
    private boolean isShowSingleFile;
    public static final String KEY_IS_SHOW_SINGLE_FILE = "IS_SHOW_SINGLE_FILE";
    private EditPool mDeleteFilePool;

    private int mBucketId;
    private String mParentFilePath;
    private int mPos;
    private final String ARG_BUCKETID = "ARG_BUCKETID";
    private final String ARG_PARENTPATH = "ARG_PARENTFILEPATH";
    private final String ARG_POS = "ARG_POS";

    private ListPopupWindow mPopupWindow;
    boolean mHasContentWidth = false;
    int mContentWidth;
    ArrayList<String> mMenuStrs;
    ArrayList<Integer> mMenuID;

    ImageView mBadge;

    private boolean mInHiddenZone = false;

    private static String[] Image_Album_projection = new String[]{
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_ID
    };

    //our own request
    private static final int REQUEST_SDTUTORIAL = 102;
    private static final int REQ_EDIT = 200;

    //SAF request
    private static final int EXTERNAL_STORAGE_PERMISSION_REQ = 42;

    private Object restoreFromSavedInstance(Context context) {
        if (mBucketId != -1) {
            String albumselection = MediaStore.Images.Media.BUCKET_ID + "=?" + " AND " + MediaStore.Images.Media.MIME_TYPE  +"!='image/gif'";

            int bucket_id = mBucketId;
            ContentResolver mCr = context.getContentResolver();
            Cursor cursor = null;
            String sortStatement = null;
            sortStatement = MediaProviderAsyncHelper.getSortStr();
            cursor = mCr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, albumselection, new String[]{String.valueOf(bucket_id)}, sortStatement);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.getCount() > mPos)
                    cursor.moveToPosition(mPos);
            }
            if (cursor != null && cursor.getPosition() != -1) {
                return cursor;
            }
        } else if (new File(mParentFilePath).isDirectory()) {
            File aParentFile = new File(mParentFilePath);
            try {
                aParentFile = aParentFile.getCanonicalFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return aParentFile;
        }
        return null;
    }

    private Object getParentFromUri(Context context, Uri uri) {
        Object ret = restoreFromSavedInstance(context);
        if (null != ret) {
            return ret;
        }

        // MediaStore (and general)
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            //we perfer file scheme rather than content scheme
            String aFilePath = getDataColumn(this, uri, null, null);
            if (null != aFilePath) {
                File aParentFile = new File(aFilePath).getParentFile();
                try {
                    aParentFile = aParentFile.getCanonicalFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mParentFilePath = aParentFile.getAbsolutePath();
                return aParentFile;
            }else{
                String albumselection = MediaStore.Images.Media.BUCKET_ID + "=?" + " AND " + MediaStore.Images.Media.MIME_TYPE  +"!='image/gif'";

                int bucket_id = -1;
                int item_id = -1;
                ContentResolver mCr = context.getContentResolver();
                Cursor cursor = null;

                try {
                    cursor = mCr.query(uri, Image_Album_projection, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        bucket_id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID));
                        item_id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                    }
                } catch (Throwable ignore) {

                } finally {
                    if (null != cursor) {
                        cursor.close();
                        cursor = null;
                    }
                }

                if (-1 != bucket_id) {
                    String sortStatement = null;
                    sortStatement = MediaProviderAsyncHelper.getSortStr();
                    cursor = mCr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, albumselection, new String[]{String.valueOf(bucket_id)}, sortStatement);
                    if (cursor != null && cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            if (cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID)) == item_id) {
                                break;
                            }
                        }
                    }
                }

                if (cursor != null && cursor.getPosition() != -1 && cursor.getPosition() != cursor.getCount()) {
                    mBucketId = bucket_id;
                    return cursor;
                } else {
                    if (cursor != null)
                        cursor.close();

                }
            }
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            File aFile = null;
            aFile = new File(uri.getPath());
            File aParentFile = aFile.getParentFile();

            try {
                aParentFile = aParentFile.getCanonicalFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            mParentFilePath = aParentFile.getAbsolutePath();
            return aParentFile;
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
            column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    @TargetApi(16)
    private static int getLruMemoryCacheSize(Context context) {

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return LOW_MemoryCache;
        } else {
            ActivityManager actManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
            MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            long totalMemory = memInfo.totalMem;
            // Memory size > 1G, use HIGH_MemoryCache
            if (totalMemory > 1024 * 1024 * 1024) {
                return HIGH_MemoryCache;
            } else {
                return LOW_MemoryCache;
            }
        }
    }

    private void updateBadge() {
        SharedPreferences mSharePrefence = getSharedPreferences("MyPrefsFile", 0);
        // used for adding new feature icon
        /*
        boolean bNewFeature_edit = mSharePrefence.getBoolean("newfeature_photoviewer_edit", true);
        boolean bNewFeature_open_with = mSharePrefence.getBoolean("newfeature_photoviewer_open_with", true);
        boolean bNewFeature_set_as = mSharePrefence.getBoolean("newfeature_photoviewer_set_as", true);

        if (null != mBadge) {
            if (bNewFeature_edit || bNewFeature_open_with) {
                mBadge.setVisibility(View.VISIBLE);
            } else {
                mBadge.setVisibility(View.INVISIBLE);
            }
        }
        */

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBucketId = -1;
        mParentFilePath = "";
        mPos = -1;

        FileUtility.getCurrentSortType(this);
        if (savedInstanceState != null) {
            mBucketId = savedInstanceState.getInt(ARG_BUCKETID, -1);
            mParentFilePath = savedInstanceState.getString(ARG_PARENTPATH, "");
            mPos = savedInstanceState.getInt(ARG_POS, -1);
        }
        mDeleteFilePool = new EditPool();
        Uri aUri = getIntent().getData();
        isShowSingleFile = getIntent().getBooleanExtra(KEY_IS_SHOW_SINGLE_FILE, false);
        Object aParent = getParentFromUri(getApplicationContext(), aUri);
        Object aParentForThumbnail = getParentFromUri(getApplicationContext(), aUri);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_view_pager);
        initActionBar();
        textViewColorful = (TextView) findViewById(R.id.textViewColorful);
        textViewColorful.setVisibility(View.VISIBLE);
        textViewColorful.setBackground(new ColorDrawable(getResources().getColor(R.color.color_photoviewer_bg)));
        int statusH = getStatusBarHeight();
        textViewColorful.setHeight(statusH);
        mFullScreen = false;
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mViewPager = (HackyViewPager) findViewById(R.id.view_pager);
        mViewPager.setOffscreenPageLimit(2);
        mRecyclerView = (RecyclerView) findViewById(R.id.thumbnail_recyclerView);
//        mThumbnailViewPager = (HackyViewPager) findViewById(R.id.thumbnail_pager);

        if (!ImageLoader.getInstance().isInited()) {
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .memoryCache(new LruMemoryCache(getLruMemoryCacheSize(getApplicationContext())))
                .diskCacheFileNameGenerator(new HashCodeFileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.FIFO)
                .build();
            ImageLoader.getInstance().init(config);
        }
            ImageLoader.getInstance().clearDiskCache();
        ImageLoader.getInstance().clearMemoryCache();

        Point aPoint = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(aPoint);
        if (null != aParent) {
            if (aParent instanceof Cursor) {
                Cursor aCursor = (Cursor) aParent;
                if (aCursor != null) {
                    int pos = aCursor.getPosition();
                    if (pos == aCursor.getCount())
                        pos -= 1;
                    if (pos < 0)
                        pos = 0;
                    ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(aCursor, true);
                    pagerAdapter.setImagePagerListener(this);
                    mViewPager.setAdapter(pagerAdapter);
                    mViewPager.setCurrentItem(pos, false);
                    mViewPager.addOnPageChangeListener(this);

                    Cursor aCursor2 = (Cursor) aParentForThumbnail;

                    PreCachingLayoutManager layoutManager = new PreCachingLayoutManager(this);
                    layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                    mRecyclerView.setLayoutManager(layoutManager);
                    mRecyclerView.setAdapter(new ThumbnailImageAdapter(aCursor2));
                    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
                    RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
                    if (animator instanceof SimpleItemAnimator) {
                        ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
                    }
                    mRecyclerView.smoothScrollToPosition(pos);
                    Adapter<?> anAdapter = mRecyclerView.getAdapter();
                    if (anAdapter instanceof ThumbnailImageAdapter) {
                        ((ThumbnailImageAdapter) anAdapter).setFocusedPos(pos);
                    }
                    mTextViewCurrent.setText(pos + 1 + "/" + mRecyclerView.getAdapter().getItemCount());
                    ItemClickSupport.addTo(mRecyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                        @Override
                        public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                            mViewPager.setCurrentItem(position, true);
                            Adapter<?> anAdapter = recyclerView.getAdapter();
                            if (anAdapter instanceof ThumbnailImageAdapter) {
                                ((ThumbnailImageAdapter) anAdapter).setFocusedPos(position);
                            }
                        }
                    });
                }
            } else if (aParent instanceof File) {
                File[] filelist = null;
                int pos = 0;
                if (isShowSingleFile) {
                    filelist = new File[1];
                    filelist[0] = new File(aUri.getPath());
                } else {
                    mInHiddenZone = FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(DirectoryType.HiddenZone, (File)aParent);
                    File aParentFile = mInHiddenZone?
                            new HiddenZoneVFile(new LocalVFile((File) aParent), null, -1) : (File)aParent;
                    filelist = aParentFile.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            String mediaFile_mime = reflectionApis.mediaFile_getMimeTypeForFile(pathname.getName());
                            if (!mInHiddenZone && pathname.isHidden() && !FileListFragment.sShowHidden)
                                return false;
                            if (null != mediaFile_mime && mediaFile_mime.startsWith("image")) {
                                return true;
                            }
                            return false;
                        }
                    });
                    String aContentPath = aUri.getPath();
                    if ("content".equalsIgnoreCase(aUri.getScheme())) {
                        aContentPath = getDataColumn(this, aUri, null, null);
                    }
                    if (null == aContentPath){
                        aUri.getPath();
                    }
                    String aCurrentPath = new File(aContentPath).getAbsolutePath();

                    try {
                        aCurrentPath = new File(aContentPath).getCanonicalPath();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    pos = 0;
                    for (int i = 0; i < filelist.length; i++) {
                        String aListedFilePath = filelist[i].getAbsolutePath();

                        try{
                            aListedFilePath = new File(aListedFilePath).getCanonicalPath();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (aListedFilePath.compareToIgnoreCase(aCurrentPath) == 0){
                            pos = i;
                            break;
                        }
                    }
                }

                ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(filelist, true);
                pagerAdapter.setImagePagerListener(this);
                mViewPager.setAdapter(pagerAdapter);
                mViewPager.setCurrentItem(pos, false);
                mViewPager.addOnPageChangeListener(this);

                LinearLayoutManager layoutManager = new LinearLayoutManager(this);
                layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                mRecyclerView.setLayoutManager(layoutManager);
                mRecyclerView.setAdapter(new ThumbnailImageAdapter(filelist));
                mRecyclerView.setItemAnimator(new DefaultItemAnimator());
                RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
                if (animator instanceof SimpleItemAnimator) {
                    ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
                }
                mRecyclerView.smoothScrollToPosition(pos);
                Adapter<?> anAdapter = mRecyclerView.getAdapter();
                if (anAdapter instanceof ThumbnailImageAdapter) {
                    ((ThumbnailImageAdapter) anAdapter).setFocusedPos(pos);
                }
                mTextViewCurrent.setText(pos + 1 + "/" + mRecyclerView.getAdapter().getItemCount());
                ItemClickSupport.addTo(mRecyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        mViewPager.setCurrentItem(position, true);
                        Adapter<?> anAdapter = recyclerView.getAdapter();
                        if (anAdapter instanceof ThumbnailImageAdapter) {
                            ((ThumbnailImageAdapter) anAdapter).setFocusedPos(position);
                        }
                    }
                });
            }
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        if (!mFullScreen)
            toggleFullScreen();
        updateBadge();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mBucketId != -1) {
            outState.putInt(ARG_BUCKETID, mBucketId);
        } else if (mParentFilePath != null) {
            outState.putString(ARG_PARENTPATH, mParentFilePath);
        }
        ThumbnailImageAdapter adapter = (ThumbnailImageAdapter) (mRecyclerView.getAdapter());
        int pos = adapter.getFocusedPos();
        outState.putInt(ARG_POS, pos);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int arg0) {
        ((ThumbnailImageAdapter) (mRecyclerView.getAdapter())).setFocusedPos(arg0);
        int count = ((ThumbnailImageAdapter) (mRecyclerView.getAdapter())).getItemCount();
        mRecyclerView.smoothScrollToPosition(arg0);
        mTextViewCurrent.setText(arg0 + 1 + "/" + count);
    }

    /**
     * set ActionBar background and invisible title
     */
    private void initActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_edit_bar_bg_wrap));
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        //+++ tsungching_lin@asus.com: support after API level 18
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        actionBar.setHomeAsUpIndicator(R.drawable.asus_ic_ab_back_photoviewer);
        //--- tsungching_lin@asus.com
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_photoviewer_bg)));
//        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_photoviewer_bar_bg));

        LayoutInflater mInflater = LayoutInflater.from(this);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        View mCustomView = mInflater.inflate(R.layout.actionbar_photoviewer, null);
        mTextViewCurrent = (TextView) mCustomView.findViewById(R.id.actionbar_current);
        actionBar.setCustomView(mCustomView, lp);
        ImageButton share = (ImageButton) mCustomView.findViewById(id.share_action);
        share.setOnClickListener(this);
        ImageButton delete = (ImageButton) mCustomView.findViewById(id.delete_action);
        delete.setOnClickListener(this);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1)
            delete.setVisibility(View.INVISIBLE);

        FrameLayout menu_item_more = (FrameLayout) mCustomView.findViewById(R.id.menuitem_more);
        menu_item_more.setOnClickListener(this);
        mBadge = (ImageView) mCustomView.findViewById(R.id.ImgView_badge);
        //ImageButton edit = (ImageButton) mCustomView.findViewById(id.edit_action);
        //edit.setOnClickListener(this);
        actionBar.setDisplayShowCustomEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    finish();
                }
                return true;
        }

        return false;
    }

    private void toggleFullScreen() {
        if (mFullScreen) {
            mFullScreen = false;
            getActionBar().show();
            mRecyclerView.setVisibility(View.VISIBLE);
            textViewColorful.setVisibility(View.VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            mFullScreen = true;
            getActionBar().hide();
            mRecyclerView.setVisibility(View.GONE);
            textViewColorful.setVisibility(View.GONE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onViewTap(View view, float x, float y) {
        toggleFullScreen();
    }

    private int getStatusBarHeight() {
        int h = 0;
        boolean mIsTranslucentEnabled = (getResources().getIdentifier("windowTranslucentStatus", "attr", "android") != 0);
        if (mIsTranslucentEnabled) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                h = getResources().getDimensionPixelSize(resourceId);
            }
        }
        return h;
    }

    @Override
    public void onClick(View v) {
        ThumbnailImageAdapter adapter = (ThumbnailImageAdapter) (mRecyclerView.getAdapter());
        Uri aUri = adapter.getFocusedUri();

        if (v.getId() == id.share_action) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("image/*");

            if ("file".equalsIgnoreCase(aUri.getScheme())) {
                String aFilePath = aUri.getPath();
                aUri = FileUtility.getUri(this.getApplicationContext(),new VFile(aFilePath),"image/*",false);
                shareIntent.setDataAndType(aUri,"image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, aUri);
            }else{
            // For a file in shared storage.  For data in private storage, use a ContentProvider.
                shareIntent.putExtra(Intent.EXTRA_STREAM, aUri);
            }

            startActivity(shareIntent);

            GaPhotoViewer.getInstance()
                    .sendEvents(ViewPagerActivity.this, GaPhotoViewer.CATEGORY_NAME,
                            GaPhotoViewer.ACTION_SHRARE, null, null);

        } else if (v.getId() == id.delete_action) {
            String aFilePath = "";
            if ("content".equalsIgnoreCase(aUri.getScheme())) {
                aFilePath = getDataColumn(this, aUri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(aUri.getScheme())) {
                aFilePath = aUri.getPath();
            }
            if (aFilePath != null) {
                //new File(aFilePath).renameTo(new File(aFilePath + "test.png"));
                //return ;
                VFile aFile = new LocalVFile(aFilePath);
                mDeleteFilePool.setFile(aFile);
                boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(this).isNeedToWriteSdToAppFolder(aFile.getAbsolutePath());
                if (bNeedWriteToAppFolder) {
                    WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                        .newInstance();
                    warnDialog.show(this.getFragmentManager(),
                        "WarnKKSDPermissionDialogFragment");
                    return;
                }
                if (SafOperationUtility.getInstance(this).isNeedToShowSafDialog(aFile.getAbsolutePath())) {
                    this.callSafChoose(SafOperationUtility.ACTION_DELETE);
                    return;
                }
                DeleteDialogFragment deleteDialogFragment = DeleteDialogFragment.newInstance(mDeleteFilePool, DeleteDialogFragment.Type.TYPE_DELETE_DIALOG);
                if (!deleteDialogFragment.isAdded()) {
                    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
                }
            } else {
                //can't locate file
            }

            GaPhotoViewer.getInstance()
                    .sendEvents(ViewPagerActivity.this, GaPhotoViewer.CATEGORY_NAME,
                            GaPhotoViewer.ACTION_DELETE, null, null);
        } else if (v.getId() == id.menuitem_more) {

            showPopup(v);
            /*

        } else if (v.getId() == id.edit_action) {
            Intent shareIntent = new Intent(Intent.ACTION_EDIT);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("image/*");

            // For a file in shared storage.  For data in private storage, use a ContentProvider.
            shareIntent.putExtra(Intent.EXTRA_STREAM, aUri);
            startActivity(shareIntent);
            */
        }
    }


    public void callSafChoose(int action) {
        if (FileUtility.isFirstSDPermission(this)) {
            Intent tutorialIntent = new Intent();
            tutorialIntent.setClass(this, TutorialActivity.class);
            tutorialIntent.putExtra(TutorialActivity.TUTORIAL_SD_PERMISSION, true);
            startActivityForResult(tutorialIntent, REQUEST_SDTUTORIAL);
            SafOperationUtility.getInstance(this).setCallSafAction(action);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
            SafOperationUtility.getInstance(this).setCallSafAction(action);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ViewPagerActivity.EXTERNAL_STORAGE_PERMISSION_REQ) {
            /*
            FileListFragment fl = getFileListFragment();

            if(resultCode == Activity.RESULT_OK){
                Uri treeUri = data.getData();
                DocumentFile rootFile = DocumentFile.fromTreeUri(this, treeUri);
                if(rootFile != null ){
                    getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }else{
                    callSafChoose(SafOperationUtility.getInstance(this).getCallSafAction());
                    return;
                }

            }else{
                if(fl != null){
                    fl.updateStatesWithoutSafPermission(SafOperationUtility.getInstance(this).getCallSafAction());
                    return;
                }
            }

            if(fl != null){
                fl.handleAction(SafOperationUtility.getInstance(this).getCallSafAction());
            }
            */
        } else if (requestCode == ViewPagerActivity.REQUEST_SDTUTORIAL && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
        } else if (requestCode == ViewPagerActivity.REQ_EDIT){
            ImageLoader.getInstance().clearDiskCache();
            ImageLoader.getInstance().clearMemoryCache();
            notifyFileUpdated();
        }
    }

    public Handler getEditHandler() {
        if (null == mHandler)
            mHandler = new DeleteHandler(this);
        return mHandler;
    }

    @Override
    public EditorUtility.RequestFrom getRequester() {
        return EditorUtility.RequestFrom.PhotoViewer;
    }

    public void notifyFileUpdated() {
        if (null != mRecyclerView && null != mViewPager) {

            ThumbnailImageAdapter thumbnailAdapter = (ThumbnailImageAdapter) (mRecyclerView.getAdapter());
            thumbnailAdapter.notifyDataSetChanged();

            ImagePagerAdapter pagerAdapter = (ImagePagerAdapter) mViewPager.getAdapter();
            pagerAdapter.notifyDataSetChanged();
        }
    }

    public void notifyFileDeleted(String aFilePath) {
        if (null != mRecyclerView && null != mViewPager) {

            ThumbnailImageAdapter thumbnailAdapter = (ThumbnailImageAdapter) (mRecyclerView.getAdapter());
            thumbnailAdapter.fileDeleted(aFilePath);

            ImagePagerAdapter pagerAdapter = (ImagePagerAdapter) mViewPager.getAdapter();
            pagerAdapter.fileDeleted(aFilePath);
            if (0 == mRecyclerView.getAdapter().getItemCount()) {
                finish();
            } else {
                int pos = thumbnailAdapter.getFocusedPos();
                mTextViewCurrent.setText(pos + 1 + "/" + mRecyclerView.getAdapter().getItemCount());
            }
        }
    }

    DeleteHandler mHandler;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ThumbnailImageAdapter adapter = (ThumbnailImageAdapter) (mRecyclerView.getAdapter());
        Uri aUri = adapter.getFocusedUri();
        SharedPreferences.Editor editor = getSharedPreferences("MyPrefsFile", 0).edit();

        if ("file".equalsIgnoreCase(aUri.getScheme())) {
            String aFilePath = aUri.getPath();
            aUri = FileUtility.getUri(this.getApplicationContext(),new VFile(aFilePath),"image/*",false);
        }

        mPopupWindow.dismiss();
        int menu_id = mMenuID.get(position).intValue();
        switch (menu_id) {
            case ACTION_EDIT:
                editor.putBoolean("newfeature_photoviewer_edit", false).commit();
                Intent shareIntent = new Intent(Intent.ACTION_EDIT);
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                shareIntent.setDataAndType(aUri, "image/*");
                startActivityForResult(shareIntent,REQ_EDIT);

                GaPhotoViewer.getInstance()
                        .sendEvents(ViewPagerActivity.this, GaPhotoViewer.CATEGORY_NAME,
                                GaPhotoViewer.ACTION_EDIT, null, null);
                break;
            case ACTION_OPEN_WITH:
                editor.putBoolean("newfeature_photoviewer_open_with", false).commit();
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(aUri, "image/*");
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                viewIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(Intent.createChooser(viewIntent,getResources().getString(R.string.action_open_with)));

                GaPhotoViewer.getInstance()
                        .sendEvents(ViewPagerActivity.this, GaPhotoViewer.CATEGORY_NAME,
                                GaPhotoViewer.ACTION_OPEN_WITH, null, null);
                break;
            case ACTION_SET_AS:
                editor.putBoolean("newfeature_photoviewer_set_as", false).commit();
                Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setDataAndType(aUri, "image/*");
                intent.putExtra("mimeType", "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(Intent.createChooser(intent, getResources().getString(R.string.action_set_as)));

                GaPhotoViewer.getInstance()
                        .sendEvents(ViewPagerActivity.this, GaPhotoViewer.CATEGORY_NAME,
                                GaPhotoViewer.ACTION_EDIT, null, null);
                break;
            default:
                break;
        }
        updateBadge();
    }

    private static class DeleteHandler extends Handler {
        private final WeakReference<ViewPagerActivity> mActivity;

        DeleteHandler(ViewPagerActivity activity) {
            mActivity = new WeakReference<ViewPagerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ViewPagerActivity activity = mActivity.get();
            if (null == activity)
                return;

            switch (msg.what) {
                case FileListFragment.MSG_DELET_COMPLETE:
                    VFile[] aCompleteFiles = (VFile[]) msg.obj;
                    String[] aPaths = null;
                    if (aCompleteFiles.length > 0) {
                        aPaths = new String[aCompleteFiles.length];
                        for (int i = 0; i < aCompleteFiles.length; i++) {
                            aPaths[i] = aCompleteFiles[i].getAbsolutePath();
                        }
                        MediaScannerConnection.scanFile(activity, aPaths, null, activity);
                    } else {
                        EditorUtility.sEditIsProcessing = false;
                        DeleteDialogFragment deleteProgressDialogFragment;
                        deleteProgressDialogFragment = (DeleteDialogFragment) (activity.getFragmentManager()).findFragmentByTag("DeleteDialogFragment");
                        if (deleteProgressDialogFragment != null)
                            deleteProgressDialogFragment.dismissAllowingStateLoss();
                    }

                    break;
                case MSG_SCAN_COMPLETE:
                    activity.notifyFileDeleted((String) msg.obj);
                    EditorUtility.sEditIsProcessing = false;
                    DeleteDialogFragment deleteProgressDialogFragment;
                    deleteProgressDialogFragment = (DeleteDialogFragment) (activity.getFragmentManager()).findFragmentByTag("DeleteDialogFragment");
                    if (deleteProgressDialogFragment != null)
                        deleteProgressDialogFragment.dismissAllowingStateLoss();
                    break;
                default:
                    break;
            }
        }
    }

    public static final int MSG_SCAN_COMPLETE = 2000;

    @Override
    public void onScanCompleted(String path, Uri uri) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SCAN_COMPLETE, path));
    }

    private static final int ACTION_EDIT = 0;
    private static final int ACTION_OPEN_WITH = 1;
    private static final int ACTION_SET_AS = 2;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void showPopup(View anchorView) {
        mPopupWindow = new ListPopupWindow(this);
        mMenuStrs = new ArrayList<String>();
        mMenuID = new ArrayList<Integer>();
        ArrayList<Integer> newFeatureList = new ArrayList<Integer>();


        mMenuStrs.add(getResources().getString(R.string.action_open_with));
        mMenuID.add(ACTION_OPEN_WITH);

        SharedPreferences mSharePrefence = getSharedPreferences("MyPrefsFile", 0);

        if (!mInHiddenZone) {
        mMenuStrs.add(getResources().getString(R.string.action_edit));
        mMenuID.add(ACTION_EDIT);
        }


        mMenuStrs.add(getResources().getString(R.string.action_set_as));
        mMenuID.add(ACTION_SET_AS);

        listpopupAdapter adapter = new listpopupAdapter(this, R.layout.popup_menu_item_layout, mMenuStrs);

        adapter.setIsNewFeature(newFeatureList);

        mPopupWindow.setAdapter(adapter);
        mPopupWindow.setModal(true);
        if (!mHasContentWidth) {
            mContentWidth = measureContentWidth(adapter);
            mHasContentWidth = true;
        }
        mPopupWindow.setContentWidth(mContentWidth);
        mPopupWindow.setAnchorView(anchorView);
        mPopupWindow.setOnItemClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mPopupWindow.setDropDownGravity(GravityCompat.END);
        mPopupWindow.show();
    }

    private int measureContentWidth(ListAdapter adapter) {
        // Menus don't tend to be long, so this is more sane than it looks.
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        final Resources res = getResources();
        int popupMaxWidth = Math.max(res.getDisplayMetrics().widthPixels / 2,
            res.getDimensionPixelSize(R.dimen.menu_popup_window_dialog_width));
        FrameLayout measureParent = null;
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (measureParent == null) {
                measureParent = new FrameLayout(this);
            }

            itemView = adapter.getView(i, itemView, measureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();
            if (itemWidth >= popupMaxWidth) {
                return popupMaxWidth;
            } else if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Pass any configuration change to the drawer toggls
        super.onConfigurationChanged(newConfig);
        ImageLoader.getInstance().clearMemoryCache();
    }
    /*
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        delayedIdle(5);
    }

    Handler _idleHandler = new Handler();
    Runnable _idleRunnable = new Runnable() {
        @Override
        public void run() {
            //handle your IDLE state
            if (!mFullScreen)
                toggleFullScreen();
        }
    };

    private void delayedIdle(int delaySecs) {
        _idleHandler.removeCallbacks(_idleRunnable);
        _idleHandler.postDelayed(_idleRunnable, (delaySecs* 1000));
    }
    */
}
