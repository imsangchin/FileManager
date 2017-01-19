
package com.asus.filemanager.provider;

import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.DebugLog;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;

public class FileManagerProvider extends ContentProvider {

    private static final String URI_MATCH_SHORTCUT = "shortcut";
    private static final String URI_MATCH_THUMBNAIL = "thumbnail";
    private static final String URI_MATCH_MEDIAFILES = "mediafiles";
    private static final String URI_MATCH_SHAREFILES = "sharefiles";
    private static final String URI_MATCH_MOUNTACCOUNTS = "mountaccounts";
    private static final String URI_MATCH_FAVORITEFILES = "favoritefiles";
    private static final String URI_MATCH_RECENTLYOPEN= "recentlyopen";

    private static final int SHORTCUT = 1;
    private static final int THUMBNAIL = 2;
    private static final int MEDIAFILES = 3;
    private static final int MEDIAFILES_ID = 4;
    private static final int SHAREFILES = 5;
    private static final int MOUNTACCOUNTS = 6;
    private static final int FAVORITEFILES = 7;
    private static final int RECENTLYOPEN = 8;

    private static UriMatcher sUriMatcher;

    private static final String CONTENT_SHORTCUT_TABLE = "shortcut";
    private static final String CONTENT_THUMBNAIL_TABLE = "thumbnail";
    private static final String CONTENT_MEDIAFILES_TABLE = "mediafiles";
    private static final String CONTENT_SHAREFILES_TABLE = "sharefiles";
    private static final String CONTENT_MOUNTACCOUNTS_TABLE = "mountaccounts";
    private static final String CONTENT_FAVORITEFILES_TABLE = "favoritefiles";
    private static final String CONTENT_RECENTLYOPEN_TABLE = "recentlyopen";

    private static final String TAG = "FileManagerProvider";

    private static final boolean DEBUG = ConstantsUtil.DEBUG;

    private DatabaseHelper mProviderHelper;

    private static StringBuilder mStringBuilder = new StringBuilder();

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(ProviderUtility.CONTENT_URI, URI_MATCH_SHORTCUT, SHORTCUT);
        sUriMatcher.addURI(ProviderUtility.CONTENT_URI, URI_MATCH_THUMBNAIL, THUMBNAIL);
        sUriMatcher.addURI(ProviderUtility.CONTENT_URI, URI_MATCH_MEDIAFILES, MEDIAFILES);
        sUriMatcher.addURI(ProviderUtility.CONTENT_URI, URI_MATCH_MEDIAFILES + "/#", MEDIAFILES_ID);
        sUriMatcher.addURI(ProviderUtility.CONTENT_URI, URI_MATCH_SHAREFILES, SHAREFILES);
        sUriMatcher.addURI(ProviderUtility.CONTENT_URI, URI_MATCH_MOUNTACCOUNTS, MOUNTACCOUNTS);
        sUriMatcher.addURI(ProviderUtility.CONTENT_URI, URI_MATCH_FAVORITEFILES, FAVORITEFILES);
        sUriMatcher.addURI(ProviderUtility.CONTENT_URI, URI_MATCH_RECENTLYOPEN, RECENTLYOPEN);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "filemanager.db";
        private static final int DATABASE_VERSION = 10;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (DEBUG)
                Log.i(TAG, "### Create sortcut table!! ###");
            db.execSQL("CREATE TABLE " + CONTENT_SHORTCUT_TABLE + " (" + ProviderUtility.ShortCut._ID + " INTEGER PRIMARY KEY,"
                    + ProviderUtility.ShortCut.LABEL + " TEXT," + ProviderUtility.ShortCut.FILE_PATH + " TEXT NOT NULL UNIQUE" + ");");

            createThumbnailTable(db);

            createMediaFilesTable(db);

            createShareFilesTable(db);

            createMountAccountsTable(db);

            createFavoriteFilesTable(db);

            createRecentlyOpenTable(db);

