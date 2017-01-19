package com.asus.filemanager.adapter;

import android.R.drawable;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.SearchResultFragment;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.apprecommend.GameLaunchFile;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.provider.ProviderUtility.Thumbnail;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.IconUtility;
import com.asus.filemanager.utility.IconUtility.ThumbnailItem;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.MimeMapUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

public class ItemIcon {

    private static final int MAX_WIDTH = 256;
    private static final int MAX_HEIGHT = 256;
    public static final int THUMBNAIL_TARGET_SIZE = 320;
    private static final String TAG = "ItemIcon";
    private static final boolean DEBUG = ConstantsUtil.DEBUG;
    private static final int USE_ORIGINAL_IMAGE = 0;
    private static final int USE_RESIZE_IMAGE = 1;
    private static final int HAS_CLOUD_STORAGE_THUMBNAIL = 2;

    private static final int MSG_DECODE_FINISH = 0;
    private static final int MSG_DECODE_ERROR = 1;

    private static final int CACHE_CAPACOTY = 128;

    private Handler mHandler;
    private WaitingStack mWaitingStack;
    private Thread mDecodeThread = null;
    private Context mContext;
    private boolean mSkipPrevious;
    ContentResolver mCr;
    final Object mLock = new Object();
    private Fragment mFragment;
    private float mRoundedCorner;

    private LinkedHashMap<String, CacheItem> mBitmapCache;

    private ArrayList<String> mSambaDownloadCache = new ArrayList<String>();
    private IconRunnable mRunnable;

    private class CacheItem {
        Bitmap bitmap = null;
        long time = -1;

        public CacheItem(Bitmap b, long lastModified) {
            bitmap = b;
            time = lastModified;
        }
    }

    private class Icon {
        public VFile f;
        public ImageView v;
        public Bitmap b;
        public int r;
        public int maxSize;
        public InfoCallback callback;
    }

    public ItemIcon(Context c, Fragment fragment) {
        mHandler = new Handler(new MsgLoop());
        mWaitingStack = new WaitingStack();
        mContext = c;
        mCr = mContext.getContentResolver();
        mRunnable = new IconRunnable(mWaitingStack);

        mBitmapCache = new LinkedHashMap<String, CacheItem>(32, 0.75f, true) {
            protected boolean removeEldestEntry(
                    Map.Entry<String, CacheItem> eldest) {
                return size() > CACHE_CAPACOTY;
            }
        };
        mFragment = fragment;
        mRoundedCorner = mContext.getResources().getDimension(
                R.dimen.item_icon_rounded_corner);
    }

    public ItemIcon(Context c) {
        this(c, false);
        mRoundedCorner = c.getResources().getDimension(
                R.dimen.item_icon_rounded_corner);
    }

    public ItemIcon(Context c, boolean skipPrevious) {
        mHandler = new Handler(new MsgLoop());
        mWaitingStack = new WaitingStack();
        mContext = c;
        mCr = mContext.getContentResolver();
        mSkipPrevious = skipPrevious;
        mRunnable = new IconRunnable(mWaitingStack);

        mBitmapCache = new LinkedHashMap<String, CacheItem>(32, 0.75f, true) {
            protected boolean removeEldestEntry(
                    Map.Entry<String, CacheItem> eldest) {
                return size() > CACHE_CAPACOTY;
            }
        };
        mRoundedCorner = mContext.getResources().getDimension(
                R.dimen.item_icon_rounded_corner);
    }

    public void clearCache() {
        synchronized (ItemIcon.class) {
            if (mBitmapCache != null) {
                mBitmapCache.clear();
            }
			if(mSambaDownloadCache!=null)
            {
                mSambaDownloadCache.clear();
            }
        }
    }

