package com.asus.filemanager.utility;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.service.cloudstorage.common.StorageVolumeHelper;

public class reflectionApis {

	public static void SystemArrayCopy(Object src, int srcPos, Object dst, int dstPos, int length){
		Class<?> clsSystem= tryClassForName("java.lang.System");
		Method mtdArrayCopy = tryGetMethod(clsSystem, "arraycopy", Object.class,int.class,Object.class,int.class,int.class);
		tryInvoke(mtdArrayCopy,null,src,srcPos,dst,dstPos,length);
	}

	public static String getSystemProperty(String propName, String defaultValue){
		Class<?> clsSystemProperties = tryClassForName("android.os.SystemProperties");
		Class<?>[] paramTypes= new Class[2];
        paramTypes[0]= String.class;
        paramTypes[1]= String.class;  
		Method mtdGet = tryGetMethod(clsSystemProperties, "get", paramTypes);
		return tryInvoke(mtdGet, null, propName,defaultValue);
	}

	public static int getSystemPropertyInt(String propName, int defaultValue){
		Class<?> clsSystemProperties = tryClassForName("android.os.SystemProperties");
		Class<?>[] paramTypes= new Class[2];
		paramTypes[0]= String.class;
		paramTypes[1]= int.class;  
		Method mtdGet = tryGetMethod(clsSystemProperties, "getInt", paramTypes);
		return tryInvoke(mtdGet, null, propName,defaultValue);
	}

	public static boolean getSystemPropertyBoolean(String propName, boolean defaultValue){
		Class<?> clsSystemProperties = tryClassForName("android.os.SystemProperties");
		Class<?>[] paramTypes= new Class[2];
		paramTypes[0]= String.class;
		paramTypes[1]= boolean.class;  
		Method mtdGet = tryGetMethod(clsSystemProperties, "getBoolean", paramTypes);
		return tryInvoke(mtdGet, null, propName,defaultValue);
	}
  
	public static final String EXTRA_STORAGE_VOLUME = "storage_volume";

	/*
	private static Object getStorageVolume(Object[] volumes, File file) {
		try {
			file = file.getCanonicalFile();
		} catch (IOException ignored) {
			return null;
		}
		for (Object volume : volumes) {
			File volumeFile = StorageVolumeHelper.getPathFile(volume);
			try {
				volumeFile = volumeFile.getCanonicalFile();
			} catch (IOException ignored) {
				continue;
			}

			if (FileUtility.contains(volumeFile, file)) {
				return volume;
			}
		}
		return null;
	}
	*/

