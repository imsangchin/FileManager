package com.asus.filemanager.functionaldirectory.recyclebin;

import com.asus.filemanager.functionaldirectory.Movable;
import com.asus.filemanager.functionaldirectory.MovableFile;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.util.Date;

/**
 * Created by Yenju_Lai on 2016/3/14.
 */
public class DeleteFile extends MovableFile {

    public DeleteFile(VFile vFile, String rootPath) {
        super(vFile);
        getDestination(rootPath, vFile.getName(), calculateTotalLength(vFile));
    }

    public DeleteFile(VFile vFile, Movable parent) {
        super(vFile, parent, "." + vFile.getName());
    }

    public DeleteFile(VFile vFile, String rootPath, String fileName) {
        super(vFile);
        getDestination(rootPath, fileName, calculateTotalLength(vFile));
    }

    public void getDestination(String rootPath, String fileName, long totalLength) {
        String relativePath = vFile.getPath().substring(rootPath.length());
        int depth = relativePath.split("/").length - 1;
        String rootDirectoryPath =  rootPath + (vFile.isDirectory() ?RecycleBinUtility.RECYCLE_BIN_DIRECTORY_PATH : RecycleBinUtility.RECYCLE_BIN_FILE_PATH)
                + "/." + fileName + "-" + depth + "-" + getCurrentTime() + "-" + totalLength;
        destPath = rootDirectoryPath + relativePath.replace("/", "/.");
    }

    private long getCurrentTime() {
        return mockTime == -1? new Date().getTime() : mockTime;
    }

    private static long mockTime = -1;

    static void setMockTime(long mockTime) {
        DeleteFile.mockTime = mockTime;
    }

    private long calculateTotalLength(VFile targetFile) {
        if (targetFile.getHasRetrictFiles()) {
            long totalLength = 0;
            for (LocalVFile file : targetFile.getRestrictFiles())
                totalLength += file.length();
            return totalLength;
        }
        if (!targetFile.isDirectory())
            return targetFile.length();
        else if (targetFile.listVFiles() == null)
            return 0;
        else {
            long totalLength = 0;
            for (VFile file : targetFile.listVFiles())
                totalLength += calculateTotalLength(file);
            return totalLength;
        }
    }
}
