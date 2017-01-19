
package com.asus.filemanager.loader;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.dialog.AlbumPicker;
import com.asus.filemanager.utility.BucketEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class AlbumPickerLoader extends AsyncTaskLoader<BucketEntry[]> {

    private static final String TAG = "AlbumPickerLoader";
    private static final boolean DEBUG = true;
    private static final int INDEX_BUCKET_DATA = 0;
    private static final int INDEX_BUCKET_ID = 1;
    private static final int INDEX_BUCKET_NAME = 2;
    private static final String BUCKET_GROUP_BY = "1) GROUP BY 1,(2";
    private static final String BUCKET_ORDER_BY = "MAX(datetaken) DESC";

    private boolean mIsAlbumMode = true;
    private boolean[] mMountDevice;
    private boolean mEnablePicasa = false;
    private BucketEntry[] mBucketEntries = null;
    private Context mContext;
    private Cursor mCursor;
    private int mScanMode;
    private String mAlbumID;

    private  String mOrderClause = ImageColumns.DATE_TAKEN + " DESC, " + ImageColumns._ID + " DESC";

    static final String[] PROJECTION =  {
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    };

    private String[] mEnviroment = null;

    static final String[] PROJECTION_FOR_BUCKET =  {
        MediaStore.Images.Media.DATA,
    };

    // +++ for Picasa
    public static final String ACTION_SYNC = "com.android.gallery3d.picasa.action.SYNC";
    public static final String AUTHORITY = "com.android.gallery3d.picasa.contentprovider";
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
    public static final Uri PHOTOS_URI = Uri.withAppendedPath(BASE_URI, "photos");
    public static final Uri ALBUMS_URI = Uri.withAppendedPath(BASE_URI, "albums");

    public static final String[] PICASA_ALBUM_PROJECTION = {
        "_id",
        "title",
        "num_photos",
        "thumbnail_Url",
        "date_edited",
    };

    private static final String[] PICASA_PHOTO_PROJECTION = {
        "_id",
        "album_id",
        "thumbnail_Url",
        "date_edited",
    };
    // ---

    public AlbumPickerLoader(Context context, boolean[] isMount, boolean isAlbumMode, String albumID, int scanMode, boolean enablePiasa) {
        super(context);
        if (DEBUG) {
            Log.d(TAG, "AlbumPickerLoader init");
        }
        mContext = context;
        mMountDevice = isMount;
        mIsAlbumMode = isAlbumMode;
        mAlbumID = albumID;
        mScanMode = scanMode;
        mEnablePicasa = enablePiasa;

        mEnviroment = ((FileManagerApplication)context.getApplicationContext()).getStorageVolumePaths();
    }

    @Override
    public BucketEntry[] loadInBackground() {
        if (DEBUG) {
            Log.d(TAG, "loadInBackground");
        }
        if (mScanMode == AlbumPicker.SELECT_ALBUM_MODE) {
            int total_photos_number = 0;
            long lastModifiedTime = 0;
            BucketEntry[] localFiles = null;

            mCursor = null;
            mCursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, BUCKET_GROUP_BY, null, BUCKET_ORDER_BY);
            if (mCursor != null) {
                mCursor.moveToFirst();

                localFiles = loadBucketEntries(mCursor);
                if (localFiles != null) {
                    int startNum = (mIsAlbumMode ? 0 : 1);
                    for (int i=startNum ; i<localFiles.length ; i++) {
                        File file = new File(localFiles[i].data);
                        localFiles[i].lastModifiedTime = file.getParentFile().lastModified();
                        if (localFiles[i].lastModifiedTime > lastModifiedTime) {
                            lastModifiedTime = localFiles[i].lastModifiedTime;
                        }

                        mCursor.close();
                        mCursor = null;
                        mCursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                PROJECTION_FOR_BUCKET, MediaStore.Images.Media.BUCKET_ID + "=?", new String[]{localFiles[i].bucketId}, mOrderClause);
                        if (mCursor != null) {
                            localFiles[i].number = mCursor.getCount();
                            total_photos_number += localFiles[i].number;
                            mCursor.moveToNext();
                            localFiles[i].data = mCursor.getString(0);
                        }
                    }

                    // consider all photos case, update all photos album data
                    if (!mIsAlbumMode && localFiles.length > 0) {
                        localFiles[0].bucketId = AlbumPicker.KEY_SELECT_ALL_PHOTOS;
                        localFiles[0].data = localFiles[1].data;
                        localFiles[0].number = total_photos_number;
                        localFiles[0].lastModifiedTime = lastModifiedTime + 1; // time order: new to old
                    }

                    Arrays.sort(localFiles, new FileUtils.CompratorByLastModified());
                } else {
                    Log.d(TAG, "loadBucketEntries return null localFiles");
                }

                mCursor.close();
                mCursor = null;
            } else {
                Log.d(TAG, "get cursor is null when calling loadInBackground at scan mode: " + mScanMode);
            }

            BucketEntry[] picasaFiles = null;
            if (mEnablePicasa) {
                picasaFiles = getPicasaAlbums();

                if (!mIsAlbumMode && picasaFiles != null && picasaFiles.length > 0) {
                    for(int i=0 ; i<picasaFiles.length ; i++) {
                        total_photos_number += picasaFiles[i].number;
                    }
                    // create all photos album
                    if (localFiles == null || localFiles.length == 0) {
                        BucketEntry bucketEntryTemp = new BucketEntry("", "", "All photos", BucketEntry.PICASAFILE);
                        localFiles = new BucketEntry[1];
                        localFiles[0] = bucketEntryTemp;
                        localFiles[0].bucketId = AlbumPicker.KEY_SELECT_ALL_PHOTOS;
                        localFiles[0].data = picasaFiles[0].data;
                        localFiles[0].number = total_photos_number;
                        localFiles[0].lastModifiedTime = 0; // time order: new to old
                    // update total number of photos which include local and picasa
                    } else {
                        if (localFiles[0].number != total_photos_number) {
                            localFiles[0].number = total_photos_number;
                        }
                    }
                }
            }

            if (localFiles != null && picasaFiles != null) {
                mBucketEntries = new BucketEntry[localFiles.length + picasaFiles.length];
                System.arraycopy((Object)localFiles, 0, (Object)mBucketEntries, 0, localFiles.length);
                System.arraycopy((Object)picasaFiles, 0, (Object)mBucketEntries, localFiles.length, picasaFiles.length);
            } else if (localFiles != null) {
                mBucketEntries = localFiles;
            } else if (picasaFiles != null) {
                mBucketEntries = picasaFiles;
            }


        } else if (mScanMode == AlbumPicker.SELECT_PHOTO_MODE) {
            BucketEntry[] localFiles = null;
            mCursor = null;

            if (mAlbumID.equals(AlbumPicker.KEY_SELECT_ALL_PHOTOS)) {
                mCursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        PROJECTION, null, null, mOrderClause);
            } else {
                mCursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        PROJECTION, MediaStore.Images.Media.BUCKET_ID + "=?", new String[]{mAlbumID}, mOrderClause);
            }
            if (mCursor != null && mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                localFiles = loadItemEntries(mCursor);

                mCursor.close();
                mCursor = null;
            } else {
                Log.d(TAG, "get cursor is null when calling loadInBackground at scan mode: " + mScanMode);
            }

            BucketEntry[] picasaFiles = null;
            if (mEnablePicasa) {
                picasaFiles = getPicasaPhotos(mAlbumID);
            }

            if (localFiles != null && picasaFiles != null) {
                mBucketEntries = new BucketEntry[localFiles.length + picasaFiles.length];
                System.arraycopy((Object)localFiles, 0, (Object)mBucketEntries, 0, localFiles.length);
                System.arraycopy((Object)picasaFiles, 0, (Object)mBucketEntries, localFiles.length, picasaFiles.length);
            } else if (localFiles != null) {
                mBucketEntries = localFiles;
            } else if (picasaFiles != null) {
                mBucketEntries = picasaFiles;
            }

        } else if (mScanMode == AlbumPicker.SELECT_PICASA_PHOTO_MODE) {
            if (mAlbumID != null) {
                mBucketEntries = getPicasaPhotos(mAlbumID);
            }
        }

        return mBucketEntries;
    }

    @Override
    public void deliverResult(BucketEntry[] data) {
        if (DEBUG) {
            Log.d(TAG, "deliverResult");
        }
        if (isReset()) {
            if (data != null) {
                onReleaseResources(data);
            }
        }
        BucketEntry[] oldFiles = mBucketEntries;
        mBucketEntries = data;

        if (isStarted()) {
            super.deliverResult(data);
        }

        if (oldFiles != null) {
            onReleaseResources(oldFiles);
        }
    }

    @Override
    protected void onStartLoading() {
        if (DEBUG) {
            Log.d(TAG, "onStartLoading");
        }
        if (mBucketEntries != null) {
            deliverResult(mBucketEntries);
        }

        if (takeContentChanged() || mBucketEntries == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        if (DEBUG) {
            Log.d(TAG, "onStopLoading");
        }
        cancelLoad();
    }

    @Override
    public void onCanceled(BucketEntry[] data) {
        super.onCanceled(data);
        if (DEBUG) {
            Log.d(TAG, "onStopLoading");
        }
        onReleaseResources(mBucketEntries);
    }

    @Override
    public void onReset() {
        super.onReset();
        if (DEBUG) {
            Log.d(TAG, "onReset");
        }
        onStopLoading();

        if (mBucketEntries != null) {
            onReleaseResources(mBucketEntries);
            mBucketEntries = null;
        }
    }

    protected void onReleaseResources(BucketEntry[] files) {
    }

    private BucketEntry[] loadBucketEntries(Cursor cursor) {
        ArrayList<BucketEntry> buffer = new ArrayList<BucketEntry>();

        // consider all photos case
        if (!mIsAlbumMode && cursor.getCount() > 0) {
            BucketEntry entry = new BucketEntry("", "", "All photos", BucketEntry.LOCALFILE);
            buffer.add(entry);
        }

        if (cursor.getCount() > 0) {
            do {
                BucketEntry entry = new BucketEntry(
                        cursor.getString(INDEX_BUCKET_DATA),
                        cursor.getString(INDEX_BUCKET_ID),
                        cursor.getString(INDEX_BUCKET_NAME),
                        BucketEntry.LOCALFILE);
                for (int j = 0 ; j < mEnviroment.length ; j++) {
                    if (mMountDevice[j]) {
                        if (entry.data.startsWith(mEnviroment[j])) {
                            if (!buffer.contains(entry)) {
                                buffer.add(entry);
                            }
                        }
                    }
                }
            } while (cursor.moveToNext());
        }

        // consider all photos case
        if (!mIsAlbumMode && buffer.size() == 1) {
            buffer.remove(0);
        }

        return buffer.toArray(new BucketEntry[buffer.size()]);
    }

    private BucketEntry[] loadItemEntries(Cursor cursor) {
        ArrayList<BucketEntry> buffer = new ArrayList<BucketEntry>();

        do {
            BucketEntry entry = new BucketEntry(cursor.getString(INDEX_BUCKET_DATA), "", "", BucketEntry.LOCALFILE);
            for (int j = 0 ; j < mEnviroment.length ; j++) {
                if (mMountDevice[j]) {
                    if (entry.data.startsWith(mEnviroment[j])) {
                        buffer.add(entry);
                    }
                }
            }
        } while (cursor.moveToNext());

        return buffer.toArray(new BucketEntry[buffer.size()]);
    }

    private static class FileUtils {
        static class CompratorByLastModified implements Comparator<Object> {

            @Override
            public int compare(Object o1, Object o2) {
                BucketEntry b1 = (BucketEntry)o1;
                BucketEntry b2 = (BucketEntry)o2;

                long diff = b1.lastModifiedTime - b2.lastModifiedTime;
                // new to old
                if (diff > 0) {
                    return -1;
                } else if (diff == 0) {
                    return 0;
                } else {
                    return 1;
                }
            }

        }
    }

    // +++ for Picasa
    private BucketEntry[] getPicasaAlbums() {
        ArrayList<BucketEntry> buffer = new ArrayList<BucketEntry>();

        ContentResolver cr = mContext.getContentResolver();
        if (cr == null) {
            throw new RuntimeException("cannot get content resolver");
        }

        Cursor cursor = null;
        try {
            cursor = cr.query(ALBUMS_URI, PICASA_ALBUM_PROJECTION, null, null, null);
        } catch (SecurityException e) {
            Log.w(TAG, "can not access gallery db becase " + e.getMessage());
        }

        if (cursor == null) {
            return null;
        }

        try {
            int number;
            String id, title, thumbnailURL;
            Long edited_data;
            while(cursor.moveToNext()) {
                id = cursor.getString(cursor.getColumnIndex(PICASA_ALBUM_PROJECTION[0]));
                title = cursor.getString(cursor.getColumnIndex(PICASA_ALBUM_PROJECTION[1]));
                number = cursor.getInt(cursor.getColumnIndex(PICASA_ALBUM_PROJECTION[2]));
                thumbnailURL = cursor.getString(cursor.getColumnIndex(PICASA_ALBUM_PROJECTION[3]));
                edited_data = cursor.getLong(cursor.getColumnIndex(PICASA_ALBUM_PROJECTION[4]));
                if (number > 0) {
                    buffer.add(new BucketEntry(id, number, title, thumbnailURL, edited_data));
                }
            }
        } finally {
            cursor.close();
            cursor = null;
        }
        return buffer.toArray(new BucketEntry[buffer.size()]);
    }

    private BucketEntry[] getPicasaPhotos(String albumId) {
        ArrayList<BucketEntry> buffer = new ArrayList<BucketEntry>();

        ContentResolver cr = mContext.getContentResolver();
        if (cr == null) {
            throw new RuntimeException("cannot get content resolver");
        }

        Cursor cursor = null;
        try {
            if (mAlbumID.equals(AlbumPicker.KEY_SELECT_ALL_PHOTOS)) {
                cursor = cr.query(PHOTOS_URI, PICASA_PHOTO_PROJECTION, null, null, null);
            } else {
                cursor = cr.query(PHOTOS_URI, PICASA_PHOTO_PROJECTION, PICASA_PHOTO_PROJECTION[1] + "=?", new String[]{albumId}, null);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "can not access gallery db becase " + e.getMessage());
        }

        if (cursor == null) {
            return null;
        }

        try {
            String photoId, thumbnailURL;
            Long edited_data;
            while(cursor.moveToNext()) {
                photoId = cursor.getString(cursor.getColumnIndex(PICASA_PHOTO_PROJECTION[0]));
                thumbnailURL = cursor.getString(cursor.getColumnIndex(PICASA_PHOTO_PROJECTION[2]));
                edited_data = cursor.getLong(cursor.getColumnIndex(PICASA_PHOTO_PROJECTION[3]));
                buffer.add(new BucketEntry(photoId, -1, "", thumbnailURL, edited_data));
            }
        } finally {
            cursor.close();
            cursor = null;
        }

        return buffer.toArray(new BucketEntry[buffer.size()]);
    }
    // ---
}