    /**
     * useDefault value set false, if you don't change the default background of
     * icon to FileManager's specified pictures
     */
    public void setIcon(VFile f, ImageView v, boolean useDefault) {
        if (useDefault) {
            int res = MimeMapUtility.getIconRes(f);
            // if (res == R.drawable.asus_ep_ic_folder) {
            // v.setBackgroundColor(Color.TRANSPARENT);
            // }else {
            // v.setBackgroundColor(Color.rgb(0xd1,0xd1,0xd1));
            // }

            v.setBackgroundColor(Color.TRANSPARENT);

            // for the category folder thumbnail, we replace the category folder
            // to the category file
            if (((mFragment instanceof FileListFragment) && f.isDirectory())) {
                FileListFragment fragment = (FileListFragment) mFragment;
                ArrayList<LocalVFile> categoryBuncketIdFiles = null;
                if (fragment.isCategoryImage()) {
                    VFile firstFile = MediaProviderAsyncHelper
                            .getFirstImageFileByBucketId(mContext,
                                    "" + f.getBucketId());
                    if (null != firstFile) {
                        f = firstFile;
                    }
                    res = R.drawable.asus_ep_ic_photo;
                } else if (fragment.isCategoryMusic()) {
                    VFile firstFile = MediaProviderAsyncHelper
                            .getFirstMusicFileByBucketId(mContext,
                                    "" + f.getBucketId());
                    if (null != firstFile) {
                        f = firstFile;
                    }
                    res = R.drawable.asus_ep_ic_music;
                } else if (fragment.isCategoryVideo()) {
                    VFile firstFile = MediaProviderAsyncHelper
                            .getFirstVideoFileByBucketId(mContext,
                                    "" + f.getBucketId());
                    if (null != firstFile) {
                        f = firstFile;
                    }
                    res = R.drawable.asus_ep_ic_movie;
                }
            }

            if (mFragment != null) {
                if (mFragment instanceof FileListFragment
                        && ((FileListFragment) mFragment).isCategoryFavorite()
                        && !f.exists()) {
                    // The link to favorite folder is missing.
                    res = R.drawable.broken_file;
                }
            }

            if (res == R.drawable.asus_ep_ic_photo
                    || res == R.drawable.asus_ep_ic_music
                    || res == R.drawable.asus_ep_ic_movie
                    || res == R.drawable.asus_ep_ic_apk
                    || (f.getVFieType() == VFileType.TYPE_CLOUD_STORAGE && ((RemoteVFile) f)
                            .getHasThumbnail())) {
                synchronized (ItemIcon.class) {
                    String thumbnailPath;
                    if(f.getVFieType() == VFileType.TYPE_SAMBA_STORAGE && res == R.drawable.asus_ep_ic_photo) {

                        SambaVFile[] files = new SambaVFile[1];
                        files[0] = (SambaVFile) f;
                        VFile dstVFile = new LocalVFile(mFragment.getActivity().getExternalCacheDir(), SambaFileUtility.SAMBA_CACHE_FOLDER +((SambaVFile) f).getIndicatorPath());
                        //file not exitst
                        if (!dstVFile.exists() && !mSambaDownloadCache.contains(dstVFile.getAbsolutePath()))
                        {
                            mSambaDownloadCache.add(dstVFile.getAbsolutePath());
                            //load samba file to local
                            dstVFile.getParentFile().mkdirs();
                            //add .nomedia file if not exist
                            try {
                                LocalVFile nomedia = new LocalVFile(dstVFile.getParentFile(), ".nomedia");
                                if (!nomedia.exists())
                                    nomedia.createNewFile();
                            }catch (IOException e){}
                            SambaFileUtility.getInstance(null).sendSambaMessage(SambaMessageHandle.FILE_DOWNLOAD, files, dstVFile.getParentFile().getAbsolutePath(), false, -1, null);
                        }
                        thumbnailPath = f.getAbsolutePath();
                    }
                    else
                    {
                        // google drive rename the thumbnail path
                        thumbnailPath = (f.getVFieType() == VFileType.TYPE_CLOUD_STORAGE && ((RemoteVFile) (f))
                                .getStorageType() == StorageType.TYPE_GOOGLE_DRIVE) ? (f
                                .getAbsolutePath() + File.separator + ((RemoteVFile) (f))
                                .getFileID()) : f.getAbsolutePath();
                    }
                    CacheItem item = mBitmapCache.get(thumbnailPath);

                    // CacheItem item = mBitmapCache.get(f.getAbsolutePath());
                    if (item != null && item.bitmap != null
                            && item.time == f.lastModified()) {
                        BitmapDrawable bd = new BitmapDrawable(item.bitmap);
                        v.setBackgroundDrawable(bd);
                        v.setImageBitmap(null);
                    } else {
                        setImageResource(v, res);
                        Icon icon = new Icon();
                        icon.f = f;
                        icon.v = v;
                        if (res == R.drawable.asus_ep_ic_photo
                                || res == R.drawable.asus_ep_ic_apk
                                || res == R.drawable.asus_ep_ic_music
                                || res == R.drawable.asus_ep_ic_movie) {
                            icon.r = res;
                        } else {
                            icon.r = HAS_CLOUD_STORAGE_THUMBNAIL;
                        }

                        synchronized (mLock) {
                            if (mSkipPrevious) {
                                mWaitingStack.clear();
                                mRunnable.skipLocked();
                            }
                            mWaitingStack.push(icon);
                            startDecodeLocked();
                        }
                    }
                }
            } else {
                if (f instanceof GameLaunchFile) {
                    Drawable drawable = getIconDrawableByPackageName(((GameLaunchFile) f).getAppPackageName());
                    setImageResource(v, drawable);
                } else {
                    setImageResource(v, res);
                }
            }
        } else {
            synchronized (ItemIcon.class) {
                // google drive rename the thumbnail path
                String thumbnailPath = (f.getVFieType() == VFileType.TYPE_CLOUD_STORAGE && ((RemoteVFile) (f))
                        .getStorageType() == StorageType.TYPE_GOOGLE_DRIVE) ? (f
                        .getAbsolutePath() + File.separator + ((RemoteVFile) (f))
                        .getFileID()) : f.getAbsolutePath();
                CacheItem item = mBitmapCache.get(thumbnailPath);

                // CacheItem item = mBitmapCache.get(f.getAbsolutePath());
                if (item != null && item.bitmap != null
                        && item.time == f.lastModified()) {
                    v.setImageBitmap(item.bitmap);
                } else {
                    Icon icon = new Icon();
                    icon.f = f;
                    icon.v = v;
                    icon.r = USE_ORIGINAL_IMAGE;
                    synchronized (mLock) {
                        if (mSkipPrevious) {
                            mWaitingStack.clear();
                            mRunnable.skipLocked();
                        }
                        mWaitingStack.push(icon);
                        startDecodeLocked();
                    }
                }
            }
        }
    }

