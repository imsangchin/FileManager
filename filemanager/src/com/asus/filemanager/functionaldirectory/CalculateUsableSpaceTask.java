package com.asus.filemanager.functionaldirectory;

import android.os.AsyncTask;
import android.os.Environment;

import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.VFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yenju_Lai on 2016/5/4.
 */
public class CalculateUsableSpaceTask extends AsyncTask<VFile, Integer, Boolean> {

    public interface OnSpaceCalculatedListener {
        void onSpaceCalculated(boolean isSufficient);
    }

    Map<String, Long> usableSpaceInStorage = new HashMap<>();
    private final OnSpaceCalculatedListener spaceCalculatedListener;

    public CalculateUsableSpaceTask(OnSpaceCalculatedListener listener) {
        spaceCalculatedListener = listener;
    }

    @Override
    protected Boolean doInBackground(VFile... files) {
        for (VFile file : files) {
            if (file instanceof DisplayVirtualFile)
                file = ((DisplayVirtualFile) file).getActualFile();
            String storageRootPath = SafOperationUtility.getInstance().getRootPathFromFullPath(file.getAbsolutePath());
            if (storageRootPath == null)
                continue;
            final boolean isExternalStorage =
                    storageRootPath.compareTo(Environment.getExternalStorageDirectory().getPath()) == 0;
            long usableSpace =
                    getUsableSpace(storageRootPath) - FileUtility.calculateTotalLength(file, isExternalStorage);
            if (usableSpace < 0)
                return false;
            usableSpaceInStorage.put(storageRootPath, usableSpace);
        }
        return true;
    }

    private long getUsableSpace(String storageRootPath) {
        if (usableSpaceInStorage.containsKey(storageRootPath))
            return usableSpaceInStorage.get(storageRootPath);
        else {
            return new VFile(storageRootPath).getUsableSpace();
        }
    }

    @Override
    protected void onPostExecute(Boolean isSufficient) {        
        if (spaceCalculatedListener != null)
            spaceCalculatedListener.onSpaceCalculated(isSufficient);
    }
}
