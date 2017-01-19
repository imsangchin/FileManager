
package com.asus.filemanager.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.remote.utility.RemoteAccountUtility.AccountInfo;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;


/** Fragment for short cuts,My storage,info */
public class ShortCutFragment extends Fragment implements Observer{
    public final static int MSG_UPDATE_UI = 1;
    public final static int MSG_TOKEN_SCAN_FILE=2;
    public final static int MSG_REMOVE_UI = 3;
    private static final String TAG = "ShortCutFragment";
    private static final String EXTRA_HIDE_REMOVABLE_STORAGE = "extra_hide_removable_storage";
    private boolean mIsHideRemovableStorage = false;
    public static VFile currentTokenFile = null;


    public BroadcastReceiver mReviverLocalChange = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("felix_zhang","mReviverLocalChange,");
            FileManagerActivity.isLocaleChanged = true;
        }
    };


    // Expandable List +++

    ExpandableListView mList;
    View mEmptyView;
    TextView mStandardEmptyView;
    View mProgressContainer;
    View mListContainer;
    boolean mListShown;

    private void scanRemoteFile(VFile file){
        FileListFragment fragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        FileManagerActivity activity = (FileManagerActivity)getActivity();
        if (fragment != null) {
            if(file instanceof RemoteVFile && ((RemoteVFile)file).getStorageType()==StorageType.TYPE_HOME_CLOUD){
                fragment.setListShown(false);
                fragment.SetScanHostIndicatorPath(((RemoteVFile)file).getStorageName());
                RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(((RemoteVFile)file).getStorageName(), null, null, ((RemoteVFile)file).getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                if(((FileManagerActivity)getActivity()).getIsShowSearchFragment()){
                    ((FileManagerActivity)getActivity()).showSearchFragment(FileManagerActivity.FragmentType.NORMAL_SEARCH, false);
                }
                activity.setActionBarTitle(RemoteAccountUtility.getInstance(getActivity()).findCloudTitleByMsgObjType(getActivity(), ((RemoteVFile)file).getMsgObjType()));
                activity.setSeclectedStorage(RemoteAccountUtility.getInstance(getActivity()).findCloudTitleByMsgObjType(getActivity(), ((RemoteVFile)file).getMsgObjType()) + ((RemoteVFile)file).getStorageName(), null);
                return;
            }
//          mFolderTreeAdapter.notifyDataSetChanged();

            SambaFileUtility.updateHostIp = false;
            RemoteFileUtility.isShowDevicesList = false;
            FileManagerActivity.isSearchIng = false;

            fragment.startScanFile(file, ScanType.SCAN_CHILD , false);


            activity.setActionBarTitle(RemoteAccountUtility.getInstance(getActivity()).findCloudTitleByMsgObjType(getActivity(), ((RemoteVFile)file).getMsgObjType()));
            activity.setSeclectedStorage(RemoteAccountUtility.getInstance(getActivity()).findCloudTitleByMsgObjType(getActivity(), ((RemoteVFile)file).getMsgObjType()) + ((RemoteVFile)file).getStorageName(), null);
            if (!activity.isPadMode()) {
                activity.closeShortCut();
            }
        }
    }

    private void scanMoveToRemoteFile(VFile file){
        MoveToDialogFragment moveToFragment = (MoveToDialogFragment)getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
        if (moveToFragment != null) {
            if(file instanceof RemoteVFile && ((RemoteVFile)file).getStorageType()==StorageType.TYPE_HOME_CLOUD){
                moveToFragment.setListShown(false);
                moveToFragment.SetScanHostIndicatorPath(((RemoteVFile)file).getStorageName());
                RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(((RemoteVFile)file).getStorageName(), null, null, ((RemoteVFile)file).getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                return;
            }
//          mFolderTreeAdapter.notifyDataSetChanged();

//            SambaFileUtility.updateHostIp = false;
//            RemoteFileUtility.isShowDevicesList = false;

            moveToFragment.startScanFile(file, ScanType.SCAN_CHILD , false);
        }
    }

    final private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_UI:
                AccountInfo info = (AccountInfo)msg.obj;
                //addCloudStorageAccount(info.getAccountName(), info.getAddress(), info.getStorageType(), info.getState());
                //addConnectedCloudStorageAccount(info.getAccountName(), info.getAddress(), info.getStorageType(), info.getState());
                if(getActivity() != null){
                    ((FileManagerActivity)getActivity()).updateCloudStorageAccountList(info);
                }
                break;
            case MSG_TOKEN_SCAN_FILE:
                Log.i(TAG,"mHandler:MSG_TOKEN_SCAN_FILE");
                if (currentTokenFile!= null) {
                    FileListFragment fragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                    if(((FileManagerActivity)getActivity()).isMoveToDialogShowing()){
                        MoveToDialogFragment moveToFragment = (MoveToDialogFragment)getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
                        moveToFragment.switchFragmentView();
                        moveToFragment.setmIndicatorFile(currentTokenFile);
                        scanMoveToRemoteFile(currentTokenFile);
                    }else if(fragment != null){
                        // Dont't erase the last path (homepage)
                        // fragment.setmIndicatorFile(currentTokenFile);
                        scanRemoteFile(currentTokenFile);
                    }

                    Log.i(TAG,"mHandler:MSG_TOKEN_SCAN_FILE scanRemoteFile(currentTokenFile)");
                }
                break;
            case MSG_REMOVE_UI:
                AccountInfo removedInfo = (AccountInfo)msg.obj;
                if(getActivity() != null){
                    ((FileManagerActivity)getActivity()).removeCloudStorageAccountList(removedInfo);
                }
                break;

            default:
                break;
            }
        };
    };
   public Handler getShortCutHandler(){
       if (mHandler!=null) {
        return mHandler;
       }else {
        return null;
    }
   }
    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    public static ArrayList<VFile> mStorageFile = new ArrayList<VFile>();
    public String mUnknownStorageTitle = "";
    private ArrayList<Object> mStorageVolume;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "ShortCutFragment onAttach");

        Intent intent = activity.getIntent();
        if (intent.getAction() != null && intent.getAction().equals(FileManagerActivity.ACTION_MULTIPLE_SELECTION)) {
            if (intent.getBooleanExtra(EXTRA_HIDE_REMOVABLE_STORAGE, false)) {
                mIsHideRemovableStorage = true;
            }
        }

        if (!mIsHideRemovableStorage) {
            IntentFilter localeChangeFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
            getActivity().registerReceiver(mReviverLocalChange, localeChangeFilter);

            ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.addObserver(this);
        }

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ShortCutFragment onCreate");
        setRetainInstance(true);
        updateStorageVolume();


    }

    private void updateStorageVolume() {
        // +++ Willie, sets the StorageType based on storage list of each device
        mStorageVolume = ((FileManagerApplication)this.getActivity().getApplication()).getStorageVolume();
        VFile[] tempVFile = ((FileManagerApplication)this.getActivity().getApplication()).getStorageFile();

        // initizalize storage array
        for (int i=0 ; i<tempVFile.length ; i++) {
            mStorageFile.add(tempVFile[i]);
        }

        for (int i = 0; i < mStorageVolume.size(); i ++) {
            String path = reflectionApis.volume_getPath(mStorageVolume.get(i));
            if (Environment.getExternalStorageDirectory().getAbsolutePath().equals(path)) {
                StorageType.TYPE_INTERNAL_STORAGE = i;
            } else if (WrapEnvironment.MOUNT_POINT_MICROSD.equals(path)) {
                StorageType.TYPE_MICROSD_STORAGE = i;
            } else if (WrapEnvironment.MOUNT_POINT_USBDISK1.equals(path)) {
                StorageType.TYPE_USBDISK1_STORAGE = i;
            } else if (WrapEnvironment.MOUNT_POINT_USBDISK2.equals(path)) {
                StorageType.TYPE_USBDISK2_STORAGE = i;
            } else if (WrapEnvironment.MOUNT_POINT_SDREADER.equals(path)) {
                StorageType.TYPE_SDREADER_STORAGE = i;
            } else {
                StorageType.TYPE_UNKNOWN_STORAGE = i;
                mUnknownStorageTitle = reflectionApis.volume_getMountPointTitle(mStorageVolume.get(i));
            }
        }
        // ---
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.shortcut_fragment, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateStorageVolume();
        updateStorageGroup(true);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "ShortCutFragment onDetach");
        try{
        if (!mIsHideRemovableStorage)
            ((FileManagerApplication)getActivity().getApplication()).mVolumeStateObserver.deleteObserver(this);
            getActivity().unregisterReceiver(mReviverLocalChange);
        }
        catch(IllegalArgumentException e){
            Log.e(TAG,"Receiver not registered");
        }
    }

    // Copy from ListFragment
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mListShown = false;
        mEmptyView = mProgressContainer = mListContainer = null;
        mStandardEmptyView = null;
        super.onDestroyView();
    }

    // Expandable ---

    public void updateStorageGroup(boolean forceUpdate) {

        if (!((FileManagerActivity) getActivity()).isPadMode()) {
            //((FileManagerActivity) getActivity()).UpdateInternalStorageSpinner(storageElementList);
            ((FileManagerActivity) getActivity()).updateLocalStorageList(mStorageVolume);

        }
    }


    public void sendRemoteStorage(VFile file, int msg) {
        RemoteFileUtility.getInstance(getActivity()).sendRemoteMessage(file, msg);
    }

    public void sendCloudStorage(VFile file, int msg) {
        String account = ((RemoteVFile)file).getStorageName();
        int type = ((RemoteVFile)file).getMsgObjType();
        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(account, file, null, type, msg);
    }

    @Override
    public void update(Observable observable, Object data) {
        if(getActivity() != null){
            updateStorageVolume();
            updateStorageGroup(false);
        }
    }
}
