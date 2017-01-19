package com.asus.filemanager.filesystem;

import com.asus.filetransfer.filesystem.IInputFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by Yenju_Lai on 2015/11/3.
 */
public class StorageRoot extends IInputFile {
    List<IInputFile> storageList;

    public StorageRoot(List<IInputFile> storageList) {
        super(File.separator);
        this.storageList = storageList;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getPath() {
        return File.separator;
    }

    @Override
    public List<IInputFile> listChildren() {
        return storageList;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public InputStream getPartialInputStream(long from, long to) throws IllegalArgumentException {
        return null;
    }

    @Override
    public long getSize() {
        return storageList.size();
    }
}
