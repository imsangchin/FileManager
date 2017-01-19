
package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.SearchResultFragment;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.PathIndicator;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Stack;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class SearchDialogFragment extends DialogFragment implements OnClickListener {

    private static final boolean DEBUG = ConstantsUtil.DEBUG;
    private static final String TAG = "SearchDialogFragment";

    private Activity mActivity;
    private static ArrayList<VFile> result = new ArrayList<VFile>();
    private static boolean isShowHide = false;
    private long mStartTime = 0;

    private class SearchRunnable implements Runnable {

        private boolean isStop = false;
        private String mSearch;

        public SearchRunnable(String searchKey) {
        if (searchKey != null)
            mSearch = searchKey.toLowerCase();
        }

        public void terminate() {
            isStop = true;
        }

        @Override
        public void run() {

            FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
            VFile searchFolder = null;

            if (fileListFragment != null) {
                searchFolder = fileListFragment.getIndicatorFile();
            }

            if (searchFolder != null) {

                Log.d(TAG, "search folder : " + searchFolder.getAbsolutePath());
                if (result.size() != 0) {
                    result.clear();
                }

                Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS);
                msg.obj = null;
                mHandler.removeMessages(MSG_UPDATE_PROGRESS);
                mHandler.sendMessage(msg);

                SharedPreferences mSharePrefence = getActivity().getSharedPreferences("MyPrefsFile", 0);
                isShowHide = mSharePrefence.getBoolean("mShowHidden", false);
                if (searchFolder.getAbsolutePath().equals("/")) {
                    if (mActivity != null) {

                        FileManagerApplication application
                                = (FileManagerApplication) mActivity.getApplication();

                        StorageManager storageManager
                                = (StorageManager) mActivity.getSystemService(Context.STORAGE_SERVICE);

                        //String[] storageVolumePaths = application.getStorageVolumePaths();

                        ArrayList<Object> storageVolume = application.getStorageVolume();

                        for(int i=0;i<storageVolume.size();i++) {
                            Object aVolume =  storageVolume.get(i);
                            String path = reflectionApis.volume_getPath(aVolume);
                            Log.w(TAG, "search in " + path);
                            String state = reflectionApis.getVolumeState(storageManager, aVolume);
                            Log.w(TAG, "getvolume state, path = " + path + ", state = " + state);

                            if (Environment.MEDIA_MOUNTED.equals(state)) {
                                Log.w(TAG, "search mounted path: " + path);
                                searchFolder = new VFile(path);
                                searchFile(searchFolder);
                            }
                        }
                    } else {
                        Log.w(TAG, "skip to search file, activity == null");
                    }
                } else {
                    searchFile(searchFolder);
                }

            }

            Message msg = mHandler.obtainMessage(MSG_COMPLETE);
//            msg.obj = result;
            msg.obj = null;
            mHandler.sendMessage(msg);
            isFinish = true;
        }

        private void searchFile(VFile searchFolder) {
            VFile[] files = null;
            Activity attachedActivity = getActivity();
            if (searchFolder != null && !isStop && null != attachedActivity) {
                if (searchFolder.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                    String path = ((SambaVFile)searchFolder).getAbsolutePath();
                    try {
                        SmbFile searchRoot = new SmbFile(path);
                        SmbFile[] subFiles = searchRoot.listFiles();
                        if (subFiles != null) {
                            files = new VFile[subFiles.length];
                            for(int i = 0;i < subFiles.length;i++) {
                                SmbFile tmp = subFiles[i];
                                if (tmp != null)
                                    files[i] = new SambaVFile(tmp.getPath(), tmp.isDirectory(), tmp.getContentLength(),
                                            tmp.getParent(), tmp.getName(), tmp.getLastModified());
                            }
                        }
                    } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (SmbException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (searchFolder.getVFieType() == VFileType.TYPE_CATEGORY_STORAGE) {
                        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                        files = fileListFragment.getFileList();
                } else {
                        files = searchFolder.listVFiles();
                }

                if (files == null) {
                    return;
                }

                for (int i = 0; i < files.length; i++) {
                    if (shouldSkipSearch(files[i]))
                        continue;
                    if (!files[i].isSearchable()) {
                        continue;
                    }

                    if (files[i].isDirectory()) {
                        //searchFile(files[i]);

                        //default stack limit size is 2m
                        Stack<VFile> mFolderStack = new Stack<VFile>();
                        mFolderStack.push(files[i]);
                        while(!mFolderStack.isEmpty()) {
                            VFile subFolder = mFolderStack.pop();
                            VFile[] mfiles = null;
                            if (searchFolder.getVFieType() == VFileType.TYPE_CATEGORY_STORAGE) {
                                ArrayList<LocalVFile> mediaFiles = null;
                                VFile parentOfSubFolder = searchFolder;
                                String path = searchFolder.getAbsolutePath();
                                String bucket_id = String.valueOf(subFolder.getBucketId());
                                if (((FileManagerActivity)attachedActivity).CATEGORY_IMAGE_FILE.equals(parentOfSubFolder)) {
                                    mediaFiles = MediaProviderAsyncHelper.getImageFilesByBucketId(attachedActivity, bucket_id, 0);
                                } else if (((FileManagerActivity)attachedActivity).CATEGORY_MUSIC_FILE.equals(parentOfSubFolder)) {
                                    mediaFiles = MediaProviderAsyncHelper.getMusicFilesByBucketId(attachedActivity, bucket_id, 0, false);
                                } else if (((FileManagerActivity)attachedActivity).CATEGORY_VIDEO_FILE.equals(parentOfSubFolder)) {
                                    mediaFiles = MediaProviderAsyncHelper.getVideoFilesByBucketId(attachedActivity, bucket_id, 0);
                                }
                                if (mediaFiles != null) {
                                    mfiles = mediaFiles.toArray(new LocalVFile[mediaFiles.size()]);
                                }
                            } else {
                                mfiles = subFolder.listVFiles();
                            }

                            if (mfiles != null) {
                                for(VFile subVF : mfiles) {

                                    if (!isShowHide && subVF.isHidden()) {
                                        continue;
                                    }

                                    if (subVF.isDirectory()) {
                                        mFolderStack.push(subVF);
                                    }

                                    String name = subVF.getName().toLowerCase();
                                    if ((isShowHide || !subVF.isHidden()) && name.contains(mSearch)) {
                                        Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS);
                                        msg.obj = subVF;
                                        mHandler.sendMessage(msg);
                                    }
                                }
                            }

                        }

                         if (!mFolderStack.isEmpty()) {
                             mFolderStack.clear();
                         }
                    }

                    String name;

                    if (((FileManagerActivity)attachedActivity).CATEGORY_FAVORITE_FILE.equals(searchFolder)) {
                        name = files[i].getFavoriteName().toLowerCase();
                    } else {
                        name = files[i].getName().toLowerCase();
                    }

                    if ((isShowHide || !files[i].isHidden()) && name.contains(mSearch)) {
                        Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS);
                        msg.obj = files[i];
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }

        private boolean shouldSkipSearch(VFile file) {
            if (!isShowHide && file.isHidden())
                return true;
            else if (FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(file)) {
                return true;
                }
                    return false;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PROGRESS:
                    if (msg.obj != null) {
                        result.add((VFile) msg.obj);
                        long mCurTime = System.currentTimeMillis();
                        if (mCurTime - mStartTime >= 200) {
                            updateResult(result);
                            mStartTime = System.currentTimeMillis();
                        }
                    }
                    break;
                case MSG_COMPLETE:
                    if (msg.obj != null) {
                        updateResult((ArrayList<VFile>) msg.obj);
                    } else {
                        updateResult(result);
                    }

                    updateSearchView();
                    onCancel(getDialog());
                    dismissAllowingStateLoss();
                    break;
                default:
                    break;
            }
        }
    };

    private SearchRunnable mRunnable;
    private Thread mThread;

    private boolean cancel = false;
    private boolean isFirst = true;
    private boolean isFinish = false;

    private String mSearchKey;
    private VFile mSearchFolder;

    private static final int MSG_UPDATE_PROGRESS = 0;
    private static final int MSG_COMPLETE = 1;

    public static SearchDialogFragment newInstance(String arg) {
        SearchDialogFragment fragment = new SearchDialogFragment();
        Bundle args = new Bundle();
        args.putString("search_key", arg);
        fragment.setArguments(args);
        return fragment;
    }

    protected void updateResult(ArrayList<VFile> result) {
        if (getFragmentManager() != null) {
            SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.searchlist);
            searchResultFragment.updateResult((VFile[]) result.toArray(new VFile[result.size()]), mSearchKey, mSearchFolder.getAbsolutePath());
        }
    }

    protected void updateSearchView() {
        if (getFragmentManager() != null) {
            SearchResultFragment searchResultFragment = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.searchlist);
            searchResultFragment.updateSearchView(mSearchKey);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onCreate");
        setRetainInstance(true);

    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            Log.d(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume");
        if (isFinish) {
            onCancel(getDialog());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            Log.d(TAG, "onPause");
        FileManagerActivity.isSearchIng = false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (DEBUG)
            Log.d(TAG, "onDismiss");
        if (getDialog() != null && getRetainInstance() && !cancel)
            return;
        FileManagerActivity.isSearchIng = false;
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (DEBUG)
            Log.d(TAG, "onCancel");

        cancel = true;
        if (mRunnable != null)
            mRunnable.terminate();
        FileManagerActivity.isSearchIng = false;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) {
            Log.d(TAG, "onActivityCreate");
        }
       ((FileManagerActivity)getActivity()).setReSearchQueryKey(mSearchKey);
        if (PathIndicator.getIndicatorVFileType() == VFileType.TYPE_CLOUD_STORAGE) {
            RemoteVFile indicatorVFile = RemoteFileUtility.getInstance(getActivity()).getPathIndicatorVFile();
            Log.v("Johnson", "indicatorVFile getParent: " + indicatorVFile.getParent());
            Log.v("Johnson", "indicatorVFile getAbsolutePath: " + indicatorVFile.getAbsolutePath());
            Log.v("Johnson", "indicatorVFile getStorageName: " + indicatorVFile.getStorageName());
            Log.v("Johnson", "indicatorVFile getFileID: " + indicatorVFile.getFileID());
            Log.v("Johnson", "indicatorVFile getVFieType: " + indicatorVFile.getVFieType());
            Log.v("Johnson", "indicatorVFile getMsgObjType: " + indicatorVFile.getMsgObjType());
            Log.v("Johnson", "mSearchKey: " + mSearchKey);

            RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(indicatorVFile.getStorageName(),
                    indicatorVFile, null, indicatorVFile.getMsgObjType(), CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_SEARCH_FILES, mSearchKey);
        } else {
            if (isFirst) {
                  if (mSearchKey==null)
                        return;
                mRunnable = new SearchRunnable(mSearchKey);
                mThread = new Thread(mRunnable);
                mThread.start();
                mStartTime = System.currentTimeMillis();
                isFirst = false;
            }
          }
        }

    public void reSearch() {
    if (mSearchKey == null)
        return;
    mRunnable = new SearchRunnable(mSearchKey);
        mThread = new Thread(mRunnable);
        mThread.start();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mSearchKey = getArguments().getString("search_key");
        if (DEBUG)
            Log.d(TAG, "search key = " + mSearchKey);

        FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        mSearchFolder = null;

        if (fileListFragment != null) {
            mSearchFolder = fileListFragment.getIndicatorFile();
        }

        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        ProgressDialog dialog = new ProgressDialog(getActivity(),
                themeAsusLightDialogAlertId == 0 ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId);
        dialog.setMessage(getResources().getString(R.string.search_progress));
        dialog.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.cancel), this);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        onCancel(dialog);
    }

    public void sendSearchResult(ArrayList<RemoteVFile> files) {
        Message msg = mHandler.obtainMessage(MSG_COMPLETE);
        msg.obj = files;
        mHandler.sendMessage(msg);
    }
}
