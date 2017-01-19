
package com.asus.filemanager.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.DebugLog;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.IconUtility.ThumbnailItem;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;

public class ProviderUtility {
    private static final boolean DEBUG = ConstantsUtil.DEBUG;
    private static final String TAG = "ProviderUtility";

    public static String CONTENT_URI = "com.asus.filemanager.provider";

    public static class ShortCutResult {
        public String mLabel;
        public String mFile_path;

        public ShortCutResult(String label, String file_path) {
            mLabel = label;
            mFile_path = file_path;
        }
    }

    public static class ShortCut {
        public static final Uri CONTENT_URI = Uri.parse("content://com.asus.filemanager.provider/shortcut");

        public static final String _ID = "_id";
        public static final String LABEL = "label";
        public static final String FILE_PATH = "file_path";

        private static final int LABEL_COLUMNINDEX = 1;
        private static final int FILE_PATH_COLUMNINDEX = 2;

        public static ShortCutResult[] getShortCut(Context context) {

            ShortCutResult[] shortcutResult = null;

            ContentResolver cr = context.getContentResolver();

            Cursor cursor = cr.query(ProviderUtility.ShortCut.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int count = cursor.getCount();
                        Log.d(TAG, "ShortCut count : " + count);
                        shortcutResult = new ShortCutResult[count];

                        for (int i = 0; i < count; i++) {
                            shortcutResult[i] = new ShortCutResult(cursor.getString(LABEL_COLUMNINDEX), cursor.getString(FILE_PATH_COLUMNINDEX));
                            cursor.moveToNext();
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            return shortcutResult;
        }
    }

    public static class Thumbnail {
        public static final Uri CONTENT_URI = Uri.parse("content://com.asus.filemanager.provider/thumbnail");

        public static final String _ID = "_id";
        public static final String FILE_PATH = "file_path";
        public static final String BITMAP = "bitmap";
        public static final String MODIFY_TIME = "modify_time";

        private static final int FILE_PATH_COLUMNINDEX = 1;
        private static final int BITMAP_COLUMNINDEX = 2;
        private static final int MODIFY_TIME_COLUMNINDEX = 3;

        synchronized public static ThumbnailItem getThumbnailItem(ContentResolver cr, String path) {
            ThumbnailItem thumbnailItem = new ThumbnailItem(null, -1);

            String selection = FILE_PATH + " =?";

            Cursor cursor = cr.query(CONTENT_URI, null, selection, new String[] {
                    path
            }, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        byte[] stream = cursor.getBlob(BITMAP_COLUMNINDEX);
                        thumbnailItem.thumbnail = BitmapFactory.decodeByteArray(stream, 0, stream.length);
                        thumbnailItem.modifyTime = cursor.getLong(MODIFY_TIME_COLUMNINDEX);
                    } else {
                        Log.d(TAG, "not find thumbnai");
                    }
                } catch(Exception e) {
                    Log.d(TAG, "Exception cursor : " + e.toString());
                    cursor.close();
                    cursor = null;
                    return thumbnailItem;
                }
                finally {
                    if (cursor != null) {
                        cursor.close();
                        cursor = null;
                    }
                }
            }

            return thumbnailItem;
        }

        synchronized public static Uri setThumbnailAndTime(ContentResolver cr, String path, byte[] bitmapStream, long time) {
            Uri result = null;
            String where = FILE_PATH + " =?";
            String[] selectionArgs = new String[] {
                    path
            };
            try {
                ContentValues values = new ContentValues(2);
                values.put(FILE_PATH, path);
                values.put(BITMAP, bitmapStream);
                values.put(MODIFY_TIME, time);
                if (cr.update(CONTENT_URI, values, where, selectionArgs) < 1) {
                    result = cr.insert(CONTENT_URI, values);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "insert bitmap error : " + path);
            }

            return result;
        }

        synchronized public static boolean updateDb(Context context) {
            boolean isClear = false;
            ContentResolver cr = context.getContentResolver();

            Cursor cursor = cr.query(CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                if (DEBUG) {
                    Log.d(TAG, "updateDb Thumbnail : cursor count : " + cursor.getCount());
                }
                try {
                    if (cursor.getCount() > 5000) {
                        isClear = cr.delete(CONTENT_URI, null, null) == -1 ? false : true;
                    }
                } finally {
                    cursor.close();
                }
            }

            return isClear;
        }
    }

    public static class MediaFiles {
        public static final Uri CONTENT_URI = Uri.parse("content://com.asus.filemanager.provider/mediafiles");

