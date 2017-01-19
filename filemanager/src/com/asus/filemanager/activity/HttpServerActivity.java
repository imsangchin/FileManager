package com.asus.filemanager.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.filesystem.FileManager;
import com.asus.filemanager.ga.GaPromote;
import com.asus.filemanager.saf.DocumentFile;
import com.asus.filemanager.utility.AnalyticsReflectionUtility;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.WifiStateMonitor;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.http.server.IHttpFileServerStateListener;
import com.asus.filetransfer.receive.IReceiveFileHandler;
import com.asus.filetransfer.send.SendFileTask;
import com.asus.filetransfer.utility.HttpFileServerAnalyzer;
import com.asus.remote.dialog.RemoteWiFiTurnOnDialogFragment;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;

public class HttpServerActivity extends BaseActivity {

    private final String TAG = getClass().getSimpleName();
    public static final int REQUEST_SDTUTORIAL = 1;
    private static final int EXTERNAL_STORAGE_PERMISSION_REQ = 2;

    private AlertDialog checkStartServerDialog;
    private AlertDialog checkAcquirePermissionDialog;
    private TextView serverDescriptionView;
    private TextView serverAddressView;
    private Button startButton;
    private Button stopButton;
    private LinearLayout safHint;

    private HttpFileServer httpFileServerServer;
    private FileManager fileManager;
    private SendFileTask sendFileTask;

    PowerManager.WakeLock wakeLock;

    private boolean bNeedShowWifiSettingDialog = true;

