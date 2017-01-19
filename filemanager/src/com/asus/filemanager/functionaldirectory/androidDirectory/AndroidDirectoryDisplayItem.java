package com.asus.filemanager.functionaldirectory.androidDirectory;

import com.asus.filemanager.adapter.DisplayItemAdapter;
import com.asus.filemanager.utility.VFile;

public class AndroidDirectoryDisplayItem implements DisplayItemAdapter.DisplayItem{

    private VFile mVFile;

    public AndroidDirectoryDisplayItem(VFile file) {
        this.mVFile = new VFile(file);
    }
    @Override
    public void setChecked(boolean checked) {

    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public VFile getOriginalFile() {
        if(mVFile !=null)
            return mVFile;
        return null;
    }

    @Override
    public String getName() {
        if(mVFile !=null)
            return mVFile.getName();
        return null;
    }

    @Override
    public boolean isInExternalStorage() {
        return false;
    }

    @Override
    public VFile getCurrentVFile() {
        if(mVFile !=null)
            return mVFile;
        return null;
    }

    @Override
    public long getDisplayTime() {
        return 0;
    }
}
