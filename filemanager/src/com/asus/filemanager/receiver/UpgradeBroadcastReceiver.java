package com.asus.filemanager.receiver;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.wrap.WrapEnvironment;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static com.asus.filemanager.utility.FileUtility.*;

public class UpgradeBroadcastReceiver extends BroadcastReceiver {

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

        String aStr = null;
        int id =0;
        if (action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
        	Log.d("upgrade","ACTION_PACKAGE_REPLACED");
        	Uri aData = intent.getData();
        	if(null != aData){
        		String package_name = aData.getSchemeSpecificPart();
        		Log.d("upgrade", package_name);
        		if (package_name.equals(context.getPackageName()) && isFirstStartup(context)){
        			aStr = context.getResources().getString(R.string.upgrade_notification_content);
        		}
            } 
        	id = 1;
        }else if (action.equals(Intent.ACTION_PACKAGE_ADDED)){
        	Log.d("upgrade","ACTION_PACKAGE_ADDED");
        	aStr = null;//"I have been added";
        	id = 2;
        }else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)){
        	Log.d("upgrade","ACTION_PACKAGE_REMOVED");
        	aStr = null;//"I have been removed";
        	id = 3;
        }
        if (null != aStr){
        	android.support.v4.app.NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(context);
        	NotificationManager mNotificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            Intent i = new Intent(context, FileManagerActivity.class);
            i.addCategory(Intent.CATEGORY_LAUNCHER)
                .setAction(Intent.ACTION_MAIN);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                            context, 1, i,
                            PendingIntent.FLAG_UPDATE_CURRENT/*0*/);
            String strTitle = context.getString(R.string.file_manager);
            strTitle = context.getString(R.string.upgrade_notification_title);
            
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
            mNotificationManager.notify("update", id, aNotification);	
        }
    }
}
