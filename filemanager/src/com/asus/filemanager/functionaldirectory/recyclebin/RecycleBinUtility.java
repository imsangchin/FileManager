package com.asus.filemanager.functionaldirectory.recyclebin;

import android.content.Context;
import android.content.SharedPreferences;

import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.functionaldirectory.IFunctionalDirectory;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yenju_Lai on 2016/3/28.
 */
public class RecycleBinUtility implements IFunctionalDirectory<RecycleBinDisplayItem> {
    public static final String RECYCLE_BIN_NAME = ".RecycleBin";
    public static final String RECYCLE_BIN_PATH = "/.RecycleBin";
    public static final String RECYCLE_BIN_FILE_PATH = RECYCLE_BIN_PATH + "/.file";
    public static final String RECYCLE_BIN_DIRECTORY_PATH = RECYCLE_BIN_PATH + "/.directory";

    private static final String TRASH_PREF = "trash_pref";
    private static final String HAVE_NEW_TRASH = "have_new_trash";
    private static final String DELETE_TRASH_TIMESTAMP = "delete_trash_timestamp";
    private static final String TRASH_TIPS_PREF = "trash_tips_pref";
    private static final String DRAWER_NOTICE_PREF = "drawer_notice_pref";
    private static final String PERMANENTLY_DELETE_PREF = "permanently_delete_pref";

    public static void preferPermanentlyDelete(Context context, boolean permanentlyDelete) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(TRASH_PREF, 0);
        sharedPreferences.edit().putBoolean(PERMANENTLY_DELETE_PREF, permanentlyDelete).commit();
    }

    public static boolean getDeleteSetting(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(TRASH_PREF, 0);
        return sharedPreferences.getBoolean(PERMANENTLY_DELETE_PREF, false);
    }

    private static RecycleBinUtility recycleBinUtility;

    public static RecycleBinUtility getInstance() {
        if (recycleBinUtility == null)
            recycleBinUtility = new RecycleBinUtility(FunctionalDirectoryUtility.getInstance());
        return recycleBinUtility;
    }

    private WeakReference<FunctionalDirectoryUtility> utilityReference;
    RecycleBinUtility(FunctionalDirectoryUtility functionalDirectoryUtility) {
        utilityReference = new WeakReference<>(functionalDirectoryUtility);
    }

    List<RecycleBinDisplayItem> listDeletedFiles(VFile storageRoot) {
        return listFiles(new LocalVFile(utilityReference.get().getCanonicalPath(storageRoot) + RECYCLE_BIN_FILE_PATH), false);
    }

    List<RecycleBinDisplayItem> listDeletedDirectory(VFile storageRoot) {
        return listFiles(new LocalVFile(utilityReference.get().getCanonicalPath(storageRoot) + RECYCLE_BIN_DIRECTORY_PATH), true);
    }

    List<RecycleBinDisplayItem> listFiles(VFile targetFile, boolean isDeletedDirectory) {
        List<RecycleBinDisplayItem> result = new ArrayList<>();
        if (targetFile == null || !targetFile.exists() || targetFile.listVFiles() == null || targetFile.listVFiles().length == 0)
            return result;
        boolean isInExternalStorage = utilityReference.get().isInExternalStorage(targetFile.getPath());
        for (VFile file : targetFile.listVFiles()) {
            RecycleBinDisplayItem recycleBinItem = createRecycleBinItem(file, isDeletedDirectory, isInExternalStorage);
            if (recycleBinItem != null) result.add(recycleBinItem);
        }
        return result;
    }

    RecycleBinDisplayItem createRecycleBinItem(VFile file, boolean isDeleteDirectory, boolean isInExternalStorage) {
        try {
            return new RecycleBinDisplayItem(file, isDeleteDirectory, isInExternalStorage);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean inFunctionalDirectory(File file) {
        String filePath;
        try {
            filePath = FileUtility.getCanonicalPath(file);
        } catch (IOException e) {
            filePath = file.getPath();
        }
        String rootPath = SafOperationUtility.getInstance().getRootPathFromFullPath(filePath);
        if (rootPath == null)
            return false;
        return filePath.startsWith(rootPath + RECYCLE_BIN_PATH);
    }

    @Override
    public List<RecycleBinDisplayItem> listFiles(VFile currentFile, RecycleBinDisplayItem currentItem) {
        List<RecycleBinDisplayItem> result = new ArrayList<>();
        result.addAll(recycleBinUtility.listDeletedDirectory(currentFile));
        result.addAll(recycleBinUtility.listDeletedFiles(currentFile));
        return result;
    }


    @Override
    public String getFunctionalDirectoryRootPath(String volumeRootPath) {
        return volumeRootPath+RECYCLE_BIN_PATH;
    }

    @Override
    public String getExcludeDirectorySelection(String pathColumnName, String... volumeRootPaths) {
        if(volumeRootPaths==null)
            return null;
        if(volumeRootPaths.length==0)
            return "(" + pathColumnName + " NOT LIKE '%" + RecycleBinUtility.RECYCLE_BIN_FILE_PATH
                    + "/%' AND "+ pathColumnName + " NOT LIKE '%" + RecycleBinUtility.RECYCLE_BIN_DIRECTORY_PATH + "/%' )";

        String selection ="(";
        for(int i=0;i<volumeRootPaths.length;i++)
        {
            if(i!=0)
                selection += " OR ";
            //avoid SQLite injection
            volumeRootPaths[i] = volumeRootPaths[i].replaceAll("'","''");
            selection += pathColumnName + " not like'" + getFunctionalDirectoryRootPath(volumeRootPaths[i])+"%'";
        }
        selection += ")";
        return selection;
    }

}