	public static String getVolumeState(StorageManager storageManager, Object storageVolume){

		String status = Environment.MEDIA_UNKNOWN;
		if (StorageVolumeHelper.sGetStateMethod != null) {
		    return StorageVolumeHelper.getState(storageVolume);
		}else{
			String aPath = StorageVolumeHelper.getPath(storageVolume);
			if (null != aPath){
				return getVolumeState(storageManager ,aPath);
			}
		}
		return status;
		/*
		Method mMethodGetStatus;
		String status = Environment.MEDIA_UNKNOWN;

		try {
			mMethodGetStatus = stroageManager.getClass().getMethod("getVolumeState",String.class); 	
			status = (String) (mMethodGetStatus.invoke(stroageManager,path));
		}catch (Exception ignore){
			
		}
		return status;
		*/
	}
	public static String getVolumeState(StorageManager stroageManager, String path){
		Method mMethodGetStatus;  
		String status = Environment.MEDIA_UNKNOWN;

		try {
			mMethodGetStatus = stroageManager.getClass().getMethod("getVolumeState",String.class); 	
			status = (String) (mMethodGetStatus.invoke(stroageManager,path));
		}catch (Exception ignore){
			
		}
		return status;
	}
	public static String volume_getMountPointTitle(Object storageVolume) {
	    String path = volume_getPath(storageVolume);
	    int lastIndex = path.lastIndexOf("/");
	    return path.substring(lastIndex + 1, path.length());
	}
	public static String volume_getPath(Object storageVolume){
		String path = "";
		String path_FromHelper= StorageVolumeHelper.getPath(storageVolume);
		if (path_FromHelper != null)
			path = path_FromHelper;
		return path;
	}
	public static String volume_getDescription(Object storageVolume,Context aContext){
		String Desc= "";
		try {
			Method method = storageVolume.getClass().getMethod("getDescription",Context.class);
			if (method != null){
				Desc = (String) (method.invoke(storageVolume,aContext));
			}
		}catch (Exception ignore){
			
		}
		return Desc;
	}
	public static Object[] getVolumeList(StorageManager stroageManager){
		Method mMethodGetStorageVolumeList;  
		Object[] sStorageVolumes = null; 
		try {
			mMethodGetStorageVolumeList = stroageManager.getClass().getMethod("getVolumeList"); 	
			sStorageVolumes = (Object[]) (mMethodGetStorageVolumeList.invoke(stroageManager));
		}catch (Exception ignore){
			
		}
		if (null == sStorageVolumes){
			sStorageVolumes = new Object[]{};
		}
		return sStorageVolumes;
	}
	public static Object[] getVolumes(StorageManager stroageManager){
		Method mMethodGetVolumes;
		List<Object> volumes = null;
		Object[] volumesArray = new Object[]{};
		try {
			mMethodGetVolumes = stroageManager.getClass().getMethod("getVolumes");
			volumes = (List<Object>) (mMethodGetVolumes.invoke(stroageManager));
			if (null != volumes){
				volumesArray = volumes.toArray(volumesArray);
			}
		}catch (Exception ignore){

		}
		return volumesArray;
	}
			
	public static File getLegacyExternalStorageDirectory(){
		Class<?> clsEnvironment = tryClassForName("android.os.Environment");
		Method mtdGet = tryGetMethod(clsEnvironment, "getLegacyExternalStorageDirectory");
		return tryInvoke(mtdGet, null);
	}
	public static String mediaFile_getMimeTypeForFile(String aFileName){
		Class<?> clsMediaFile= tryClassForName("android.media.MediaFile");
		Class<?>[] paramTypes= new Class[1];
		paramTypes[0]= String.class;
		Method mtdGet = tryGetMethod(clsMediaFile, "getMimeTypeForFile", paramTypes);
		String ret = null;
		ret = tryInvoke(mtdGet, null, aFileName);	
		if (ret != null){
			ret = ret.toLowerCase();
		}
		return ret;
	}
	public static int mediaFile_getFileTypeForMimeType(String mimeType){
		Class<?> clsMediaFile= tryClassForName("android.media.MediaFile");
		Class<?>[] paramTypes= new Class[1];
		paramTypes[0]= String.class;
		Method mtdGet = tryGetMethod(clsMediaFile, "getFileTypeForMimeType", paramTypes);
		return tryInvoke(mtdGet, null, mimeType);	
	}
	public static boolean mediaFile_isAudioFileType(int type){
		Class<?> clsMediaFile= tryClassForName("android.media.MediaFile");
		Class<?>[] paramTypes= new Class[1];
		paramTypes[0]= int.class;
		Method mtdGet = tryGetMethod(clsMediaFile, "isAudioFileType", paramTypes);
		return tryInvoke(mtdGet, null, type);	
	}
	public static boolean mediaFile_isImageFileType(int type){
		Class<?> clsMediaFile= tryClassForName("android.media.MediaFile");
		Class<?>[] paramTypes= new Class[1];
		paramTypes[0]= int.class;
		Method mtdGet = tryGetMethod(clsMediaFile, "isImageFileType", paramTypes);
		return tryInvoke(mtdGet, null, type);	
	}
	public static boolean mediaFile_isVideoFileType(int type){
		Class<?> clsMediaFile= tryClassForName("android.media.MediaFile");
		Class<?>[] paramTypes= new Class[1];
		paramTypes[0]= int.class;
		Method mtdGet = tryGetMethod(clsMediaFile, "isVideoFileType", paramTypes);
		return tryInvoke(mtdGet, null, type);	
	}
	public static AssetManager getAssetManagerWithPath(String path){
		AssetManager assetManager = null;
		
	    try {
	        assetManager = AssetManager.class.newInstance();
	        Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
	        addAssetPath.invoke(assetManager, path);
	    } catch (Exception e) {
	        e.printStackTrace();
	        assetManager = null;
	    }
	    return assetManager;
	}
	public static int getUserId(){
		Class<?> clsMediaFile= tryClassForName("android.os.UserHandle");
		Class<?>[] paramTypes= new Class[1];
		paramTypes[0]= int.class;
		Method mtdGet = tryGetMethod(clsMediaFile, "getUserId", paramTypes);
		return tryInvoke(mtdGet, null, android.os.Process.myUid());
	}
	/*
	public static void setMobileDataState(Context context, boolean mobileDataEnabled){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        	TelephonyManager telephonyService = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        	Method mtdSetDataEnabled = tryGetMethod(telephonyService.getClass(),"setDataEnabled", boolean.class);
        	try{
        		tryInvoke(mtdSetDataEnabled,telephonyService,mobileDataEnabled);
        	}catch (Throwable ignore){

        	}
        }else{
        	ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        	Method mtdSetDataEnabled = tryGetMethod(cm.getClass(),"setMobileDataEnabled",boolean.class);
        	tryInvoke(mtdSetDataEnabled,cm,mobileDataEnabled);
        }
	}

	public static boolean getMobileDataState(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        	TelephonyManager telephonyService = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        	Method mtdGetDataEnabled = tryGetMethod(telephonyService.getClass(),"getDataEnabled");
        	return tryInvoke(mtdGetDataEnabled,telephonyService);
        }else{
        	ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        	Method mtdGetDataEnabled = tryGetMethod(cm.getClass(),"getMobileDataEnabled");
        	return tryInvoke(mtdGetDataEnabled,cm);
        }
	}
	*/

