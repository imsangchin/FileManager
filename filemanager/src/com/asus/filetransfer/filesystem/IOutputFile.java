package com.asus.filetransfer.filesystem;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Yenju_Lai on 2015/9/14.
 */
public abstract class IOutputFile {

    protected String path;
    protected IOutputFile(String path) {
        if (path == null)
            throw new NullPointerException("Path can't be null");
        this.path = path;
    }

    public abstract boolean exists();

    public abstract boolean isDirectory();

    public abstract boolean mkdirs();

    public abstract OutputStream getOutputStream() throws IOException;

    public abstract long getSize();

    public String getPath() { return path; }

    public abstract boolean renameTo(String newFilePath);

    public abstract boolean delete();

    public abstract String createNewFile();
}
