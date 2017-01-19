package com.asus.filemanager.ga;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GaHiddenCabinet extends GoogleAnalyticsBase {

    public static final String CATEGORY_NAME = "hidden_cabinet";

    private static final String DEFAULT_TRACKER_ID = "UA-56127731-26";
    public static final String KEY_ID = "GA_HIDDEN_CABINET_ID";
    public static final String KEY_ENABLE_TRACKING = "GA_HIDDEN_CABINET_ENABLE_TRACKING";
    public static final String KEY_SAMPLE_RATE = "GA_HIDDEN_CABINET_SAMPLE_RATE";

    private static final String KEY_LAST_SEND_TOTAL_FILES_COUNT = "last_send_total_files_count";
    private static final String KEY_LAST_SEND_FINGERPRINT_TO_UNLOCK_STATUS = "last_send_fingerprint_to_unlock_status";

    private static final String ACTION_ANALYZE_FILES_COUNT = "analyze_files_count";
    private static final String ACTION_SETUP_PASSWORD = "setup_password";
    private static final String ACTION_SETUP_ACCOUNT = "setup_account";
    private static final String ACTION_CHANGE_PASSWORD = "change_password";
    private static final String ACTION_FORGET_PASSWORD = "forget_password";
    private static final String ACTION_FINGERPRINT_TO_UNLOCK_SETTINGS = "fingerprint_to_unlock_settings";
    private static final String ACTION_UNLOCK_FROM_HOMEPAGE = "unlock_from_homepage";
    private static final String ACTION_UNLOCK_FROM_AUTO_LOCK = "unlock_from_auto_lock";
    private static final String ACTION_MOVE_TO_HIDDEN_CABINET = "move_to_hidden_cabinet";

    private static final String LABEL_FILES_COUNT_EMPTY = "empty";
    private static final String LABEL_FILES_COUNT_LEVEL_1 = "1 ~ 10";
    private static final String LABEL_FILES_COUNT_LEVEL_2 = "11 ~ 30";
    private static final String LABEL_FILES_COUNT_LEVEL_3 = "31 ~ 50";
    private static final String LABEL_FILES_COUNT_LEVEL_4 = "> 50";
    private static final String LABEL_ALREADY_SET_BACKUP_ACCOUNT = "already_set_backup_account";
    private static final String LABEL_NOT_SET_BACKUP_ACCOUNT = "not_set_backup_account";
    private static final String LABEL_ACCOUNT_VERIFY_PASS = "account_verify_pass";
    private static final String LABEL_ACCOUNT_VERIFY_FAIL = "account_verify_fail";
    private static final String LABEL_UNLOCK_BY_PASSWORD = "unlock_by_password";
    private static final String LABEL_UNLOCK_BY_FINGERPRINT = "unlock_by_fingerprint";
    private static final String LABEL_FINGERPRINT_TO_UNLOCK_STATUS_ENABLE = "enable";
    private static final String LABEL_FINGERPRINT_TO_UNLOCK_STATUS_DISABLE = "disable";
    private static final String LABEL_FINGERPRINT_TO_UNLOCK_STATUS_NOT_SUPPORT = "not_support";

    private static GaHiddenCabinet mInstance;

    public enum FingerprintToUnlockStatus {
        NOT_SUPPORT, DISABLE, ENABLE
    }

    public static GaHiddenCabinet getInstance() {
        if (mInstance == null) {
            mInstance = new GaHiddenCabinet();
        }
        return mInstance;
    }

    private GaHiddenCabinet() {
        super(KEY_ID, KEY_ENABLE_TRACKING,
                KEY_SAMPLE_RATE, DEFAULT_TRACKER_ID, 100.0f);
    }

    public void sendMoveToHiddenCabinetEvent(Context context) {
        sendEvents(context, CATEGORY_NAME, ACTION_MOVE_TO_HIDDEN_CABINET,
                null, null);
    }

    public void sendTotalFilesEvent(Context context, int totalFilesCount) {
        int lastSendTotalFilesCount = getLastSendTotalFilesCount(context);
        if (lastSendTotalFilesCount != -1) {
            String lastSendFileCountLabel = getFileCountLabel(lastSendTotalFilesCount);
            sendEvents(context, CATEGORY_NAME, ACTION_ANALYZE_FILES_COUNT,
                    lastSendFileCountLabel, Long.valueOf(-1));
        }
        String currentSendFileCountLabel = getFileCountLabel(totalFilesCount);
        sendEvents(context, CATEGORY_NAME, ACTION_ANALYZE_FILES_COUNT,
                currentSendFileCountLabel, Long.valueOf(1));
        setLastSendTotalFilesCount(context, totalFilesCount);
    }

    public void sendSetupPasswordEvent(Context context, boolean hasBackupAccount) {
        sendEvents(context, CATEGORY_NAME, ACTION_SETUP_PASSWORD,
                hasBackupAccount ? LABEL_ALREADY_SET_BACKUP_ACCOUNT : LABEL_NOT_SET_BACKUP_ACCOUNT, null);
    }

    public void sendSetupAccountEvent(Context context, boolean hasBackupAccount) {
        sendEvents(context, CATEGORY_NAME, ACTION_SETUP_ACCOUNT,
                hasBackupAccount ? LABEL_ALREADY_SET_BACKUP_ACCOUNT : LABEL_NOT_SET_BACKUP_ACCOUNT, null);
    }

    public void sendChangePasswordEvent(Context context, boolean hasBackupAccount) {
        sendEvents(context, CATEGORY_NAME, ACTION_CHANGE_PASSWORD,
                hasBackupAccount ? LABEL_ALREADY_SET_BACKUP_ACCOUNT : LABEL_NOT_SET_BACKUP_ACCOUNT, null);
    }

    public void sendForgetPasswordEvent(Context context, boolean isSuccess) {
        sendEvents(context, CATEGORY_NAME, ACTION_FORGET_PASSWORD,
                isSuccess ? LABEL_ACCOUNT_VERIFY_PASS : LABEL_ACCOUNT_VERIFY_FAIL, null);
    }

    public void sendUnlockFromHomepageEvent(Context context, boolean unlockByFingerprint) {
        sendEvents(context, CATEGORY_NAME, ACTION_UNLOCK_FROM_HOMEPAGE,
                unlockByFingerprint ? LABEL_UNLOCK_BY_FINGERPRINT : LABEL_UNLOCK_BY_PASSWORD, null);
    }

    public void sendUnlockFromAutoLockEvent(Context context, boolean unlockByFingerprint) {
        sendEvents(context, CATEGORY_NAME, ACTION_UNLOCK_FROM_AUTO_LOCK,
                unlockByFingerprint ? LABEL_UNLOCK_BY_FINGERPRINT : LABEL_UNLOCK_BY_PASSWORD, null);
    }

    public void sendFingerprintToEnableEvent(Context context, int currentStatus) {
        int lastStatus = getLastSendFingerprintToUnlockStatus(context);
        if (currentStatus != lastStatus) {
            String lastLabel = getFingerprintToUnlockLabelByStatus(lastStatus);
            if (lastLabel != null) {
                sendEvents(context, CATEGORY_NAME, ACTION_FINGERPRINT_TO_UNLOCK_SETTINGS,
                        lastLabel, Long.valueOf(-1));
            }
            String currentLabel = getFingerprintToUnlockLabelByStatus(currentStatus);
            sendEvents(context, CATEGORY_NAME, ACTION_FINGERPRINT_TO_UNLOCK_SETTINGS,
                    currentLabel, Long.valueOf(1));
            setLastSendFingerprintToUnlockStatus(context, currentStatus);
        }
    }

    private int getLastSendTotalFilesCount(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(KEY_LAST_SEND_TOTAL_FILES_COUNT, -1);
    }

    private void setLastSendTotalFilesCount(Context context, int totalFilesCount) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(KEY_LAST_SEND_TOTAL_FILES_COUNT, totalFilesCount).commit();
    }

    private int getLastSendFingerprintToUnlockStatus(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(KEY_LAST_SEND_FINGERPRINT_TO_UNLOCK_STATUS, -1);
    }

    private void setLastSendFingerprintToUnlockStatus(Context context, int status) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(KEY_LAST_SEND_FINGERPRINT_TO_UNLOCK_STATUS, status).commit();
    }

    private String getFileCountLabel(int totalFilesCount) {
        if (totalFilesCount == 0) {
            return LABEL_FILES_COUNT_EMPTY;
        } else if (totalFilesCount > 0 && totalFilesCount <= 10) {
            return LABEL_FILES_COUNT_LEVEL_1;
        } else if (totalFilesCount > 10 && totalFilesCount <= 30) {
            return LABEL_FILES_COUNT_LEVEL_2;
        } else if (totalFilesCount > 30 && totalFilesCount <= 50) {
            return LABEL_FILES_COUNT_LEVEL_3;
        }
        return LABEL_FILES_COUNT_LEVEL_4;
    }

    private String getFingerprintToUnlockLabelByStatus(int status) {
        if (status < 0) {
            return null;
        }
        if (status == FingerprintToUnlockStatus.ENABLE.ordinal()) {
            return LABEL_FINGERPRINT_TO_UNLOCK_STATUS_ENABLE;
        } else if (status == FingerprintToUnlockStatus.DISABLE.ordinal()) {
            return LABEL_FINGERPRINT_TO_UNLOCK_STATUS_DISABLE;
        }
        return LABEL_FINGERPRINT_TO_UNLOCK_STATUS_NOT_SUPPORT;
    }
}