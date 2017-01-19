package com.asus.filemanager.functionaldirectory;

import android.os.Handler;
import android.os.Message;

import com.asus.filemanager.dialog.MoveDstExistDialogFragment;
import com.asus.filemanager.dialog.MoveDstExistDialogFragment.Action;
import com.asus.filemanager.editor.EditResult;
import com.asus.filemanager.editor.Mutex;
import com.asus.filemanager.utility.InsufficientStorageException;
import com.asus.filemanager.utility.VFile;

/**
 * Created by Yenju_Lai on 2016/3/4.
 */
public class MoveFileTask {
    private static MoveFileTask mockInstance;
    public static final int MSG_SHOW_MOVE_DST_EXIST_FRAGMENT = 2048;

    static void setMockHelper(MoveFileTask mockDeleteFileTask) {
        mockInstance = mockDeleteFileTask;
    }

    public static MoveFileTask createTask(Destination destination) {
        if (mockInstance != null) return mockInstance;
        return new MoveFileTask(destination, FunctionalDirectoryUtility.getInstance());
    }

    public static MoveFileTask createTask(Destination destination, Handler handler) {
        if (mockInstance != null) return mockInstance;
        return new MoveFileTask(destination, FunctionalDirectoryUtility.getInstance(), handler);
    }

    public EditResult moveFiles(VFile[] src, boolean isNeedToWaitMediaScanner) {
        try {
            return moveVFiles(src, null, isNeedToWaitMediaScanner);
        } catch (InsufficientStorageException e) {
            e.printStackTrace();
            return createEditResultWithECode(EditResult.E_NOSPC);
        }
    }

    public enum Destination {
        ORIGINAL_PATH, HIDDEN_ZONE, RECYCLE_BIN
    }

    private final Destination destination;
    private FunctionalDirectoryUtility functionalDirectoryUtility;
    private final Handler handler;
    private Action appliedAction = null;

    MoveFileTask(Destination destination, FunctionalDirectoryUtility functionalDirectoryUtility) {
        this(destination, functionalDirectoryUtility, null);
    }

    MoveFileTask(Destination destination, FunctionalDirectoryUtility functionalDirectoryUtility, Handler handler) {
        this.handler = handler;
        this.destination = destination;
        this.functionalDirectoryUtility = functionalDirectoryUtility;
    }

    protected EditResult moveVFiles(VFile[] files, final Movable targetFile, boolean isNeedToWaitMediaScanner) {
        if (files == null || files.length == 0) {
            return createEditResultWithECode(EditResult.E_FAILURE);
        }
        EditResult result = createEditResultWithECode(EditResult.E_SUCCESS);
        for (VFile file : files) {
            if (file == null) continue;
            if (file instanceof DisplayVirtualFile)
                file = ((DisplayVirtualFile) file).getActualFile();
            if (targetFile == null && !functionalDirectoryUtility.permissionGranted(file)) {
                return createEditResultWithECode(EditResult.E_PERMISSION);
            }
            if (file.isDirectory()) {
                result =  moveDirectory(file, functionalDirectoryUtility.createMovableFile(destination, file, targetFile), isNeedToWaitMediaScanner);
            }
            else {
                result =  moveSingleFile(functionalDirectoryUtility.createMovableFile(destination, file, targetFile), isNeedToWaitMediaScanner);
            }
            if (result.ECODE != result.E_SUCCESS)
                return result;
        }
        return result;
    }

    private EditResult moveDirectory(VFile file, Movable targetFile, boolean isNeedToWaitMediaScanner) {
        if (file.listVFiles() == null || file.listVFiles().length == 0) {
            return moveSingleFile(targetFile, isNeedToWaitMediaScanner);
        } else {
            EditResult result = moveVFiles(file.listVFiles(), targetFile, isNeedToWaitMediaScanner);
            if (result.ECODE != EditResult.E_SUCCESS) {
                return result;
            }
            if (file.listFiles() == null || file.listFiles().length == 0)
                return deleteFile(file, isNeedToWaitMediaScanner);
            else
                return result;
        }
    }

    private EditResult moveSingleFile(Movable targetFile, boolean isNeedToWaitMediaScanner) {
        if (handler != null && targetFile.destinationExist() && appliedAction == null) {
            MoveDstExistDialogFragment fragment = MoveDstExistDialogFragment.newInstance(targetFile);
            Message msg = new Message();
            msg.what = MSG_SHOW_MOVE_DST_EXIST_FRAGMENT;
            msg.obj = fragment;
            handler.sendMessage(msg);
            Mutex.Lock();
            updateApplyAction(fragment);
            if (fragment.getSelectedAction() == MoveDstExistDialogFragment.Action.KEEP)
                return createEditResultWithECode(EditResult.E_SUCCESS);
            else if (fragment.getSelectedAction() == Action.CANCEL)
                return createEditResultWithECode(EditResult.E_FAILURE);
        }
        if (appliedAction == MoveDstExistDialogFragment.Action.KEEP)
            return createEditResultWithECode(EditResult.E_SUCCESS);
        EditResult result = new EditResult();
        result.ECODE = targetFile.move()? EditResult.E_SUCCESS : EditResult.E_FAILURE;
            functionalDirectoryUtility.scanFile(targetFile, isNeedToWaitMediaScanner);
        return result;
    }

    private void updateApplyAction(MoveDstExistDialogFragment fragment) {
        if (fragment.isAlwaysApply()) {
            appliedAction = fragment.getSelectedAction();
        }
    }

    private EditResult deleteFile(VFile file, boolean isNeedToWaitMediaScanner) {
        EditResult result = new EditResult();
        result.ECODE = file.delete()? EditResult.E_SUCCESS : EditResult.E_FAILURE;
        if (result.ECODE == EditResult.E_SUCCESS)
            functionalDirectoryUtility.scanFile(file, isNeedToWaitMediaScanner);
        return result;
    }

    private EditResult createEditResultWithECode(int code) {
        EditResult result = new EditResult();
        result.ECODE = code;
        return result;
    }
}
