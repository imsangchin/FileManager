package com.asus.filemanager.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;

import com.asus.filemanager.utility.reflectionApis;
import com.asus.service.cloudstorage.common.VolumeInfoHelper;
import com.github.junrar.Volume;

import java.io.File;
import java.io.IOException;
import java.util.Observable;

import static com.asus.filemanager.utility.StorageEventListenerHelper.setOnStorageEventListener;

/**
 * Created by Lancelot on 15/12/31.
 */
public class VolumeStateObserver extends Observable{
    private FileManagerApplication mApplication;
    StorageManager mStorageManager;
    private static final String TAG = "VolumeStateObserver";
    public static final String KEY_EVENT = "KEY_EVENT";
    public static final String KEY_PATH = "KEY_PATH";

    private VolumeStateObserver(){};

    public VolumeStateObserver(FileManagerApplication anApplicationContext){
        mApplication = anApplicationContext;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme("file");
        anApplicationContext.registerReceiver(mReceiver, filter);

        mStorageManager = mApplication.getStorageManager();
        if (null != mStorageManager){
            setOnStorageEventListener(mStorageManager, storageEventListener);
        }
    }

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive " + intent.getAction());
            mApplication.reInitEnvironment();

            String path = null;
            Object storageVolume = (Object) intent.getParcelableExtra(reflectionApis.EXTRA_STORAGE_VOLUME);
            if (storageVolume!=null) {
                path = reflectionApis.volume_getPath(storageVolume);
                if (path == null) {
                    Log.d(TAG, "onReceive path is null");
                }
            }

            if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
                //notify here
                VolumeStateObserver.this.setChanged();
                Bundle aBundle = new Bundle();
                aBundle.putString(KEY_EVENT, Intent.ACTION_MEDIA_MOUNTED);
                aBundle.putString(KEY_PATH, path);
                VolumeStateObserver.this.notifyObservers(aBundle);
            }else if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)){
                //notify here
                VolumeStateObserver.this.setChanged();
                Bundle aBundle = new Bundle();
                aBundle.putString(KEY_EVENT, Intent.ACTION_MEDIA_UNMOUNTED);
                aBundle.putString(KEY_PATH, path);
                VolumeStateObserver.this.notifyObservers(aBundle);
            }
        }
    };

    private StorageEventListener storageEventListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            Log.d(TAG, "onVolumeStateChanged");
            mApplication.reInitEnvironment();

            String newStateStr = VolumeInfoHelper.getEnvironmentForState(newState);
            File pathFile= VolumeInfoHelper.getPath(vol);
            String path = null;
            if (pathFile!= null){
                path = pathFile.getAbsolutePath();
                try {
                    path = pathFile.getCanonicalPath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (newStateStr.equals(Environment.MEDIA_MOUNTED)){
                //notify here
                notifyObservers(path, Intent.ACTION_MEDIA_MOUNTED);
            }else if (newStateStr.equals(Environment.MEDIA_UNMOUNTED)){
                //notify here
                notifyObservers(path, Intent.ACTION_MEDIA_UNMOUNTED);
            }else if (newStateStr.equals(Environment.MEDIA_EJECTING)){
                //notify here
                notifyObservers(path, Intent.ACTION_MEDIA_EJECT);
            }
        }

        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.d(TAG, "onStorageStateChanged");
        }

        //public void onDiskScanned(DiskInfo disk, int volumeCount) {
        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            Log.d(TAG, "onDiskScanned");
        }

        //public void onDiskDestroyed(DiskInfo disk) {
        public void onDiskDestroyed(DiskInfo disk) {
            Log.d(TAG, "onDiskDestroyed");
        }
    };

    private void notifyObservers(String path, String event) {
        VolumeStateObserver.this.setChanged();
        Bundle aBundle = new Bundle();
        aBundle.putString(KEY_EVENT, event);
        if (path != null){
           aBundle.putString(KEY_PATH, path);
        }
        VolumeStateObserver.this.notifyObservers(aBundle);
    }
}