    protected void setMockReceiveFileHandler(IReceiveFileHandler mockReceiveFileHandler) {
        httpFileServerServer = new HttpFileServer(fileManager, mockReceiveFileHandler, 55432);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ColorfulLinearLayout.setContentView(this, R.layout.activity_httpserver, R.color.theme_color);
        serverAddressView = (TextView) findViewById(R.id.ip);

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        serverDescriptionView = (TextView) findViewById(R.id.description);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fileManager.needSAFPermission())
                    showCheckStartServerDialog();
                else
                    startServer();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServer();
            }
        });
        fileManager = new FileManager(this);
        httpFileServerServer= new HttpFileServer(fileManager, 55432);
        initActionBar();
        safHint = (LinearLayout) findViewById(R.id.saf_hint);
        safHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acquireExternalStoragePermission();
            }
        });
        sendGAWhenLaunchIfNeeded();
        HttpFileServerAnalyzer.init(this, isFromPromoteNotification() == null?
                HttpFileServerAnalyzer.Entry.Tool
                : HttpFileServerAnalyzer.Entry.PromoteNotification);
    }

    @Override
    protected void onDestroy() {
        HttpFileServerAnalyzer.deInit();
        super.onDestroy();
    }

    private String isFromPromoteNotification() {
        Intent intent = getIntent();
        return intent == null ? null : intent.getStringExtra("ga");
    }

    private void sendGAWhenLaunchIfNeeded() {
        boolean isGAEnable = AnalyticsReflectionUtility.getEnableAsusAnalytics(this);
        if (isGAEnable) {
            String mFromNotification = isFromPromoteNotification();
            if (null != mFromNotification){
                Log.d(TAG, "send ga  : " + mFromNotification);
                GaPromote.getInstance().sendEvents(this, mFromNotification,
                        GaPromote.PROMOTE_CLICK_ACTION, null, null);
            }
        }
    }

    /** set ActionBar background and invisible title */
    private void initActionBar() {
        final android.app.ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.asus_ep_edit_bar_bg_wrap));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(this.getResources().getString(R.string.httpserver_title));
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void showCheckStartServerDialog() {
        if (checkStartServerDialog == null)
            initCheckStartServerDialog();
        checkStartServerDialog.show();
    }

    private void showCheckAcquirePermissionDialog() {
        if (checkAcquirePermissionDialog == null)
            initCheckAcquirePermissionDialog();
        checkAcquirePermissionDialog.show();
    }

    private void initCheckStartServerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, ThemeUtility.getAsusAlertDialogThemeId());
        builder.setTitle(getString(R.string.httpserver_no_permission_title));
        builder.setMessage(getString(R.string.httpserver_no_permission_message));
        builder.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startServer();
            }
        });
        builder.setNegativeButton(getString(android.R.string.no), null);
        checkStartServerDialog = builder.create();
    }

    private void initCheckAcquirePermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, ThemeUtility.getAsusAlertDialogThemeId());
        builder.setTitle(getString(R.string.saf_tutorial_title));
        builder.setMessage(getString(R.string.httpserver_acquire_permission_message));
        builder.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showSAFTutorial();
            }
        });
        builder.setNegativeButton(getString(android.R.string.no), null);
        checkAcquirePermissionDialog = builder.create();
    }

    private void stopServer() {
        httpFileServerServer.stop();
        Log.d("test", "server stop");
    }

    private synchronized void releaseWakeLock() {
        if (wakeLock == null)
            return;
        wakeLock.release();
        wakeLock = null;
    }
    private synchronized void acquireWakeLock() {
        if (wakeLock != null)
            return;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        wakeLock.acquire();
    }
    private void startServer() {
        try {
            httpFileServerServer.start(5, new IHttpFileServerStateListener() {
                @Override
                public void onServerStarted() {
                    Log.d("test", "server start");
                    startButton.setVisibility(View.GONE);
                    stopButton.setVisibility(View.VISIBLE);
                    serverDescriptionView.setText(getString(R.string.httpserver_instruction));
                    WifiStateMonitor.addObserver(HttpServerActivity.this, httpFileServerServer);
                    acquireWakeLock();
                    HttpFileServerAnalyzer.serverStarted(isFromPromoteNotification() == null?
                            HttpFileServerAnalyzer.Entry.Tool
                            : HttpFileServerAnalyzer.Entry.PromoteNotification);
                }
                @Override
                public void onServerStopped() {
                    HttpFileServerAnalyzer.serverStopped();
                    startButton.setVisibility(View.VISIBLE);
                    stopButton.setVisibility(View.GONE);
                    WifiStateMonitor.removeObserver(httpFileServerServer);
                    serverDescriptionView.setText(getString(R.string.httpserver_description));
                    serverAddressView.setText("");
                    releaseWakeLock();
                }

                @Override
                public void onAddressChanged(String address) {
                    if (address == null) {
                        serverDescriptionView.setText(getString(R.string.httpserver_no_network));
                        serverAddressView.setText("");

                        // add alert dialog to notify user at first time
                        if(bNeedShowWifiSettingDialog) {
                            RemoteWiFiTurnOnDialogFragment remoteWiFiTurnOnDialogFragment = RemoteWiFiTurnOnDialogFragment.newInstance(-1);
                            remoteWiFiTurnOnDialogFragment.show(getFragmentManager(), "RemoteWiFiTurnOnDialogFragment");
                        }
                    }
                    else {
                        serverDescriptionView.setText(
                            String.format("%s %s\n\n%s",
                                getString(R.string.httpserver_connectwifiname),
                                getWifiAPName(),
                                getString(R.string.httpserver_instruction)));
                        serverAddressView.setText(address);
                    }

                    bNeedShowWifiSettingDialog = false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Observer volumeObserver = new Observer() {
        @Override
        public void update(Observable observable, Object data) {
            updateSAFPermissionUI();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        ((FileManagerApplication)getApplication()).mVolumeStateObserver.addObserver(volumeObserver);
        updateSAFPermissionUI();
    }

    private void updateSAFPermissionUI() {
        safHint.setVisibility(fileManager.needSAFPermission()? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onPause() {
        ((FileManagerApplication)getApplication()).mVolumeStateObserver.deleteObserver(volumeObserver);
        stopServer();
        super.onPause();
    }


    public void acquireExternalStoragePermission() {
        if (httpFileServerServer.isServerStarted()) {
            showCheckAcquirePermissionDialog();
        }
        else {
            showSAFTutorial();
        }
    }


    private void showSAFTutorial() {
        Intent tutorialIntent = new Intent();
        tutorialIntent.setClass(this, TutorialActivity.class);
        tutorialIntent.putExtra(TutorialActivity.TUTORIAL_SD_PERMISSION, true);
        startActivityForResult(tutorialIntent, HttpServerActivity.REQUEST_SDTUTORIAL);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SDTUTORIAL && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, EXTERNAL_STORAGE_PERMISSION_REQ);
        }else if(requestCode == EXTERNAL_STORAGE_PERMISSION_REQ && resultCode == Activity.RESULT_OK) {
            Uri treeUri = data.getData();
            Log.d(TAG, "---SAF -uri--" + treeUri.toString());
            DocumentFile rootFile = DocumentFile.fromTreeUri(this, treeUri);
            if (rootFile != null /*&& rootFile.getName().equals("MicroSD")*/) {
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                updateSAFPermissionUI();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(needShowWarningDialog()) return true;
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if(needShowWarningDialog()) return;
        super.onBackPressed();
    }

    private boolean needShowWarningDialog() {
        // http server is running and wifi is on connection
        if(httpFileServerServer.isAlive()){
            showWarningDialogForStopHttpServer();
            return true;
        }
        return false;
    }

    private void showWarningDialogForStopHttpServer(){
        new AlertDialog.Builder(this, ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(getString(R.string.file_manager))
                .setMessage(getString(R.string.warning_stophttpserver))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // leave activity
                        finish();
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
        }).create().show();
    }

    public String getWifiAPName() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return "";
        }
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null) {
            return "";
        }

        if(info.getNetworkId() != -1){
            return info.getSSID();
        }

        Method getWifiApConfiguration = null;
        try {
            getWifiApConfiguration = wifiManager.getClass().getMethod("getWifiApConfiguration", null);
            WifiConfiguration wifiConfiguration = (WifiConfiguration) getWifiApConfiguration.invoke(wifiManager, null);
            return wifiConfiguration.SSID;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return "";
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return "";
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(!ColorfulLinearLayout.isColorfulTextViewNeeded()) return;

        TextView colorfulView = (TextView) findViewById(R.id.textview_colorful);
        if(colorfulView == null) return;
        // device that api level below 19 would change the height of actionbar when rotate screen
        // need to reset colorfullayout height
        colorfulView.setHeight(ColorfulLinearLayout.getColorfulLayoutHeight(this));
    }
}
