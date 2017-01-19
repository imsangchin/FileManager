package com.asus.filemanager.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.AddCloudAccountAdapter;
import com.asus.filemanager.dialog.PermissionReasonDialogFragment;
import com.asus.filemanager.filesystem.FileManager;
import com.asus.filemanager.ga.GaBrowseFile;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.ToolbarUtil;
import com.asus.filemanager.utility.permission.PermissionChecker;
import com.asus.filemanager.utility.permission.PermissionDialog;
import com.asus.filemanager.utility.permission.PermissionManager;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.service.cloudstorage.common.MsgObj;

public class AddCloudAccountActivity extends BaseAppCompatActivity implements PermissionChecker {

    private static final String TAG = AddCloudAccountActivity.class.getSimpleName();

    private Activity mActivity;
    private ListView mListView;
    private AddCloudAccountAdapter mAdapter;
    private String[] mCloudTitles;
    private List<Entry<String, Integer>> mLoginedAccounts;
    private int mLoginMsgObjType;
    private int mPendingLoginMsgObjType;

    /**
     * {@link PermissionManager} to handle request-permission task.
     */
    private PermissionManager mPermissionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        mAdapter = new AddCloudAccountAdapter(mActivity, mCloudTitles);
        mLoginMsgObjType = MsgObj.TYPE_UNKNOWN_STORAGE;
        mPendingLoginMsgObjType = MsgObj.TYPE_UNKNOWN_STORAGE;
        mPermissionManager = new PermissionManager(this);
        ColorfulLinearLayout.setContentView(this, R.layout.layout_addcloudaccount, R.color.theme_color);
        ToolbarUtil.setupToolbar(this);
        initActionBar();
        initListView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Entry<String, Integer>> loginedAccount = RemoteAccountUtility.getInstance(mActivity)
                .getLoginedAccountNameAndMsgObjType(mActivity);
        if (loginedAccount != null && mLoginedAccounts != null
                && loginedAccount.size() > mLoginedAccounts.size()) {
            // new account add
            List<Entry<String, Integer>> tmpLoginedAccount = new ArrayList<Entry<String, Integer>>();
            tmpLoginedAccount.addAll(loginedAccount);
            tmpLoginedAccount.removeAll(mLoginedAccounts);
            for (Entry<String, Integer> account : tmpLoginedAccount) {
                if (mLoginMsgObjType == account.getValue()) {
                    gaSendEventMsgObjType(mActivity, mLoginMsgObjType);
                    tmpLoginedAccount.clear();
                    Log.v(TAG, "cloud " +
                        RemoteAccountUtility.getInstance(mActivity).findCloudTitleByMsgObjType(mActivity, mLoginMsgObjType) +
                        " login success"
                    );
                    finish();
                    return;
                }
            }
        }
        mLoginedAccounts = loginedAccount;

        List<String> cloudTitles = RemoteAccountUtility.getInstance(mActivity)
                .getAvailableLoginCloudTitles(mActivity);

