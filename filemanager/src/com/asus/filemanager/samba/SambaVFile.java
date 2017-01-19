package com.asus.filemanager.samba;

import com.asus.filemanager.utility.VFile;

public class SambaVFile extends VFile {
	String mPath;
//	SmbFile mSmbFile;
    private double mFileSize = 0;
    private boolean mIsDirectory = false;
    private String mParentPath = null;
    private String mName = null;
    private long lastModified  = 0;
//	public SambaVFile(SmbFile smbFile){
//		super(smbFile.getPath(), VFileType.TYPE_SAMBA_STORAGE);
//		mSmbFile = smbFile;
//		mPath = mSmbFile.getPath();
//		try {
//			mIsDirectory = mSmbFile.isDirectory();
//			mFileSize = mSmbFile.getContentLength();
//			mParentPath = mSmbFile.getParent();
//			mName = mSmbFile.getName();
//			lastModified = mSmbFile.getLastModified();
//		} catch (SmbException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	public SambaVFile(String path, boolean isdirectory, double size, String parentPath, String name, long modified){
		super(path, VFileType.TYPE_SAMBA_STORAGE);
		mPath = path;
		mIsDirectory = isdirectory;
		mFileSize = size;
		mParentPath = parentPath;
		mName = name;
		lastModified = modified;

	}

	public SambaVFile(VFile file){
		super(file);
		mPath = file.getAbsolutePath();
		mFileSize = file.length();
		mParentPath = file.getParent();
		mName = file.getName();
	}

	public SambaVFile(String path){
		super(path, VFileType.TYPE_SAMBA_STORAGE);
		mPath = path;
	}

	public String getSambaName(){
		return mName;
	}

	public String getSambaPath(){
		return mPath;
	}

	public int getVFieType() {
        return VFileType.TYPE_SAMBA_STORAGE;
    }

	@Override
    public boolean isDirectory() {
        return mIsDirectory;
    }

	@Override
    public long length() {
        return (long)mFileSize;
    }

	@Override
    public SambaVFile getParentFile() {
		if (mParentPath != null)
			return new SambaVFile(mParentPath);

		return null;
	}

	@Override
    public String getAbsolutePath() {
		return mPath;
	}

	@Override
	public long lastModified(){
		return lastModified;
	}

	public String getParentPath() {
		return mParentPath;
	}



	public String getIndicatorPath(){
		String rootPath = SambaFileUtility.getInstance(null).getRootScanPath();
		String fullPath = mPath;
		if(rootPath.contains("*")){
			fullPath = fullPath.replaceAll("\\*", "s");
			rootPath = rootPath.replaceAll("\\*", "s");
		}
		String indicatorPath = fullPath.replaceFirst(rootPath, "/");
		return indicatorPath;
	}
}
