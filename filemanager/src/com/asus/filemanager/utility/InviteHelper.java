package com.asus.filemanager.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.asus.filemanager.R;
import com.asus.lite.facebook.Facebook;
import com.google.android.gms.appinvite.AppInviteInvitation;

import android.R.string;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.Html;
import com.asus.filemanager.wrap.WrapEnvironment;

public class InviteHelper {

    private static Intent generateCustomChooserIntent(Intent prototype, String[] forbiddenChoices, String profileDisplayName, Context aContext){
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        List<HashMap<String, String>> intentMetaInfo = new ArrayList<HashMap<String, String>>();
        Intent chooserIntent;

        Intent dummy = new Intent(prototype.getAction());
        dummy.setType(prototype.getType());
        List<ResolveInfo> resInfo = aContext.getPackageManager().queryIntentActivities(dummy,0);

        if (!resInfo.isEmpty()){
            for (ResolveInfo resolveInfo : resInfo){
                if (resolveInfo.activityInfo == null || 
                		Arrays.asList(forbiddenChoices).contains(resolveInfo.activityInfo.packageName))
                    continue;
                //Get all the posible sharers
                HashMap<String, String> info = new HashMap<String, String>();
                info.put("packageName", resolveInfo.activityInfo.packageName);
                info.put("className", resolveInfo.activityInfo.name);
                String appName = String.valueOf(resolveInfo.activityInfo
                        .loadLabel(aContext.getPackageManager()));
                info.put("simpleName", appName);
                intentMetaInfo.add(info);
            }

            if (!intentMetaInfo.isEmpty())
            {
                // sorting for nice readability
                Collections.sort(intentMetaInfo,
                        new Comparator<HashMap<String, String>>()
                        {
                            @Override public int compare(
                                    HashMap<String, String> map,
                                    HashMap<String, String> map2)
                            {
                                return map.get("simpleName").compareTo(
                                        map2.get("simpleName"));
                            }
                        });

                // create the custom intent list
                for (HashMap<String, String> metaInfo : intentMetaInfo)
                {
                    Intent targetedShareIntent = (Intent) prototype.clone();
                    targetedShareIntent.setPackage(metaInfo.get("packageName"));
                    targetedShareIntent.setClassName(
                            metaInfo.get("packageName"),
                            metaInfo.get("className"));
                    setContent(metaInfo.get("packageName"), targetedShareIntent, profileDisplayName,false, aContext);
                    targetedShareIntents.add(targetedShareIntent);
                }
                String shareVia = aContext.getString(R.string.tell_friend);
                chooserIntent = Intent.createChooser(targetedShareIntents
                        .remove(targetedShareIntents.size() - 1), shareVia);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        targetedShareIntents.toArray(new Parcelable[] {}));
                return chooserIntent;
            }
        }

