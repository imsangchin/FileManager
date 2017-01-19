package com.asus.filemanager.functionaldirectory.androidDirectory;

import com.asus.filemanager.functionaldirectory.IFunctionalDirectory;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AndroidDirectoryUtility implements IFunctionalDirectory<AndroidDirectoryDisplayItem>{

    public static final String ANDROID_DIRECTORY_NAME = "Android";
    public static final String ANDROID_DIRECTORY_PATH = "/Android";

    private static AndroidDirectoryUtility sAndroidDirectoryUtility;

    public static AndroidDirectoryUtility getInstance() {
        if (sAndroidDirectoryUtility == null)
            sAndroidDirectoryUtility = new AndroidDirectoryUtility();
        return sAndroidDirectoryUtility;
    }

    @Override
    public List<AndroidDirectoryDisplayItem> listFiles(VFile currentFile, AndroidDirectoryDisplayItem currentItem) {
        return listRootFiles(currentFile);
    }

    @Override
    public boolean inFunctionalDirectory(File file) {
        return file.getAbsolutePath().contains(ANDROID_DIRECTORY_PATH);
    }

    @Override
    public String getFunctionalDirectoryRootPath(String volumeRootPath) {
        return volumeRootPath+ANDROID_DIRECTORY_PATH;
    }

    @Override
    public String getExcludeDirectorySelection(String pathColumnName, String... volumeRootPaths) {
        if(volumeRootPaths==null)
            return null;
        if(volumeRootPaths.length==0)
            return "(" + pathColumnName + " NOT LIKE '%" + ANDROID_DIRECTORY_PATH
                    + "/%' )";

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


    public List<AndroidDirectoryDisplayItem> listRootFiles(VFile storageRoot) {
        VFile androidDirectory = new LocalVFile(storageRoot, ANDROID_DIRECTORY_NAME);
        List<AndroidDirectoryDisplayItem> files = new ArrayList<>();
        if ( !androidDirectory.exists() || androidDirectory.listVFiles() == null)
            return files;
        for (VFile file : androidDirectory.listVFiles()) {
            try {
                files.add(new AndroidDirectoryDisplayItem(file));
            } catch (RuntimeException e) {
                //do nothing
            }
        }
        return files;
    }
}
