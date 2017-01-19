package com.asus.filemanager.filesystem;

import android.content.Context;
import android.util.Log;

import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filetransfer.filesystem.IOutputFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Yenju_Lai on 2015/11/26.
 */
public class OutputBySAF extends IOutputFile {

    private String TAG = this.getClass().getSimpleName();

    Context context;
    SafOperationUtility safOperationUtility;
    File file;

    protected OutputBySAF(Context context, String path) {
        super(path);
        this.context = context;
        file = new File(path);
        safOperationUtility = SafOperationUtility.getInstance();
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
        if (exists())
            return false;
        else {
            safOperationUtility.createNotExistFolder(new File(path + File.separator + "tempFile"));
            return isDirectory();
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!exists())
            createNewFile();
        DocumentFile targetFile = safOperationUtility.getDocFileFromPath(path);
        if (targetFile == null)
            throw new IOException("can't create file by saf");
        return context.getContentResolver().openOutputStream(targetFile.getUri(), "wa");
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public boolean renameTo(String newFilePath) {
        DocumentFile srcFile = safOperationUtility.getDocFileFromPath(file.getAbsolutePath());
        String newFileName = newFilePath.substring(newFilePath.lastIndexOf(File.separator) + 1);
        if (srcFile == null)
            return false;
        if (srcFile.isDirectory() && srcFile.listFiles().length == 0) {
            DocumentFile dstFile = safOperationUtility.getDocFileFromPath(file.getParent() + File.separator + newFileName);
            if (dstFile != null && dstFile.isDirectory()) {
                delete();
                return true;
            }
        }
            boolean success = srcFile.renameTo(newFileName);
            if (success)
                path = newFilePath;
            return success;
        }

    @Override
    public boolean delete() {
        DocumentFile targetFile = safOperationUtility.getDocFileFromPath(path);
        if (targetFile == null)
            return false;
        return targetFile.delete();
    }

    @Override
    public String createNewFile() {
        if (exists())
            return null;
        String parentPath = path.substring(0, path.lastIndexOf(File.separator));
        String fileName = path.substring(path.lastIndexOf(File.separator) + 1);
        DocumentFile parentFile = safOperationUtility.getDocFileFromPath(parentPath);
        DocumentFile file = parentFile.createFile("*/*", fileName);
        return file != null && exists()? path : null;
    }
}
