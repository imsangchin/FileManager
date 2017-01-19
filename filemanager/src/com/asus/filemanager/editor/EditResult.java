
package com.asus.filemanager.editor;

public class EditResult {

    public static class Error {
        public static final String ENOSPC = "No space left on device";
    }

    public static final int E_SUCCESS = 0;
    public static final int E_FAILURE = 1;
    public static final int E_PERMISSION = 2;
    public static final int E_EXIST = 3;
    public static final int E_NOSPC = 4;
    public static final int E_REMOTEACTION = 5;

    public int ECODE = E_SUCCESS;
    public int numFiles = 0;
    public int numFolders = 0;
    public long numbytes = 0;

    //for Remote Copy
    public double copySize;
    public double copyTotalSize;
    public int copyStatus = 1;

    public int fileTotalCount = 0;
    public int fileCurrentCount = 0;
    public double currentStatus = 1;
    public double currentFileCopySize = 0;
    public double currentFileTotalSize = 0;
    public String mCurrentFileName = "";
}