            // predefined columns
            db.beginTransaction();
            try {
                addShortCutToTable(db, "My Picture", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
                addShortCutToTable(db, "My Camera", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
                addShortCutToTable(db, "My Music", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath());
                addShortCutToTable(db, "Download", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        private void createFavoriteFilesTable(SQLiteDatabase db) {
            if (DEBUG) {
                Log.i(TAG, "### Create FavoriteFiles table!! ###");
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS " + CONTENT_FAVORITEFILES_TABLE + " ("
                    + ProviderUtility.FavoriteFiles._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ProviderUtility.FavoriteFiles.DISPLAY_NAME + " TEXT NOT NULL,"
                    + ProviderUtility.FavoriteFiles.DATA + " TEXT"
                    + ");");
        }

        private void createRecentlyOpenTable(SQLiteDatabase db) {
            if (DEBUG) {
                Log.i(TAG, "### Create RecentlyOpen table!! ###");
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS " + CONTENT_RECENTLYOPEN_TABLE + " ("
                    + ProviderUtility.RecentlyOpen._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ProviderUtility.RecentlyOpen.DISPLAY_NAME + " TEXT NOT NULL,"
                    + ProviderUtility.RecentlyOpen.DATA + " TEXT NOT NULL,"
                    + ProviderUtility.RecentlyOpen.DATE_OPENED + " LONG"
                    + ");");
        }

        private void createMountAccountsTable(SQLiteDatabase db) {
            if (DEBUG) {
                Log.i(TAG, "### Create MountAccounts table!! ###");
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS " + CONTENT_MOUNTACCOUNTS_TABLE + " ("
                    + ProviderUtility.MountAccounts._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ProviderUtility.MountAccounts.ACCOUNT_NAME + " TEXT NOT NULL,"
                    + ProviderUtility.MountAccounts.ACCOUNT_TYPE + " TEXT NOT NULL,"
                    + ProviderUtility.MountAccounts.AUTHTOKEN_TYPE + " TEXT"
                    + ");");
        }

        private void createShareFilesTable(SQLiteDatabase db) {
            if (DEBUG) {
                Log.i(TAG, "### Create ShareFiles table!! ###");
            }

            db.execSQL("CREATE TABLE " + CONTENT_SHAREFILES_TABLE + " (" +
            ProviderUtility.ShareFiles._ID + " INTEGER PRIMARY KEY,"
                    + ProviderUtility.ShareFiles.FILE_PATH + " TEXT NOT NULL UNIQUE,"
                    + ProviderUtility.ShareFiles.SHARE_TYPE + " INTEGER"
                    + ");");
        }

        private void createThumbnailTable(SQLiteDatabase db) {
            if (DEBUG) {
                Log.i(TAG, "### Create thumbnail table!! ###");
            }
            db.execSQL("CREATE TABLE " + CONTENT_THUMBNAIL_TABLE + " (" + ProviderUtility.Thumbnail._ID + " INTEGER PRIMARY KEY,"
                    + ProviderUtility.Thumbnail.FILE_PATH + " TEXT NOT NULL UNIQUE," + ProviderUtility.Thumbnail.BITMAP + " STREAM NOT NULL," + ProviderUtility.Thumbnail.MODIFY_TIME + " INTEGER NOT NULL DEFAULT -1" + ");");
        }

        private void createMediaFilesTable(SQLiteDatabase db) {
            if (DEBUG)
                Log.i(TAG, "### Create mediafiles table!! ###");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + CONTENT_MEDIAFILES_TABLE + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "_data TEXT," +
                    "_size INTEGER," +
                    "parent INTEGER," +
                    "date_added INTEGER," +
                    "date_modified INTEGER," +
                    "mime_type TEXT," +
                    "title TEXT," +
                    "_display_name TEXT," +
                    "media_type INTEGER" +
                    ");");
        }

        private void addShortCutToTable(SQLiteDatabase db, String label, String file_path) {
            StringBuilder sb = mStringBuilder;
            sb.setLength(0);
            sb.append("INSERT INTO ")
                    .append(CONTENT_SHORTCUT_TABLE)
                    .append(" (label,file_path) VALUES ('")
                    .append(label)
                    .append("','")
                    .append(file_path)
                    .append("');");
            db.execSQL(sb.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (DEBUG) {
                Log.v(TAG, "onUpgrade : " + oldVersion + "  -> " + newVersion);
            }
            if (oldVersion == 1) {
                if (DEBUG) {
                    Log.v(TAG, " oldVersion == 1 : update ");
                }
                createMediaFilesTable(db);
                oldVersion++;
            }

            if (oldVersion == 2 || oldVersion == 3 || oldVersion == 4) {
                if (DEBUG) {
                    Log.v(TAG, " oldVersion == 2 : update ");
                }
                db.execSQL("DROP TABLE IF EXISTS " + CONTENT_THUMBNAIL_TABLE);
                createThumbnailTable(db);
                oldVersion++;
            }

            if(oldVersion == 5) {
                if (DEBUG) {
                    Log.v(TAG, " oldVersion == 5 : update ");
                }
                createShareFilesTable(db);
            }

            if(oldVersion < 8){
                if (DEBUG) {
                    Log.v(TAG, " oldVersion < 8 : update ");
                }
                createMountAccountsTable(db);
            }

            if(oldVersion < 9){
                if (DEBUG) {
                    Log.v(TAG, " oldVersion < 9 : update ");
                }
                createFavoriteFilesTable(db);
            }

            if (oldVersion < 10) {
                if (DEBUG) {
                    Log.v(TAG, " oldVersion < 10 : update ");
                }
                createRecentlyOpenTable(db);
            }

        }

    }

    @Override
    public boolean onCreate() {
        mProviderHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (DEBUG)
            Log.i(TAG, "### delete ###");

        SQLiteDatabase db = mProviderHelper.getWritableDatabase();

        int count;
        String id;

        int match = sUriMatcher.match(uri);

        switch (match) {
            case SHORTCUT:
                count = db.delete(CONTENT_SHORTCUT_TABLE, selection, selectionArgs);
                break;
            case THUMBNAIL:
                count = db.delete(CONTENT_THUMBNAIL_TABLE, selection, selectionArgs);
                break;
            case MEDIAFILES:
                count = db.delete(CONTENT_MEDIAFILES_TABLE, selection, selectionArgs);
                break;
            case MEDIAFILES_ID:
                id = uri.getPathSegments().get(1);
                count = db.delete(CONTENT_MEDIAFILES_TABLE, "_id" + "=" + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case SHAREFILES:
                count = db.delete(CONTENT_SHAREFILES_TABLE, selection, selectionArgs);
                break;
            case MOUNTACCOUNTS:
                count = db.delete(CONTENT_MOUNTACCOUNTS_TABLE, selection, selectionArgs);
                break;
            case FAVORITEFILES:
                count = db.delete(CONTENT_FAVORITEFILES_TABLE, selection, selectionArgs);
                break;
            case RECENTLYOPEN:
                count = db.delete(CONTENT_RECENTLYOPEN_TABLE, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URI=" + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (DEBUG)
            Log.i(TAG, "### insert ###");

        SQLiteDatabase db = mProviderHelper.getWritableDatabase();

        long rowId;
        Uri newUri = null;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case SHORTCUT:
                rowId = db.insert(CONTENT_SHORTCUT_TABLE, null, values);

                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(ProviderUtility.ShortCut.CONTENT_URI, rowId);
                }
                break;
            case THUMBNAIL:
                rowId = db.insert(CONTENT_THUMBNAIL_TABLE, null, values);

                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(ProviderUtility.Thumbnail.CONTENT_URI, rowId);
                }
                break;
            case MEDIAFILES:
                rowId = db.insert(CONTENT_MEDIAFILES_TABLE, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(ProviderUtility.MediaFiles.CONTENT_URI, rowId);
                }
                break;
            case SHAREFILES:
                rowId = db.insert(CONTENT_SHAREFILES_TABLE, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(ProviderUtility.ShareFiles.CONTENT_URI, rowId);
                }
                break;
            case MOUNTACCOUNTS:
                rowId = db.insert(CONTENT_MOUNTACCOUNTS_TABLE, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(ProviderUtility.MountAccounts.CONTENT_URI, rowId);
                }
                break;
            case FAVORITEFILES:
                rowId = db.insert(CONTENT_FAVORITEFILES_TABLE, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(ProviderUtility.MountAccounts.CONTENT_URI, rowId);
                }
                break;
            case RECENTLYOPEN:
                rowId = db.insert(CONTENT_RECENTLYOPEN_TABLE, null, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(ProviderUtility.RecentlyOpen.CONTENT_URI, rowId);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI=" + uri);
        }

        if(newUri != null)
            getContext().getContentResolver().notifyChange(newUri, null);

        return newUri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (DEBUG)
            Log.i(TAG, "### query uri=" + uri + ", selection=" + selection);

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int match = sUriMatcher.match(uri);

        switch (match) {
            case SHORTCUT:
                qb.setTables(CONTENT_SHORTCUT_TABLE);
                break;
            case THUMBNAIL:
                qb.setTables(CONTENT_THUMBNAIL_TABLE);
                break;
            case MEDIAFILES:
                qb.setTables(CONTENT_MEDIAFILES_TABLE);
                break;
            case MEDIAFILES_ID:
                qb.setTables(CONTENT_MEDIAFILES_TABLE);
                qb.appendWhere("_id" + "=" + uri.getPathSegments().get(1));
                break;
            case SHAREFILES:
                qb.setTables(CONTENT_SHAREFILES_TABLE);
                break;
            case MOUNTACCOUNTS:
                qb.setTables(CONTENT_MOUNTACCOUNTS_TABLE);
                break;
            case FAVORITEFILES:
                qb.setTables(CONTENT_FAVORITEFILES_TABLE);
                break;
            case RECENTLYOPEN:
                qb.setTables(CONTENT_RECENTLYOPEN_TABLE);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = mProviderHelper.getReadableDatabase();
        Cursor result = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        if (result == null) {
            if (DEBUG)
                Log.i(TAG, "### SyncSettings.query failed!! ###");
        } else {
            result.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (DEBUG)
            Log.i(TAG, "### update ###");

        int count;
        String id;
        int match = sUriMatcher.match(uri);

        SQLiteDatabase db = mProviderHelper.getWritableDatabase();

        switch (match) {
            case SHORTCUT:
                count = db.update(CONTENT_SHORTCUT_TABLE, values, selection, selectionArgs);
                break;
            case THUMBNAIL:
                count = db.update(CONTENT_THUMBNAIL_TABLE, values, selection, selectionArgs);
                break;
            case MEDIAFILES:
                count = db.update(CONTENT_MEDIAFILES_TABLE, values, selection, selectionArgs);
                break;
            case MEDIAFILES_ID:
                id = uri.getPathSegments().get(1);
                count = db.update(CONTENT_MEDIAFILES_TABLE, values, "_id" + "=" + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case SHAREFILES:
                count = db.update(CONTENT_SHAREFILES_TABLE, values, selection, selectionArgs);
                break;
            case MOUNTACCOUNTS:
                count = db.update(CONTENT_MOUNTACCOUNTS_TABLE, values, selection, selectionArgs);
                break;
            case FAVORITEFILES:
                count = db.update(CONTENT_FAVORITEFILES_TABLE, values, selection, selectionArgs);
                break;
            case RECENTLYOPEN:
                count = db.update(CONTENT_RECENTLYOPEN_TABLE, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Cannot update URI=" + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public String getType(Uri uri) {

        return null;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (sUriMatcher.match(uri) == MEDIAFILES_ID) {
            ParcelFileDescriptor fileDescriptor = null;
            if (DEBUG) {
                Log.d(TAG, "openFile : " + uri.toString());
            }
            try {
                fileDescriptor = openFileHelper(uri, mode);
            } catch (FileNotFoundException ex) {
                Log.e(TAG, "catch FileNotFoundException ");
            }
            return fileDescriptor;
        } else {
            return super.openFile(uri, mode);
        }
    }

}
