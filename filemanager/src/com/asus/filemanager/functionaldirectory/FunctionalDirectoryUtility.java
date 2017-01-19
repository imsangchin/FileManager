package com.asus.filemanager.functionaldirectory;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.functionaldirectory.androidDirectory.AndroidDirectoryUtility;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneUtility;
import com.asus.filemanager.functionaldirectory.hiddenzone.HideFile;
import com.asus.filemanager.functionaldirectory.recyclebin.RecycleBinUtility;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.functionaldirectory.recyclebin.DeleteFile;
import com.asus.filemanager.functionaldirectory.recyclebin.RestoreFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.io.File;
import java.io.IOException;

/**
 * Created by Yenju_Lai on 2016/3/28.
 */
public class FunctionalDirectoryUtility {

    public enum DirectoryType {
        RecycleBin,
        HiddenZone,
        AndroidDirectory
    }

    protected FunctionalDirectoryUtility() {
    }

    private static FunctionalDirectoryUtility functionalDirectoryUtility;


    public static FunctionalDirectoryUtility getInstance() {
        if (functionalDirectoryUtility == null)
            functionalDirectoryUtility = new FunctionalDirectoryUtility();
        return functionalDirectoryUtility;
    }

    protected Movable createMovableFile(MoveFileTask.Destination destination, VFile file, Movable parentFile) {
        String canonicalPath = getCanonicalPath(file);
        VFile canonicalFile = new LocalVFile(canonicalPath);
        if (file.getHasRetrictFiles())
            canonicalFile.setRestrictFiles(file.getRestrictFiles());
        String storageRootPath = getRootPathFromFullPath(canonicalPath);
        Movable movableFile = null;
        switch (destination) {
            case RECYCLE_BIN:
                if  (parentFile != null) {
                    movableFile = new DeleteFile(canonicalFile, parentFile);
                }
                else {
                    String hiddenZoneFileName =
                            HiddenZoneUtility.getInstance().getHiddenZoneFileName(canonicalFile, storageRootPath);
                    movableFile = hiddenZoneFileName == null?
                            new DeleteFile(canonicalFile, storageRootPath)
                            : new DeleteFile(canonicalFile, storageRootPath, hiddenZoneFileName);
                }
                break;
            case ORIGINAL_PATH:
                movableFile = parentFile == null? new RestoreFile(canonicalFile, storageRootPath)
                        : new RestoreFile(canonicalFile, (RestoreFile)parentFile);
                break;
            case HIDDEN_ZONE:
                movableFile = parentFile == null? new HideFile(canonicalFile, storageRootPath)
                        : new HideFile(canonicalFile, (HideFile)parentFile);
                break;
        }
        return movableFile;
    }

    private String getRootPathFromFullPath(String fullPath) {
        return SafOperationUtility.getInstance().getRootPathFromFullPath(fullPath);
    }

    public String getCanonicalPath(File file) {
        try {
            return FileUtility.getCanonicalPath(file);
        } catch (IOException e) {
            return file.getPath();
        }
    }

    protected boolean permissionGranted(VFile vFile) {
        Log.d("test", "check permission");
        FileUtility.FileInfo info = FileUtility.getInfo(vFile);
        return info.PermissionWrite;
    }

    public boolean isInExternalStorage(String filePath) {
        return getRootPathFromFullPath(filePath).compareTo(Environment.getExternalStorageDirectory().getPath()) == 0;
    }

    public void scanFile(VFile target, boolean isNeedToWaitMediaScanner) {
        if (!target.exists())
            MediaProviderAsyncHelper.deleteFile(target, isNeedToWaitMediaScanner);
        else {
            if (target.isDirectory())
                MediaProviderAsyncHelper.addFolder(target, isNeedToWaitMediaScanner);
            else
                MediaProviderAsyncHelper.addFile(target, isNeedToWaitMediaScanner);
        }
    }

    public void scanFile(Movable target, boolean isNeedToWaitMediaScanner) {
        scanFile(target.getSourceFile(), isNeedToWaitMediaScanner);
        scanFile(new LocalVFile(target.getDestination()), isNeedToWaitMediaScanner);
    }

    public String getHiddenDirectorySelection(String pathColumnName) {
        return RecycleBinUtility.getInstance().getExcludeDirectorySelection(pathColumnName);
    }

    public boolean inFunctionalDirectory(VFile file) {
        return RecycleBinUtility.getInstance().inFunctionalDirectory(file)
                || HiddenZoneUtility.getInstance().inFunctionalDirectory(file);
    }

    public boolean inFunctionalDirectory(DirectoryType directoryType, File file) {
        if(DirectoryType.RecycleBin.equals(directoryType))
        {
            return RecycleBinUtility.getInstance().inFunctionalDirectory(file);
        }
        else if(DirectoryType.HiddenZone.equals(directoryType))
        {
            return HiddenZoneUtility.getInstance().inFunctionalDirectory(file);
        }
        return false;
    }

    public IFunctionalDirectory getIFunctionalDirectory(DirectoryType directoryType)
    {
        if(DirectoryType.RecycleBin.equals(directoryType))
        {
            return RecycleBinUtility.getInstance();
        }
        else if(DirectoryType.HiddenZone.equals(directoryType))
        {
            return HiddenZoneUtility.getInstance();
        }
        return null;
    }

    public String getExcludeFunctionalDirectorySelection(String pathColumnName, String... volumeRootPaths)
    {
        //add AndroidDirectoryUtility Exclude (/Root/Android/), because duplicateFiles no need to scan /Android/ folder
       return RecycleBinUtility.getInstance().getExcludeDirectorySelection(pathColumnName,volumeRootPaths)+" AND "
                +HiddenZoneUtility.getInstance().getExcludeDirectorySelection(pathColumnName,volumeRootPaths)+ " AND "
                 +AndroidDirectoryUtility.getInstance().getExcludeDirectorySelection(pathColumnName,volumeRootPaths) ;
    }

    public String getLocalizedPath(Context context, String originalPath) {
        if (originalPath.startsWith(HiddenZoneUtility.HIDDEN_ZONE_NAME))
            return originalPath.replaceFirst(
                    HiddenZoneUtility.HIDDEN_ZONE_NAME, context.getResources().getString(R.string.tools_hidden_zone));
        return originalPath;
    }
}
