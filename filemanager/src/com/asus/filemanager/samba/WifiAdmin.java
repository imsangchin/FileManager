package com.asus.filemanager.samba;


import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.List;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;

public class WifiAdmin {
	
    private WifiManager mWifiManager;
    WifiLock mWifiLock;
    private Context mContext;
    private static final String TAG = "WifiAdmin";
    
    public WifiAdmin(Context context){
    	mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    	mContext = context;
    }
    
    public boolean openWifi(){
    	boolean result = true;
    	if(!mWifiManager.isWifiEnabled()){
    		result = mWifiManager.setWifiEnabled(true);
    	}
    	return result;
    }
    
    public boolean isWifiOpen(){
    	return mWifiManager.isWifiEnabled();
    }
    
    public String getCurrentSSID(){
    	WifiInfo info = mWifiManager.getConnectionInfo();
    	String SSID = info.getSSID();
    	return SSID;
    }

    public String getCurrentIPAddress() {
        /*
        WifiInfo info = mWifiManager.getConnectionInfo();
        String ipaddress = Formatter.formatIpAddress(info.getIpAddress());
        // if(ipaddress.equals("0.0.0.0")){
        //   ipaddress = getLocalIpAddress();
        // }
        */

        // Use new logic, can handle both IPv4  and IPv6.
        int ipAddress = mWifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN))
        {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
        String address;
        try
        {
            address = InetAddress.getByAddress(ipByteArray).getHostAddress();
        }
        catch(UnknownHostException ex)
        {
            Log.e(TAG, "Unable to get host address.");
            address = null;
        }

        return address;
    }
    
    public int checkState(){
    	return mWifiManager.getWifiState();
    }
    
    public boolean addNetWork(WifiConfiguration config){
    	boolean result;
    	int id = mWifiManager.addNetwork(config);
    	result = mWifiManager.enableNetwork(id, true);
    	return result;
    }
    
    public void disconnectWifi(int netID){
    	mWifiManager.disableNetwork(netID);
    	mWifiManager.disconnect();
    }
    
    public boolean disconnect(){
    	return mWifiManager.disconnect();
    }
    
    private WifiConfiguration IsExist(String SSID)   
    {   
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks(); 
           if(existingConfigs != null){
               for (WifiConfiguration existingConfig : existingConfigs)    
               {  
                 if (existingConfig.SSID.equals("\""+SSID+"\""))   
                 {   
                     return existingConfig;   
                 }   
               }
           }
        return null;    
    } 
    
    public WifiConfiguration CreateWifiInfo(String SSID,String Password,int Type){
    	WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();  
        config.allowedGroupCiphers.clear();  
        config.allowedKeyManagement.clear();  
        config.allowedPairwiseCiphers.clear();  
        config.allowedProtocols.clear();
        
        config.SSID = "\"" + SSID + "\""; 
        WifiConfiguration tempConfig = IsExist(SSID);
        if(tempConfig != null){
        	mWifiManager.removeNetwork(tempConfig.networkId);
        }
        
        if(Type == 0){
        	config.wepKeys[0] = "";
        	config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        	config.wepTxKeyIndex = 0;
        }else if(Type == 1){
            config.hiddenSSID = true; 
            int length = Password.length();
			if ((length == 10 || length == 26 || length == 58)
					&& Password.matches("[0-9A-Fa-f]*")) {
				config.wepKeys[0] = Password;
			} else {
				config.wepKeys[0] = '"' + Password + '"';
			}
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);  
        }else{
            config.preSharedKey = "\""+Password+"\"";  
            config.hiddenSSID = true;    
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);    
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);                          
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);                          
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);                      
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP); 
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP); 
            config.status = WifiConfiguration.Status.ENABLED; 
        }
        
        
    	return config;
    }

/*    
    private String getLocalIpAddress(){
    	String localIp = "";
    	String s = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.ETHERNET_STATIC_NETMASK);
    	if(s == null){
    		Log.d(TAG, "Netmask field is empty, not need to check!");
    		return localIp;
    	}
    	
        if (validateIpConfigFields(s)) {
            Log.d(TAG, "Netmask field is ip format, not need to convert!");
            return localIp;
        }
	
    	int netmaskInt = NetworkUtils.prefixLengthToNetmaskInt(Integer.valueOf(s));

        InetAddress inetAddress = NetworkUtils.intToInetAddress(netmaskInt);
        localIp = inetAddress.getHostAddress();
        Log.d(TAG, "convert done, IP is " + localIp);
        return localIp;
    }
*/    

    /* remove unused file
    private boolean validateIpConfigFields(String ip) {
        try {
            NetworkUtils.numericToInetAddress(ip);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
    */
}
