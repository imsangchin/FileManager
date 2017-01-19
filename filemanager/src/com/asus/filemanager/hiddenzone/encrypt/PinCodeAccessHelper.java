package com.asus.filemanager.hiddenzone.encrypt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.asus.filemanager.functionaldirectory.hiddenzone.Encryptor;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneUtility;

import android.accounts.Account;
import android.os.Environment;

public class PinCodeAccessHelper {
    private static final String TAG = "PinCodeAccessHelper";

    private static PinCodeAccessHelper sPinCodeAccessHelper;

    private static final File PW_FILE = new File(Environment.getExternalStorageDirectory()
            + "/" + HiddenZoneUtility.HIDDEN_ZONE_DIRECTORY_NAME, ".password");
    private static final File BACKUP_ACCOUNT_FILE = new File(Environment.getExternalStorageDirectory()
            + "/" + HiddenZoneUtility.HIDDEN_ZONE_DIRECTORY_NAME, ".backup_account");

    public static PinCodeAccessHelper getInstance() {
        if (sPinCodeAccessHelper == null) {
            return new PinCodeAccessHelper();
        }
        return sPinCodeAccessHelper;
    }

    private PinCodeAccessHelper() {
    }

    public boolean checkPinCode(String pinCode) {
        String encryptPinCode = Encryptor.getEncryptor().encode(pinCode);
        String storedPinCode = "";

        try {
            storedPinCode = FileUtils.readFileToString(PW_FILE, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return storedPinCode.equalsIgnoreCase(encryptPinCode);
    }

    public boolean setPinCode(String pinCode) {
        if (pinCode == null) {
            return PW_FILE.delete();
        } else {
            String encryptPinCode = Encryptor.getEncryptor().encode(pinCode);
            try {
                FileUtils.writeStringToFile(PW_FILE, encryptPinCode, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public boolean hasPinCode() {
        return PW_FILE.exists();
    }

    public boolean hasRecoveryAccount() {
        return getRecoveryAccount() != null;
    }

    public Account getRecoveryAccount() {
        try {
            List<String> list = FileUtils.readLines(BACKUP_ACCOUNT_FILE, "UTF-8");
            if (list.size() < 2) {
                return null;
            }
            String storedAccountName = Encryptor.getEncryptor().decode(list.get(0));
            String storedAccountType = Encryptor.getEncryptor().decode(list.get(1));
            return new Account(storedAccountName, storedAccountType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setRecoveryAccount(String accountName, String accountType) {
        String encryptAccountName = Encryptor.getEncryptor().encode(accountName);
        String encryptAccountType = Encryptor.getEncryptor().encode(accountType);

        List<String> list = new ArrayList<String>();
        list.add(encryptAccountName);
        list.add(encryptAccountType);
        try {
            FileUtils.writeLines(BACKUP_ACCOUNT_FILE, "UTF-8", list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearRecoveryAccount() {
        if (BACKUP_ACCOUNT_FILE.exists()) {
            BACKUP_ACCOUNT_FILE.delete();
        }
    }
}
