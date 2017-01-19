package com.asus.filemanager.saf;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;

import android.R.string;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

public class SafOperationUtility {
	private static final String TAG = "SAF";
	private static final boolean DEBUG = ConstantsUtil.DEBUG;
	
    public static SafOperationUtility mSafInstance = null;
    private Context mContext = null;
    
    private static final String GET_DEVICE_BRAND = "getprop ro.product.brand";
	//private static final String  MicroSdRootPath = WrapEnvironment.getEpadExternalStorageDirectory().getAbsolutePath() + "/MicroSD";
    
    /***action type******/
	public static final int ACTION_PASTE = 9;
    public static final int ACTION_COPY_TO = 10;
    public static final int ACTION_MOVE_TO = 11;
    public static final int ACTION_DELETE = 12;
    public static final int ACTION_RENAME = 13;
    public static final int ACTION_MKDIR = 14;
    public static final int ACTION_ZIP = 15;
    public static final int ACTION_EXTRACT = 16;
    public static final int ACTION_CREATE = 17;
    public static final int ACTION_RESTORE = 18;
    public static final int ACTION_MOVE_TO_HIDDEN_ZONE = 19;
    
    public interface SafActionHandler {
        void handleAction(int action);
    }
    private LocalVFile mChooseDFile = null;
    
    private int Call_Action = -1;
    
	private HashMap<String,Boolean> mWriteCapMap;
	private boolean mHasDocumentUI;

	private boolean checkWriteMediaStoragePermission() {
		boolean granted = false;
		if (null != mContext && mContext.checkCallingOrSelfPermission("android.permission.WRITE_MEDIA_STORAGE") == PackageManager.PERMISSION_GRANTED) {
			granted = true;
		}
		return granted;
	}

    public static SafOperationUtility getInstance(Context context){
    	if(mSafInstance == null){
    		mSafInstance = new SafOperationUtility(context);
    	}
    	return mSafInstance;
    }
    public static SafOperationUtility getInstance(){
    	if(mSafInstance == null){
    		mSafInstance = new SafOperationUtility(FileManagerApplication.getAppContext());
    	}
    	return mSafInstance;

    }
    
    public SafOperationUtility (Context context){
    	mContext = context;
		mHasDocumentUI = false;
		if (mContext != null){
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			List<ResolveInfo> resInfo = mContext.getPackageManager().queryIntentActivities(intent,0);
			if (null == resInfo || resInfo.size() <=0) {
				//if we can't find SAF activity, treat it as we don't need it seen we can't do anything if we can't launch SAF
				Log.d(TAG, "cannot find SAF activity");
				mHasDocumentUI = false;
			}else{
				mHasDocumentUI = true;
			}
		}
		mWriteCapMap = new HashMap<String,Boolean>();
    }
    
    public void setCallSafAction(int action){
    	Call_Action = action;
    }
    
    public int getCallSafAction(){
    	return this.Call_Action;
    }
    
    public void setChoosedFile(LocalVFile file){
    	mChooseDFile = file;
    }
    
    public VFile getChoosedFile(){
    	return this.mChooseDFile;
    }

