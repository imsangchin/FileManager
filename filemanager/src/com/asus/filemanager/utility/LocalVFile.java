package com.asus.filemanager.utility;

import android.os.Parcel;
import android.os.Parcelable;

import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

public class LocalVFile extends VFile implements Parcelable {

    private static final String TAG = "LocalVFile";
    private DocumentFile mDocumentFile;

    public LocalVFile(String path) {
        super(path);
    }

    public LocalVFile(String path, int type) {
        super(path, type);
    }

    public LocalVFile(String path, int type, int categoryItem) {
        super(path, type, categoryItem);
    }

    public LocalVFile(File dir, String name) {
        super(dir, name);
    }

    public LocalVFile(String dirPath, String name) {
        super(dirPath, name);
    }

    public LocalVFile(URI uri) {
        super(uri);
    }

    public LocalVFile(File file) {
        super(file.getAbsolutePath());
    }

    public String getExtensiontName() {
        int p = 0;
        if (!isDirectory()) {
            p = getName().lastIndexOf('.');
            if (p < 0) {
                p = 0;
            } else {
                p++;
            }
        }
        return getName().substring(p);
    }

    public String getNameNoExtension() {
        int p = getName().length();
        if (!isDirectory()) {
            p = getName().lastIndexOf('.');
            if (p <= 0) {
                p = getName().length();
            }
        }
        return getName().substring(0, p);
    }

/*
    public String getFolderPath() {
        int p = 0;
        p = getAbsolutePath().lastIndexOf('/');
        if (p < 0) {
            p = 0;
        }
        return getAbsolutePath().substring(0, p);
    }

    public String getAttrFull() {
        String s = "[DFHRWEA]";
        if (!isDirectory())
            s = s.replace('D', '-');
        if (!isFile())
            s = s.replace('F', '-');
        if (!isHidden())
            s = s.replace('H', '-');
        if (!canRead())
            s = s.replace('R', '-');
        if (!canWrite())
            s = s.replace('W', '-');
        if (!exists())
            s = s.replace('E', '-');
        if (!isAbsolute())
            s = s.replace('A', '-');
        return s;
    }
*/
    public String getAttrSimple() {
        String s = "DRW";
        if (!isDirectory())
            s = s.replace('D', '-');
        if (!canRead())
            s = s.replace('R', '-');
        if (!canWrite())
            s = s.replace('W', '-');
        return s;
    }

    public boolean canDelete() {
        boolean canDelete = false;
        File parent = getParentFile();
        if (parent != null && parent.canWrite()) {
            canDelete = true;
        }
        return canDelete;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getAbsolutePath());
        out.writeInt(mChecked ? 1 : 0);
    }

    public static final Parcelable.Creator<LocalVFile> CREATOR = new Parcelable.Creator<LocalVFile>() {
        public LocalVFile createFromParcel(Parcel in) {
            return new LocalVFile(in);
        }

        public LocalVFile[] newArray(int size) {
            return new LocalVFile[size];
        }
    };

    public LocalVFile(Parcel in) {
        super(in.readString());
        mChecked = (in.readInt() == 1);
    }

    public LocalVFile(File file, int type) {
        super(file, type);
    }

    @Override
    public boolean mkdir() {
        if (SafOperationUtility.getInstance().isNeedToWriteSdBySaf(this.getAbsolutePath())) {
            DocumentFile parentFile = SafOperationUtility.getInstance().getDocFileFromPath(getParent());
            return parentFile == null? false : parentFile.createDirectory(getName()) != null;
        }
        return super.mkdir();
    }

    @Override
    public boolean mkdirs() {
        if (exists())
            return false;
        if (SafOperationUtility.getInstance().isNeedToWriteSdBySaf(this.getAbsolutePath())) {
            SafOperationUtility.getInstance().createNotExistFolder(new File(getAbsolutePath() + File.separator + "tempFile"));
            return isDirectory();
        }
        return super.mkdirs();
    }

    @Override
    public boolean renameTo(File newPath) {
        if (SafOperationUtility.getInstance().isNeedToWriteSdBySaf(this.getAbsolutePath())) {
            DocumentFile documentFile = getDocumentFile();
            return documentFile == null? super.renameTo(newPath) : renameBySAF(newPath);
        }
        return super.renameTo(newPath);
        }


    private boolean renameBySAF(File newPath){
            if (getParent().compareTo(newPath.getParent()) == 0)
                return mDocumentFile.renameTo(newPath.getName());
        else {
            try {
                return copyFile(newPath)? mDocumentFile.delete() : false;
            } catch (IOException e) {
                return false;
            } catch (InsufficientStorageException e){
                e.printStackTrace();
                return false;
            }
        }
        }

    private boolean copyFile(File newPath) throws IOException {
        FileUtility.checkUsableSpace(length(), newPath);
        DocumentFile parent = SafOperationUtility.getInstance().getDocFileFromPath(newPath.getParent());
        if (parent == null)
            return false;
        if (isDirectory())
            return parent.createDirectory(newPath.getName()) != null;
        else {
            DocumentFile dstDocFile = parent.createFile("*/*", newPath.getName());
            final int bufLen = this.length() > 65536 ? 65536 : 8192;
            byte[] buffer = new byte[bufLen];
            int rd;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(this));
            BufferedOutputStream bos = SafOperationUtility.getInstance().getDocFileOutputStream(dstDocFile);
            try {
                while ((rd = bis.read(buffer, 0, bufLen)) != -1) {
                    bos.write(buffer, 0, rd);
                }
                bos.flush();
            } finally {
                bis.close();
                bos.close();
                SafOperationUtility.getInstance().closeParcelFile();
            }
            return true;
        }
    }

    @Override
    public boolean delete() {
        if (SafOperationUtility.getInstance().isNeedToWriteSdBySaf(this.getAbsolutePath())) {
            DocumentFile documentFile = getDocumentFile();
            return documentFile == null? super.delete() : documentFile.delete();
        }
        return super.delete();
        }

    private DocumentFile getDocumentFile() {
        return mDocumentFile == null?
                (mDocumentFile = SafOperationUtility.getInstance().getDocFileFromPath(this.getAbsolutePath()))
                : mDocumentFile;
    }

    public LocalVFile[] listVFiles() {
        if (mHasRestrictFiles) {
            if (mRestrictFiles.size() != 0) {
                LocalVFile[] vFiles = new LocalVFile[mRestrictFiles.size()];
                mRestrictFiles.toArray(vFiles);
                return vFiles;
            } else {
                return null;
            }
        } else {
            LocalVFile[] result = null;
            String[] filenames = list();
            if (filenames == null) {
                return null;
            }
            int count = filenames.length;
            result = new LocalVFile[count];
            for (int i = 0; i < count; ++i) {
                result[i] = new LocalVFile(this, filenames[i]);
            }
            return result;
        }
    }

    public LocalVFile getParentFile(File parentFile) {
        LocalVFile vfile = null ;
        if (parentFile != null) {
            vfile = new LocalVFile(parentFile);
        }
        return vfile;
    }

}