package com.asus.filemanager.samba.provider;

import com.asus.filemanager.samba.SambaItem;
import com.asus.filemanager.samba.provider.PCAccountInfoProvider.UserInfoColumns;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public final class PcInfoDbHelper {
	
    public static Context mContext = null;
    private final static Uri PCINFO_URI = UserInfoColumns.USERINFO_URI;

    public static void init(Context context){
	    mContext = context;
    }

    public static SambaItem queryAccountInfo(String pc,String ip){
	    SambaItem item = new SambaItem(pc,ip);
	    if(mContext != null){
		    ContentResolver crv = mContext.getContentResolver();
		    Cursor cr = crv.query(PCINFO_URI, null, UserInfoColumns.PC_NAME + "='" + pc + "'", null, null);
		    if(cr != null && cr.moveToNext()){
		    	String account = cr.getString(cr.getColumnIndex(UserInfoColumns.ACCOUNT_NAME));
		    	String password = cr.getString(cr.getColumnIndex(UserInfoColumns.PASSWORD));
		    	item = new SambaItem(pc,ip,account,password);
		    }
	    }
	    return item;
    }

    public static void deleteAccountInfo(String pc,String account){
    	if(mContext == null){
    		return;
    	}
		ContentResolver crv = mContext.getContentResolver();
		Cursor cr;
		if(TextUtils.isEmpty(pc)){
		    cr = crv.query(PCINFO_URI, null, null, null, null);
		    if(cr != null && cr.getCount() >=1){
			    while(cr.moveToNext()){
				    crv.delete(PCINFO_URI, null, null);
			    }
			    cr.close();
		    }
		}else{
			cr = crv.query(PCINFO_URI,null,UserInfoColumns.PC_NAME + "='" + pc + "'" + " AND " + UserInfoColumns.ACCOUNT_NAME + "='" + account + "'",null,null);
		    if(cr != null && cr.moveToNext()){
		    	crv.delete(PCINFO_URI,UserInfoColumns.PC_NAME + "='" + pc + "'" + " AND " + UserInfoColumns.ACCOUNT_NAME + "='" + account + "'",null);
		    	cr.close();
		    }
		}
    }

    public static void saveAccountInfo(SambaItem item){
    	if(mContext == null || item == null){
    		return;
    	}

	    ContentResolver crv = mContext.getContentResolver();
	    ContentValues value = new ContentValues();
	    String pcName = item.getPcName();
	    value.put(UserInfoColumns.PC_NAME, pcName);
	    value.put(UserInfoColumns.IP_ADDRESS, item.getIpAddress());
	    value.put(UserInfoColumns.ACCOUNT_NAME, item.getAccount());
	    value.put(UserInfoColumns.PASSWORD, item.getPassword());
	    Cursor cr = crv.query(PCINFO_URI, null, UserInfoColumns.PC_NAME + "='" + pcName + "'", null, null);
	    if(cr != null && cr.moveToNext()){
	    	crv.update(PCINFO_URI, value, UserInfoColumns.PC_NAME + "='" + pcName + "'", null);
	    	cr.close();
	    }else{
	    	crv.insert(PCINFO_URI, value);
	    }
    }


}
