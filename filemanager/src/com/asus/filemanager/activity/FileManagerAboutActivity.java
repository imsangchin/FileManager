package com.asus.filemanager.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.FileManagerAboutAdapter;
import com.asus.filemanager.dialog.WhatsNewDialogFragment;
import com.asus.filemanager.ga.GaMenuItem;
import com.asus.filemanager.utility.AnalyticsReflectionUtility;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.Utility;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.uservoice.uservoicesdk.UserVoice;

public class FileManagerAboutActivity extends BaseActivity implements WhatsNewDialogFragment.OnWhatsNewDialogFragmentListener {

    public static final boolean isUser = Build.TYPE.equals("user");

    private ListView listView;
    private FileManagerAboutAdapter aboutAdapter;
    private static final String TAG = "AboutActivity";
    private static int REQUEST_TUTORIAL = 1;
    private boolean inspireAsusExist = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inspireAsusExist = AnalyticsReflectionUtility.InspireAsusExist(
                FileManagerAboutActivity.this);
        ColorfulLinearLayout.setContentView(this, R.layout.layout_about, R.color.theme_color);
        initActionBar();
        getAboutList();
        setListener();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    Log.d(TAG,"onOptionsItemSelected()");
                    finish();
                }
                return true;
        }

        return false;
    }

    private void setListener() {
        // TODO Auto-generated method stub
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                int arg2, long arg3) {
            // TODO Auto-generated method stub
            Log.d(TAG,"setOnItemLongClickListener()");
            return false;
        }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick (AdapterView<?> parent, View view, int position, long id) {

            if (0 == id) {
                Intent tutorialIntent = new Intent();
                tutorialIntent.setClass(FileManagerAboutActivity.this, TutorialActivity.class);
                startActivityForResult(tutorialIntent, REQUEST_TUTORIAL);
                GaMenuItem.getInstance().sendEvents(FileManagerAboutActivity.this, GaMenuItem.CATEGORY_NAME,
                            GaMenuItem.ACTION_TUTORIAL, null, null);
            } else if (1 == id){
                Intent propagateIntent = new Intent();
                propagateIntent.setClassName("com.asus.filemanager", "com.asus.filemanager.activity.FileManagerEULATemplActivity");
                propagateIntent.putExtra("infoType",ConstantsUtil.TEMPL_OPENSOURCE);
                startActivity(propagateIntent);
            } else if (2 == id){
                Intent propagateIntent = new Intent();
                propagateIntent.setClassName("com.asus.filemanager", "com.asus.filemanager.activity.FileManagerEULATemplActivity");
                propagateIntent.putExtra("infoType",ConstantsUtil.TEMPL_USERAGREEMENT);
                startActivity(propagateIntent);
            }else if(3 == id){
                String url = getString(R.string.privacy_policy_url);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }else if(4 == id){
                String url = getString(R.string.terms_of_service_url);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }else if(5 == id){
                if (Utility.isMonkeyRunning()) {
                    return;
                }
                if(ItemOperationUtility.getInstance().checkCtaPermission(FileManagerAboutActivity.this)) {
                    FileManagerActivity.initFeedBackResource(FileManagerAboutActivity.this);
                    UserVoice.launchUserVoice(getApplicationContext()); // Show the UserVoice portal
                }
            }else if(6 == id){

                SharedPreferences settings2 = FileManagerAboutActivity.this.getSharedPreferences("MyPrefsFile", 0);
                Boolean bInspireUS = settings2.getBoolean(ConstantsUtil.PREV_INSPIREUS, true);
                SharedPreferences.Editor editor = settings2.edit();
                editor.putBoolean(ConstantsUtil.PREV_INSPIREUS, !bInspireUS);
                editor.apply();
                aboutAdapter.notifyDataSetChanged();

                Boolean isEnable = !bInspireUS;
                GoogleAnalytics.getInstance(FileManagerAboutActivity.this).setAppOptOut(!isEnable);
                FirebaseAnalytics.getInstance(FileManagerAboutActivity.this).setAnalyticsCollectionEnabled(isEnable);
                /*
                if (!bInspireUS)
                    ((GlobalVariable) getApplicationContext()).mObserver.bAcceptRecordFlurryData = true;
                else
                    ((GlobalVariable) getApplicationContext()).mObserver.bAcceptRecordFlurryData = false;
                ((GlobalVariable) getApplicationContext()).mObserver.checkEnableAsusInspired();

                settingsAdapter.notifyDataSetChanged();
                */
            }else if(7 == id){
                WhatsNewDialogFragment whatsDialogFragment = WhatsNewDialogFragment.newInstance();
                whatsDialogFragment.setStyle(WhatsNewDialogFragment.STYLE_NO_TITLE, R.style.FMAlertDialogStyle);
                whatsDialogFragment.show(getFragmentManager(), WhatsNewDialogFragment.DIALOG_TAG);
                /*
                AboutDialogFragment aboutDialog = AboutDialogFragment.newInstance();
                aboutDialog.show(getFragmentManager(), "AboutDialogFragment");
                */
                /*
                ChangeLog cl = new ChangeLog(ShareRimAboutActivity.this);
                ChangelogDialogFragment changelogDialogFragment = ChangelogDialogFragment.newInstance(cl.getFullLog(),true);
                if(!isFinishing()) changelogDialogFragment.show(getFragmentManager(), "changelogdialog");
                */
                /*
                AboutDialog aboutDialog = AboutDialog.newInstance(true);
                aboutDialog.show(getFragmentManager(), "AboutDialogFragment");
                */
            }

        }
        });
    }

    private void getAboutList() {
        listView = (ListView) findViewById(R.id.about_list);
        //listView.setBackgroundColor(Color.WHITE);
        aboutAdapter = new FileManagerAboutAdapter(this.getApplicationContext());
        listView.setAdapter(aboutAdapter);
        if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
            ColorDrawable colorDrawable = new ColorDrawable(getResources().getColor(R.color.grey));
            listView.setDivider(colorDrawable);
            listView.setDividerHeight(1);
        }
    }


    private String getProfileDisplayname() {
        if(!isUser) {       // userdebug should not read Contacts for CTA test
            return "";
        } else {
            String displayName = "";
            String[] projection = new String[] {ContactsContract.Profile.DISPLAY_NAME};
            Uri dataUri = Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
            ContentResolver contentResolver = getContentResolver();
            Cursor c = contentResolver.query(dataUri, projection, null, null, null);
            if(null == c) {
                return "";
            } else {
                try {
                    if (c.moveToFirst()) {
                        displayName = c.getString(c.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
                    }
                } finally {
                    c.close();
                }
                return displayName;
            }
        }
    }
    /** set ActionBar background and invisible title */
    private void initActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_edit_bar_bg_wrap));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(this.getResources().getString(R.string.about));
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

   }

    @Override
    public void onWhatsNewDialogConfirmed() {

    }

    @Override
    public void onWhatsNewDialogDismissed() {

    }
}
