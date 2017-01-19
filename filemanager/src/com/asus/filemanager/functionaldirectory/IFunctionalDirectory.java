package com.asus.filemanager.functionaldirectory;

import com.asus.filemanager.utility.VFile;

import java.io.File;
import java.util.List;

/**
 * Created by Yenju_Lai on 2016/5/11.
 */
public interface IFunctionalDirectory<T> {
    List<T> listFiles(VFile currentFile, T currentItem);
    boolean inFunctionalDirectory(File file);
    String getFunctionalDirectoryRootPath(String volumeRootPath);
    String getExcludeDirectorySelection(String pathColumnName, String... volumeRootPaths);
}
