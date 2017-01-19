package com.asus.filemanager.filesystem;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filetransfer.filesystem.FileCompressor;
import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.filesystem.IOutputFile;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.utility.HttpFileServerAnalyzer;
import com.asus.filetransfer.utility.HttpServerEvents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Yenju_Lai on 2015/9/2.
 */

public class FileManager {
    private static final String TAG = "FileManager";
    private Context context = null;
    private FileManagerApplication application = null;
    private List<IInputFile> storageList = null;
    private SafOperationUtility safOperationUtility = null;

    private String mockStorageLocation = null;

    protected FileManager(Context context) {
        this.context = context;
        storageList = new ArrayList<>();
        if (context != null)
            safOperationUtility = SafOperationUtility.getInstance(context);
    }

    protected void setMockStorageLocation(String mockStorageLocation) {
        this.mockStorageLocation = mockStorageLocation;
    }

    public FileManager(Activity activity) {
        this((Context)activity);
        this.application = (FileManagerApplication)activity.getApplication();
    }
    public FileManager(Service service) {
        this((Context)service);
        this.application = (FileManagerApplication)service.getApplication();
    }

    private String convertStorageToActualPath(String path) {
        if (storageList.size() == 0)
            getStorageList();
        synchronized (storageList) {
            for (IInputFile storage : storageList) {
                if (path.startsWith(File.separator + storage.getName())) {
                    return path.replaceFirst(File.separator + storage.getName(), storage.getPath());
                }
                }
            return path;
            }
        }

    public IInputFile getInputFile(String path) throws FileNotFoundException {
        if (path.compareTo(File.separator) == 0)
            return new StorageRoot(getStorageList());
        IInputFile file = new LocalFile(convertStorageToActualPath(path), this);
        if (file.exists())
            return file;
        throw new FileNotFoundException("File not found: " + path);
    }

    protected IInputFile.Writable canWrite(String path) {
        if (!new File(path).isDirectory())
            return IInputFile.Writable.No;
        else if (safOperationUtility.isNeedToWriteSdBySaf(path))
            return safOperationUtility.isNeedToShowSafDialog(path)?IInputFile.Writable.SAF :  IInputFile.Writable.Yes;
        else
            try {
                new OutputByFile(path + File.separator + System.currentTimeMillis());
                return IInputFile.Writable.Yes;
            } catch (IOException e) {
            }
        return IInputFile.Writable.No;
    }

    public List<IInputFile> getStorageList() {
        if (context == null || application == null)
            return storageList;
        final StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        ArrayList<Object> storageVolume = application.getStorageVolume();
        VFile[] tempVFile = application.getStorageFile();

        synchronized (storageList) {
            storageList.clear();
            for (int i = 0; i < storageVolume.size(); i++) {
                if (FolderElement.StorageType.TYPE_INTERNAL_STORAGE != i) {
                    if (storageManager != null && reflectionApis.getVolumeState(storageManager, reflectionApis.volume_getPath(storageVolume.get(i))).equals(Environment.MEDIA_MOUNTED)) {
                        storageList.add(
                                new LocalStorage(tempVFile[i].getPath()
                                        , reflectionApis.volume_getMountPointTitle(storageVolume.get(i)), this, false));
                    }
                } else {
                    storageList.add(
                            new LocalStorage(tempVFile[i].getPath()
                                    , application.getResources().getString(R.string.internal_storage_title), this, true));
                }
            }
            return storageList;
        }
    }

    public boolean needSAFPermission() {
        for (IInputFile storage : getStorageList())
            if (canWrite(storage.getPath()) == IInputFile.Writable.SAF)
                return true;
        return false;
    }

    public IInputFile getAssetFile(String path) throws FileNotFoundException {
        if (path.compareTo("/") == 0) {
            path = "/index.html";
            HttpFileServerAnalyzer.commandExecuted(new HttpServerEvents(HttpServerEvents.Action.Browse));
        }
        return new AssetsInputFile(context, path.substring("/".length()));
    }