    private void setImageResource(ImageView view, int resId) {
        Resources res = mContext.getResources();
        Bitmap bmpDefaultIcon = BitmapFactory.decodeResource(res, resId);
        // BitmapDrawable bd = new BitmapDrawable(mContext.getResources(),
        // getRoundedCornerBitmap(bmpDefaultIcon, mRoundedCorner));
        BitmapDrawable bd = new BitmapDrawable(mContext.getResources(),
                bmpDefaultIcon);
        view.setBackgroundDrawable(bd);
        view.setImageBitmap(null);
    }

    private void setImageResource(ImageView view, Drawable drawable) {
        view.setBackgroundDrawable(drawable);
        view.setImageBitmap(null);
    }

    public void setResizedIcon(VFile f, ImageView v, int maxSize,
            InfoCallback callback) {
        synchronized (ItemIcon.class) {
            Icon icon = new Icon();
            icon.f = f;
            icon.v = v;
            icon.r = USE_RESIZE_IMAGE;
            icon.maxSize = maxSize;
            icon.callback = callback;
            synchronized (mLock) {
                if (mSkipPrevious) {
                    mWaitingStack.clear();
                    mRunnable.skipLocked();
                }
                mWaitingStack.push(icon);
                startDecodeLocked();
            }
        }
    }

    private void startDecodeLocked() {
        Log.d(TAG, "startDecodeLocked, mDecodeThread:" + mDecodeThread
                    + " empty:" + mWaitingStack.empty());

        if (mDecodeThread == null) {
            Log.d(TAG, "create thread");
            mDecodeThread = new Thread(mRunnable);
            mDecodeThread.setName("Icon-Thread");
            mDecodeThread.setPriority(Thread.MIN_PRIORITY);
            mDecodeThread.start();
        }
    }

    private class IconRunnable implements Runnable {
        private WaitingStack mStack;
        private boolean mSkip = false;

        public IconRunnable(WaitingStack waitingStack) {
            mStack = waitingStack;
        }

        public void skipLocked() {
            mSkip = true;
        }

