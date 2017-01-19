package com.asus.filemanager.wrap;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.reflectionApis;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WrapEnvironment {

    public static String  MOUNT_POINT_MICROSD;
    public static String  MOUNT_POINT_USBDISK1;
    public static String  MOUNT_POINT_USBDISK2;
    public static String  MOUNT_POINT_SDREADER;
    public static String  MOUNT_POINT_MICROSD_KEY;

    public static String  SDCARD_CANONICAL_PATH;
    public static String  MICROSD_CANONICAL_PATH;
    public static String  USBDISK1_CANONICAL_PATH;
    public static String  USBDISK2_CANONICAL_PATH;
    public static String  USBDISK3_CANONICAL_PATH;
    public static String  USBDISK4_CANONICAL_PATH;
    public static String  USBDISK5_CANONICAL_PATH;
    public static String  USBDISK6_CANONICAL_PATH;
    public static String  USBDISK7_CANONICAL_PATH;
    public static String  USBDISK8_CANONICAL_PATH;
    public static String  SDREADER_CANONICAL_PATH;

    public static File EPAD_EXTERNAL_STORAGE_DIRECTORY;
    public static File EPAD_INTERNAL_STORAGE_DIRECTORY;
    public static File EPAD_EXTERNAL_STORAGE_DIRECTORY_NO_REMOVABLE;

    public static boolean SUPPORT_REMOVABLE;
    public static boolean SUPPORT_STORAGE_SD_OR_USB;

    public static String SECONDARY_STORAGE = null;

    static {
        MOUNT_POINT_MICROSD =
            reflectionApis.getSystemProperty("ro.epad.mount_point.microsd", "/Removable/MicroSD");
        MOUNT_POINT_USBDISK1 =
            reflectionApis.getSystemProperty("ro.epad.mount_point.usbdisk1", "/Removable/USBdisk1");
        MOUNT_POINT_USBDISK2 =
            reflectionApis.getSystemProperty("ro.epad.mount_point.usbdisk2", "/Removable/USBdisk2");
        MOUNT_POINT_SDREADER =
            reflectionApis.getSystemProperty("ro.epad.mount_point.sdreader", "/Removable/SD");
        MOUNT_POINT_MICROSD_KEY = "sdcard1";

        SDCARD_CANONICAL_PATH = getCanonicalPath(Environment.getExternalStorageDirectory().getAbsolutePath(), Environment.getExternalStorageDirectory().getAbsolutePath());
        MICROSD_CANONICAL_PATH = getCanonicalPath("/storage/MicroSD", "/storage/MicroSD");
        USBDISK1_CANONICAL_PATH = getCanonicalPath("/storage/USBdisk1", "/storage/USBdisk1");
        USBDISK2_CANONICAL_PATH = getCanonicalPath("/storage/USBdisk2", "/storage/USBdisk2");
        USBDISK3_CANONICAL_PATH = getCanonicalPath("/storage/USBdisk3", "/storage/USBdisk3");
        USBDISK4_CANONICAL_PATH = getCanonicalPath("/storage/USBdisk4", "/storage/USBdisk4");
        USBDISK5_CANONICAL_PATH = getCanonicalPath("/storage/USBdisk5", "/storage/USBdisk5");
        USBDISK6_CANONICAL_PATH = getCanonicalPath("/storage/USBdisk6", "/storage/USBdisk6");
        USBDISK7_CANONICAL_PATH = getCanonicalPath("/storage/USBdisk7", "/storage/USBdisk7");
        USBDISK8_CANONICAL_PATH = getCanonicalPath("/storage/USBdisk8", "/storage/USBdisk8");
        SDREADER_CANONICAL_PATH = getCanonicalPath("/storage/SD", "/storage/SD");

        EPAD_EXTERNAL_STORAGE_DIRECTORY = getDirectory("EPAD_EXTERNAL_STORAGE", "/Removable");
        EPAD_INTERNAL_STORAGE_DIRECTORY = getDirectory("EPAD_INTERNAL_STORAGE", "/sdcard");
        EPAD_EXTERNAL_STORAGE_DIRECTORY_NO_REMOVABLE = getDirectory("EPAD_EXTERNAL_STORAGE", "/storage");
        try {
            String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
            // Add all secondary storages
            if(!TextUtils.isEmpty(rawSecondaryStoragesStr))
            {
                // All Secondary SD-CARDs splitted into array
                final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
                if (rawSecondaryStorages.length>0){
                    SECONDARY_STORAGE = rawSecondaryStorages[0];
                }
            }

        }catch (Throwable ignore){};


        SUPPORT_REMOVABLE = (new File("/Removable")).exists();
        SUPPORT_STORAGE_SD_OR_USB =
            new File("/storage/MicroSD").exists() ||
                new File("/storage/USBdisk1").exists() ||
                new File("/storage/USBdisk2").exists() ||
                new File("/storage/USBdisk3").exists() ||
                new File("/storage/USBdisk4").exists() ||
                new File("/storage/USBdisk5").exists();
    }



    public static File getEpadExternalStorageDirectory() {
        return SUPPORT_REMOVABLE ? EPAD_EXTERNAL_STORAGE_DIRECTORY : EPAD_EXTERNAL_STORAGE_DIRECTORY_NO_REMOVABLE;
    }

    public static File getEpadExternalStoragePublicDirectory(String type) {
        return new File(getEpadExternalStorageDirectory(), type);
    }

    public static File getEpadInternalStorageDirectory() {
        return EPAD_INTERNAL_STORAGE_DIRECTORY;
    }

    public static File getEpadInternalStoragePublicDirectory(String type) {
        return new File(getEpadInternalStorageDirectory(), type);
    }

    private static File getDirectory(String variableName, String defaultPath) {
        String path = System.getenv(variableName);
        return path == null ? new File(defaultPath) : new File(path);
    }

    public static final boolean IS_AT_AND_T = "att".equalsIgnoreCase(reflectionApis.getSystemProperty("ro.build.asus.sku",""));

    public static boolean isAZSFeatureExist(Context context)
    {
      if (context == null) {
        return false;
      }
      FeatureInfo[] featureInfos = context.getPackageManager().getSystemAvailableFeatures();
      if ((featureInfos != null) && (featureInfos.length > 0)) {
        for (FeatureInfo feature : featureInfos) {
          if ((feature != null) && (feature.name != null) && ("asus.software.azs".contains(feature.name))) {
        	  return true;
          }
        }
      }

      return false;
    }

    public static boolean isAZSPackageExist(Context context)
    {
      if (context == null) {
        return false;
      }

      PackageManager pm = context.getPackageManager();
      if (pm != null) {
          try {
        	  if (pm.getPackageInfo("com.asus.server.azs", 0) != null)
        		  return true;
          } catch (PackageManager.NameNotFoundException e) {

          }
      }

      return false;
    }

    public static boolean isAZSEnable(Context context) {
 	   return isAZSFeatureExist(context) && isAZSPackageExist(context);
    }

    private static final String VERIZON_PACKAGE_NAME = "com.vcast.mediamanager";
    private static final String VERIZON_ACTION_FILES = "com.vcast.mediamanager.ACTION_FILES";

    private static final String CM_PACKAGE_NAME = "com.cleanmaster.mguard";


    public static final boolean IS_VERIZON = "vzw".equalsIgnoreCase(reflectionApis.getSystemProperty("ro.build.asus.sku",""));
    public static final String MODEL_NAME = reflectionApis.getSystemProperty("ro.product.device","");

    public static boolean isVerizonPackageExist(Context context)
    {
        if (context == null) {
            return false;
        }

        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            try {
                if (pm.getPackageInfo(VERIZON_PACKAGE_NAME, 0) != null)
                    return true;
            } catch (PackageManager.NameNotFoundException e) {

            }
        }

        return false;
    }

    private static final String GooglePlayStorePackageNameOld = "com.google.market";
    private static final String GooglePlayStorePackageNameNew = "com.android.vending";

    public static boolean isPlayStorePackageExist(Context aContext) {
        PackageManager pm = aContext.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(GooglePlayStorePackageNameOld) ||
                packageInfo.packageName.equals(GooglePlayStorePackageNameNew)) {
                return true;
            }
        }
        return false;
    }

    private static final String CM_PKG_NAME = "com.cleanmaster.mguard";
    private static final String CM_PKG_NAME_CN = "com.cleanmaster.mguard_cn";
    private static final String CM_PKG_NAME_X86 = "com.cleanmaster.mguard_x86";
    private static final String CM_STUB_ACTIVITY = "com.keniu.security.main.MainActivity";
    private static final String CM_SWITCH_ACTIVITY = "com.cooperate.UISwitchActivity";
    private static final String CM_ACTIVITY_EXTRA_TO = "extra_to";
    private static final String CM_ACTIVITY_EXTRA_FROM = "extra_from";
    private static final String CM_ACTIVITY_JUNK_STANDARD = "junk";
    private static final String CM_ACTIVITY_SOURCE = "source";
    private static final String CM_ACTITIVY_SOURCE_VALUE = "266";

    private static boolean isActivityExist(Context aContext, Intent intent){
        PackageManager pm = aContext.getPackageManager();
        if (pm != null) {
            if(aContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                return true;
            }
        }

        return false;
    }
    private static Intent getCleanMasterIntent(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(CM_PKG_NAME, CM_SWITCH_ACTIVITY));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(CM_ACTIVITY_EXTRA_FROM, context.getPackageName());
        intent.putExtra(CM_ACTIVITY_EXTRA_TO, CM_ACTIVITY_JUNK_STANDARD);
        intent.putExtra(CM_ACTIVITY_SOURCE, CM_ACTITIVY_SOURCE_VALUE);

        if (isActivityExist(context, intent)) {
            return intent;
        }else{
            intent.setComponent(new ComponentName(CM_PKG_NAME_CN, CM_SWITCH_ACTIVITY));
            if (isActivityExist(context, intent)) {
                return intent;
            }
            intent.setComponent(new ComponentName(CM_PKG_NAME_X86, CM_SWITCH_ACTIVITY));
            if (isActivityExist(context, intent)) {
                return intent;
            }
            intent.setComponent(new ComponentName(CM_PKG_NAME, CM_SWITCH_ACTIVITY));
        }

        intent.setComponent(new ComponentName(CM_PKG_NAME, CM_STUB_ACTIVITY));
        if (isActivityExist(context, intent)) {
            return intent;
        }
        return null;
    }

    public static boolean isCMPackagePreloadedAndExist(Context context)
    {
        if (context == null) {
            return false;
        }

        // asus preload app is stored in system partition
        // we only keep CM tool item in homepage for the asus user whose device
        // has preloaded CM app.
        boolean isAsusDevice = "asus".equalsIgnoreCase(Build.BRAND);
        boolean isCmSystemApp = isSystemApp(context, CM_PKG_NAME)
                || isSystemApp(context, CM_PKG_NAME_CN)
                || isSystemApp(context, CM_PKG_NAME_X86);
        boolean isCmPreloaded = isAsusDevice && isCmSystemApp;

        if (isCmPreloaded && null != getCleanMasterIntent(context))
                    return true;

        return false;
    }

    private static boolean isSystemApp(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static void launchCM(Context context)
    {

        try {
            Intent intent = getCleanMasterIntent(context);
            context.startActivity(intent);
        }
        catch(Exception e) {  // ActivityNotFoundException
            try {
                Intent i = context.getPackageManager().getLaunchIntentForPackage(CM_PACKAGE_NAME);
                context.startActivity(i);
            } catch (Throwable ignore) {


            }
        }
    }
    
    public static boolean isVerizonEnable(Context context) {
        return IS_VERIZON && isVerizonPackageExist(context);
    }

    public static Intent getVerizonActionFileIntent(Context context)
    {
        PackageManager packageManager = context.getPackageManager();
        Intent inetent = packageManager.getLaunchIntentForPackage(VERIZON_PACKAGE_NAME);
        if(inetent!=null)
        {
          inetent.setAction(VERIZON_ACTION_FILES);
        }
        return inetent;
    }

    public static String getVerizonLabel(Context context)
    {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(VERIZON_PACKAGE_NAME, PackageManager.GET_META_DATA);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    public static Drawable getVerizonIcon(Context context)
    {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(VERIZON_PACKAGE_NAME, PackageManager.GET_META_DATA);
            return packageManager.getApplicationIcon(applicationInfo);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean SUPPORT_FEATURE_ASUS_PEN = supportFeature(FileManagerApplication.getAppContext(), "asus.hardware.pen");

    public static boolean supportFeature(Context context, String feature){
    	if(context != null){
			PackageManager pm = context.getPackageManager();
			if (pm != null) {
			    return pm.hasSystemFeature(feature);
			}
    	}
		return false;
	}

    private static String getCanonicalPath(String filePath, String defaultPath) {
    	String path = defaultPath;

    	File file = new File(filePath);
        try {
			path = FileUtility.getCanonicalPath(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return path;
    }
	
	public static boolean IS_LOW_MEMORY_DEVICE = false;
    public static boolean isLowMemoryDevice(Context context) {

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN || context==null) {
            return false;
        } else {
            ActivityManager actManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            long totalMemory = memInfo.totalMem;
            // Memory size <= 1G, Low memory device
            if (totalMemory <= 1073741824L) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static final boolean IS_CN_DEVICE = reflectionApis.getSystemProperty("persist.sys.cta.security","").toLowerCase().startsWith("1") |
            "cn".equalsIgnoreCase(reflectionApis.getSystemProperty("ro.build.asus.sku",""));

    public static boolean isSupportCloud() {
        boolean ret = true;
        if (ConstantsUtil.IS_AT_AND_T)
            ret = false;

        if (WrapEnvironment.IS_VERIZON) {
            ret = false;
        }

        if ((IS_CN_DEVICE) && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)) {
            ret = false;
        }
        return ret;
    }
}
