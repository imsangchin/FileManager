package com.asus.filetransfer.http.server;

/**
 * Created by Yenju_Lai on 2015/12/7.
 */
public interface IHttpFileServerStateListener {
    void onServerStarted();
    void onServerStopped();
    void onAddressChanged(String address);
}
