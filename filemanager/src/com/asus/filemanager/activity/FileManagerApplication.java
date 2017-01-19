
package com.asus.filemanager.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.multidex.MultiDex;
import android.text.TextUtils;
import android.util.Log;

import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.provider.AllFilesDatabase;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.settings.GeneralPreference;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ScanAllFilesTask;
import com.asus.filemanager.utility.ScanDuplicateFilesTask;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.service.cloudstorage.common.DiskInfoHelper;
import com.asus.service.cloudstorage.common.MsgObj;
import com.asus.service.cloudstorage.common.StorageVolumeHelper;
import com.asus.service.cloudstorage.common.VolumeInfoHelper;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.asus.filemanager.wrap.WrapEnvironment.supportFeature;

public class FileManagerApplication extends android.support.multidex.MultiDexApplication {

	public static Map<String, MsgObj>msgMap = new HashMap<String, MsgObj>();
	public static Map<String, VFile> msgDstPathMap = new HashMap<String, VFile>();

    private static final String TAG = "FileManagerApplication";
    private static Context sContext;
    private ArrayList<Object> mStorageVolumeList = null;
    private ArrayList<Object> mVolumeInfoList = null;
    private String[] mStorageVolumePath = null;
    private String[] mStorageVolumeState = null;
    private VFile[] mStorageFile = null;
    private StorageManager mStorageManager;
    private WifiManager mWifiManager;
    private boolean mIsAsus;
    public VolumeStateObserver mVolumeStateObserver;

    private static Activity activityReference = null;
    private HashMap<String, ScanAllFilesTask> scanAllFilesTaskHashMap = new HashMap<String, ScanAllFilesTask>();
    private ScanDuplicateFilesTask scanDuplicateFilesTask;
    private ScanDuplicateFilesTask.OnDuplicateFileResultListener onDuplicateFileResultListener;
    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // init the AyscHelper
        MediaProviderAsyncHelper.Init(this);
        EditorAsyncHelper.Init(this);
        sContext = getApplicationContext();

        // initialize the cache folder and clear it
        initCacheFiles();

        if (mStorageVolumeList == null) {
            initEnvironment();
        }

        ThemeUtility.retrieveAsusThemeId(sContext);

        mIsAsus = SafOperationUtility.getInstance().isAsusPhone();
        WrapEnvironment.SUPPORT_FEATURE_ASUS_PEN = supportFeature(FileManagerApplication.getAppContext(), "asus.hardware.pen");
        /*
         * Ensure the default values are set for any receiver, activity,
         * service, etc. of Task
         */
        GeneralPreference.setDefaultValues(this);

        if (!ItemOperationUtility.getInstance().hasSetFirstLaunchTime(sContext)) {
            Long currentTime = System.currentTimeMillis() / 1000; // in seconds
            ItemOperationUtility.getInstance().setFirstLaunchTime(sContext, currentTime);
        }
        mVolumeStateObserver = new VolumeStateObserver(FileManagerApplication.this);

		WrapEnvironment.IS_LOW_MEMORY_DEVICE = WrapEnvironment.isLowMemoryDevice(sContext);

        this.registerActivityLifecycleCallbacks(dataCollectionDAUcallback);

