package com.asus.filemanager.functionaldirectory.recyclebin;

import com.asus.filemanager.functionaldirectory.DisplayVirtualFile;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

/**
 * Created by Yenju_Lai on 2016/3/29.
 */
public class RecycleBinVFile extends LocalVFile implements DisplayVirtualFile {

    private final String displayName;
    private final Boolean isDirectory;
    private VFile actualFile;

    public RecycleBinVFile(VFile vFile, String name) {
        this(vFile, name, null);
    }

    public RecycleBinVFile(VFile vFile, String name, Boolean isDirectory) {
        super(vFile);
        this.actualFile = vFile;
        this.displayName = name;
        this.isDirectory = isDirectory == null? super.isDirectory() : isDirectory;
    }

    @Override
    public String getName() {
        return displayName == null? super.getName() : displayName;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isFile() {
        return !isDirectory();
    }

    @Override
    public boolean alwaysPermanentlyDelete() {
        return true;
    }

    @Override
    public VFile getActualFile() {
        return actualFile;
    }
}
