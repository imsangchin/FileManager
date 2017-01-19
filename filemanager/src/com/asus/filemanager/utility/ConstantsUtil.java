package com.asus.filemanager.utility;

import com.asus.filemanager.wrap.WrapEnvironment;

public class ConstantsUtil {
   public interface DeviceStatus{
	   public final static int STATE_UNREGISTERED = -1;
	   public final static int STATE_OFFLINE = 0;
	   public final static int STATE_IDEL = 1;
	   public final static int STATE_ONLINE = 2;
	   public final static int STATE_IN_LOGIN_STAGE = 3;
	   public final static int STATE_NIC_ALIVE = 4;

   }

   public final static String ASUS_MUSIC_OPEN_ACTION = "asus.intent.action.FM_AUDIO_PREVIEW";
   public final static String ASUS_GALLERY_OPEN_ACTION = "asus_gallery_open_action";
   public final static String ASUS_MUSIC_PACKAGENAME = "com.asus.music";
   public final static String ASUS_MUSIC_FILEID_key = "fileid_key";
   public final static String ASUS_MUSIC_CLOUDTYPE_KEY = "cloudtype_key";
   public final static String ASUS_MUSIC_DEVICEID_KEY = "cloud_deviced_key";
   public final static String ASUS_MUSIC_CLOUD_PATH_KEY = "cloud_path_key";
   public final static String AUUS_MUSIC_CLOUD_ACCOUNT_KEY = "cloud_account_key";
   public final static String ASUS_MUSIC_PARAMS_KEY = "params_key";
   public final static String ASUS_MUSIC_GALLERY_FILENAME__KEY = "MUSIC_FILE_NAME_KEY";
   public final static String ASUS_GALLERY_PACKAGENAME = "com.asus.ephoto";
   public final static String ASUS_GALLERY_NEW_PACKAGENAME = "com.asus.gallery";


   public static class OpenType{
   public final static int OPEN_TYPE_DEFAULT = -1;
   public final static int OPEN_TYPE_MUSIC = 1;
   public final static int OPEN_TYPE_GALLERY = 2;
   }

   public final static boolean AZS_ENABLE = true;

   public final static boolean IS_AT_AND_T = WrapEnvironment.IS_AT_AND_T;

    // for EULA template type
    public static final int TEMPL_OPENSOURCE = 0;
    public static final int TEMPL_USERAGREEMENT = 1;

    public final static String PREV_INSPIREUS = "inspireus";

    public static final int MODE_TABLET                                  = 1000;
    public static final int MODE_TABLET_SEVEN_INCH                       = 1001;
    public static final int MODE_PHONE                                   = 1002;


    public static final int GRID_MODE_NORMAL    = 0;
    public static final int GRID_MODE_MEDIA     = 1;

    public static int mGridMode = GRID_MODE_NORMAL;

    public static boolean DEBUG = false;

    public static final String ICON_TAG = "icon-";
    public static final String LAYOUT_HINT = "LAYOUT_HINT";

    public enum TYPE_HINTLAYOUT {
        SHORTCUT, EDITCATEGORY
    }
}
