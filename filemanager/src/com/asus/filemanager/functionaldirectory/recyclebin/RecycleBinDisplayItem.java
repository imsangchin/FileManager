package com.asus.filemanager.functionaldirectory.recyclebin;

import com.asus.filemanager.adapter.DisplayItemAdapter;
import com.asus.filemanager.functionaldirectory.hiddenzone.Encryptor;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneUtility;
import com.asus.filemanager.utility.VFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Yenju_Lai on 2016/3/25.
 */
public class RecycleBinDisplayItem implements DisplayItemAdapter.DisplayItem {

    private boolean checked = false;
    private boolean isDirectory = false;
    private boolean isInExternalStorage = false;
    private VFile vFile;
    private VFile actualDeletedVFile;
    private String name;
    private String originalPath;
    private long deleteTime;
    private long fileLength;

    RecycleBinDisplayItem() {
    }

    public RecycleBinDisplayItem(VFile vFile, boolean isDirectory, boolean isInExternalStorage) {
        this.isDirectory = isDirectory;
        this.isInExternalStorage = isInExternalStorage;
        this.vFile = vFile;
        parseDeleteFileInfo();
        this.vFile = new RecycleBinVFile(vFile, name, isDirectory);
    }

    private void parseDeleteFileInfo() {
        Pattern pattern =
                Pattern.compile("\\.(.+)-([0-9]+)-([0-9]+)-([0-9]*)");
        Matcher matcher = pattern.matcher(vFile.getName());
        if (matcher.find()) {
            this.name = matcher.group(1);
            this.deleteTime = Long.parseLong(matcher.group(3));
            this.fileLength = Long.parseLong(matcher.group(4));
        }
        else throw new RuntimeException("Invalid file name.");
        actualDeletedVFile = findOriginalFileAndPath(Integer.parseInt(matcher.group(2)));
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getDisplayTime() {
        return deleteTime;
    }

    public long getFileLength() {
        return fileLength;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isInExternalStorage() {
        return isInExternalStorage;
    }

    @Override
    public VFile getOriginalFile() {
        return actualDeletedVFile;
    }

    public VFile getCurrentVFile() {
        return vFile;
    }

    private VFile findOriginalFileAndPath(int depth) {
        VFile file = vFile;
        originalPath = file.getParent().replace(isDirectory ?
                RecycleBinUtility.RECYCLE_BIN_DIRECTORY_PATH : RecycleBinUtility.RECYCLE_BIN_FILE_PATH, "");
        if (depth > 0) {
            boolean isInHiddenZone = false;
            String originalName = "";
            for (int i = 0; i < depth; i++) {
                if (file.listVFiles() == null || file.listVFiles().length != 1)
                    throw new RuntimeException("Invalid file counts in recycle bin directory");
                file = file.listVFiles()[0];
                originalName = file.getName().substring(1);
                if (i == 0) {
                    isInHiddenZone = originalName.compareTo(HiddenZoneUtility.HIDDEN_ZONE_DIRECTORY_NAME) == 0;
                    originalPath = isInHiddenZone? HiddenZoneUtility.HIDDEN_ZONE_NAME :  originalPath + "/" + originalName;
                } else if (isInHiddenZone) {
                    int index = originalName.indexOf("-") + 1;
                    index = index > 0? index : 1;
                    originalName = Encryptor.getEncryptor().decode(originalName.substring(index));
                    originalPath = originalPath + "/" + originalName;
                } else {
                    originalPath = originalPath + "/" + originalName;
                }
            }
            if (file.isDirectory() != isDirectory())
                throw new RuntimeException("Not match origin file type");
            if (originalName.compareTo(name) != 0)
                throw new RuntimeException("Not match origin file name");
        }
        actualDeletedVFile = new RecycleBinVFile(file, name, file.isDirectory());
        //originalPath = originalPath.replace("/.", "/");
        return actualDeletedVFile;
    }

    public String getOriginalPath() {
        return originalPath;
    }
}
