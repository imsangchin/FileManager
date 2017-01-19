package com.asus.filemanager.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class GameAppProvider extends ContentProvider {
    /*
     * Defines a handle to the database helper object. The MainDatabaseHelper class is defined
     * in a following snippet.
     */
    private MainDatabaseHelper mOpenHelper;

    // Defines the database name
    public static final String AUTHORITY = "com.asus.filemanager.gameappprovier.provider";
    private static final String DBNAME = "game_app";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DBNAME);
    public static final String GAME_TABLE_NAME = "Game";
    public static final String ID = "_ID";
    public static final String PACKAGE_NAME = "_package_name";
    public static final String CATEGORY = "_category";
    public static final String IS_GAME = "_is_game";

    // A string that defines the SQL statement for creating a table
    private static final String SQL_CREATE_MAIN = "CREATE TABLE " +
        GAME_TABLE_NAME +                // Table's name
        " (" +                           // The columns in the table
        ID + " INTEGER PRIMARY KEY, " +
        PACKAGE_NAME + " TEXT, " +
        CATEGORY + " TEXT, " +
        IS_GAME + " INTERGER"
        + ")";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.insert(GAME_TABLE_NAME, null, values);
        return null;
    }

    @Override
    public boolean onCreate() {
        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = new MainDatabaseHelper(
            getContext(),        // the application context
            DBNAME,              // the name of the database)
            null,                // uses the default SQLite cursor
            1                    // the version number
        );

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(GAME_TABLE_NAME);

        // Make the query.
        Cursor c = sqlBuilder.query(db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rownum = db.update(GAME_TABLE_NAME, values, selection, selectionArgs);
        return rownum;
    }

    /**
     * Helper class that actually creates and manages the provider's underlying data repository.
     */
    protected static final class MainDatabaseHelper extends SQLiteOpenHelper {

        /*
         * Instantiates an open helper for the provider's SQLite data repository
         * Do not do database creation and upgrade here.
         */
        MainDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, DBNAME, null, 1);
        }

        /*
         * Creates the data repository. This is called when the provider attempts to open the
         * repository and SQLite reports that it doesn't exist.
         */
        public void onCreate(SQLiteDatabase db) {

            // Creates the main table
            db.execSQL(SQL_CREATE_MAIN);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

}
