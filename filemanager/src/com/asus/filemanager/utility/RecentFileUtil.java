package com.asus.filemanager.utility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.common.MsgObj.FileObj;
import com.asus.service.cloudstorage.common.MsgObj.StorageObj;

import android.R.bool;
import android.R.integer;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class RecentFileUtil {
	public static final String TAG = "RecentFileUtil";
	public static final String RECENT_SCAN_FILES = "Recent_scan_files";
	public interface RecentFileTable{
		public static final String COLUMN_ID="_id";
		public static final String  TABLE_NAME = "recentFiles";
		public static final String  VFILETYPE = "vFileType";
		public static final String  STORAGETYPE = "storageType";
		public static final String  STORAGENAME = "storageName";
		public static final String  PARENTPATH = "parentPath";
		public static final String  PATH = "path";
		public static final String  FILENAME = "fileName";
		public static final String  FILEID = "fileId";
		public static final String  PARENTFILEID = "parentFileId";
		public static final String  DEVICEID = "deviceId";
		public static final String  SCANTIME = "scanTime";


		public static final int  COLUMN_ID_INDEX = 0;
		public static final int  VFILETYPE_INDEX = 1;
		public static final int  STORAGETYPE_INDEX = 2;
		public static final int  STORAGENAME_INDEX = 3;
		public static final int  PARENTPATH_INDEX = 4;
		public static final int  PATH_INDEX = 5;
		public static final int  FILENAME_INDEX = 6;
		public static final int  FILEID_INDEX = 7;
		public static final int  PARENTFILEID_INDEX = 8;
		public static final int  DEVICEID_INDEX = 9;
		public static final int  SCANTIME_INDEX = 10;

	}
	private static DbHelper dBHelper = null;
	private static void init(Context context){
		if (dBHelper == null) {
			dBHelper = DbHelper.getInstance(context);
		}
	}
	public static SQLiteDatabase getWriteDatabase(Context context){
		init(context);
		return dBHelper.getWritableDatabase();
	}
	public static SQLiteDatabase getReadDatabase(Context context){
		init(context);
		return dBHelper.getReadableDatabase();
	}

	public static boolean isHasSavedFile(VFile vFile,Context context){
		int vfileType = vFile.getVFieType();
		SQLiteDatabase db = getReadDatabase(context);
		Cursor cursor = null;
		String selection = null;
		String[] selectionArgs = null;
		String argStr = "";
		switch (vfileType) {
		case VFileType.TYPE_LOCAL_STORAGE:
			selection = RecentFileTable.PATH + " =?";
			selectionArgs = new String[]{vFile.getPath()};
			break;
		case VFileType.TYPE_CLOUD_STORAGE:
			RemoteVFile temFile = ((RemoteVFile)vFile);
			int msgType = temFile.getMsgObjType();
			selection = RecentFileTable.STORAGETYPE + "=? and "
						+RecentFileTable.STORAGENAME + "=? and "
						+RecentFileTable.PATH + "=?" ;
			argStr = temFile.getStorageType() + "," + temFile.getStorageName() + "," + temFile.getPath();
			if (msgType == MsgObj.TYPE_HOMECLOUD_STORAGE) {
				selection += " and "+RecentFileTable.DEVICEID + "=? ";
				argStr += "," + temFile.getmDeviceId();
			}else if ((msgType != MsgObj.TYPE_DROPBOX_STORAGE && msgType != MsgObj.TYPE_BAIDUPCS_STORAGE)) {
				selection += " and "+ RecentFileTable.FILEID + "=?";
				argStr += ","+temFile.getFileID();
			}
			selectionArgs = new String[]{argStr};
			break;
		default:
			break;
		}
		try {
			cursor = db.query(RecentFileTable.TABLE_NAME, null, selection, selectionArgs, null, null, null);
			if (cursor != null && cursor.moveToNext()) {
				return true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			cursor.close();
		}
		return false;


	}

	public static void saveVfile(VFile vFile,Context context){
		boolean flag = false;
		int vfileType = vFile.getVFieType();
		String fileName = null;
		if (vfileType == VFileType.TYPE_LOCAL_STORAGE) {
			fileName = vFile.getName();
		}else if(vfileType == VFileType.TYPE_CLOUD_STORAGE) {
			fileName =( (RemoteVFile)vFile).getName();
		}
		if (fileName == null || fileName.equals("")) {
			return;
		}
		SQLiteDatabase db = getReadDatabase(context);
		Cursor cursor = null;
		String selection = null;
		String[] selectionArgs = null;
		RemoteVFile temFile = null;
		String argStr = "";
		switch (vfileType) {
		case VFileType.TYPE_LOCAL_STORAGE:
			selection =RecentFileTable.VFILETYPE + "=? and " + RecentFileTable.PATH + " =?";
			selectionArgs = new String[]{String.valueOf(vFile.getVFieType()),vFile.getPath()};
			break;
		case VFileType.TYPE_CLOUD_STORAGE:
			 temFile = ((RemoteVFile)vFile);
			int msgType = temFile.getMsgObjType();
			selection = RecentFileTable.VFILETYPE + "=? and "
						+RecentFileTable.STORAGETYPE + "=? and "
						+RecentFileTable.STORAGENAME + "=? and "
						+RecentFileTable.PATH + "=?" ;
			argStr =temFile.getVFieType()+","+ temFile.getStorageType() + "," + temFile.getStorageName() + "," + temFile.getPath();
			if (msgType == MsgObj.TYPE_HOMECLOUD_STORAGE) {
				selection += " and "+RecentFileTable.DEVICEID + "=? ";
				argStr += "," + temFile.getmDeviceId();
			}else if (msgType != MsgObj.TYPE_DROPBOX_STORAGE) {
				selection += " and "+ RecentFileTable.FILEID + "=?";
				argStr += ","+temFile.getFileID();
			}
			String[]strs = argStr.split(",");
			selectionArgs = new String[strs.length];
			for (int i = 0; i < strs.length; i++) {
				selectionArgs[i]=strs[i];
			}
			break;
		default:
			break;
		}
		try {
			cursor = db.query(RecentFileTable.TABLE_NAME, null, selection, selectionArgs, null, null, null);
			if (cursor != null && cursor.moveToNext()) {
				flag = true;
			}else {
				flag = false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if (cursor!=null) {
				cursor.close();
			}
		}
		SQLiteDatabase writeDb = getWriteDatabase(context);
		writeDb.beginTransaction();
		ContentValues values = new ContentValues();
		try {
			values.put(RecentFileTable.SCANTIME, System.currentTimeMillis());
			if (flag) {
				writeDb.update(RecentFileTable.TABLE_NAME, values, selection, selectionArgs);
			}else {
				switch (vfileType) {
				case VFileType.TYPE_LOCAL_STORAGE:
					values.put(RecentFileTable.VFILETYPE, vFile.getVFieType());
					values.put(RecentFileTable.PATH,((LocalVFile) vFile).getPath());
					values.put(RecentFileTable.FILENAME, ((LocalVFile) vFile).getName());
					break;
				case VFileType.TYPE_CLOUD_STORAGE:
					values.put(RecentFileTable.VFILETYPE,vFile.getVFieType());
					values.put(RecentFileTable.STORAGETYPE,temFile.getStorageType());
					values.put(RecentFileTable.STORAGENAME, temFile.getStorageName());
					values.put(RecentFileTable.PATH, temFile.getPath());
					values.put(RecentFileTable.FILENAME, temFile.getName());
					values.put(RecentFileTable.FILEID, temFile.getFileID());
					values.put(RecentFileTable.PARENTFILEID, temFile.getParentFileID());
					values.put(RecentFileTable.PARENTPATH, temFile.getParent());
					break;
				default:
					break;
				}
				writeDb.insert(RecentFileTable.TABLE_NAME, null, values);
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();

		}finally{
			writeDb.endTransaction();
		}
	}
	public static VFile[] getRecentFiles(Context context,int day){
		SQLiteDatabase db = getReadDatabase(context);
		VFile[] vFiles = null;
		String selection = null;
		String[]selectionArgs = null;
		Cursor cursor = null;
		List<Integer> deleteFileIds = new ArrayList<Integer>();
		db.beginTransaction();
		if (day > 0) {
			selection = RecentFileTable.SCANTIME +">=?" ;
			selectionArgs = new String[]{String.valueOf(((new Date()).getTime() - day * 24 * 60 * 60 * 1000 ) )};
		}
		try {
			cursor = db.query(RecentFileTable.TABLE_NAME, null, selection, selectionArgs, null, null, RecentFileTable.SCANTIME + " desc ");
			if (cursor != null ) {
				vFiles = new VFile[cursor.getCount()];
			}
			int i = 0;
			while (cursor != null && cursor.moveToNext()) {
				int vfileType = cursor.getInt(RecentFileTable.VFILETYPE_INDEX);
				int id = cursor.getInt(RecentFileTable.COLUMN_ID_INDEX);
				switch (vfileType) {
				case VFileType.TYPE_LOCAL_STORAGE:
					vFiles[i] = new LocalVFile(cursor.getString(RecentFileTable.PATH_INDEX));
					if (!vFiles[i].exists() || !vFiles[i].isDirectory()) {
						deleteFileIds.add(id);
						continue;
					}
					break;
				case VFileType.TYPE_CLOUD_STORAGE:
					StorageObj storageObj = new StorageObj(RemoteVFile.getMsgObjType(cursor.getInt(RecentFileTable.STORAGETYPE_INDEX)), cursor.getString(RecentFileTable.STORAGENAME_INDEX), "");
					storageObj.setDeviceId(cursor.getString(RecentFileTable.DEVICEID_INDEX));
					FileObj fileObj = new FileObj(cursor.getString(RecentFileTable.FILENAME_INDEX), cursor.getString(RecentFileTable.PARENTPATH_INDEX), true, 2048, cursor.getLong(RecentFileTable.SCANTIME_INDEX), "DWR", true);
					fileObj.setFileId(cursor.getString(RecentFileTable.FILEID_INDEX));
					fileObj.setHasThumbnail(false);
					fileObj.setParentId(cursor.getString(RecentFileTable.PARENTFILEID_INDEX));
					RemoteVFile temp = new RemoteVFile(fileObj,storageObj);
					vFiles[i] =temp;
					break;
				case VFileType.TYPE_SAMBA_STORAGE:
					break;

				default:
					break;
				}
				i++;

			}
			deleteHasDeletedFileFromRecentFiles(deleteFileIds, context);
		} catch (Exception e) {
			// TODO: handle exception
		}finally{
			cursor.close();
		}
		return vFiles;
	}
	public static void deleteHasDeletedFileFromRecentFiles(List<Integer>deleteIds,Context context){
		if (deleteIds == null || deleteIds.size()<=0) {
			return;
		}else {
			SQLiteDatabase db = getWriteDatabase(context);
			String temp = "";
			String whereClause = RecentFileTable.COLUMN_ID + "=?";
			int listSize = deleteIds.size();
		     if (listSize == 1) {
	                temp = deleteIds.get(0).toString();
	            } else {
	                for (int i = 0; i < listSize - 1; i++) {
	                    temp = temp + deleteIds.get(i) + ",";
	                }
	                temp = temp + deleteIds.get(listSize - 1);
	            }
	            Log.d(TAG, "startClearOldFile need to delete listSize:"+listSize+" temp:"+temp);
	            String[] whereArgs = new String[] { temp };
			try {
				db.beginTransaction();
				db.delete(RecentFileTable.TABLE_NAME, whereClause, whereArgs);
				db.setTransactionSuccessful();
			} catch (Exception e) {
				// TODO: handle exception
			}finally{
				db.endTransaction();
			}
		}
	}
	public static void printData(Context context){
		SQLiteDatabase db = getReadDatabase(context);
		Cursor cursor = null;
		try {
			 cursor = db.query(RecentFileTable.TABLE_NAME, null, null, null, null, null, null);
			String[]names = cursor.getColumnNames();
			while (cursor.moveToNext()) {
				String temp = "";
				for (int i = 0; i < names.length; i++) {
				 temp += names[i]+":" + cursor.getString(cursor.getColumnIndex(names[i]))+",";
				}
				Log.d(TAG,temp);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally{
			if (cursor!=null) {
				cursor.close();
			}
		}
	}
	public static void delete(Context context,int day){
		SQLiteDatabase db = getWriteDatabase(context);
		String whereCase = null;
		String[]whereArgs = null;
		db.beginTransaction();
		if (day > 0) {
			whereCase = RecentFileTable.SCANTIME +"<=?" ;
			whereArgs = new String[]{String.valueOf(((new Date()).getTime() - day * 24 * 60 * 60 * 1000 ) )};
		}
		try {
			db.delete(RecentFileTable.TABLE_NAME, whereCase, whereArgs);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db.endTransaction();
	}
}
