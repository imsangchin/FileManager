package com.asus.filemanager.filesystem;

import com.asus.filemanager.utility.DateUtility;
import com.asus.filetransfer.filesystem.IInputFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yenju_Lai on 2015/9/14.
 */
public class LocalFile extends IInputFile {
    File file = null;
    FileManager fileManager;

    public LocalFile(String path, FileManager fileManager) {
        super(path);
        try {
            file = new File(path).getCanonicalFile();
        } catch (IOException e) {
        file = new File(path);
    }
        this.fileManager = fileManager;
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public String getName() {
        return file.exists() ? file.getName() : null;
    }

    @Override
    public String getPath() {
        return file.exists() ? file.getPath() : null;
    }

    @Override
    public List<IInputFile> listChildren() {
        if (!file.exists() || !file.isDirectory())
            return null;
        List<IInputFile> list = new ArrayList<IInputFile>();
        for (File child : file.listFiles())
            list.add(new LocalFile(child.getPath(), fileManager));
        return list;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public InputStream getPartialInputStream(long from, long to) throws IllegalArgumentException {
        if (from < 0 || (to - from) < 0)
            throw new IllegalArgumentException();
        try {
            InputStream inputStream = getInputStream();
            inputStream.skip(from);
            return inputStream;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected String getModifiedTime() {
        return DateUtility.formatShortDateAndTime(null, file.lastModified());
    }

    @Override
    public long getSize() {
        return isDirectory()? file.listFiles() == null? 0 :file.listFiles().length  : file.length();
    }

    @Override
    protected Writable canWrite() {
        return fileManager.canWrite(getPath());
    }
}
