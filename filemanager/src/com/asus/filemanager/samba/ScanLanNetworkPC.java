package com.asus.filemanager.samba;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.samba.provider.PcInfoDbHelper;
import com.asus.filemanager.samba.util.SambaUtils;
import com.asus.filemanager.utility.ToastUtility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import jcifs.netbios.NbtAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public final class ScanLanNetworkPC {

    private final static String TAG = "ScanLanNetworkPC";
    WifiAdmin mAdmin;
    boolean mShowLastLoginAccount = true;
    private boolean mHasSavedPc = false;
    private static boolean clearAccountsInfo = false;
    private static ScanLanNetworkPC mScanWorkTask = null;
    private ArrayList<SambaItem> mHostIpList =  new ArrayList<>();
    private Timer mTimer = null;
    private Context mActivity = null;
//  private TimerTask mTask = null;
    private static final int UPDATE_RESULT = 111;
    private static final int HIDE_REFRESH = 112;
    private static final int SHOW_TIME_OUT = 113;
    private static final int NO_RESULT_FINISH = 114;
    private static final int CONNECT_ERROR = 115;

//  private final String HOST_IP = "host_ipaddress";
//  private final String HOST_NAME = "host_name";
    private static final String SAMBA_ITEM = "smaba_item";
    private String mCurrentIp = "";
    private String mSavedIp = "0.0.0.0";

    private static final String NAME_TAG = "<20>";
    private static final String SPECIAL_NAME_TAG = "<00>";
    private static final String ACTIVE_TAG = "<ACTIVE>";
//  private final String SHARE_TAG = "<1e>";
    private static final String PRINT_TAG = "<PERMANENT>";
//  private final String MAC_PRINT_TAG ="00-00-00-00-00-00";
    private static final String NMBLOOKUP = "cmd/nmblookup";
    private static final String X86_NMBLOOKUP = "cmd/x86/nmblookup";
    private static final String CMD_DIR = "data/data/com.asus.filemanager/cmd";
    private static final String CMD_FILE = CMD_DIR + "/nmblookup";
    private static final String RUN_CHMOD_CMD = "chmod 744 data/data/com.asus.filemanager/cmd/nmblookup";
    private static final String RUN_NMBLOOKUP_CMD = "./data/data/com.asus.filemanager/cmd/nmblookup -A ";
    private static final String GET_SYS_PROP = "getprop ro.product.cpu.abi";

    private ScanLanNetworkPC(Context context) {
        mAdmin = new WifiAdmin(context);
        copyCmdFile(context);
        mActivity = context;
    }

    public static ScanLanNetworkPC getInstance(Context context) {
        if(mScanWorkTask == null) {
            mScanWorkTask = new ScanLanNetworkPC(context);
        }
        return mScanWorkTask;
    }

    public void startScanPc() {
        mHostIpList.clear();
        String currentIp = mAdmin.getCurrentIPAddress();
        Log.d(TAG,"currentIp== " + currentIp);
        mCurrentIp = currentIp;
        int index = currentIp.lastIndexOf(".");
        String subNet = currentIp.substring(0, index);
        mShowLastLoginAccount = true;
        startScanThreads(subNet);
        initTimer();
    }

    public void startScanPc(String url)
    {
        mHostIpList.clear();
        mShowLastLoginAccount = true;
        new ScanLanPcThread(url).start();
    }

    private final Handler ScanResultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
            switch(msg.what) {
                case UPDATE_RESULT:
                    Bundle data = msg.getData();
                    SambaItem item = data.getParcelable(SAMBA_ITEM);
                    if(mShowLastLoginAccount && mHasSavedPc) {
                        mHostIpList.add(0, item);
                        mShowLastLoginAccount = false;
                    } else if(item != null && !TextUtils.isEmpty(item.getAccount())) {
                        if(mHasSavedPc && !TextUtils.isEmpty(mHostIpList.get(0).getAccount())) {
                            mHostIpList.add(1, item);
                        } else {
                            mHostIpList.add(0, item);
                        }
                    } else {
                        mHostIpList.add(item);
                    }
                    if(SambaFileUtility.updateHostIp && !sambaFileUtility.getIsLoginProcessing()) {
                        sambaFileUtility.refresFileList(true);
                        updateListAdapter();
                    }
                    break;
                case HIDE_REFRESH:
                    if(!sambaFileUtility.getIsLoginProcessing()) {
                        sambaFileUtility.refresFileList(true);
                    }
                    sambaFileUtility.hideRefresh();
                    SambaFileUtility.ScanFinish = true;
                    break;
                case SHOW_TIME_OUT:
                    sambaFileUtility.refresFileList(true);
                    sambaFileUtility.hideRefresh();
                    ToastUtility.show(mActivity, R.string.scan_time_out, Toast.LENGTH_LONG);
                    break;
                case NO_RESULT_FINISH:
                    updateListAdapter();
                    break;
                case CONNECT_ERROR:
                    if(!sambaFileUtility.getIsLoginProcessing()) {
                        sambaFileUtility.refresFileList(true);
                    }
                    sambaFileUtility.hideRefresh();
                    SambaFileUtility.ScanFinish = true;

                    String errMessage = (String) msg.obj;
                    if(errMessage != null && mActivity != null)
                    {
                        ToastUtility.show(mActivity, errMessage);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void startScanThreads(String subIp) {
        ScanThread mScan = new ScanThread(1, 255, subIp);
        mScan.start();
    }

    class ScanThread extends Thread {
        int startIp;
        int endIp;
        String subNet;

        public ScanThread(int start, int end, String subnet) {
            startIp = start;
            endIp = end;
            subNet = subnet;
        }

        public void run() {
            Log.d(TAG,"start===>");
            updateLastLoginAccountToFront();

            InitSystem initSystem = new InitSystem(subNet, startIp, endIp);
            Thread thread = new Thread(initSystem);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            SambaFileUtility.ScanFinish = true;
            cancelTimer();
            if(SambaFileUtility.updateHostIp) {
                hideRefreshProgress();
                if(mHostIpList.size() == 0) {
                    Message msg = new Message();
                    msg.what = NO_RESULT_FINISH;
                    ScanResultHandler.sendMessage(msg);
                }
            }

            Log.d(TAG,"----Scan Finish PC Count=----" + mHostIpList.size());
        }
    }

    private synchronized void UpdateHostIpAddress(SambaItem item) {
        Message msg = new Message();
        msg.what = UPDATE_RESULT;
        Bundle data = new Bundle();
        data.putParcelable(SAMBA_ITEM, item);
        msg.setData(data);
        ScanResultHandler.sendMessage(msg);
    }

    private synchronized void hideRefreshProgress() {
        Message msg = new Message();
        msg.what = HIDE_REFRESH;
        ScanResultHandler.sendMessage(msg);
    }

    public synchronized void updateListAdapter() {
        if(clearAccountsInfo) {
            if(mHostIpList.size() > 0) {
                for(int num = 0; num < mHostIpList.size(); num ++) {
                    mHostIpList.get(num).clearAccount();
                    mHostIpList.get(num).clearPassword();
                }
            }
            clearAccountsInfo = false;
        }
        SambaFileUtility.getInstance(null).updateListViewByHostPc(mHostIpList);
    }

    private synchronized void updateLastLoginAccountToFront() {
        mHasSavedPc = false;
        if(mShowLastLoginAccount) {
            SambaFileUtility sfu = SambaFileUtility.getInstance(null);
            String LastIp = sfu.getTheLastTimeLoginIp();
            //check if last login PC is still in this LAN
            if(sfu.isIpSameArea(LastIp)) {
                mSavedIp = LastIp;
                String pcName = getPcName(LastIp);
                if(!TextUtils.isEmpty(pcName)) {
                    mHasSavedPc = true;
                    UpdateHostIpAddress(PcInfoDbHelper.queryAccountInfo(pcName, LastIp));
                }
            }
        }
    }

    private String getPcName(String ip) {
        String pcName = "";
        /*
        int end;
        String subNameString;
        String cmdResult = runLookupCmd(ip);
        if(!TextUtils.isEmpty(cmdResult)) {
            int ipStart = cmdResult.indexOf(ip);
            String subString = cmdResult.substring(ipStart);
            if(subString.contains(NAME_TAG)) {
                end = subString.indexOf(NAME_TAG);
                subNameString = subString.substring(ip.length(), end);
                if(subNameString.contains(ACTIVE_TAG)) {
                    int begin = subNameString.lastIndexOf(ACTIVE_TAG);
                    int beginNum = begin + ACTIVE_TAG.length();
                    String nameString = subNameString.substring(beginNum);
                    pcName = nameString.trim();
                } else {
                    pcName = subNameString.trim();
                }
            } else {
                end = subString.indexOf(SPECIAL_NAME_TAG);
                subNameString = subString.substring(ip.length(), end);
                pcName = subNameString.trim();
            }
        }
        */

        try
        {
            NbtAddress hostAddress = NbtAddress.getByName(ip);
            if(hostAddress.isActive() && !hostAddress.isGroupAddress())
            {
                pcName = hostAddress.getHostName();
            }
        }
        catch (UnknownHostException e)
        {
            // Skip handling exception because there may be
            // many ip address that can not resolve.
        }
        return pcName;
    }

    private String runLookupCmd(String ip) {
        String result = "";
        try {
            Process process = Runtime.getRuntime().exec(RUN_NMBLOOKUP_CMD + ip);
            BufferedReader  reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];

            // StringBuilder is thread-safe.
            // StringBuffer output = new StringBuffer();
            StringBuilder output = new StringBuilder();

            while((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            //process.waitFor();

            Log.d(TAG,"----Get result----" + output.toString());

            if(output.toString().contains("No reply") || output.toString().contains(PRINT_TAG)) {
                    //|| output.toString().contains(MAC_PRINT_TAG)){
            } else {
                result = output.toString();
            }

            process.destroy();
        } catch (IOException e) {
            Log.d(TAG,"---------------" + e.toString());
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void copyCmdFile(Context context) {
        AssetManager am = context.getResources().getAssets();
        File cmdDir = new File(CMD_DIR);
        if(!cmdDir.exists() || !cmdDir.isDirectory()) {
            cmdDir.mkdir();
        }
        File cmdFile = new File(CMD_FILE);
        if(cmdFile.exists()) {
            cmdFile.delete();
        }
        if(!cmdFile.exists()) {
            boolean success;
            try {
                success = cmdFile.createNewFile();
                if(success) {
                    InputStream is;
                    try {
                        if(isX86Cpu()) {
                            is = am.open(X86_NMBLOOKUP);
                        } else {
                            is = am.open(NMBLOOKUP);
                        }
                        BufferedInputStream bis = new BufferedInputStream(is);
                        FileOutputStream fos = new FileOutputStream(cmdFile);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        final int buflen = 32768;
                        byte[] buffer = new byte[buflen];
                        int rd;
                        try {
                            rd = bis.read(buffer, 0, buflen);
                            while (rd != -1)
                            {
                                bos.write(buffer, 0, rd);
                                rd = bis.read(buffer, 0, buflen);
                            }
                            bos.flush();
                        } finally {
                            bis.close();
                            bos.close();
                        }
                        Runtime.getRuntime().exec(RUN_CHMOD_CMD);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private class InitSystem implements Runnable {

        private volatile ArrayList<Thread> arrayThread = new ArrayList<>();

        private final int MAXTHREADNUM = 60;

        private int threadNumNow = 0;

        private volatile ArrayList<String> arrayIP;

        private InitSystem(String subNet,int start,int end) {
            arrayIP = new ArrayList<>();
            for (int i = start; i < end; i++) {
                String ip = subNet + "." + i;
                if(!mCurrentIp.equals(ip)) {
                    arrayIP.add(ip);
                }
            }
        }

        public void run() {
            synchronized (this) {
                try {
                    while (arrayIP.size() > 0) { //&& SambaFileUtility.updateHostIp) {
                        while (threadNumNow >= MAXTHREADNUM){//&& SambaFileUtility.updateHostIp) {
                            Log.d(TAG,"over ======Max threads,stop now----");
                            for (Thread thread : arrayThread) {
                                if (!thread.getState().equals(Thread.State.TERMINATED)) {
                                    thread.join();
                                }
                                --threadNumNow;
                            }
                            arrayThread = new ArrayList<>();
                        }
//                      if(SambaFileUtility.updateHostIp){
                        Thread thread = new Thread(new InnerClass(arrayIP.remove(0)));
                        thread.start();
                        threadNumNow++;
                        arrayThread.add(thread);
                        if(arrayIP.size() == 0) {
                            for (Thread threadLeft : arrayThread) {
                                if (!threadLeft.getState().equals(Thread.State.TERMINATED)) {
                                    threadLeft.join();
                                }
                             --threadNumNow;
                            }
                        }
//                      }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private class InnerClass implements Runnable {
            private String ip;

            private InnerClass(String ip) {
                this.ip = ip;
            }
            public void run() {
//              if(!SambaFileUtility.updateHostIp){
//                  return;
//              }
                synchronized (this) {
                    if(mHasSavedPc && mSavedIp.equals(ip)) {
                        return;
                    }
                    String pcName = getPcName(ip);
                    if(!TextUtils.isEmpty(pcName)) {
                        UpdateHostIpAddress(PcInfoDbHelper.queryAccountInfo(pcName, ip));
                    }
                }
            }
        }
    }

    private void initTimer() {
        if(mTimer != null) {
            mTimer = null;
            mTimer = new Timer();
        }

        mTimer = new Timer();
        TimerTask mTask = new TimerTask() {

            @Override
            public void run() {
                Message msg = new Message();
                msg.what = SHOW_TIME_OUT;
                ScanResultHandler.sendMessage(msg);
            }
        };
        mTimer.schedule(mTask, 60000);
    }
    
    private void cancelTimer() {
        mTimer.cancel();
    }

    private boolean isX86Cpu() {
        boolean result = false;
        try {
            Process process = Runtime.getRuntime().exec(GET_SYS_PROP);
            BufferedReader  reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuilder output = new StringBuilder();
            while((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            if(output.toString().contains("x86")) {
                result = true;
            }
        } catch (IOException e) {
            Log.d(TAG,"-------get sys prop error------" + e.toString());
            e.printStackTrace();
        }
        return result;
    }

    public void clearSavedAccounts() {
        clearAccountsInfo = true;
        updateListAdapter();
    }

    public void updateLastLoginAccount() {
        Log.d(TAG,"=updateLastLoginAccount=");
        SambaFileUtility sfu = SambaFileUtility.getInstance(null);
        String ip = sfu.getTheLastTimeLoginIp();

        for(int i = 0; i < mHostIpList.size(); i++) {
            SambaItem item = mHostIpList.get(i);
            if(item.getIpAddress().equals(ip)) {
                item = PcInfoDbHelper.queryAccountInfo(item.getPcName(), ip);
                mHostIpList.set(i, item);
            }
        }
    }

    public boolean isNeedToRescanPc() {
        return mHostIpList.size() == 0;
    }

    /**
     * Thread task to find connected host using jcifs library.
     */
    class ScanLanPcThread extends Thread {

        /**
         * The desired url to connect network device.
         */
        private String mUrl;

        public ScanLanPcThread(String url)
        {
            mUrl = url;
        }

        @Override
        public void run()
        {
            SmbFile smbRootFile;
            SmbFile[] smbFileGroups;
            try
            {
                updateLastLoginAccountToFront();

                jcifs.Config.registerSmbURLHandler();
                smbRootFile = new SmbFile(mUrl, NtlmPasswordAuthentication.ANONYMOUS);
                if(smbRootFile.getType() == SmbFile.TYPE_WORKGROUP)
                {
                    smbFileGroups = smbRootFile.listFiles();
                    updateConnectedItems(smbFileGroups);
                    hideRefreshProgress();
                    SambaFileUtility.ScanFinish = true;
                }
                else
                {
                    // Connect workgroup failed.
                    Message msg = new Message();
                    msg.what = HIDE_REFRESH;
                    ScanResultHandler.sendMessage(msg);
                }
            }
            catch (MalformedURLException e)
            {
                Log.e(TAG, "ScanLanPcThread MalformedURLException: " + e.toString());
                e.printStackTrace();
            }
            catch (SmbException e)
            {
                Log.e(TAG, "ScanLanPcThread SmbException: " + e.toString());
                Log.e(TAG, "ScanLanPcThread SmbException status: " + SambaUtils.convertSmbNtStatus(e.getNtStatus()));
                e.printStackTrace();

                try
                {
                    // Try to do the same task again, but not use anonymous login.
                    smbRootFile = new SmbFile(mUrl);
                    smbFileGroups = smbRootFile.listFiles();
                    updateConnectedItems(smbFileGroups);
                    hideRefreshProgress();
                    SambaFileUtility.ScanFinish = true;
                }
                catch (MalformedURLException e1)
                {
                    Log.e(TAG, "ScanLanPcThread MalformedURLException2: " + e1.toString());
                    e1.printStackTrace();
                }
                catch (SmbException e1)
                {
                    String errMessage = SambaUtils.convertSmbNtStatus(e1.getNtStatus());
                    Log.e(TAG, "ScanLanPcThread SmbException2: " + e1.toString());
                    Log.e(TAG, "ScanLanPcThread SmbException again, status: " + errMessage);
                    e1.printStackTrace();

                    Message msg = new Message();
                    msg.what = CONNECT_ERROR;
                    msg.obj = errMessage;
                    ScanResultHandler.sendMessage(msg);
                }
            }
        }

        /**
         * Store all scan samba workgroup or server if exists.
         *
         * @param smbFiles A list contains all connected samba workgroup or server.
         */
        private void updateConnectedItems(@Nullable SmbFile[] smbFiles)
        {
            if(smbFiles == null)
            {
                return;
            }

            for (SmbFile smbFile : smbFiles)
            {
                String pcName = SambaUtils.getServerNameWithoutLastSlash(smbFile.getName());
                String url = smbFile.getURL().toString();

                if(!TextUtils.isEmpty(pcName))
                {
                    UpdateHostIpAddress(PcInfoDbHelper.queryAccountInfo(pcName, url));
                }
            }
        }
    }
}