    // Asus ZenUI 3.5 new feature "TwinApps" which will emulate a virtual user.
    // We can use reflection to get twinApps userId from UserManager.
    public static int getTwinAppsId() {
        try {
            Class<?> c = Class.forName("android.os.UserManager");
            Object ob = FileManagerApplication.getAppContext()
                    .getSystemService(Context.USER_SERVICE);
            Method m = c.getMethod("getTwinAppsId", (Class[]) null);
            return ((int) m.invoke(ob, (Object[]) null));
        } catch (Exception e) {
            return -1;
        }
    }

    // the path format may look like => /storage/emulated/[userId]
    public static String getTwinAppsStorageVolumePath() {
        String externalStorageDirectoryPath = Environment.getExternalStorageDirectory().getPath();
        int userId = reflectionApis.getUserId();
        int twinAppsId = reflectionApis.getTwinAppsId();
        if (userId == -1 || twinAppsId == -1) {
            return null;
        }

        if (externalStorageDirectoryPath.endsWith(String.valueOf(userId))) {
            int index = externalStorageDirectoryPath.lastIndexOf("/" + String.valueOf(userId));
            return externalStorageDirectoryPath.substring(0, index)
                    + "/" + twinAppsId;
        }
        return null;
    }

	public static Class<?> tryClassForName(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static Method tryGetMethod(Class<?> cls, String name,
			Class<?>... parameterTypes) {
		try {
			return cls.getDeclaredMethod(name, parameterTypes);
		} catch (Exception e) {
			return null;
		}
	}

	public static String stackTraceToString(Throwable e) {
	    StringBuilder sb = new StringBuilder();
	    for (StackTraceElement element : e.getStackTrace()) {
	        sb.append(element.toString());
	        sb.append("\n");
	    }
	    return sb.toString();
	}
	@SuppressWarnings("unchecked")
	private static <T> T tryInvoke(Method m, Object object, Object... args) {
		try {
			return (T) m.invoke(object, args);
		} catch (InvocationTargetException e) {
			Log.d("reflection",stackTraceToString(e.getCause()));
			throw new RuntimeException(e);
		} catch (Exception e) {
			return null;
		}
	}
	
}

