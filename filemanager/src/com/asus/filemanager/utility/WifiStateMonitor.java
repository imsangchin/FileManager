package com.asus.filemanager.utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Yenju_Lai on 2016/1/13.
 */
public class WifiStateMonitor extends Observable {

    private static final String TAG = "WifiStateMonitor";
    private static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static final String WIFI_AP_IP_ADDRESS = "192.168.43.1";

    private WifiStateMonitor(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    private static WifiStateMonitor wifiStateMonitor = null;
    private Context context;
    private WifiManager wifiManager;
    private String currentIp;

    public static void addObserver(Context context, Observer observer) {
        Log.d(TAG, "addObserver");
        if (wifiStateMonitor == null)
            wifiStateMonitor = new WifiStateMonitor(context);
        if (wifiStateMonitor.countObservers() == 0)
            wifiStateMonitor.registerBroadCastReceiver();
        wifiStateMonitor.addObserver(observer);
        wifiStateMonitor.updateNetworkState();
        Log.d(TAG, "add observer:" + wifiStateMonitor.countObservers());
    }

    public static void removeObserver(Observer observer) {
        if (wifiStateMonitor == null)
            return;
        wifiStateMonitor.deleteObserver(observer);
        if (wifiStateMonitor.countObservers() == 0)
            wifiStateMonitor.unregisterBroadCastReceiver();
    }

    private void registerBroadCastReceiver(){
        IntentFilter filters = new IntentFilter();
        filters.addAction(WIFI_AP_STATE_CHANGED_ACTION);
        filters.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filters.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
//        filters.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filters.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filters.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        context.registerReceiver(mBroadcastReceiver, filters);
    }

    private void unregisterBroadCastReceiver(){
        context.unregisterReceiver(mBroadcastReceiver);
    }

    private boolean updateWifiState()
    {
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        int ipAddress = wInfo.getIpAddress();
        if (ipAddress == 0)
            return false;
        else {
            notifyIpChanged(Formatter.formatIpAddress(ipAddress/*wifiManager.getConnectionInfo().getIpAddress()*/));
            return true;
        }
    }

    private boolean updateHotspotState() {
        try{
            Method method = WifiManager.class.getMethod("isWifiApEnabled");
            if ((Boolean)method.invoke(wifiManager)) {
                notifyIpChanged(WIFI_AP_IP_ADDRESS);
                return true;
            }
        }catch(Exception e){
        }
        return false;
    }

    private synchronized void updateNetworkState() {
        if (!updateWifiState() && !updateHotspotState())
            notifyIpChanged(null);
    }

    private void notifyIpChanged(String ip) {
        currentIp = ip;
        WifiStateMonitor.this.setChanged();
        WifiStateMonitor.this.notifyObservers(currentIp);
        Log.d(TAG, "notifyIpChanged:" + currentIp);
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            updateNetworkState();
        }
    };

}