        return Intent.createChooser(prototype, aContext.getString(R.string.inviteContentTitle));
    }

    private static Intent generateCustomChooserIntentWhitelist(Intent prototype, String[] whitelsit, String[] blacklist, String profileDisplayName, Context aContext){
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        List<HashMap<String, String>> intentMetaInfo = new ArrayList<HashMap<String, String>>();
        ArrayList<String> intentPackageName = new ArrayList<String>();
        ArrayList<String> rfc822PackageName = new ArrayList<String>();
        Intent chooserIntent;

        Intent dummy = new Intent(prototype.getAction());
        dummy.setType(prototype.getType());
        List<ResolveInfo> resInfo = aContext.getPackageManager().queryIntentActivities(dummy,0);

        Intent dummyRFC822 = new Intent(prototype.getAction());
        dummyRFC822.setType("message/rfc822");
        List<ResolveInfo> resInfo2 = aContext.getPackageManager().queryIntentActivities(dummyRFC822,0);
        
        if (null != resInfo && null != resInfo2 && !resInfo2.isEmpty()){
        	resInfo.addAll(resInfo2);

        	for (ResolveInfo resolveInfo : resInfo2){
        		rfc822PackageName.add(resolveInfo.activityInfo.packageName);
        	}
        }

        if (!resInfo.isEmpty()){
            for (ResolveInfo resolveInfo : resInfo){
                if (resolveInfo.activityInfo == null || 
                		(!Arrays.asList(whitelsit).contains(resolveInfo.activityInfo.packageName) && (!resInfo2.contains(resolveInfo))) ||
                		(Arrays.asList(blacklist).contains(resolveInfo.activityInfo.packageName)))
                    continue;
                if (intentPackageName.contains(resolveInfo.activityInfo.packageName)){
                	//found duplicate
                	continue;
                }
                intentPackageName.add(resolveInfo.activityInfo.packageName);
                //Get all the posible sharers
                HashMap<String, String> info = new HashMap<String, String>();
                info.put("packageName", resolveInfo.activityInfo.packageName);
                info.put("className", resolveInfo.activityInfo.name);
                String appName = String.valueOf(resolveInfo.activityInfo
                        .loadLabel(aContext.getPackageManager()));
                info.put("simpleName", appName);
                
                intentMetaInfo.add(info);
            }

            if (!intentMetaInfo.isEmpty())
            { //found at least one intent from whitelist
                // sorting for nice readability
                Collections.sort(intentMetaInfo,
                        new Comparator<HashMap<String, String>>()
                        {
                            @Override public int compare(
                                    HashMap<String, String> map,
                                    HashMap<String, String> map2)
                            {
                                return map.get("simpleName").compareTo(
                                        map2.get("simpleName"));
                            }
                        });

                // create the custom intent list
                for (HashMap<String, String> metaInfo : intentMetaInfo)
                {
                    Intent targetedShareIntent = (Intent) prototype.clone();
                    targetedShareIntent.setPackage(metaInfo.get("packageName"));
                    targetedShareIntent.setClassName(
                            metaInfo.get("packageName"),
                            metaInfo.get("className"));
                    String packageName = metaInfo.get("packageName");
                    boolean bRFC822 = false;
                    if (rfc822PackageName.contains(packageName)){
                    	bRFC822 = true;
                    }
                    setContent(packageName, targetedShareIntent, profileDisplayName, bRFC822, aContext);
                    targetedShareIntents.add(targetedShareIntent);
                }
                String shareVia = aContext.getString(R.string.tell_friend);
                chooserIntent = Intent.createChooser(targetedShareIntents
                        .remove(targetedShareIntents.size() - 1), shareVia);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        targetedShareIntents.toArray(new Parcelable[] {}));
                return chooserIntent;
            }else{
            	return InviteHelper.generateCustomChooserIntent(prototype,blacklist,profileDisplayName,aContext);
            }
        }

        return Intent.createChooser(prototype, aContext.getString(R.string.inviteContentTitle, getUserName(profileDisplayName,aContext)));
    }
    private static String getUserName(String profileDisplayName,Context aContext){
    	String displayName = profileDisplayName;
    	try{
        if(null == displayName || 0 == displayName.compareTo("")) {
            AccountManager manager = AccountManager.get(aContext); 
            Account[] accounts = manager.getAccountsByType("com.google"); 
            List<String> possibleEmails = new LinkedList<String>();

            for (Account account : accounts) {
              possibleEmails.add(account.name);
            }

            if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
                String email = possibleEmails.get(0);
                String[] parts = email.split("@");

                if (parts.length > 1){
                    displayName = parts[0];
                }
            }

        }
        
        if (null == displayName || 0 == displayName.compareTo("")){
        	//we really can't get display name, set ASUS
        	displayName = "ASUS";
        }
    	}catch(Throwable ignore){
        	displayName = "ASUS";
    	}
        return displayName;
    }
    public static void invite(Context aContext, String profileDisplayName){
    	String[] blacklist = new String[]{
    			"com.asus.sharerim",
    			"com.asus.server.azs",
    			"com.asus.supernote",
    			"com.google.android.apps.docs",
    			"com.android.nfc"
    			};
    	String[] whitelist = new String[]{
    			"com.android.bluetooth",
    			"com.google.android.apps.plus",
    			"com.google.android.talk",
    			"com.google.android.gm",
    			"com.facebook.katana",
    			"com.facebook.orca",
    			"jp.naver.line.android",
    			"com.asus.message",
    			"com.asus.email",
    			"mobisocial.omlet",
    			"com.whatsapp"
    			};

    	Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setAction(Intent.ACTION_SEND);
    	shareIntent.setType("text/plain");
    	shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,aContext.getString(R.string.inviteContentTitle, getUserName(profileDisplayName,aContext)));
