package com.asus.filemanager.functionaldirectory.hiddenzone;

import android.util.Log;

import com.asus.filemanager.functionaldirectory.DisplayVirtualFile;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility.DirectoryType;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Yenju_Lai on 2016/3/29.
 */
public class HiddenZoneVFile extends LocalVFile implements DisplayVirtualFile {

    private final String displayName;
    private VFile actualFile;
    private final long modifyTime;


    public HiddenZoneVFile(VFile vFile, String name, long addedTime) {
        super(vFile);
        this.actualFile = vFile;
        this.displayName = name;
        this.modifyTime = addedTime;
    }

    @Override
    public String getName() {
        return displayName == null? super.getName() : displayName;
    }

    @Override
    public VFile getActualFile() {
        return actualFile;
    }

    @Override
    public LocalVFile[] listVFiles() {
        return (LocalVFile[] )listFiles((FileFilter)null);
        /*VFile[] files = super.listVFiles();
        HiddenZoneVFile[] hiddenZoneVFiles = new HiddenZoneVFile[files.length];
        for (int i = 0; i < files.length; i++) {
            Pattern pattern =
                    Pattern.compile("\\.([0-9]+)-(.+)");
            Matcher matcher = pattern.matcher(files[i].getName());
            hiddenZoneVFiles[i] = matcher.find()?
                    new HiddenZoneVFile(files[i], Encryptor.getEncryptor().decode(matcher.group(2)), modifyTime)
                    : new HiddenZoneVFile(files[i], Encryptor.getEncryptor().decode(files[i].getName().substring(1)), modifyTime);
        }
        return hiddenZoneVFiles;*/
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        VFile[] files = super.listVFiles();
        int fileCount = files.length;
        for (int i = 0; i < files.length; i++) {
            try {
                Pattern pattern =
                        Pattern.compile("\\.([0-9]+)-(.+)");
                Matcher matcher = pattern.matcher(files[i].getName());
                files[i] = matcher.find() ?
                        new HiddenZoneVFile(files[i], Encryptor.getEncryptor().decode(matcher.group(2)), modifyTime)
                        : new HiddenZoneVFile(files[i], Encryptor.getEncryptor().decode(files[i].getName().substring(1)), modifyTime);
                if (filter == null || filter.accept(files[i])) {
                    continue;
                }
            } catch(Exception e) {
                //file name not encrypt
            }
            files[i] = null;
            fileCount--;
        }
        if (fileCount == files.length)
            return files;
        File[] newFiles = new File[fileCount];
        for (int i = 0, j = 0; i < files.length; i++) {
            if (files[i] != null) {
                newFiles[j++] = files[i];
            }
        }
        return newFiles;
    }

    @Override
    public boolean renameTo(File newPath) {
        //Log.d("test", "renameTo: " + newPath.getName());
        if (FunctionalDirectoryUtility.getInstance().inFunctionalDirectory(DirectoryType.HiddenZone, newPath)) {
        String expectedName = newPath.getName();
        String actualFileName = super.getName();
        Encryptor.IEncryptor encryptor = Encryptor.getEncryptor();
        String newFileName = actualFileName.replace(encryptor.encode(displayName), encryptor.encode(expectedName));
        return super.renameTo(new File(getParent(), newFileName));
        } else {
            return super.renameTo(newPath);
        }
    }

    @Override
    public long lastModified() {
        return modifyTime == -1? actualFile.lastModified() : modifyTime;
    }
}