        // Special case, when app was force close at this page,
        // such as change permission in setting page manually,then open app again.
        // At this point many components are not finish initialization,
        // thus we directly close this page and back to main page.
        if(cloudTitles.size() == 0)
        {
            Intent intent = new Intent(this, FileManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        List<String> accountNames = new ArrayList<String>();
        for (int i = 0; i < cloudTitles.size(); ++i) {
            accountNames.add("");
        }
        if (!cloudTitles.isEmpty() && !accountNames.isEmpty()) {
            mCloudTitles = cloudTitles.toArray(new String[cloudTitles.size()]);
        }
        mAdapter.updateAdapter(mActivity, mCloudTitles);

        // When user grant permission from setting page and back to this page,
        // dismiss this dialog.
        if ((PermissionManager.checkPermissions(this,
                new String[]{Manifest.permission.GET_ACCOUNTS})))
        {
            Fragment dialogFragment = getFragmentManager().findFragmentByTag(PermissionDialog.TAG);
            if (dialogFragment != null && dialogFragment instanceof PermissionDialog)
            {
                ((PermissionDialog) dialogFragment).dismissAllowingStateLoss();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                finish();
            }
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
        @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if ((requestCode != PermissionManager.REQUEST_PERMISSION  && requestCode != PermissionManager.RE_REQUEST_PERMISSION ) ||
           grantResults.length == 0)
        {
            return;
        }

        ArrayList<String> permToBeRequest = new ArrayList<>();
        ArrayList<Integer> reasonsToBeViewed = new ArrayList<>();

        for (int i=0;i<grantResults.length;i++)
        {
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, permissions[i]))
            {
                reasonsToBeViewed.add(R.string.permission_essential);
                permToBeRequest.add(permissions[i]);
            }

        }
        if(requestCode == PermissionManager.RE_REQUEST_PERMISSION)
        {
            if (!(PermissionManager.checkPermissions(this,
                    new String[]{Manifest.permission.GET_ACCOUNTS})))
            {
                permToBeRequest = new ArrayList<>();
                reasonsToBeViewed = new ArrayList<>();

                reasonsToBeViewed.add(R.string.permission_reason_contact);
                permToBeRequest.add(Manifest.permission.GET_ACCOUNTS);

                int requestState = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.GET_ACCOUNTS) ? PermissionManager.RE_REQUEST_PERMISSION : 0;
                PermissionDialog newFragment = PermissionDialog
                        .newInstance(reasonsToBeViewed, permToBeRequest, requestState);
                newFragment.show(getFragmentManager(), PermissionDialog.TAG);
            }
            else
            {
                // User grant permission, dialog can dismiss.
                Fragment dialogFragment = getFragmentManager().findFragmentByTag(PermissionDialog.TAG);
                if(dialogFragment != null && dialogFragment instanceof PermissionDialog)
                {
                    ((PermissionDialog)dialogFragment).dismissAllowingStateLoss();
                }

                if (mPendingLoginMsgObjType != MsgObj.TYPE_UNKNOWN_STORAGE) {
                    mLoginMsgObjType = mPendingLoginMsgObjType;
                    RemoteAccountUtility.getInstance(mActivity).addAccount(mLoginMsgObjType);
                    mPendingLoginMsgObjType = MsgObj.TYPE_UNKNOWN_STORAGE;
                }
            }
        }
    }

    private void initListView() {
        mListView = (ListView) findViewById(R.id.about_list);
        mListView.setBackgroundColor(Color.WHITE);
        mListView.setEmptyView(createEmptyTextView(this, mListView, getString(R.string.no_items)));
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (RemoteAccountUtility.getInstance(mActivity).showWifiTurnOnDialogIfNeed()) {
                    return;
                }
                if (!ItemOperationUtility.getInstance().checkCtaPermission(
                        mActivity)) {
                    // ToastUtility.show(mActivity,
                    // mActivity.getResources().getString(R.string.network_cta_hint));
                    return;
                }

                // Currently only Google Drive and Dropbox need contact permission.
                if(id == MsgObj.TYPE_DROPBOX_STORAGE || id == MsgObj.TYPE_GOOGLE_DRIVE_STORAGE)
                {
                    if (!(PermissionManager.checkPermissions(AddCloudAccountActivity.this,
                            new String[]{Manifest.permission.GET_ACCOUNTS})))
                    {
                        requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS},
                                PermissionManager.RE_REQUEST_PERMISSION);
                        mPendingLoginMsgObjType = (int) parent.getItemIdAtPosition(position);
                        return;
                    }
                }

                int msgObjType = (int) parent.getItemIdAtPosition(position);
                mLoginMsgObjType = msgObjType;
                RemoteAccountUtility.getInstance(mActivity).addAccount(msgObjType);
            }
        });
    }

    private static TextView createEmptyTextView(Context context, ListView listview, String emptyText) {
        TextView emptyTextView = new TextView(context);
        emptyTextView.setText(emptyText);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        ((ViewGroup)(listview.getParent())).addView(emptyTextView, lp);
        return emptyTextView;
    }

    /** set ActionBar background and invisible title */
    private void initActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setSplitBackgroundDrawable(ContextCompat.getDrawable(this,
                    R.drawable.asus_ep_edit_bar_bg_wrap));
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(this.getResources().getString(
                    R.string.add_cloud_storage_dialog_title));
            actionBar.setIcon(android.R.color.transparent);
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    private static void gaSendEventMsgObjType(Context context, int msgObjType) {
        switch (msgObjType) {
        case MsgObj.TYPE_UNKNOWN_STORAGE:
            break;
        case MsgObj.TYPE_WIFIDIRECT_STORAGE:
            break;
        case MsgObj.TYPE_GOOGLE_DRIVE_STORAGE:
            GaBrowseFile.getInstance()
                    .sendEvents(context, GaBrowseFile.CATEGORY_NAME,
                            GaBrowseFile.ACTION_BROWSE_FROM_DRAWER,
                            GaBrowseFile.LABEL_GOOGLE_DRIVE, null);
            break;
        case MsgObj.TYPE_DROPBOX_STORAGE:
            GaBrowseFile.getInstance()
                    .sendEvents(context, GaBrowseFile.CATEGORY_NAME,
                            GaBrowseFile.ACTION_BROWSE_FROM_DRAWER,
                            GaBrowseFile.LABEL_DROPBOX, null);
            break;
        case MsgObj.TYPE_SKYDRIVE_STORAGE:
            GaBrowseFile.getInstance()
                    .sendEvents(context, GaBrowseFile.CATEGORY_NAME,
                            GaBrowseFile.ACTION_BROWSE_FROM_DRAWER,
                            GaBrowseFile.LABEL_ONEDRIVE, null);
            break;
        case MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE:
            GaBrowseFile.getInstance()
                    .sendEvents(context, GaBrowseFile.CATEGORY_NAME,
                            GaBrowseFile.ACTION_BROWSE_FROM_DRAWER,
                            GaBrowseFile.LABEL_ASUS_WEBSTORAGE, null);
            break;
        case MsgObj.TYPE_HOMECLOUD_STORAGE:
            GaBrowseFile.getInstance()
                    .sendEvents(context, GaBrowseFile.CATEGORY_NAME,
                            GaBrowseFile.ACTION_BROWSE_FROM_DRAWER,
                            GaBrowseFile.LABEL_ASUS_HOMECLOUD, null);
            break;
        case MsgObj.TYPE_BAIDUPCS_STORAGE:
            GaBrowseFile.getInstance()
                    .sendEvents(context, GaBrowseFile.CATEGORY_NAME,
                            GaBrowseFile.ACTION_BROWSE_FROM_DRAWER,
                            GaBrowseFile.LABEL_BAIDU, null);
            break;
        case MsgObj.TYPE_AUCLOUD_STORAGE:
            // FIXME:
            // not support?
            break;
        case MsgObj.TYPE_YANDEX_STORAGE:
            GaBrowseFile.getInstance()
                    .sendEvents(context, GaBrowseFile.CATEGORY_NAME,
                            GaBrowseFile.ACTION_BROWSE_FROM_DRAWER,
                            GaBrowseFile.LABEL_YANDEX, null);
            break;
        }
    }

    //// The following two are PermissionChecker implementation. ////
    @Override
    public PermissionManager getManager()
    {
        return mPermissionManager;
    }

    @Override
    public void permissionDeniedForever(ArrayList<String> permissions)
    {
        // go to settings directly
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + getPackageName()));
        intent.putExtra(":settings:fragment_args_key", "permission_settings");
        intent.putExtra(":settings:fragment_args_key_highlight_times", 3);
        try {
            startActivityForResult(intent,FileManagerActivity.FILE_MANAGER_SETTING_PERMISSION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
