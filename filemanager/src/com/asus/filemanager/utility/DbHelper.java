package com.asus.filemanager.utility;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.asus.filemanager.utility.RecentFileUtil.RecentFileTable;

public class DbHelper extends SQLiteOpenHelper{
	private static final String DB_NAME = "FileManagerRecentFile.db";
	private static final int DB_VERSION = 1;
	private static DbHelper dbHelper = null;
	public  DbHelper (Context context){
		super(context, DB_NAME, null, DB_VERSION);
	}
    public static synchronized DbHelper getInstance(Context context) {
        if (dbHelper == null) {
            dbHelper = new DbHelper(context);
        }
        return dbHelper;
    }
	@Override
	public void onCreate(SQLiteDatabase db) {
		createRecentFilesTable(db);


	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE if exists " + RecentFileTable.TABLE_NAME);
		createRecentFilesTable(db);
	}

	private void createRecentFilesTable(SQLiteDatabase db){
		   db.execSQL("CREATE TABLE " + RecentFileTable.TABLE_NAME + " ("
				   	  + RecentFileTable.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
					  + RecentFileTable.VFILETYPE + " INTEGER DEFAULT -1,"
					  + RecentFileTable.STORAGETYPE + " INTEGER DEFAULT -1,"
					  + RecentFileTable.STORAGENAME +" TEXT ,"
					  + RecentFileTable.PARENTPATH +" TEXT,"
					  + RecentFileTable.PATH +" TEXT,"
					  + RecentFileTable.FILENAME +" TEXT,"
					  + RecentFileTable.FILEID +" TEXT,"
					  + RecentFileTable.PARENTFILEID +" TEXT,"
					  + RecentFileTable.DEVICEID +" TEXT,"
					  + RecentFileTable.SCANTIME + " INTEGER "
					  +");");
	}

}
