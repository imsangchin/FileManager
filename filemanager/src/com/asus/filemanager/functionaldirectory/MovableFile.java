package com.asus.filemanager.functionaldirectory;

import android.support.annotation.NonNull;

import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Yenju_Lai on 2016/4/13.
 */
public abstract class MovableFile implements Movable {
    protected VFile vFile;
    protected VFile dstFile;
    protected String destPath;

    protected MovableFile(VFile vFile) {
        this.vFile = vFile;
    }

    protected MovableFile(VFile vFile, Movable parent, String displayName) {
        this.vFile = vFile;
        this.destPath = parent.getDestination() + "/" + displayName;
    }

    @Override
    public VFile getSourceFile() {
        return vFile;
    }

    @Override
    public String getDestination() {
        return destPath;
    }

    @Override
    public boolean destinationExist() {
        return getDstFile().exists();
    }

    @NonNull
    private VFile getDstFile() {
        return dstFile == null? (dstFile = new LocalVFile(getDestination())) : dstFile;
    }

    @Override
    public boolean move() {
        File dstFile = getDstFile();
        File dstParentFile = new LocalVFile(dstFile.getParent());
        if (!dstParentFile.exists() && !dstParentFile.mkdirs()) {
            FileUtility.checkUsableSpace((long)FileUtility.MIN_FOLDER_SIZE, dstFile);
            return false;
        }
        /*if (SafOperationUtility.getInstance().isNeedToWriteSdBySaf(vFile.getPath()))
            try {
                return renameBySAF();
            } catch (IOException e) {
                return false;
            }*/
        return vFile.renameTo(dstFile);
    }

    private boolean renameBySAF() throws IOException {
        DocumentFile parent = SafOperationUtility.getInstance().getDocFileFromPath(getDstFile().getParent());
        if (parent == null)
            return false;
        DocumentFile dstDocFile = parent.createFile("*/*", getDstFile().getName());
        final int bufLen = vFile.length() > 65536 ? 65536 : 8192;
            byte[] buffer = new byte[bufLen];
            int rd;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(vFile));
            BufferedOutputStream bos = SafOperationUtility.getInstance().getDocFileOutputStream(dstDocFile);
            try {
            while ((rd = bis.read(buffer, 0, bufLen)) != -1)
            {
                    bos.write(buffer, 0, rd);
                }
                bos.flush();
            } finally {
                bis.close();
                bos.close();
                SafOperationUtility.getInstance().closeParcelFile();
        }
        return vFile.delete();
    }
}
