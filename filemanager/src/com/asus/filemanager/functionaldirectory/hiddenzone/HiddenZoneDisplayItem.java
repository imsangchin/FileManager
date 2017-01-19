package com.asus.filemanager.functionaldirectory.hiddenzone;

import com.asus.filemanager.adapter.DisplayItemAdapter;
import com.asus.filemanager.utility.VFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Yenju_Lai on 2016/5/12.
 */
public class HiddenZoneDisplayItem implements DisplayItemAdapter.DisplayItem {

    private boolean checked = false;
    private boolean isInExternalStorage = false;
    private VFile vFile;
    private VFile actualDeletedVFile;
    private String name;
    private String originalPath;
    private long addedTime;
    private long fileLength;

    public HiddenZoneDisplayItem(VFile file, boolean isInExternalStorage) {
        this.isInExternalStorage = isInExternalStorage;
        parseFileName(file.getName());
        this.vFile = new HiddenZoneVFile(file, name, addedTime);
    }

    private void parseFileName(String name) {
        Pattern pattern =
                Pattern.compile("\\.([0-9]+)-(.+)");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            this.addedTime = Long.parseLong(matcher.group(1));
            this.name = Encryptor.getEncryptor().decode(matcher.group(2));
        }
        else throw new RuntimeException("Invalid file name.");
    }

    public HiddenZoneDisplayItem(VFile file, HiddenZoneDisplayItem parent) {
        this.isInExternalStorage = parent.isInExternalStorage();
        this.addedTime = parent.getDisplayTime();
        if (file instanceof HiddenZoneVFile) {
            name = file.getName();
            this.vFile = file;
        } else {
            decodeName(file.getName());
            this.vFile = new HiddenZoneVFile(file, name, addedTime);
        }
    }

    private void decodeName(String name) {
        if (!name.startsWith("."))
            throw new RuntimeException("Invalid file name.");
        this.name = Encryptor.getEncryptor().decode(name.substring(1));
    }

    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public VFile getOriginalFile() {
        return vFile;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isInExternalStorage() {
        return isInExternalStorage;
    }

    @Override
    public long getDisplayTime() {
        return addedTime;
    }

    public VFile getCurrentVFile() {
        return vFile;
    }
}
