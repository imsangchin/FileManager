package com.asus.filemanager.functionaldirectory.hiddenzone;

import com.asus.filemanager.functionaldirectory.MovableFile;
import com.asus.filemanager.utility.VFile;

import java.util.Date;

/**
 * Created by Yenju_Lai on 2016/5/10.
 */
public class HideFile extends MovableFile{

    public HideFile(VFile vFile, String rootPath) {
        super(vFile);
        getHiddenZoneDestination(rootPath);
    }

    public HideFile(VFile file, HideFile parentFile) {
        super(file, parentFile, "." + Encryptor.getEncryptor().encode(file.getName()));
    }

    private void getHiddenZoneDestination(String rootPath) {
        /*String relativePath = vFile.getPath().substring(rootPath.length());
        int depth = relativePath.split("/").length - 1;
        destPath = rootPath + (vFile.isDirectory() ? HiddenZoneUtility.HIDDEN_ZONE_DIRECTORY_PATH : HiddenZoneUtility.HIDDEN_ZONE_FILE_PATH)
                + "/." + Encryptor.getEncryptor().encode(vFile.getName()) + "-" + depth + "-" + getCurrentTime();
        String[] route = relativePath.split("/");
        for (String directoryName : route) {
            if (directoryName.length() > 0) {
                destPath = destPath + "/." + Encryptor.getEncryptor().encode(directoryName);
            }
        }*/
        destPath = rootPath + HiddenZoneUtility.HIDDEN_ZONE_PATH + "/." + getCurrentTime() + "-"
                + Encryptor.getEncryptor().encode(vFile.getName());
    }

    private long getCurrentTime() {
        return mockTime == -1? new Date().getTime() : mockTime;
    }

    private static long mockTime = -1;

    static void setMockTime(long mockTime) {
        HideFile.mockTime = mockTime;
    }
}
