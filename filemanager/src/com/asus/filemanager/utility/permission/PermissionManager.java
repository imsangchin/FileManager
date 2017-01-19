package com.asus.filemanager.utility.permission;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;


import com.asus.filemanager.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class PermissionManager {

    private Activity activity;
    private static final int NOTIFICATION_ID = 1;
    public static final int REQUEST_PERMISSION = 1;
    public static final int RE_REQUEST_PERMISSION = 2;

    public PermissionManager(Activity a) {
        if (a == null)
            throw new IllegalArgumentException("Activity cannot be null");
        this.activity = a;
    }

    /**
     * Internal callback
     * @param permissions Permissions list
     * @param request A request code
     */
    @TargetApi(23)
    void onReasonAccepted(ArrayList<String> permissions, int request) {
        activity.requestPermissions(permissions.toArray(new String[permissions.size()]), request);
    }

    void onReasonRejected(int request) {
        if (request == PermissionManager.REQUEST_PERMISSION) {
            activity.finish();
        }else if (request == PermissionManager.RE_REQUEST_PERMISSION){
            //currently we do nothing here, allow user continue to use this app
        }else{
            activity.finish();
        }
    }

    /**
     * Check if permissions are granted
     * @param perms Permissions list
     * @return True if all the permissions are granted, false otherwise
     */
    public boolean checkPermissions(String[] perms) {
        boolean res = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String p : perms) {
            if (PackageManager.PERMISSION_GRANTED != activity.checkSelfPermission(p)) {
                res = false;
            }
        }
        return res;
    }

    public static boolean checkPermissions(Context aContext, String[] perms) {
        boolean res = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String p : perms) {
            if (PackageManager.PERMISSION_GRANTED != aContext.checkSelfPermission(p)) {
                res = false;
            }
        }
        return res;
    }
    /**
     * Request permissions
     * @param permissionMap A permission map
     * @param requestCode A request code
     * @return True if all the permissions are granted, false otherwise
     */
    @TargetApi(23)
    public boolean requestPermissions(Map<String, Integer> permissionMap, int requestCode) {
        boolean res = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || permissionMap == null || permissionMap.size() == 0) {
            return true;
        }

        ArrayList<String> permToBeRequest = new ArrayList<>();
        ArrayList<Integer> reasonsToBeViewed = new ArrayList<>();

        for (String p : permissionMap.keySet()) {
            if (PackageManager.PERMISSION_GRANTED != activity.checkSelfPermission(p)) {
                res = false;
                permToBeRequest.add(p);
                if (activity.shouldShowRequestPermissionRationale(p) && permissionMap.get(p) != 0) {
                    reasonsToBeViewed.add(permissionMap.get(p));
                }
            }
        }
        if (!res) {
            if (reasonsToBeViewed.size() > 0) {
                /* currently, we don't show reason to user, just ask him again
                PermissionDialog newFragment = PermissionDialog.newInstance(reasonsToBeViewed, permToBeRequest,
                        requestCode);
                newFragment.show(activity.getFragmentManager(), PermissionDialog.TAG);
                */
                onReasonAccepted(permToBeRequest, requestCode);
            } else {
                onReasonAccepted(permToBeRequest, requestCode);
            }
        }
        return res;
    }

    /**
     * Helper method to build a permission map with no "reasons"
     * @param perms Permissions list
     * @return A map where the resources have always id 0
     */
    public static Map<String, Integer> generatePermissionMap(String[] perms) {
        HashMap<String, Integer> map = new HashMap<>();

        if (perms.length == 0)
            throw new IllegalArgumentException();

        for (String s : perms) {
            map.put(s, 0);
        }
        return map;
    }

    /**
     * Request permission from service or broadcast receiver
     * @param context A context
     * @param perms Permissions list
     * @param text Notification text
     * @return True if the permission is granted, false otherwise
     */
    public static boolean requestPermissions(Context context, String[] perms, @StringRes int text) {
        boolean res = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String p : perms) {
            if (PackageManager.PERMISSION_GRANTED != context.checkSelfPermission(p)) {
                res = false;
                NotificationManager manager = (NotificationManager) context.getSystemService(Context
                        .NOTIFICATION_SERVICE);
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.setData(Uri.parse("package:" + context.getPackageName()));
                PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_ONE_SHOT);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setColor(Color.RED)
                        .setSmallIcon(R.drawable.ic_notification_filemanager)
                        .setContentTitle(context.getString(R.string.saf_tutorial_title))
                        .setTicker(context.getString(text))
                        .setContentIntent(pi)
                        .setContentText(context.getString(text));
                manager.notify(NOTIFICATION_ID, mBuilder.build());
                break;
            }
        }
        return res;
    }
}
