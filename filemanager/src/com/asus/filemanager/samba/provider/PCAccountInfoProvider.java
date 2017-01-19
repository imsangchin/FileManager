package com.asus.filemanager.samba.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class PCAccountInfoProvider extends ContentProvider{
	
    private static final String DATABASE_NAME = "accountInfo.db";
    private static final String DATABASE_TABLE = "PcAccounts";
    private static final String AUTHORITY = "com.asus.filemanager.samba.PCAccountInfoProvider";
    private static final int VERSION = 1;
    private static final int SAMBA_QUERY = 1;
    private static final UriMatcher mUriMatcher;
    static {
    	mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    	mUriMatcher.addURI(AUTHORITY, DATABASE_TABLE, SAMBA_QUERY);
    }
    
	private DatabaseHelper mOpenHelper;
	
	
	public static class UserInfoColumns implements BaseColumns{
		
		private UserInfoColumns() {};
		
		public static final Uri USERINFO_URI = Uri.parse("content://" + AUTHORITY + "/" + DATABASE_TABLE);
		public static final String USERINFO_TYPE = "vnd.android.cursor.dir/" + AUTHORITY;
		
		public static final String PC_NAME ="pc_name";
		public static final String IP_ADDRESS = "ip_address";
		public static final String ACCOUNT_NAME = "account_name";
		public static final String PASSWORD = "password";
		
	}
	
	private class DatabaseHelper extends SQLiteOpenHelper{
		
		private static final String DATABASE_CREATE = "create table " + 
				DATABASE_TABLE + " (" + 
				UserInfoColumns._ID + " integer primary key autoincrement, " + 
				UserInfoColumns.PC_NAME + " text not null, " + 
				UserInfoColumns.IP_ADDRESS + " text not null, " + 
				UserInfoColumns.ACCOUNT_NAME + " text not null, " + 
				UserInfoColumns.PASSWORD + " text not null);";
		

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, VERSION);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		mOpenHelper = new DatabaseHelper(getContext());
		PcInfoDbHelper.init(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder mQueryBuilder = new SQLiteQueryBuilder();
		switch (mUriMatcher.match(uri)){
		    case  SAMBA_QUERY:
				mQueryBuilder.setTables(DATABASE_TABLE);
		    	break;
		    default:
		    	throw new IllegalArgumentException("Unknown URL " + uri);
		}
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor cursor = mQueryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		// TODO Auto-generated method stub
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		switch (mUriMatcher.match(uri)){
		    case SAMBA_QUERY:
		    	return UserInfoColumns.USERINFO_TYPE;
		    default:
		    	throw new IllegalArgumentException("Unknown URL " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long id = -1;
		switch(mUriMatcher.match(uri)){
		    case SAMBA_QUERY:
		    	id = db.insert(DATABASE_TABLE, null, values);
		    	if(id > -1){
		    		Uri insertId = ContentUris.withAppendedId(UserInfoColumns.USERINFO_URI, id);
		    		getContext().getContentResolver().notifyChange(insertId, null);
		    		return insertId;
		    	}else{
		    		return null;
		    	}
		    default:
		    	 throw new SQLException("Fail to insert into " + uri);
		}
		// TODO Auto-generated method stub
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count = -1;
		switch(mUriMatcher.match(uri)){
		    case SAMBA_QUERY:
		    	count = db.delete(DATABASE_TABLE, selection, selectionArgs);
		    	getContext().getContentResolver().notifyChange(uri, null);
		    	break;
		    default:
		    	throw new SQLException("Fail to insert into " + uri); 
		}
		// TODO Auto-generated method stub
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count = -1;
		switch(mUriMatcher.match(uri)){
		    case SAMBA_QUERY:
		    	count = db.update(DATABASE_TABLE, values, selection, selectionArgs);
		    	getContext().getContentResolver().notifyChange(uri, null);
		    	break;
		    default:
		    	throw new SQLException("Fail to insert into " + uri); 
		}
		// TODO Auto-generated method stub
		return count;
	}

}