    public IOutputFile getOutputFile(String path) throws IOException {
        String actualPath = convertStorageToActualPath(path);
        if (safOperationUtility.isNeedToWriteSdBySaf(actualPath)) {
            if (safOperationUtility.isNeedToShowSafDialog(actualPath))
                throw new IOException("No saf permission");
            Log.d(TAG, "isNeedToWriteSdBySaf");
            return new OutputBySAF(context, actualPath);
        }
        Log.d(TAG, "can write by File");
        return new OutputByFile(actualPath);
    }

    public FileCompressor createFileCompressor(String path) throws IOException {
        String compressedFileDir = context.getCacheDir() + HttpFileServer.PATH_TEMP_FOLDER_FOR_COMPRESSION;
        new File(compressedFileDir).mkdirs();
        return new FileCompressor(
                new File(findAvailableFilePath(compressedFileDir + path + HttpFileServer.PATH_COMPRESSION_SUFFIX, false)));
            }

    private String findAvailableFilePath(String expectedPath, boolean isDirectory) {
        int dotIndex = expectedPath.lastIndexOf(".");
        String pathPrefix = dotIndex == -1? expectedPath : expectedPath.substring(0, dotIndex);
        String pathSuffix = dotIndex == -1? "" : expectedPath.substring(dotIndex);
        int index = 1;
        File expectedFile = new File(expectedPath);
        while (expectedFile.exists()) {
            if (isDirectory && expectedFile.isDirectory())
                break;
            expectedFile = new File(String.format("%s (%d)%s", pathPrefix, index, pathSuffix));
            index++;
        }
        return expectedFile.getPath();
    }

    public boolean delete(String path) {
        return delete(new File(convertStorageToActualPath(path)), true);
    }

    private boolean delete(File deleteFile, boolean isRoot) {
        if (deleteFile.isDirectory())
            return deleteDirectory(deleteFile, isRoot);
        else
            return deleteFile(deleteFile, isRoot);
    }

    private boolean deleteDirectory(File deleteFile, boolean isRoot) {
        for (File file : deleteFile.listFiles()) {
            if (!delete(file, false))
                return false;
            }
        return deleteFile(deleteFile, isRoot);
        }

    private boolean deleteFile(File deleteFile, boolean isRoot) {
        try {
            if (!getOutputFile(deleteFile.getPath()).delete())
            return false;
        if (isRoot)
            scanFile(deleteFile);
        return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getStorageLocation() {
        if (mockStorageLocation != null)
            return mockStorageLocation;
        SharedPreferences mSharePrefence = context.getSharedPreferences("HttpServerPrefs", 0);
        String storageLocation = mSharePrefence.getString("STORAGE_LOCATION", null);
        if (storageLocation == null)
            return Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DOWNLOADS;
        else if (new File(storageLocation).isDirectory())
            return storageLocation;
        else {
            return new File(storageLocation).mkdirs()? storageLocation :
                    Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DOWNLOADS;
        }

    }

    public void triggerMediaScanner(String newFilePath) {
        if (context == null) return;
        File newFile = new File(newFilePath);
        if (newFile.isDirectory())
            scanDirectory(newFile);
        else {
            scanFile(newFile);
        }
    }

    private void scanFile(File newFile) {
        try {
            MediaScannerConnection.scanFile(context, new String[]{FileUtility.getCanonicalPath(newFile)}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scanDirectory(File newFile) {
        try {
            final String tempFilePath = getOutputFile(
                    findAvailableFilePath(newFile.getPath() + File.separator + "temp", false)).createNewFile();
            if (tempFilePath == null)
                return;
            MediaScannerConnection.scanFile(
                    context, new String[]{FileUtility.getCanonicalPath(new File(tempFilePath))}, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(final String path, Uri uri) {
                    new Thread(new Runnable() {
                @Override
                        public void run() {
                    delete(path);
                }
                    }).start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearCache() {
        if (context == null)
            return;
        Log.d(TAG, "clear compression file cache: " +
                delete(new File(context.getCacheDir() + HttpFileServer.PATH_TEMP_FOLDER_FOR_COMPRESSION), false));
    }

    public String renameFile(String srcPath, String dstPath) throws IOException {
        if (srcPath.compareTo(dstPath) == 0)
            return dstPath;
        IOutputFile srcFile = getOutputFile(srcPath);
        if (!srcFile.renameTo(findAvailableFilePath(dstPath, srcFile.isDirectory())))
            throw new IOException("Rename failed");
        return srcFile.getPath();
    }
}
