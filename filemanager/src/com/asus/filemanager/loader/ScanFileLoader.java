
package com.asus.filemanager.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Config;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.provider.ProviderUtility;
import com.asus.filemanager.functionaldirectory.recyclebin.RecycleBinUtility;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.SortUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class ScanFileLoader extends AsyncTaskLoader<VFile[]> {

    private static final String TAG = "ScanFileLoader";
    private static final boolean DEBUG = Config.DEBUG;

    public static class ScanType {
        public static final int SCAN_NONE = 0;
        public static final int SCAN_CHILD = 1;
        public static final int SCAN_SIBLING = 2;
        public static final int SCAN_FOLDER_ONLY = 3;
    }

    private String[] visiableFile;
    /*= {
            WrapEnvironment.getEpadExternalStorageDirectory().getName(),
            WrapEnvironment.getEpadInternalStorageDirectory().getName()
    };*/

    private String mScanPath;
    private Context mContext;
    private VFile[] mFiles;
    private boolean mIsShowHidden;
    private int mScanType;
    private int mSortType;
    private int mVFileType;
    private EditPool mCheckPool;
    private String[] mFileFilter;
    private String ErrorMsg = null;

    public ScanFileLoader(Context context, String scanPath, int scanType, int sortType, int vfileType, boolean isShowHidden, EditPool checkPool, String[] filter) {
        super(context);
        if (DEBUG) {
            Log.d(TAG, "ScanFileLoader init");
        }
        mContext = context;
        mScanPath = scanPath;
        mScanType = scanType;
        mVFileType = vfileType;
        mIsShowHidden = isShowHidden;
        mSortType = sortType;
        mCheckPool = checkPool;
        mFileFilter = filter;

        ErrorMsg = null;
    }

    @Override
    public VFile[] loadInBackground() {
        VFile scanFile = new VFile(mScanPath, mVFileType);
        VFile[] files = null;

        String[] favoriteFilePaths = null;

        if (mVFileType == VFileType.TYPE_REMOTE_STORAGE || mVFileType == VFileType.TYPE_CLOUD_STORAGE) {
           // files = RemoteFileUtility.getRemoteFileList();
            //RemoteFileUtility.clearRemoteFileList();
            files = RemoteFileUtility.getInstance(null).mRemoteFileListMap.get(scanFile.getAbsolutePath());
            if (files == null) {
                files = new RemoteVFile[0];
            }
            RemoteFileUtility.getInstance(null).mRemoteFileListMap.remove(scanFile.getAbsolutePath());
        } else if (mVFileType == VFileType.TYPE_SAMBA_STORAGE){
            try {
                SmbFile scanSmbFile = new SmbFile(mScanPath);
                if (scanSmbFile != null){
                    SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
                    sambaFileUtility.setRoot(scanSmbFile);
                    SmbFile[] subFiles = scanSmbFile.listFiles();
                    if(mScanPath.equals(sambaFileUtility.getRootScanPath())){
                        subFiles = sambaFileUtility.fileterSystemFileForSamba(subFiles);
                    }
                    if (subFiles == null){
                        if (DEBUG) {
                            Log.d(TAG, "SAMBA init fail");
                        }
                    }else{
                        files = new VFile[subFiles.length];
                        for(int i = 0;i < subFiles.length;i++){
                            SmbFile tmp = subFiles[i];
                            if (tmp != null)
                                files[i] = new SambaVFile(tmp.getPath(), tmp.isDirectory(), tmp.length(),
                                        tmp.getParent(), tmp.getName(), tmp.getLastModified());
                        }
                    }
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                ErrorMsg  = e.getMessage();
                e.printStackTrace();
            } catch (SmbException e) {
                // TODO Auto-generated catch block
                ErrorMsg = e.getMessage();
                e.printStackTrace();
            }
        }
        else {
            if(isExternalStorageMounted()){
                visiableFile = new String[]{
                        WrapEnvironment.getEpadExternalStorageDirectory().getName(),
                        WrapEnvironment.getEpadInternalStorageDirectory().getName()
                };
            }else{
                visiableFile = new String[]{
                        WrapEnvironment.getEpadInternalStorageDirectory().getName()
                };
            }
            files = scanFile.listVFiles();

            favoriteFilePaths = ProviderUtility.FavoriteFiles.getPaths(mContext.getContentResolver());
        }

        // +++ Johnson
        if (mScanType == ScanType.SCAN_FOLDER_ONLY) {
            ArrayList<VFile> temp = new ArrayList<VFile>();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    temp.add(files[i]);
                }
            }
            if (temp.size() > 0) {
                files = null;
                files = new VFile[temp.size()];
                for (int i = 0; i < files.length; i++) {
                    files[i] = temp.get(i);
                }
            } else {
                return null;
            }
        }
        // ---

        if (mVFileType==VFileType.TYPE_LOCAL_STORAGE && visiableFile != null) {
            Arrays.sort(visiableFile);
            files = filterHiddenFile(files);
        }

        if (mFileFilter != null) {
            Arrays.sort(mFileFilter);
            files = fileFilter(files);
        }

        if(favoriteFilePaths != null){
            Arrays.sort(favoriteFilePaths);
            files = setFileInfo(files, favoriteFilePaths);
        }

        if (files != null)
            Arrays.sort(files, SortUtility.getComparator(mSortType));

        if (mCheckPool != null && files != null) {
            VFile[] checkFiles = mCheckPool.getFiles();
            if (checkFiles != null && checkFiles.length > 0) {
                Arrays.sort(checkFiles, SortUtility.getComparator(mSortType));
                udateCheck(files, checkFiles, mSortType);
            }
        }

        /*
         * for test loading... try { Thread.sleep(5000); } catch
         * (InterruptedException e) { e.printStackTrace(); }
         */

        return files;
    }

    private void udateCheck(VFile[] src, VFile[] files, int sortType) {

        for (int i = 0; i < files.length; i++) {
            int find = -1;

            find = Arrays.binarySearch(src, files[i], SortUtility.getComparator(sortType));
            if (find > -1) {
                src[find].setChecked(true);
            }

        }
    }

    @Override
    public void deliverResult(VFile[] data) {
        if (DEBUG) {
            Log.d(TAG, "deliverResult");
        }
        if (isReset()) {
            if (data != null) {
                onReleaseResources(data);
            }
        }
        VFile[] oldFiles = mFiles;
        mFiles = data;

        if (isStarted()) {
            super.deliverResult(data);
        }

        if (oldFiles != null) {
            onReleaseResources(oldFiles);
        }
        if(ErrorMsg != null){
//          if(ErrorMsg.contains("0x")){

            // Not all condition need to show login dialog.
            /*
            if(SambaFileUtility.getInstance(null).isLoginAnonymous()) {
                SambaFileUtility.getInstance(null).showLoginDialog();
            }
            */

            if(ErrorMsg.equalsIgnoreCase("Access is denied.") || ErrorMsg.contains("Logon failure")) {

                ToastUtility.show(mContext, R.string.permission_deny, Toast.LENGTH_LONG);
                if(SambaFileUtility.getInstance(null).isLoginAnonymous()) {
                    SambaFileUtility.getInstance(null).showLoginDialog();
                }
            } else {
                ToastUtility.show(mContext, ErrorMsg);
            }
            Log.d(TAG,"===scanfile error ==" + ErrorMsg);
//          }
//          else{
//              ToastUtility.show(mContext, ErrorMsg);
//          }
        }
    }

    @Override
    protected void onStartLoading() {
        // super.onStartLoading();
        if (DEBUG) {
            Log.d(TAG, "onStartLoading");
        }
        if (mFiles != null) {
            deliverResult(mFiles);
        }

        if (takeContentChanged() || mFiles == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            ErrorMsg = null;
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // super.onStopLoading();
        if (DEBUG) {
            Log.d(TAG, "onStopLoading");
        }
        cancelLoad();
    }

    @Override
    public void onCanceled(VFile[] data) {
        super.onCanceled(data);
        if (DEBUG) {
            Log.d(TAG, "onStopLoading");
        }
        onReleaseResources(mFiles);
    }

    @Override
    public void onReset() {
        super.onReset();
        if (DEBUG) {
            Log.d(TAG, "onReset");
        }
        onStopLoading();

        if (mFiles != null) {
            onReleaseResources(mFiles);
            mFiles = null;
        }
    }

    protected void onReleaseResources(VFile[] files) {

    }

    private VFile[] filterRecycleBin(VFile[] files) {
        if (files == null || files.length == 0)
            return null;
        String parentPath = null;
        try {
            parentPath = FileUtility.getCanonicalPath(files[0].getParentFile());
        } catch (IOException e) {
            parentPath = files[0].getParent();
        }
        if (SafOperationUtility.getInstance().getRootPathFromFullPath(parentPath).compareTo(parentPath) != 0)
            return files;
        else {
            ArrayList<VFile> pool = new ArrayList<VFile>();
            for (int i = 0; i < files.length && files[i] != null; i++)
            {
                if (files[i].getName().compareTo(RecycleBinUtility.RECYCLE_BIN_NAME)!=0)
                    pool.add(files[i]);
            }
            VFile[] r = new VFile[pool.size()];
            for (int i = 0; i < r.length; i++)
            {
                r[i] = pool.get(i);
            }
            return r;
        }
    }

    private VFile[] filterHiddenFile(VFile[] fa)
    {
        if (fa == null || fa.length == 0)
            return null;
        ArrayList<VFile> pool = new ArrayList<VFile>();
        for (int i = 0; i < fa.length && fa[i] != null; i++)
        {
            if (!isHidden(fa[i]))
                pool.add(fa[i]);
        }
        VFile[] r = new VFile[pool.size()];
        for (int i = 0; i < r.length; i++)
        {
            r[i] = pool.get(i);
        }
        return r;
    }

    private boolean isHidden(VFile vFile) {
        if (mIsShowHidden)
            return FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(vFile);
        else {
        if (vFile.isHidden()) {
            return true;
            } else if (vFile.getParentFile().getPath().equals("/")
                && !(Arrays.binarySearch(visiableFile, vFile.getName()) > -1)) {
            return true;
            } else {
                return false;
            }
        }
    }

    private VFile[] fileFilter(VFile[] fa) {
        if (fa == null || fa.length == 0) {
            return null;
        }
        ArrayList<VFile> pool = new ArrayList<VFile>();
        for (int i = 0; i < fa.length && fa[i] != null; i++) {
            if (FileListFragment.sIsMIMEFilter) {
                if (isMimePass(fa[i])) {
                    pool.add(fa[i]);
                }
            } else {
                if (isExtPass(fa[i])) {
                    pool.add(fa[i]);
                }
            }
        }
        VFile[] r = new VFile[pool.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = pool.get(i);
        }
        return r;
    }

    private boolean isMimePass(VFile vFile) {
        if (vFile.isDirectory()) {
            return true;
        }
        String s = MimeTypeMap.getSingleton().getMimeTypeFromExtension(vFile.getExtensiontName().toLowerCase());
        if (s == null) {
            return false;
        }
        if (Arrays.binarySearch(mFileFilter, s) > -1) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isExtPass(VFile vFile) {
        if (vFile.isDirectory()) {
            return true;
        }
        String s = vFile.getExtensiontName().toLowerCase();
        if (s == null) {
            return false;
        }
        if (Arrays.binarySearch(mFileFilter, s) > -1) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isExternalStorageMounted(){
        boolean mounted = false;
        final StorageManager mStorageManager = (StorageManager) mContext.getSystemService(
                Context.STORAGE_SERVICE);
        FileManagerApplication application = (FileManagerApplication) FileManagerApplication.getAppContext();
        ArrayList<Object> mVolumeList = application.getStorageVolume();
        for(Object volume : mVolumeList){
            String folderPath = reflectionApis.volume_getPath(volume);
            if(!folderPath.equals(Environment.getExternalStorageDirectory().getAbsolutePath()) && reflectionApis.getVolumeState(mStorageManager,volume).equals(Environment.MEDIA_MOUNTED)){
                mounted = true;
            }
        }

        return mounted;
    }

    private VFile[] setFileInfo(VFile[] fa, String[] favoriteFilePaths) {
        if (fa == null || fa.length == 0 || favoriteFilePaths == null || favoriteFilePaths.length == 0) {
            return fa;
        }

        ArrayList<VFile> pool = new ArrayList<VFile>();
        for (int i = 0; i < fa.length && fa[i] != null; i++) {
            String path = null;
            try {
                path = FileUtility.getCanonicalPathForUser(fa[i].getCanonicalPath());
            } catch (IOException e2) {
                path = fa[i].getAbsolutePath();
                e2.printStackTrace();
            }
            int index = Arrays.binarySearch(favoriteFilePaths, path);

            if(fa[i].isDirectory() && index > -1){
                fa[i].setFavoriteName(fa[i].getName());
            }
            pool.add(fa[i]);
        }

        VFile[] r = new VFile[pool.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = pool.get(i);
        }
        return r;
    }
}
