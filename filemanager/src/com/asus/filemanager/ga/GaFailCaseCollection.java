package com.asus.filemanager.ga;

import android.content.Context;

import com.asus.filemanager.editor.EditResult;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.utility.VFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GaFailCaseCollection extends GoogleAnalyticsBase {

    // v1: first release
    // v2: do not consider it is a fail case when user press cancel button to
    //     cancel the pasting task.
    public static final String CATEGORY_NAME = "fail_case_collection_v2";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-25";

    public static final String KEY_ID = "GA_FAIL_CASE_COLLECTION_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_FAIL_CASE_COLLECTION_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_FAIL_CASE_COLLECTION_SAMPLE_RATE";

    public static final String ACTION_COPY_TO_FAIL = "copy_to_fail";
    public static final String ACTION_MOVE_TO_FAIL = "move_to_fail";
    public static final String ACTION_DELETE_FAIL = "delete_fail";

    private static GaFailCaseCollection mInstance;

    public static GaFailCaseCollection getInstance() {
        if (mInstance == null) {
            mInstance = new GaFailCaseCollection();
        }

        return mInstance;
    }

    private GaFailCaseCollection() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendCopyOrMoveToFailEvents(Context context, EditResult result,
            VFile[] srcFiles, VFile dstFile, boolean isMoveTo) {
        if (!isValidData(srcFiles) || !isValidData(dstFile)) {
            return;
        }

        String reason = getReasonMessage(result);

        List<String> srcNeedSafList = createNeedToWriteSdBySafList(srcFiles);
        boolean srcNeedSaf = srcNeedSafList.size() != 0 ? true : false;
        removeItemIfSafIsObtained(srcNeedSafList);
        boolean srcSafIsObtained = srcNeedSafList.size() == 0 ? true : false;
        String srcMessage = String.format("src (%s)", generateSafMessage(srcNeedSaf, srcSafIsObtained));

        List<String> dstNeedSafList = createNeedToWriteSdBySafList(new VFile[] {dstFile});
        boolean dstNeedSaf = dstNeedSafList.size() != 0 ? true : false;
        removeItemIfSafIsObtained(dstNeedSafList);
        boolean dstSafIsObtained = dstNeedSafList.size() == 0 ? true : false;
        String dstMessage = String.format("dst (%s)", generateSafMessage(dstNeedSaf, dstSafIsObtained));

        String message = reason + ", " + srcMessage + ", " + dstMessage;
        super.sendEvents(context, CATEGORY_NAME, isMoveTo ? ACTION_MOVE_TO_FAIL : ACTION_COPY_TO_FAIL,
                message, srcFiles[0].getHasRetrictFiles() ? 1L : 0L);
    }

    public void sendDeleteFailEvents(Context context, EditResult result, VFile[] srcFiles) {
        if (!isValidData(srcFiles)) {
            return;
        }

        String reason = getReasonMessage(result);

        List<String> srcNeedSafList = createNeedToWriteSdBySafList(srcFiles);
        boolean srcNeedSaf = srcNeedSafList.size() != 0 ? true : false;
        removeItemIfSafIsObtained(srcNeedSafList);
        boolean srcSafIsObtained = srcNeedSafList.size() == 0 ? true : false;
        String srcMessage = String.format("src (%s)", generateSafMessage(srcNeedSaf, srcSafIsObtained));

        String message = reason + ", " + srcMessage;
        super.sendEvents(context, CATEGORY_NAME, ACTION_DELETE_FAIL,
                message, srcFiles[0].getHasRetrictFiles() ? 1L : 0L);
    }

    private boolean isValidData(VFile[] files) {
        if (files == null || files.length == 0) return false;
        int fileType = files[0].getVFieType();
        return (fileType == VFile.VFileType.TYPE_LOCAL_STORAGE)
                || (fileType == VFile.VFileType.TYPE_CATEGORY_STORAGE);
    }

    private boolean isValidData(VFile file) {
       return isValidData(new VFile[] {file});
    }

    private String getReasonMessage(EditResult result) {
        switch (result.ECODE) {
        case EditResult.E_EXIST:
            return "already exist";
        case EditResult.E_FAILURE:
            return "failure";
        case EditResult.E_PERMISSION:
            return "permission deny";
        case EditResult.E_NOSPC:
            return "no space";
        default:
            return "";
        }
    }

    private List<String> createNeedToWriteSdBySafList(VFile[] files) {
        List<String> list = new ArrayList<String>();
        SafOperationUtility safOperationUtility = SafOperationUtility.getInstance();
        for (VFile vFile : files) {
            if (safOperationUtility.isNeedToWriteSdBySaf(vFile.getAbsolutePath())) {
                list.add(vFile.getAbsolutePath());
            }
        }

        return list;
    }

    private void removeItemIfSafIsObtained(List<String> list) {
        SafOperationUtility safOperationUtility = SafOperationUtility.getInstance();
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String filePath = iterator.next();
            if (!safOperationUtility.isNeedToShowSafDialog(filePath)) {
                iterator.remove();
            }
        }
    }

    private String generateSafMessage(boolean needSaf, boolean safIsObtained) {
        if (needSaf) {
            if (safIsObtained) {
                return "has saf";
            } else {
                return "needs saf";
            }
        } else {
            return "no needs saf";
        }
    }
}
