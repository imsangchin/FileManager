package com.asus.filemanager.functionaldirectory.recyclebin;

import com.asus.filemanager.functionaldirectory.MovableFile;
import com.asus.filemanager.utility.VFile;

/**
 * Created by Yenju_Lai on 2016/4/13.
 */
public class RestoreFile extends MovableFile {

    public RestoreFile(VFile vFile, String storageRoot) {
        super(vFile);
        destPath = storageRoot;
    }

    public RestoreFile(VFile file, RestoreFile parentFile) {
        super(new RecycleBinVFile(file, file.getName().substring(1)), parentFile, file.getName().substring(1));
    }

    @Override
    public String getDestination() {
        return destPath;
    }
}
