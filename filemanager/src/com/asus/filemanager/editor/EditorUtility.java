
package com.asus.filemanager.editor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.VolumeStateObserver;
import com.asus.filemanager.ga.GaFailCaseCollection;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.functionaldirectory.DisplayVirtualFile;
import com.asus.filemanager.functionaldirectory.MoveFileTask;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FileUtility.FileInfo;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.Utility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.RemoteActionEntry;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CopyArgument;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class EditorUtility {

    private static final String TAG = "EditorUtility";
    private static int folder_depth = 0;
    public static boolean sQuit = false;
    public static boolean sOverWrite = false;
    public static boolean sApplyAll = false;
    public static boolean sEditIsProcessing = false;

    public enum RequestFrom {
        FileList,
        PhotoViewer,
        DuplicateFiles,
        StorageAnalyzer
    }

    public static class ExistPair {

        private File mOld;
        private File mNew;

        public ExistPair(File oldfile, File newfile) {
            mOld = oldfile;
            mNew = newfile;
        }

        public File getOldFile() {
            return mOld;
        }

        public File getNewFile() {
            return mNew;
        }
    }

    public static boolean isSpecialChar(String src) {
        //if (src.isEmpty() || src.startsWith(" ") || src.startsWith("."))
        if (src.isEmpty() || src.startsWith(" "))
            return true;

        String temp = src.replace('/', '*')
                .replace('\\', '*')
                .replace('|', '*')
                .replace('\n', '*')
                .replace(':', '*')
                .replace('?', '*')
                .replace('"', '*')
                .replace('<', '*')
                .replace('>', '*');
        if (temp.indexOf("*") == -1)
            return false;
        else
            return true;
    }

    public static boolean isNameTooLong(String src) {
        return src.getBytes().length > 255;
    }

    // Delete Action +++
    public static boolean deleteFile(Context context, VFile[] files, boolean isNeedToWaitMediaScanner) {

        EditResult result = doDeleteFiles(files, isNeedToWaitMediaScanner);
        boolean isDeleteSuccessfully = false;

        if (result.ECODE == EditResult.E_PERMISSION) {
            ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
        } else if (result.ECODE == EditResult.E_FAILURE) {
            ToastUtility.show(context, R.string.delete_fail, Toast.LENGTH_LONG);
        } else {
            if (files != null)
            {
                if (files.length == 1)
                    ToastUtility.show(context, R.string.delete_success_one, files[0].getName());
                else
                    ToastUtility.show(context, R.string.delete_success_more, Toast.LENGTH_LONG);
            }
            isDeleteSuccessfully = true;
        }
        if (!isDeleteSuccessfully) {
            GaFailCaseCollection.getInstance().sendDeleteFailEvents(
                    context, result, files);
        }
        return isDeleteSuccessfully;
    }

    public static boolean moveFileToRecycleBin(Context context, VFile[] files, boolean isNeedToWaitMediaScanner) {
        EditResult result = MoveFileTask.createTask(MoveFileTask.Destination.RECYCLE_BIN).moveFiles(files, isNeedToWaitMediaScanner);

        if (result.ECODE == EditResult.E_PERMISSION) {
            ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
            return false;
        } else if (result.ECODE == EditResult.E_FAILURE) {
            ToastUtility.show(context, R.string.move_to_trash_fail, Toast.LENGTH_LONG);
            return false;
        } else if (result.ECODE == EditResult.E_NOSPC) {
            ToastUtility.show(context, R.string.no_space_fail, Toast.LENGTH_LONG);
            return false;
        } else {
            if (files != null)
            {
                if (files.length == 1)
                    ToastUtility.show(context, R.string.move_to_trash_success_one, files[0].getName());
                else
                    ToastUtility.show(context, R.string.move_to_trash_success_more, Toast.LENGTH_LONG);
            }
            return true;
        }
    }

    public static boolean moveToHiddenZone(Context context, VFile[] files, boolean isNeedToWaitMediaScanner) {
        EditResult result = MoveFileTask.createTask(MoveFileTask.Destination.HIDDEN_ZONE).moveFiles(files, isNeedToWaitMediaScanner);

        if (result.ECODE == EditResult.E_PERMISSION) {
            ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
            return false;
        } else if (result.ECODE == EditResult.E_FAILURE) {
            ToastUtility.show(context, R.string.move_to_hidden_zone_fail, Toast.LENGTH_LONG);
            return false;
        } else if (result.ECODE == EditResult.E_NOSPC) {
            ToastUtility.show(context, R.string.no_space_fail, Toast.LENGTH_LONG);
            return false;
        } else {
            if (files != null)
            {
                if (files.length == 1)
                    ToastUtility.show(context, R.string.move_to_hidden_zone_success_one, files[0].getName());
                else
                    ToastUtility.show(context, R.string.move_to_hidden_zone_success_more, Toast.LENGTH_LONG);
            }
            return true;
        }
    }

    public static boolean restoreFile(Context context, VFile[] files, Handler handler, boolean isNeedToWaitMediaScanner) {
        EditResult result = MoveFileTask.createTask(MoveFileTask.Destination.ORIGINAL_PATH, handler).moveFiles(files, isNeedToWaitMediaScanner);

        if (result.ECODE == EditResult.E_PERMISSION) {
            ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
            return false;
        } else if (result.ECODE == EditResult.E_FAILURE) {
            ToastUtility.show(context, R.string.restore_fail, Toast.LENGTH_LONG);
            return false;
        } else if (result.ECODE == EditResult.E_NOSPC) {
            ToastUtility.show(context, R.string.no_space_fail, Toast.LENGTH_LONG);
            return false;
        } else {
            if (files != null)
            {
                if (files.length == 1)
                    ToastUtility.show(context, R.string.restore_success_one, files[0].getName());
                else
                    ToastUtility.show(context, R.string.restore_success_more, Toast.LENGTH_LONG);
            }
            return true;
        }
    }

    public static EditResult doDeleteFiles(VFile[] src, boolean isNeedToWaitMediaScanner)
    {
        EditResult r = new EditResult();
        if (src == null) {
            r.ECODE = EditResult.E_FAILURE;
            return r;
        }
        folder_depth = 0;
        for (int i = 0; i < src.length; i++) {

            if (src[i] == null)
                continue;

            FileInfo info = FileUtility.getInfo(src[i]);
            if (!info.PermissionWrite) {
                r.ECODE = EditResult.E_PERMISSION;
                return r;
            } else {
                EditResult tr = doDeleteFile(src[i], isNeedToWaitMediaScanner);
                r.ECODE = tr.ECODE;
                r.numFiles += tr.numFiles;
                r.numFolders += tr.numFolders;
                if (r.ECODE != EditResult.E_SUCCESS)
                    return r;
            }
        }
        return r;
    }

    public static EditResult doDeleteFile(VFile f, boolean isNeedToWaitMediaScanner)
    {
        EditResult r = new EditResult();

        if (f == null || folder_depth > FileUtility.MAX_FOLDER_DEPTH) {
            r.ECODE = EditResult.E_FAILURE;
            return r;
        }
        if (f instanceof DisplayVirtualFile)
            f = ((DisplayVirtualFile) f).getActualFile();

        boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(f.getAbsolutePath());
        DocumentFile targetFile = null;

        if(useSaf){
            targetFile = SafOperationUtility.getInstance().getDocFileFromPath(f.getAbsolutePath());
        }

        if (f.isDirectory()) {
            if(useSaf){
                if (targetFile!=null){

                    if (f.getHasRetrictFiles()) {
                        VFile[] childs = f.listVFiles();
                        if (childs != null) {
                            for (int i = 0; i < childs.length && childs[i] != null; i++) {
                                EditResult tr = doDeleteFile(childs[i], isNeedToWaitMediaScanner);
                                r.ECODE = tr.ECODE;

                                if (r.ECODE == EditResult.E_SUCCESS) {
                                    f.removeFromRestrictFiles(childs[i]);
                                } else {
                                    break;
                                }
                            }
                            if (f.getRestrictFiles().size() != 0) {
                                r.ECODE = EditResult.E_FAILURE;
                            } else if (f.list() != null && f.list().length == 0) {
                                r.ECODE = targetFile.delete() ? EditResult.E_SUCCESS : EditResult.E_FAILURE;
                            }
                        }
                    } else {
                        r.ECODE = targetFile.delete() ? EditResult.E_SUCCESS : EditResult.E_FAILURE;
                    }
                }else {
                    r.ECODE = EditResult.E_FAILURE;
                }
            }else{

            folder_depth++;

            VFile[] childs = f.listVFiles();

            if (childs != null) {
                for (int i = 0; i < childs.length && childs[i] != null; i++) {
                    EditResult tr = doDeleteFile(childs[i], isNeedToWaitMediaScanner);
                    r.ECODE = tr.ECODE;
                    r.numFiles += tr.numFiles;
                    r.numFolders += tr.numFolders;
                    if (r.ECODE == EditResult.E_SUCCESS) {
                        f.removeFromRestrictFiles(childs[i]);
                    } else {
                        break;
                    }
                }
            }
            if (r.ECODE == EditResult.E_SUCCESS) {
                if (f.delete()) {
                    MediaProviderAsyncHelper.deleteFile(f, isNeedToWaitMediaScanner);
                    r.numFolders++;
                } else if (f.getHasRetrictFiles()) {
                    if (f.getRestrictFiles().size() != 0) {
                        // there still has some restrict files
                        // doesn't be deleted successfully.
                        r.ECODE = EditResult.E_FAILURE;
                    }
                } else {
                    r.ECODE = EditResult.E_FAILURE;
                }
            }
            folder_depth--;

            }
        } else {
            if(useSaf){
                if(targetFile != null){
                    if(targetFile.delete()){
                        MediaProviderAsyncHelper.deleteFile(f, isNeedToWaitMediaScanner);
                        r.numFiles++;
                    }else{
                        r.ECODE = EditResult.E_FAILURE;
                    }
                }else{
                    r.ECODE = EditResult.E_FAILURE;
                }
            }else{
                if (f.delete()) {
                    MediaProviderAsyncHelper.deleteFile(f, isNeedToWaitMediaScanner);
                    r.numFiles++;
                } else {
                    r.ECODE = EditResult.E_FAILURE;
                }
            }
        }
        return r;
    }

    // Deletet Action ---

    // Paste Action +++

    public static boolean pasteFile(FileManagerApplication context, VFile[] files, Handler handler, boolean isDelete,
                                    boolean isDraggingItems, final VFile pasteVFile, boolean isNeedToWaitMediaScanner) {

        Observer volumeStateObserver = new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                String event = ((Bundle)o).getString(VolumeStateObserver.KEY_EVENT);
                if (event.compareTo(Intent.ACTION_MEDIA_MOUNTED) == 0)
                    return;
                String path = ((Bundle)o).getString(VolumeStateObserver.KEY_PATH);
                boolean dstNotExist = pasteVFile.getVFieType() == VFileType.TYPE_LOCAL_STORAGE && !pasteVFile.exists();
                boolean dstNotMounted = path != null && pasteVFile.getAbsolutePath().startsWith(path);
                if (dstNotExist || dstNotMounted) {
                    Log.d(TAG , "dst not mounted, cancel paste");
                    EditorAsyncHelper.setPasteFileTerminate();
                    EditorUtility.sEditIsProcessing = false;
                }
            }
        };
        context.mVolumeStateObserver.addObserver(volumeStateObserver);
        EditResult result = new EditResult();

        result = doPaste2Path(context, files, handler, isDelete, pasteVFile, isNeedToWaitMediaScanner);

        showPasteFileResult(context, result.ECODE, null, isDraggingItems, isDelete);

        // when user press the cancel button to give up current running task.
        // we don't consider it is a fail case.
        if (result.ECODE != EditResult.E_SUCCESS
                && (sQuit == false)) {
            GaFailCaseCollection.getInstance().sendCopyOrMoveToFailEvents(
                    context, result, files, pasteVFile, isDelete);
        }
        context.mVolumeStateObserver.deleteObserver(volumeStateObserver);
        return (result.ECODE == EditResult.E_SUCCESS);
    }
    private static boolean isDeviceToOtherDevice(RemoteVFile srcFile,RemoteVFile destFile){
        if (srcFile.getStorageType() == StorageType.TYPE_HOME_CLOUD && destFile.getStorageType() == StorageType.TYPE_HOME_CLOUD ) {
            if (!srcFile.getmDeviceId().equals(destFile.getmDeviceId())) {
                return true;
            }
        }
        return false;
    }
    private static boolean isCloudToOtherGoogleCloud(RemoteVFile srcFile,RemoteVFile destFile){
        if (srcFile.getStorageType() == StorageType.TYPE_GOOGLE_DRIVE && destFile.getStorageType() == StorageType.TYPE_GOOGLE_DRIVE ) {
            if (!srcFile.getStorageName().equals(destFile.getStorageName())) {
                return true;
            }
        }
        return false;
    }
    private static EditResult doPaste2Path(Context context, VFile[] src, Handler h,
            boolean deleteSrc, VFile pasteVFile, boolean isNeedToWaitMediaScanner) {
        int targetDataType = pasteVFile.getVFieType();
        String dstfolder = pasteVFile.getAbsolutePath();
        EditResult result = new EditResult();
        if (src == null || dstfolder == null) {
            result.ECODE = EditResult.E_FAILURE;
            return result;
        }

        // now only consider local storage permission and remote storage always can write
        boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(dstfolder);
        boolean bDstFolderCanWrite = true;


        if (useSaf){
            DocumentFile dstFolder = SafOperationUtility.getInstance().getDocFileFromPath(dstfolder);
            if (null == dstFolder || !dstFolder.canWrite())
                bDstFolderCanWrite = false;
        }else{
            bDstFolderCanWrite = (targetDataType == VFileType.TYPE_LOCAL_STORAGE ? new LocalVFile(dstfolder).canWrite() : true);
        }
        if (!bDstFolderCanWrite) {
            result.ECODE = EditResult.E_PERMISSION;
            if (src.length>0&&src[0].getVFieType() == VFileType.TYPE_CLOUD_STORAGE || src[0].getVFieType()==VFileType.TYPE_SAMBA_STORAGE) {
                h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_REMOTE_FAIL_CLOSE_DIALOG, result));
            }
            return result;
        }

        // remote storage copy case
        int srcVfileType = src[0].getVFieType();
        if (targetDataType == VFileType.TYPE_CLOUD_STORAGE && srcVfileType == VFileType.TYPE_CLOUD_STORAGE) {
            int remoteType1 = ((RemoteVFile)src[0]).getStorageType();
            int remoteType2 = ((RemoteVFile)pasteVFile).getStorageType();
            Log.v(TAG, "copy from remote: " + remoteType1 + " to remote2: " + remoteType2);
            if (remoteType1 == remoteType2) {
                String action = (deleteSrc ? CopyArgument.Move : CopyArgument.Copy);
                RemoteActionEntry remoteActionEntry = new RemoteActionEntry(src, pasteVFile, action, true);
                if (isDeviceToOtherDevice((RemoteVFile)src[0], (RemoteVFile)pasteVFile)) {
                    h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_DEVICE_TO_OTHER_DEVICE, remoteActionEntry));
                }else if (isCloudToOtherGoogleCloud((RemoteVFile)src[0], (RemoteVFile)pasteVFile)) {
                    h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_REMOTE_TO_OTHER_REMOTE, remoteActionEntry));
                }else {
                    h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_REMOTE_TO_REMOTE, remoteActionEntry));
                }
            } else {
                //  fixed by felix_zhang
                 String action = (deleteSrc ? CopyArgument.Move : CopyArgument.Copy);
                 RemoteActionEntry remoteActionEntry = new RemoteActionEntry(src, pasteVFile, action, true);
                 h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_REMOTE_TO_OTHER_REMOTE, remoteActionEntry));
            }

            result.ECODE = EditResult.E_REMOTEACTION;
            return result;
        } else if (targetDataType != VFileType.TYPE_LOCAL_STORAGE || (srcVfileType != VFileType.TYPE_LOCAL_STORAGE)) {
            String action = (deleteSrc ? CopyArgument.Move : CopyArgument.Copy);

            // AZS support copy/move retrict files from version code: 2240
            boolean isAzsEnable = WrapEnvironment.isAZSEnable(context);
            boolean isSupportRetrictFiles = isAzsEnable ?
                    (Utility.getPackageVersionCode(context, "com.asus.server.azs") > 2240) : true;

            RemoteActionEntry remoteActionEntry = new RemoteActionEntry(src, pasteVFile, action, isSupportRetrictFiles);
            if (src != null && src.length > 0 && src[0].getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
                for (int i = 0; i < src.length && src[i] != null; i++) {
                    FileInfo info = FileUtility.getInfo(src[i]);
                    if (!info.PermissionRead) {
                        result.ECODE = EditResult.E_PERMISSION;
                        return result;
                    }
                }
                h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_LOCAL_TO_REMOTE, remoteActionEntry));
            }
            else {
                if(targetDataType == VFileType.TYPE_CLOUD_STORAGE && srcVfileType == VFileType.TYPE_SAMBA_STORAGE){
                    //  samb to cloud
                    h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_SAMB_TO_CLOUD, remoteActionEntry));
                }else if(targetDataType == VFileType.TYPE_SAMBA_STORAGE && srcVfileType == VFileType.TYPE_CLOUD_STORAGE ){
                    // cloud to samb
                    h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_CLOUD_TO_SAMB, remoteActionEntry));
                }else {
                    //remote to local
                    h.sendMessage(h.obtainMessage(FileListFragment.MSG_COPY_REMOTE_TO_LOCAL, remoteActionEntry));
                }
            }

            result.ECODE = EditResult.E_REMOTEACTION;
            return result;
        }

        // Do nothing and return success when cut-file and paste-dst is in the
        // same folder
        if (deleteSrc) {
            try {
                String dstFileCanonicalPath = new File(dstfolder).getCanonicalPath();
                String srcfParentCanonicalPath = src[0].getParentFile().getCanonicalPath();

                if (dstFileCanonicalPath.equals(srcfParentCanonicalPath)) {
                    result.ECODE = EditResult.E_SUCCESS;
                    return result;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        folder_depth = 0;
        for (int i = 0; i < src.length && src[i] != null; i++) {
            if (src[i].getVFieType() == VFileType.TYPE_LOCAL_STORAGE && !src[i].exists()) {
                result.ECODE = EditResult.E_FAILURE;
                return result;
            }

            FileInfo info = FileUtility.getInfo(src[i]);
            /*
            if (!info.PermissionRead) {
                result.ECODE = EditResult.E_PERMISSION;
                return result;
            } else
            */
            if (IsSourceIncludeTarget(src[i], pasteVFile)) {
                result.ECODE = EditResult.E_FAILURE;
                return result;
            } else {
                EditResult tr = doPaste2Path(src[i], dstfolder, h, deleteSrc, targetDataType, isNeedToWaitMediaScanner);
                result.ECODE = tr.ECODE;
                result.numbytes += tr.numbytes;
                result.numFiles += tr.numFiles;
                result.numFolders += tr.numFolders;
                if (result.ECODE != EditResult.E_SUCCESS){
                    if (!info.PermissionRead)
                        result.ECODE = EditResult.E_PERMISSION;
                    return result;
                }
            }
        }

        return result;
    }

    private static EditResult doPaste2Path(VFile f, String dstfolder, Handler h, boolean deleteSrc,
            int targetDataType, boolean isNeedToWaitMediaScanner) {
        EditResult result = new EditResult();
        if (sQuit || f == null || dstfolder == null || folder_depth > FileUtility.MAX_FOLDER_DEPTH) {
            result.ECODE = EditResult.E_FAILURE;
            return result;
        }

        if (f.isDirectory()) {
            folder_depth++;
            result = doPasteFolder2Path(f, dstfolder, h, deleteSrc, targetDataType, isNeedToWaitMediaScanner);
            folder_depth--;
            return result;
        } else
            return doPasteFile2Path(f, dstfolder, h, deleteSrc, targetDataType, isNeedToWaitMediaScanner);
    }

    private static EditResult doPasteFolder2Path(VFile f, String dstfolder, Handler h, boolean deleteSrc,
            int targetDataType, boolean isNeedToWaitMediaScanner) {
        boolean folderExisted = false;
        EditResult r = new EditResult();
        VFile dst = (targetDataType == VFileType.TYPE_LOCAL_STORAGE ?
                new LocalVFile(dstfolder + "/" + f.getName()) : new RemoteVFile(dstfolder + "/" + f.getName()));
        boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(dstfolder);
        if (dst.exists() && dst.isDirectory())
            folderExisted = true;
        if (!sApplyAll && folderExisted) {
            if (h != null) {
                sOverWrite = true;
            } else {
                r.ECODE = EditResult.E_EXIST;
                return r;
            }
        }
        boolean b = false;
        if (folderExisted) {
            if (sOverWrite)
                b = true;
            else
                return r;
        } else {
            if(useSaf){
                DocumentFile destparentFile = SafOperationUtility.getInstance().getDocFileFromPath(dstfolder);
                if(destparentFile != null){
                    b = destparentFile.createDirectory(f.getName()) == null ? false : true;
                }
            }else{
                b = dst.mkdir();
            }
        }

        if (b) {
            MediaProviderAsyncHelper.addFile(dst, isNeedToWaitMediaScanner);
            r.numFolders++;

            VFile[] childs = f.listVFiles();

            if (childs != null) {
                for (int i = 0; i < childs.length; i++) {

                    if (childs[i] == null)
                        continue;

                    EditResult tr = doPaste2Path(childs[i], dst.getAbsolutePath(), h,
                            deleteSrc, targetDataType, isNeedToWaitMediaScanner);

                    r.ECODE = tr.ECODE;
                    r.numbytes += tr.numbytes;
                    r.numFiles += tr.numFiles;
                    r.numFolders += tr.numFolders;
                    if (r.ECODE == EditResult.E_SUCCESS) {
                        f.removeFromRestrictFiles(childs[i]);
                    }
                    if (r.ECODE != EditResult.E_SUCCESS)
                        return r;
                }
            }
            if (deleteSrc && r.ECODE == EditResult.E_SUCCESS) {
                EditResult tr = doDeleteFile(f, isNeedToWaitMediaScanner);
                if (tr.ECODE != EditResult.E_SUCCESS)
                    r.ECODE = tr.ECODE;
            }
            return r;
        } else {
            if (new File(dstfolder).getUsableSpace() < FileUtility.MIN_FOLDER_SIZE) {
                Log.w(TAG, "no space");
                r.ECODE = EditResult.E_NOSPC;
            } else {
                r.ECODE = EditResult.E_FAILURE;
            }
            return r;
        }
    }

    private static EditResult doPasteFile2Path(VFile f, String dstfolder, Handler h, boolean deleteSrc,
            int targetDataType, boolean isNeedToWaitMediaScanner)
    {
        boolean fileExisted = false;
        EditResult r = new EditResult();
        VFile dst = new LocalVFile(dstfolder + "/" + f.getName());
        DocumentFile destDocFile = null;
        boolean useSaf = SafOperationUtility.getInstance().isNeedToWriteSdBySaf(dstfolder);
        if (dst.exists() && dst.isFile())
            fileExisted = true;
        if (!sApplyAll && fileExisted) {
            if (h != null)
            {
                h.sendMessage(h.obtainMessage(FileListFragment.MSG_PASTE_PAUSE, 0, 0, new ExistPair(dst, f)));
                Mutex.Lock();
                if (sQuit)
                {
                    r.ECODE = EditResult.E_FAILURE;
                    return r;
                }
            } else {
                r.ECODE = EditResult.E_EXIST;
                return r;
            }
        }
        try {
            boolean toPass = false;
            boolean apply = false;
            if (fileExisted) {
                if (sOverWrite) {
                    apply = true;
                } else {
                    EditResult temp = new EditResult();
                    temp.numFiles = 1;
                    temp.numbytes = f.length();
                    if (h != null)
                        h.sendMessage(h.obtainMessage(FileListFragment.MSG_PASTE_PROG_FILE, 0, 0, temp));
                    return r;
                }
            }

            if (deleteSrc) {
                // source file path may come from media provider when user copy/cut file from category.
                // we need to change source file path to sdcard path.
                boolean isSameFile = FileUtility.changeToSdcardPath(
                        f.getAbsolutePath()).equalsIgnoreCase(dst.getAbsolutePath());
                if (sOverWrite && isSameFile) {
                    EditResult temp = new EditResult();
                    temp.numFiles = 1;
                    temp.numbytes = f.length();
                    if (h != null)
                        h.sendMessage(h.obtainMessage(FileListFragment.MSG_PASTE_PROG_FILE, 0, 0, temp));
                    return r;
                }
                toPass = f.renameTo(dst);
            }

            if (toPass) {
                r.numbytes = dst.length();
                r.numFiles = 1;
                if (h != null)
                    h.sendMessage(h.obtainMessage(FileListFragment.MSG_PASTE_PROG_FILE, 0, 0, r));
                MediaProviderAsyncHelper.rename(f, dst, isNeedToWaitMediaScanner);
                r.ECODE = EditResult.E_SUCCESS;
                return r;
            } else {
                if(useSaf){
                    DocumentFile destparentFile = SafOperationUtility.getInstance().getDocFileFromPath(dstfolder);
                    if (null == destparentFile){
                        r.ECODE = EditResult.E_FAILURE;
                        return r;
                    }
                    if(apply){
                        toPass = true;
                        if(!f.getAbsolutePath().equalsIgnoreCase(dst.getAbsolutePath())){
                            if(destparentFile.findFile(f.getName()).delete()){
                                destDocFile = destparentFile.createFile("*/*", f.getName());
                                toPass = destDocFile != null ? true : false;
                            }
                        }
                    }else{
                        destDocFile = destparentFile.createFile("*/*", f.getName());
                        if(destDocFile != null){
                            toPass = true;
                        }
                    }
                }else{
                    if (apply) {
                        toPass = true;
                    } else {
                        toPass = dst.createNewFile();
                    }
                }

            }

            if (toPass)
            {
                // source file path may come from media provider when user copy/cut file from category.
                // we need to change source file path to sdcard path.
                boolean isSameFile = FileUtility.changeToSdcardPath(
                        f.getAbsolutePath()).equalsIgnoreCase(dst.getAbsolutePath());
                if (sOverWrite && isSameFile) {
                    EditResult temp = new EditResult();
                    temp.numFiles = 1;
                    temp.numbytes = f.length();
                    if (h != null)
                        h.sendMessage(h.obtainMessage(FileListFragment.MSG_PASTE_PROG_FILE, 0, 0, temp));
                    return r;
                }

                if (f.length() > (dst.getUsableSpace() + dst.length())) {
                    Log.d(TAG, "file size(" + f.length() + ") > usable space(" + (dst.getUsableSpace() + dst.length()) + ")");
                    throw new IOException(EditResult.Error.ENOSPC);
                }

                if (targetDataType == VFileType.TYPE_REMOTE_STORAGE) {
                    Log.d(TAG, "do doPasteFile2Path, file type is remote, we don't do anything");
                } else {

                    r.mCurrentFileName = f.getName();

                    final int buflen = f.length() > 65536 ? 65536 : 8192;
                    byte[] buffer = new byte[buflen];
                    int rd = -1;
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
                    BufferedOutputStream bos;
                    if(useSaf){
                        bos = SafOperationUtility.getInstance().getDocFileOutputStream(destDocFile);
                    }else{
                        bos = new BufferedOutputStream(new FileOutputStream(dst));
                    }
                    try {
                        rd = bis.read(buffer, 0, buflen);
                        while (rd != -1)
                        {
                            bos.write(buffer, 0, rd);
                            r.numbytes += rd;
                            if (h != null)
                                h.sendMessage(h.obtainMessage(FileListFragment.MSG_PASTE_PROG_SIZE, 0, 0, r));
                            if (sQuit)
                            {
                                EditResult tr = doDeleteFile(new VFile(dst), isNeedToWaitMediaScanner);
                                r.ECODE = EditResult.E_FAILURE;
                                break;
                            }
                            rd = -1;
                            rd = bis.read(buffer, 0, buflen);
                        }
                        bos.flush();
                    } finally {
                        if(bis != null) bis.close();
                        if(bos != null) bos.close();
                        if(useSaf){
                            SafOperationUtility.getInstance().closeParcelFile();
                        }
                        FileUtility.sync();
                    }
                }

                r.numFiles++;
                if (h != null)
                    h.sendMessage(h.obtainMessage(FileListFragment.MSG_PASTE_PROG_FILE, 0, 0, r));
                if(r.ECODE == EditResult.E_SUCCESS)
                    MediaProviderAsyncHelper.addFile(new VFile(dst), isNeedToWaitMediaScanner);
                if (deleteSrc && r.ECODE == EditResult.E_SUCCESS)
                {
                    EditResult tr = doDeleteFile(new VFile(f), isNeedToWaitMediaScanner);
                    if (tr.ECODE != EditResult.E_SUCCESS)
                        r.ECODE = tr.ECODE;
                }
                return r;
            }
            else
            {
                r.ECODE = EditResult.E_FAILURE;
                return r;
            }
        } catch (IOException e) {
            if (e.getMessage().equals(EditResult.Error.ENOSPC)) {
                Log.w(TAG, "no space");
                r.ECODE = EditResult.E_NOSPC;
            } else {
                e.printStackTrace();
                r.ECODE = EditResult.E_FAILURE;
            }

            if (!sOverWrite)
                doDeleteFile(new VFile(dst), isNeedToWaitMediaScanner);
            return r;
        }
    }

    /*public static boolean IsSourceIncludeTarget(String src, String dst) {
        return dst.startsWith(src);
    }*/
    public static boolean IsSourceIncludeTarget(VFile src, VFile dst) {
        String srcpath = null;
        String dstPath = null;

        try {
            srcpath = FileUtility.getCanonicalPath(src).toLowerCase() + File.separator;
            dstPath = FileUtility.getCanonicalPath(dst).toLowerCase() + File.separator;
        } catch (IOException e) {
            srcpath = src.getAbsolutePath().toLowerCase() + File.separator;
            dstPath = dst.getAbsolutePath().toLowerCase() + File.separator;
        }
        return dstPath.startsWith(srcpath);
    }

    private static void showPasteFileResult(Context context, int result, String folder, boolean isDraggingItems, boolean isDelete) {
        switch (result) {
            case EditResult.E_EXIST:
                ToastUtility.show(context, R.string.target_exist, Toast.LENGTH_LONG);
                break;
            case EditResult.E_FAILURE:
                ToastUtility.show(context, R.string.paste_fail, Toast.LENGTH_LONG);
                break;
            case EditResult.E_PERMISSION:
                ToastUtility.show(context, R.string.permission_deny, Toast.LENGTH_LONG);
                break;
            case EditResult.E_SUCCESS:
                if (isDraggingItems) {
                    if (isDelete) {
                        ToastUtility.show(context, R.string.toast_drag_moved_items, Toast.LENGTH_LONG);
                    } else {
                        ToastUtility.show(context, R.string.toast_drag_copied_items, Toast.LENGTH_LONG);
                    }
                }
                if (folder != null) {
                    ToastUtility.show(context, R.string.paste_success, Toast.LENGTH_LONG, folder);
                }
                break;
            case EditResult.E_NOSPC:
                ToastUtility.show(context, R.string.no_space_fail, Toast.LENGTH_LONG);
                break;
        }
    }
    // Paste Action ---
}
