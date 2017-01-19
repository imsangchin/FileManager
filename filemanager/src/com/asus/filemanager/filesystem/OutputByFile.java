package com.asus.filemanager.filesystem;

import android.util.Log;

import com.asus.filetransfer.filesystem.IOutputFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Yenju_Lai on 2015/9/14.
 */
public class OutputByFile extends IOutputFile {

    private String TAG = this.getClass().getSimpleName();
    File file = null;

    public OutputByFile(String path) throws IOException {
        super(path);
        try {
            file = new File(path).getCanonicalFile();
        } catch (IOException e) {
            file = new File(path);
        }
        if (!file.exists()) {
            if (file.createNewFile()) {
                file.delete();
            }
            else
                throw new IOException("No write permission");
        }
        else if (!file.canWrite())
            throw new IOException("No write permission");
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
    public boolean mkdirs() {
        return file.mkdirs();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(file, true);
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public boolean renameTo(String newFilePath) {
        File newFile = new File(newFilePath);
        boolean success;
        if (newFile.isDirectory())
            success = this.delete();
        else
            success = file.renameTo(newFile);
        if (success) {
            path = newFilePath;
            file = new File(path);
        }
        return success;
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @Override
    public String createNewFile() {
        try {
            return file.createNewFile()? path : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
