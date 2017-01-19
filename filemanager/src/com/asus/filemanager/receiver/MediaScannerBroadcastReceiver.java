package com.asus.filemanager.receiver;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.StorageAnalyzerActivity;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.provider.MediaProviderAsyncHelper;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.permission.PermissionManager;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MediaScannerBroadcastReceiver extends BroadcastReceiver {

    private final int DEFAULT_MIN_RETENTION_PERIOD= 2;

    // sylvia 20160323, Notification setting
    private static final String KEY_LARGE_FILES = "pref_large_files_notification";
    private static final String KEY_RECENT_FILES = "pref_recent_files_notification";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;
        if (context == null)
            return;

        String action = intent.getAction();
        if (action == null)
            return;

        if (WrapEnvironment.IS_VERIZON)
            return;

        if (! (PermissionManager.checkPermissions(context,
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}))) {

            //we don't have permission, return
            return;
        }

        if (0 == action.compareToIgnoreCase("android.intent.action.MEDIA_SCANNER_FINISHED") ||
            0 == action.compareToIgnoreCase("com.asus.filemanager.check_large_file")
            ) {
            checkFiles(context);
            scheduleAlarm(context);
        }

        Log.d("MediaScannerReceiver","MediaScannerBroadcastReceiver");
    }

    public static void scheduleAlarm(Context context) {
        if (WrapEnvironment.IS_VERIZON)
            return;

        AlarmManager alarmMgr;
        PendingIntent alarmIntent;
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent anIntent = new Intent(context, MediaScannerBroadcastReceiver.class);
        anIntent.setAction("com.asus.filemanager.check_large_file");
        alarmIntent = PendingIntent.getBroadcast(context, 0, anIntent, 0);

        // If the alarm has been set, cancel it.
        if (alarmMgr!= null) {
            alarmMgr.cancel(alarmIntent);

            // Set the alarm to start at 12:00 a.m.
            // every day at scheduled time
            Calendar calendar = Calendar.getInstance();
            // if it's after or equal 9 am schedule for next day
            if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 12) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            //1000 * 60 * 60 * 24;
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);
        }
    }

    private boolean shouldShowNotification(Context context){
        if (!ItemOperationUtility.getInstance().hasSetLastRetentionNotificationTime(context))
            return true;

        long daysSentinal = ItemOperationUtility.getInstance().getMinRetentionPeriod(context, DEFAULT_MIN_RETENTION_PERIOD);
        long lastRetentionNotificationTime = ItemOperationUtility.getInstance().getLastRetentionNotificationTime(context, 0);

        long millis1 = lastRetentionNotificationTime*1000L;
        long millis2 = System.currentTimeMillis();

        long difference = millis2 - millis1 ;

        long days = TimeUnit.MILLISECONDS.toDays(difference);
        if (days >=daysSentinal)
            return true;

        return false;
    }
    private void checkFiles(Context context) {

        if (ItemOperationUtility.getInstance().hasSetLastCheckLargeFileRetentionTime(context)) {
            if (!shouldShowNotification(context))
                return;
            //check large file count
            long lastCheckTime = ItemOperationUtility.getInstance().getLastCheckLargeFileRetentionTime(context, 0);
            ArrayList<LocalVFile> recentlyAddLargeFiles = MediaProviderAsyncHelper.getFilesByTimeAndSize(
                context, lastCheckTime, false, false, FileManagerActivity.SUPPORT_LARGE_FILES_THRESHOLD
            );

            int recentlyAddedLargeFileCount = recentlyAddLargeFiles.size();
            if (recentlyAddedLargeFileCount >= FileManagerActivity.LARGE_FILES_COUNT_THRESHOLD) {
                // sylvia 20160322, large files notification
                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_LARGE_FILES, false)) {
                    return;
                }

                NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(context);
                NotificationManager mNotificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);

                Intent i = new Intent(Intent.ACTION_MAIN);