        ThemeUtility.initThemeType(getApplicationContext());
        //setCaughtExceptionHandler();
    }

    public static Context getAppContext() {
        return sContext;
    }

    private void setCaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.d("FileManager","uncaughtException: "+ex);
                StringWriter stackTrace = new StringWriter();
                ex.printStackTrace(new PrintWriter(stackTrace));
                System.err.println(stackTrace);
                String s = stackTrace.toString();
                if (s != null)
                    Log.d("ERROR", s);
                    
                // send firebase crash log
                FirebaseCrash.report(ex);

                Intent crashedIntent = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                crashedIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(crashedIntent);

                //for restarting the Activity
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });
    }


    // Generally, the external cache storage path for FileManager is
    // /mnt/sdcard/Android/data/com.asus.filemanager/cache
    // /storage/emulated/0/Android/data/com.asus.filemanager/cache jb-4.2
    // If the storage is not currently mounted, it will return null.
    // Delete all the cache files at first, then create necessary folders and files.
    private void initCacheFiles() {
        try {
            if (FileUtility.isExternalStorageAvailable() && getExternalCacheDir() != null) {
                LocalVFile[] cacheFiles = new LocalVFile(getExternalCacheDir()).listVFiles();
                if (cacheFiles != null) {
                    for(LocalVFile file : cacheFiles) {
                        FileUtility.delete(file);
                    }
                    Log.i(TAG, "delete cache files");
                }

                LocalVFile nomedia = new LocalVFile(getExternalCacheDir(), ".nomedia");
                LocalVFile pfile = new LocalVFile(getExternalCacheDir(), ".pfile/");
                LocalVFile cfile = new LocalVFile(getExternalCacheDir(), ".cfile/"); // for cloud storage
                LocalVFile sfile = new LocalVFile(getExternalCacheDir(), SambaFileUtility.SAMBA_CACHE_FOLDER);

                if (!nomedia.getParentFile().exists()) {
                    nomedia.getParentFile().mkdirs();
                }
                pfile.mkdir();
                cfile.mkdir();
                sfile.mkdir();
                nomedia.createNewFile();
                new LocalVFile(pfile, ".nomedia").createNewFile();
                new LocalVFile(cfile, ".nomedia").createNewFile();
                new LocalVFile(sfile, ".nomedia").createNewFile();
            
            }
        } catch (IOException e){
            e.printStackTrace();
        } catch (Throwable ignore){
            ignore.printStackTrace();
        }
    }

    // +++ Willie, initializes the mount points based on storage list of each device
    private void initEnvironment() {

        if (mStorageManager == null) {
            mStorageManager = (StorageManager) sContext.getSystemService(Context.STORAGE_SERVICE);
        }

        Object[] storageVolume = reflectionApis.getVolumeList(mStorageManager);
        Object[] VolumeInfo = reflectionApis.getVolumes(mStorageManager); //this will return volume info

        for (int i = 0; i < VolumeInfo.length; i ++) {
            Log.d("initEnvironment", "Volumeinfo = " + VolumeInfo[i].toString());
        }

        ArrayList<Object> tempStorageVolumeList = new ArrayList<Object>();
        if (VolumeInfo.length>0){
            //prefer volumeInfo
            mVolumeInfoList = new ArrayList<Object>();
            for (int i = 0; i < VolumeInfo.length; i++) {
                if (VolumeInfoHelper.getType(VolumeInfo[i]) == VolumeInfoHelper.TYPE_EMULATED ||
                    VolumeInfoHelper.getType(VolumeInfo[i]) == VolumeInfoHelper.TYPE_PUBLIC){
                    if (VolumeInfoHelper.isPrimary(VolumeInfo[i])){
                        tempStorageVolumeList.add(0,VolumeInfoHelper.buildStorageVolume(VolumeInfo[i], getAppContext(), reflectionApis.getUserId(), false));
                        mVolumeInfoList.add(0,VolumeInfo[i]);
                    }else{
                        tempStorageVolumeList.add(VolumeInfoHelper.buildStorageVolume(VolumeInfo[i], getAppContext(), reflectionApis.getUserId(), false));
                        mVolumeInfoList.add(VolumeInfo[i]);
                    }
                }
            }

            storageVolume = new Object[tempStorageVolumeList.size()];

            for (int i=0;i<tempStorageVolumeList.size();i++){
                storageVolume[i]  = tempStorageVolumeList.get(i);
            }
        }
        if (storageVolume != null) {
            mStorageVolumeList = new ArrayList<Object>();
            mStorageFile = new VFile[0];
            mStorageVolumePath = new String[0];
            mStorageVolumeState = new String[0];

            if (storageVolume.length > 0) {
                mStorageFile = new VFile[storageVolume.length];
                mStorageVolumePath = new String[storageVolume.length];
                mStorageVolumeState = new String[storageVolume.length];
            }

            for (int i = 0; i < storageVolume.length; i++) {
                // internal storage is filtered by isEmulated(),
                // external storage is filtered by isRemovable(),
                // we don't need otherwise
                //if (storageVolume[i].isEmulated() || storageVolume[i].isRemovable()) {
                    mStorageVolumeList.add(storageVolume[i]);
                //}

                if (Environment.getExternalStorageDirectory().getAbsolutePath().equals(reflectionApis.volume_getPath(mStorageVolumeList.get(i)))) {
                    mStorageFile[i] = new LocalVFile(WrapEnvironment.getEpadInternalStorageDirectory());
                } else {
                    String path = reflectionApis.volume_getPath(mStorageVolumeList.get(i));

                    if(WrapEnvironment.SUPPORT_REMOVABLE){
                        if(path.equals("/storage/MicroSD") || path.equals("/storage/sdcard1")){
                            mStorageFile[i] = new LocalVFile("/Removable/MicroSD");
                        } else if(path.equals("/storage/USBdisk1")){
                            mStorageFile[i] = new LocalVFile("/Removable/USBdisk1");
                        } else if (path.equals("/storage/USBdisk2")) {
                            mStorageFile[i] = new LocalVFile("/Removable/USBdisk2");
                        } else if (path.equals("/storage/USBdisk3")) {
                            mStorageFile[i] = new LocalVFile("/Removable/USBdisk3");
                        } else if (path.equals("/storage/USBdisk4")) {
                            mStorageFile[i] = new LocalVFile("/Removable/USBdisk4");
                        } else if (path.equals("/storage/USBdisk5")) {
                            mStorageFile[i] = new LocalVFile("/Removable/USBdisk5");
                        } else{
                            mStorageFile[i] = new LocalVFile(path);
                        }
                    }else{
                        mStorageFile[i] = new LocalVFile(path);
                    }
                }

                mStorageVolumePath[i] = reflectionApis.volume_getPath(mStorageVolumeList.get(i));
                mStorageVolumeState[i] = reflectionApis.getVolumeState(mStorageManager, mStorageVolumeList.get(i));
            }

        } else {
            Log.w(TAG, "storageVolume is null when calling initEnvironment()");
        }
        

        if (!TextUtils.isEmpty(android.os.Build.MANUFACTURER) && android.os.Build.MANUFACTURER.equalsIgnoreCase("HUAWEI")){
            if (StorageVolumeHelper.isEmulated(mStorageVolumeList.get(0))) {
                WrapEnvironment.SDCARD_CANONICAL_PATH = mStorageVolumePath[0];
                if (mStorageVolumePath.length>1)
                    WrapEnvironment.MICROSD_CANONICAL_PATH = mStorageVolumePath[1];
            }else{
                WrapEnvironment.SDCARD_CANONICAL_PATH = mStorageVolumePath[1];
                WrapEnvironment.MICROSD_CANONICAL_PATH = mStorageVolumePath[0];
            }
        }else {
            if (null != mVolumeInfoList){
                //mVolumInfoList will on available on M and above
                int iUSBCount = 0;
                for (int i=0;i<mVolumeInfoList.size();i++){
                    Object diskInfo = VolumeInfoHelper.getDisk(mVolumeInfoList.get(i));
                    Log.d(TAG, "storageVolume: getDisk= " + diskInfo);
                    if (null != diskInfo){
                        if (DiskInfoHelper.isSd(diskInfo)){
                            WrapEnvironment.MICROSD_CANONICAL_PATH = mStorageVolumePath[i];
                        }else if (DiskInfoHelper.isUsb(diskInfo)){
                            if (iUSBCount == 0){
                                iUSBCount++;
                                WrapEnvironment.USBDISK1_CANONICAL_PATH = mStorageVolumePath[i];
                            }else if (iUSBCount == 1){
                                iUSBCount++;
                                WrapEnvironment.USBDISK2_CANONICAL_PATH = mStorageVolumePath[i];
                            }else if (iUSBCount == 2){
                                iUSBCount++;
                                WrapEnvironment.USBDISK3_CANONICAL_PATH = mStorageVolumePath[i];
                            }else if (iUSBCount == 3){
                                iUSBCount++;
                                WrapEnvironment.USBDISK4_CANONICAL_PATH = mStorageVolumePath[i];
                            }else if (iUSBCount == 4){
                                iUSBCount++;
                                WrapEnvironment.USBDISK5_CANONICAL_PATH = mStorageVolumePath[i];
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isNeedSafPermission(){
        boolean bNeedSaf = false;
        if (null != mStorageVolumePath){
            for (int i=0;i<mStorageVolumePath.length;i++){
                if (mStorageVolumeState[i].equals(Environment.MEDIA_MOUNTED)){
                    bNeedSaf = SafOperationUtility.getInstance().isNeedToShowSafDialog(mStorageVolumePath[i],false);
                    if (bNeedSaf){
                        break;
                    }
                    Log.w(TAG, "storageVolume: path = " + mStorageVolumePath[i] + " need saf:" + bNeedSaf);
                }else{
                    Log.w(TAG, "storageVolume: path = " + mStorageVolumePath[i] + " need saf: no need");
                }
            }
        }else{
            return bNeedSaf;
        }
        return bNeedSaf;
    }

    public void reInitEnvironment() {
        mStorageVolumeList = null;
        mVolumeInfoList = null;
        initEnvironment();
    }

    public String[] getStorageVolumePaths() {
        if (mStorageVolumeList == null) {
            initEnvironment();
        }
        return mStorageVolumePath;
    }

    public VFile[] getStorageFile() {
        if (mStorageVolumeList == null) {
            initEnvironment();
        }
        return mStorageFile;
    }

    public ArrayList<Object> getStorageVolume(){
        if (mStorageVolumeList == null) {
            initEnvironment();
        }
        return mStorageVolumeList;
    }

    public StorageManager getStorageManager(){
        return mStorageManager;
    }

    public boolean isWiFiTurnOn() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        }
        return mWifiManager.isWifiEnabled();
    }

    public boolean isWifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi.isConnected();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }
    public boolean isASUS(){
        return mIsAsus;
    }

    private ActivityLifecycleCallbacks dataCollectionDAUcallback = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if(FileManagerActivity.isFromStorageAnalyzer)
                return;
//            if(activityReference==null)
//            {
//                Log.i(TAG, "sendDataCollectionForDAU");
//                new DataCollectionTask(FileManagerApplication.this,DataCollectionTask.PAGE_ID_START_FOREGROUND).execute();
//            }
//            else if(activity.equals(activityReference))
//            {
//                Log.i(TAG, "sendDataCollectionForDAU");
//                new DataCollectionTask(FileManagerApplication.this,DataCollectionTask.PAGE_ID_START_FOREGROUND).execute();
//            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            activityReference = activity;
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if(activity.equals(activityReference))
            {
                activityReference = null;
            }
        }
    };

    //for storage analyzer
    public void startScanAllFiles(ScanAllFilesTask.OnScanAllFilesResultListener onScanAllFilesResultListener,String... rootPaths)
    {
        if(rootPaths==null)
            return;

        for(int i=0;i<rootPaths.length;i++)
        {
            String path = rootPaths[i];
            ScanAllFilesTask sacaAllFilesTask;
            if(scanAllFilesTaskHashMap.containsKey(path))
            {
                sacaAllFilesTask = scanAllFilesTaskHashMap.get(path);
                if(sacaAllFilesTask.isRunning())
                {

                    sacaAllFilesTask.setOnScanAllFilesResultListener(onScanAllFilesResultListener);
                    continue;
                }
            }
            Log.i(TAG, "FileManagerApplication startScanAllFiles");
            //Task not exist or finished, create new one
            sacaAllFilesTask = new ScanAllFilesTask(getApplicationContext(),onScanAllFilesResultListener);
            sacaAllFilesTask.setFileManagerApplication(this);
            sacaAllFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,new VFile(path));
            scanAllFilesTaskHashMap.put(path, sacaAllFilesTask);
        }
    }

    public boolean isScanAllFilesFinished(String rootPath,ScanAllFilesTask.OnScanAllFilesResultListener onScanAllFilesResultListener)
    {
        if(scanAllFilesTaskHashMap.containsKey(rootPath))
        {
            ScanAllFilesTask sacaAllFilesTask = scanAllFilesTaskHashMap.get(rootPath);
            if(sacaAllFilesTask.isRunning())
            {
                sacaAllFilesTask.setOnScanAllFilesResultListener(onScanAllFilesResultListener);
                return false;
            }
        }
        return true;
    }

    public boolean isScanAllFilesFinished()
    {
        return isScanAllFilesFinished(null);
    }

    public boolean isScanAllFilesFinished(ScanAllFilesTask.OnScanAllFilesResultListener onScanAllFilesResultListener)
    {
        for(ScanAllFilesTask scanAllFilesTask: scanAllFilesTaskHashMap.values())
        {

            if(scanAllFilesTask.isRunning()) {
                if(onScanAllFilesResultListener!=null)
                    scanAllFilesTask.setOnScanAllFilesResultListener(onScanAllFilesResultListener);
                return false;
        }
        }
        return true;
    }

    public void startScanDuplicateFiles(int mode,ScanDuplicateFilesTask.OnDuplicateFileResultListener onDuplicateFileResultListener,String... rootPaths)
    {
        if(!isScanDupFilesFinished(onDuplicateFileResultListener))
        {
            return;
        }

        if(!isScanAllFilesFinished())
        {
            return;
        }

        if(rootPaths==null) {
            AllFilesDatabase allFilesDatabase = new AllFilesDatabase(this);
            List<String> tmpPaths = allFilesDatabase.getGroupRootPaths(true);
            if(tmpPaths.size()!=0)
                rootPaths = tmpPaths.toArray(new String[tmpPaths.size()]);
        }
        switch (mode)
        {
            case ScanDuplicateFilesTask.MODE_RESCAN:
                Log.i(TAG,"FileManagerApplication start ScanDuplicateFilesTask MODE_RESCAN");
                scanDuplicateFilesTask = new ScanDuplicateFilesTask(this, this.onDuplicateFileResultListener);
                scanDuplicateFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,rootPaths);
                break;
            case ScanDuplicateFilesTask.MODE_SCAN_RECORDED:
                Log.i(TAG,"FileManagerApplication start ScanDuplicateFilesTask MODE_SCAN_RECORDED");
                scanDuplicateFilesTask = new ScanDuplicateFilesTask(this, this.onDuplicateFileResultListener,scanDuplicateFilesTask.MODE_SCAN_RECORDED);
                scanDuplicateFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, rootPaths);
                break;
        }
    }

    public boolean isScanDupFilesFinished(ScanDuplicateFilesTask.OnDuplicateFileResultListener onDuplicateFileResultListener)
    {
        if(onDuplicateFileResultListener!=null) {
            this.onDuplicateFileResultListener = onDuplicateFileResultListener;
        }
        if(scanDuplicateFilesTask!=null)
        {
            if(scanDuplicateFilesTask.isRunning())
            {
                scanDuplicateFilesTask.setOnDuplicateFileResultListener(this.onDuplicateFileResultListener);
                return false;
            }
        }
        return true;
    }
}
