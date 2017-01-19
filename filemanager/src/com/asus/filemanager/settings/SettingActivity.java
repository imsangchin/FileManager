package com.asus.filemanager.settings;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceActivity.Header;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.asus.filemanager.R;
import com.asus.filemanager.settings.AccountPreference.AccountChangedCallback;
import com.asus.filemanager.utility.FolderElement.StorageType;

public class SettingActivity extends PreferenceActivity implements AccountChangedCallback {

    private Handler mHandler = new Handler(); //associates this handler with the queue for the current thread.
    private ArrayList<String> mAccountsName = new ArrayList<String>();
    private ArrayList<String> mAccountsId;
    private ArrayList<Integer> mAccountsType;
    private ArrayList<AccountSettingsEntry> accountSettingsList = new ArrayList<AccountSettingsEntry>();

    public static final String ACCOUNT_NAME_KEY = "account_name";
    public static final String ACCOUNT_ID_KEY = "account_id";
    public static final String ACCOUNT_TYPE_KEY = "account_type";
    public static final String GOOGLE_TYPE = "com.google";
    public static final String ASUS_SERVICE_ACCOUNT_TYPE    = "com.asus.account.webstorage";

    private List<Header> mHeaders;

    public class AccountSettingsEntry {
        public String accountName;
        public String accountId;
        public int accountType;

        public AccountSettingsEntry(String name, String id, int type) {
            accountName = name;
            accountId = id;
            accountType = type;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        GeneralUtils.setAsusThemeIfNeeded(this);
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        mAccountsName = intent.getStringArrayListExtra("accountNames");
        mAccountsId = intent.getStringArrayListExtra("accountIds");
        mAccountsType = intent.getIntegerArrayListExtra("accountTypes");

        // debug
        if(mAccountsName != null){
            for (int i=0 ; i<mAccountsName.size() ; i++) {
                Log.v("Johnson", i + " mAccountsName: " + mAccountsName.get(i) + " mAccountsId: " + mAccountsId.get(i) +
                    " mAccountsType: " + mAccountsType.get(i));
            }

            for (int i=0 ; i<mAccountsName.size() ; i++) {
                accountSettingsList.add(new AccountSettingsEntry(mAccountsName.get(i), mAccountsId.get(i), mAccountsType.get(i)));
            }
            Log.v("Johnson", "onCreate accountSettingsList size: " + accountSettingsList.size());
        }

        invalidateHeaders();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.filemanager_settings_headers, target);
        Log.v("Johnson", "onBuildHeaders accountSettingsList size: " + accountSettingsList.size());
        // remove ASUS Web Storage if it was removed from settings
        if (accountSettingsList != null && accountSettingsList.size() > 0) {
            // Add accounts to list of preference header.
            for (int i=0 ; i<accountSettingsList.size() ; i++) {
                Header accountHeader = new Header();
                accountHeader.title = accountSettingsList.get(i).accountName;
                accountHeader.fragment = AccountPreference.class.getName();
                Bundle args = new Bundle();
                args.putString(ACCOUNT_NAME_KEY, accountSettingsList.get(i).accountName);
                args.putString(ACCOUNT_ID_KEY, accountSettingsList.get(i).accountId);
                args.putInt(ACCOUNT_TYPE_KEY, accountSettingsList.get(i).accountType);
                accountHeader.fragmentArguments = args;
                Log.v("Johnson", i + " add accountSettingsList: " + accountSettingsList.get(i).accountName);
                target.add(target.size(), accountHeader);
            }
        }
        mHeaders = target;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_title_bar, menu);
        getActionBar()
                .setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        // post a CheckAccounts runnable to main thread. the runnable will refresh the account list.
        if (mHandler != null) {
            mHandler.post(mCheckAccounts);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mCheckAccounts);
        }
        super.onPause();
    }

    /**
     * This runnable will refresh the account list.
     * Note this runnable must run in main thread.
     */
    Runnable mCheckAccounts = new Runnable() {
        @Override
        public void run() {
            if (checkAccounts()) {
                invalidateHeaders();
            }
        }
    };

    /** callback from second level fragment to notify selected account not existed anymore*/
    public void onAccountNotExisted(){
        if(!isMultiPane()){
            // escape from second level
            onBackPressed();
        }else{
        	if (null != mHeaders)
        		switchToHeader(mHeaders.get(0));
        }
    }

    public boolean checkAccounts() {
        boolean isChanged = false;

        if (accountSettingsList != null && accountSettingsList.size() > 0) {
            int total = accountSettingsList.size();
            for (int i=total -1 ; i>=0 ; i--) {
                String accountType = "";
                switch(accountSettingsList.get(i).accountType) {
                case StorageType.TYPE_ASUSWEBSTORAGE:
                    accountType = SettingActivity.ASUS_SERVICE_ACCOUNT_TYPE;
                    break;
                case StorageType.TYPE_GOOGLE_DRIVE:
                    accountType = SettingActivity.GOOGLE_TYPE;
                    break;
                }
                if (!accountType.equals("")) {
                    Account[] accounts = AccountManager.get(this).getAccountsByType(accountType);
                    if (accounts != null && accounts.length > 0) {
                        for (int j=0 ; j<accounts.length ; j++) {
                            if (accountSettingsList.get(i).accountName.equals(accounts[j].name)) {
                                break;
                            } else if (j == accounts.length - 1) {
                                accountSettingsList.remove(i);
                                isChanged = true;
                            }
                        }
                    }
                }
            }
        }
        Log.v("Johnson", "checkAccounts accountSettingsList size: " + accountSettingsList.size());
        return isChanged;
    }

    public void updateSpaceUsed() {
        Log.v("Johnson", "updateSpaceUsed");
    }
    
    private boolean doValidcheck(String fragmentName) throws IllegalArgumentException{  
          
        return true;  
    }  
      
  
    protected boolean isValidFragment(String fragmentName) {  
        return doValidcheck(fragmentName);  
    }  
}