        public static final String DATA = "_data";
        public static final String SIZE = "_size";
        public static final String PARENT = "parent";
        public static final String DATE_ADDED = "date_added";
        public static final String DATE_MODIFIED = "date_modified";
        public static final String MIME_TYPE = "mime_type";
        public static final String TITLE = "title";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String MEDIA_TYPE = "media_type";

        public static Uri insertFile(ContentResolver cr, VFile file) {
            Uri result = null;

            String mime = reflectionApis.mediaFile_getMimeTypeForFile(file.getName());
            if (mime == null) {
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getExtensiontName().toLowerCase());
            }

            try {
                ContentValues values = new ContentValues();
                values.put(DATA, file.getAbsolutePath());
                values.put(SIZE, file.length());
                if (mime != null) {
                    values.put(MIME_TYPE, mime);
                }
                values.put(TITLE, file.getName());
                values.put(DISPLAY_NAME, file.getName());
                result = cr.insert(CONTENT_URI, values);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "insert mediafile error : " + file.getAbsolutePath());
            }
            if (DEBUG && result != null) {
                Log.d(TAG, "insert mediafiles : " + result.toString());
            }
            return result;
        }

        public static Uri getMediaFileUri(ContentResolver cr, VFile file) {
            Uri result = null;

            String selection = DATA + " =?";

            Cursor cursor = cr.query(CONTENT_URI, null, selection, new String[] {
                    file.getAbsolutePath()
            }, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        result = Uri.parse(CONTENT_URI + "/" + cursor.getInt(0));
                    } else {
                        if (DEBUG) {
                            Log.d(TAG, "not find mediaFileUri : " + file);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            if (result == null) {
                result = insertFile(cr, file);
            }

            return result;
        }

        public static int updateDb(Context context, String path) {
            int count = 0;
            ContentResolver cr = context.getContentResolver();

            if (path != null) {
                String where = DATA + " LIKE ?";
                String[] selectionArgs = {
                        path + "/%"
                };
                count = cr.delete(CONTENT_URI, where, selectionArgs);
                if (DEBUG) {
                    Log.d(TAG, "updateDb MediaFiles : path = " + path + ", delete count = " + count);
                }

            }
            return count;
        }
    }

    public static class ShareFiles {
        public static final Uri CONTENT_URI = Uri.parse("content://com.asus.filemanager.provider/sharefiles");

        public static final String _ID = "_id";
        public static final String FILE_PATH = "file_path";
        public static final String SHARE_TYPE = "share_type";

        private static final int FILE_PATH_COLUMNINDEX = 1;
        private static final int SHARE_TYPE_COLUMNINDEX = 2;

        public synchronized static Uri insertShareFile(ContentResolver cr, String path, int type) {
            Uri result = null;
            try {
                ContentValues values = new ContentValues(2);
                values.put(FILE_PATH, path);
                values.put(SHARE_TYPE, type);
                result = cr.insert(CONTENT_URI, values);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "insert bitmap error : " + path);
            }
            return result;
        }

        public synchronized static int removeShareFile(ContentResolver cr, String path, int type) {
            String where = FILE_PATH + "=?" + " AND " + SHARE_TYPE + "=?";

            String[] selectionArgs = {
                    path, String.valueOf(type)
            };
            int result = cr.delete(CONTENT_URI, where, selectionArgs);
            return result;
        }

        public static int getShareFile(ContentResolver cr, String path) {
            String selection = FILE_PATH + " =?";
            String[] selectionArgs = new String[] {
                    path
            };
            int result = -1;

            Cursor cursor = cr.query(CONTENT_URI, null, selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        result = cursor.getInt(SHARE_TYPE_COLUMNINDEX);
                    }
                } finally {
                    cursor.close();
                }
            }

            return result;
        }

        public static String[] getShareFiles(ContentResolver cr, int type) {
            String selection = SHARE_TYPE + " =?";
            String[] selectionArgs = new String[] {
                    String.valueOf(type)
            };
            String[] result = null;

            Cursor cursor = cr.query(CONTENT_URI, null, selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        result = new String[cursor.getCount()];
                        int i = 0;
                        while (cursor.moveToNext()) {
                            result[i] = cursor.getString(FILE_PATH_COLUMNINDEX);
                            i++;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            return result;
        }
    }

    public static class MountAccounts {
        public static final Uri CONTENT_URI = Uri.parse("content://com.asus.filemanager.provider/mountaccounts");

        public static final String _ID = "_id";
        public static final String ACCOUNT_NAME = "account_name";
        public static final String ACCOUNT_TYPE = "account_type";
        public static final String AUTHTOKEN_TYPE = "authtoken_type";

        public synchronized static Uri insertAccounts(ContentResolver cr, String account_type, String authtoken_type, String account_name) {
            Uri result = null;
            if (authtoken_type == null)
                authtoken_type = "";

            String selection = ACCOUNT_NAME + "=?" + " AND " + ACCOUNT_TYPE + "=?" + " AND " + AUTHTOKEN_TYPE + "=?";
            String[] selectionArgs = { account_name, account_type, authtoken_type };

            Cursor cursor = null;
            try {
                ContentValues values = new ContentValues(3);
                values.put(ACCOUNT_NAME, account_name);
                values.put(ACCOUNT_TYPE, account_type);
                values.put(AUTHTOKEN_TYPE, authtoken_type);

                cursor = cr.query(CONTENT_URI, null, selection, selectionArgs, null);
                if (cursor == null || cursor.getCount() == 0) {
                    result = cr.insert(CONTENT_URI, values);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "insert mount account error : " + account_name);
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                   cursor.close();
                }
            }
            return result;
        }

        public synchronized static int removeAccounts(ContentResolver cr, String account_type, String authtoken_type, String account_name) {
            String where = ACCOUNT_NAME + "=?" + " AND " + ACCOUNT_TYPE + "=?" + " AND " + AUTHTOKEN_TYPE + "=?";

            String[] selectionArgs = { account_name, account_type, authtoken_type };
            int result = cr.delete(CONTENT_URI, where, selectionArgs);
            return result;
        }

        public static boolean isMounted(ContentResolver cr, String account_type, String authtoken_type, String account_name) {
            if (authtoken_type == null)
                authtoken_type = "";

            String selection = ACCOUNT_NAME + "=?" + " AND " + ACCOUNT_TYPE + "=?" + " AND " + AUTHTOKEN_TYPE + "=?";
            String[] selectionArgs = { account_name, account_type, authtoken_type };
            boolean result = false;

            Cursor cursor = cr.query(CONTENT_URI, null, selection, selectionArgs, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    result = true;
                }
                cursor.close();
            }

            return result;
        }

    }

    public static class FavoriteFiles {
        public static final Uri CONTENT_URI = Uri.parse("content://com.asus.filemanager.provider/favoritefiles");

        public static final String _ID = "_id";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String DATA = "_data";

        public synchronized static Uri insertFile(ContentResolver cr, String _display_name, String _data) {
            Uri result = null;
            if (_display_name == null || _data == null)
                return null;

            String selection = DATA + "=?";
            String[] selectionArgs = { _data };

            try {
                ContentValues values = new ContentValues(2);
                values.put(DISPLAY_NAME, _display_name);
                values.put(DATA, _data);

                if (cr.update(CONTENT_URI, values, selection, selectionArgs) < 1) {
                    result = cr.insert(CONTENT_URI, values);
                } else {
                    result = (new Uri.Builder()).build();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "insert favorite files error : " + _data);
            }
            return result;
        }

        public synchronized static int removeFile(ContentResolver cr, VFile file) {
            if (file == null)
                return -1;

            String where = DATA + "=?";
            String path;
            try {
                path = FileUtility.getCanonicalPathForUser(file.getCanonicalPath());
            } catch (IOException e2) {
                path = file.getAbsolutePath();
                e2.printStackTrace();
            }
            String[] selectionArgs = { path };

            int result = cr.delete(CONTENT_URI, where, selectionArgs);
            return result;
        }

        public synchronized static int removeFiles(ContentResolver cr, VFile[] files) {
            if (files == null)
                return -1;

            String where = DATA + " in (";

            String[] paths = new String [files.length];
            String path;
            int index = 0;
            for(VFile file: files) {
                try {
                    path = FileUtility.getCanonicalPathForUser(file.getCanonicalPath());
                } catch (IOException e2) {
                    path = file.getAbsolutePath();
                    e2.printStackTrace();
                }
                where += "?,";
                paths[index] = path;
                ++index;
            }
            where += "'')";

            int result = cr.delete(CONTENT_URI, where, paths);
            return result;
        }

        public static boolean exists(ContentResolver cr, String _display_name) {
            boolean result = false;
            if (_display_name == null)
                return true;

            String selection = DISPLAY_NAME + "=?";
            String[] selectionArgs = { _display_name };

            Cursor cursor = cr.query(CONTENT_URI, null, selection, selectionArgs, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    result = true;
                }
                cursor.close();
            }

            return result;
        }

        public static String getFavoriteNameByPath(ContentResolver cr, String path) {
            String favoriteName = null;
            String selection = DATA + "=?";
            String[] selectionArgs = { path };
            Cursor cursor = cr.query(CONTENT_URI, null, selection, selectionArgs, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                favoriteName = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                cursor.close();
            }
            return favoriteName;
        }

        public static boolean exists(ContentResolver cr, VFile file) {
            boolean result = false;
            if (file == null)
                return true;

            String selection = DATA + "=?";
            String[] selectionArgs = { file.getAbsolutePath() };

            Cursor cursor = cr.query(CONTENT_URI, null, selection, selectionArgs, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    result = true;
                }
                cursor.close();
            }

            return result;
        }

        public static int getCount(ContentResolver cr, boolean bShowHidden) {
            return getFiles(cr, bShowHidden).size();
        }

        public static ArrayList<LocalVFile> getFiles(ContentResolver cr, boolean bShowHidden) {
            ArrayList<LocalVFile> files = new ArrayList<LocalVFile>();

            Cursor cursor = cr.query(CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                while(cursor.moveToNext()) {
                    // Transform canonical path to symbolic path
                    LocalVFile file = new LocalVFile(FileUtility.changeToSdcardPath(cursor.getString(cursor.getColumnIndex(DATA))));
                    file.setFavoriteName(cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
                    if (!bShowHidden){
                        if (!file.isHidden()){
                            files.add(file);
                        }
                    }else{
                        files.add(file);
                    }
                 }
                cursor.close();
            }

            return files;
        }

        public static String[] getPaths(ContentResolver cr) {
            ArrayList<String> paths = new ArrayList<String>();

            Cursor cursor = cr.query(CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                while(cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(DATA));
                    paths.add(path);
                 }
                cursor.close();
            }

            return paths.toArray(new String[paths.size()]);
        }
    }

    public static class RecentlyOpen {

        // Notes:
        // FIXME:
        // the following path should be change to the first one:
        // 1. "/storage/emulated/0/path" (used in db)
        // 2. "/storage/emulated/legacy/path"
        // 3. "/sdcard/path"
        //
        // the public method always need to translate the input data:
        // data = FileUtility.changeToStoragePath(data);

        public static final Uri CONTENT_URI = Uri.parse("content://com.asus.filemanager.provider/recentlyopen");

        public static final String TABLE_NAME = "recentlyopen";

        public static final String _ID = "_id";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String DATA = "_data";
        public static final String DATE_OPENED = "date_opened";

        public static List<LocalVFile> getFiles(ContentResolver cr, boolean isShowHidden) {
            List<LocalVFile> files = new ArrayList<LocalVFile>();
            List<String> deletedFiles = new ArrayList<String>();
            Cursor cursor = null;
            try {
                cursor = queryAll(cr);
                if (cursor != null && cursor.moveToFirst()) {
                    String filePath = null;
                    LocalVFile file = null;
                    do {
                        // Don't use FileUtility.changeToSdcardPath, performance issue!
                        // filePath = FileUtility.changeToSdcardPath(cursor.getString(cursor.getColumnIndex(DATA)));
                        filePath = cursor.getString(cursor.getColumnIndex(DATA));
                        file = new LocalVFile(filePath);
                        if (file.exists()) {
                            if (!file.isHidden() ||
                                    (isShowHidden && !FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(file))) {
                                file.setLastModified(cursor.getLong(cursor.getColumnIndex(DATE_OPENED)));
                                files.add(file);
                            }
                        } else {
                            deletedFiles.add(filePath);
                        }
                    } while (cursor.moveToNext());
                    for (String deletedFile: deletedFiles) {
                        delete(cr, deletedFile);
                    }
                }
                return files;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        public static long getCount(ContentResolver cr) {
            long count = 0;
            Cursor cursor = null;
            try {
                cursor = cr.query(CONTENT_URI, new String[] { "COUNT(" + _ID + ")" }, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getLong(0);
                }
                return count;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        public static Uri insert(ContentResolver cr, String dispName, String data, long dateOpened) {
            // FIXME:
            // the following path should be change to the first one:
            // 1. "/storage/emulated/0/path" (used in db)
            // 2. "/storage/emulated/legacy/path"
            // 3. "/sdcard/path"
            data = FileUtility.changeToStoragePath(data);
            Log.d(TAG, "insert: " + data);
            ContentValues values = new ContentValues();
            values.put(DISPLAY_NAME, dispName);
            values.put(DATA, data);
            values.put(DATE_OPENED, dateOpened);
            return cr.insert(CONTENT_URI, values);
        }

        public static int update(ContentResolver cr, String dispName, String data, long dateOpened) {
            // FIXME:
            // the following path should be change to the first one:
            // 1. "/storage/emulated/0/path" (used in db)
            // 2. "/storage/emulated/legacy/path"
            // 3. "/sdcard/path"
            data = FileUtility.changeToStoragePath(data);
            Log.d(TAG, "update: " + data);
            ContentValues values = new ContentValues();
            values.put(DISPLAY_NAME, dispName);
            values.put(DATA, data);
            values.put(DATE_OPENED, dateOpened);
            return cr.update(CONTENT_URI, values, DATA + "=?", new String[] { data });
        }

        public static int delete(ContentResolver cr, String data) {
            // FIXME:
            // the following path should be change to the first one:
            // 1. "/storage/emulated/0/path" (used in db)
            // 2. "/storage/emulated/legacy/path"
            // 3. "/sdcard/path"
            data = FileUtility.changeToStoragePath(data);
            Log.d(TAG, "delete: " + data);
            return cr.delete(CONTENT_URI, DATA + "=?", new String[] { data });
        }

        public static int rename(ContentResolver cr, String data1, String dispName2, String data2) {
            // FIXME:
            // the following path should be change to the first one:
            // 1. "/storage/emulated/0/path" (used in db)
            // 2. "/storage/emulated/legacy/path"
            // 3. "/sdcard/path"
            data1 = FileUtility.changeToStoragePath(data1);
            data2 = FileUtility.changeToStoragePath(data2);
            Log.d(TAG, "rename: " + data1 + " -> " + data2);
            if (!exist(cr, data1)) {
                Log.w(TAG, "file is not exist: " + data1);
                return 0;
            }
            ContentValues values = new ContentValues();
            values.put(DISPLAY_NAME, dispName2);
            values.put(DATA, data2);
            values.put(DATE_OPENED, findDateOpened(cr, data1));
            // delete data2 if it exist (should not exist duplicate data2)
            if (delete(cr, data2) != 0) {
                Log.w(TAG, "delete exist file: " + data2);
            }
            return cr.update(CONTENT_URI, values, DATA + "=?", new String[] { data1 });
        }

        public static String findDisplayName(ContentResolver cr, String data) {
            // FIXME:
            // the following path should be change to the first one:
            // 1. "/storage/emulated/0/path" (used in db)
            // 2. "/storage/emulated/legacy/path"
            // 3. "/sdcard/path"
            data = FileUtility.changeToStoragePath(data);
            Cursor cursor = null;
            try {
                cursor = cr.query(CONTENT_URI, null, DATA + "=?", new String[] { data }, null);
                if (cursor != null & cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                }
                return null;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        public static long findDateOpened(ContentResolver cr, String data) {
            // FIXME:
            // the following path should be change to the first one:
            // 1. "/storage/emulated/0/path" (used in db)
            // 2. "/storage/emulated/legacy/path"
            // 3. "/sdcard/path"
            data = FileUtility.changeToStoragePath(data);
            Cursor cursor = null;
            try {
                cursor = cr.query(CONTENT_URI, null, DATA + "=?", new String[] { data }, null);
                if (cursor != null & cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndex(DATE_OPENED));
                }
                return 0;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        public static int deleteMinDateOpened(ContentResolver cr) {
            Log.d(TAG, "deleteMinDateOpened");
            return cr.delete(CONTENT_URI, String.format("%s=(SELECT MIN(%s) FROM %s)",
                DATE_OPENED, DATE_OPENED, TABLE_NAME), null
            );
        }

        public static int deleteAll(ContentResolver cr) {
            Log.d(TAG, "deleteAll");
            return cr.delete(CONTENT_URI, null, null);
        }

        public static void dump(ContentResolver cr) {
            Cursor cursor = cr.query(CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        Log.v(TAG, cursorItemtoString(cursor));
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }

        private static Cursor queryAll(ContentResolver cr) {
            return cr.query(CONTENT_URI, null, null, null, null);
        }

        private static boolean exist(ContentResolver cr, String data) {
            Cursor cursor = null;
            try {
                cursor = cr.query(CONTENT_URI, null, DATA + "=?", new String[] { data }, null);
                return (cursor != null && cursor.getCount() != 0);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private static String cursorItemtoString(Cursor cursor) {
            return String.format("%d %s %s %d",
                cursor.getInt(cursor.getColumnIndex(_ID)),
                cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)),
                cursor.getString(cursor.getColumnIndex(DATA)),
                cursor.getLong(cursor.getColumnIndex(DATE_OPENED))
            );
        }
    };
}