//    	shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "I'm using (Share Link) for Android and I recommend it. Click here: http://www.yourdomain.com");

    	Intent aIntent = generateCustomChooserIntentWhitelist(shareIntent, whitelist, blacklist, profileDisplayName, aContext);
        //Intent aIntent = generateCustomChooserIntent(shareIntent, blacklist, profileDisplayName, aContext);
//    	aIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	aContext.startActivity(aIntent);
    }
    private static void setContent(String packageName, Intent targetedShareIntent, String profileDisplayName, boolean bRFC822, Context aContext) {
//String aInviteContentHtml = aContext.getString(iInviteContent, getUserName(profileDisplayName,aContext),weblink);
        String weblink = "https://play.google.com/store/apps/details?id=com.asus.filemanager&referrer=utm_source%3Dfilemanager%26utm_medium%3Dtell-a-friend";

        if (WrapEnvironment.IS_CN_DEVICE) {
        	weblink = "http://www.wandoujia.com/apps/com.asus.filemanager";
        }
        String aInviteContentHtml	  = aContext.getString(R.string.inviteContent_p3) + "<br/><br/>" +
        								weblink + "<br/><br/>" +
        								aContext.getString(R.string.inviteContent_p1) + "<br/><br/>" +
        								aContext.getString(R.string.inviteContent_p2);

        String aInviteContent	  = aContext.getString(R.string.inviteContent_p3) + "\n\n" +
        		weblink + "\n\n" +
				aContext.getString(R.string.inviteContent_p2) + "\n\n" +
				aContext.getString(R.string.inviteContent_p3);

        if(packageName.equalsIgnoreCase("com.google.android.gm")) {
            //Gmail
            targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(aInviteContentHtml));
            targetedShareIntent.setType("message/rfc822");
        } else if (packageName.contains("email")) {
            targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, aInviteContent);
            targetedShareIntent.setType("message/rfc822");
        } else if (packageName.equalsIgnoreCase("com.android.bluetooth")) {
            targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(aInviteContentHtml));
            targetedShareIntent.setType("text/html");
        } else if (bRFC822){
            targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, aInviteContent);
            targetedShareIntent.setType("message/rfc822");
        } else {
            targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, aInviteContent);
            targetedShareIntent.setType("text/plain");
        }
    }
    public static void inviteFB(Activity anHostActivity){
        Facebook facebook = new Facebook(anHostActivity, anHostActivity.getResources().getString(R.string.facebook_app_id));
        facebook.appInvites(anHostActivity.getResources().getString(R.string.appInviteFBLink),anHostActivity);
    }
	//chenhsin++
    public static void inviteGoogle(Activity activity,int requestCode){
    	Intent intent = new AppInviteInvitation.IntentBuilder(activity.getString(R.string.inviteContent_p3))
	    .setMessage(activity.getString(R.string.inviteContent_p3)) 
	    .build();
    	activity.startActivityForResult(intent, requestCode);
    }
    public static boolean isGoogleInviteAvailable(Activity activity){
        Intent intent = null;
        List<ResolveInfo> resInfo = null;
        try {
            intent = new AppInviteInvitation.IntentBuilder(activity.getString(R.string.inviteContent_p3))
                .setMessage(activity.getString(R.string.inviteContent_p3))
                .build();
            resInfo = activity.getPackageManager().queryIntentActivities(intent,0);
        }catch(Throwable ignore){

        }

        if (null != resInfo && !resInfo.isEmpty()){
            return true;
        }else{
            return false;
        }
    }
}