        @Override
        public void run() {

            while (true) {
                Icon mIcon;
                synchronized (mLock) {
                    if (!mStack.empty()) {
                        mIcon = mStack.pop();
                        Log.d(TAG, "pop from thread");
                    } else {
                        mDecodeThread = null;
                        Log.d(TAG, "exit thread");
                        break;
                    }
                    mSkip = false;
                }

                if (mIcon.r == R.drawable.asus_ep_ic_photo
                        || mIcon.r == R.drawable.asus_ep_ic_music
                        || mIcon.r == R.drawable.asus_ep_ic_movie
                        || mIcon.r == R.drawable.asus_ep_ic_apk
                        || mIcon.r == USE_ORIGINAL_IMAGE
                        || mIcon.r == HAS_CLOUD_STORAGE_THUMBNAIL) {
                    // google drive rename the thumbnail path
                    String thumbnailPath = (mIcon.f.getVFieType() == VFileType.TYPE_CLOUD_STORAGE && ((RemoteVFile) (mIcon.f))
                            .getStorageType() == StorageType.TYPE_GOOGLE_DRIVE) ? (mIcon.f
                            .getAbsolutePath() + File.separator + ((RemoteVFile) (mIcon.f))
                            .getFileID()) : mIcon.f.getAbsolutePath();
                    ThumbnailItem thumbnailItem = Thumbnail.getThumbnailItem(
                            mCr, thumbnailPath);

                    // ThumbnailItem thumbnailItem =
                    // Thumbnail.getThumbnailItem(mCr,
                    // mIcon.f.getAbsolutePath());
                    if (thumbnailItem.thumbnail != null
                            && thumbnailItem.modifyTime == mIcon.f
                                    .lastModified()) {
                        mIcon.b = (thumbnailItem.thumbnail);
                        // query picasa for their thumbnail
                    } else if (mIcon.f.getVFieType() == VFileType.TYPE_PICASA_STORAGE) {
                        getPicasaThumbnail(mIcon);
                        // query cloud storage
                    } else if (mIcon.f.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                        if (!((((RemoteVFile) (mIcon.f)).getStorageType() == StorageType.TYPE_ASUSWEBSTORAGE && mIcon.r == R.drawable.asus_ep_ic_movie) || ((RemoteVFile) (mIcon.f))
                                .getStorageType() == StorageType.TYPE_HOME_CLOUD
                                && mIcon.r == R.drawable.asus_ep_ic_movie)) {
                            if (mFragment instanceof FileListFragment) {
                                if (mFragment == null) {
                                    return;
                                }
                                FileListFragment fileListFragment = (FileListFragment) mFragment
                                        .getFragmentManager().findFragmentById(
                                                R.id.filelist);
                                // update remote thumbnail when user doesn't
                                // scroll
                                if (fileListFragment != null
                                        && !fileListFragment.isScrolling()) {
                                    ShortCutFragment shortcutFragment = (ShortCutFragment) mFragment
                                            .getFragmentManager()
                                            .findFragmentByTag(
                                                    FileManagerActivity.sShortCutFragmentTag);
                                    shortcutFragment
                                            .sendCloudStorage(
                                                    mIcon.f,
                                                    CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_THUMBNAIL);
                                }
                            } else if (mFragment instanceof SearchResultFragment) {
                                SearchResultFragment searchResultFragment = (SearchResultFragment) mFragment
                                        .getFragmentManager().findFragmentById(
                                                R.id.searchlist);
                                if (searchResultFragment != null
                                        && !searchResultFragment.isScrolling()) {
                                    ShortCutFragment shortcutFragment = (ShortCutFragment) mFragment
                                            .getFragmentManager()
                                            .findFragmentByTag(
                                                    FileManagerActivity.sShortCutFragmentTag);
                                    shortcutFragment
                                            .sendCloudStorage(
                                                    mIcon.f,
                                                    CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_THUMBNAIL);
                                }
                            }
                        }

                    } else {
                        Bitmap bitmap = null;
                        if (mIcon.r == R.drawable.asus_ep_ic_apk) {
                            bitmap = IconUtility.getApkIcon(mContext,
                                    mIcon.f.getAbsolutePath());
                        } else if (mIcon.r == R.drawable.asus_ep_ic_music) {
                            bitmap = retrieveAlbumArt(mContext,
                                    mIcon.f.getAbsolutePath(),
                                    THUMBNAIL_TARGET_SIZE);
                        } else if (mIcon.r == R.drawable.asus_ep_ic_movie) {
                            bitmap = getVideoThumbnail(
                                    mIcon.f.getAbsolutePath(),
                                    THUMBNAIL_TARGET_SIZE);
                        } else {
                            String decodePath;
                            if(mIcon.f.getVFieType() == VFileType.TYPE_SAMBA_STORAGE)
                            {
                                decodePath = new LocalVFile(mFragment.getActivity().getExternalCacheDir(),SambaFileUtility.SAMBA_CACHE_FOLDER +((SambaVFile)  mIcon.f).getIndicatorPath()).getAbsolutePath();
                            }
                            else
                            {
                                decodePath = mIcon.f.getAbsolutePath();
                            }
                            bitmap = loadResizedBitmap(
                                    decodePath,
                                    THUMBNAIL_TARGET_SIZE);
                            if (bitmap != null) {
                                int degree = getImageOrientation(mIcon.f
                                        .getAbsolutePath());
                                if (degree > 0) {
                                    Bitmap tempBitmap = getNewRotatedImage(
                                            bitmap, degree);
                                    if (tempBitmap != null) {
                                        bitmap = tempBitmap;
                                        tempBitmap = null;
                                    } else {
                                        Log.d(TAG,
                                                "Cannot get the rotated bitmap");
                                    }
                                }
                            }
                        }
                        if (bitmap != null) {
                            /*
                             * int degree =
                             * getImageOrientation(mIcon.f.getAbsolutePath());
                             * if (degree > 0) { Bitmap tempBitmap =
                             * getNewRotatedImage(bitmap, degree); if
                             * (tempBitmap != null) { bitmap = tempBitmap;
                             * tempBitmap = null; } else { Log.d(TAG,
                             * "Cannot get the rotated bitmap"); } }
                             */

                            mIcon.b = bitmap;
							if(mIcon.f.getVFieType()!=VFileType.TYPE_SAMBA_STORAGE) {
								try {
									bitmap = ThumbnailUtils.extractThumbnail(
											bitmap, THUMBNAIL_TARGET_SIZE,
											THUMBNAIL_TARGET_SIZE);
									ByteArrayOutputStream stream = new ByteArrayOutputStream();
									bitmap.compress(Bitmap.CompressFormat.PNG, 20,
											stream);
									byte[] bitmapStream = stream.toByteArray();
									stream.close();
									Thumbnail.setThumbnailAndTime(mCr,
											mIcon.f.getAbsolutePath(),
											bitmapStream, mIcon.f.lastModified());
								} catch (Exception e) {
									Log.e(TAG, "Save bitmap error:" + e);
								}
							}
                        } else {
                            Log.d(TAG,
                                    "Fail to generate thumbnail, use default icon");
                        }
                    }
                } else if (mIcon.r == USE_RESIZE_IMAGE) {
                    Bitmap bitmap;
                    String decodePath;
                    if(mIcon.f.getVFieType() == VFileType.TYPE_SAMBA_STORAGE)
                    {
                        decodePath = new LocalVFile(mFragment.getActivity().getExternalCacheDir(), SambaFileUtility.SAMBA_CACHE_FOLDER +((SambaVFile)  mIcon.f).getIndicatorPath()).getAbsolutePath();
                    }
                    else
                    {
                        decodePath = mIcon.f.getAbsolutePath();
                    }
                    bitmap = loadResizedBitmap(decodePath,
                            mIcon.maxSize, mIcon.maxSize, false);
                    if (bitmap != null) {
                        int degree = getImageOrientation(mIcon.f
                                .getAbsolutePath());
                        if (degree > 0) {
                            Bitmap tempBitmap = getNewRotatedImage(bitmap,
                                    degree);
                            if (tempBitmap != null) {
                                bitmap = tempBitmap;
                                tempBitmap = null;
                            } else {
                                Log.d(TAG, "Cannot get the rotated bitmap");
                            }
                        }
                    }
                    mIcon.b = bitmap;
                }

                synchronized (mLock) {
                    if (mIcon.b != null && !mSkip) {
                        mHandler.sendMessage(mHandler.obtainMessage(
                                MSG_DECODE_FINISH, mIcon));
                    }
                }
            }
        }
    }