    public boolean hasZenuiSoftwareFeature() {
        Context context = FileManagerApplication.getAppContext();
        PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature("asus.software.zenui")) {
            return true;
        } else {
            return false;
        }
    } 
	public String dfwrapper(){
		try {
			Process p = Runtime.getRuntime().exec("df");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			int read;
			char[] buffer = new char[4096];
			StringBuilder output = new StringBuilder();
			while((read = reader.read(buffer)) > 0){
				output.append(buffer, 0, read);
			}
			reader.close();
			String dfresult = output.toString();

			if(!TextUtils.isEmpty(dfresult))
				return dfresult;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}
	public String duwrapper(){
		try {
			String[] cmd = {
				"du",
				"-chs",
				"/storage/*"
			};
			String []cmd2 = {
				"du",
				"-d0",
				"-h",
				"/storage/MicroSD"
			};

			Process p = Runtime.getRuntime().exec(cmd2);
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			int read;
			char[] buffer = new char[4096];
			StringBuilder output = new StringBuilder();
			while((read = reader.read(buffer)) > 0){
				output.append(buffer, 0, read);
			}
			reader.close();
			String duresult = output.toString();

			BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			StringBuilder errOutput = new StringBuilder();
			StringBuilder err = new StringBuilder();
			while((read = errReader.read(buffer)) > 0){
				errOutput.append(buffer, 0, read);
			}
			errReader.close();
			String duerr = output.toString();
			if(!TextUtils.isEmpty(duresult))
				return duresult;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

	public static long getDirSizes(File dir)
	{
		long ret = 0;
		Map<String, Long> sizes = new HashMap<String, Long>();

		try {
			//Process du = Runtime.getRuntime().exec("/system/bin/du -s "+dir.getCanonicalPath(), new String[]{}, Environment.getRootDirectory());
			Process du = Runtime.getRuntime().exec("du -d0  "+dir.getCanonicalPath());
			BufferedReader in = new BufferedReader(new InputStreamReader(
				du.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split("\\s+");
				String sizeStr = parts[0];
				Long size = Long.parseLong(sizeStr);
				String path = parts[1];
				if (path != null && size != null)
					sizes.put(path, size);
			}
		} catch (IOException e) {
			Log.w(TAG, "Could not execute DU command for " + dir.getAbsolutePath(), e);
		}
		if (null != sizes){
			try{
				Long retLong = sizes.get(dir.getCanonicalPath());
				if (retLong != null)
					ret = retLong.longValue();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	public boolean isAsusPhone(){
	    if (hasZenuiSoftwareFeature())
	        return true;
		boolean result = false;
		try {
			Process p = Runtime.getRuntime().exec(GET_DEVICE_BRAND);
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			int read;
	            char[] buffer = new char[4096];
	            StringBuilder output = new StringBuilder();
	            while((read = reader.read(buffer)) > 0){
	                output.append(buffer, 0, read);
	            }
	            reader.close();
	            String brand = output.toString();

	            if(!TextUtils.isEmpty(brand) && brand.toLowerCase().contains("asus")){
	            	result = true;
	            }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
    public boolean isNeedToShowSafDialog(String targetPath,boolean setChoosedFile)
    {
		String aRootPath = getRootPathFromFullPath(targetPath);
	    if(isNeedToWriteSdBySaf(targetPath) && (checkPermissionIsObtained(aRootPath) == null)){
	    	Log.d(TAG,"==isNeedToShowSafDialog=true");
            if(setChoosedFile)
	    	    setChoosedFile(new LocalVFile(targetPath));
	    	return true;
	    }
        return false;
    }

    public boolean isNeedToShowSafDialog(String targetPath){
        return isNeedToShowSafDialog(targetPath,true);
	}
	
	public boolean isNeedToWriteSdBySaf(String targetPath){

		String aRootPath = getRootPathFromFullPath(targetPath);
		if (null == aRootPath)
			return false;

		boolean need = false;

		if(Build.VERSION.SDK_INT >= 21){
            Log.d(TAG, "==isNeedToWriteSdBySaf= SDK_INT > 21, checking SAF activity");
			need = true;
			if (!mHasDocumentUI){
				need = false;
			}

			Log.d(TAG, "==isNeedToWriteSdBySaf= SDK_INT > 21, checking write capability for path " + aRootPath);

			Boolean bWriteCap = mWriteCapMap.get(aRootPath);
			if (null != bWriteCap) {
				Log.d(TAG, "get write cap for path from cache = " + bWriteCap);
				need = !(bWriteCap.booleanValue());
			}else{
				//check if we have write permission to this folder
				File aTempFile = null;
				try{
					if (!TextUtils.isEmpty(aRootPath))
						aTempFile = (File.createTempFile("FileManager","tmp",new File(aRootPath)));
				}catch (IOException ignore){
                    Log.d(TAG, "we don't have write permission reason " + ignore);
				}
				if (null != aTempFile && aTempFile.exists()) {
					Log.d(TAG, "we have write permission");
					need = false;
					aTempFile.delete();
				}
				mWriteCapMap.put(aRootPath,!need);
			}

		}

		Log.d(TAG, "==isNeedToWriteSdBySaf= " + need);
		return need;
	}

	public boolean isNeedToWriteSdToAppFolder(String targetPath){
		boolean need = false;
		if(Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21 && !isAsusPhone() ){
			String aRootPath = getRootPathFromFullPath(targetPath);
			if (aRootPath != null && !aRootPath.startsWith(WrapEnvironment.SDCARD_CANONICAL_PATH)) {
				Log.d(TAG, "==isNeedToWriteSdToAppFolder=true");
				need = true;
			}
		}
		if (need){
			String aRootPath = getRootPathFromFullPath(targetPath);
			//check if we have write permission to this folder

			File aTempFile = null;
			try{
				if (!TextUtils.isEmpty(aRootPath))
					aTempFile = (File.createTempFile("FileManager","tmp",new File(aRootPath)));
			}catch (IOException ignore){

			}
			if (null != aTempFile && aTempFile.exists())
			{
				Log.d(TAG, "though we it seems to be blongs to SD card, but we have write permission");
				need = false;
				aTempFile.delete();
			}
		}
		return need;
	}	

	@TargetApi(19)
	private Uri checkPermissionIsObtained(String rootPath){
        Uri mSDcardUri = null;
        if (null == rootPath)
        	return mSDcardUri;
        File aFile = new File(rootPath);
        String rootFileName = aFile.getName();

		List<UriPermission> permissionList = mContext.getContentResolver().getPersistedUriPermissions();
		if(permissionList != null && permissionList.size() > 0){
			for(UriPermission p : permissionList){
				try{
					DocumentFile rootDir = DocumentFile.fromTreeUri(mContext, p.getUri());
					String name = rootDir.getName();
					if (DEBUG)
					    Log.d(TAG,"checkPermissionIsObtained, rootDir name = " + name + "rootFileName = " + rootFileName);
					if(rootDir.getName() != null && rootDir.getName().equals(rootFileName)){
						mSDcardUri = p.getUri();
						break;
					}

				}catch (IllegalArgumentException e){
					Log.d(TAG,"====e====" + e);
				}
			}
			
		}else{
			Log.d(TAG,"====no permission====");
		}
		return mSDcardUri;
	}
	
	public DocumentFile getDocFileFromPath(String targetpath){
		Uri rootUri = checkPermissionIsObtained(getRootPathFromFullPath(targetpath));
		if(rootUri == null){
			return null;
		}
		DocumentFile mDestFile = null;
		File destFile = new File(targetpath);
		String keyPath = getDocumentPath(targetpath);
		if (DEBUG)
		    Log.d(TAG,"=full path==" + destFile.getAbsolutePath());
		if(destFile.exists()){
			String destName = destFile.getName();	
			DocumentFile rootDir = DocumentFile.fromTreeUri(mContext, rootUri);
			if (DEBUG)
			    Log.d(TAG,"===rootDir==" + rootDir + "  rootUri=" + rootUri + "  rootDir name = " + rootDir.getName() + "  destName = " + destName);
			if(rootDir.getName().equals(destName)){
				mDestFile = rootDir;
			}else{
                //[TT-830535][TT-828628], when targetPath=sdcard then keyPath get null cause crash
                if(keyPath==null) {
                    return null;
                }
		        Uri treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri,DocumentsContract.getTreeDocumentId(rootUri));
		        Cursor cursor = mContext.getContentResolver().query(treeUri, null, null, null, null);
		        if(cursor != null){
			        if(cursor.getCount() != 0){
				        while(cursor.moveToNext()){
					        int id = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
					        String fileId = cursor.getString(id);
					        if(keyPath.equals(getFilePathById(fileId))){
					        	 mDestFile = DocumentFile.fromSingleUri(mContext, DocumentsContract.buildDocumentUriUsingTree(rootUri,fileId));
					        	 break;
					        }
					        if(keyPath.startsWith(getFilePathById(fileId))){
				                Uri tUri = DocumentsContract.buildDocumentUriUsingTree(rootUri,fileId);
						        SearchOk = false;
						        Uri result = getMatchDocFileUri(mContext,tUri,keyPath);
						        while(!SearchOk){
							        result = getMatchDocFileUri(mContext,result,keyPath);
							        Log.d(TAG,"==result===" + result);
						        }
						        if(result != null){
						            mDestFile = DocumentFile.fromSingleUri(mContext, result);
						            break;
						        }
					        }
				       }
			       }
			    cursor.close();
		        }
	        }
	    }
	    return mDestFile;
	}
	
//	private String getAbsolutePathByUri(Uri docUri){
//        final String docId = DocumentsContract.getDocumentId(docUri);
//	    final String[] split = docId.split(":");
//	    return SdCardRootPath + "/" + split[1];
//    }

	public String getRootPathFromFullPath(String fullpath){
		try {
			fullpath = new File(fullpath).getCanonicalPath();
			fullpath = FileUtility.getCanonicalPathForUser(fullpath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        String[] mEnviroment = ((FileManagerApplication)mContext.getApplicationContext()).getStorageVolumePaths();

        ArrayList<String> mRoots = new ArrayList<String>(Arrays.asList(mEnviroment));
        Collections.sort(mRoots, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				if (lhs == null || rhs == null)
					return 0;
				return (rhs.length() - lhs.length());
			}
		});

		String rootPath = null;

		for (String root : mRoots){
			if(fullpath != null && root != null
			        && fullpath.startsWith(root)) {
				rootPath = root;
				break;
			}	
		}

		if (rootPath == null) {
		    // not find root path, try to check TwinApps volume path;
		    rootPath = reflectionApis.getTwinAppsStorageVolumePath();
		}

		if (DEBUG)
		    Log.d(TAG,"getRootPathFromFullPath rootPath==" + rootPath +", fullpath = " + fullpath);
		return rootPath;
	}

	public Object getStorageVolumeFromFullPath(String fullpath){
		String aRootPath = getRootPathFromFullPath(fullpath);
		String[] mEnvironment = ((FileManagerApplication)mContext.getApplicationContext()).getStorageVolumePaths();
		ArrayList<Object> mStorageVolume = ((FileManagerApplication)mContext.getApplicationContext()).getStorageVolume();

		Object storageVolume = null;
		for (int i=0;i<mEnvironment.length;i++){
			if (mEnvironment[i].compareTo(aRootPath) == 0){
				storageVolume =	mStorageVolume.get(i);
				break;
			}
		}
		return storageVolume;
	}

	private String getDocumentPath(String fullpath){
		try {
			fullpath = new File(fullpath).getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        String[] mEnviroment = ((FileManagerApplication)mContext.getApplicationContext()).getStorageVolumePaths();
        ArrayList<String> mRoots = new ArrayList<String>(Arrays.asList(mEnviroment));
        Collections.sort(mRoots, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				return (rhs.length() - lhs.length());
			}
		});

		String docPath = null;

		for (String root : mRoots){
			if(fullpath.startsWith(root)){
				String[] pArray = fullpath.split(root);
				if(pArray != null && pArray.length == 2){
					docPath = pArray[1] + File.separator;
				}
				break;
			}	
		}
		/*
		if(fullpath.startsWith(MicroSdRootPath)){
			String[] pArray = fullpath.split(MicroSdRootPath);
			if(pArray != null && pArray.length == 2){
				docPath = pArray[1] + File.separator;
			}
		}
		*/
		if (DEBUG)
		    Log.d(TAG,"==docPath==" + docPath + ", source=" + fullpath);
		return docPath;
	}
	
	private String getFilePathById(String id){
	    final String[] split = id.split(":");
	    return "/" + split[1] + File.separator;
	}
    
    private static boolean SearchOk = false;
    public Uri getMatchDocFileUri(Context context, Uri self,String key) {
		if (self == null) {
			SearchOk = true;
			return null;
		}
        final ContentResolver resolver = context.getContentResolver();
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(self,
                DocumentsContract.getDocumentId(self));
        Uri matchUri = null;

        Cursor c = null;
        try {
            c = resolver.query(childrenUri, new String[] {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID }, null, null, null);
            while (c.moveToNext()) {
                final String documentId = c.getString(0);
                String absPath = getFilePathById(documentId);
                if(key.startsWith(absPath)){
					if (DEBUG)
                	    Log.d(TAG,"=key=" + key + "==absPath==" + absPath);
                	if(key.equals(absPath)){
                		SearchOk = true;
                        matchUri = DocumentsContract.buildDocumentUriUsingTree(self,
                                documentId);
                		break;
                	}
                    matchUri = DocumentsContract.buildDocumentUriUsingTree(self,
                        documentId);
                    break;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed query: " + e);
        } finally {
            closeQuietly(c);
        }

        return matchUri;
    }
    
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }
    
    private ParcelFileDescriptor pdf = null;
    public BufferedOutputStream getDocFileOutputStream(DocumentFile file){
    	BufferedOutputStream ots = null;
		try {
			if(pdf != null){
				pdf = null;
			}
			pdf = mContext.getContentResolver().openFileDescriptor(file.getUri(), "w");
			ots = new BufferedOutputStream(new FileOutputStream(pdf.getFileDescriptor()));			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ots;
		
    }
    
    public void closeParcelFile(){
    	try {
    		if(pdf != null){
			    pdf.close();
    		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void createNotExistFolder(File f){
 		File tempF = f;
 		Stack<File> mParentFolder = new Stack<File>();
 		while(!tempF.getParentFile().exists()){
   			tempF = tempF.getParentFile();
 			mParentFolder.push(tempF);
			if (DEBUG)
 			    Log.d(TAG,"====tempF==" + tempF.getAbsolutePath());
 		}

 		while(!mParentFolder.isEmpty()){
 			File tFile = mParentFolder.pop();
 			if(!tFile.exists()){
		        DocumentFile parentFile = getDocFileFromPath(tFile.getParent());
				if (DEBUG)
		            Log.d(TAG,"===.getfParentFile()=====" + tFile.getParent());
		        if(parentFile != null){
	    	        parentFile.createDirectory(tFile.getName());
	            }
 			}
 		}
 		if(!mParentFolder.isEmpty()){
 			mParentFolder.clear();
 		}
    }
	public void clearWriteCapMap(){
		mWriteCapMap.clear();
	}
		
	public boolean belongToInternalStorage(String targetPath) {
		String aRootPath = getRootPathFromFullPath(targetPath);
		if (aRootPath != null && !aRootPath.startsWith(WrapEnvironment.SDCARD_CANONICAL_PATH)) {
			return false;
		}
		return true;
	}
}
