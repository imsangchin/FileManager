
package com.asus.filemanager.editor;

import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteVFile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditPool implements Serializable {

    private static final long serialVersionUID = 6910737363217829019L;
    private VFile[] mPool;
    private boolean mExtraBoolean = false;
    private String mPastePath;
    private String mZipName;
    private int mTargetDataType;
    private VFile mPasteVFile;
    private int pasteDialogType;

    public EditPool() {
    }

    public int getSize() {
        if (mPool == null)
            return 0;
        return mPool.length;
    }

    public VFile getFile() {
        if (mPool == null || mPool.length == 0)
            return null;
        return mPool[0];
    }

    public VFile[] getFiles() {
        return mPool;
    }

    public void setFile(VFile f) {
        if (f == null)
        {
            mPool = null;
            return;
        }
        mPool = new VFile[1];
        mPool[0] = new VFile(f.getAbsolutePath());
    }

    public int setFiles(List<VFile> source, boolean isNeedCheck) {
        if (source == null) {
            return 0;
        }

        VFile[] array = source.toArray(new VFile[source.size()]);
        return setFiles(array, isNeedCheck);
    }

    public int setFiles(VFile[] source, boolean isNeedCheck) {
        ArrayList<VFile> array = new ArrayList<VFile>();
        if (source == null)
            return array.size();

        //++yiqiu_huang, handle samba case
        if (source.length > 0 && source[0].getVFieType() == VFileType.TYPE_SAMBA_STORAGE){
        	for (int i = 0; i < source.length && source[i] != null; i++) {
                if (!isNeedCheck || (isNeedCheck && source[i].getChecked()))
                    array.add(source[i]);
            }
        	mPool = new SambaVFile[array.size()];
        	for (int i = 0; i < mPool.length; i++)
                mPool[i] = array.get(i);
            return array.size();
        }

        boolean isRemoteType = false;
        if (source.length > 0) {
            isRemoteType = (source[0].getVFieType() != VFileType.TYPE_LOCAL_STORAGE);
        }
        for (int i = 0; i < source.length && source[i] != null; i++) {
            if (!isNeedCheck || (isNeedCheck && source[i].getChecked()))
                array.add(source[i]);
//                array.add(isRemoteType ? new RemoteVFile(source[i]) : new LocalVFile(source[i].getAbsolutePath()));
        }
        mPool = (isRemoteType ? new RemoteVFile[array.size()] : new LocalVFile[array.size()]);

        for (int i = 0; i < mPool.length; i++)
            mPool[i] = array.get(i);
        return array.size();
    }

    public void clear() {
        mPool = null;
    }

    public boolean getExtraBoolean() {
        return mExtraBoolean;
    }

    public void setExtraBoolean(boolean extra) {
        mExtraBoolean = extra;
    }

    public void setPastePath(String path) {
        mPastePath = path;
    }

    public String getPastePath() {
        return mPastePath;
    }

    public void setZipName(String path) {
        mZipName = path;
    }

    public String getZipName() {
        return mZipName;
    }

    // +++ remote storage function
    public void setTargetDataType(int value) {
        mTargetDataType = value;
    }

    public int getTargetDataType() {
        return mTargetDataType;
    }

    public void setPasteVFile(VFile file) {
        mPasteVFile = file;
    }

    public VFile getPasteVFile() {
        return mPasteVFile;
    }

	public int getPasteDialogType() {
		return pasteDialogType;
	}

	public void setPasteDialogType(int pasteDialogType) {
		this.pasteDialogType = pasteDialogType;
	}

    // ---
}
