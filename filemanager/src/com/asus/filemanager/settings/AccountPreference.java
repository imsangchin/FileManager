package com.asus.filemanager.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

/**
 * GeneralPreference is the general setting page.
 * We can get the preference value by using the static field "KEY_XXXXXX".
 *
 * @see SettingActivity
 * @author jason_uang
 * @version 1.0
 */
public class AccountPreference extends PreferenceFragment
        implements OnSharedPreferenceChangeListener {

    // Preference keys
    private static final String KEY_USER_NAME = "preferences_user_name";
    private static final String KEY_SPACE_USED = "preferences_space_used";
    private static final String KEY_SET_CACHE_SIZE = "preferences_set_cache_size";
    private static final String KEY_CLEAR_CACHE = "preferences_clear_cache";
    private static final String KEY_REMOVE_ACCOUNT = "preferences_remove_account_action";

    private String mAccountName;
    private String mAccountId;
    private int mAccountType;
    private Activity mActivity;
    private AccountChangedCallback mAccountChangedCallback;

    private Preference mUserName;

    private SharedPreferences.OnSharedPreferenceChangeListener mCloudStoragelistener;
    private SharedPreferences mCloudStoragePref;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mAccountChangedCallback = (AccountChangedCallback)activity;
    }

    public interface AccountChangedCallback{
        public void onAccountNotExisted();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.account_preferences);

        mAccountName = getArguments().getString(SettingActivity.ACCOUNT_NAME_KEY);
        mAccountId = getArguments().getString(SettingActivity.ACCOUNT_ID_KEY);
        mAccountType = getArguments().getInt(SettingActivity.ACCOUNT_TYPE_KEY);

        // change StorageType to MsgObj type
        mAccountType = RemoteVFile.getMsgObjType(mAccountType);

        mUserName = (Preference) findPreference(KEY_USER_NAME);
        mUserName.setSummary(mAccountName);

        Log.v("Johnson", "send MSG_APP_REQUEST_STORAGE_USAGE");
        RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(mAccountName, null, null, mAccountType, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_STORAGE_USAGE);

        mCloudStoragePref = mActivity.getSharedPreferences("settings", 0);
        mCloudStoragelistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.v("Johnson", "get updated sharedPreference debug 01");
                if (sharedPreferences.equals(mCloudStoragePref)) {
                    Log.v("Johnson", "get updated sharedPreference debug 02");
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!isAccountExisted(mAccountType)){
            if(mAccountChangedCallback != null)
                mAccountChangedCallback.onAccountNotExisted();
            return;
        }
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        mCloudStoragePref.registerOnSharedPreferenceChangeListener(mCloudStoragelistener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mCloudStoragePref.unregisterOnSharedPreferenceChangeListener(mCloudStoragelistener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        Preference pref = findPreference(key);
        if (pref.equals(findPreference(KEY_SPACE_USED))) {

        } else if (pref.equals(findPreference(KEY_SET_CACHE_SIZE))) {

        } else if (pref.equals(findPreference(KEY_CLEAR_CACHE))) {

        } else if (pref.equals(findPreference(KEY_REMOVE_ACCOUNT))) {

        }
    }

    private boolean isAccountExisted(int type){
        String accountType = "";
        switch(type) {
        case StorageType.TYPE_ASUSWEBSTORAGE:
            accountType = SettingActivity.ASUS_SERVICE_ACCOUNT_TYPE;
            break;
        case StorageType.TYPE_GOOGLE_DRIVE:
            accountType = SettingActivity.GOOGLE_TYPE;
            break;
        }
        if (!accountType.equals("")) {
            Account[] accounts = AccountManager.get(mActivity).getAccountsByType(accountType);
            if (accounts == null) {
                return false;
            } else {
                for (int i=0 ; i<accounts.length ; i++) {
                    if (accounts[i].name.equals(mAccountName) && accounts[i].type.equals(accountType)) {
                        break;
                    } else if (i == accounts.length - 1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