    private Bitmap getVideoThumbnail(String videoPath, int kind) {
        Bitmap bitmap = null;
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        if (bitmap != null) {
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, 96, 96,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        // if (bitmap != null) {
        // bitmap = getRoundedCornerBitmap(bitmap, mRoundedCorner);
        // }
        return bitmap;
    }

    private class MsgLoop implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_DECODE_FINISH:
                Log.d(TAG, "MSG_DECODE_FINISH");
                Icon icon = (Icon) msg.obj;
                if (icon.v.getTag() != null
                        && icon.f.getAbsolutePath().startsWith(
                                (String) icon.v.getTag())) {
                    // if (icon.v.getTag() != null && ((String)
                    // icon.v.getTag()).equals(icon.f.getAbsolutePath())) {
                    Log.d(TAG, "set image");
                    if (icon.r != USE_RESIZE_IMAGE) {
                        synchronized (ItemIcon.class) {
                            CacheItem item = new CacheItem(icon.b,
                                    icon.f.lastModified());
                            // mBitmapCache.put(icon.f.getAbsolutePath(), item);

                            // google drive rename the thumbnail path
                            String thumbnailPath = (icon.f.getVFieType() == VFileType.TYPE_CLOUD_STORAGE && ((RemoteVFile) (icon.f))
                                    .getStorageType() == StorageType.TYPE_GOOGLE_DRIVE) ? (icon.f
                                    .getAbsolutePath() + File.separator + ((RemoteVFile) (icon.f))
                                    .getFileID()) : icon.f.getAbsolutePath();
                            mBitmapCache.put(thumbnailPath, item);
                        }
                    }
                    BitmapDrawable bd = new BitmapDrawable(icon.b);
                    icon.v.setBackgroundDrawable(bd);
                    icon.v.setImageBitmap(null);

                }
                if (icon.callback != null) {
                    icon.callback.onGetInfo(icon.b);
                }
                return true;
            case MSG_DECODE_ERROR:
                synchronized (mLock) {
                    Log.d(TAG, "MSG_DECODE_ERROR");
                    mDecodeThread = null;
                    startDecodeLocked();
                }
                return true;
            }
            return false;
        }
    }

    public static Bitmap loadResizedBitmap(String filename, int width,
            int height, boolean exact) {

        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        if (options.outHeight > 0 && options.outWidth > 0) {
            int oldWidth = options.outWidth;
            int oldHeight = options.outHeight;
            options.inSampleSize = 1;
            while (options.outWidth > width || options.outHeight > height) {
                options.outWidth /= 2;
                options.outHeight /= 2;
                options.inSampleSize *= 2;
            }

            BitmapFactory.decodeFile(filename, options);
            if (options.inSampleSize > 1
                    && ((options.outWidth == oldWidth) || (options.outHeight == oldHeight))) {
                if (DEBUG) {
                    Log.d(TAG, "set " + filename + " thumbnail = null");
                }
            } else {
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeFile(filename, options);
                if (exact && bitmap != null) {
                    Bitmap oldBitmap = bitmap;
                    int exact_size = width / 2;
                    float scale = Math.max((float) bitmap.getWidth()
                            / exact_size, (float) bitmap.getHeight()
                            / exact_size);
                    if (scale > 1) {
                        bitmap = Bitmap.createScaledBitmap(oldBitmap,
                                (int) Math.ceil((bitmap.getWidth() / scale)),
                                (int) Math.ceil((bitmap.getHeight() / scale)),
                                false);
                        oldBitmap.recycle();
                        oldBitmap = null;
                    }
                }
            }
        }
        return bitmap;
    }

    private class WaitingStack {
        private Stack<Icon> mStackIcon = new Stack<Icon>();
        private Stack<String> mStackPath = new Stack<String>();

        public void push(Icon i) {
            mStackPath.push(i.f.getAbsolutePath());
            mStackIcon.push(i);
        }

        public Icon pop() {
            mStackPath.pop();
            return mStackIcon.pop();
        }

        public Icon remove(int idx) {
            mStackPath.remove(idx);
            return mStackIcon.remove(idx);
        }

        public int indexOf(Icon i) {
            return mStackPath.indexOf(i.f.getAbsolutePath());
        }

        public boolean empty() {
            return mStackIcon.empty();
        }

        public void clear() {
            mStackIcon.clear();
            mStackPath.clear();
        }

    }

    // +++, Johnson, refer to gallery's thumbnail source code
    public Bitmap loadResizedBitmap(String filePath, int targetSize) {

        Bitmap bitmap = onDecodeOriginal(filePath, targetSize);

        if (bitmap == null) {
            return null;
        }

        bitmap = resizeDownAndCropCenter(bitmap, targetSize, true);
        // bitmap = getRoundedCornerBitmap(bitmap, mRoundedCorner);
        return bitmap;
    }

    private Bitmap onDecodeOriginal(String filePath, int targetSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        if(WrapEnvironment.IS_LOW_MEMORY_DEVICE)
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        else
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        // try to decode from JPEG EXIF
        ExifInterface exif = null;
        byte[] thumbData = null;
        try {
            exif = new ExifInterface(filePath);
            if (exif != null) {
                thumbData = exif.getThumbnail();
            }
        } catch (Throwable t) {
            Log.w(TAG, "fail to get exif thumb", t);
        }
        if (thumbData != null) {
            Bitmap bitmap = requestDecodeIfBigEnough(thumbData, options,
                    targetSize);
            if (bitmap != null)
                return bitmap;
        }

        return requestDecode(filePath, options, targetSize);
    }

    private static Bitmap requestDecode(final String filePath, Options options,
            int targetSize) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            FileDescriptor fd = fis.getFD();
            return requestDecode(filePath, fd, options, targetSize);
        } catch (Exception ex) {
            Log.w(TAG, ex);
            return null;
        } finally {
            closeSilently(fis);
        }
    }

    private static Bitmap requestDecode(String filePath, FileDescriptor fd,
            Options options, int targetSize) throws Exception {
        if (options == null)
            options = new Options();

        options.inJustDecodeBounds = true;

        if (!isFileExist(filePath)) {
            return null;
        }
        BitmapFactory.decodeFileDescriptor(fd, null, options);

        if (options.outHeight == -1 || options.outWidth == -1) {
            return requestDecodeStream(filePath, options, targetSize);
        }

        options.inSampleSize = computeSampleSizeLarger(options.outWidth,
                options.outHeight, targetSize);
        options.inJustDecodeBounds = false;

        if (!isFileExist(filePath)) {
            return null;
        }
        Bitmap result = BitmapFactory.decodeFileDescriptor(fd, null, options);
        // We need to resize down if the decoder does not support inSampleSize.
        // (For example, GIF images.)
        if (result != null) {
            result = resizeDownIfTooBig(result, targetSize, true);
        }
        return ensureGLCompatibleBitmap(result);
    }

    private static Bitmap requestDecodeStream(String filePath, Options options,
            int targetSize) {
        if (options == null)
            options = new Options();

        // the inputStream can not use twice in BitmapFactory.decodeStream()
        FileInputStream fis1 = null;
        FileInputStream fis2 = null;
        Bitmap result = null;

        options.inJustDecodeBounds = true;
        try {
            fis1 = new FileInputStream(filePath);
            BitmapFactory.decodeStream(fis1, null, options);
        } catch (Exception ex) {
            Log.w(TAG, ex);
            return null;
        } finally {
            closeSilently(fis1);
        }

        options.inSampleSize = computeSampleSizeLarger(options.outWidth,
                options.outHeight, targetSize);
        options.inJustDecodeBounds = false;

        try {
            fis2 = new FileInputStream(filePath);
            result = BitmapFactory.decodeStream(fis2, null, options);
        } catch (Exception ex) {
            Log.w(TAG, ex);
            return null;
        } finally {
            closeSilently(fis2);
        }
        // We need to resize down if the decoder does not support inSampleSize.
        // (For example, GIF images.)
        if (result != null) {
            result = resizeDownIfTooBig(result, targetSize, true);
        }
        return ensureGLCompatibleBitmap(result);
    }

    // throw exception when unmounted but ExternalStorageState still be mounted
    private static boolean isFileExist(String filePath) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            fis.read();
            return true;
        } catch (Exception ex) {
            Log.w(TAG, ex);
            return false;
        } finally {
            closeSilently(fis);
        }
    }

    /**
     * Decodes the bitmap from the given byte array if the image size is larger
     * than the given requirement.
     * <p/>
     * Note: The returned image may be resized down. However, both width and
     * height must be larger than the <code>targetSize</code>.
     */
    private static Bitmap requestDecodeIfBigEnough(byte[] data,
            Options options, int targetSize) {
        if (options == null)
            options = new Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        if (options.outWidth < targetSize || options.outHeight < targetSize) {
            return null;
        }
        options.inSampleSize = computeSampleSizeLarger(options.outWidth,
                options.outHeight, targetSize);
        options.inJustDecodeBounds = false;
        return ensureGLCompatibleBitmap(BitmapFactory.decodeByteArray(data, 0,
                data.length, options));
    }

    // Resize the bitmap if each side is >= targetSize * 2
    private static Bitmap resizeDownIfTooBig(Bitmap bitmap, int targetSize,
            boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.max((float) targetSize / srcWidth,
                (float) targetSize / srcHeight);
        if (scale > 0.5f)
            return bitmap;
        return resizeBitmapByScale(bitmap, scale, recycle);
    }

    private static Bitmap resizeBitmapByScale(Bitmap bitmap, float scale,
            boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth() && height == bitmap.getHeight())
            return bitmap;
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle)
            bitmap.recycle();
        return target;
    }

    // This computes a sample size which makes the longer side at least
    // minSideLength long. If that's not possible, return 1.
    private static int computeSampleSizeLarger(int w, int h, int minSideLength) {
        int initialSize = Math.max(w / minSideLength, h / minSideLength);
        if (initialSize <= 1)
            return 1;

        return initialSize <= 8 ? prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Returns the previous power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0
    private static int prevPowerOf2(int n) {
        if (n <= 0)
            throw new IllegalArgumentException();
        return Integer.highestOneBit(n);
    }

    private static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != null)
            return bitmap;
        Bitmap newBitmap = bitmap.copy(getConfig(bitmap), false);
        bitmap.recycle();
        return newBitmap;
    }

    public static Bitmap resizeDownAndCropCenter(Bitmap bitmap, int size,
            boolean recycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w == 0 || h == 0)
            return bitmap;

        int minSide = Math.min(w, h);
        if (w == h && minSide <= size)
            return bitmap;
        size = Math.min(size, minSide);

        float scale = Math.max((float) size / bitmap.getWidth(), (float) size
                / bitmap.getHeight());
        Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
        int width = Math.round(scale * bitmap.getWidth());
        int height = Math.round(scale * bitmap.getHeight());
        Canvas canvas = new Canvas(target);
        canvas.translate((size - width) / 2f, (size - height) / 2f);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle)
            bitmap.recycle();
        return target;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config;
        if (WrapEnvironment.IS_LOW_MEMORY_DEVICE) {
            config = Bitmap.Config.RGB_565;
        } else {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    private static void closeSilently(Closeable c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Throwable t) {
            Log.w(TAG, "close fail", t);
        }
    }

    // --- refer to gallery's thumbnail source code

    // +++ for picasa
    public void getPicasaThumbnail(final Icon icon) {
        AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                String path = icon.f.getAbsolutePath();
                try {
                    URI uri = new URI(path);
                    HttpGet httpRequest = new HttpGet(uri);
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = (HttpResponse) httpclient
                            .execute(httpRequest);
                    HttpEntity entity = response.getEntity();
                    BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(
                            entity);
                    InputStream input = bufHttpEntity.getContent();
                    input.mark(input.available());

                    BitmapFactory.Options pre_options = new BitmapFactory.Options();
                    pre_options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(input, null, pre_options);

                    final int REQUIRED_SIZE = 128;

                    int scale = 1;
                    while (pre_options.outWidth / scale / 2 >= REQUIRED_SIZE
                            && pre_options.outHeight / scale / 2 >= REQUIRED_SIZE) {
                        scale *= 2;
                    }

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = scale;
                    options.inJustDecodeBounds = false;

                    input.reset();
                    httpRequest.abort();

                    return BitmapFactory.decodeStream(input, null, options);

                } catch (URISyntaxException e) {
                    Log.e(TAG,
                            "URISyntaxException occurs when calling getPicasaThumbnail");
                } catch (IOException e) {
                    Log.e(TAG,
                            "IOException occurs when calling getPicasaThumbnail");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                if (result != null) {
                    try {
                        if(icon.f.getVFieType()!=VFileType.TYPE_SAMBA_STORAGE) {
							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							result.compress(Bitmap.CompressFormat.PNG, 100, stream);
							byte[] bitmapStream = stream.toByteArray();
							stream.close();
							Thumbnail.setThumbnailAndTime(mCr,
									icon.f.getAbsolutePath(), bitmapStream,
									icon.f.lastModified());
						}
                        icon.b = result;

                        mHandler.sendMessage(mHandler.obtainMessage(
                                MSG_DECODE_FINISH, icon));
                    } catch (IOException e) {
                        Log.e(TAG,
                                "IOException occurs when calling getPicasaThumbnail");
                    }

                } else {
                    Log.d(TAG, "bitmap is null when calling getPicasaThumbnail");
                }
            }
        };
        task.execute();
    }

    // --

    private int getImageOrientation(String imagePath) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(imagePath);
        } catch (IOException e) {
            Log.w(TAG,
                    "When calling getImageOrientation(), we cannot get the image's exif at "
                            + imagePath);
        }
        if (exif == null) {
            return -1;
        }

        String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        // Tim
        // Crash found from Firebase: prevent excute method with null object
        if(orientation == null) return -1;
        if (orientation.equals(ExifInterface.ORIENTATION_NORMAL)) {
            return 0;
        } else if (orientation.equals(ExifInterface.ORIENTATION_ROTATE_90 + "")) {
            return 90;
        } else if (orientation
                .equals(ExifInterface.ORIENTATION_ROTATE_180 + "")) {
            return 180;
        } else if (orientation
                .equals(ExifInterface.ORIENTATION_ROTATE_270 + "")) {
            return 270;
        } else {
            return -1;
        }
    }

    private Bitmap getNewRotatedImage(Bitmap bitmap, int degress) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degress);

        if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0)
            return bitmap;
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    public Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == 0 || height == 0)
            return bitmap;

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), getConfig(null));
        Canvas canvas = new Canvas(output);
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        final int color = 0xff424242;
        final Paint paint = new Paint();

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // final Rect block = new Rect(0, (int)roundPx, width, height);
        // canvas.drawRect(block, paint);
        final RectF rectF = new RectF(0, 0, width, height);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        final Rect src = new Rect(0, 0, width, height);
        final Rect dst = src;
        canvas.drawBitmap(bitmap, src, dst, paint);
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        return output;
    }

    private Bitmap retrieveAlbumArt(Context context, String absolutePath,
            int targetSize) {
        // Log.v(TAG, "retrieveAlbumArt=" + absolutePath);
        absolutePath = FileUtility.changeToStoragePath(absolutePath);
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] cursor_cols = { MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID };
        final Cursor cursor = context.getContentResolver().query(uri,
                cursor_cols, MediaStore.Audio.Media.DATA + "=?",
                new String[] { absolutePath }, null);

        Bitmap bitmap = null;
        if (cursor != null ) {
            if(cursor.moveToFirst()) {
                Long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                cursor.close();
                Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId);
                // Log.v(TAG, "albumArtUri=" + albumArtUri);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(
                            context.getContentResolver(), albumArtUri);
                    // if (bitmap != null) {
                    // Log.v(TAG, "bitmap.getWidth=" + bitmap.getWidth());
                    // Log.v(TAG, "bitmap.getHeight=" + bitmap.getHeight());
                    // }
                    if (bitmap != null) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, targetSize,
                                targetSize, true);
                    }
                } catch (FileNotFoundException exception) {
                    exception.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e){
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        // if (bitmap != null) {
        // bitmap = getRoundedCornerBitmap(bitmap, mRoundedCorner);
        // }
        return bitmap;
    }

    private Drawable getIconDrawableByPackageName(String packageName) {
        PackageManager packageManager = mContext.getPackageManager();
        Drawable icon = null;
        try {
            return packageManager.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
