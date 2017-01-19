package com.asus.filetransfer.utility;

import android.content.Context;

import com.asus.filemanager.activity.HttpServerActivity;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Yenju_Lai on 2016/1/28.
 */
public class HttpFileServerAnalyzer {

    public enum Entry {
        Tool,
        PromoteNotification
    }

    public static final String USAGE_TIME = "UsageTime";

    private static IGoogleAnalytics mockGoogleAnalytics;
    private static IGoogleAnalytics gaFileTransfer;

    private static HttpFileServerAnalyzer analyzer;

    private HashMap<HttpServerEvents, Long> collectedData = new HashMap<>();
    private Entry entry;
    private Date startTime;

    private HttpFileServerAnalyzer(Entry entry) {
        this.entry = entry;
        this.startTime = new Date();
    }

    protected static void setMockInstance(IGoogleAnalytics mockGaFileTransfer) {
        mockGoogleAnalytics = mockGaFileTransfer;
    }

    protected static void init(Context context) {
        gaFileTransfer = mockGoogleAnalytics == null? new GaFileTransfer(context) : mockGoogleAnalytics;
    }

    public static void init(Context context, Entry entry) {
        init(context);
        gaFileTransfer.sendEvents("LaunchActivity", entry.name(), null, 0L);
    }

    public static void deInit() {
        gaFileTransfer = null;
    }

    public static void serverStarted(Entry entry) {
        analyzer = new HttpFileServerAnalyzer(entry);
    }

    public static void serverStopped() {
        if (analyzer == null)
            return;
        analyzer.sendAllCollectedData();
        analyzer = null;
    }

    public static void commandExecuted(HttpServerEvents event) {
        if (analyzer == null)
            return;
        analyzer.pushEvent(event);
    }

    private void pushEvent(HttpServerEvents event) {
        collectedData.put(event, collectedData.containsKey(event)? (collectedData.get(event) + 1) : 1);
    }


    private void sendServerUsageTime() {
        long usageTime = (new Date().getTime() - startTime.getTime());
        String label = null;
        gaFileTransfer.sendTiming(USAGE_TIME, entry.name(), label, usageTime);
    }

    private void sendAllCollectedData() {
        if (gaFileTransfer == null)
            return;
        sendServerUsageTime();
        Iterator iterator = collectedData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            HttpServerEvents event = (HttpServerEvents)entry.getKey();
            long eventCount = (Long)entry.getValue();
            gaFileTransfer.sendEvents(event.getCatalog(), event.getAction().toString(), event.getLabel(), eventCount);
        }
    }
}
