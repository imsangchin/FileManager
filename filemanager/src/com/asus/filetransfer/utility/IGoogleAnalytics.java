package com.asus.filetransfer.utility;

/**
 * Created by Yenju_Lai on 2016/1/28.
 */
public interface IGoogleAnalytics {

    void sendEvents(String category, String action, String label, Long value);
    void sendTiming(String category, String variable, String label, Long value);
}