//                i.putExtra("categoryItemId", CategoryItem.LARGE_FILE);
//                i.putExtra("ga", GaPromote.PROMOTE_LARGEFILE_NOTIFICATION);
//                i.setClassName(context.getPackageName(),
//                    "com.asus.filemanager.activity.FileManagerActivity");
                /**change redircet StorageAnalyzer**/
                i.putExtra(StorageAnalyzerActivity.KEY_GA_ACTION, GaStorageAnalyzer.ACTION_ANALYZER_PAGE_LARGEFILE_NOTIFICATION);
                i.setClass(context, StorageAnalyzerActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 1, i,
                    PendingIntent.FLAG_UPDATE_CURRENT/*0*/);
                String strTitle = context.getString(R.string.file_manager);

                Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
                int height = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
                int width = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

                int icon_width = Math.min(height, width);
                icon = Bitmap.createScaledBitmap(icon, icon_width, icon_width, false);

                String aStr = context.getString(R.string.retention_notification_large_file);
                aStr = String.format(aStr,recentlyAddedLargeFileCount);

                mNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle(mNotificationBuilder)
                    .bigText(aStr)
                    .setBigContentTitle(strTitle))
                    .setContentTitle(strTitle)
                    .setContentIntent(pendingIntent)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setContentText(aStr)
                        //.setColor(0xffffd73e)
                    .setLargeIcon(icon)
                    .setSmallIcon(R.drawable.ic_notification_filemanager);


                Notification aNotification = mNotificationBuilder.build();
                mNotificationManager.notify("retention", 10, aNotification);

                Long currentTime = System.currentTimeMillis() / 1000; // in seconds
                ItemOperationUtility.getInstance().setLastCheckLargeFileRetentionTime(context, currentTime);
                ItemOperationUtility.getInstance().setLastRetentionNotificationTime(context, currentTime);
            } else {
                if (!shouldShowNotification(context))
                    return;

                // sylvia 20160322, recent files notification
                if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_RECENT_FILES, false)) {
                    return;
                }

                Calendar c = Calendar.getInstance();
                c.add(Calendar.DATE, -3 /* last 3 days*/);
                long beforeTime = c.getTimeInMillis() / 1000; // in seconds
                long firstLauchTime = ItemOperationUtility.getInstance().getLastCheckRecentFileRetentionTime(context, 0);

                long size = MediaProviderAsyncHelper.countTtlFilesSizeByTime(context, Math.max(beforeTime,firstLauchTime), false, false);
                if (size > FileManagerActivity.SUPPORT_FILES_SIZE_THRESHOLD) {
                    NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(context);
                    NotificationManager mNotificationManager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);

                    Intent i = new Intent(Intent.ACTION_MAIN);
//                    i.putExtra("categoryItemId", CategoryItem.RECENT);
//                    i.putExtra("ga", GaPromote.PROMOTE_RECENTFILE_NOTIFICATION);
//                    i.setClassName(context.getPackageName(),
//                        "com.asus.filemanager.activity.FileManagerActivity");
                    /**change redircet StorageAnalyzer**/
                    i.putExtra(StorageAnalyzerActivity.KEY_GA_ACTION, GaStorageAnalyzer.ACTION_ANALYZER_PAGE_RECENTFILE_NOTIFICATION);
                    i.setClass(context, StorageAnalyzerActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 1, i,
                        PendingIntent.FLAG_UPDATE_CURRENT/*0*/);
                    String strTitle = context.getString(R.string.file_manager);

                    Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
                    int height = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
                    int width = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

                    int icon_width = Math.min(height, width);
                    icon = Bitmap.createScaledBitmap(icon, icon_width, icon_width, false);

                    String aStr = context.getString(R.string.retention_notification_recent_file);
                    aStr = String.format(aStr, FileUtility.bytes2String(context,size,1));

                    mNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle(mNotificationBuilder)
                        .bigText(aStr)
                        .setBigContentTitle(strTitle))
                        .setContentTitle(strTitle)
                        .setContentIntent(pendingIntent)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setContentText(aStr)
                            //.setColor(0xffffd73e)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.drawable.ic_notification_filemanager);

                    Notification aNotification = mNotificationBuilder.build();
                    mNotificationManager.notify("retention", 10, aNotification);

                    Long currentTime = System.currentTimeMillis() / 1000; // in seconds
                    ItemOperationUtility.getInstance().setLastCheckRecentFileRetentionTime(context, currentTime);
                    ItemOperationUtility.getInstance().setLastRetentionNotificationTime(context, currentTime);
                }
            }
        } else {
            Long currentTime = System.currentTimeMillis() / 1000; // in seconds
            ItemOperationUtility.getInstance().setLastCheckLargeFileRetentionTime(context, currentTime);
        }
    }
}
