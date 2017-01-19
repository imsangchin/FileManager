package com.asus.filemanager.functionaldirectory;
import android.os.AsyncTask;

import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ChenHsin_Hsieh on 2016/2/19.
 */
public class ScanFunctionalDirectoryTask<T> extends AsyncTask<VFile, Integer, List<T>> {

    private final T currentItem;
    private IFunctionalDirectory functionalDirectory;
    private OnScanFunctionalDirectoryResultListener onScanFunctionalDirectoryResultListener;

    public interface OnScanFunctionalDirectoryResultListener<T> {
        void onScanStart();
        void onScanResult(List<T> result);
    }

    public ScanFunctionalDirectoryTask(IFunctionalDirectory<T> functionalDirectory,
                                          OnScanFunctionalDirectoryResultListener onScanSpecialDirectoryResultListener)
    {
        this(functionalDirectory, null, onScanSpecialDirectoryResultListener);
    }

    public ScanFunctionalDirectoryTask(IFunctionalDirectory<T> functionalDirectory, T targetItem,
                                          OnScanFunctionalDirectoryResultListener onScanSpecialDirectoryResultListener)
    {
        this.onScanFunctionalDirectoryResultListener = onScanSpecialDirectoryResultListener;
        this.functionalDirectory = functionalDirectory;
        this.currentItem = targetItem;
    }
    @Override
    protected void onPreExecute() {
        if (onScanFunctionalDirectoryResultListener != null)
            onScanFunctionalDirectoryResultListener.onScanStart();
    }

    @Override
    protected List<T> doInBackground(VFile... files) {
        List<T> result = new ArrayList<>();
        if (files == null)
            return result;
        for (VFile file : files) {
            if (isCancelled())
                break;
            if (file == null)
                continue;
            result.addAll(functionalDirectory.listFiles(file, currentItem));
        }

        // also scan the functional directory under twinApps volume
        if (!isCancelled()) {
            String twinAppsVolumePath = reflectionApis.getTwinAppsStorageVolumePath();
            if (twinAppsVolumePath != null) {
                result.addAll(functionalDirectory.listFiles(
                        new VFile(twinAppsVolumePath), currentItem));
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(List<T> result) {
        if (onScanFunctionalDirectoryResultListener != null && !isCancelled())
            onScanFunctionalDirectoryResultListener.onScanResult(result);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        onScanFunctionalDirectoryResultListener = null;
    }
}
