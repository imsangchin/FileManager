package com.asus.filemanager.receiver;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.ga.GaPromote;
import com.asus.filemanager.utility.permission.PermissionDialog;
import com.asus.filemanager.utility.permission.PermissionManager;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.util.HashMap;
import java.util.Map;

public class USBReceiver extends BroadcastReceiver{

	private Context mContext;
	private final int DEFAULT_RETENTION_USB_MOD = 5;
    private static final String KEY_WIRELESS_TRANSFER = "pref_wireless_transfer";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null)
			return;
		if (context == null)
			return;
		if(!intent.getAction().equals("android.hardware.usb.action.USB_STATE")){
			return;
		}
        if (intent.getExtras().getBoolean("connected") == false){
            return;
        }
		if (intent.getExtras().getBoolean("configured") == false){
			return;
		}

        // sylvia 20160322, wireless transfer notification
        Boolean enableWirelessNotify = PreferenceManager.getDefaultSharedPreferences(context).
                getBoolean(KEY_WIRELESS_TRANSFER, true);
        if (!enableWirelessNotify) {
            return;
        }

		if (WrapEnvironment.IS_VERIZON)
			return;

		Log.d("USB_PLUG",intent.getExtras().toString());
		for (String key: intent.getExtras().keySet())
		{
			Log.d("USB_PLUG", key + " = \"" + intent.getExtras().get(key) + "\"");
		}
		boolean bShowNotification = false;
		try {
			SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefsFile", 0);
			long retention_usb_mod = sharedPreferences.getLong("RETENTION_USB_MOD", DEFAULT_RETENTION_USB_MOD);
			long retention_usb_cnt = sharedPreferences.getLong("RETENTION_USB_CNT", 0);

			if (retention_usb_mod == 0) {
				bShowNotification = true;
			}else{
				if ((retention_usb_cnt + retention_usb_mod) % retention_usb_mod == 0){
					sharedPreferences.edit().putLong("RETENTION_USB_CNT", 1).commit();
					bShowNotification = true;
				}else{
					sharedPreferences.edit().putLong("RETENTION_USB_CNT", retention_usb_cnt+1).commit();
				}
			}
		}catch (Throwable ignore){

		}

		if (!bShowNotification)//skip this time
			return;

		android.support.v4.app.NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(context);
		NotificationManager mNotificationManager = (NotificationManager) context
			.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent i = new Intent();
		try {
			i.setClass(context, Class.forName("com.asus.filemanager.activity.HttpServerActivity"));
		}catch (ClassNotFoundException clsNotFound){
			//we can't find http server class, return now..
			return;
		}

		if (! (PermissionManager.checkPermissions(context,
			new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}))) {

			i = new Intent(context, FileManagerActivity.class);
			i.addCategory(Intent.CATEGORY_LAUNCHER)
				.setAction(Intent.ACTION_MAIN);
		}

		i.addCategory(Intent.CATEGORY_LAUNCHER)
			.setAction(Intent.ACTION_MAIN);
		i.putExtra("ga", GaPromote.PROMOTE_USB_NOTIFICATION);
		PendingIntent pendingIntent = PendingIntent.getActivity(
			context, 1, i,
			PendingIntent.FLAG_UPDATE_CURRENT/*0*/);
		String strTitle = context.getString(R.string.file_manager);
		strTitle = context.getString(R.string.promotion_notification_title);
		String aStr = context.getString(R.string.promotion_notification_content);

		Bitmap icon= BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
		int height = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
		int width = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

		int icon_width = Math.min(height, width);
		icon = Bitmap.createScaledBitmap(icon, icon_width, icon_width, false);

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
		mNotificationManager.notify("update", 2, aNotification);
	}

}
