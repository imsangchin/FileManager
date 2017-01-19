package com.asus.filemanager.utility;
import android.content.Context;
import android.os.AsyncTask;

import com.asus.filemanager.provider.AllFilesDatabase;
import com.asus.filemanager.provider.DuplicateFilesDatabase;

public class UpdateDeleteFilesTask extends AsyncTask<VFile, Integer, Boolean> {

    public static final int MODE_UPDATE_FILES_IN_SAME_FOLDER = 0;
    public static final int MODE_UPDATE_FILES_IN_DIFFERENT_FOLDER =1;

    private AllFilesDatabase allFilesDatabase;
    private DuplicateFilesDatabase duplicateFilesDatabase;
    private OnUpdateDeleteFilesResultListener onUpdateDeleteFilesResultListener;
    long deleteSizes = 0;
    private VFile[] deleteFiles;
    private int mode = MODE_UPDATE_FILES_IN_SAME_FOLDER;

    public interface OnUpdateDeleteFilesResultListener {
        public void onUpdateDeleteStart();
        public void onUpdateDeleteResult(boolean success, long deleteSizes);
    }

    public UpdateDeleteFilesTask(Context context, VFile[] deleteFiles, OnUpdateDeleteFilesResultListener onUpdateDeleteFilesResultListener,int mode) {
        this.mode = mode;
        this.deleteFiles = deleteFiles;
        this.onUpdateDeleteFilesResultListener = onUpdateDeleteFilesResultListener;
        allFilesDatabase = new AllFilesDatabase(context);
        duplicateFilesDatabase = new DuplicateFilesDatabase(context);
    }

    public UpdateDeleteFilesTask(Context context, VFile[] deleteFiles, OnUpdateDeleteFilesResultListener onUpdateDeleteFilesResultListener) {
        this(context,deleteFiles,onUpdateDeleteFilesResultListener, MODE_UPDATE_FILES_IN_SAME_FOLDER);
    }

    public void onPreExecute() {
        if (onUpdateDeleteFilesResultListener != null)
            onUpdateDeleteFilesResultListener.onUpdateDeleteStart();
    }

    @Override
    public Boolean doInBackground(VFile... files) {
        // TODO Auto-generated method stub
        if (deleteFiles == null)
            return false;

        switch (mode) {
            case MODE_UPDATE_FILES_IN_SAME_FOLDER:
                updateDeleteFilesInSameFolder();
                break;
            case MODE_UPDATE_FILES_IN_DIFFERENT_FOLDER:
                updateDeleteFilesInDifferentFolder();
                break;
        }

        duplicateFilesDatabase.delete(files);

        return true;
    }

    private void updateDeleteFilesInSameFolder()
    {
        //calc deleteSizes
        for (int i = 0; i < deleteFiles.length; i++) {
            deleteSizes += deleteFiles[i].length();
        }
        //get group id for query root path
        int groupId = allFilesDatabase.getFile(FileUtility.getCanonicalPathNoException(deleteFiles[0])).getGroupId();
        allFilesDatabase.delete(deleteFiles);

        VFile currentParentFile = deleteFiles[0].getParentFile();
        String rootPath = allFilesDatabase.getGroupRootPath(groupId);

        updateAllParentFilesDeleteSizes(rootPath, currentParentFile, deleteSizes);
        return;
    }

    private void updateDeleteFilesInDifferentFolder()
    {
        //get group id for query root path
        int groupId = allFilesDatabase.getFile(FileUtility.getCanonicalPathNoException(deleteFiles[0])).getGroupId();
        String rootPath = allFilesDatabase.getGroupRootPath(groupId);
        for (int i = 0; i < deleteFiles.length; i++) {
            String filePath = FileUtility.getCanonicalPathNoException(deleteFiles[i]);
            if(filePath != null && rootPath != null && !filePath.startsWith(rootPath))
            {
                //root path change
                groupId = allFilesDatabase.getFile(FileUtility.getCanonicalPathNoException(deleteFiles[0])).getGroupId();
                rootPath = allFilesDatabase.getGroupRootPath(groupId);
            }

            updateAllParentFilesDeleteSizes(rootPath, deleteFiles[i].getParentFile(), deleteFiles[i].length());
        }
        duplicateFilesDatabase.delete(deleteFiles);

        return;
    }

    private void updateAllParentFilesDeleteSizes(String rootPath,VFile currentParentFile,long deleteSizes){
        if (FileUtility.getCanonicalPathNoException(currentParentFile).equals(rootPath) || rootPath==null) {
            return;
        }
        String updateFilePath = FileUtility.getCanonicalPathNoException(currentParentFile);
        //search parent folder until root and update delete sizes
        while (!updateFilePath.equals(rootPath)) {
            VFile vFile = allFilesDatabase.getFile(updateFilePath);
            if (vFile != null) {
                if(vFile.length()==0)
                    break;
                vFile.setInStoragePercentage((1-((float)deleteSizes/vFile.length())) * vFile.getInStoragePercentage());
                vFile.setLength(vFile.length() - deleteSizes);
                allFilesDatabase.update(vFile);
            }
            currentParentFile = currentParentFile.getParentFile();
            updateFilePath = FileUtility.getCanonicalPathNoException(currentParentFile);
        }
    }

    protected void onPostExecute(Boolean result) {
        if (onUpdateDeleteFilesResultListener != null) {
            onUpdateDeleteFilesResultListener.onUpdateDeleteResult(result, deleteSizes);
        }
    }
}
